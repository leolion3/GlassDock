package com.glassdock.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.os.Handler
import android.os.Looper

class HomeActivity : Activity() {

    private lateinit var heroTag: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var heroAccentBar: View
    private lateinit var weatherText: TextView
    private val idleHandler = Handler(Looper.getMainLooper())
    private val startScreensaver = Runnable {
        startActivity(Intent(this, ScreensaverActivity::class.java))
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(startScreensaver)
        idleHandler.postDelayed(startScreensaver, 180_000L) // 3 minutes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildRoot())
        WeatherClient.fetchAsync { weatherText.text = it }
    }

    override fun onResume() {
        super.onResume()
        resetIdleTimer()
    }

    override fun onPause() {
        idleHandler.removeCallbacks(startScreensaver)
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        resetIdleTimer()
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_BACK -> true
        KeyEvent.KEYCODE_MENU -> {
            startActivity(Intent(this, SettingsHubActivity::class.java))
            true
        }

        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            startActivity(Intent(this, ScreensaverActivity::class.java))
            true
        }

        else -> super.onKeyDown(keyCode, event)
    }

    private fun buildRoot(): FrameLayout = FrameLayout(this).apply {
        background = UiTheme.backdrop()
        setPadding(dp(52), dp(36), dp(52), dp(38))
        clipChildren = false
        clipToPadding = false

        addView(GeminiLogoView(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(30), dp(30), Gravity.TOP or Gravity.START)
        })
        addView(buildTopRightStatus())
        addView(buildCenterShelf())
        addView(buildGlassDock())
    }

    private fun buildTopRightStatus(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        )

        addView(TextClock(context).apply {
            format24Hour = "HH:mm"
            format12Hour = "h:mm"
            textSize = 30f
            includeFontPadding = false
            setTextColor(Color.parseColor("#EEF2F6"))
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        })

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, 0)
            addView(TextClock(context).apply {
                format24Hour = "EEE, d MMM"
                format12Hour = "EEE, MMM d"
                textSize = 13f
                setTextColor(Color.parseColor("#7E8C9E"))
            })
            weatherText = TextView(context).apply {
                text = "  ·  --°"
                textSize = 13f
                setTextColor(Color.parseColor("#9FB0C4"))
            }
            addView(weatherText)
        })
    }

    private fun buildCenterShelf(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL
        ).apply { bottomMargin = dp(34) }

        addView(buildHeroColumn())
        addView(buildQuickJumpRow())
    }

    private fun buildHeroColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(32)
            }
        heroTag = TextView(context).apply {
            textSize = 11f
            letterSpacing = 0.22f
            typeface = Typeface.DEFAULT_BOLD
        }
        heroTitle = TextView(context).apply {
            textSize = 44f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 1
            setPadding(0, dp(6), 0, dp(6))
        }
        heroSub = TextView(context).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#8A99AD"))
            setPadding(0, 0, 0, dp(16))
        }
        heroAccentBar = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(3))
        }
        addView(heroTag)
        addView(heroTitle)
        addView(heroSub)
        addView(heroAccentBar)
    }

    private fun buildQuickJumpRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false

        addView(TextView(context).apply {
            text = "Quick jump"
            textSize = 13f
            setTextColor(Color.parseColor("#6E7C8E"))
            setPadding(dp(8), 0, 0, dp(10))
        })

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        AppCatalog.dockApps.forEach { row.addView(buildPosterCard(it)) }
        addView(row)
    }

    private fun buildPosterCard(app: AppSpec): FrameLayout = FrameLayout(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(dp(122), dp(174)).apply { setMargins(dp(7), 0, dp(7), 0) }
        isFocusable = true
        isFocusableInTouchMode = true
        background =
            UiTheme.gradientCard(context, app.cardTopHex, app.cardBottomHex, app.accentHex, false)
        clipToOutline = true

        addView(AppIconView(context, app.id, app.label).apply {
            layoutParams = FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER)
                .apply { bottomMargin = dp(12) }
        })
        addView(TextView(context).apply {
            text = app.label
            textSize = 13f
            setTextColor(Color.parseColor("#DCE4EE"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply { bottomMargin = dp(14) }
        })

        setOnFocusChangeListener { v, focused ->
            v.animateFocus(focused, 1.07f)
            v.background = UiTheme.gradientCard(
                context,
                app.cardTopHex,
                app.cardBottomHex,
                app.accentHex,
                focused
            )
            if (focused) updateHero(app.category.uppercase(), app.label, app.tagline, app.accentHex)
        }
        setOnClickListener { AppCatalog.launch(context, app) }
    }

    private fun buildGlassDock(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = UiTheme.dockPill(context)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        clipChildren = false
        clipToPadding = false
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )

        AppCatalog.dockApps.forEachIndexed { idx, app ->
            val tile = buildDockTile(app)
            addView(tile)
            if (idx == 0) {
                tile.requestFocus()
                updateHero(app.category.uppercase(), app.label, app.tagline, app.accentHex)
            }
        }

        addView(View(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(dp(1), dp(34)).apply { setMargins(dp(12), 0, dp(12), 0) }
            setBackgroundColor(Color.parseColor("#24FFFFFF"))
        })

        addView(
            buildUtilityButton(
                "apps",
                "All Apps",
                "LIBRARY",
                "Streaming, Mediatheks & installed apps"
            ) {
                startActivity(Intent(context, AllAppsActivity::class.java))
            })
        addView(
            buildUtilityButton(
                "settings",
                "Settings",
                "SYSTEM",
                "Wi-Fi, Display, Apps & Fire TV settings"
            ) {
                startActivity(Intent(context, SettingsHubActivity::class.java))
            })
    }

    private fun buildDockTile(app: AppSpec): FrameLayout = FrameLayout(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(dp(58), dp(58)).apply { setMargins(dp(7), 0, dp(7), 0) }
        isFocusable = true
        isFocusableInTouchMode = true
        background = UiTheme.iconTile(context, app.tileTopHex, app.tileBottomHex)
        clipToOutline = true
        addView(AppIconView(context, app.id, app.label))

        setOnFocusChangeListener { v, focused ->
            v.animateFocus(focused, 1.15f)
            v.background =
                UiTheme.iconTile(context, app.tileTopHex, app.tileBottomHex, focused = focused)
            if (focused) updateHero(app.category.uppercase(), app.label, app.tagline, app.accentHex)
        }
        setOnClickListener { AppCatalog.launch(context, app) }
    }

    private fun buildUtilityButton(
        iconType: String,
        title: String,
        tag: String,
        sub: String,
        onClick: () -> Unit
    ): FrameLayout = FrameLayout(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(dp(46), dp(46)).apply { setMargins(dp(6), 0, dp(6), 0) }
        isFocusable = true
        isFocusableInTouchMode = true
        background = UiTheme.circleButton(context, false)
        addView(UtilityIconView(context, iconType))

        setOnFocusChangeListener { v, focused ->
            v.animateFocus(focused, 1.14f)
            v.background = UiTheme.circleButton(context, focused)
            if (focused) updateHero(tag, title, sub, "#90CAF9")
        }
        setOnClickListener { onClick() }
    }

    private fun updateHero(tag: String, title: String, sub: String, accentHex: String) {
        heroTag.text = tag
        heroTitle.text = title
        heroSub.text = sub
        val color = Color.parseColor(accentHex)
        heroTag.setTextColor(color)
        heroAccentBar.setBackgroundColor(color)
    }
}
