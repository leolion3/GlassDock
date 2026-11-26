package com.glassdock.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class HomeRedirectService : AccessibilityService() {

    private companion object {
        const val AMAZON_LAUNCHER_PKG = "com.amazon.tv.launcher"
        val ALLOWED_OVERLAY_MARKERS = listOf("Setting", "Menu", "Dialog", "Overlay", "Quick")
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlayActive = false

    private val verifyRedirect = Runnable {
        if (!overlayActive && isAmazonLauncherForeground()) {
            launchGlassDock()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != AMAZON_LAUNCHER_PKG) return

        val cls = event.className?.toString().orEmpty()
        if (ALLOWED_OVERLAY_MARKERS.any { cls.contains(it, ignoreCase = true) }) {
            overlayActive = true
            handler.removeCallbacks(verifyRedirect)
            return
        }

        overlayActive = false
        launchGlassDock()

        // Re-verify after rapid Home-button presses settle
        handler.removeCallbacks(verifyRedirect)
        handler.postDelayed(verifyRedirect, 350L)
        handler.postDelayed(verifyRedirect, 850L)
    }

    override fun onInterrupt() = handler.removeCallbacks(verifyRedirect)

    override fun onDestroy() {
        handler.removeCallbacks(verifyRedirect)
        super.onDestroy()
    }

    private fun isAmazonLauncherForeground(): Boolean {
        val activePkg = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
        return activePkg == AMAZON_LAUNCHER_PKG
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
}
