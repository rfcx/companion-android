package org.rfcx.companion.util.geojson

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.io.FileWriter

object GeoJsonUtils {

    fun generateGeoJson(context: Context, fileName: String, points: List<DoubleArray>) {
        val gson = Gson()
        val json = JsonObject()
        //add Type
        json.addProperty("type", "FeatureCollection")

        //create features
        val features = points.map {
            val tempJson = JsonObject()
            tempJson.addProperty("type", "Feature")

            //create Geometry type
            val geometry = JsonObject()
            geometry.addProperty("type", "Point")
            //create Geometry coordinate
            geometry.add("coordinates", it.toJsonArray())

            tempJson.add("geometry", geometry)
            tempJson
        }

        //combine all data
        json.add("features", gson.toJsonTree(features).asJsonArray)

        //write to file
        val dir = File(context.filesDir, "deployment-tracking")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val file = File(dir, fileName)
        val writer = FileWriter(file)
        gson.toJson(json, writer)

        //close
        writer.close()
    }

    private fun DoubleArray.toJsonArray(): JsonArray {
        val jsonArray = JsonArray()
        this.forEach {
            jsonArray.add(it)
        }
        return jsonArray
    }
}
