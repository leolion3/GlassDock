package com.glassdock.launcher

import android.content.Context

enum class FavoriteToggleResult {
    PINNED,
    UNPINNED,
    LIMIT_REACHED
}

/**
 * Persists and resolves the user's pinned Home/Dock apps in insertion order.
 */
object FavoritesStore {

    private const val PREFS_NAME = "glassdock_prefs"
    private const val KEY_FAVORITE_KEYS = "favorite_app_keys"
    const val MAX_FAVORITES = 8

    /**
     * Returns a stable unique storage key for an [AppSpec].
     *
     * @param app Application specification from [AppCatalog].
     * @return Primary package name when available, otherwise the spec id.
     */
    fun keyFor(app: AppSpec): String = app.packages.firstOrNull() ?: app.id

    /**
     * Checks whether [app] is currently pinned to the Home screen.
     *
     * @param context Android context used to read SharedPreferences.
     * @param app Application specification to test.
     * @return true if pinned; false otherwise.
     */
    fun isFavorite(context: Context, app: AppSpec): Boolean {
        val activeKeys = readEffectiveKeys(context)
        return matchesAnyKey(app, activeKeys)
    }

    /**
     * Toggles [app] in the persisted favorites list while enforcing [MAX_FAVORITES].
     *
     * Hoare Triple:
     * {P}: context is valid and app has a non-empty key.
     *  C : Loads current effective keys, removes matching entries if present or appends
     *      keyFor(app) when size < MAX_FAVORITES, and persists asynchronously.
     * {Q}: Persisted list size is in [0, MAX_FAVORITES] and returned enum reflects the outcome.
     *
     * @param context Android context used to update SharedPreferences.
     * @param app Application specification to pin or unpin.
     * @return [FavoriteToggleResult] describing whether the app was pinned, unpinned, or rejected.
     */
    fun toggleFavorite(context: Context, app: AppSpec): FavoriteToggleResult {
        val current = readEffectiveKeys(context).toMutableList()
        if (removeMatchingKeys(current, app)) {
            saveKeys(context, current)
            return FavoriteToggleResult.UNPINNED
        }
        if (current.size >= MAX_FAVORITES) {
            return FavoriteToggleResult.LIMIT_REACHED
        }
        current.add(keyFor(app))
        saveKeys(context, current)
        return FavoriteToggleResult.PINNED
    }

    /**
     * Resolves the ordered list of pinned [AppSpec] items from [availableApps],
     * falling back to [AppCatalog.dockApps] if none of the saved apps are available.
     *
     * Hoare Triple:
     * {P}: context is valid and availableApps contains all known/installed AppSpecs.
     *  C : Maps each saved key to the first matching AppSpec in availableApps in saved order.
     * {Q}: Returns a non-empty list (unless both availableApps and dockApps are empty)
     *      with at most MAX_FAVORITES distinct apps.
     *
     * @param context Android context used to read SharedPreferences.
     * @param availableApps Combined list of core and installed apps.
     * @return Ordered list of apps to render on the Home Quick Jump shelf and Glass Dock.
     */
    fun resolvePinnedApps(context: Context, availableApps: List<AppSpec>): List<AppSpec> {
        val activeKeys = readEffectiveKeys(context)
        val resolved =
            activeKeys.mapNotNull { key -> findAppByKey(availableApps, key) }.distinctBy(::keyFor)
        if (resolved.isNotEmpty()) {
            return resolved.take(MAX_FAVORITES)
        }
        return AppCatalog.dockApps.take(MAX_FAVORITES)
    }

    private fun readEffectiveKeys(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FAVORITE_KEYS)) {
            return AppCatalog.dockApps.take(MAX_FAVORITES).map(::keyFor)
        }
        val raw = prefs.getString(KEY_FAVORITE_KEYS, "").orEmpty()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun saveKeys(context: Context, keys: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITE_KEYS, keys.joinToString(","))
            .apply()
    }

    private fun matchesAnyKey(app: AppSpec, keys: List<String>): Boolean {
        return keys.any { key -> isAppMatchingKey(app, key) }
    }

    private fun removeMatchingKeys(keys: MutableList<String>, app: AppSpec): Boolean {
        return keys.removeAll { key -> isAppMatchingKey(app, key) }
    }

    private fun findAppByKey(apps: List<AppSpec>, key: String): AppSpec? {
        return apps.firstOrNull { app -> isAppMatchingKey(app, key) }
    }

    private fun isAppMatchingKey(app: AppSpec, key: String): Boolean {
        return app.packages.contains(key) || app.id == key
    }
}
