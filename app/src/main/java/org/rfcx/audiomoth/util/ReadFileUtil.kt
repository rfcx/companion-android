package org.rfcx.audiomoth.util

import android.content.Context
import androidx.annotation.RawRes
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type

class ReadFileUtil<T> {

    private fun readRawFile(context: Context, @RawRes rawRes: Int): String {
        val inputStream = context.resources.openRawResource(rawRes)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val result = StringBuilder()
        try {
            var line: String? = null
            while ({ line = bufferedReader.readLine(); line }() != null) {
                result.append(line)
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result.toString()
    }

    fun parseRawJson(context: Context, @RawRes rawJsonRes: Int, type: Type, typeAdapter: T?): T? {
        val json = readRawFile(context, rawJsonRes)
        val gsonBuilder = GsonBuilder()
        if (typeAdapter != null) {
            gsonBuilder.registerTypeAdapter(type, typeAdapter)
        }
        val gson = gsonBuilder.create()
        return gson.fromJson(json, type)
    }
}