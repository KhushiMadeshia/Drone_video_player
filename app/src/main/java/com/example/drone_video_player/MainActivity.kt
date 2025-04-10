package com.example.drone_video_player

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drone_video_player.R
import com.example.drone_video_player.databinding.ActivityMainBinding

import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    var libVLC: LibVLC? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())



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
    }
}