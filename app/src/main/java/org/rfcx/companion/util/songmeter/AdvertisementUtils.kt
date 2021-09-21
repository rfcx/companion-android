package org.rfcx.companion.util.songmeter

class AdvertisementUtils {

    private val manufacturingCompanyIndexes = listOf(5, 6)
    private val beaconIdIndex = 7
    private val serialNumberIndexes = listOf(8, 10)
    private val prefixIndexed = listOf(11, 15)
    private var telemetryPrefixes: String? = null
    private var timeRecordingPrefixes: String? = null
    private var fullPrefixes: String? = null
    private var serialNumber: String? = null

    private enum class PayloadType(val id: Int) { Telemetry(0), TimeRecording(1) }

    fun getSerialNumber() = serialNumber

    fun getPrefixes() = fullPrefixes

    fun convertAdvertisementToObject(advertisement: ByteArray) {
        val beaconIdByte = advertisement[beaconIdIndex]
        val beaconIdBinary = beaconIdByte.toBinaryString()
        val payloadType = getPayloadType(beaconIdBinary)

        val prefixesBytes = advertisement.copyOfRange(prefixIndexed[0], prefixIndexed[1] + 1)
        when (payloadType.id) {
            PayloadType.Telemetry.id -> {
                telemetryPrefixes = binaryToPrefixes(prefixesBytes.toBinaryString())
            }
            PayloadType.TimeRecording.id -> {
                timeRecordingPrefixes = binaryToPrefixes(prefixesBytes.toBinaryString())
            }
        }
        if (!telemetryPrefixes.isNullOrBlank() && !timeRecordingPrefixes.isNullOrBlank()) {
            fullPrefixes = telemetryPrefixes + timeRecordingPrefixes
        }

        val serialNumberBytes =
            advertisement.copyOfRange(serialNumberIndexes[0], serialNumberIndexes[1] + 1)
        serialNumber = bytesToSerialNumber(serialNumberBytes)
    }

    /**
     * SerialNumber are hex strings of bytes
     */
    private fun bytesToSerialNumber(snBytes: ByteArray): String {
        return snBytes.toHexString().substring(1)
    }

    /**
     * Prefixes can be converted from binary values to character
     *    NULL   = 0
     * "A" - "Z" = 1-26
     * "0" - "9" = 27-36
     *    "-"    = 37
     */
    private fun binaryToPrefixes(prefixesBinary: String): String {
        return PrefixesMapper.toPrefixesString(
            prefixesBinary.substring(0, prefixesBinary.length - 4).chunked(6)
                .map { Integer.parseInt(it, 2) })
    }

    /**
     * PayloadType defined by first index of binary String (7th bit)
     * 0 = Telemetry
     * 1 = Time Recording
     */
    private fun getPayloadType(beaconIdBinary: String): PayloadType {
        val type = beaconIdBinary[0].toString().toInt()
        return PayloadType.values()[type]
    }

    fun Byte.toBinaryString() = String.format("%8s", Integer.toBinaryString((this + 256) % 256))
        .replace(' ', '0')

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    fun ByteArray.toBinaryString() = joinToString("") { it.toBinaryString() }

    fun Byte.toHexString() = "%02x".format(this)
}
