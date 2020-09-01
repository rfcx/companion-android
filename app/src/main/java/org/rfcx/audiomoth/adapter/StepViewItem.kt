package org.rfcx.audiomoth.adapter

sealed class StepViewItem {
    data class StepItem(val number: Int, val name: String, var isPassed: Boolean = false, var isSelected: Boolean = false, var canSkip: Boolean = false) : StepViewItem()

    data class DividerItem(val number: Int, var isPassed: Boolean = false) : StepViewItem()
}
