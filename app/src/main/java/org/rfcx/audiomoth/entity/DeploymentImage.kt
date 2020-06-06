package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class DeploymentImage(
    @PrimaryKey
    var id: Int = 0,
    var deploymentId: Int = 0,
    @Expose(serialize = false)
    var localPath: String = "",
    var remotePath: String? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel