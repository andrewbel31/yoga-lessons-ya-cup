package com.andreibelous.yogalessons.view.gradient

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.*
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.view.animation.lerp

class GradientView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
    }

    private val colors = intArrayOf(
        getColor(R.color.gradient_start),
        getColor(R.color.gradient_center),
        getColor(R.color.gradient_end)
    )

    private fun getColor(@ColorRes colorRes: Int): Int =
        ContextCompat.getColor(context, colorRes)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        paint.shader = LinearGradient(
            -width / 2f,
            height / 2f,
            width / 2f,
            height / 2f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    private var animator: Animator? = null
    private var currentAngle = -45f
        set(value) {
            field = value
            invalidate()
        }

    private var currentScale = 1.3f
        set(value) {
            field = value
            invalidate()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        animator = ObjectAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                currentAngle = -45f + (it.animatedValue as Float) * 270
                val fraction = it.animatedFraction
                currentScale = lerp(1.3f, 1.0f, fraction)
            }
            interpolator = LinearOutSlowInInterpolator()
            duration = 1500L
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null

        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(currentAngle)
        canvas.scale(currentScale, currentScale)
        canvas.drawPaint(paint)
        canvas.restore()
    }
}