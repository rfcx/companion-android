package org.rfcx.companion.entity.socket.response

data class SentinelResponse(
    val sentinel: SentinelData = SentinelData()
) : SocketResposne

data class SentinelData(
    val input: String = "input*0*0*0*0*0",
    val system: String = "system*0*0*0*0*0",
    val battery: String = "battery*0*0*0*0*0"
) {
    fun convertToInfo(): SentinelInfo {
        val inputData = input.split("*")
        val systemData = system.split("*")
        val batteryData = battery.split("*")
        return SentinelInfo(
            SentinelInput(inputData[2].toInt(), inputData[3].toInt(), inputData[4].toInt(), inputData[5].toInt()),
            SentinelSystem(systemData[2].toInt(), systemData[3].toInt(), systemData[4].toInt(), systemData[5].toInt()),
            SentinelBattery(batteryData[2].toInt(), batteryData[3].toInt(), batteryData[4].toDouble(), batteryData[5].toInt())
            )
    }
}

data class SentinelInfo(
    val input: SentinelInput = SentinelInput(),
    val system: SentinelSystem = SentinelSystem(),
    val battery: SentinelBattery = SentinelBattery()
)

data class SentinelInput(
    val voltage: Int = 0,
    val current: Int = 0,
    val misc: Int = 0,
    val power: Int = 0
)

data class SentinelSystem(
    val voltage: Int = 0,
    val current: Int = 0,
    val temp: Int = 0,
    val power: Int = 0
)

data class SentinelBattery(
    val voltage: Int = 0,
    val current: Int = 0,
    val percentage: Double = 0.0,
    val power: Int = 0
)
