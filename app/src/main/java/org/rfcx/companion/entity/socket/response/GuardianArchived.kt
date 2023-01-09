package org.rfcx.companion.entity.socket.response

data class GuardianArchived(
    val archivedStart: Long,
    val archivedEnd: Long,
    val count: Int,
    val duration: Int,
    val skipping: Int
) {
    fun toListOfTimestamp(): List<Long> {
        val timestamps = arrayListOf<Long>()
        for (i in 0 until count) {
            timestamps.add(archivedStart + (60000 * i))
        }
        return timestamps
    }
}
