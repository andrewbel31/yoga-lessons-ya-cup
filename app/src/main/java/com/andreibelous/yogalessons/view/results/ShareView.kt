package com.andreibelous.yogalessons.view.results

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.dp

class ShareView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_share_item, this)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    private val button = findViewById<View>(R.id.share_item_button).apply {
        val radii = context.dp(8f)
        background = RippleDrawable(
            ColorStateList.valueOf(Color.LTGRAY),
            null,
            ShapeDrawable(
                RoundRectShape(
                    floatArrayOf(radii, radii, radii, radii, radii, radii, radii, radii),
                    null,
                    null
                )
            )
        )
    }


    fun bind(action: () -> Unit) {
        button.setOnClickListener { action() }
    }
}