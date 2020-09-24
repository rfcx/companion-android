package org.rfcx.audiomoth.entity

sealed class DeploymentState {
    enum class Edge(val key: Int) {
        Locate(1), Config(2), Sync(3), Verify(4), Deploy(5), ReadyToUpload(6);

        companion object {
            private val map = values().associateBy(DeploymentState.Edge::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }

    enum class Guardian(val key: Int) {
        Connect(1), Register(2), Locate(3), Config(4), SolarPanel(5), Signal(6), Microphone(7), Checkin(8), Deploy(9), ReadyToUpload(10);

        companion object {
            private val map = values().associateBy(DeploymentState.Guardian::key)
            fun fromInt(ageEstimate: Int) = map[ageEstimate] ?: Locate
        }
    }
}
