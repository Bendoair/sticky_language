package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceTranslation
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory

/**
 * Host (desktop Linux/macOS) smoke test for the on-device GGUF presets.
 *
 * Why not the Android AAR / an emulator? The vendored llama-kotlin `.so` libraries are
 * Android ELF and will not load in a plain JVM on this computer. llama.cpp's
 * `llama-completion` binary exercises the same GGUF + JSON-schema constrained decoding
 * path the phone uses, which is enough to verify models, prompts, and schemas.
 *
 * Opt-in (skipped in normal `./gradlew test`):
 * ```
 * tools/local-llm-smoke/run.sh
 * # or
 * LOCAL_LLM_SMOKE=1 ./gradlew :shared:test --tests '*.LocalLlmHostSmokeTest'
 * ```
 *
 * Env overrides: `LOCAL_LLM_CLI`, `LOCAL_LLM_MODEL`, `LOCAL_LLM_LIB`.
 */
class LocalLlmHostSmokeTest {

    @Test
    fun translate_korean_phrase_produces_parseable_json() {
        assumeEnabled()
        val source = "안녕하세요"
        val learned = Language.KOREAN
        val explain = Language.ENGLISH
        val system = LlmPrompts.translateSentenceSystem(learned, explain) +
            LlmPrompts.schemaFallbackSuffix(LlmSchemas.describe(LlmSchemas.sentenceTranslation))
        val user = LlmPrompts.translateSentenceUser(source, emptyList(), learned, explain)

        val raw = runCompletion(
            system = system,
            user = user,
            schema = LlmSchemas.sentenceTranslation.toString(),
            nPredict = 2048,
            label = "translate",
        )
        val result = try {
            LlmJson.decode(SentenceTranslation.serializer(), raw)
        } catch (e: Exception) {
            dumpFailure("translate", raw)
            throw AssertionError(
                "Failed to parse translation JSON (${e.message}). " +
                    "Raw output dumped to ${failureDump("translate").absolutePath}. " +
                    "Snippet: ${raw.take(400)}…${raw.takeLast(200)}",
                e,
            )
        }

        assertTrue("translation blank: $raw", result.translation.isNotBlank())
        println("translate OK [${model().name}] → \"${result.translation}\"")
    }

    @Test
    fun breakdown_korean_phrase_produces_parseable_json() {
        assumeEnabled()
        val source = "저는 학생입니다."
        val learned = Language.KOREAN
        val explain = Language.ENGLISH
        val system = LlmPrompts.breakdownSystem(learned, explain) +
            LlmPrompts.schemaFallbackSuffix(LlmSchemas.describe(LlmSchemas.breakdown))
        val user = LlmPrompts.breakdownUser(source, learned, explain)

        val raw = runCompletion(
            system = system,
            user = user,
            schema = LlmSchemas.breakdown.toString(),
            nPredict = 2048,
            label = "breakdown",
        )
        val result = try {
            LlmJson.decode(BreakdownResult.serializer(), raw)
        } catch (e: Exception) {
            dumpFailure("breakdown", raw)
            throw AssertionError(
                "Failed to parse breakdown JSON (${e.message}). " +
                    "Raw output dumped to ${failureDump("breakdown").absolutePath}. " +
                    "Snippet: ${raw.take(400)}…${raw.takeLast(200)}",
                e,
            )
        }

        assertTrue("no sentences: $raw", result.sentences.isNotEmpty())
        assertTrue("first sentence has no chunks: $raw", result.sentences.first().chunks.isNotEmpty())
        println(
            "breakdown OK [${model().name}] → ${result.sentences.size} sentence(s), " +
                "first has ${result.sentences.first().chunks.size} chunks",
        )
    }

    private fun assumeEnabled() {
        assumeTrue(
            "Set LOCAL_LLM_SMOKE=1 to run host GGUF smoke tests",
            System.getenv("LOCAL_LLM_SMOKE") == "1" || System.getProperty("local.llm.smoke") == "true",
        )
        assumeTrue("llama-completion not found at ${cli().absolutePath}", cli().isFile && cli().canExecute())
        assumeTrue("GGUF not found at ${model().absolutePath}", model().isFile && model().length() > 1_000_000)
    }

    private fun runCompletion(
        system: String,
        user: String,
        schema: String,
        nPredict: Int,
        label: String,
    ): String {
        val work = createTempDirectory("sticky-llm-smoke-$label").toFile()
        try {
            val schemaFile = File(work, "schema.json").apply { writeText(schema) }
            val systemFile = File(work, "system.txt").apply { writeText(system) }
            val userFile = File(work, "user.txt").apply { writeText(user) }

            val cmd = listOf(
                cli().absolutePath,
                "-m", model().absolutePath,
                "-c", "4096",
                "-n", nPredict.toString(),
                "--temp", "0.2",
                "--repeat-penalty", "1.15",
                "--jinja",
                "--single-turn",
                "--no-display-prompt",
                "-sysf", systemFile.absolutePath,
                "-f", userFile.absolutePath,
                "-jf", schemaFile.absolutePath,
            )
            return launch(cmd, libDir(), work, timeoutMinutes = 8)
        } finally {
            work.deleteRecursively()
        }
    }

    private fun launch(cmd: List<String>, libDir: File, work: File, timeoutMinutes: Long): String {
        val pb = ProcessBuilder(cmd).directory(work)
        val env = pb.environment()
        val existing = env["LD_LIBRARY_PATH"].orEmpty()
        env["LD_LIBRARY_PATH"] = if (existing.isBlank()) {
            libDir.absolutePath
        } else {
            "${libDir.absolutePath}:$existing"
        }

        val proc = pb.start()
        val stdout = proc.inputStream.bufferedReader().readText()
        val stderr = proc.errorStream.bufferedReader().readText()
        val finished = proc.waitFor(timeoutMinutes, TimeUnit.MINUTES)
        if (!finished) {
            proc.destroyForcibly()
            error("llama-completion timed out after ${timeoutMinutes}m\n$stderr")
        }
        if (proc.exitValue() != 0) {
            error("llama-completion exit ${proc.exitValue()}\nstderr:\n$stderr\nstdout:\n$stdout")
        }
        val cleaned = cleanModelOutput(stdout)
        if (cleaned.isBlank()) {
            error("empty model output\nstderr:\n$stderr\nstdout:\n$stdout")
        }
        return cleaned
    }

    private fun dumpFailure(label: String, raw: String) {
        failureDump(label).apply {
            parentFile?.mkdirs()
            writeText(raw)
        }
    }

    companion object {
        fun cleanModelOutput(raw: String): String {
            var t = raw.trim()
            t = t.removeSuffix("[end of text]").trim()
            t = t.removeSuffix("<|im_end|>").trim()
            return LlmJson.stripFences(t)
        }

        fun repoRoot(): File {
            var dir = File(System.getProperty("user.dir")).canonicalFile
            repeat(6) {
                if (File(dir, "settings.gradle.kts").exists()) return dir
                dir = dir.parentFile ?: return@repeat
            }
            return File(System.getProperty("user.dir")).canonicalFile
        }

        fun smokeDir(): File = File(repoRoot(), "tools/local-llm-smoke")

        fun failureDump(label: String): File = File(smokeDir(), "failures/$label.json")

        fun cli(): File {
            System.getenv("LOCAL_LLM_CLI")?.let { return File(it) }
            return File(smokeDir(), "llama.cpp/build/bin/llama-completion")
        }

        fun model(): File {
            System.getenv("LOCAL_LLM_MODEL")?.let { return File(it) }
            // Prefer the app's default 1.5B when present; fall back to 0.5B.
            val preferred = File(smokeDir(), "models/qwen2.5-1.5b-instruct-q4_k_m.gguf")
            if (preferred.isFile) return preferred
            return File(smokeDir(), "models/qwen2.5-0.5b-instruct-q4_k_m.gguf")
        }

        fun libDir(): File {
            System.getenv("LOCAL_LLM_LIB")?.let { return File(it) }
            return cli().parentFile
        }
    }
}
