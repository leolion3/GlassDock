package com.glassdock.launcher

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class ScreensaverActivity : Activity() {

    private companion object {
        const val CLOCK_TICK_MS = 15_000L
        const val DRIFT_INTERVAL_MS = 28_000L
        const val PHOTO_ROTATE_MS = 35_000L
        const val LAUNCH_GUARD_MS = 450L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var createdAtMs = 0L

    private var playlist: List<LandscapePhoto> = emptyList()
    private var photoIndex = 0
    private var showingFirstBg = true

    private lateinit var root: FrameLayout
    private lateinit var bgA: ImageView
    private lateinit var bgB: ImageView
    private lateinit var contentBox: LinearLayout
    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var weatherView: TextView
    private lateinit var captionView: TextView

    private val clockTickTask = object : Runnable {
        override fun run() {
            updateClockAndDate()
            handler.postDelayed(this, CLOCK_TICK_MS)
        }
    }

    private val driftTask = object : Runnable {
        override fun run() {
            moveToRandomPosition()
            handler.postDelayed(this, DRIFT_INTERVAL_MS)
        }
    }

    private val rotatePhotoTask = object : Runnable {
        override fun run() {
            loadNextLandscape()
            handler.postDelayed(this, PHOTO_ROTATE_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createdAtMs = System.currentTimeMillis()
        setContentView(buildRoot())
        updateClockAndDate()

        WeatherClient.fetchAsync { weatherView.text = it.trim().removePrefix("·").trim() }
        LandscapeClient.fetchPlaylistAsync { photos ->
            if (isFinishing || isDestroyed || photos.isEmpty()) return@fetchPlaylistAsync
            playlist = photos.shuffled()
            loadNextLandscape()
            handler.postDelayed(rotatePhotoTask, PHOTO_ROTATE_MS)
        }
    }

    override fun onResume() {
        super.onResume()
        updateClockAndDate()
        handler.postDelayed(clockTickTask, CLOCK_TICK_MS)
        root.post { handler.postDelayed(driftTask, DRIFT_INTERVAL_MS) }
    }

    override fun onPause() {
        handler.removeCallbacks(clockTickTask)
        handler.removeCallbacks(driftTask)
        handler.removeCallbacks(rotatePhotoTask)
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && System.currentTimeMillis() - createdAtMs > LAUNCH_GUARD_MS) {
            finish()
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (System.currentTimeMillis() - createdAtMs > LAUNCH_GUARD_MS) finish()
        return true
    }

    private fun buildRoot(): FrameLayout {
        val ambientGlow = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#66000000"), Color.TRANSPARENT)
        ).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(180).toFloat()
        }

        root = FrameLayout(this).apply {
            background = ambientGlow
            clipChildren = false
            clipToPadding = false
        }

        bgA = createBackgroundImageView()
        bgB = createBackgroundImageView()

        val scrim = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#59000000"),
                    Color.parseColor("#40000000"),
                    Color.parseColor("#99000000")
                )
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        contentBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )

            addView(GeminiLogoView(context).apply {
                alpha = 0.85f
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                    bottomMargin = dp(16)
                }
            })

            clockView = TextView(context).apply {
                textSize = 78f
                includeFontPadding = false
                maxLines = 1
                setTextColor(Color.WHITE)
                setShadowLayer(12f, 0f, 3f, Color.parseColor("#99000000"))
                typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(clockView)
            addView(buildDateAndWeather())
        }

        captionView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#B3DCE4EE"))
            setShadowLayer(4f, 0f, 1f, Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            ).apply {
                leftMargin = dp(48)
                bottomMargin = dp(34)
            }
        }

        root.addView(bgA)
        root.addView(bgB)
        root.addView(scrim)
        root.addView(contentBox)
        root.addView(captionView)
        return root
    }

    private fun createBackgroundImageView(): ImageView = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0f
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    private fun buildDateAndWeather(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(10), 0, 0)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dateView = TextView(context).apply {
            textSize = 17f
            maxLines = 1
            setTextColor(Color.parseColor("#E6EEF2F6"))
            setShadowLayer(6f, 0f, 2f, Color.parseColor("#AA000000"))
        }

        val dot = TextView(context).apply {
            text = "  ·  "
            textSize = 17f
            setTextColor(Color.parseColor("#99DCE4EE"))
        }

        weatherView = TextView(context).apply {
            text = "--°"
            textSize = 17f
            maxLines = 1
            setTextColor(Color.parseColor("#E6EEF2F6"))
            setShadowLayer(6f, 0f, 2f, Color.parseColor("#AA000000"))
        }

        addView(dateView)
        addView(dot)
        addView(weatherView)
    }

    private fun loadNextLandscape() {
        if (playlist.isEmpty()) return
        val photo = playlist[photoIndex % playlist.size]
        photoIndex++

        LandscapeClient.loadBitmapAsync(photo) { bitmap ->
            if (isFinishing || isDestroyed || bitmap == null) return@loadBitmapAsync
            crossfadeBackground(bitmap, photo.title)
        }
    }

    private fun crossfadeBackground(bitmap: Bitmap, title: String) {
        val incoming = if (showingFirstBg) bgB else bgA
        val outgoing = if (showingFirstBg) bgA else bgB
        showingFirstBg = !showingFirstBg

        incoming.animate().cancel()
        incoming.setImageBitmap(bitmap)
        incoming.alpha = 0f
        incoming.scaleX = 1.0f
        incoming.scaleY = 1.0f

        incoming.animate()
            .alpha(1f)
            .setDuration(1400L)
            .withEndAction {
                incoming.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(PHOTO_ROTATE_MS)
                    .start()
            }
            .start()

        outgoing.animate().alpha(0f).setDuration(1400L).start()
        captionView.text = title
    }

    private fun updateClockAndDate() {
        val now = Date()
        val timePattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        clockView.text = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
        dateView.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
    }

    private fun moveToRandomPosition() {
        val maxX = ((root.width - contentBox.width) / 2f - dp(48)).coerceAtLeast(0f)
        val maxY = ((root.height - contentBox.height) / 2f - dp(48)).coerceAtLeast(0f)
        val targetX = if (maxX > 0f) Random.nextFloat() * (maxX * 2f) - maxX else 0f
        val targetY = if (maxY > 0f) Random.nextFloat() * (maxY * 2f) - maxY else 0f

        contentBox.animate()
            .alpha(0.2f)
            .setDuration(450)
            .withEndAction {
                contentBox.translationX = targetX
                contentBox.translationY = targetY
                contentBox.animate().alpha(1f).setDuration(600).start()
            }
            .start()
    }
}