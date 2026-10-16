package app.ciying.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

/** A short native Canvas reveal. No bitmap assets, network or continuous animation. */
class CreatorRevealView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1700
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { progress = it.animatedValue as Float; invalidate() }
    }
    private val density = resources.displayMetrics.density
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ValueAnimator.areAnimatorsEnabled()) animator.start() else progress = 1f
    }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = width / 2f
        val y = height / 2f
        val spread = min(width, height) * 0.44f
        paint.shader = RadialGradient(x, y, spread * 1.3f,
            intArrayOf(Color.rgb(24, 66, 52), Color.rgb(6, 22, 18)), null, Shader.TileMode.CLAMP)
        paint.alpha = 255; paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        val burst = (progress / 0.72f).coerceIn(0f, 1f)
        val fade = (sin(burst * PI).toFloat() * (1f - progress)).coerceIn(0f, 1f)
        paint.style = Paint.Style.STROKE
        for (ring in 0..2) {
            val phase = ((burst - ring * 0.11f) / (1f - ring * 0.11f)).coerceIn(0f, 1f)
            paint.color = Color.rgb(224, 202, 139)
            paint.alpha = (sin(phase * PI) * 130 * (1 - progress)).toInt().coerceIn(0, 255)
            paint.strokeWidth = density * (1.6f - ring * 0.35f)
            canvas.drawCircle(x, y, density * 15 + spread * phase, paint)
        }
        paint.style = Paint.Style.FILL
        for (i in 0 until 64) {
            val angle = i * 2.399963f + progress * 0.6f
            val radius = spread * (0.12f + burst * (0.45f + (i % 7) * 0.075f))
            val px = x + cos(angle) * radius
            val py = y + sin(angle) * radius * 0.65f
            paint.color = if (i % 3 == 0) Color.rgb(244, 228, 176) else Color.rgb(136, 201, 166)
            paint.alpha = (fade * (140 + i % 5 * 22)).toInt().coerceIn(0, 255)
            canvas.drawCircle(px, py, density * (0.7f + i % 3 * 0.55f), paint)
        }
        val initials = (1f - (progress / 0.48f)).coerceIn(0f, 1f)
        paint.color = Color.rgb(229, 211, 155); paint.alpha = (initials * 255).toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        paint.textSize = density * (38f + progress * 24f)
        canvas.drawText("T.G", x, y - (paint.ascent() + paint.descent()) / 2, paint)
        paint.alpha = 255
    }
}
