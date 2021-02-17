package org.rfcx.companion.entity.request

data class EditDeploymentRequest(
    var stream: StreamRequest? = null,
    var project: ProjectRequest? = null
)
