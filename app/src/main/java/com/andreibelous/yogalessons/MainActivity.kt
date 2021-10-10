package com.andreibelous.yogalessons

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.andreibelous.yogalessons.mapper.NewsToViewAction
import com.andreibelous.yogalessons.mapper.StateToViewModel
import com.andreibelous.yogalessons.mapper.UiEventToWish
import com.andreibelous.yogalessons.recording.AudioRecorder
import com.andreibelous.yogalessons.recording.AudioRecordingFeature
import com.andreibelous.yogalessons.view.AudioRecordingView
import com.badoo.binder.Binder
import com.badoo.binder.using
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private var disposables: CompositeDisposable? = null
    private var audioRecorder: AudioRecorder? = null
    private var audioFeature: AudioRecordingFeature? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        disposables = CompositeDisposable()
        val view = AudioRecordingView(findViewById(R.id.root), lifecycle)
        val recorder = AudioRecorder(applicationContext).also { audioRecorder = it }
        val feature = AudioRecordingFeature(recorder).also { audioFeature = it }
        disposables?.add(feature)

        with(Binder(CreateDestroyBinderLifecycle(lifecycle))) {
            bind(view to feature using UiEventToWish)
            bind(feature to view using StateToViewModel)
            bind(view to ::handleUiEvent.asConsumer())
            bind(feature.news to view::execute.asConsumer() using NewsToViewAction)
        }
    }

    private fun handleUiEvent(event: AudioRecordingView.Event) {
        when (event) {
            is AudioRecordingView.Event.StartClicked -> dispatchToFeature()
            is AudioRecordingView.Event.CloseClicked -> {
                dispose()
                finish()
            }
            else -> Unit
        }
    }

    private fun dispatchToFeature() {
        val permission = Manifest.permission.RECORD_AUDIO
        val isGranted =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            audioFeature?.accept(AudioRecordingFeature.Wish.StartRecording)
        }

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

        if (shouldShowRationale) {
            // TODO
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            REQUEST_MICROPHONE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // TODO fixme

        audioFeature?.accept(AudioRecordingFeature.Wish.StartRecording)
    }

    override fun onDestroy() {
        dispose()
        super.onDestroy()
    }

    private fun dispose() {
        disposables?.dispose()
        disposables = null
        audioRecorder?.dispose()
        audioRecorder = null
    }

    private companion object {

        private const val REQUEST_MICROPHONE_PERMISSION_CODE: Int = 1001
    }
}