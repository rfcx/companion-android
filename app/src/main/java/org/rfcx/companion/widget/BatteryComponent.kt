package org.rfcx.companion.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.*
import kotlinx.android.synthetic.main.widget_battery_item.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.socket.BatteryLevel
import org.rfcx.companion.util.getIntColor

class BatteryComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var levelBatteryView: View
    private var batteryPositiveView: View
    private val constraintSet = ConstraintSet()

    init {
        View.inflate(context, R.layout.widget_battery_item, this)
        levelBatteryView = findViewById(R.id.levelBatteryView)
        batteryPositiveView = findViewById(R.id.batteryPositiveView)
        initAttrs(attrs)
    }

    var levelBattery: Int? = null
        set(value) {
            field = value
            levelBattery?.let {
                setConstraintSet(it)
                setColorOfBattery(it)
            }
        }

    private fun setConstraintSet(level: Int) {
        constraintSet.clone(constraintLayout)

        val endOf = when (level) {
            BatteryLevel.BatteryLevel1.key, BatteryLevel.BatteryDepleted.key, BatteryLevel.BatteryLevelLessThanDay.key -> R.id.guidelineVertical1
            BatteryLevel.BatteryLevel2.key -> R.id.guidelineVertical2
            BatteryLevel.BatteryLevel3.key -> R.id.guidelineVertical3
            BatteryLevel.BatteryLevel4.key -> R.id.guidelineVertical4
            BatteryLevel.BatteryLevel5.key -> R.id.guidelineVertical5
            BatteryLevel.BatteryLevel6.key -> R.id.guidelineVertical6
            BatteryLevel.BatteryLevel7.key -> R.id.guidelineVertical7
            else -> R.id.guidelineVertical8
        }

        // level battery view constraint start to start of parent
        constraintSet.connect(levelBatteryView.id, START, PARENT_ID, START)

        // level battery view constraint top to top of parent
        constraintSet.connect(levelBatteryView.id, TOP, PARENT_ID, TOP)

        // level battery view constraint bottom to bottom of parent
        constraintSet.connect(levelBatteryView.id, BOTTOM, PARENT_ID, BOTTOM)

        // level battery view constraint end to end of parent
        constraintSet.connect(levelBatteryView.id, END, endOf, END)

        // finally, apply the constraint set to layout
        constraintSet.applyTo(constraintLayout)
    }

    private fun setColorOfBattery(level: Int) {
        if (level == BatteryLevel.BatteryDepleted.key || level == BatteryLevel.BatteryLevelLessThanDay.key) {
            levelBatteryView.setBackgroundColor(context.getIntColor(R.color.text_error))
        } else {
            levelBatteryView.setBackgroundColor(context.getIntColor(R.color.colorPrimary))
        }

        if (level == BatteryLevel.BatteryLevel8.key) {
            batteryPositiveView.setBackgroundColor(context.getIntColor(R.color.colorPrimary))
        } else {
            batteryPositiveView.setBackgroundColor(context.getIntColor(R.color.gray_30))
        }
    }

    private fun initAttrs(attrs: AttributeSet?) {
        if (attrs == null) return
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BatteryComponent)
        levelBattery = typedArray.getInt(R.styleable.BatteryComponent_batteryLevel, 0)
        typedArray.recycle()
    }
}
