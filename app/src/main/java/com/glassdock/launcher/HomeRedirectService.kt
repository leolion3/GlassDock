package com.glassdock.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * TV-agnostic AccessibilityService that intercepts stock launcher appearances across
 * Fire OS, Android TV, Google TV, and OEM TV launchers and redirects back to Glass Dock.
 */
class HomeRedirectService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val overlayActive = AtomicBoolean(false)

    @Volatile
    private var stockLauncherPackages: Set<String> = KNOWN_STOCK_LAUNCHERS

    private val verifyRedirect = Runnable {
        if (shouldVerifyRedirect()) {
            launchGlassDock()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        stockLauncherPackages = resolveStockLauncherPackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isWindowStateEvent(event)) return
        if (isTemporarilyPaused()) return

        val pkg = event?.packageName?.toString() ?: return
        if (!isStockLauncher(pkg)) return

        handleStockLauncherEvent(event.className?.toString().orEmpty())
    }

    override fun onInterrupt() {
        handler.removeCallbacks(verifyRedirect)
    }

    override fun onDestroy() {
        handler.removeCallbacks(verifyRedirect)
        super.onDestroy()
    }

    private fun handleStockLauncherEvent(className: String) {
        if (isAllowedOverlay(className)) {
            overlayActive.set(true)
            handler.removeCallbacks(verifyRedirect)
            return
        }
        overlayActive.set(false)
        launchGlassDock()
        scheduleVerificationChecks()
    }

    private fun scheduleVerificationChecks() {
        handler.removeCallbacks(verifyRedirect)
        handler.postDelayed(verifyRedirect, FIRST_VERIFY_DELAY_MS)
        handler.postDelayed(verifyRedirect, SECOND_VERIFY_DELAY_MS)
    }

    private fun shouldVerifyRedirect(): Boolean {
        if (overlayActive.get() || isTemporarilyPaused()) return false
        val activePkg = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
        return activePkg != null && isStockLauncher(activePkg)
    }

    private fun isWindowStateEvent(event: AccessibilityEvent?): Boolean {
        return event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    private fun isTemporarilyPaused(): Boolean {
        return SystemClock.elapsedRealtime() < pausedUntilMs.get()
    }

    private fun isStockLauncher(pkg: String): Boolean {
        return pkg != packageName && stockLauncherPackages.contains(pkg)
    }

    private fun isAllowedOverlay(className: String): Boolean {
        return ALLOWED_OVERLAY_MARKERS.any { className.contains(it, ignoreCase = true) }
    }

    private fun resolveStockLauncherPackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved =
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val discovered = resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
        return (KNOWN_STOCK_LAUNCHERS + discovered) - packageName
    }

    private fun launchGlassDock() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        startActivity(intent)
    }

    companion object {
        private const val FIRST_VERIFY_DELAY_MS = 350L
        private const val SECOND_VERIFY_DELAY_MS = 850L
        private const val DEFAULT_PAUSE_MS = 3_000L

        private val KNOWN_STOCK_LAUNCHERS = setOf(
            "com.amazon.tv.launcher",
            "com.amazon.firehomestarter",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher",
            "com.mitv.tvhome",
            "com.sony.dtv.homelauncher"
        )

        private val ALLOWED_OVERLAY_MARKERS = listOf(
            "Setting",
            "Menu",
            "Dialog",
            "Overlay",
            "Quick",
            "Preferences",
            "Wifi",
            "Bluetooth",
            "Accessibility"
        )

        private val pausedUntilMs = AtomicLong(0L)

        /**
         * Temporarily suspends Home redirection (e.g., when opening a system Settings
         * screen from SettingsHubActivity).
         *
         * @param durationMs Duration in milliseconds to pause redirection.
         */
        fun pauseRedirectTemporarily(durationMs: Long = DEFAULT_PAUSE_MS) {
            pausedUntilMs.set(SystemClock.elapsedRealtime() + durationMs.coerceAtLeast(0L))
        }
    }
}
