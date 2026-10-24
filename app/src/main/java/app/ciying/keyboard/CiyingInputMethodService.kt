package app.ciying.keyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class CiyingInputMethodService : InputMethodService() {
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var dictionary: CandidateDictionary? = null
    private fun dictionary() = dictionary ?: CandidateDictionary(applicationContext).also { dictionary = it }
    private var query: java.util.concurrent.Future<*>? = null
    private var repeatingDelete: Runnable? = null
    private fun stopDeleting() {
        repeatingDelete?.let { handler.removeCallbacks(it) }
        repeatingDelete = null
    }
    private var generation = 0
    private var session = 0
    private var committing = false
    private val pendingKeys = java.util.ArrayDeque<() -> Unit>()
    private val composing = StringBuilder()
    private lateinit var candidates: LinearLayout
    private lateinit var keyArea: LinearLayout
    private var english = false
    private var symbols = false
    private var shifted = false
    private var policy = EditorPolicy(false, false, false)
    private var appearanceMode = "forest"
    private var palette = KeyboardAppearance.palette("forest")
    private val ink get() = palette.ink
    private lateinit var keyboardRoot: LinearLayout
    private var appearanceToken = ""
    private val green get() = if (appearanceMode == "forest") palette.accent else palette.ink
    private fun beforeCursor(): String {
        if (policy.privateInput) return ""
        return currentInputConnection?.getTextBeforeCursor(48, 0)?.toString().orEmpty()
            .removeSuffix(composing.toString()).takeLast(16)
    }
    override fun onEvaluateFullscreenMode() = false

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        stopDeleting()
        super.onStartInput(attribute, restarting)
        session++
        generation++
        committing = false
        pendingKeys.clear()
        composing.clear()
        policy = EditorPolicy.from(attribute?.inputType ?: 1, attribute?.imeOptions ?: 0)
        english = policy.english
        symbols = policy.numeric
        shifted = false
        refreshCandidates()
        if (::keyArea.isInitialized) renderKeys()
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(232, 239, 235))
            setPadding(dp(4), 0, dp(4), dp(8))
        }
        keyboardRoot = root
        appearanceToken = ""
        applyAppearance()
        candidates = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(48) }
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(candidates)
        }, LinearLayout.LayoutParams(-1, -2))
        keyArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keyArea)
        renderKeys()
        refreshCandidates()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyAppearance()
        if (::keyArea.isInitialized) { renderKeys(); refreshCandidates() }
    }
    private fun applyAppearance() {
        val mode = KeyboardAppearance.mode(this)
        val photo = KeyboardAppearance.photo(this)
        val token = "$mode:${photo.lastModified()}"
        if (appearanceToken == token) return
        appearanceToken = token
        appearanceMode = mode
        palette = KeyboardAppearance.palette(mode)
        if (::keyboardRoot.isInitialized) {
            val bitmap = if (mode == "photo") runCatching { android.graphics.BitmapFactory.decodeFile(photo.path) }.getOrNull() else null
            keyboardRoot.background = if (bitmap != null) KeyboardPhotoDrawable(bitmap)
                else android.graphics.drawable.ColorDrawable(palette.background)
        }
        window?.window?.navigationBarColor = palette.background
    }
    private fun renderKeys() {
        stopDeleting()
        keyArea.removeAllViews()
        if (symbols) {
            characterRow("1234567890")
            characterRow("@#￥%&*+-=")
            val third = row()
            third.addView(key("ABC", 1.5f, true) { symbols = false; renderKeys() })
            "（）？！：；".forEach { ch -> third.addView(key(ch.toString()) { commitLiteral(ch.toString()) }) }
            third.addView(deleteKey())
        } else {
            characterRow("qwertyuiop")
            val second = row()
            second.addView(View(this), LinearLayout.LayoutParams(0, 1, 0.5f))
            "asdfghjkl".forEach { addLetter(second, it) }
            second.addView(View(this), LinearLayout.LayoutParams(0, 1, 0.5f))
            val third = row()
            third.addView(key("⇧", 1.5f, true) { shifted = !shifted; renderKeys() }.apply {
                contentDescription = "切换大小写"
                if (shifted) setTextColor(green)
            })
            "zxcvbnm".forEach { addLetter(third, it) }
            third.addView(deleteKey())
        }
        val bottom = row()
        bottom.addView(key(if (symbols) "ABC" else "123", 1.35f, true) {
            commitRaw(); symbols = !symbols; renderKeys(); refreshCandidates()
        })
        bottom.addView(key(if (english) "英" else "中", 1f, true) {
            if (!(policy.privateInput && policy.english)) {
                commitRaw(); english = !english; symbols = false; renderKeys(); refreshCandidates()
            }
        })
        bottom.addView(key(if (english) "," else "，", 0.85f) { commitLiteral(if (english) "," else "，") })
        bottom.addView(key(if (english) "space" else "空格", 3.6f) {
            if (composing.isNotEmpty()) commitCandidate() else currentInputConnection?.commitText(" ", 1)
        })
        bottom.addView(key(if (english) "." else "。", 0.85f) { commitLiteral(if (english) "." else "。") })
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = if (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) EditorInfo.IME_ACTION_NONE
            else options and EditorInfo.IME_MASK_ACTION
        val title = when (action) {
            EditorInfo.IME_ACTION_SEARCH -> "搜索"
            EditorInfo.IME_ACTION_SEND -> "发送"
            EditorInfo.IME_ACTION_DONE -> "完成"
            EditorInfo.IME_ACTION_NEXT -> "下一项"
            else -> "换行"
        }
        bottom.addView(key(title, 1.5f, accent = true) {
            commitCandidate {
                commitRaw()
                if (!sendDefaultEditorAction(true)) sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
        })
    }

    private fun rowHeight() = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 44 else 56
    private fun row() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        keyArea.addView(this, LinearLayout.LayoutParams(-1, dp(rowHeight())))
    }
    private fun characterRow(chars: String) {
        val line = row()
        chars.forEach { ch ->
            if (symbols) line.addView(key(ch.toString()) { commitLiteral(ch.toString()) })
            else addLetter(line, ch)
        }
    }
    private fun addLetter(line: LinearLayout, ch: Char) {
        val letter = if (shifted) ch.uppercaseChar().toString() else ch.toString()
        line.addView(key(letter) { appendPinyin(letter) })
    }
    private fun deleteKey() = key("⌫", 1.5f, true) { delete() }.apply {
        contentDescription = "删除"
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopDeleting()
                    val repeat = object : Runnable {
                        override fun run() {
                            if (repeatingDelete !== this || !view.isPressed) return
                            if (!committing) delete()
                            handler.postDelayed(this, 55L)
                        }
                    }
                    repeatingDelete = repeat
                    handler.postDelayed(repeat, 350L)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.x < 0 || event.y < 0 || event.x >= view.width || event.y >= view.height) {
                        stopDeleting()
                        view.isPressed = false
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    val held = event.eventTime - event.downTime >= 350L
                    stopDeleting()
                    if (held) { view.isPressed = false; true } else false
                }
                MotionEvent.ACTION_CANCEL -> { stopDeleting(); false }
                else -> false
            }
        }
    }
    private fun key(title: String, weight: Float = 1f, utility: Boolean = false,
                    accent: Boolean = false, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = if (title.length > 1) 15f else 23f
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(if (accent && appearanceMode == "forest") Color.WHITE else ink)
        layoutParams = LinearLayout.LayoutParams(0, dp(rowHeight() - 8), weight).apply {
            setMargins(dp(3), dp(4), dp(3), dp(4))
        }
        val fill = if (accent) palette.accent else if (utility) palette.utility else palette.key
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), rounded(palette.pressed))
            addState(intArrayOf(), rounded(fill))
        }
        elevation = dp(1).toFloat()
        setOnClickListener { if (committing) pendingKeys.add(action) else action() }
    }
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(6).toFloat(); if (appearanceMode != "forest") setStroke(dp(1), if (appearanceMode == "white") 0xffdddddd.toInt() else 0xff444444.toInt()) }
    private fun appendPinyin(letter: String) {
        if (english) currentInputConnection?.commitText(letter, 1)
        else if (composing.length < 64) { composing.append(letter.lowercase()); updateComposing(); refreshCandidates() }
    }
    private fun delete() {
        if (composing.isNotEmpty()) {
            composing.deleteAt(composing.lastIndex); updateComposing(); refreshCandidates()
        } else if (!currentInputConnection?.getSelectedText(0).isNullOrEmpty()) currentInputConnection?.commitText("", 1)
        else currentInputConnection?.deleteSurroundingTextInCodePoints(1, 0)
    }
    private fun updateComposing() { currentInputConnection?.setComposingText(composing.toString(), 1) }
    private fun commitRaw() {
        if (composing.isEmpty()) return
        currentInputConnection?.commitText(composing.toString(), 1)
        composing.clear(); refreshCandidates()
    }
    private fun commitCandidate(after: () -> Unit = {}) {
        if (composing.isEmpty()) { after(); return }
        val input = composing.toString()
        val context = beforeCursor()
        val correction = !policy.privateInput
        val currentSession = session
        committing = true
        generation++
        query?.cancel(false)
        worker.submit {
            val first = runCatching { dictionary().lookup(input, context, correction, personalized = correction).firstOrNull() }.getOrNull()
            handler.post {
                if (session != currentSession) return@post
                committing = false
                if (first != null) choose(first) else commitRaw()
                after()
                while (!committing && pendingKeys.isNotEmpty()) pendingKeys.removeFirst().invoke()
            }
        }
    }
    private fun commitLiteral(text: String) { commitCandidate { commitRaw(); currentInputConnection?.commitText(text, 1) } }
    private fun choose(item: Candidate) {
        val input = composing.toString()
        val context = beforeCursor()
        currentInputConnection?.commitText(item.hanzi, 1)
        composing.delete(0, item.consumed.coerceAtMost(composing.length))
        if (composing.isNotEmpty()) updateComposing()
        if (!policy.privateInput) worker.submit { runCatching { dictionary().remember(input, item, context) } }
        refreshCandidates()
    }
    private fun refreshCandidates() {
        if (!::candidates.isInitialized) return
        val request = ++generation
        query?.cancel(false)
        candidates.removeAllViews()
        candidates.orientation = LinearLayout.HORIZONTAL
        candidates.layoutParams = android.widget.FrameLayout.LayoutParams(-2, -2)
        if (composing.isEmpty()) {
            candidates.addView(label(if (policy.privateInput) "私密输入 · 不记录选词" else if (english) "英文输入" else "词映 · 让词语照见世界", ink))
            return
        }
        val input = composing.toString()
        val context = beforeCursor()
        val correction = !policy.privateInput

        query = worker.submit {
            val result = runCatching { dictionary().lookup(input, context, correction, personalized = correction) }
            handler.post {
                if (request != generation || composing.toString() != input) return@post
                if (result.isFailure) candidates.addView(label("词库加载失败，请重启词映", ink))
                val items = result.getOrDefault(emptyList())
                val hasSpecial = items.any { it.gloss == SPECIAL_GAOZIHAO_TEXT }
                if (hasSpecial) {
                    candidates.orientation = LinearLayout.VERTICAL
                    candidates.layoutParams = android.widget.FrameLayout.LayoutParams(resources.displayMetrics.widthPixels - dp(8), -2)
                }
                var candidateRow = candidates
                items.forEach { item ->
                    val special = item.gloss == SPECIAL_GAOZIHAO_TEXT
                    val base = if (special) "${item.hanzi}：${item.gloss}" else if (item.gloss.isBlank()) item.hanzi else "${item.hanzi}   ${item.gloss}"
                    val caption = base + if (item.correctedFrom != null) " · 纠错" else ""
                    if (hasSpecial && !special && candidateRow === candidates) {
                        candidateRow = LinearLayout(this)
                        candidates.addView(HorizontalScrollView(this).apply {
                            isHorizontalScrollBarEnabled = false
                            addView(candidateRow)
                        }, LinearLayout.LayoutParams(-1, dp(48)))
                    }
                    (if (special) candidates else candidateRow).addView(label(caption, ink).apply {
                        if (special) {
                            textSize = 16f
                            setPadding(dp(12), dp(8), dp(12), dp(8))
                            layoutParams = LinearLayout.LayoutParams(resources.displayMetrics.widthPixels - dp(8), -2)
                            text = android.text.SpannableString(caption).apply {
                                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, item.hanzi.length, 0)
                            }
                        }
                        contentDescription = "${item.hanzi} ${item.gloss}"
                        setOnClickListener { if (!committing && composing.toString() == input) choose(item) }
                    })
                    if (special) candidateRow = candidates
                }
            }
        }
    }
    override fun onFinishInput() {
        stopDeleting()
        super.onFinishInput()
        session++; generation++; composing.clear(); pendingKeys.clear(); committing = false
        refreshCandidates()
    }
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
                                   candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composing.isNotEmpty() && candidatesEnd >= 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composing.clear(); session++; committing = false; pendingKeys.clear()
            currentInputConnection?.finishComposingText()
            refreshCandidates()
        }
    }
    override fun onFinishInputView(finishingInput: Boolean) {
        stopDeleting()
        super.onFinishInputView(finishingInput)
    }
    override fun onDestroy() {
        stopDeleting()
        session++; generation++
        query?.cancel(false)
        worker.submit { dictionary?.close() }
        worker.shutdown()
        super.onDestroy()
    }
    private fun label(title: String, color: Int) = TextView(this).apply {
        text = title; textSize = 16f; gravity = Gravity.CENTER_VERTICAL
        setTextColor(color); setPadding(dp(12), 0, dp(12), 0)
        layoutParams = LinearLayout.LayoutParams(-2, dp(48))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
