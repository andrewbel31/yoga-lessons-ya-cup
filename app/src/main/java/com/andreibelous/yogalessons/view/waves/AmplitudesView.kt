package com.andreibelous.yogalessons.view.waves

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.andreibelous.yogalessons.recording.ProcessedResult

class AmplitudesView
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
    private var noiseLevel = 0.0
    private var max = 0.0
    private var min = 0.0
    private var times: List<Int> = emptyList()

    private var path = Path()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        pathEffect = CornerPathEffect(10f)
        style = Paint.Style.STROKE
    }

    fun bind(model: AmplitudesViewModel) {
        if (model.token != token && model.data.amplitude.isNotEmpty()) {
            token = model.token
            data = model.data.amplitude
            noiseLevel = model.data.noiseLevel
            times = model.data.times
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
        val scaleY = height / (max - min)

        times.forEach {
            canvas.drawLine(
                it * scaleX,
                0f,
                it * scaleX,
                height.toFloat(),
                paint
            )
        }

        canvas.drawLine(
            0f,
            -(noiseLevel * scaleY).toFloat() + height,
            width.toFloat(),
            -(noiseLevel * scaleY).toFloat() + height,
            paint
        )

        with(path) {
            reset()
            moveTo(0f, -(data[0] * scaleY).toFloat() + height)

            for (i in 1 until data.size) {
                path.lineTo(
                    ((i) * scaleX),
                    -(data[i] * scaleY).toFloat() + height
                )
            }

            canvas.drawPath(this, paint)
        }
    }
}

data class AmplitudesViewModel(
    val token: String,
    val data: ProcessedResult
)