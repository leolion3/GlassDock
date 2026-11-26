package com.glassdock.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast

class AllAppsActivity : Activity() {

    private var extendedVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildRoot())
    }

    private fun buildRoot(): ScrollView = ScrollView(this).apply {
        background = UiTheme.backdrop()
        isFillViewport = true
        clipChildren = false
        clipToPadding = false

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(52), dp(36), dp(52), dp(48))
            clipChildren = false
            clipToPadding = false
        }

        container.addView(buildHeader())

        val excluded = AppCatalog.coreLibraryApps.flatMap { it.packages }.toSet() + packageName
        val (promotedExtras, otherApps) = AppCatalog.queryInstalledApps(packageManager, excluded)
        val primaryApps = AppCatalog.coreLibraryApps + promotedExtras

        val primaryGrid = createGrid()
        primaryApps.forEachIndexed { idx, app ->
            val card = buildAppCard(app)
            primaryGrid.addView(card)
            if (idx == 0) card.requestFocus()
        }
        container.addView(primaryGrid)

        if (otherApps.isNotEmpty()) {
            val extendedGrid = createGrid().apply {
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16) }
            }
            otherApps.forEach { extendedGrid.addView(buildAppCard(it)) }

            container.addView(buildToggleButton(otherApps.size, extendedGrid))
            container.addView(extendedGrid)
        }

        addView(container)
    }

    private fun buildHeader(): FrameLayout = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(24) }

        addView(TextView(context).apply {
            text = "Streaming & Essentials"
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            )
            ellipsize = TextUtils.TruncateAt.END
        })

        addView(TextView(context).apply {
            text = "Hold Select or press Menu to pin  ·  Back to return"
            textSize = 13f
            setTextColor(Color.parseColor("#6E7C8E"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
            ellipsize = TextUtils.TruncateAt.END
        })
    }

    private fun createGrid(): GridLayout = GridLayout(this).apply {
        columnCount = 4
        clipChildren = false
        clipToPadding = false
    }

    private fun buildAppCard(app: AppSpec): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = true
        setPadding(dp(14), dp(14), dp(16), dp(14))
        background = UiTheme.glassCard(context, app.accentHex, false)
        layoutParams = GridLayout.LayoutParams().apply {
            width = dp(202)
            height = dp(84)
            setMargins(dp(8), dp(8), dp(8), dp(8))
        }

        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(14) }
            background = UiTheme.iconTile(context, app.tileTopHex, app.tileBottomHex, radiusDp = 14)
            clipToOutline = true
            addView(AppIconView(context, app.id, app.label))
        }

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply {
                text = app.label
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            addView(TextView(context).apply {
                text = app.category
                textSize = 12f
                setTextColor(Color.parseColor("#7E8C9E"))
                setPadding(0, dp(2), 0, 0)
                ellipsize = TextUtils.TruncateAt.END
            })
        }

        val starBadge = buildStarBadge(app)

        addView(iconBox)
        addView(textCol)
        addView(starBadge)

        setOnFocusChangeListener { v, focused ->
            v.animateFocus(focused, 1.05f)
            v.background = UiTheme.glassCard(context, app.accentHex, focused)
        }
        setOnClickListener { AppCatalog.launch(context, app) }
        bindPinControls(this, app, starBadge)
    }

    private fun buildStarBadge(app: AppSpec): TextView = TextView(this).apply {
        text = if (FavoritesStore.isFavorite(this@AllAppsActivity, app)) "★" else ""
        textSize = 15f
        setTextColor(Color.parseColor("#38BDF8"))
        setPadding(dp(6), 0, 0, 0)
    }

    private fun bindPinControls(card: View, app: AppSpec, starBadge: TextView) {
        card.setOnLongClickListener {
            toggleAppFavorite(app, starBadge)
            true
        }
        card.setOnKeyListener { _, keyCode, event ->
            handlePinKeyEvent(event, keyCode, app, starBadge)
        }
    }

    private fun handlePinKeyEvent(
        event: KeyEvent,
        keyCode: Int,
        app: AppSpec,
        starBadge: TextView
    ): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || keyCode != KeyEvent.KEYCODE_MENU) {
            return false
        }
        toggleAppFavorite(app, starBadge)
        return true
    }

    private fun toggleAppFavorite(app: AppSpec, starBadge: TextView) {
        val message = when (FavoritesStore.toggleFavorite(this, app)) {
            FavoriteToggleResult.PINNED -> {
                starBadge.text = "★"
                "Pinned ${app.label} to Home"
            }

            FavoriteToggleResult.UNPINNED -> {
                starBadge.text = ""
                "Removed ${app.label} from Home"
            }

            FavoriteToggleResult.LIMIT_REACHED -> {
                "Max ${FavoritesStore.MAX_FAVORITES} pinned apps — unpin one first"
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun buildToggleButton(count: Int, targetGrid: GridLayout): TextView =
        TextView(this).apply {
            text = "Show other apps ($count)  ▾"
            ellipsize = TextUtils.TruncateAt.END
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.parseColor("#9FB0C4"))
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(dp(22), dp(12), dp(22), dp(12))
            background = UiTheme.pillButton(context, false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
                leftMargin = dp(8)
            }

            setOnFocusChangeListener { v, focused ->
                v.background = UiTheme.pillButton(context, focused)
                setTextColor(if (focused) Color.WHITE else Color.parseColor("#9FB0C4"))
            }
            setOnClickListener {
                extendedVisible = !extendedVisible
                targetGrid.visibility = if (extendedVisible) View.VISIBLE else View.GONE
                text = if (extendedVisible) "Hide other apps  ▴" else "Show other apps ($count)  ▾"
            }
        }
}
