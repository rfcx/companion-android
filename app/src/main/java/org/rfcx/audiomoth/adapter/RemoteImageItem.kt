package org.rfcx.audiomoth.adapter

data class RemoteImageItem(var imageId: Int, val remotePath: String, val canDelete: Boolean) : BaseListItem {
    override fun getItemId(): Int = imageId
}
