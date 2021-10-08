package com.andreibelous.yogalessons.recording

data class AudioRecordingState(
    val time: Long? = 0,
    val amplitude: Int = 0,
    val step: Step = Step.Initial
) {

    sealed interface Step {

        object Initial : Step
        object Recording : Step
        data class Error(val throwable: Throwable) : Step
        data class Finished(val token: String, val data: ProcessedResult) : Step
    }
}