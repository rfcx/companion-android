package org.rfcx.companion.util.audiocoverage

import com.google.gson.JsonObject
import org.rfcx.companion.view.deployment.guardian.storage.HeatmapItem
import java.util.*
import kotlin.collections.HashMap

object AudioCoverageUtils {

    fun toDateTimeStructure(listOfArchived: List<Long>): JsonObject {
        if (listOfArchived.isEmpty()) return JsonObject()

        val tree = JsonObject()
        listOfArchived.forEach { time ->
            val cal = Calendar.getInstance()
            cal.time = Date(time)
            val year = cal.get(Calendar.YEAR).toString()
            val month = (cal.get(Calendar.MONTH)).toString()
            val day = cal.get(Calendar.DAY_OF_MONTH).toString()
            val hour = cal.get(Calendar.HOUR_OF_DAY).toString()

            if (!tree.has(year)) {
                tree.add(year, JsonObject())
            }
            if (!tree.getAsJsonObject(year).has(month)) {
                tree.getAsJsonObject(year).add(month, JsonObject())
            }
            if (!tree.getAsJsonObject(year).getAsJsonObject(month).has(day)) {
                tree.getAsJsonObject(year).getAsJsonObject(month).add(day, JsonObject())
            }
            if (!tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day).has(hour)) {
                tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day).addProperty(hour, 0)
            }
            var currentAmount =
                tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day)
                    .get(hour).asInt
            tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day)
                .addProperty(hour, ++currentAmount)
        }

        return tree
    }

    fun filterByMonthYear(item: JsonObject, month: Int, year: Int): List<HeatmapItem> {
        val obj = item.getAsJsonObject(year.toString()).getAsJsonObject((month).toString())

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        val day = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val heatmapItems = arrayListOf<HeatmapItem>()
        for (d in 1..day) {
            heatmapItems.add(HeatmapItem.YAxis(d.toString()))
            for (h in 0..23) {
                var value = 0
                if (obj.has(d.toString())) {
                    if (obj.getAsJsonObject(d.toString()).has(h.toString())) {
                        value = obj.getAsJsonObject(d.toString()).get(h.toString()).asInt
                    }
                }
                heatmapItems.add(HeatmapItem.Normal(value))
            }
        }
        return heatmapItems
    }

    fun getLatestMonthYear(item: List<Long>): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        if (item.isNotEmpty()) {
            cal.time = Date(item.last())
        }
        return Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun getAvailableMonths(item: JsonObject): HashMap<Int, List<Int>> {
        val map = hashMapOf<Int, List<Int>>()
        item.keySet().forEach { year ->
            val months = arrayListOf<Int>()
            item.getAsJsonObject(year).keySet().forEach { month ->
                months.add(month.toInt())
            }
            map[year.toInt()] = months
        }
        return map
    }
}
