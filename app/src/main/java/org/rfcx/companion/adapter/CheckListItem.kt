package org.rfcx.companion.adapter

sealed class CheckListItem {
    data class CheckItem(val number: Int, val name: String, var isPassed: Boolean = false, var isRequired: Boolean = true) : CheckListItem()
    data class Header(val name: String) : CheckListItem()
}
