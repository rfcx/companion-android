package org.rfcx.audiomoth.entity

open class Image(
    var id: Int = 0,
    var reportId: Int = 0,
    var guid: String? = null,
    var localPath: String = "",
    var syncState: Int = 0,
    var remotePath: String? = null
)