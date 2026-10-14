package helium314.keyboard.dsremo

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs

object HandwritingRecogniser {
    private const val TAG = "DsremoHandwrite"

    private val modelManager = RemoteModelManager.getInstance()

    fun recognise(
        context: Context,
        languageTag: String,
        strokes: List<HandwritingOverlay.Stroke>,
        onStatus: (String) -> Unit,
        onResult: (String?) -> Unit,
    ) {
        val identifier = resolveModelIdentifier(languageTag)
        if (identifier == null) {
            onStatus("No handwriting model for this language")
            onResult(null)
            return
        }
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        modelManager.isModelDownloaded(model).addOnSuccessListener { downloaded ->
            if (downloaded) {
                runRecognition(model, strokes, onStatus, onResult)
            } else {
                onStatus("Downloading handwriting model, one time only")
                modelManager.download(model, DownloadConditions.Builder().build())
                    .addOnSuccessListener { runRecognition(model, strokes, onStatus, onResult) }
                    .addOnFailureListener { throwable ->
                        Log.w(TAG, "model download failed: ${throwable.message}")
                        onStatus("Model download failed, check your connection")
                        onResult(null)
                    }
            }
        }.addOnFailureListener { throwable ->
            Log.w(TAG, "model status check failed: ${throwable.message}")
            onStatus("Handwriting is unavailable right now")
            onResult(null)
        }
    }

    private fun resolveModelIdentifier(languageTag: String): DigitalInkRecognitionModelIdentifier? {
        val normalisedTag = when {
            languageTag.startsWith("hi") -> "hi-Deva"
            languageTag.startsWith("mr") -> "mr-Deva"
            languageTag.startsWith("bn") -> "bn-Beng"
            languageTag.startsWith("ta") -> "ta"
            languageTag.startsWith("te") -> "te"
            languageTag.startsWith("kn") -> "kn"
            languageTag.startsWith("ml") -> "ml"
            languageTag.startsWith("gu") -> "gu-Gujr"
            languageTag.startsWith("pa") -> "pa-Guru"
            languageTag.startsWith("ur") -> "ur"
            languageTag.startsWith("en") -> "en-US"
            else -> languageTag
        }
        return runCatching {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(normalisedTag)
        }.getOrNull()
    }

    private fun runRecognition(
        model: DigitalInkRecognitionModel,
        strokes: List<HandwritingOverlay.Stroke>,
        onStatus: (String) -> Unit,
        onResult: (String?) -> Unit,
    ) {
        val recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
        val inkBuilder = Ink.builder()
        for (stroke in strokes) {
            val strokeBuilder = Ink.Stroke.builder()
            for (point in stroke.points) {
                strokeBuilder.addPoint(Ink.Point.create(point.xPx, point.yPx, point.timeMs))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        recognizer.recognize(inkBuilder.build())
            .addOnSuccessListener { result ->
                val topCandidate = result.candidates.firstOrNull()?.text
                if (topCandidate.isNullOrEmpty()) onStatus("Could not read that, try writing larger")
                onResult(topCandidate)
                runCatching { recognizer.close() }
            }
            .addOnFailureListener { throwable ->
                Log.w(TAG, "recognition failed: ${throwable.message}")
                onStatus("Could not read that, try again")
                onResult(null)
                runCatching { recognizer.close() }
            }
    }

    fun onFinishInput(context: Context) {
        val keepModel = context.prefs()
            .getBoolean(Settings.PREF_HANDWRITING_KEEP_MODEL, Defaults.PREF_HANDWRITING_KEEP_MODEL)
        if (keepModel) return
        modelManager.getDownloadedModels(DigitalInkRecognitionModel::class.java)
            .addOnSuccessListener { downloaded ->
                downloaded.forEach { model ->
                    modelManager.deleteDownloadedModel(model)
                        .addOnFailureListener { throwable ->
                            Log.w(TAG, "model delete failed: ${throwable.message}")
                        }
                }
            }
            .addOnFailureListener { throwable ->
                Log.w(TAG, "model list failed: ${throwable.message}")
            }
    }
}
