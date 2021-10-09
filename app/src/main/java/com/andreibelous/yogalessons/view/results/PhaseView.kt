package com.andreibelous.yogalessons.view.results

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.recording.Phase
import com.badoo.mvicore.modelWatcher

class PhaseView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_phase_item, this)
        orientation = HORIZONTAL
    }

    private val phaseTitle = findViewById<TextView>(R.id.phase_item_title)
    private val phaseFrom = findViewById<TextView>(R.id.phase_item_from)
    private val phaseTo = findViewById<TextView>(R.id.phase_item_to)

    fun bind(phase: Phase) {
        modelWatcher(phase)
    }

    private val modelWatcher = modelWatcher<Phase> {
        watch(Phase::type) { phaseTitle.text = it.toName() }
        watch(Phase::startStr) { phaseFrom.text = it }
        watch(Phase::endStr) { phaseTo.text = it }
    }

    private fun Phase.Type.toName(): String =
        when (this) {
            Phase.Type.Pause -> "Пауза"
            Phase.Type.Inhale -> "Вдох"
            Phase.Type.Exhale -> "Выдох"
        }
}