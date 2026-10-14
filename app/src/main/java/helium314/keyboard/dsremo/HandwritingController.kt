package helium314.keyboard.dsremo

import android.view.KeyEvent
import android.view.View
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs

object HandwritingController {

    private fun overlayOf(inputView: View?): HandwritingOverlay? =
        inputView?.findViewById(R.id.dsremo_handwriting_overlay)

    @JvmStatic
    fun isShowing(inputView: View?): Boolean =
        overlayOf(inputView)?.visibility == View.VISIBLE

    @JvmStatic
    fun hide(inputView: View?): Boolean {
        val overlay = overlayOf(inputView) ?: return false
        if (overlay.visibility != View.VISIBLE) return false
        overlay.dismiss()
        return true
    }

    @JvmStatic
    fun toggle(latinIME: LatinIME, inputView: View?) {
        val root = inputView ?: return
        val overlay = overlayOf(root) ?: return
        if (overlay.visibility == View.VISIBLE) {
            overlay.dismiss()
            return
        }
        val prefs = latinIME.prefs()
        if (!prefs.getBoolean(Settings.PREF_HANDWRITING_ENABLED, Defaults.PREF_HANDWRITING_ENABLED)) return
        val followSubtype = prefs.getBoolean(Settings.PREF_HANDWRITING_FOLLOW_SUBTYPE, Defaults.PREF_HANDWRITING_FOLLOW_SUBTYPE)
        val languageTag = if (followSubtype) {
            helium314.keyboard.latin.RichInputMethodManager.getInstance().currentSubtypeLocale.toLanguageTag()
        } else {
            prefs.getString(Settings.PREF_HANDWRITING_LANGUAGE, Defaults.PREF_HANDWRITING_LANGUAGE)
                ?: Defaults.PREF_HANDWRITING_LANGUAGE
        }
        overlay.bind(object : HandwritingOverlay.Listener {
            override fun onRecognisedText(text: String) {
                val connection = latinIME.currentInputConnection ?: return
                connection.commitText("$text ", 1)
            }
            override fun onBackspace() {
                val connection = latinIME.currentInputConnection ?: return
                connection.deleteSurroundingText(1, 0)
            }
            override fun onSpace() {
                val connection = latinIME.currentInputConnection ?: return
                connection.commitText(" ", 1)
            }
            override fun onNewline() {
                val connection = latinIME.currentInputConnection ?: return
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            override fun onDone() {
                overlay.dismiss()
            }
        }, languageTag)
        overlay.showOver(root.findViewById(R.id.main_keyboard_frame))
    }
}
