package com.andreibelous.yogalessons.view

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import com.andreibelous.yogalessons.*
import com.andreibelous.yogalessons.animation.AnimationSpec
import com.andreibelous.yogalessons.animation.animatedFloat
import com.andreibelous.yogalessons.animation.getValue
import com.andreibelous.yogalessons.animation.setValue
import com.andreibelous.yogalessons.recording.ProcessedResult
import com.andreibelous.yogalessons.view.results.ResultsView
import com.andreibelous.yogalessons.view.results.ResultsViewModel
import com.andreibelous.yogalessons.view.waves.AmplitudesDebugView
import com.andreibelous.yogalessons.view.waves.AmplitudesViewModel
import com.andreibelous.yogalessons.view.waves.WavesView
import com.badoo.mvicore.modelWatcher
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer

class AudioRecordingView(
    root: ViewGroup,
    lifecycle: Lifecycle,
    private val events: PublishRelay<Event> = PublishRelay.create()
) : Consumer<AudioRecordingViewModel>, ObservableSource<AudioRecordingView.Event> by events {

    sealed interface Event {

        object StartClicked : Event
        object StopClicked : Event
        object ShareClicked : Event
        object CloseClicked : Event
    }

    private val context = root.context
    private val contentRoot = root.findViewById<ViewGroup>(R.id.content_root)
    private var dialog: AlertDialog? = null
    private val waves = root.findViewById<WavesView>(R.id.waves)
    private val button = root.findViewById<TextView>(R.id.button_step).apply {
        RippleDrawable(
            ColorStateList.valueOf(Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )
    }
    private val buttonClose = root.findViewById<View>(R.id.button_close).apply {
        setOnClickListener { events.accept(Event.CloseClicked) }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )
    }
    private val buttonResults = root.findViewById<TextView>(R.id.label_results).apply {
        val radii = context.dp(24f)
        val stroke = context.dp(2f)
        val radiiArr = floatArrayOf(radii, radii, radii, radii, radii, radii, radii, radii)
        background =
            RippleDrawable(
                ColorStateList.valueOf(Color.WHITE),
                GradientDrawable().apply {
                    setStroke(stroke.toInt(), Color.WHITE)
                    cornerRadii = radiiArr
                },
                ShapeDrawable(RoundRectShape(radiiArr, null, null))
            )

        setOnClickListener { showResults() }
    }


    private val time = root.findViewById<TextView>(R.id.label_time).apply { alpha = 0.0f }
    private val timeTitle = root.findViewById<TextView>(R.id.label_title).apply { alpha = 0.0f }
    private val amplitudes = root.findViewById<AmplitudesDebugView>(R.id.amplitudes_view)
    private val resultsView = root.findViewById<ResultsView>(R.id.results_view)
    private val dimOverlay =
        root.findViewById<View>(R.id.dim_overlay).apply {
            alpha = 0.0f
            gone()
        }
    private val behaviour = BottomSheetBehavior.from(resultsView).apply {
        addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> dimOverlay.gone()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val progress = maxOf(slideOffset, 0f)
                    dimOverlay.alpha = progress
                }
            }
        )
    }

    private var timeAnimatedState by animatedFloat(
        initial = 0f,
        animationSpec = AnimationSpec(
            duration = 150,
            interpolator = OvershootInterpolator(1.1f)
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
        with(contentRoot) {
            layoutTransition = LayoutTransition()
            clipChildren = false
        }

        root.doOnPreDraw {
            resultsView.layoutParams = resultsView.layoutParams.apply {
                height = (root.height * 0.75f).toInt()
            }
        }

        hideResults()
        dimOverlay.setOnClickListener { behaviour.state = BottomSheetBehavior.STATE_COLLAPSED }
        lifecycle.subscribe {
            dialog?.dismiss()
            dialog = null
        }
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
                time.text = it
                timeAnimatedState = 1f
            } else {
                timeAnimatedState = 0f
            }
        }
        watch(AudioRecordingViewModel::step) {
            when (val step = it) {
                is AudioRecordingViewModel.Step.Initial -> {
                    hideResults()
                    button.text = "старт"
                    amplitudes
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                }
                is AudioRecordingViewModel.Step.Recording -> {
                    hideResults()
                    button.text = "стоп"
                    button.setOnClickListener { events.accept(Event.StopClicked) }
                }
                is AudioRecordingViewModel.Step.Error -> {
                    hideResults()
                    button.text = "старт"
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                }
                is AudioRecordingViewModel.Step.Finished -> {
                    button.text = "старт"
                    button.setOnClickListener { events.accept(Event.StartClicked) }
                    if (step.result.amplitude.isNotEmpty()) {
                        buttonResults.visible()
                        resultsView.bind(
                            ResultsViewModel(
                                token = step.token,
                                result = step.result
                            ) {
                                events.accept(Event.ShareClicked)
                            }
                        )
                        if (BuildConfig.DEBUG) {
                            amplitudes.bind(
                                AmplitudesViewModel(
                                    token = step.token,
                                    data = step.result
                                )
                            )
                            amplitudes.visible()
                        }

                    } else {
                        hideResults()
                    }
                }
            }
        }
    }

    private fun hideResults() {
        amplitudes.gone()
        buttonResults.gone()
        behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun showResults() {
        dimOverlay.visible()
        behaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun execute(action: Action) {
        when (action) {
            is Action.ShowResultsDialog -> showResults()
            is Action.ShowError -> {
                dialog?.dismiss()
                AlertDialog.Builder(context)
                    .setTitle("Упс...")
                    .setMessage("Что то пошло не так. Попробуете еще раз?")
                    .setPositiveButton("попробовать") { _, _ -> events.accept(Event.StartClicked) }
                    .setNegativeButton("нет") { _, _ ->
                        dialog?.dismiss()
                        dialog = null
                    }
                    .setCancelable(true)
                    .create()
                    .also { dialog = it }
                    .show()

            }
        }
    }

    sealed interface Action {

        object ShowResultsDialog : Action
        object ShowError : Action
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