package org.andstatus.game2048

import android.content.Context
import android.net.Uri
import com.soywiz.korio.lang.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class DocumentSequence(val context: Context, val uri: Uri) : Sequence<String>, Closeable {
    private val bufferLength = 20000
    private val buffer = CharArray(bufferLength)
    private val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    private val inputStreamReader: InputStreamReader
    private val sequence: Sequence<String>

    init {
        inputStreamReader = InputStreamReader(inputStream ?: InputStream.nullInputStream(), StandardCharsets.UTF_8)
        sequence = generateSequence {
            val count: Int = inputStreamReader.read(buffer)
            if (count == -1) {
                null
            } else {
                if (count < bufferLength) {
                    String(buffer.copyOf(count))
                } else {
                    String(buffer)
                }
            }
        }
    }

    override fun iterator(): Iterator<String> = sequence.iterator()

    override fun close() {
        try {
            inputStreamReader?.close()
            inputStream?.close()
        } catch (t: Throwable) {
            myLog("Error closing ${this::class}: $t")
        }
    }
}
