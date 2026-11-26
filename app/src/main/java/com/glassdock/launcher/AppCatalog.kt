package com.glassdock.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

data class AppSpec(
    val id: String,
    val label: String,
    val category: String,
    val tagline: String,
    val accentHex: String,
    val tileTopHex: String,
    val tileBottomHex: String,
    val cardTopHex: String = "#162033",
    val cardBottomHex: String = "#0B101A",
    val packages: List<String>
)

object AppCatalog {

    val dockApps = listOf(
        AppSpec(
            "prime", "Prime Video", "Streaming", "Movies, originals & live sports",
            "#00A8E1", "#00A8E1", "#006A9E", "#0C3547", "#061520",
            listOf("com.amazon.firebat", "com.amazon.avod", "com.amazon.avod.thirdpartyclient")
        ),
        AppSpec(
            "disney", "Disney+", "Streaming", "Disney, Pixar, Marvel & Star Wars",
            "#3B82F6", "#143EB8", "#0A1D56", "#102A6B", "#07102E",
            listOf("com.disney.disneyplus")
        ),
        AppSpec(
            "netflix", "Netflix", "Streaming", "Series, films & documentaries",
            "#E50914", "#1A1A1E", "#0D0D10", "#4A0B0E", "#1A0506",
            listOf("com.netflix.ninja")
        ),
        AppSpec(
            "youtube", "YouTube", "Video", "Subscriptions & recommended videos",
            "#FF3B30", "#FF1A1A", "#B80000", "#4A1212", "#1C0707",
            listOf("com.amazon.firetv.youtube", "com.google.android.youtube.tv")
        )
    )

    val coreLibraryApps = dockApps + listOf(
        AppSpec(
            "zdf", "ZDF Mediathek", "Mediathek", "Public TV & documentaries",
            "#FA7D19", "#FA7D19", "#C45600",
            packages = listOf("com.zdf.android.mediathek", "de.zdf.android.mediathek")
        ),
        AppSpec(
            "ard", "ARD Mediathek", "Mediathek", "Das Erste & regional channels",
            "#3B82F6", "#0044CC", "#00227A",
            packages = listOf("de.swr.ard.avp.mobile.android.amazon", "de.swr.avp.ard.tv")
        ),
        AppSpec(
            "expressvpn", "ExpressVPN", "VPN", "Encrypted network tunnel",
            "#DA3940", "#DA3940", "#9E1C22",
            packages = listOf("com.expressvpn.vpn")
        )
    )

    private val extraBrandMatchers = listOf(
        BrandRule("plex", "Media Server", "#E5A00D", "#282A2D", "#15171A", listOf("plex")),
        BrandRule("jellyfin", "Media Server", "#AA5CC3", "#00A4DC", "#AA5CC3", listOf("jellyfin")),
        BrandRule("kodi", "Media Center", "#17B2E7", "#17B2E7", "#0E6B8F", listOf("kodi", "xbmc")),
        BrandRule("arte", "Mediathek", "#FA481C", "#FA481C", "#B82A08", listOf("arte")),
        BrandRule("spotify", "Music", "#1DB954", "#1DB954", "#117A37", listOf("spotify")),
        BrandRule("twitch", "Live", "#9146FF", "#9146FF", "#5B24B8", listOf("twitch")),
        BrandRule("joyn", "Streaming", "#00E5A0", "#111827", "#064E3B", listOf("joyn")),
        BrandRule("rtl", "Streaming", "#E11D48", "#E11D48", "#881337", listOf("rtlplus", "tvnow")),
        BrandRule("vlc", "Player", "#FF8800", "#FF8800", "#B35900", listOf("videolan", "vlc")),
        BrandRule("stremio", "Streaming", "#8A5AAB", "#7B5BF5", "#432889", listOf("stremio")),
        BrandRule(
            "vpn",
            "VPN",
            "#38BDF8",
            "#1E3A8A",
            "#0F172A",
            listOf("nordvpn", "surfshark", "mullvad", "wireguard", "tailscale", "vpn")
        )
    )

    private val fallbackPalettes = listOf(
        Triple("#38BDF8", "#1E3A5F", "#0F2038"),
        Triple("#A78BFA", "#3B2667", "#1F143A"),
        Triple("#34D399", "#134E4A", "#0A2C2A"),
        Triple("#FB923C", "#6B2F12", "#3A1909"),
        Triple("#F472B6", "#641E44", "#360F24"),
        Triple("#60A5FA", "#1D3557", "#0E1B2E")
    )

    private data class BrandRule(
        val iconId: String,
        val category: String,
        val accentHex: String,
        val tileTopHex: String,
        val tileBottomHex: String,
        val keywords: List<String>
    )

    fun queryInstalledApps(
        pm: PackageManager,
        excluded: Set<String>
    ): Pair<List<AppSpec>, List<AppSpec>> {
        val discovered = linkedMapOf<String, String>()
        val queries = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        for (query in queries) {
            pm.queryIntentActivities(query, 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg !in excluded && pkg !in discovered) {
                    discovered[pkg] = info.loadLabel(pm).toString()
                }
            }
        }

        val promoted = mutableListOf<AppSpec>()
        val others = mutableListOf<AppSpec>()

        discovered.entries
            .sortedBy { it.value.lowercase() }
            .forEach { (pkg, label) ->
                val spec = resolveDynamicSpec(pkg, label)
                if (spec.category != "App") promoted.add(spec) else others.add(spec)
            }

        return promoted to others
    }

    fun launch(context: Context, app: AppSpec) {
        val pm = context.packageManager
        val intent = app.packages.firstNotNullOfOrNull { pkg ->
            pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        if (app.id == "prime" && launchPrimeFallback(context)) return
        Toast.makeText(context, "${app.label} is not installed", Toast.LENGTH_SHORT).show()
    }

    private val systemPrefixes = listOf("com.amazon.", "com.android.", "android.")

    private val mediathekKeywords = listOf(
        "mediathek", "3sat", "tagesschau", "kika", "waipu", "zattoo", "magentatv", "dazn"
    )

    private fun resolveDynamicSpec(pkg: String, label: String): AppSpec {
        val lowerPkg = pkg.lowercase()
        val palette = fallbackPalettes[(pkg.hashCode() and 0x7fffffff) % fallbackPalettes.size]

        // Keep Amazon/Android system utilities tucked under "Other apps"
        if (systemPrefixes.any { lowerPkg.startsWith(it) }) {
            return buildGenericSpec(pkg, label, "generic", "App", "System & other", palette)
        }

        val haystack = "$lowerPkg ${label.lowercase()}"
        val matched = extraBrandMatchers.firstOrNull { rule ->
            rule.keywords.any { haystack.contains(it) }
        }
        if (matched != null) {
            return AppSpec(
                id = matched.iconId,
                label = label,
                category = matched.category,
                tagline = matched.category,
                accentHex = matched.accentHex,
                tileTopHex = matched.tileTopHex,
                tileBottomHex = matched.tileBottomHex,
                packages = listOf(pkg)
            )
        }

        val isMediathek = mediathekKeywords.any { haystack.contains(it) }
        return if (isMediathek) {
            buildGenericSpec(pkg, label, "mediathek", "Mediathek", "Streaming & TV", palette)
        } else {
            buildGenericSpec(pkg, label, "generic", "App", "Installed app", palette)
        }
    }

    private fun buildGenericSpec(
        pkg: String,
        label: String,
        iconId: String,
        category: String,
        tagline: String,
        palette: Triple<String, String, String>
    ): AppSpec = AppSpec(
        id = iconId,
        label = label,
        category = category,
        tagline = tagline,
        accentHex = palette.first,
        tileTopHex = palette.second,
        tileBottomHex = palette.third,
        packages = listOf(pkg)
    )

    private fun launchPrimeFallback(context: Context): Boolean = runCatching {
        context.startActivity(Intent().apply {
            component = ComponentName(
                "com.amazon.firebat",
                "com.amazon.firebatcore.deeplink.DeepLinkRoutingActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.isSuccess
}
