package com.andreibelous.yogalessons.view.results

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.recording.Phase
import com.badoo.mvicore.modelWatcher

class PhaseView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_phase_item, this)
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    private val phaseTitle = findViewById<TextView>(R.id.phase_item_title)
    private val phaseDuration = findViewById<TextView>(R.id.phase_item_duration)

    fun bind(phase: Phase) {
        modelWatcher(phase)
    }

    @SuppressLint("SetTextI18n")
    private val modelWatcher = modelWatcher<Phase> {
        watch(Phase::type) { phaseTitle.text = it.toName() }
        watch(Phase::durationStr) { phaseDuration.text = "$it сек." }
    }

    private fun Phase.Type.toName(): String =
        when (this) {
            Phase.Type.Pause -> "Пауза"
            Phase.Type.Inhale -> "Вдох"
            Phase.Type.Exhale -> "Выдох"
        }
}