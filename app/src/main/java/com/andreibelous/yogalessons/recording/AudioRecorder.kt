package com.andreibelous.yogalessons.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.HandlerThread
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Environment
import android.os.Build
import android.util.Log
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AudioRecorder(
    context: Context
) {

    private val _updates = PublishRelay.create<Event>()
    val updates: Observable<Event> = _updates

    private var handlerThread: HandlerThread? = HandlerThread("AudioThread").apply { start() }
    private var handler: RecordingHandler? =
        RecordingHandler(handlerThread!!.looper, context, _updates)

    fun start() {
        handler?.sendEmptyMessage(RecordingHandler.START_RECORDING)
    }

    fun finish() {
        handler?.sendEmptyMessage(RecordingHandler.FINISH_RECORDING)
    }

    fun dispose() {
        handler?.sendEmptyMessage(RecordingHandler.DISPOSE)
        handler = null
        handlerThread?.quitSafely()
        handlerThread = null
    }

    sealed interface Event {

        object Started : Event
        data class Finished(val token: String, val data: ProcessedResult) : Event
        data class AmplitudeUpdated(val amplitude: Int) : Event
        data class TimeUpdated(val millis: Long) : Event
        data class ErrorHappened(val throwable: Throwable) : Event
    }
}

class RecordingHandler(
    looper: Looper,
    private val context: Context,
    private val events: Consumer<AudioRecorder.Event>
) : Handler(looper) {

    private var mediaRecorder: MediaRecorder? = null
    private var timer: Disposable? = null
    private var lastStartTime: Long = 0
    private var file: File? = null
    private val amplitudes = mutableListOf<Int>()
    private val times = mutableListOf<Long>()
    private val processor = Processor()

    init {
        startTimer()
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            START_RECORDING -> startRecording()
            FINISH_RECORDING -> finishRecording()
            DISPOSE -> dispose()
        }
    }

    private fun startRecording() {
        try {
            amplitudes.clear()
            times.clear()
            file = File(getFileCacheDir(context), "${System.currentTimeMillis()}.3gp")
            mediaRecorder = prepareMediaRecorder()
            lastStartTime = System.currentTimeMillis()
        } catch (t: Throwable) {
            events.accept(AudioRecorder.Event.ErrorHappened(t))
            mediaRecorder?.stop()
            releaseRecorder()
        }
    }

    private fun prepareMediaRecorder(): MediaRecorder =
        MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(file?.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            }

            start()
            events.accept(AudioRecorder.Event.Started)
        }

    private fun startTimer() {
        timer = Observable.interval(0, AMPLITUDE_INTERVAL, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.from(looper))
            .subscribe(
                { _ ->
                    mediaRecorder?.let {
                        val currentTime = System.currentTimeMillis()
                        val amplitude = it.maxAmplitude
                        val ampl = if (amplitude > 0) amplitude else amplitudes.lastOrNull() ?: 0
                        events.accept(AudioRecorder.Event.AmplitudeUpdated(ampl * 5))
                        amplitudes.add(amplitude)
                        times.add(currentTime)
                        events.accept(AudioRecorder.Event.TimeUpdated(currentTime - lastStartTime))
                    } ?: run {
                        events.accept(AudioRecorder.Event.AmplitudeUpdated(0))
                    }
                },
                { finishRecording() }
            )
    }

    private fun finishRecording() {
        stop()
        releaseRecorder()
        val processed = processor.process(amplitudes, times)
        events.accept(
            AudioRecorder.Event.Finished(
                token = UUID.randomUUID().toString(),
                data = processed
            )
        )
    }

    private fun deleteFileIfExists() {
        try {
            if (file?.exists() == true) {
                file?.delete()
                file = null
            }
        } catch (ex: Exception) {

        }
    }

    private fun stop() {
        try {
            mediaRecorder?.stop()
        } catch (ignored: Exception) {

        }
    }

    private fun releaseRecorder() {
        deleteFileIfExists()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun dispose() {
        releaseRecorder()
        timer?.dispose()
        timer = null
    }

    private fun getFileCacheDir(context: Context): File =
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.externalCacheDir ?: context.cacheDir
        } else {
            context.cacheDir
        }

    companion object {

        private const val AMPLITUDE_INTERVAL = 20L

        const val START_RECORDING = 1001
        const val FINISH_RECORDING = 1002
        const val DISPOSE = 1003

        private const val TAG = "RecordingHandler"
    }
}