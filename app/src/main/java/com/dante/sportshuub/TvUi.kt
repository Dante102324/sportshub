package com.dante.sportshuub

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

// Shared look for the simple list screens (favorites pickers).
object TvUi {

    val backgroundColor = Color.rgb(18, 18, 21)
    val cardColor = Color.rgb(38, 38, 43)
    val gray = Color.rgb(180, 180, 185)

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun row(
        context: Context,
        text: String,
        onClick: (TextView) -> Unit
    ): TextView {

        val view = TextView(context)

        view.text = text
        view.textSize = 17f
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER_VERTICAL
        view.setPadding(dp(context, 16), 0, dp(context, 16), 0)
        view.isFocusable = true
        view.isClickable = true

        style(context, view, false)

        view.setOnFocusChangeListener { v, hasFocus ->
            style(context, v as TextView, hasFocus)
        }

        view.setOnClickListener { onClick(view) }

        view.layoutParams = LinearLayout.LayoutParams(
            -1,
            dp(context, 54)
        ).apply {
            bottomMargin = dp(context, 10)
        }

        return view
    }

    private fun style(context: Context, view: TextView, focused: Boolean) {
        val bg = GradientDrawable()
        bg.setColor(cardColor)
        bg.cornerRadius = dp(context, 9).toFloat()
        if (focused) {
            bg.setStroke(dp(context, 3), Color.WHITE)
        }
        view.background = bg
    }
}
