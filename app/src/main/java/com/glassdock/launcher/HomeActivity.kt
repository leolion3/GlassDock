package com.glassdock.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.util.concurrent.Executors
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader

class HomeActivity : Activity() {

    private lateinit var heroTag: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var heroAccentBar: View
    private lateinit var weatherText: TextView
    private lateinit var quickJumpAppsRow: LinearLayout
    private lateinit var dockAppsContainer: LinearLayout

    private val mainHandler = Handler(Looper.getMainLooper())
    private val catalogExecutor = Executors.newSingleThreadExecutor()
    private var renderedPinnedKeys: List<String> = emptyList()
    private var backInvokedCallback: Any? = null

    private val startScreensaver = Runnable {
        startActivity(Intent(this, ScreensaverActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildRoot())
        registerBackInterceptor()
        applyPinnedApps(FavoritesStore.resolvePinnedApps(this, AppCatalog.coreLibraryApps))
        WeatherClient.fetchAsync { weatherText.text = it }
    }

    override fun onResume() {
        super.onResume()
        resetIdleTimer()
        refreshPinnedAppsAsync()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(startScreensaver)
        super.onPause()
    }

    override fun onDestroy() {
        unregisterBackInterceptor()
        mainHandler.removeCallbacks(startScreensaver)
        catalogExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        resetIdleTimer()
        return super.dispatchKeyEvent(event)
    }

    private fun registerBackInterceptor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val callback = OnBackInvokedCallback { /* Intentionally consume Back on Home screen */ }
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback
        )
        backInvokedCallback = callback
    }

    private fun unregisterBackInterceptor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val callback = backInvokedCallback as? OnBackInvokedCallback ?: return
        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
        backInvokedCallback = null
    }

    @Deprecated("Fallback for API < 33 (Fire OS 8); API 33+ uses OnBackInvokedDispatcher")
    @Suppress("DEPRECATION")
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        // Intentionally consume Back on API < 33 so the launcher does not exit
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
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

    private fun resetIdleTimer() {
        mainHandler.removeCallbacks(startScreensaver)
        mainHandler.postDelayed(startScreensaver, 180_000L)
    }

    private fun refreshPinnedAppsAsync() {
        catalogExecutor.execute {
            val allApps = loadAllAvailableApps()
            val pinned = FavoritesStore.resolvePinnedApps(this, allApps)
            postPinnedAppsUpdate(pinned)
        }
    }

    private fun postPinnedAppsUpdate(pinned: List<AppSpec>) {
        mainHandler.post {
            if (!isFinishing && !isDestroyed) {
                applyPinnedApps(pinned)
            }
        }
    }

    private fun loadAllAvailableApps(): List<AppSpec> {
        val excluded = AppCatalog.coreLibraryApps.flatMap { it.packages }.toSet() + packageName
        val (promotedExtras, otherApps) = AppCatalog.queryInstalledApps(packageManager, excluded)
        return AppCatalog.coreLibraryApps + promotedExtras + otherApps
    }

    private fun applyPinnedApps(pinnedApps: List<AppSpec>) {
        val newKeys = pinnedApps.map(FavoritesStore::keyFor)
        if (newKeys == renderedPinnedKeys) return

        renderedPinnedKeys = newKeys
        rebuildQuickJump(pinnedApps)
        rebuildDockApps(pinnedApps)
    }

    private fun rebuildQuickJump(pinnedApps: List<AppSpec>) {
        quickJumpAppsRow.removeAllViews()
        pinnedApps.forEach { quickJumpAppsRow.addView(buildPosterCard(it)) }
    }

    private fun rebuildDockApps(pinnedApps: List<AppSpec>) {
        val compact = pinnedApps.size > 6
        dockAppsContainer.removeAllViews()
        pinnedApps.forEachIndexed { idx, app ->
            val tile = buildDockTile(app, compact)
            dockAppsContainer.addView(tile)
            if (idx == 0) {
                tile.requestFocus()
                updateHero(app.category.uppercase(), app.label, app.tagline, app.accentHex)
            }
        }
    }

    private fun buildDockTile(app: AppSpec, compact: Boolean): FrameLayout =
        FrameLayout(this).apply {
            val tileSize = dp(if (compact) 50 else 58)
            val margin = dp(if (compact) 5 else 7)
            layoutParams = LinearLayout.LayoutParams(tileSize, tileSize).apply {
                setMargins(margin, 0, margin, 0)
            }
            isFocusable = true
            isFocusableInTouchMode = true
            background = UiTheme.iconTile(context, app.tileTopHex, app.tileBottomHex)
            clipToOutline = true
            addView(AppIconView(context, app.id, app.label))

            setOnFocusChangeListener { v, focused ->
                v.animateFocus(focused, 1.15f)
                v.background =
                    UiTheme.iconTile(context, app.tileTopHex, app.tileBottomHex, focused = focused)
                if (focused) updateHero(
                    app.category.uppercase(),
                    app.label,
                    app.tagline,
                    app.accentHex
                )
            }
            setOnClickListener { AppCatalog.launch(context, app) }
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
        translationZ = dp(12).toFloat()
        layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(24)
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
        translationZ = 0f
        clipChildren = false
        clipToPadding = false
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2.05f
        )

        addView(TextView(context).apply {
            text = "Quick jump"
            textSize = 13f
            setTextColor(Color.parseColor("#6E7C8E"))
            setPadding(dp(15), 0, 0, dp(2))
        })

        addView(buildQuickJumpScroller())
    }

    private fun buildQuickJumpScroller(): HorizontalScrollView =
        LeftFadeScrollView(
            context = this,
            fadeWidthPx = dp(64),
            verticalPadPx = dp(24),
            rightOverdrawPx = dp(160)
        ).apply {
            isHorizontalScrollBarEnabled = false
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8), dp(14), dp(8), dp(14))

            quickJumpAppsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            addView(quickJumpAppsRow)
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

        dockAppsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        addView(dockAppsContainer)

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
                "Wi-Fi, Display, Apps & TV settings"
            ) {
                startActivity(Intent(context, SettingsHubActivity::class.java))
            })
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

    private class LeftFadeScrollView(
        context: Context,
        private val fadeWidthPx: Int,
        private val verticalPadPx: Int,
        private val rightOverdrawPx: Int
    ) : HorizontalScrollView(context) {

        private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            shader = LinearGradient(
                0f, 0f, fadeWidthPx.toFloat(), 0f,
                Color.BLACK, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }

        override fun dispatchDraw(canvas: Canvas) {
            if (scrollX <= 0 || fadeWidthPx <= 0) {
                super.dispatchDraw(canvas)
                return
            }

            val fadeStart = scrollX.toFloat()
            val effectiveFadeWidth = minOf(scrollX, fadeWidthPx).toFloat()
            val top = -verticalPadPx.toFloat()
            val bottom = (height + verticalPadPx).toFloat()
            val right = (scrollX + width + rightOverdrawPx).toFloat()

            val saveCount = canvas.saveLayer(fadeStart, top, right, bottom, null)
            super.dispatchDraw(canvas)

            canvas.save()
            canvas.translate(fadeStart, 0f)
            canvas.scale(effectiveFadeWidth / fadeWidthPx.toFloat(), 1f)
            canvas.drawRect(0f, top, fadeWidthPx.toFloat(), bottom, fadePaint)
            canvas.restore()

            canvas.restoreToCount(saveCount)
        }
    }
}
