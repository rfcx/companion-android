package org.rfcx.companion.entity

sealed class DeploymentState {
    enum class AudioMoth(val key: Int) {
        Locate(1), Sync(2), Deploy(3), ReadyToUpload(4);

        companion object {
            private val map = values().associateBy(DeploymentState.AudioMoth::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }

    enum class Guardian(val key: Int) {
        Connect(1), Register(2), Locate(3), Config(4), SolarPanel(5), Signal(6), Microphone(7), Checkin(8), Deploy(9), Advanced(10), ReadyToUpload(11);

        companion object {
            private val map = values().associateBy(DeploymentState.Guardian::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }
}
