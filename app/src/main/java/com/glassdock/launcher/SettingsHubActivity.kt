package com.glassdock.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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

/**
 * Platform-agnostic TV settings hub supporting Fire OS, Android TV, and Google TV.
 */
class SettingsHubActivity : Activity() {

    private data class SettingsItem(
        val iconType: String,
        val title: String,
        val subtitle: String,
        val accentHex: String,
        val tileTopHex: String,
        val tileBottomHex: String,
        val componentClass: String? = null,
        val fallbackActions: List<String> = emptyList(),
        val onCustomClick: (() -> Unit)? = null
    )

    private val isFireTv: Boolean by lazy { detectFireTvEnvironment() }
    private val allItems: List<SettingsItem> by lazy { buildSettingsItems() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildRoot())
    }

    private fun buildRoot(): ScrollView = ScrollView(this).apply {
        background = UiTheme.backdrop()
        isFillViewport = true
        clipChildren = false
        clipToPadding = false
        addView(buildContainer())
    }

    private fun buildContainer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(52), dp(36), dp(52), dp(44))
        clipChildren = false
        clipToPadding = false
        addView(buildHeader())
        addView(buildAvailableGrid())
    }

    private fun buildHeader(): FrameLayout = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(24) }
        addView(buildHeaderTitle())
        addView(buildHeaderHint())
    }

    private fun buildHeaderTitle(): TextView = TextView(this).apply {
        text = if (isFireTv) "Fire TV Settings" else "TV Settings"
        textSize = 26f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(Color.WHITE)
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START or Gravity.CENTER_VERTICAL
        )
    }

    private fun buildHeaderHint(): TextView = TextView(this).apply {
        text = "Back to return"
        textSize = 13f
        setTextColor(Color.parseColor("#6E7C8E"))
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.CENTER_VERTICAL
        )
    }

    private fun buildAvailableGrid(): GridLayout = GridLayout(this).apply {
        columnCount = 3
        clipChildren = false
        clipToPadding = false
        populateGrid(this, allItems.filter(::isItemAvailable))
    }

    private fun populateGrid(grid: GridLayout, items: List<SettingsItem>) {
        // Loop invariant: all items[0..idx-1] are attached to grid and item 0 has focus requested.
        items.forEachIndexed { idx, item ->
            val card = buildSettingsCard(item)
            grid.addView(card)
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
        addView(buildCardIcon(item))
        addView(buildCardLabels(item))
        bindCardInteractions(this, item)
    }

    private fun buildCardIcon(item: SettingsItem): FrameLayout = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(14) }
        background = UiTheme.iconTile(context, item.tileTopHex, item.tileBottomHex, radiusDp = 14)
        addView(UtilityIconView(context, item.iconType))
    }

    private fun buildCardLabels(item: SettingsItem): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        addView(buildLabelText(item.title, 15f, Color.WHITE, Typeface.BOLD, 0))
        addView(
            buildLabelText(
                item.subtitle,
                12f,
                Color.parseColor("#7E8C9E"),
                Typeface.NORMAL,
                dp(2)
            )
        )
    }

    private fun buildLabelText(
        content: String,
        sizeSp: Float,
        color: Int,
        style: Int,
        topPaddingPx: Int
    ): TextView = TextView(this).apply {
        text = content
        textSize = sizeSp
        typeface = Typeface.create("sans-serif-medium", style)
        setTextColor(color)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, topPaddingPx, 0, 0)
    }

    private fun bindCardInteractions(card: LinearLayout, item: SettingsItem) {
        card.setOnFocusChangeListener { view, focused ->
            view.animateFocus(focused, 1.04f)
            view.background = UiTheme.glassCard(this, item.accentHex, focused)
        }
        card.setOnClickListener { openSubSetting(item) }
    }

    private fun openSubSetting(item: SettingsItem) {
        val customAction = item.onCustomClick
        if (customAction != null) {
            customAction()
            return
        }
        launchSystemSetting(item)
    }

    private fun launchSystemSetting(item: SettingsItem) {
        val intent = resolveLaunchIntent(item)
        if (intent != null && startSettingsIntent(intent)) return
        Toast.makeText(this, "${item.title} could not be opened", Toast.LENGTH_SHORT).show()
    }

    private fun startSettingsIntent(intent: Intent): Boolean {
        HomeRedirectService.pauseRedirectTemporarily()
        return runCatching { startActivity(intent) }.isSuccess
    }

    private fun isItemAvailable(item: SettingsItem): Boolean {
        return item.onCustomClick != null || resolveLaunchIntent(item) != null
    }

    private fun resolveLaunchIntent(item: SettingsItem): Intent? {
        return resolveDirectIntent(item.componentClass)
            ?: resolveFallbackIntent(item.fallbackActions)
    }

    private fun resolveDirectIntent(componentClass: String?): Intent? {
        if (componentClass.isNullOrBlank()) return null
        val intent = Intent().setComponent(ComponentName(FIRE_TV_SETTINGS_PACKAGE, componentClass))
        return intent.takeIf(::canLaunch)
    }

    private fun resolveFallbackIntent(actions: List<String>): Intent? {
        return actions.asSequence()
            .map(::Intent)
            .firstOrNull(::canLaunch)
    }

    private fun canLaunch(intent: Intent): Boolean {
        val info = packageManager.resolveActivity(intent, 0)?.activityInfo ?: return false
        if (!info.exported || info.packageName in BLOCKED_SETTINGS_HOSTS) return false
        val permission = info.permission ?: return true
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun detectFireTvEnvironment(): Boolean {
        val hasFireSettings = runCatching {
            packageManager.getPackageInfo(FIRE_TV_SETTINGS_PACKAGE, 0)
        }.isSuccess
        return hasFireSettings || Build.MANUFACTURER.equals("Amazon", ignoreCase = true)
    }

    private fun buildSettingsItems(): List<SettingsItem> = listOf(
        SettingsItem(
            "wifi",
            "Network & Wi-Fi",
            "Wi-Fi, proxy & signal",
            "#38BDF8",
            "#0284C7",
            "#075985",
            "com.amazon.tv.settings.v2.tv.network.NetworkActivity",
            listOf(
                Settings.ACTION_WIFI_SETTINGS,
                Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS
            )
        ),
        SettingsItem(
            "display",
            "Display & Sounds",
            "Resolution, HDR & audio",
            "#A78BFA",
            "#7C3AED",
            "#4C1D95",
            "com.amazon.tv.settings.v2.tv.display_sounds.DisplayAndSoundsActivity",
            listOf(
                Settings.ACTION_DISPLAY_SETTINGS,
                Settings.ACTION_SOUND_SETTINGS
            )
        ),
        SettingsItem(
            "apps",
            "Applications",
            "Manage apps & permissions",
            "#34D399",
            "#059669",
            "#065F46",
            "com.amazon.tv.settings.v2.tv.applications.ApplicationsActivity",
            listOf(
                Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS,
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
                Settings.ACTION_APPLICATION_SETTINGS
            )
        ),
        SettingsItem(
            "bluetooth",
            "Remotes & Bluetooth",
            "Controllers & speakers",
            "#60A5FA",
            "#2563EB",
            "#1E3A8A",
            "com.amazon.tv.settings.v2.tv.controllers_bluetooth_devices.ControllersAndBluetoothActivity",
            listOf(
                Settings.ACTION_BLUETOOTH_SETTINGS,
                Settings.ACTION_INPUT_METHOD_SETTINGS
            )
        ),
        SettingsItem(
            "device",
            if (isFireTv) "My Fire TV" else "Device & System",
            "About, ADB & restart",
            "#FB923C",
            "#EA580C",
            "#9A3412",
            "com.amazon.tv.settings.v2.tv.device.DeviceActivity",
            if (isFireTv) {
                listOf(Settings.ACTION_DEVICE_INFO_SETTINGS)
            } else {
                listOf(Settings.ACTION_DEVICE_INFO_SETTINGS, Settings.ACTION_SETTINGS)
            }
        ),
        SettingsItem(
            "settings",
            if (isFireTv) "Preferences" else "System Settings",
            "Privacy, language & time",
            "#94A3B8",
            "#475569",
            "#1E293B",
            "com.amazon.tv.settings.v2.tv.preferences.PreferencesActivity",
            if (isFireTv) emptyList() else listOf(Settings.ACTION_SETTINGS)
        ),
        SettingsItem(
            "settings",
            "Accessibility",
            "Services & captions",
            "#F472B6",
            "#DB2777",
            "#831843",
            "com.amazon.tv.settings.v2.tv.accessibility.AccessibilityActivity",
            listOf(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        ),
        SettingsItem(
            "device",
            "Account & Profile",
            if (isFireTv) "Amazon account settings" else "Accounts & sign-in",
            "#FBBF24",
            "#D97706",
            "#78350F",
            "com.amazon.tv.settings.v2.tv.my_account.MyAccountActivity",
            listOf(
                Settings.ACTION_SYNC_SETTINGS,
                Settings.ACTION_ADD_ACCOUNT
            )
        ),
        SettingsItem(
            "display",
            "Screensaver",
            "Start ambient clock now",
            "#38BDF8",
            "#0F172A",
            "#1E293B",
            onCustomClick = { startActivity(Intent(this, ScreensaverActivity::class.java)) }
        )
    )

    companion object {
        private const val FIRE_TV_SETTINGS_PACKAGE = "com.amazon.tv.settings.v2"
        private val BLOCKED_SETTINGS_HOSTS = setOf(
            "com.amazon.tv.launcher",
            "com.amazon.firehomestarter"
        )
    }
}
