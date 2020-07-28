package org.rfcx.audiomoth.adapter

data class LocalImageItem(var imageId: Int, val localPath: String, val canDelete: Boolean) : BaseListItem {
    override fun getItemId(): Int = imageId
}
