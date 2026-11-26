package com.glassdock.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsHubActivity : Activity() {

    private data class SettingsItem(
        val iconType: String,
        val title: String,
        val subtitle: String,
        val accentHex: String,
        val tileTopHex: String,
        val tileBottomHex: String,
        val componentClass: String? = null,
        val fallbackAction: String? = null,
        val onCustomClick: (() -> Unit)? = null
    )

    private val allItems = listOf(
        SettingsItem(
            "wifi", "Network & Wi-Fi", "Wi-Fi, proxy & signal", "#38BDF8", "#0284C7", "#075985",
            "com.amazon.tv.settings.v2.tv.network.NetworkActivity", Settings.ACTION_WIFI_SETTINGS
        ),
        SettingsItem(
            "display",
            "Display & Sounds",
            "Resolution, HDR & audio",
            "#A78BFA",
            "#7C3AED",
            "#4C1D95",
            "com.amazon.tv.settings.v2.tv.display_sounds.DisplayAndSoundsActivity",
            Settings.ACTION_DISPLAY_SETTINGS
        ),
        SettingsItem(
            "apps",
            "Applications",
            "Manage apps & permissions",
            "#34D399",
            "#059669",
            "#065F46",
            "com.amazon.tv.settings.v2.tv.applications.ApplicationsActivity",
            Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        ),
        SettingsItem(
            "bluetooth",
            "Remotes & Bluetooth",
            "Controllers & speakers",
            "#60A5FA",
            "#2563EB",
            "#1E3A8A",
            "com.amazon.tv.settings.v2.tv.controllers_bluetooth_devices.ControllersAndBluetoothActivity",
            Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        SettingsItem(
            "device",
            "My Fire TV",
            "About, ADB & restart",
            "#FB923C",
            "#EA580C",
            "#9A3412",
            "com.amazon.tv.settings.v2.tv.device.DeviceActivity",
            Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        SettingsItem(
            "settings", "Preferences", "Privacy, language & time", "#94A3B8", "#475569", "#1E293B",
            "com.amazon.tv.settings.v2.tv.preferences.PreferencesActivity"
        ),
        SettingsItem(
            "settings",
            "Accessibility",
            "Services & captions",
            "#F472B6",
            "#DB2777",
            "#831843",
            "com.amazon.tv.settings.v2.tv.accessibility.AccessibilityActivity",
            Settings.ACTION_ACCESSIBILITY_SETTINGS
        ),
        SettingsItem(
            "device",
            "Account & Profile",
            "Amazon account settings",
            "#FBBF24",
            "#D97706",
            "#78350F",
            "com.amazon.tv.settings.v2.tv.my_account.MyAccountActivity"
        ),
        SettingsItem(
            "display", "Screensaver", "Start ambient clock now", "#38BDF8", "#0F172A", "#1E293B",
            onCustomClick = { startActivity(Intent(this, ScreensaverActivity::class.java)) }
        )
    )

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
            setPadding(dp(52), dp(36), dp(52), dp(44))
            clipChildren = false
            clipToPadding = false
        }

        container.addView(buildHeader())
        container.addView(buildAvailableGrid())
        addView(container)
    }

    private fun buildHeader(): FrameLayout = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(24) }

        addView(TextView(context).apply {
            text = "Fire TV Settings"
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            )
        })

        addView(TextView(context).apply {
            text = "Back to return"
            textSize = 13f
            setTextColor(Color.parseColor("#6E7C8E"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        })
    }

    private fun buildAvailableGrid(): GridLayout = GridLayout(this).apply {
        columnCount = 3
        clipChildren = false
        clipToPadding = false

        val available =
            allItems.filter { it.onCustomClick != null || resolveLaunchIntent(it) != null }
        available.forEachIndexed { idx, item ->
            val card = buildSettingsCard(item)
            addView(card)
            if (idx == 0) card.requestFocus()
        }
    }

    private fun buildSettingsCard(item: SettingsItem): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = true
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = UiTheme.glassCard(context, item.accentHex, false)
        layoutParams = GridLayout.LayoutParams().apply {
            width = dp(268)
            height = dp(86)
            setMargins(dp(8), dp(8), dp(8), dp(8))
        }

        addView(FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(14) }
            background =
                UiTheme.iconTile(context, item.tileTopHex, item.tileBottomHex, radiusDp = 14)
            addView(UtilityIconView(context, item.iconType))
        })

        addView(buildCardLabels(item))

        setOnFocusChangeListener { v, focused ->
            v.animateFocus(focused, 1.04f)
            v.background = UiTheme.glassCard(context, item.accentHex, focused)
        }
        setOnClickListener { openSubSetting(item) }
    }

    private fun buildCardLabels(item: SettingsItem): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        addView(TextView(context).apply {
            text = item.title
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        addView(TextView(context).apply {
            text = item.subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#7E8C9E"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(2), 0, 0)
        })
    }

    private fun openSubSetting(item: SettingsItem) {
        item.onCustomClick?.let {
            it()
            return
        }
        val intent = resolveLaunchIntent(item)
        if (intent != null && runCatching { startActivity(intent) }.isSuccess) return
        Toast.makeText(this, "${item.title} could not be opened", Toast.LENGTH_SHORT).show()
    }

    private fun resolveLaunchIntent(item: SettingsItem): Intent? {
        val direct = item.componentClass?.let {
            Intent().setComponent(ComponentName("com.amazon.tv.settings.v2", it))
        }
        if (direct != null && canLaunch(direct)) return direct

        val fallback = item.fallbackAction?.let { Intent(it) }
        if (fallback != null && canLaunch(fallback)) return fallback

        return null
    }

    private fun canLaunch(intent: Intent): Boolean {
        val info = packageManager.resolveActivity(intent, 0)?.activityInfo ?: return false
        if (!info.exported) return false
        val permission = info.permission ?: return true
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
