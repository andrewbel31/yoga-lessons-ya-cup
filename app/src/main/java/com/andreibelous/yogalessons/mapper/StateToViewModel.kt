package com.andreibelous.yogalessons.mapper

import com.andreibelous.yogalessons.recording.AudioRecordingState
import com.andreibelous.yogalessons.recording.AudioRecordingState.Step
import com.andreibelous.yogalessons.view.AudioRecordingViewModel

internal object StateToViewModel : (AudioRecordingState) -> AudioRecordingViewModel {

    override fun invoke(state: AudioRecordingState): AudioRecordingViewModel =
        AudioRecordingViewModel(
            amplitude = state.amplitude,
            time = state.time?.millisToTime(),
            step = state.step.toVmStep()
        )

    private fun Step.toVmStep(): AudioRecordingViewModel.Step =
        when (this) {
            is Step.Initial -> AudioRecordingViewModel.Step.Initial
            is Step.Recording -> AudioRecordingViewModel.Step.Recording
            is Step.Error -> AudioRecordingViewModel.Step.Error(this.throwable)
            is Step.Finished -> AudioRecordingViewModel.Step.Finished(token, data)
        }


    private fun Long.millisToTime(): String? {
        if (this == 0L) {
            return null
        }
        val minutes = this / 1000 / 60
        val seconds = this / 1000 % 60

        val secondsStr = seconds.toString()
        val secs: String = if (secondsStr.length >= 2) {
            secondsStr.substring(0, 2)
        } else {
            "0$secondsStr"
        }
        return "$minutes:$secs"
    }
}