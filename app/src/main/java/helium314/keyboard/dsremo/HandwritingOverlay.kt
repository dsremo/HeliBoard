package helium314.keyboard.dsremo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class HandwritingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    interface Listener {
        fun onRecognisedText(text: String)
        fun onBackspace()
        fun onSpace()
        fun onNewline()
        fun onDone()
    }

    private var listener: Listener? = null
    private val canvasView = StrokePadView(context)
    private val hintView: TextView
    private var languageTag: String = "hi"
    private var busy = false

    init {
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        hintView = TextView(context).apply {
            setTextColor(Color.parseColor("#DDDDDD"))
            textSize = 13f
            setPadding(dp(4), 0, dp(4), dp(4))
        }
        canvasView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(180)
        ).apply { weight = 1f }
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val clear = makeButton("Clear") { canvasView.clearInk() }
        val space = makeButton("Space") { listener?.onSpace() }
        val enter = makeButton("↵") { listener?.onNewline() }
        val back = makeButton("⌫") { listener?.onBackspace() }
        val done = makeButton("Done") { listener?.onDone() }
        val send = makeButton("Send") { recogniseAndEmit() }
        for (button in listOf(clear, space, enter, back, done, send)) {
            (button.layoutParams as LinearLayout.LayoutParams).weight = 1f
            buttons.addView(button)
        }
        root.addView(hintView)
        root.addView(canvasView)
        root.addView(buttons)
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isClickable = true
        isFocusable = true
    }

    fun showOver(keyboardFrame: View?) {
        val frameHeight = keyboardFrame?.height ?: 0
        android.util.Log.d("DsremoHandwrite", "showOver frameHeight=$frameHeight")
        val params = layoutParams
        if (params != null) {
            params.height = if (frameHeight > 0) frameHeight else ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = params
        }
        canvasView.clearInk()
        busy = false
        visibility = View.VISIBLE
    }

    fun dismiss() {
        visibility = View.GONE
        canvasView.clearInk()
        busy = false
        listener = null
    }

    fun bind(listener: Listener, languageTag: String) {
        this.listener = listener
        this.languageTag = languageTag
        hintView.text = hintFor(languageTag)
    }

    private fun hintFor(tag: String): String {
        val language = tag.substringBefore('-').lowercase(java.util.Locale.ROOT)
        val prompt = when (language) {
            "hi", "mr", "ne" -> "देवनागरी में लिखें"
            "bn" -> "বাংলায় লিখুন"
            "ta" -> "தமிழில் எழுதுங்கள்"
            "te" -> "తెలుగులో రాయండి"
            "gu" -> "ગુજરાતીમાં લખો"
            "kn" -> "ಕನ್ನಡದಲ್ಲಿ ಬರೆಯಿರಿ"
            "ml" -> "മലയാളത്തിൽ എഴുതുക"
            "pa" -> "ਪੰਜਾਬੀ ਵਿੱਚ ਲਿਖੋ"
            else -> "Write here"
        }
        return "$prompt  ·  Clear to erase, Send to insert, Done to close"
    }

    private fun makeButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E2E2E"))
            gravity = android.view.Gravity.CENTER
            textSize = 15f
            setPadding(dp(6), dp(10), dp(6), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun recogniseAndEmit() {
        if (busy) return
        val ink = canvasView.buildInk()
        if (ink == null) {
            showStatus("Write something first")
            return
        }
        busy = true
        showStatus("Recognising")
        HandwritingRecogniser.recognise(context, languageTag, ink, { status -> showStatus(status) }) { result ->
            post {
                busy = false
                if (!result.isNullOrEmpty()) {
                    listener?.onRecognisedText(result)
                    canvasView.clearInk()
                    hintView.text = hintFor(languageTag)
                }
            }
        }
    }

    private fun showStatus(message: String) {
        post { hintView.text = message }
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()

    private class StrokePadView(context: Context) : View(context) {
        private val strokes = mutableListOf<Stroke>()
        private var currentPath: Path? = null
        private var currentStroke: Stroke? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(3f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.parseColor("#252525"))
            strokes.forEach { canvas.drawPath(it.path, paint) }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val touchX = event.x
            val touchY = event.y
            val nowMs = System.currentTimeMillis()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val path = Path().apply { moveTo(touchX, touchY) }
                    val stroke = Stroke(path, mutableListOf(InkPoint(touchX, touchY, nowMs)))
                    currentPath = path
                    currentStroke = stroke
                    strokes.add(stroke)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(touchX, touchY)
                    currentStroke?.points?.add(InkPoint(touchX, touchY, nowMs))
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentPath = null
                    currentStroke = null
                }
            }
            return true
        }

        fun clearInk() {
            strokes.clear()
            invalidate()
        }

        fun buildInk(): List<Stroke>? = if (strokes.isEmpty()) null else strokes.toList()

        private fun dpToPx(value: Float): Float = resources.displayMetrics.density * value
    }

    data class InkPoint(val xPx: Float, val yPx: Float, val timeMs: Long)
    class Stroke(val path: Path, val points: MutableList<InkPoint>)
}
