package org.rfcx.companion.util.geojson

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.io.FileWriter
import java.util.*

object GeoJsonUtils {

    fun generateFileName(deployedAt: Date, deploymentId: String): String {
        return "$deployedAt-$deploymentId.json"
    }

    fun generateGeoJson(context: Context, fileName: String, points: List<DoubleArray>): File {
        val gson = Gson()
        val json = JsonObject()
        //add Type
        json.addProperty("type", "FeatureCollection")

        //create features
        val tempJson = JsonObject()
        tempJson.addProperty("type", "Feature")

        //create Geometry type
        val geometry = JsonObject()
        geometry.addProperty("type", "LineString")
        //create Geometry coordinate
        geometry.add("coordinates", points.toJsonArray())

        tempJson.add("geometry", geometry)

        //combine all data
        json.add("features", gson.toJsonTree(tempJson))

        //write to file
        val dir = File(context.filesDir, "deployment-tracking")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val file = File(dir, fileName)
        val writer = FileWriter(file)
        gson.toJson(json, writer)

        //close writer
        writer.close()
        return file
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
}
