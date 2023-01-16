package org.rfcx.companion.util.audiocoverage

import com.google.gson.JsonObject
import org.rfcx.companion.view.deployment.guardian.storage.HeatmapItem
import java.util.*

object AudioCoverageUtils {

    fun toDateTimeStructure(listOfArchived: List<Long>): JsonObject {
        if (listOfArchived.isEmpty()) return JsonObject()

        val firstTimestamp = listOfArchived.first()
        val lastTimestamp = listOfArchived.last()

        val firstCal = Calendar.getInstance()
        firstCal.time = Date(firstTimestamp)

        val lastCal = Calendar.getInstance()
        lastCal.time = Date(lastTimestamp)

        val tree = JsonObject()
        // create all years
        for (yr in firstCal.get(Calendar.YEAR)..lastCal.get(Calendar.YEAR)) {
            tree.add(yr.toString(), JsonObject())
        }

        // create all months
        tree.keySet().forEach {
            for (mth in 1..12) {
                tree.get(it).asJsonObject.add(mth.toString(), JsonObject())
            }
        }

        // create all days
        tree.keySet().forEach { year ->
            tree.get(year).asJsonObject.keySet().forEach { month ->
                var day = 31
                if (month.toInt() == 2) {
                    day = 28
                } else if (listOf(4, 6, 9, 11).contains(month.toInt())) {
                    day = 30
                }
                for (d in 1..day) {
                    tree.get(year).asJsonObject.get(month).asJsonObject.add(
                        d.toString(),
                        JsonObject()
                    )
                }
            }
        }

        // create all hours
        tree.keySet().forEach { year ->
            tree.get(year).asJsonObject.keySet().forEach { month ->
                tree.get(year).asJsonObject.get(month).asJsonObject.keySet().forEach { day ->
                    for (hour in 0..23) {
                        tree.get(year).asJsonObject.get(month).asJsonObject.get(day).asJsonObject.addProperty(
                            hour.toString(),
                            0
                        )
                    }
                }
            }
        }

        listOfArchived.forEach { time ->
            val cal = Calendar.getInstance()
            cal.time = Date(time)
            val year = cal.get(Calendar.YEAR).toString()
            val month = (cal.get(Calendar.MONTH) + 1).toString()
            val day = cal.get(Calendar.DAY_OF_MONTH).toString()
            val hour = cal.get(Calendar.HOUR_OF_DAY).toString()

            var currentAmount = tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day).get(hour).asInt
            tree.getAsJsonObject(year).getAsJsonObject(month).getAsJsonObject(day).addProperty(hour, ++currentAmount)
        }

        return tree
    }

    fun filterByMonthYear(item: JsonObject, month: Int, year: Int): List<HeatmapItem> {
        val obj = item.getAsJsonObject(year.toString()).getAsJsonObject((month + 1).toString())

        val heatmapItems = arrayListOf<HeatmapItem>()
        obj.keySet().forEach { day ->
            heatmapItems.add(HeatmapItem.YAxis(day))
            obj.getAsJsonObject(day.toString()).keySet().forEach { hour ->
                val value = obj.getAsJsonObject(day.toString()).get(hour.toString()).asInt
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

    fun getMinMaxYear(item: List<Long>): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        var min = cal.get(Calendar.YEAR)
        var max = cal.get(Calendar.YEAR)
        if (item.isNotEmpty()) {
            cal.time = Date(item.first())
            min = cal.get(Calendar.YEAR)

            cal.time = Date(item.last())
            max = cal.get(Calendar.YEAR)
        }
        return Pair(min, max)
    }
}
