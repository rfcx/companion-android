package org.rfcx.companion.util.file

import android.content.Context
import org.rfcx.companion.entity.socket.Classifier
import java.io.File

object ClassifierUtils {

    fun getAllDownloadedClassifiers(context: Context): List<Classifier> {
        val classifiers = mutableListOf<Classifier>()
        val downloadedClassifiers = File(context.filesDir, "guardian-classifier").listFiles()
        downloadedClassifiers?.forEach {
            classifiers.add(Classifier(it.name, it.absolutePath))
        }
        return classifiers
    }

}
