package com.dante.sportshuub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import java.util.Locale

// "Ask Sports AI": type a question or use the mic. Answers come from OpenAI,
// grounded on the scoreboard the app already loaded.
class AskAiActivity : FragmentActivity() {

    companion object {
        const val EXTRA_QUERY = "query"
        const val EXTRA_LISTEN = "listen"
    }

    private lateinit var scroll: ScrollView
    private lateinit var messageBox: LinearLayout
    private lateinit var input: EditText
    private lateinit var micButton: TextView
    private lateinit var speakButton: TextView

    private val history = mutableListOf<ChatTurn>()
    private var busy = false

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speakAnswers = true

    private val prefs by lazy { getSharedPreferences("sports_hub_ai", MODE_PRIVATE) }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is needed for voice questions. You can still type.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speakAnswers = prefs.getBoolean("speak", true)

        setContentView(createScreen())

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
            }
        }

        if (OpenAiClient.isConfigured()) {
            addBubble(
                "Ask me anything about sports. Type below, or press the microphone button and speak.",
                user = false
            )
        } else {
            addBubble(
                "The AI isn't set up yet. Add OPENAI_API_KEY to local.properties, sync Gradle, and rebuild.",
                user = false
            )
        }

        val query = intent.getStringExtra(EXTRA_QUERY)
        if (!query.isNullOrBlank()) {
            ask(query)
        } else if (intent.getBooleanExtra(EXTRA_LISTEN, false)) {
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // Some remotes send a search / voice key while this screen is open.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST) {
            startListening()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // =====================================================
    // ASKING
    // =====================================================

    private fun ask(question: String) {

        val text = question.trim()
        if (text.isEmpty() || busy) return

        tts?.stop()

        addBubble(text, user = true)
        history.add(ChatTurn("user", text))

        if (!OpenAiClient.isConfigured()) {
            addBubble(
                "The AI isn't set up yet. Add OPENAI_API_KEY to local.properties.",
                user = false
            )
            history.removeAt(history.lastIndex)
            return
        }

        busy = true

        val thinking = addBubble("Thinking…", user = false)
        val turns = history.takeLast(8)

        Thread {
            val result = runCatching {
                OpenAiClient.ask(turns, SportsAiContext.build())
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                busy = false

                result.onSuccess { answer ->
                    thinking.text = answer
                    history.add(ChatTurn("assistant", answer))
                    speak(answer)
                }

                result.onFailure { error ->
                    thinking.text = "Sorry. ${error.message ?: "Something went wrong."}"
                    history.removeAt(history.lastIndex)
                }

                scrollToEnd()
            }
        }.start()
    }

    private fun speak(text: String) {
        if (speakAnswers && ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "answer")
        }
    }

    // =====================================================
    // VOICE
    // =====================================================

    private val listener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (!partial.isNullOrBlank()) {
                input.setText(partial)
            }
        }

        override fun onResults(results: Bundle?) {
            finishListening()

            val spoken = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            input.setText("")

            if (!spoken.isNullOrBlank()) {
                ask(spoken)
            }
        }

        override fun onError(error: Int) {
            finishListening()
            input.setText("")

            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "I didn't catch that. Press the mic and try again."
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Voice recognition needs an internet connection."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Microphone permission is missing."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "Voice recognition is busy. Try again in a moment."
                else -> "Voice recognition had a problem (code $error)."
            }

            Toast.makeText(this@AskAiActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startListening() {

        if (listening) {
            recognizer?.stopListening()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Voice recognition isn't available here. Type your question; the keyboard's mic key may work.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        tts?.stop()

        val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }

        val recognize = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        listening = true
        micButton.text = "🎤  Listening…"
        engine.startListening(recognize)
    }

    private fun finishListening() {
        listening = false
        micButton.text = "🎤"
    }

    // =====================================================
    // SCREEN
    // =====================================================

    private fun createScreen(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(TvUi.backgroundColor)
        root.setPadding(dp(50), dp(30), dp(50), dp(24))

        // ---- Header ----

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        header.addView(
            chip("←  BACK") { finish() },
            LinearLayout.LayoutParams(-2, dp(48)).apply { setMargins(0, 0, dp(20), 0) }
        )

        val title = TextView(this)

        title.text = "ASK SPORTS AI"
        title.textSize = 28f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)

        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        speakButton = chip(speakLabel()) {
            speakAnswers = !speakAnswers
            prefs.edit().putBoolean("speak", speakAnswers).apply()
            speakButton.text = speakLabel()
            if (!speakAnswers) tts?.stop()
        }

        header.addView(speakButton, LinearLayout.LayoutParams(-2, dp(48)))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(60)))

        // ---- Conversation ----

        messageBox = LinearLayout(this)
        messageBox.orientation = LinearLayout.VERTICAL
        messageBox.setPadding(0, dp(8), 0, dp(8))

        scroll = ScrollView(this)
        scroll.isFocusable = false
        scroll.addView(messageBox)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---- Input row ----

        val inputRow = LinearLayout(this)

        inputRow.orientation = LinearLayout.HORIZONTAL
        inputRow.gravity = Gravity.CENTER_VERTICAL
        inputRow.setPadding(0, dp(10), 0, 0)

        input = EditText(this)

        input.hint = "Type a question…"
        input.setHintTextColor(TvUi.gray)
        input.setTextColor(Color.WHITE)
        input.textSize = 18f
        input.setSingleLine()
        input.imeOptions = EditorInfo.IME_ACTION_SEND
        input.setPadding(dp(18), 0, dp(18), 0)
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.background = GradientDrawable().apply {
            setColor(TvUi.cardColor)
            cornerRadius = dp(24).toFloat()
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendTyped()
                true
            } else {
                false
            }
        }

        inputRow.addView(
            input,
            LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, 0, dp(12), 0) }
        )

        inputRow.addView(
            chip("ASK") { sendTyped() },
            LinearLayout.LayoutParams(-2, dp(52)).apply { setMargins(0, 0, dp(12), 0) }
        )

        micButton = chip("🎤") { startListening() }

        inputRow.addView(micButton, LinearLayout.LayoutParams(-2, dp(52)))

        root.addView(inputRow, LinearLayout.LayoutParams(-1, -2))

        return root
    }

    private fun sendTyped() {
        val text = input.text.toString()
        input.setText("")
        ask(text)
    }

    private fun speakLabel(): String =
        if (speakAnswers) "🔊  SPEAK: ON" else "🔇  SPEAK: OFF"

    private fun addBubble(text: String, user: Boolean): TextView {

        val view = TextView(this)

        view.text = text
        view.textSize = 18f
        view.setTextColor(Color.WHITE)
        view.setPadding(dp(18), dp(12), dp(18), dp(12))
        view.maxWidth = (resources.displayMetrics.widthPixels * 0.7f).toInt()

        // Focusable so the remote can scroll back through a long conversation.
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        val fill = if (user) Color.rgb(40, 70, 115) else TvUi.cardColor

        fun style(focused: Boolean) {
            view.background = GradientDrawable().apply {
                setColor(fill)
                cornerRadius = dp(16).toFloat()
                if (focused) setStroke(dp(2), Color.WHITE)
            }
        }

        style(false)
        view.setOnFocusChangeListener { _, hasFocus -> style(hasFocus) }

        messageBox.addView(
            view,
            LinearLayout.LayoutParams(-2, -2).apply {
                gravity = if (user) Gravity.END else Gravity.START
                setMargins(0, dp(6), 0, dp(6))
            }
        )

        scrollToEnd()

        return view
    }

    private fun scrollToEnd() {
        scroll.post { scroll.smoothScrollTo(0, messageBox.height) }
    }

    private fun chip(text: String, onClick: () -> Unit): TextView {

        val view = TextView(this)

        view.text = text
        view.textSize = 16f
        view.typeface = Typeface.DEFAULT_BOLD
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(22), 0, dp(22), 0)
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        fun style(focused: Boolean) {
            view.background = GradientDrawable().apply {
                setColor(Color.rgb(38, 38, 43))
                cornerRadius = dp(26).toFloat()
                if (focused) setStroke(dp(3), Color.WHITE)
            }
        }

        style(false)
        view.setOnFocusChangeListener { _, hasFocus -> style(hasFocus) }
        view.setOnClickListener { onClick() }

        return view
    }

    private fun dp(value: Int): Int = TvUi.dp(this, value)
}
