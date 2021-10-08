package com.andreibelous.yogalessons.view.waves

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import com.andreibelous.yogalessons.dp
import com.andreibelous.yogalessons.view.waves.BlobDrawable.SCALE_BIG_MIN
import com.andreibelous.yogalessons.view.waves.BlobDrawable.SCALE_SMALL_MIN

class WavesView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blob1 = BlobDrawable(12)
    private val blob2 = BlobDrawable(9)
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private val mainColor = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(mainColor, (255 * 0.45f).toInt())
    }

    private val radius = context.dp(40f) * BLOB_SCALE_COEF

    init {
        setWillNotDraw(false)
        blob2.minRadius = context.dp(55f * BLOB_SCALE_COEF)
        blob2.maxRadius = context.dp(67f * BLOB_SCALE_COEF)
        blob2.generateBlob()

        blob1.minRadius = context.dp(65f * BLOB_SCALE_COEF)
        blob1.maxRadius = context.dp(75f * BLOB_SCALE_COEF)
        blob1.generateBlob()

        blob1.paint.color =
            ColorUtils.setAlphaComponent(mainColor, (255 * 0.15f).toInt())
        blob2.paint.color =
            ColorUtils.setAlphaComponent(mainColor, (255 * 0.30f).toInt())
    }

    private var animator: Animator? = null
    private var generalScale: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        animator = ObjectAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                generalScale = it.animatedValue as Float
            }
            duration = 1000L
            interpolator = OvershootInterpolator(1.1f)
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    fun update(value: Float) {
        blob1.setValue(value.coerceAtMost(MAX_AMPLITUDE) / MAX_AMPLITUDE, true)
        blob2.setValue(value.coerceAtMost(MAX_AMPLITUDE) / MAX_AMPLITUDE, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val delta = System.currentTimeMillis() - lastUpdateTime

        blob1.updateAmplitude(delta)
        blob2.updateAmplitude(delta)

        blob1.updateAmplitude(delta)
        blob1.update(blob1.amplitude, 0.6f)
        blob2.updateAmplitude(delta)
        blob2.update(blob2.amplitude, 0.7f)

        lastUpdateTime = System.currentTimeMillis()
        drawBlobs(canvas)
        invalidate()
    }

    private fun drawBlobs(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        with(canvas) {
            save()
            val scale1 = generalScale * (SCALE_BIG_MIN + SCALE_MULTIPLIER * blob1.amplitude)
            scale(scale1, scale1, cx, cy)
            blob1.draw(cx, cy, canvas, blob1.paint)
            restore()
            val scale2 = generalScale * (SCALE_SMALL_MIN + SCALE_MULTIPLIER * blob2.amplitude)
            save()
            scale(scale2, scale2, cx, cy)
            blob2.draw(cx, cy, canvas, blob2.paint)
            restore()

            val scale = (scale1 + scale2) / 2
            drawCircle(cx, cy, radius * scale, paint)
        }
    }

    private companion object {

        private const val SCALE_MULTIPLIER = 1.2f
        private const val MAX_AMPLITUDE = 1200f
        private const val BLOB_SCALE_COEF = 1.1f
    }
}