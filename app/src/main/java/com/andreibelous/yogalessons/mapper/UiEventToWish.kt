package com.andreibelous.yogalessons.mapper

import com.andreibelous.yogalessons.recording.AudioRecordingFeature.Wish
import com.andreibelous.yogalessons.view.AudioRecordingView.Event

internal class UiEventToWish() : (Event) -> Wish? {

    override fun invoke(event: Event): Wish? =
        when (event) {
            is Event.StopClicked -> Wish.FinishRecording
            else -> null
        }
}