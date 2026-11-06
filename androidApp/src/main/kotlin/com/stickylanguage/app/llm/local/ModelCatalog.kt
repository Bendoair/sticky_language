package com.stickylanguage.app.llm.local

/**
 * Curated on-device GGUF presets. Sizes and SHA-256 come from Hugging Face LFS metadata.
 */
object ModelCatalog {
    const val DEFAULT_ID = "qwen25-15b"

    val all: List<ModelSpec> = listOf(
        ModelSpec(
            id = "qwen25-05b",
            displayName = "Qwen2.5 0.5B",
            description = "Fastest. Shallow notes; best for low-RAM phones.",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            bytes = 491_400_032L,
            sha256 = "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db",
            minRamMb = 4_096,
            quant = "Q4_K_M",
        ),
        ModelSpec(
            id = DEFAULT_ID,
            displayName = "Qwen2.5 1.5B",
            description = "Default. Usable multilingual tutor on mid-range phones.",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            bytes = 1_117_320_736L,
            sha256 = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e",
            minRamMb = 6_144,
            quant = "Q4_K_M",
        ),
        ModelSpec(
            id = "qwen25-3b",
            displayName = "Qwen2.5 3B",
            description = "Best quality. Needs ~8 GB RAM.",
            fileName = "qwen2.5-3b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
            bytes = 2_104_932_768L,
            sha256 = "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d",
            minRamMb = 8_192,
            quant = "Q4_K_M",
        ),
        ModelSpec(
            id = "llama32-1b",
            displayName = "Llama 3.2 1B",
            description = "English-strong fallback; smaller multilingual edge.",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            bytes = 807_694_464L,
            sha256 = "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83",
            minRamMb = 4_096,
            quant = "Q4_K_M",
        ),
    )

    fun byId(id: String): ModelSpec? = all.firstOrNull { it.id == id }
}

data class ModelSpec(
    val id: String,
    val displayName: String,
    val description: String,
    val fileName: String,
    val url: String,
    val bytes: Long,
    val sha256: String,
    val minRamMb: Int,
    val quant: String,
) {
    val sizeLabel: String
        get() {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) String.format("%.1f GB", gb) else String.format("%.0f MB", bytes / (1024.0 * 1024.0))
        }
}
