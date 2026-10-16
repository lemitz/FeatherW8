package com.featherw8.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.featherw8.player.ui.artwork.CustomArtworkStore
import com.featherw8.player.ui.FeatherW8App
import com.featherw8.player.ui.theme.AccentOptions
import com.featherw8.player.ui.theme.detectDeviceProfile
import com.featherw8.player.ui.theme.DeviceProfile
import com.featherw8.player.ui.theme.FeatherW8Theme

class MainActivity : ComponentActivity() {
    var onAppEntry: (() -> Unit)? = null

    /** A file another app asked us to play ("Open with"); cleared once playback has been started. */
    private var viewIntentUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomArtworkStore.load(this)
        request120HzIfAvailable()
        // A recreated activity (rotation, process restart) must not replay the original intent.
        if (savedInstanceState == null) viewIntentUri = intent.viewUri()
        setContent {
            val profile = remember { detectDeviceProfile() }
            val preferences = remember { getSharedPreferences("featherw8", Context.MODE_PRIVATE) }
            var dotFontEnabled by rememberSaveable {
                mutableStateOf(profile == DeviceProfile.NOTHING && preferences.getBoolean("nothing_dot_font_enabled", true))
            }
            var accentId by rememberSaveable {
                val saved = preferences.getString("accent_color", "red") ?: "red"
                mutableStateOf(AccentOptions.firstOrNull { it.id == saved }?.id ?: "red")
            }
            FeatherW8Theme(profile, dotFontEnabled, accentId) {
                FeatherW8App(
                    profile = profile,
                    dotFontEnabled = dotFontEnabled,
                    onDotFontChange = { enabled ->
                        dotFontEnabled = enabled
                        preferences.edit().putBoolean("nothing_dot_font_enabled", enabled).apply()
                    },
                    accentId = accentId,
                    onAccentChange = { id ->
                        accentId = id
                        preferences.edit().putString("accent_color", id).apply()
                    },
                    openUri = viewIntentUri,
                    onOpenUriHandled = { viewIntentUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewIntentUri = intent.viewUri()
    }

    private fun Intent.viewUri(): Uri? {
        if (action != Intent.ACTION_VIEW) return null
        if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        val uri = data ?: return null
        // Keep access across a restart of the playback service where the sender allows it.
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return uri
    }

    override fun onStart() {
        super.onStart()
        onAppEntry?.invoke()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { request120HzIfAvailable(nothingOnly = true) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.post { request120HzIfAvailable(nothingOnly = true) }
    }

    @Suppress("DEPRECATION")
    private fun request120HzIfAvailable(nothingOnly: Boolean = false) {
        if (nothingOnly && detectDeviceProfile() != DeviceProfile.NOTHING) return
        val display = windowManager.defaultDisplay
        val currentMode = display.mode
        val sameResolutionModes = display.supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
        }
        val preferredMode = sameResolutionModes
            .filter { it.refreshRate >= 90f }
            .minByOrNull { kotlin.math.abs(it.refreshRate - 120f) }
        val attributes = window.attributes
        attributes.preferredRefreshRate = preferredMode?.refreshRate ?: 120f
        if (preferredMode != null) attributes.preferredDisplayModeId = preferredMode.modeId
        window.attributes = attributes
    }
}
