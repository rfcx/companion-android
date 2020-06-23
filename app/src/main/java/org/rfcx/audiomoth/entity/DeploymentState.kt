package org.rfcx.audiomoth.entity


sealed class DeploymentState {
    enum class AudioMoth(val key: Int) {
        Locate(1), Config(2), Sync(3), Verify(4), Deploy(5), ReadyToUpload(6);

        companion object {
            private val map = values().associateBy(DeploymentState.AudioMoth::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }

    enum class Guardian(val key: Int){
        Connect(1), Locate(2), Config(3), Sync(4), Deploy(5), ReadyToUpload(6);

        companion object {
            private val map = values().associateBy(DeploymentState.Guardian::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }
}
