package org.rfcx.companion.util

import android.os.Environment
import org.rfcx.companion.BuildConfig
import java.io.File
import java.util.*

object ImageUtils {

    const val FILE_CONTENT_PROVIDER = BuildConfig.APPLICATION_ID + ".provider"
    const val REQUEST_TAKE_PHOTO = 4001
    const val REQUEST_GALLERY = 5001

    // region Take a photo
    fun createImageFile(): File {
        val directoryName = "RFCx-Companion"
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
}
