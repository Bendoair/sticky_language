package com.stickylanguage.shared.model

/**
 * Script family used to pick an on-device OCR recognizer.
 */
enum class Script { LATIN, KOREAN, JAPANESE, CHINESE, DEVANAGARI }

data class Language(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val script: Script = Script.LATIN,
) {
    val displayName: String get() = if (nativeName == englishName) englishName else "$englishName · $nativeName"

    companion object {
        val KOREAN = Language("ko", "Korean", "한국어", Script.KOREAN)
        val ENGLISH = Language("en", "English", "English")
        val JAPANESE = Language("ja", "Japanese", "日本語", Script.JAPANESE)
        val CHINESE = Language("zh", "Chinese", "中文", Script.CHINESE)
        val SPANISH = Language("es", "Spanish", "Español")
        val FRENCH = Language("fr", "French", "Français")
        val GERMAN = Language("de", "German", "Deutsch")
        val ITALIAN = Language("it", "Italian", "Italiano")
        val PORTUGUESE = Language("pt", "Portuguese", "Português")
        val DUTCH = Language("nl", "Dutch", "Nederlands")
        val HUNGARIAN = Language("hu", "Hungarian", "Magyar")
        val POLISH = Language("pl", "Polish", "Polski")
        val TURKISH = Language("tr", "Turkish", "Türkçe")
        val VIETNAMESE = Language("vi", "Vietnamese", "Tiếng Việt")
        val INDONESIAN = Language("id", "Indonesian", "Bahasa Indonesia")
        val HINDI = Language("hi", "Hindi", "हिन्दी", Script.DEVANAGARI)

        val all: List<Language> = listOf(
            KOREAN, ENGLISH, JAPANESE, CHINESE, SPANISH, FRENCH, GERMAN, ITALIAN,
            PORTUGUESE, DUTCH, HUNGARIAN, POLISH, TURKISH, VIETNAMESE, INDONESIAN, HINDI,
        )

        fun byCode(code: String?): Language? = all.firstOrNull { it.code == code }
    }
}
