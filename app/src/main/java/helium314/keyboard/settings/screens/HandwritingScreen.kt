// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.previewDark
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.settings.preferences.ListPreference

@Composable
fun HandwritingScreen(
    onClickBack: () -> Unit,
) {
    val prefs = LocalContext.current.prefs()
    val recomposeTrigger = (LocalContext.current.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((recomposeTrigger?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
    val enabled = prefs.getBoolean(Settings.PREF_HANDWRITING_ENABLED, Defaults.PREF_HANDWRITING_ENABLED)
    val followSubtype = prefs.getBoolean(Settings.PREF_HANDWRITING_FOLLOW_SUBTYPE, Defaults.PREF_HANDWRITING_FOLLOW_SUBTYPE)
    val items = listOf(
        Settings.PREF_HANDWRITING_ENABLED,
        if (enabled) Settings.PREF_HANDWRITING_FOLLOW_SUBTYPE else null,
        if (enabled && !followSubtype) Settings.PREF_HANDWRITING_LANGUAGE else null,
        if (enabled) Settings.PREF_HANDWRITING_KEEP_MODEL else null,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_handwriting),
        settings = items
    )
}

fun createHandwritingSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_HANDWRITING_ENABLED, R.string.handwriting_enabled, R.string.handwriting_enabled_summary) {
        SwitchPreference(it, Defaults.PREF_HANDWRITING_ENABLED)
    },
    Setting(context, Settings.PREF_HANDWRITING_FOLLOW_SUBTYPE, R.string.handwriting_follow_subtype, R.string.handwriting_follow_subtype_summary) {
        SwitchPreference(it, Defaults.PREF_HANDWRITING_FOLLOW_SUBTYPE)
    },
    Setting(context, Settings.PREF_HANDWRITING_LANGUAGE, R.string.handwriting_recogniser_language, R.string.handwriting_recogniser_language_summary) {
        ListPreference(it, handwritingLanguageItems, Defaults.PREF_HANDWRITING_LANGUAGE)
    },
    Setting(context, Settings.PREF_HANDWRITING_KEEP_MODEL, R.string.handwriting_keep_model, R.string.handwriting_keep_model_summary) {
        SwitchPreference(it, Defaults.PREF_HANDWRITING_KEEP_MODEL)
    },
)

private val handwritingLanguageItems = listOf(
    "English" to "en-US",
    "हिन्दी (Hindi)" to "hi-Deva",
    "मराठी (Marathi)" to "mr-Deva",
    "বাংলা (Bengali)" to "bn-Beng",
    "ગુજરાતી (Gujarati)" to "gu-Gujr",
    "ਪੰਜਾਬੀ (Punjabi)" to "pa-Guru",
    "தமிழ் (Tamil)" to "ta",
    "తెలుగు (Telugu)" to "te",
    "ಕನ್ನಡ (Kannada)" to "kn",
    "മലയാളം (Malayalam)" to "ml",
    "اردو (Urdu)" to "ur",
)

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            HandwritingScreen { }
        }
    }
}
