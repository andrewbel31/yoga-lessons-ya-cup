package com.andreibelous.yogalessons.mapper

import com.andreibelous.yogalessons.recording.AudioRecordingFeature
import com.andreibelous.yogalessons.view.AudioRecordingView

internal object NewsToViewAction : (AudioRecordingFeature.News) -> AudioRecordingView.Action? {

    override fun invoke(news: AudioRecordingFeature.News): AudioRecordingView.Action? =
        when (news) {
            is AudioRecordingFeature.News.Finished -> AudioRecordingView.Action.ShowResultsDialog
            is AudioRecordingFeature.News.Error -> AudioRecordingView.Action.ShowError
        }
}