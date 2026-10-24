package app.ciying.keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import java.io.File

data class KeyboardPalette(val background: Int, val ink: Int, val key: Int, val utility: Int, val accent: Int, val pressed: Int)

object KeyboardAppearance {
    fun preferences(context: Context) = context.getSharedPreferences("keyboard-appearance", Context.MODE_PRIVATE)
    fun photo(context: Context) = File(context.filesDir, "keyboard-photo.jpg")
    fun mode(context: Context) = preferences(context).getString("mode", "forest") ?: "forest"
    fun palette(mode: String) = when (mode) {
        "black" -> KeyboardPalette(Color.BLACK, Color.WHITE, Color.BLACK, Color.BLACK, Color.BLACK, 0xff333333.toInt())
        "white" -> KeyboardPalette(Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, 0xffdddddd.toInt())
        "photo" -> KeyboardPalette(Color.BLACK, Color.WHITE, 0x66000000, 0x88000000.toInt(), 0x99000000.toInt(), 0xaa333333.toInt())
        else -> KeyboardPalette(0xffe8efeb.toInt(), 0xff20372c.toInt(), Color.WHITE, 0xffd5e3db.toInt(), 0xff1f6753.toInt(), 0xffc3cbd4.toInt())
    }
}

/** Scales a bounded local bitmap to fill the keyboard without stretching it. */
class KeyboardPhotoDrawable(private val bitmap: Bitmap) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    override fun draw(canvas: Canvas) {
        val scale = maxOf(bounds.width().toFloat() / bitmap.width, bounds.height().toFloat() / bitmap.height)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val left = bounds.centerX() - width / 2
        val top = bounds.centerY() - height / 2
        val save = canvas.save()
        canvas.clipRect(bounds)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), paint)
        canvas.drawColor(0x55000000)
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
