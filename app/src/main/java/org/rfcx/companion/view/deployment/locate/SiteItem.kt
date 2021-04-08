package org.rfcx.companion.view.deployment.locate

import org.rfcx.companion.entity.Locate
import java.util.*

data class SiteItem(val locate: Locate = Locate(), val distance: Float = 0F)

data class SiteWithLastDeploymentItem(val locate: Locate = Locate(), val date: Date? = null, val distance: Float? = 0F)
