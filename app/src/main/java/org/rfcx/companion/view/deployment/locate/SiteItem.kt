package org.rfcx.companion.view.deployment.locate

import org.rfcx.companion.entity.Stream
import java.util.*

data class SiteItem(val stream: Stream = Stream(), val distance: Float = 0F)

data class SiteWithLastDeploymentItem(val stream: Stream = Stream(), val date: Date? = null, val distance: Float? = 0F)
