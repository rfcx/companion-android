package org.rfcx.companion.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import org.rfcx.companion.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object ImageUtils {

    const val FILE_CONTENT_PROVIDER = BuildConfig.APPLICATION_ID + ".provider"
    const val REQUEST_TAKE_PHOTO = 4001
    const val REQUEST_GALLERY = 5001

    // region Take a photo
    fun createImageFile(): File {
        val directoryName = "RFCx-Guardian-App"
        val imageFileName = "IMG_${Date().time}"
        val directory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).absolutePath,
            directoryName
        )

        if (!directory.exists()) {
            directory.mkdir()
        }

        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            directory /* directory */
        )
    }

    fun createImageFile(image: Uri, context: Context): String? {
        val contentResolver = context.contentResolver ?: return null
        val filePath = (context.applicationInfo.dataDir + File.separator
                + System.currentTimeMillis())

        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(image) ?: return null
            val outputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (ignore: IOException) {
            return null
        }
        return file.absolutePath
    }
}
