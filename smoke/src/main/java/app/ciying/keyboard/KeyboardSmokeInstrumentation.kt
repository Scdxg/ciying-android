package app.ciying.keyboard

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class KeyboardTestActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(EditText(this).apply {
            hint = "词映自动输入测试"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = android.view.Gravity.TOP
        })
    }
}

/** Exercises the real IME on a connected phone using platform APIs only. */
class KeyboardSmokeInstrumentation : Instrumentation() {
    private var preview = false
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); preview = arguments?.getString("preview") == "true"; start() }
    override fun onStart() {
        if (preview) { captureKeyboardPreview(); return }
        val results = mutableListOf<String>()
        fun progress(message: String) { sendStatus(0, Bundle().apply { putString("stream", message) }) }
        try {
            progress("Connecting test automation")
            val info = uiAutomation.serviceInfo
            info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            uiAutomation.serviceInfo = info
            progress("Opening test activity")
            val resumed = java.util.concurrent.atomic.AtomicReference<Activity>()
            (targetContext.applicationContext as android.app.Application).registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) { resumed.set(activity) }
                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            })
            uiAutomation.executeShellCommand("am start -n app.ciying.keyboard.test/app.ciying.keyboard.KeyboardTestActivity -f 268468224").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
            repeat(100) { if (resumed.get() == null) SystemClock.sleep(100) }
            val activity = resumed.get() ?: error("Activity launch timed out")
            progress("Focusing editor")
            check(!(targetContext.getSystemService(Activity.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked) {
                "Phone is locked; unlock it before running this test"
            }
            uiAutomation.executeShellCommand("ime set app.ciying.keyboard/.CiyingInputMethodService").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
            fun editor(view: View): EditText? {
                if (view is EditText) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) editor(view.getChildAt(i))?.let { return it }
                return null
            }
            lateinit var field: EditText
            runOnMainSync {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                field = editor(activity.window.decorView) ?: error("Test editor missing")
                field.setText("")
                field.requestFocus()
                (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
            }
            // Window focus is delivered after onResume; an earlier show request can be ignored.
            var focused = false
            repeat(50) {
                if (!focused) {
                    runOnMainSync { focused = field.hasWindowFocus() }
                    if (!focused) SystemClock.sleep(100)
                }
            }
            check(focused) { "Test window did not gain focus" }
            SystemClock.sleep(500)
            runOnMainSync {
                (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, InputMethodManager.SHOW_FORCED)
            }
            SystemClock.sleep(800)
            fun find(text: String): AccessibilityNodeInfo? {
                fun walk(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                    if (node == null) return null
                    val caption = node.text?.toString().orEmpty()
                    if (caption == text || (text.startsWith("@") && caption.startsWith(text.drop(1) + "   "))) return node
                    for (i in 0 until node.childCount) walk(node.getChild(i))?.let { return it }
                    return null
                }
                return uiAutomation.windows.asSequence().filter { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }.mapNotNull { walk(it.root) }.firstOrNull()
            }
            fun click(text: String) {
                progress("Click: $text")
                var node: AccessibilityNodeInfo? = null
                repeat(200) {
                    if (node == null) { node = find(text); if (node == null) SystemClock.sleep(100) }
                }
                if (node == null) {
                    uiAutomation.executeShellCommand("dumpsys input_method").use {
                        val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
                        progress(dump.lineSequence().filter { line -> line.contains("Shown") || line.contains("mCurMethodId") || line.contains("mShowRequested") || line.contains("mIsInputViewShown") || line.contains("mWindowVisible") || line.contains("mInputView=") }.joinToString("\n"))
                    }
                }
                check(node != null) { "IME key/candidate not found: $text; windows=${uiAutomation.windows.map { it.type }}" }
                check(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Click failed: $text" }
                SystemClock.sleep(150)
            }
            fun expect(text: String) {
                var actual = ""
                repeat(50) {
                    if (actual != text || it == 0) {
                        runOnMainSync { actual = field.text.toString() }
                        if (actual != text) SystemClock.sleep(100)
                    }
                }
                check(actual == text) { "Expected '$text', got '$actual'" }
                results.add("PASS editor=$text")
            }
            fun awaitCandidate(text: String): AccessibilityNodeInfo? {
                var candidate: AccessibilityNodeInfo? = null
                repeat(50) { if (candidate == null) { candidate = find(text); if (candidate == null) SystemClock.sleep(100) } }
                return candidate
            }
            click("s"); expect("s")
            check(find("s") != null) { "Letter key changed after click" }
            click("⌫"); expect("")
            runOnMainSync { field.setText("abcdefghij"); field.setSelection(10) }
            val bounds = android.graphics.Rect()
            find("⌫")!!.getBoundsInScreen(bounds)
            val down = SystemClock.uptimeMillis()
            fun touch(action: Int) {
                val event = android.view.MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                    bounds.exactCenterX(), bounds.exactCenterY(), 0)
                event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                check(uiAutomation.injectInputEvent(event, true))
                event.recycle()
            }
            touch(android.view.MotionEvent.ACTION_DOWN)
            SystemClock.sleep(650)
            touch(android.view.MotionEvent.ACTION_UP)
            var afterHold = ""
            runOnMainSync { afterHold = field.text.toString() }
            check(afterHold.length in 1..8) { "Long delete did not repeat: $afterHold" }
            SystemClock.sleep(250)
            runOnMainSync { check(field.text.toString() == afterHold) { "Delete continued after release" }; field.setText("") }
            results.add("PASS long-delete repeats and stops on release")
            click("n"); click("i")
            val firstCandidateBounds = android.graphics.Rect()
            awaitCandidate("@你")!!.getBoundsInScreen(firstCandidateBounds)
            check(firstCandidateBounds.left < 50) { "Raw pinyin still precedes first candidate" }
            results.add("PASS first candidate starts candidate row")
            check(find("@你")?.text?.contains("you") == true) { "Chinese/English candidate missing" }
            click("@你"); expect("你")
            click("h"); click("a"); click("o"); click("空格"); expect("你好")
            click("⌫"); expect("你")
            click("中"); click("s"); expect("你s")
            click("英"); click("123"); click("1"); expect("你s1")
            click("ABC"); click("换行"); expect("你s1\n")
            fun reset() { runOnMainSync { field.setText("") }; SystemClock.sleep(200) }
            reset()
            "tuoxie".forEach { click(it.toString()) }
            check(find("@拖鞋")?.text?.contains("slippers") == true) { "拖鞋 / slippers missing" }
            check(find("@妥协")?.text?.contains("compromise") == true) { "妥协 / compromise missing" }
            click("@拖鞋"); expect("拖鞋")
            reset()
            "tuoxie".forEach { click(it.toString()) }
            click("@妥协"); expect("妥协")
            reset()
            "woxuexiyingyu".forEach { click(it.toString()) }
            click("空格"); expect("我学习英语")
            reset()
            click("n"); click("h"); click("@你好"); expect("你好")
            reset()
            "shijain".forEach { click(it.toString()) }
            check(awaitCandidate("@时间")?.text?.contains("纠错") == true) { "Transposed spelling correction missing" }
            click("@时间"); expect("时间")
            reset()
            "canjia".forEach { click(it.toString()) }
            click("@参加")
            "shijian".forEach { click(it.toString()) }
            click("空格"); expect("参加实践")

            val lastAction = java.util.concurrent.atomic.AtomicInteger(-1)
            fun configure(type: Int, options: Int = 0) {
                runOnMainSync {
                    field.setText(""); field.inputType = type; field.imeOptions = options
                    field.setOnEditorActionListener { _, action, _ -> lastAction.set(action); true }
                    field.requestFocus()
                    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.restartInput(field); imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
                }
                SystemClock.sleep(600)
            }
            val textType = android.text.InputType.TYPE_CLASS_TEXT
            configure(textType or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
            var activeMethod = ""
            uiAutomation.executeShellCommand("dumpsys input_method").use {
                activeMethod = android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
                    .lineSequence().firstOrNull { line -> line.contains("mCurMethodId=") }.orEmpty()
            }
            if (activeMethod.contains("securityinputmethod")) {
                results.add("PASS OEM-password-secure-keyboard-handoff (system protection preserved)")
            } else {
                click("s"); expect("s")
                check(find("私密输入 · 不记录选词") != null) { "Password privacy indicator missing" }
                check(find("@是") == null) { "Password input exposed candidates" }
            }
            configure(textType or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE, android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
            runOnMainSync { field.setText("参加"); field.setSelection(field.length()) }
            "shijian".forEach { click(it.toString()) }
            click("空格"); expect("参加时间")
            check(find("私密输入 · 不记录选词") != null) { "Private-editor indicator missing" }
            results.add("PASS no-personalized-context")
            configure(android.text.InputType.TYPE_CLASS_NUMBER)
            click("1"); click("2"); expect("12")
            configure(textType or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            click("a"); expect("a")
            configure(textType, android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
            click("搜索"); check(lastAction.get() == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
            results.add("PASS editor-action=search")
            configure(textType, android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
            click("发送"); check(lastAction.get() == android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
            results.add("PASS editor-action=send (local listener only)")
            configure(textType, android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            click("完成"); check(lastAction.get() == android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            results.add("PASS editor-action=done")
            configure(textType or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            runOnMainSync { field.setText("A😀"); field.setSelection(field.length()) }
            click("⌫"); expect("A")
            runOnMainSync { field.setText("ABC"); field.setSelection(1, 3) }
            click("⌫"); expect("A")
            configure(textType or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            check(uiAutomation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90))
            SystemClock.sleep(1200)
            "ni".forEach { click(it.toString()) }; click("@你"); expect("你")
            val deleteBounds = android.graphics.Rect()
            find("⌫")!!.getBoundsInScreen(deleteBounds)
            check(deleteBounds.width() > 0 && deleteBounds.height() > 0)
            results.add("PASS landscape-keyboard")
            uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
            SystemClock.sleep(1000)

            // A real Android WebView editor exercises the browser input connection.
            val web = java.util.concurrent.atomic.AtomicReference<android.webkit.WebView>()
            runOnMainSync {
                val view = android.webkit.WebView(activity)
                view.settings.javaScriptEnabled = true
                activity.setContentView(view)
                view.loadDataWithBaseURL("https://ciying.invalid/", "<html><body><textarea aria-label='Ciying browser test' style='width:90%;height:200px;font-size:24px'></textarea></body></html>", "text/html", "UTF-8", null)
                web.set(view)
            }
            SystemClock.sleep(1200)
            runOnMainSync {
                web.get().requestFocus()
                web.get().evaluateJavascript("document.querySelector('textarea').focus()", null)
                (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(web.get(), InputMethodManager.SHOW_FORCED)
            }
            SystemClock.sleep(800)
            "ni".forEach { click(it.toString()) }; click("@你")
            val browserValue = java.util.concurrent.atomic.AtomicReference<String>()
            runOnMainSync { web.get().evaluateJavascript("document.querySelector('textarea').value") { browserValue.set(it) } }
            repeat(30) { if (browserValue.get() == null) SystemClock.sleep(100) }
            check(browserValue.get() == "\"你\"") { "WebView input failed: ${browserValue.get()}" }
            results.add("PASS Android-WebView=你")

            uiAutomation.executeShellCommand("am start -n app.ciying.keyboard/.MainActivity").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
            SystemClock.sleep(1000)
            if (uiAutomation.rootInActiveWindow?.packageName == "app.ciying.keyboard") {
                val screenshot = uiAutomation.takeScreenshot()
                if (screenshot != null) {
                    val file = java.io.File(targetContext.getExternalFilesDir(null), "ciying-home-0.3.0.png")
                    file.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    results.add("PASS home-screenshot=${file.absolutePath}")
                }
            }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", results.joinToString("\n")) })
        } catch (error: Throwable) {
            uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", results.joinToString("\n") + "\nFAIL: " + error.stackTraceToString()) })
        }
    }
    private fun captureKeyboardPreview() {
        try {
            val info = uiAutomation.serviceInfo
            info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            uiAutomation.serviceInfo = info
            uiAutomation.executeShellCommand("am start -n app.ciying.keyboard/.MainActivity").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
            SystemClock.sleep(800)
            fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
                if (node == null) return null
                if (predicate(node)) return node
                for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
                return null
            }
            check(uiAutomation.rootInActiveWindow?.packageName == "app.ciying.keyboard")
            val editor = find(uiAutomation.rootInActiveWindow) { it.className == "android.widget.EditText" }
            check(editor?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
            SystemClock.sleep(1000)
            fun key(title: String) {
                var node: AccessibilityNodeInfo? = null
                repeat(30) {
                    if (node == null) {
                        node = uiAutomation.windows.filter { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                            .firstNotNullOfOrNull { find(it.root) { child -> child.text?.toString() == title } }
                        if (node == null) SystemClock.sleep(100)
                    }
                }
                check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
                SystemClock.sleep(250)
            }
            key("n"); key("i"); SystemClock.sleep(600)
            val screenshot = checkNotNull(uiAutomation.takeScreenshot())
            val file = java.io.File(targetContext.getExternalFilesDir(null), "ciying-keyboard-0.3.0.png")
            file.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            key("⌫"); key("⌫")
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "PASS keyboard-screenshot=${file.absolutePath}") })
        } catch (error: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "FAIL preview: ${error.stackTraceToString()}") })
        }
    }
}


