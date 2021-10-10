package com.andreibelous.yogalessons.recording

import com.andreibelous.yogalessons.recording.AudioRecordingFeature.News
import com.andreibelous.yogalessons.recording.AudioRecordingFeature.Wish
import com.andreibelous.yogalessons.recording.AudioRecordingState.*
import com.andreibelous.yogalessons.toObservable
import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.Bootstrapper
import com.badoo.mvicore.element.NewsPublisher
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.BaseFeature
import com.badoo.mvicore.feature.Feature
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class AudioRecordingFeature(
    private val audioRecorder: AudioRecorder
) : Feature<Wish, AudioRecordingState, News> by BaseFeature(
    initialState = AudioRecordingState(),
    wishToAction = Action::ExecuteWish,
    actor = ActorImpl(audioRecorder),
    reducer = ReducerImpl(),
    bootstrapper = BootstrapperImpl(audioRecorder),
    newsPublisher = NewsPublisherImpl()
) {

    sealed interface Wish {

        object StartRecording : Wish
        object FinishRecording : Wish

    }

    sealed interface News {

        object Finished : News
        object Error : News
    }

    private sealed interface Effect {

        object RecordingStarted : Effect
        data class TimeUpdated(val millis: Long) : Effect
        data class AmplitudeUpdated(val amplitude: Int) : Effect
        data class RecordingFinished(val token: String, val data: ProcessedResult) : Effect
        data class ErrorHappened(val throwable: Throwable) : Effect

    }

    private sealed interface Action {

        data class ExecuteWish(val wish: Wish) : Action
        object HandleRecordingStarted : Action
        data class HandleRecordingFinished(val token: String, val data: ProcessedResult) : Action
        data class HandleError(val throwable: Throwable) : Action
        data class HandleTimeUpdated(val millis: Long) : Action
        data class HandleAmplitudeUpdated(val amplitude: Int) : Action
    }

    private class ActorImpl(
        private val recorder: AudioRecorder
    ) : Actor<AudioRecordingState, Action, Effect> {

        override fun invoke(state: AudioRecordingState, action: Action): Observable<out Effect> =
            when (action) {
                is Action.ExecuteWish -> executeWish(action.wish)
                is Action.HandleRecordingStarted -> Effect.RecordingStarted.toObservable()
                is Action.HandleRecordingFinished ->
                    Effect.RecordingFinished(action.token, action.data).toObservable()
                is Action.HandleTimeUpdated ->
                    state.toObservable()
                        .filter { it.step == Step.Recording }
                        .flatMap { Effect.TimeUpdated(millis = action.millis).toObservable() }
                is Action.HandleError -> Effect.ErrorHappened(action.throwable).toObservable()
                is Action.HandleAmplitudeUpdated -> Effect.AmplitudeUpdated(action.amplitude)
                    .toObservable()
            }.observeOn(AndroidSchedulers.mainThread())

        private fun executeWish(wish: Wish): Observable<out Effect> =
            when (wish) {
                is Wish.StartRecording -> {
                    recorder.start()
                    Observable.empty()
                }
                is Wish.FinishRecording -> {
                    recorder.finish()
                    Observable.empty()
                }
            }
    }

    private class ReducerImpl : Reducer<AudioRecordingState, Effect> {

        override fun invoke(state: AudioRecordingState, effect: Effect): AudioRecordingState =
            when (effect) {
                is Effect.RecordingStarted ->
                    state.copy(
                        step = Step.Recording,
                        time = null
                    )
                is Effect.TimeUpdated -> state.copy(time = effect.millis)
                is Effect.AmplitudeUpdated -> state.copy(amplitude = effect.amplitude)
                is Effect.ErrorHappened ->
                    state.copy(
                        step = Step.Error(effect.throwable),
                        time = null
                    )
                is Effect.RecordingFinished ->
                    state.copy(
                        step = Step.Finished(
                            token = effect.token,
                            data = effect.data
                        ),
                        time = null
                    )
            }
    }

    private class BootstrapperImpl(
        private val audioRecorder: AudioRecorder
    ) : Bootstrapper<Action> {

        override fun invoke(): Observable<Action> =
            audioRecorder
                .updates
                .map { event ->
                    when (event) {
                        is AudioRecorder.Event.Started -> Action.HandleRecordingStarted
                        is AudioRecorder.Event.Finished ->
                            Action.HandleRecordingFinished(
                                event.token,
                                event.data
                            )
                        is AudioRecorder.Event.ErrorHappened -> Action.HandleError(event.throwable)
                        is AudioRecorder.Event.AmplitudeUpdated ->
                            Action.HandleAmplitudeUpdated(event.amplitude)
                        is AudioRecorder.Event.TimeUpdated -> Action.HandleTimeUpdated(event.millis)
                    }
                }
    }

    private class NewsPublisherImpl : NewsPublisher<Action, Effect, AudioRecordingState, News> {

        override fun invoke(action: Action, effect: Effect, state: AudioRecordingState): News? =
            when (effect) {
                is Effect.ErrorHappened -> News.Error
                is Effect.RecordingFinished -> News.Finished
                else -> null
            }
    }
}