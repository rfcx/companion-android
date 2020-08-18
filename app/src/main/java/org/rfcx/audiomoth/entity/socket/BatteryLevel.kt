package org.rfcx.audiomoth.entity.socket

enum class BatteryLevel(val key: Int) {
    BatteryLevel1(1),
    BatteryLevel2(2),
    BatteryLevel3(3),
    BatteryLevel4(4),
    BatteryLevel5(5),
    BatteryLevel6(6),
    BatteryLevel7(7),
    BatteryLevel8(8),
    BatteryLevelLessThanDay(-1),
    BatteryDepleted(0),
}