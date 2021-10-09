package com.andreibelous.yogalessons.view.results

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.andreibelous.yogalessons.R

class ShareView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_share_item, this)
    }



}