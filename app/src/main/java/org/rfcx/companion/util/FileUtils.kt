package org.rfcx.companion.util

import android.webkit.MimeTypeMap
import java.io.File
import java.util.*


object FileUtils {

    fun File.getMimeType(fallback: String = "image/*"): String {
        return MimeTypeMap.getFileExtensionFromUrl(toString())
            ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(toLowerCase(Locale.getDefault())) }
            ?: fallback
    }
}
