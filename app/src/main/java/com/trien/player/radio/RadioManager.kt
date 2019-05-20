package com.trien.player.radio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import org.greenrobot.eventbus.EventBus

class RadioManager private constructor(private val context: Context) {

    private var serviceBound: Boolean = false

    val isPlaying: Boolean
        get() = service!!.isPlaying

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {

            service = (binder as RadioService.LocalBinder).service
            serviceBound = true

        }

        override fun onServiceDisconnected(arg0: ComponentName) {

            serviceBound = false
        }
    }

    init {
        serviceBound = false
    }

    fun playOrPause(streamUrl: String) {

        if (service != null) {
            service!!.playOrPause(streamUrl)
        }
        Log.v("trien", "tr")

    }

    fun pause() {

        if (service != null) {
            service!!.pause()
        }
        Log.v("trien", "tr")

    }

    fun play(streamUrl: String) {

        if (service != null) {
            service!!.play(streamUrl)
        }
        Log.v("trien", "tr")

    }

    fun stop() {

        if (service != null) {
            service!!.stop()
        }
        Log.v("trien", "tr")

    }

    fun bind() {

        val intent = Intent(context, RadioService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        if (service != null)
            EventBus.getDefault().post(service!!.status)
    }

    fun unbind() {

        context.unbindService(serviceConnection)

    }

    companion object {

        private var instance: RadioManager? = null

        var service: RadioService? = null
            private set

        fun with(context: Context): RadioManager {

            if (instance == null)
                instance = RadioManager(context)

            return instance as RadioManager
        }
    }

}
