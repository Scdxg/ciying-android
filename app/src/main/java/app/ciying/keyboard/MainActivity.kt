package app.ciying.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val forest = Color.rgb(31, 83, 64)
    private val ink = Color.rgb(32, 55, 44)
    private val muted = Color.rgb(105, 125, 113)
    private lateinit var status: TextView
    private lateinit var statusDetail: TextView
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun shape(color: Int, radius: Int = 20) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun section(parent: LinearLayout): LinearLayout = column().apply {
        setPadding(dp(20), dp(20), dp(20), dp(20)); background = shape(Color.WHITE)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(20), dp(14), dp(20), 0) })
    }
    private fun button(title: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = title; textSize = 15f; isAllCaps = false
        setTextColor(if (primary) Color.WHITE else forest)
        background = shape(if (primary) forest else Color.rgb(233, 240, 233), 14)
        stateListAnimator = null
        layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) }
        setOnClickListener { action() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 55, 43)
        window.navigationBarColor = Color.rgb(246, 248, 243)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val page = column().apply { setBackgroundColor(Color.rgb(246, 248, 243)); isFocusableInTouchMode = true }
        val hero = FrameLayout(this)
        hero.addView(ImageView(this).apply {
            setImageResource(R.drawable.forest_cover); scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "云雾环绕的青绿色山林"
        }, FrameLayout.LayoutParams(-1, -1))
        hero.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x22314838, 0xE318372A.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))
        val heroText = column().apply { setPadding(dp(26), dp(22), dp(26), dp(22)) }
        heroText.addView(text("CIYING  /  词映", 13f, Color.WHITE, true).apply { letterSpacing = 0.16f })
        heroText.addView(text("让词语\n照见世界", 34f, Color.WHITE, true).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(30) }
        })
        heroText.addView(text("输入中文，顺便遇见英文。", 15f, 0xFFE7EEE4.toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        })
        heroText.addView(text("离线运行   ·   无需账号", 12f, 0xFFE4D7AC.toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) }
        })
        hero.addView(heroText)
        page.addView(hero, LinearLayout.LayoutParams(-1, dp(330)))

        val setup = section(page)
        status = text("准备好开始输入", 21f, ink, true)
        statusDetail = text("启用词映，让每次输入多一点收获。", 13f, muted)
        setup.addView(status); setup.addView(statusDetail)
        setup.addView(button("启用词映输入法", true) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        setup.addView(button("切换到词映", false) {
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()
        })

        val tryCard = section(page)
        tryCard.addView(text("同一个读音，不同的意思", 20f, ink, true))
        tryCard.addView(text("公式   formula     /     公示   public notice", 14f, forest).apply {
            setPadding(0, dp(8), 0, dp(12))
        })
        tryCard.addView(EditText(this).apply {
            hint = "在这里试试：gongshi、shijain"
            textSize = 16f; minLines = 3; gravity = Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = shape(Color.rgb(244, 247, 241), 14)
            setTextColor(ink); setHintTextColor(muted)
            layoutParams = LinearLayout.LayoutParams(-1, dp(116))
        })
        tryCard.addView(text("也可以试试整句：woxuexiyingyu", 12f, muted).apply { setPadding(0, dp(10), 0, 0) })

        val features = section(page)
        features.addView(text("让输入更顺手", 20f, ink, true))
        listOf("懂一点上下文" to "结合前面的词，让合适的候选更靠前。", "打错，也能找到" to "少打、多打或按错相邻字母，都有机会找回。", "安心留在本机" to "无需联网。密码框不显示候选、不记录选词。").forEachIndexed { index, item ->
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(16), 0, 0) }
            line.addView(text(listOf("文", "↺", "隐")[index], 20f, forest, true).apply {
                gravity = Gravity.CENTER; background = shape(Color.rgb(234, 241, 232), 12)
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
            line.addView(column().apply {
                setPadding(dp(12), 0, 0, 0)
                addView(text(item.first, 15f, ink, true)); addView(text(item.second, 12f, muted))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            features.addView(line)
        }
        val footer = column().apply { setPadding(dp(24), dp(16), dp(24), dp(24)) }
        footer.addView(text("开源许可与图片来源", 13f, forest).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(10)); setOnClickListener { showLicenses() }
        })
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        footer.addView(text("词映 $version   ·   好好输入，慢慢认识世界", 11f, muted).apply {
            gravity = Gravity.CENTER

        })
        page.addView(footer)
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(page) })
        page.requestFocus()
    }
    override fun onResume() {
        super.onResume()
        if (!::status.isInitialized) return
        val manager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val enabled = manager.enabledInputMethodList.any { it.packageName == packageName }
        val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith(packageName + "/") == true
        status.text = if (selected) "词映已就绪" else if (enabled) "再切换一下，就能开始" else "准备好开始输入"
        statusDetail.text = if (selected) "你的下一句话，可以从这里开始。" else "按下面的步骤启用并切换到词映。"
    }
    private fun showLicenses() {
        val content = text("", 13f).apply {
            text = assets.open("licenses/THIRD_PARTY.md").bufferedReader().use { it.readText() } + "\n\n" +
                assets.open("licenses/GPL-3.0.txt").bufferedReader().use { it.readText() }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        AlertDialog.Builder(this).setTitle("开源与图片许可").setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("关闭", null).show()
    }
}
