package com.andreibelous.yogalessons.view.results

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.dp
import com.andreibelous.yogalessons.getColorCompat

class AmplitudesViewV2
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
        clipToOutline = false
    }

    private var token: String = ""
    private var data: List<Double> = emptyList()
        set(value) {
            field = value
            max = data.maxOrNull() ?: max
            min = data.minOrNull() ?: min
        }
    private var max = 0.0
    private var min = 0.0

    private var paint = Paint().apply {
        pathEffect = CornerPathEffect(10f)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2f)
    }

    private val colors = intArrayOf(
        context.getColorCompat(R.color.gradient_start),
        context.getColorCompat(R.color.gradient_center),
        context.getColorCompat(R.color.gradient_end)
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    fun bind(model: AmplitudesViewModelV2) {
        if (model.token != token && model.data.isNotEmpty()) {
            token = model.token
            data = model.data
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            return
        }

        drawPath(canvas)
    }

    private fun drawPath(canvas: Canvas) {
        val scaleX = width / data.size.toFloat()
        val scaleY = height / (max - min) / 2

        for (i in data.indices) {
            val x = i * scaleX
            val y1 = height / 2 + (scaleY * data[i]).toFloat()
            val y2 = height / 2 - (scaleY * data[i]).toFloat()

            canvas.drawLine(x, y1, x, y2, paint)
        }
    }
}

data class AmplitudesViewModelV2(
    val token: String,
    val data: List<Double>
)