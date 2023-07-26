package org.rfcx.companion.entity

sealed class DeploymentState {
    enum class AudioMoth(val key: Int) {
        Locate(1), Sync(2), Deploy(3), ReadyToUpload(11);

        companion object {
            private val map = values().associateBy(DeploymentState.AudioMoth::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }

    enum class SongMeter(val key: Int) {
        Locate(1), Sync(2), Deploy(3), ReadyToUpload(11);

        companion object {
            private val map = AudioMoth.values().associateBy(DeploymentState.AudioMoth::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }
}
