package com.example.surymeter.meter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class SpeedOverlay {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var valueView: TextView? = null
    private var unitView: TextView? = null

    fun show(context: Context) {
        if (view != null) return
        mainHandler.post {
            if (view != null) return@post
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val bg = GradientDrawable().apply {
                cornerRadius = context.dp(8f)
                setColor(Color.parseColor("#CC000000"))
            }

            val value = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                text = "0"
            }
            val unit = TextView(context).apply {
                setTextColor(Color.parseColor("#DDDDDD"))
                textSize = 7f
                text = "bps"
            }
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = bg
                setPadding(
                    context.dp(6f).toInt(),
                    context.dp(1f).toInt(),
                    context.dp(6f).toInt(),
                    context.dp(1f).toInt()
                )
            }
            container.addView(value)
            container.addView(unit)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = context.dp(48f).toInt()
                y = context.statusBarHeight() / 2
            }

            try {
                wm.addView(container, params)
            } catch (_: Exception) {
                return@post
            }
            view = container
            valueView = value
            unitView = unit
        }
    }

    fun update(number: String, unit: String) {
        mainHandler.post {
            valueView?.text = number
            unitView?.text = unit
        }
    }

    fun hide(context: Context) {
        mainHandler.post {
            val v = view ?: return@post
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(v)
            } catch (_: Exception) {
            }
            view = null
            valueView = null
            unitView = null
        }
    }

    private fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

    private fun Context.statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }
}
