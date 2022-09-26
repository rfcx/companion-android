package org.rfcx.companion.util.songmeter

private val characterTable = mapOf(
    0 to null,
    1 to "A",
    2 to "B",
    3 to "C",
    4 to "D",
    5 to "E",
    6 to "F",
    7 to "G",
    8 to "H",
    9 to "I",
    10 to "J",
    11 to "K",
    12 to "L",
    13 to "M",
    14 to "N",
    15 to "O",
    16 to "P",
    17 to "Q",
    18 to "R",
    19 to "S",
    20 to "T",
    21 to "U",
    22 to "V",
    23 to "W",
    24 to "X",
    25 to "Y",
    26 to "Z",
    27 to "0",
    28 to "1",
    29 to "2",
    30 to "3",
    31 to "4",
    32 to "5",
    33 to "6",
    34 to "7",
    35 to "8",
    36 to "9",
    37 to "-"
)

object PrefixesMapper {
    fun toPrefixesString(binaries: List<Int>): String {
        return binaries.mapNotNull { characterTable[it] }.joinToString("")
    }
}
