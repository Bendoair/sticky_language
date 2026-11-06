package com.stickylanguage.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stickylanguage.shared.model.Script
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device OCR via ML Kit. The recognizer is picked from the learned language's script.
 */
@Singleton
class OcrService @Inject constructor(@ApplicationContext private val context: Context) {

    private val recognizers = mutableMapOf<Script, TextRecognizer>()

    private fun recognizer(script: Script): TextRecognizer = recognizers.getOrPut(script) {
        when (script) {
            Script.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Script.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            Script.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            Script.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            Script.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        }
    }

    suspend fun recognize(uri: Uri, script: Script): String {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer(script).process(image).await()
        // Keep block/line structure: lines inside a block are joined with newlines, blocks separated by a blank line.
        return result.textBlocks.joinToString("\n") { block ->
            block.lines.joinToString("\n") { it.text }
        }.trim()
    }
}
