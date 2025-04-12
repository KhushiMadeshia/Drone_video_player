package com.example.drone_video_player

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drone_video_player.databinding.ActivityMainBinding
import android.util.Log
import android.widget.Toast
import java.io.File
import android.Manifest
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper

import android.util.Rational
import android.view.View
import android.widget.TextView


import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    var libVLC: LibVLC? = null
    private var isRecording = false
    private lateinit var recordTimer: TextView
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var startTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        recordTimer = binding!!.contentMain.recordingTimer



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
            }
        }



        libVLC = LibVLC(this)
        binding!!.contentMain.buttonStream.setOnClickListener {

            val rtspUrl = binding!!.contentMain.rtspInput.text.toString().trim()
            val media = Media(libVLC, Uri.parse(rtspUrl))
            media.addOption("--aout=opensles")
            media.addOption("--audio-time-stretch")
            media.addOption("-vvv") // verbosity
            val mediaPlayer = MediaPlayer(libVLC)
            mediaPlayer.media = media
            mediaPlayer.vlcVout.setVideoSurface(
                binding!!.contentMain.videoView.getHolder().getSurface(),
                binding!!.contentMain.videoView.getHolder()
            )
            mediaPlayer.vlcVout.setWindowSize(
                binding!!.contentMain.videoView.getWidth(),
                binding!!.contentMain.videoView.getHeight()
            )
            mediaPlayer.vlcVout.attachViews()
            mediaPlayer.play()
        }

        binding!!.contentMain.recordButton.setOnClickListener {
            val rtspUrl = binding!!.contentMain.rtspInput.text.toString().trim()
            if (rtspUrl.isEmpty()) {
                Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isRecording) {
                startRecordingStream(rtspUrl)
            } else {
                stopRecordingStream()
            }
        }

        binding!!.contentMain.pipButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val aspectRatio = Rational(16, 9)
                val pipBuilder = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(pipBuilder)
            } else {
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }


    }
    private fun startRecordingStream(rtspUrl: String) {
        val outputFile = File(getExternalFilesDir(null), "recorded_stream.ts")
        val outputPath = outputFile.absolutePath.replace(" ", "\\ ")

        val media = Media(libVLC, rtspUrl)
        media.setHWDecoderEnabled(true, false)

        // Display on screen AND write to file
        val sout = ":sout=#duplicate{dst=display,dst=std{access=file,mux=ts,dst=$outputPath}}"
        media.addOption(sout)
        media.addOption(":network-caching=300")
        media.addOption(":rtsp-tcp")
        val mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()

        isRecording = true
        binding!!.contentMain.recordButton.text = "Stop Recording"
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        Log.d("RECORDING", "Started recording to: $outputPath")
        startTimer()

    }
    private fun stopRecordingStream() {
        val mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.stop()
        isRecording = false
        binding!!.contentMain.recordButton.text = "Start Recording"
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
        Log.d("RECORDING", "Stopped recording.")
        stopTimer()

    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val mediaPlayer = MediaPlayer(libVLC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !isInPictureInPictureMode &&
            mediaPlayer.isPlaying) {

            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration

    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // Hide all UI except video
            binding?.contentMain?.rtspInput?.visibility = View.GONE
            binding?.contentMain?.buttonStream?.visibility = View.GONE
            binding?.contentMain?.recordButton?.visibility = View.GONE
            binding?.contentMain?.pipButton?.visibility = View.GONE
        } else {
            // Show everything again when back
            binding?.contentMain?.rtspInput?.visibility = View.VISIBLE
            binding?.contentMain?.buttonStream?.visibility = View.VISIBLE
            binding?.contentMain?.recordButton?.visibility = View.VISIBLE
            binding?.contentMain?.pipButton?.visibility = View.VISIBLE
        }
    }
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        recordTimer.visibility = View.VISIBLE

        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = (elapsed / 1000).toInt()
                val minutes = seconds / 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds % 60)
                recordTimer.text = timeFormatted
                timerHandler.postDelayed(this, 1000)
            }
        }

        timerHandler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        recordTimer.text = "00:00"
        recordTimer.visibility = View.GONE
    }


}