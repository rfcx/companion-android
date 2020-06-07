package org.rfcx.audiomoth.entity

enum class DeploymentState(val key: Int) {
    Locate(1), Config(2), Sync(3), Verify(4), Deploy(5), ReadyToUpload(6);

    companion object {
        private val map = DeploymentState.values().associateBy(DeploymentState::key)
        fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
    }
}