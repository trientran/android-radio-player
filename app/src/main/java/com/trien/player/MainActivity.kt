package com.trien.player

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast

import com.trien.player.radio.PlaybackStatus
import com.trien.player.radio.RadioManager
import kotlinx.android.synthetic.main.activity_main.*

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : AppCompatActivity() {

    private lateinit var radioManager: RadioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioManager = RadioManager.with(this)
    }

    public override fun onStart() {

        super.onStart()

        EventBus.getDefault().register(this)

    }

    public override fun onStop() {

        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {

        super.onDestroy()
        radioManager.unbind()
    }

    override fun onResume() {
        super.onResume()

        radioManager.bind()
    }

    override fun onBackPressed() {

        finish()

    }

    @Subscribe
    fun onEvent(status: String) {

        when (status) {

            PlaybackStatus.LOADING -> {
            }

            PlaybackStatus.ERROR ->

                Toast.makeText(this, R.string.no_stream, Toast.LENGTH_SHORT).show()
        }// loading

        radioPlayButton.setImageResource(if (status == PlaybackStatus.PLAYING)
            R.drawable.pause_button
        else
            R.drawable.play_button)

    }

    fun playButtonPressed(view: View) {
        val streamURL = getString(R.string.radio_url)
        radioManager.playOrPause(streamURL)
    }
}
