package org.rfcx.companion.widget

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import org.rfcx.companion.R

class BottomNavigationMenuItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var iconImageView: AppCompatImageView
    private var titleTextView: AppCompatTextView
    private var badgeGroup: Group
    private var countTextView: AppCompatTextView
    private var circleImageView: AppCompatImageView

    init {
        View.inflate(context, R.layout.widget_bottom_navigation_menu_item, this)
        iconImageView = findViewById(R.id.iconImageView)
        titleTextView = findViewById(R.id.menuTitleTextView)
        badgeGroup = findViewById(R.id.badgeGroup)
        countTextView = findViewById(R.id.countTextView)
        circleImageView = findViewById(R.id.badgeImageView)
        initAttrs(attrs)
    }

    var title: CharSequence? = null
        set(value) {
            field = value
            titleTextView.text = value
        }

    var titleColor: Int = 0
        set(value) {
            field = value
            if (value == 0 || menuSelected) return
            titleTextView.setTextColor(value)
        }

    var titleColorSelected: Int = 0

    var icon: Drawable? = null
        set(value) {
            field = value
            iconImageView.setImageDrawable(field)
        }

    var iconTintColor: Int = 0

    var menuSelected: Boolean = false
        set(value) {
            field = value
            menuSelected(value)
        }

    var badgeNumber: Int = 0
        set(value) {
            field = value
            updateBadge()
        }

    var isShowBadge: Boolean = false
        set(value) {
            field = value
            updateBadge()
        }

    private fun updateBadge() {
        val displayValue = if (badgeNumber > 99) "99+" else badgeNumber.toString()
        countTextView.text = displayValue
        badgeGroup.visibility = if (isShowBadge && (badgeNumber > 0) && !menuSelected) View.VISIBLE else View.GONE
    }

    private fun menuSelected(selected: Boolean) {
        if (selected) {
            badgeGroup.visibility = View.GONE
            titleTextView.visibility = View.VISIBLE
            titleTextView.setTextColor(titleColorSelected)
            if (iconTintColor != 0)
                iconImageView.setColorFilter(titleColorSelected,
                    PorterDuff.Mode.SRC_IN)
        } else {
            titleTextView.visibility = View.VISIBLE
            titleTextView.setTextColor(titleColor)
            iconImageView.colorFilter = null
            updateBadge()
        }
    }

    private fun initAttrs(attrs: AttributeSet?) {
        if (attrs == null) return
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationMenuItem)

        title = typedArray.getString(R.styleable.BottomNavigationMenuItem_title)
        titleColor = ContextCompat.getColor(context,
            typedArray.getResourceId(R.styleable.BottomNavigationMenuItem_titleColor, R.color.grey_active))
        titleColorSelected = ContextCompat.getColor(context,
            typedArray.getResourceId(R.styleable.BottomNavigationMenuItem_titleColorSelected, R.color.grey_active))

        icon = typedArray.getDrawable(R.styleable.BottomNavigationMenuItem_icon)
        iconTintColor = ContextCompat.getColor(context,
            typedArray.getResourceId(R.styleable.BottomNavigationMenuItem_iconTintColor, android.R.color.transparent))
        menuSelected = typedArray.getBoolean(R.styleable.BottomNavigationMenuItem_menuSelected, false)
        isShowBadge = typedArray.getBoolean(R.styleable.BottomNavigationMenuItem_showBadge, false)

        typedArray.recycle()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val saveState = BottomNavigationMenuItemSaveState(superState)
        saveState.isSelected = this.menuSelected
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is BottomNavigationMenuItemSaveState) {
            super.onRestoreInstanceState(state)
            return
        } else {
            super.onRestoreInstanceState(state.superState)
            this.menuSelected = state.isSelected
        }
    }

    private class BottomNavigationMenuItemSaveState : BaseSavedState {

        var isSelected = false

        constructor(source: Parcel) : super(source) {
            isSelected = source.readByte() != 0.toByte()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeByte(if (isSelected) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<BottomNavigationMenuItemSaveState> {
            override fun createFromParcel(parcel: Parcel): BottomNavigationMenuItemSaveState {
                return BottomNavigationMenuItemSaveState(parcel)
            }

            override fun newArray(size: Int): Array<BottomNavigationMenuItemSaveState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
