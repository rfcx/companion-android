package org.rfcx.companion

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.rfcx.companion.entity.socket.response.GuardianArchivedCoverage
import org.rfcx.companion.util.audiocoverage.AudioCoverageUtils

class AudioCoverageUtilsTest {
    @Test
    fun canExtractListOfTimestampToStructure() {
        val listOfTimestamp = listOf(
            GuardianArchivedCoverage(60, listOf(1673263960000L))
        )

        val result = AudioCoverageUtils.toDateTimeStructure(listOfTimestamp)

        assertEquals(result.getAsJsonObject("2023").getAsJsonObject("0").getAsJsonObject("9").get("18").asInt, 1)
    }
}
