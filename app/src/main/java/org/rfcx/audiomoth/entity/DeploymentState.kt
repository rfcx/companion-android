package org.rfcx.audiomoth.entity

sealed class DeploymentState {
    enum class Edge(val key: Int) {
        Locate(1), Config(2), Sync(3), Verify(4), Deploy(5), ReadyToUpload(6);

        companion object {
            private val map = values().associateBy(DeploymentState.Edge::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }

    enum class Guardian(val key: Int){
        Connect(1), Locate(2), Config(3), Signal(4), Microphone(5), Checkin(6), Deploy(7), ReadyToUpload(8);

        companion object {
            private val map = values().associateBy(DeploymentState.Guardian::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }
}
