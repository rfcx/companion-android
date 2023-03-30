package org.rfcx.companion.util.songmeter

class AdvertisementUtils {

    private val manufacturingCompanyIndexes = listOf(5, 6)
    private val readyToPairIndex = 3
    private val beaconIdIndex = 7
    private val serialNumberIndexes = listOf(8, 10)
    private val serialNumberIndexesWhenPair = listOf(7, 9)
    private val prefixIndexed = listOf(11, 15)
    private var telemetryPrefixes: String? = null
    private var timeRecordingPrefixes: String? = null
    private var fullPrefixes: String? = null
    private var serialNumber: String? = null
    private var readyToPair: Boolean = false

    private enum class PayloadType(val id: Int) { Telemetry(0), TimeRecording(1) }

    fun getSerialNumber() = serialNumber

    fun getPrefixes() = fullPrefixes

    fun getReadyToPair() = readyToPair

    fun clear() {
        serialNumber = null
        fullPrefixes = null
        readyToPair = false
        telemetryPrefixes = null
        timeRecordingPrefixes = null
    }

    fun convertAdvertisementToObject(advertisement: ByteArray) {
        val beaconIdByte = advertisement[beaconIdIndex]
        val beaconIdBinary = beaconIdByte.toBinaryString()
        val payloadType = getPayloadType(beaconIdBinary)

        val prefixesBytes = advertisement.copyOfRange(prefixIndexed[0], prefixIndexed[1] + 1)
        val readyToPairBytes = advertisement[readyToPairIndex]
        readyToPair = getIsReadyToPair(readyToPairBytes.toBinaryString())
        when (payloadType.id) {
            PayloadType.Telemetry.id -> {
                if (!readyToPair) telemetryPrefixes = binaryToPrefixes(prefixesBytes.toBinaryString())
            }
            PayloadType.TimeRecording.id -> {
                if (!readyToPair) timeRecordingPrefixes = binaryToPrefixes(prefixesBytes.toBinaryString())
            }
        }
        if (telemetryPrefixes != null && timeRecordingPrefixes != null) {
            fullPrefixes = telemetryPrefixes + timeRecordingPrefixes
        }

        val serialNumberBytes = if (readyToPair) {
            advertisement.copyOfRange(serialNumberIndexesWhenPair[0], serialNumberIndexesWhenPair[1] + 1)
        } else {
            advertisement.copyOfRange(serialNumberIndexes[0], serialNumberIndexes[1] + 1)
        }
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
                .map { Integer.parseInt(it, 2) }
        )
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

    /**
     * Is ready to pair defined by first index of binary String (3rd bit)
     * 00011011 = not ready
     * 00001111 = ready
     */
    private fun getIsReadyToPair(pairBinary: String): Boolean {
        if (pairBinary == "00011011") return false
        else if (pairBinary == "00001111") return true
        return false
    }

    fun Byte.toBinaryString() = String.format("%8s", Integer.toBinaryString((this + 256) % 256))
        .replace(' ', '0')

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    fun ByteArray.toBinaryString() = joinToString("") { it.toBinaryString() }

    fun Byte.toHexString() = "%02x".format(this)
}
