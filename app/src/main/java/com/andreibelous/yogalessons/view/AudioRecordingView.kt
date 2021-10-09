package com.andreibelous.yogalessons.view

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import com.andreibelous.yogalessons.R
import com.andreibelous.yogalessons.gone
import com.andreibelous.yogalessons.recording.ProcessedResult
import com.andreibelous.yogalessons.view.animation.*
import com.andreibelous.yogalessons.view.results.ResultsView
import com.andreibelous.yogalessons.view.results.ResultsViewModel
import com.andreibelous.yogalessons.view.waves.AmplitudesView
import com.andreibelous.yogalessons.view.waves.AmplitudesViewModel
import com.andreibelous.yogalessons.view.waves.WavesView
import com.andreibelous.yogalessons.visible
import com.badoo.mvicore.modelWatcher
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer

class AudioRecordingView(
    root: ViewGroup,
    private val events: PublishRelay<Event> = PublishRelay.create()
) : Consumer<AudioRecordingViewModel>, ObservableSource<AudioRecordingView.Event> by events {

    sealed interface Event {

        object StartClicked : Event
        object StopClicked : Event
        object ShareClicked : Event
        object CloseClicked : Event
    }

    private val waves = root.findViewById<WavesView>(R.id.waves)
    private val button = root.findViewById<TextView>(R.id.button_step)
    private val buttonClose = root.findViewById<View>(R.id.button_close)
    private val time = root.findViewById<TextView>(R.id.label_time)
    private val timeTitle = root.findViewById<TextView>(R.id.label_title)
    private val amplitudes = root.findViewById<AmplitudesView>(R.id.amplitudes_view)
    private val resultsView = root.findViewById<ResultsView>(R.id.results_view)
    private val behaviour = BottomSheetBehavior.from(resultsView)

    private var timeAnimatedState by animatedFloat(
        initial = 0f,
        animationSpec = AnimationSpec(
            duration = 150,
            interpolator = AccelerateInterpolator()
        )
    ) {
        with(timeTitle) {
            scaleX = lerp(0.8f, 1f, it)
            scaleY = lerp(0.8f, 1f, it)
            alpha = it
        }

        with(time) {
            scaleX = lerp(0.8f, 1f, it)
            scaleY = lerp(0.8f, 1f, it)
            alpha = it
        }
    }

    init {
        buttonClose.setOnClickListener { events.accept(Event.CloseClicked) }
        buttonClose.background = RippleDrawable(
            ColorStateList.valueOf(android.graphics.Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )

        button.background = RippleDrawable(
            ColorStateList.valueOf(android.graphics.Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )
        root.layoutTransition = LayoutTransition()
        root.clipChildren = false
        hideBottomSheet()
    }

    override fun accept(vm: AudioRecordingViewModel) {
        modelWatcher(vm)
    }

    private val modelWatcher = modelWatcher<AudioRecordingViewModel> {
        watch(AudioRecordingViewModel::amplitude) {
            waves.update(it.toFloat())
        }
        watch(AudioRecordingViewModel::time) {
            if (it != null) {
                time.visible()
                timeTitle.visible()
                timeAnimatedState = 1f
                time.text = it
            } else {
                timeAnimatedState = 0f
            }
        }
        watch(AudioRecordingViewModel::step) {
            when (val step = it) {
                is AudioRecordingViewModel.Step.Initial -> {
                    hideBottomSheet()
                    amplitudes.gone()
                    button.text = "старт"
                    amplitudes
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                }
                is AudioRecordingViewModel.Step.Recording -> {
                    hideBottomSheet()
                    amplitudes.gone()
                    button.text = "стоп"
                    button.setOnClickListener { events.accept(Event.StopClicked) }
                }
                is AudioRecordingViewModel.Step.Error -> {
                    hideBottomSheet()
                    amplitudes.gone()
                    button.text = "старт"
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                }
                is AudioRecordingViewModel.Step.Finished -> {
                    button.text = "старт"
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                    if (step.result.amplitude.isNotEmpty()) {
                        amplitudes.visible()
                        amplitudes.bind(
                            AmplitudesViewModel(
                                token = step.token,
                                data = step.result
                            )
                        )
                        showBottomSheet(
                            ResultsViewModel(
                                token = step.token,
                                result = step.result
                            ) {
                                events.accept(Event.ShareClicked)
                            }
                        )
                    } else {
                        amplitudes.gone()
                        hideBottomSheet()
                    }
                }
            }
        }
    }

    private fun showBottomSheet(model: ResultsViewModel) {
        resultsView.bind(model)
        behaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideBottomSheet() {
        behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}

data class AudioRecordingViewModel(
    val amplitude: Int,
    val time: String? = null,
    val step: Step = Step.Initial
) {

    sealed interface Step {

        object Initial : Step
        object Recording : Step
        data class Error(val throwable: Throwable) : Step
        data class Finished(val token: String, val result: ProcessedResult) : Step
    }
}