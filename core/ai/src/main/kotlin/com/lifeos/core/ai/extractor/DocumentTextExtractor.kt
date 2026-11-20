package com.lifeos.core.ai.extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extrator unificado de documentos do Life OS (Pilar 5):
 * - `.md` / `.txt`: leitura direta UTF-8.
 * - `.docx`: descompactação nativa OpenXML (`word/document.xml`) sem peso de bibliotecas externas.
 * - `.pdf`: renderização nativa via `PdfRenderer` + reconhecimento óptico ML Kit.
 * - Imagens (`PNG`/`JPG`): OCR direto de páginas de livros físicos ou recibos via ML Kit.
 */
@Singleton
class DocumentTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun extractTextFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val fileName = resolveDisplayName(uri).lowercase(Locale.ROOT)

        when {
            mimeType.contains("pdf") || fileName.endsWith(".pdf") -> extractFromPdf(uri)
            mimeType.contains("wordprocessingml") || fileName.endsWith(".docx") -> extractFromDocx(uri)
            mimeType.startsWith("image/") -> extractFromImage(uri)
            else -> extractFromPlainText(uri)
        }
    }

    private fun extractFromPlainText(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }.orEmpty()
    }

    private fun extractFromDocx(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        return inputStream.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        return@use parseOpenXmlDocument(zip)
                    }
                    entry = zip.nextEntry
                }
                ""
            }
        }
    }

    private fun parseOpenXmlDocument(stream: InputStream): String {
        val parser = Xml.newPullParser()
        parser.setInput(stream, Charsets.UTF_8.name())
        val builder = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        builder.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        builder.append('\n')
                    }
                }
            }
            eventType = parser.next()
        }
        return builder.toString().trim()
    }

    private suspend fun extractFromPdf(uri: Uri): String {
        val tempPdf = File(context.cacheDir, "lifeos_ingest_${System.currentTimeMillis()}.pdf")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempPdf).use { output -> input.copyTo(output) }
            } ?: return ""

            val pfd = android.os.ParcelFileDescriptor.open(
                tempPdf,
                android.os.ParcelFileDescriptor.MODE_READ_ONLY
            )
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val fullText = StringBuilder()

            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val maxPages = renderer.pageCount.coerceAtMost(MAX_PDF_PAGES)
                    for (pageIndex in 0 until maxPages) {
                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                (page.width * 2).coerceAtLeast(1),
                                (page.height * 2).coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            try {
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                                val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                                if (result.text.isNotBlank()) {
                                    fullText.appendLine(result.text)
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
            recognizer.close()
            fullText.toString().trim()
        } finally {
            tempPdf.delete()
        }
    }

    private suspend fun extractFromImage(uri: Uri): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return ""

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else ""
            }.orEmpty()
        }.getOrDefault("")
    }

    companion object {
        private const val MAX_PDF_PAGES = 15
    }
}
