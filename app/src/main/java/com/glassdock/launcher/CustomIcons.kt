package com.glassdock.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class GeminiLogoView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.parseColor("#4285F4"),
                Color.parseColor("#9B72CB"),
                Color.parseColor("#D96570")
            ),
            null, Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(cx, 0f)
        path.quadTo(cx, cy, w, cy)
        path.quadTo(cx, cy, cx, h)
        path.quadTo(cx, cy, 0f, cy)
        path.quadTo(cx, cy, cx, 0f)
        path.close()
        canvas.drawPath(path, paint)
    }
}

class AppIconView(
    context: Context,
    private val iconId: String,
    private val label: String = ""
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        when (iconId) {
            "prime" -> drawPrime(canvas, w, cx, cy)
            "disney" -> drawDisney(canvas, w, h, cx, cy)
            "netflix" -> drawLetter(canvas, "N", "#E50914", w * 0.54f, cx, cy + w * 0.19f)
            "youtube" -> drawPlayTriangle(canvas, cx, cy, w * 0.20f)
            "zdf" -> drawLetter(canvas, "ZDF", "#FFFFFF", w * 0.34f, cx, cy + w * 0.12f)
            "ard" -> drawArd(canvas, w, cx, cy)
            "expressvpn", "vpn" -> drawShield(canvas, w, h, cx)
            "plex" -> drawPlex(canvas, w, h, cx, cy)
            "jellyfin" -> drawJellyfin(canvas, w, h, cx, cy)
            "kodi" -> drawKodi(canvas, w, cx, cy)
            "arte" -> drawLetter(canvas, "arte", "#FFFFFF", w * 0.30f, cx, cy + w * 0.10f)
            "spotify" -> drawSpotify(canvas, w, cx, cy)
            "mediathek" -> drawMonogram(canvas, w, cx, cy, showBroadcastDot = true)
            else -> drawMonogram(canvas, w, cx, cy, showBroadcastDot = false)
        }
    }

    private fun drawPrime(canvas: Canvas, w: Float, cx: Float, cy: Float) {
        drawLetter(canvas, "prime", "#FFFFFF", w * 0.28f, cx, cy + w * 0.02f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.055f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(
            RectF(w * 0.24f, cy - w * 0.08f, w * 0.76f, cy + w * 0.26f),
            20f,
            140f,
            false,
            paint
        )
    }

    private fun drawDisney(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        drawLetter(canvas, "D+", "#FFFFFF", w * 0.34f, cx, cy + w * 0.14f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.045f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(RectF(w * 0.20f, h * 0.22f, w * 0.80f, h * 0.72f), 205f, 130f, false, paint)
    }

    private fun drawArd(canvas: Canvas, w: Float, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        canvas.drawCircle(cx, cy, w * 0.30f, paint)
        drawLetter(canvas, "1", "#FFFFFF", w * 0.36f, cx, cy + w * 0.13f)
    }

    private fun drawPlayTriangle(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx - r * 0.65f, cy - r)
        path.lineTo(cx + r * 1.05f, cy)
        path.lineTo(cx - r * 0.65f, cy + r)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawShield(canvas: Canvas, w: Float, h: Float, cx: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(cx, h * 0.20f)
        path.lineTo(w * 0.76f, h * 0.30f)
        path.lineTo(w * 0.72f, h * 0.62f)
        path.quadTo(cx, h * 0.82f, cx, h * 0.82f)
        path.quadTo(cx, h * 0.82f, w * 0.28f, h * 0.62f)
        path.lineTo(w * 0.24f, h * 0.30f)
        path.close()
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(w * 0.40f, h * 0.44f)
        path.lineTo(cx, h * 0.62f)
        path.lineTo(w * 0.60f, h * 0.44f)
        canvas.drawPath(path, paint)
    }

    private fun drawPlex(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        paint.color = Color.parseColor("#E5A00D")
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx - w * 0.14f, h * 0.22f)
        path.lineTo(cx + w * 0.02f, h * 0.22f)
        path.lineTo(cx + w * 0.22f, cy)
        path.lineTo(cx + w * 0.02f, h * 0.78f)
        path.lineTo(cx - w * 0.14f, h * 0.78f)
        path.lineTo(cx + w * 0.06f, cy)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawJellyfin(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.075f
        paint.strokeJoin = Paint.Join.ROUND
        path.reset()
        path.moveTo(cx, h * 0.22f)
        path.lineTo(w * 0.78f, h * 0.74f)
        path.lineTo(w * 0.22f, h * 0.74f)
        path.close()
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy + h * 0.08f, w * 0.10f, paint)
    }

    private fun drawKodi(canvas: Canvas, w: Float, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        val r = w * 0.28f
        path.reset()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r, cy)
        path.lineTo(cx, cy + r)
        path.lineTo(cx - r, cy)
        path.close()
        canvas.drawPath(path, paint)
        drawLetter(canvas, "K", "#FFFFFF", w * 0.30f, cx, cy + w * 0.11f)
    }

    private fun drawSpotify(canvas: Canvas, w: Float, cx: Float, cy: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        paint.strokeCap = Paint.Cap.ROUND
        listOf(-0.14f, 0.0f, 0.14f).forEachIndexed { i, offset ->
            val span = w * (0.28f - i * 0.04f)
            val rect = RectF(
                cx - span,
                cy + w * offset - w * 0.10f,
                cx + span,
                cy + w * offset + w * 0.22f
            )
            canvas.drawArc(rect, 215f, 110f, false, paint)
        }
    }

    private fun drawMonogram(
        canvas: Canvas,
        w: Float,
        cx: Float,
        cy: Float,
        showBroadcastDot: Boolean
    ) {
        val initials = buildInitials(label)
        val size = if (initials.length > 2) w * 0.30f else w * 0.36f
        drawLetter(canvas, initials, "#FFFFFF", size, cx, cy + size * 0.35f)
        if (showBroadcastDot) {
            paint.color = Color.parseColor("#B3FFFFFF")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * 0.78f, w * 0.22f, w * 0.06f, paint)
        }
    }

    private fun drawLetter(
        canvas: Canvas,
        text: String,
        hex: String,
        size: Float,
        x: Float,
        y: Float
    ) {
        paint.color = Color.parseColor(hex)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        paint.textSize = size
        canvas.drawText(text, x, y, paint)
    }

    private fun buildInitials(raw: String): String {
        val words = raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }
    }
}

class UtilityIconView(context: Context, private val type: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DCE4EE")
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val cx = w / 2f
        val cy = height / 2f
        when (type) {
            "apps" -> drawGrid(canvas, cx, cy, w)
            "wifi" -> drawWifi(canvas, cx, cy, w)
            "display" -> drawDisplay(canvas, cx, cy, w)
            "device" -> drawDevice(canvas, cx, cy, w)
            "bluetooth" -> drawBluetooth(canvas, cx, cy, w)
            else -> drawGear(canvas, cx, cy, w)
        }
    }

    private fun drawGrid(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.FILL
        val gap = w * 0.15f
        val r = w * 0.045f
        for (row in -1..1) {
            for (col in -1..1) {
                canvas.drawCircle(cx + col * gap, cy + row * gap, r, paint)
            }
        }
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.065f
        paint.strokeCap = Paint.Cap.ROUND
        val innerR = w * 0.15f
        canvas.drawCircle(cx, cy, innerR, paint)
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val x1 = cx + (innerR * 1.15f * cos(angle)).toFloat()
            val y1 = cy + (innerR * 1.15f * sin(angle)).toFloat()
            val x2 = cx + (innerR * 1.65f * cos(angle)).toFloat()
            val y2 = cy + (innerR * 1.65f * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    private fun drawWifi(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        paint.strokeCap = Paint.Cap.ROUND
        listOf(0.32f, 0.20f).forEach { factor ->
            val r = w * factor
            canvas.drawArc(
                RectF(cx - r, cy - r + w * 0.12f, cx + r, cy + r + w * 0.12f),
                225f,
                90f,
                false,
                paint
            )
        }
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy + w * 0.20f, w * 0.055f, paint)
    }

    private fun drawDisplay(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        canvas.drawRoundRect(
            RectF(w * 0.20f, w * 0.24f, w * 0.80f, w * 0.64f),
            w * 0.06f,
            w * 0.06f,
            paint
        )
        canvas.drawLine(cx - w * 0.14f, w * 0.76f, cx + w * 0.14f, w * 0.76f, paint)
    }

    private fun drawDevice(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        canvas.drawCircle(cx, cy, w * 0.28f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, w * 0.08f, paint)
    }

    private fun drawBluetooth(canvas: Canvas, cx: Float, cy: Float, w: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.06f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val path = Path().apply {
            moveTo(cx - w * 0.12f, cy - w * 0.12f)
            lineTo(cx + w * 0.14f, cy + w * 0.14f)
            lineTo(cx, cy + w * 0.28f)
            lineTo(cx, cy - w * 0.28f)
            lineTo(cx + w * 0.14f, cy - w * 0.14f)
            lineTo(cx - w * 0.12f, cy + w * 0.12f)
        }
        canvas.drawPath(path, paint)
    }
}
