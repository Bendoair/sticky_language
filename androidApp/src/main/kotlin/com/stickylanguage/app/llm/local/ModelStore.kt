package com.stickylanguage.app.llm.local

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    fun fileFor(spec: ModelSpec): File = File(root, spec.fileName)

    fun partialFileFor(spec: ModelSpec): File = File(root, "${spec.fileName}.partial")

    fun isReady(spec: ModelSpec): Boolean {
        val f = fileFor(spec)
        return f.isFile && f.length() == spec.bytes
    }

    fun delete(spec: ModelSpec) {
        fileFor(spec).delete()
        partialFileFor(spec).delete()
    }

    fun freeBytes(): Long = try {
        StatFs(root.absolutePath).availableBytes
    } catch (_: Exception) {
        0L
    }

    fun deviceRamMb(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return (info.totalMem / (1024L * 1024L)).toInt()
    }

    fun canFitDownload(spec: ModelSpec): Boolean {
        val needed = spec.bytes + 64L * 1024L * 1024L // spare headroom
        return freeBytes() >= needed
    }

    fun meetsRam(spec: ModelSpec): Boolean = deviceRamMb() >= spec.minRamMb
}
