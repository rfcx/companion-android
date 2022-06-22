package org.rfcx.companion.util.file

import android.content.Context
import org.rfcx.companion.entity.guardian.Classifier
import java.io.File

object ClassifierUtils {

    fun getAllDownloadedClassifiers(context: Context): Array<File>? {
        return File(context.filesDir, "classifiers").listFiles()
    }
}
