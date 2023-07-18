package org.rfcx.companion.util.geojson

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import okhttp3.ResponseBody
import org.rfcx.companion.entity.FeatureCollection
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.toISO8601Format
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream
import java.util.*

object GeoJsonUtils {

    fun generateFileName(deployedAt: Date, deploymentId: String): String {
        return "${deployedAt.toISO8601Format()}-$deploymentId.json"
    }

    fun generateGeoJson(context: Context, fileName: String, points: List<DoubleArray>): File {
        val gson = Gson()
        val json = JsonObject()
        // add Type
        json.addProperty("type", "FeatureCollection")

        // create features
        val featureArray = JsonArray()

        val featureItem = JsonObject()
        featureItem.addProperty("type", "Feature")

        val propertyItem = JsonObject()
        propertyItem.addProperty("color", randomColor())
        featureItem.add("properties", propertyItem)

        // create Geometry type
        val geometry = JsonObject()
        geometry.addProperty("type", "LineString")
        // create Geometry coordinate
        geometry.add("coordinates", points.toJsonArray())
        featureItem.add("geometry", geometry)

        featureArray.add(featureItem)

        // combine all data
        json.add("features", gson.toJsonTree(featureArray).asJsonArray)

        // write to file
        return createFile(context, fileName, json)
    }

    private fun createFile(context: Context, fileName: String, json: JsonObject): File {
        val gson = Gson()
        val dir = File(context.filesDir, "deployment-tracking")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val file = File(dir, fileName)
        val writer = FileWriter(file)
        gson.toJson(json, writer)

        // close writer
        writer.close()
        return file
    }

    private fun createFile(context: Context, fileName: String, inputStream: InputStream): String {
        val dir = File(context.filesDir, "deployment-tracking")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val file = File(dir, fileName)
        try {
            val fos = FileOutputStream(file)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } catch (e: Exception) {
            Log.e("GeoJsonUtils-createFile", e.toString())
        } finally {
            inputStream.close()
            return file.absolutePath
        }
    }

    fun downloadGeoJsonFile(
        context: Context,
        asset: DeploymentAssetResponse,
        deploymentId: String,
        deployedAt: Date,
        callback: DownloadTrackCallback
    ) {
        ApiManager.getInstance().getDeviceApi(context).getGeoJsonFile(asset.id)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    response.body()?.byteStream()?.let {
                        val path =
                            createFile(context, generateFileName(deployedAt, deploymentId), it)
                        try {
                            val json = File(path).readText()
                            Gson().fromJson(json, FeatureCollection::class.java)
                            callback.onSuccess(path)
                        } catch (e: JsonSyntaxException) {
                            callback.onFailed(e.message ?: "error occur")
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback.onFailed(t.message ?: "error occur")
                }
            })
    }

    private fun List<DoubleArray>.toJsonArray(): JsonArray {
        val jsonArray = JsonArray()
        this.forEach { dbArray ->
            val tempJsonArray = JsonArray()
            dbArray.forEach { db ->
                tempJsonArray.add(db)
            }
            jsonArray.add(tempJsonArray)
        }
        return jsonArray
    }

    private fun randomColor(): String {
        val rnd = Random()
        val color = rnd.nextInt(0xffffff + 1)
        return String.format("#%06x", color).toUpperCase(Locale.getDefault())
    }

    interface DownloadTrackCallback {
        fun onSuccess(filePath: String)
        fun onFailed(msg: String)
    }
}
