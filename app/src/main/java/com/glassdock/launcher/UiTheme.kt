package com.glassdock.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun View.animateFocus(hasFocus: Boolean, scale: Float = 1.12f) {
    val target = if (hasFocus) scale else 1.0f
    animate().scaleX(target).scaleY(target).setDuration(110).start()
}

object UiTheme {

    fun backdrop(): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(
            Color.parseColor("#070E1A"),
            Color.parseColor("#04070E"),
            Color.parseColor("#010204")
        )
    )

    fun gradientCard(
        context: Context,
        topHex: String,
        bottomHex: String,
        accentHex: String,
        focused: Boolean
    ): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.parseColor(topHex), Color.parseColor(bottomHex))
    ).apply {
        cornerRadius = context.dp(18).toFloat()
        val strokeColor = if (focused) Color.parseColor(accentHex) else Color.parseColor("#1FFFFFFF")
        setStroke(context.dp(if (focused) 2 else 1), strokeColor)
    }

    fun glassCard(
        context: Context,
        accentHex: String,
        focused: Boolean,
        radiusDp: Int = 18
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(radiusDp).toFloat()
        setColor(Color.parseColor(if (focused) "#B3141D2E" else "#730D121D"))
        val strokeColor = if (focused) Color.parseColor(accentHex) else Color.parseColor("#1AFFFFFF")
        setStroke(context.dp(if (focused) 2 else 1), strokeColor)
    }

    fun iconTile(
        context: Context,
        topHex: String,
        bottomHex: String,
        radiusDp: Int = 16,
        focused: Boolean = false
    ): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor(topHex), Color.parseColor(bottomHex))
    ).apply {
        cornerRadius = context.dp(radiusDp).toFloat()
        if (focused) setStroke(context.dp(2), Color.WHITE)
    }

    fun dockPill(context: Context): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(26).toFloat()
        setColor(Color.parseColor("#8A0D121D"))
        setStroke(context.dp(1), Color.parseColor("#1FFFFFFF"))
    }

    fun pillButton(context: Context, focused: Boolean): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(20).toFloat()
        setColor(Color.parseColor(if (focused) "#2A364F" else "#660D121D"))
        val strokeColor = if (focused) Color.WHITE else Color.parseColor("#24FFFFFF")
        setStroke(context.dp(if (focused) 2 else 1), strokeColor)
    }

    fun circleButton(context: Context, focused: Boolean): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor(if (focused) "#3DFFFFFF" else "#1AFFFFFF"))
        if (focused) setStroke(context.dp(2), Color.WHITE)
    }
}
