package org.rfcx.companion.util.geojson

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object GeoJsonUtils {

    fun generateGeoJson(points: List<DoubleArray>) {
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
        json.add("features", Gson().toJsonTree(features).asJsonArray)

    }

    private fun DoubleArray.toJsonArray(): JsonArray {
        val jsonArray = JsonArray()
        this.forEach {
            jsonArray.add(it)
        }
        return jsonArray
    }
}
