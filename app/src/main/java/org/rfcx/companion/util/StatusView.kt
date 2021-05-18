package org.rfcx.companion.util
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.widget_status_view.view.*
import org.rfcx.companion.R

class StatusView : FrameLayout {
    private var slideOut: Animation? = null
    private var slideIn: Animation? = null
    private var theme: Int = 1 // normal

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet) {
        val typedArray = context.theme
            .obtainStyledAttributes(attrs, R.styleable.StatusView, 0, 0)

        // Get attribute values
        theme = typedArray.getInt(R.styleable.StatusView_status_theme, 1)

        typedArray.recycle()

        slideIn = AnimationUtils.loadAnimation(context, R.anim.slide_in)
        slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out)

        View.inflate(context, R.layout.widget_status_view, this)
        prepareView()
    }

    private fun prepareView() {
        this.visibility = View.GONE
        root.background = backgroundColor
        statusText.setTextColor(fontColor)
    }

    fun onShow(msg: String) {
        statusText.text = msg
        enterAnimation(this)
    }

    fun onShowWithDelayed(msg: String) {
        statusText.text = msg
        enterAnimation(this)
        Handler(Looper.getMainLooper()).postDelayed({
            exitAnimation(this)
        }, DEFAULT_DELAY)
    }

    private fun enterAnimation(enterView: View?) {
        if (enterView == null) return
        if (enterView.visibility == View.VISIBLE) return
        enterView.visibility = View.VISIBLE
        enterView.startAnimation(slideIn)
    }

    private fun exitAnimation(exitView: View?) {
        if (exitView == null)
            return
        exitView.startAnimation(slideOut)
        slideOut?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationStart(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                exitView.visibility = View.GONE
                slideOut?.setAnimationListener(null)
            }
        })
    }

    private val backgroundColor
        get() = ContextCompat.getDrawable(context,
            if (theme == BACKGROUND_NORMAL) R.color.widget_status_view_normal_bg
            else R.color.widget_status_view_dark_bg)

    private val fontColor
        get() = ContextCompat.getColor(context,
            if (theme == BACKGROUND_NORMAL) R.color.colorPrimary
            else R.color.white)

    companion object {
        private const val BACKGROUND_DARK = 0
        private const val BACKGROUND_NORMAL = 1
        private const val DEFAULT_DELAY = 5000L
    }
}
