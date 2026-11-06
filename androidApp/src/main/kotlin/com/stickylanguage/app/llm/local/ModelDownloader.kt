package com.stickylanguage.app.llm.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data class Downloading(val modelId: String, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState() {
        val progress: Float get() = if (totalBytes <= 0L) 0f else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
    }
    data class Verifying(val modelId: String) : ModelDownloadState()
    data class Ready(val modelId: String) : ModelDownloadState()
    data class Failed(val modelId: String, val message: String) : ModelDownloadState()
}

@Singleton
class ModelDownloader @Inject constructor(
    private val store: ModelStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var job: Job? = null

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()

    fun download(spec: ModelSpec) {
        if (store.isReady(spec)) {
            _state.value = ModelDownloadState.Ready(spec.id)
            return
        }
        job?.cancel()
        job = scope.launch {
            mutex.withLock {
                try {
                    runDownload(spec)
                } catch (e: CancellationException) {
                    _state.value = ModelDownloadState.Idle
                    throw e
                } catch (e: Exception) {
                    _state.value = ModelDownloadState.Failed(spec.id, e.message ?: "Download failed")
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = ModelDownloadState.Idle
    }

    suspend fun delete(spec: ModelSpec) {
        cancel()
        withContext(Dispatchers.IO) { store.delete(spec) }
        _state.value = ModelDownloadState.Idle
    }

    private fun runDownload(spec: ModelSpec) {
        if (!store.canFitDownload(spec) && !store.partialFileFor(spec).exists()) {
            throw IllegalStateException("Not enough free storage for ${spec.sizeLabel}")
        }

        val partial = store.partialFileFor(spec)
        val dest = store.fileFor(spec)
        var existing = if (partial.exists()) partial.length() else 0L
        if (existing > spec.bytes) {
            partial.delete()
            existing = 0L
        }

        _state.value = ModelDownloadState.Downloading(spec.id, existing, spec.bytes)

        val requestBuilder = Request.Builder().url(spec.url).header("User-Agent", "StickyLanguage/1.0")
        if (existing > 0L) requestBuilder.header("Range", "bytes=$existing-")

        http.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Empty response body")
            val append = response.code == 206 && existing > 0L
            RandomAccessFile(partial, "rw").use { raf ->
                if (append) raf.seek(existing) else {
                    raf.setLength(0)
                    existing = 0L
                }
                val buffer = ByteArray(256 * 1024)
                var downloaded = existing
                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        raf.write(buffer, 0, read)
                        downloaded += read
                        _state.value = ModelDownloadState.Downloading(spec.id, downloaded.coerceAtMost(spec.bytes), spec.bytes)
                    }
                }
            }
        }

        _state.value = ModelDownloadState.Verifying(spec.id)
        val hash = sha256(partial)
        if (!hash.equals(spec.sha256, ignoreCase = true)) {
            partial.delete()
            throw IllegalStateException("Checksum mismatch — download corrupted, try again")
        }
        if (partial.length() != spec.bytes) {
            partial.delete()
            throw IllegalStateException("Unexpected file size ${partial.length()} (expected ${spec.bytes})")
        }
        if (dest.exists()) dest.delete()
        if (!partial.renameTo(dest)) {
            partial.copyTo(dest, overwrite = true)
            partial.delete()
        }
        _state.value = ModelDownloadState.Ready(spec.id)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun refreshReady(spec: ModelSpec) {
        if (store.isReady(spec)) _state.update { ModelDownloadState.Ready(spec.id) }
    }
}
