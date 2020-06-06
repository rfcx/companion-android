package org.rfcx.audiomoth.entity

enum class DeploymentState(val key: Int) {
    locate(1), config(2), sync(3), verify(4), deploy(5)
}