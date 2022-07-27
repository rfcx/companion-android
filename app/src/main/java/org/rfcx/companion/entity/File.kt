package org.rfcx.companion.entity

import org.rfcx.companion.entity.response.FileResponse
import org.rfcx.companion.util.file.FileStatus

data class File(
    val file: FileResponse,
    val status: FileStatus
)
