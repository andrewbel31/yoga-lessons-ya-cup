package com.andreibelous.yogalessons.view.results

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.dp
import com.andreibelous.yogalessons.getColorCompat

class HeaderView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_header_item, this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    private val colors = intArrayOf(
        context.getColorCompat(R.color.gradient_start),
        context.getColorCompat(R.color.gradient_center),
        context.getColorCompat(R.color.gradient_end)
    )

    private val topImage = findViewById<View>(R.id.header_item_top_image).apply {
        this.clipToOutline = true
        this.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = context.dp(4f)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }

    private val text = findViewById<TextView>(R.id.header_item_title).apply {
        doOnPreDraw {

            paint.shader =
                LinearGradient(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
        }
    }

    private val amplitudesView = findViewById<AmplitudesViewV2>(R.id.header_item_amplitude_view)

    fun bind(token: String, data: List<Double>) {
        amplitudesView.bind(AmplitudesViewModelV2(token, data))
    }
}