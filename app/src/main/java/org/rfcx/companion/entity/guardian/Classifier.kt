package org.rfcx.companion.entity.guardian

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class Classifier(
    @PrimaryKey
    var id: String = "",
    var name: String = "",
    var version: String = "",
    var path: String = "",
    var type: String = "tflite",
    var sha1: String = "",
    var sampleRate: String = "",
    var inputGain: String = "",
    var windowSize: String = "",
    var stepSize: String = "",
    var classifications: String = "",
    var classificationsFilterThreshold: String = ""
) : RealmModel {

    fun toGuardianClassifier(): String {
        val gson = Gson()

        val subObj = JsonObject()
        subObj.addProperty("classifier_name", name)
        subObj.addProperty("classifier_version", version)
        subObj.addProperty("sample_rate", sampleRate)
        subObj.addProperty("input_gain", inputGain)
        subObj.addProperty("window_size", windowSize)
        subObj.addProperty("step_size", stepSize)
        subObj.addProperty("classifications", classifications)
        subObj.addProperty("classifications_filter_threshold", classificationsFilterThreshold)

        val obj = JsonObject()
        obj.addProperty("asset_id", id)
        obj.addProperty("file_type", type)
        obj.addProperty("checksum", sha1)
        obj.addProperty("meta_json_blob", gson.toJson(subObj))
        return gson.toJson(obj)
    }

    companion object {
        const val TABLE_NAME = "Classifier"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_VERSION = "version"
        const val FIELD_PATH = "path"
        const val FIELD_SHA1 = "sha1"
        const val FIELD_SAMPLE_RATE = "sampleRate"
        const val FIELD_INPUT_GAIN = "inputGain"
        const val FIELD_WINDOW_SIZE = "windowSize"
        const val FIELD_STEP_SIZE = "stepSize"
        const val FIELD_CLASSIFICATIONS = "classifications"
        const val FIELD_CLASSIFICATION_FILTER_THRESHOLD = "classificationsFilterThreshold"
    }
}
