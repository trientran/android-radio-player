package com.trien.player.radio

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.trien.player.R

import org.greenrobot.eventbus.EventBus

class RadioService : Service(), Player.EventListener, AudioManager.OnAudioFocusChangeListener {

    private val iBinder = LocalBinder()

    private var handler: Handler? = null
    private var exoPlayer: SimpleExoPlayer? = null
    var mediaSession: MediaSessionCompat? = null
        private set
    private var transportControls: MediaControllerCompat.TransportControls? = null

    private var onGoingCall = false
    private var telephonyManager: TelephonyManager? = null

    private var wifiLock: WifiManager.WifiLock? = null

    private var audioManager: AudioManager? = null

    private var notificationManager: MediaNotificationManager? = null

    private var serviceInUse = false

    var status: String? = null
        private set

    private var strAppName: String? = null
    private var strLiveBroadcast: String? = null
    private var streamUrl: String? = null

    private val becomingNoisyReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            pause()

        }

    }

    private val phoneStateListener = object : PhoneStateListener() {

        override fun onCallStateChanged(state: Int, incomingNumber: String) {

            if (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_RINGING) {

                if (!isPlaying) return

                onGoingCall = true
                stop()

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {

                if (!onGoingCall) return

                onGoingCall = false
                resume()

            }
        }

    }

    private val mediasSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPause() {
            super.onPause()

            pause()
        }

        override fun onStop() {
            super.onStop()

            stop()

            notificationManager!!.cancelNotification()
        }

        override fun onPlay() {
            super.onPlay()

            resume()
        }
    }

    val isPlaying: Boolean
        get() = this.status == PlaybackStatus.PLAYING

    inner class LocalBinder : Binder() {
        val service: RadioService
            get() = this@RadioService
    }

    override fun onBind(intent: Intent): IBinder? {

        serviceInUse = true

        return iBinder

    }

    override fun onCreate() {
        super.onCreate()

        strAppName = resources.getString(R.string.app_name)
        strLiveBroadcast = resources.getString(R.string.notification_title)

        onGoingCall = false

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        notificationManager = MediaNotificationManager(this)

        wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock")

        mediaSession = MediaSessionCompat(this, javaClass.simpleName)
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession!!.setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, strAppName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, strLiveBroadcast)
                .build())
        mediaSession!!.setCallback(mediasSessionCallback)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        handler = Handler()
        /*  DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);*/
        exoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
        exoPlayer!!.addListener(this)

        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        status = PlaybackStatus.IDLE

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val action = intent.action

        if (TextUtils.isEmpty(action))
            return Service.START_NOT_STICKY

        val result = audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            stop()

            return Service.START_NOT_STICKY
        }

        if (action!!.equals(ACTION_PLAY, ignoreCase = true)) {

            transportControls!!.play()

        } else if (action.equals(ACTION_PAUSE, ignoreCase = true)) {

            transportControls!!.pause()

        } else if (action.equals(ACTION_STOP, ignoreCase = true)) {

            transportControls!!.stop()
        }

        return Service.START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent): Boolean {

        serviceInUse = false

        if (status == PlaybackStatus.IDLE)
            stopSelf()

        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent) {

        serviceInUse = true

    }

    override fun onDestroy() {

        pause()

        exoPlayer!!.release()
        exoPlayer!!.removeListener(this)

        if (telephonyManager != null)
            telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        notificationManager!!.cancelNotification()

        mediaSession!!.release()

        unregisterReceiver(becomingNoisyReceiver)

        super.onDestroy()

    }

    override fun onAudioFocusChange(focusChange: Int) {

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {

                exoPlayer!!.volume = 0.8f

                resume()
            }

            AudioManager.AUDIOFOCUS_LOSS ->

                stop()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->

                if (isPlaying) pause()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->

                if (isPlaying)
                    exoPlayer!!.volume = 0.1f
        }

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        when (playbackState) {
            Player.STATE_BUFFERING -> status = PlaybackStatus.LOADING
            Player.STATE_ENDED -> status = PlaybackStatus.STOPPED
            Player.STATE_IDLE -> status = PlaybackStatus.IDLE
            Player.STATE_READY -> status = if (playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
            else -> status = PlaybackStatus.IDLE
        }

        if (status != PlaybackStatus.IDLE)
            notificationManager!!.startNotification(status)

        EventBus.getDefault().post(status)
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {

        EventBus.getDefault().post(PlaybackStatus.ERROR)

    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

    }

    override fun onSeekProcessed() {

    }

    fun play(streamUrl: String) {

        this.streamUrl = streamUrl

        if (wifiLock != null && !wifiLock!!.isHeld) {

            wifiLock!!.acquire()

        }

        /*DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getClass().getSimpleName()), bandwidthMeter);
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(Uri.parse(streamUrl), dataSourceFactory, extractorsFactory, handler, null);
*/

        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, javaClass.simpleName))
        val mediaSource = DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(streamUrl))

        exoPlayer!!.prepare(mediaSource)
        exoPlayer!!.playWhenReady = true

    }


    fun resume() {

        streamUrl?.let { play(it) }

    }

    fun pause() {

        exoPlayer!!.playWhenReady = false

        audioManager!!.abandonAudioFocus(this)
        wifiLockRelease()
    }

    fun stop() {

        exoPlayer!!.stop()

        audioManager!!.abandonAudioFocus(this)
        wifiLockRelease()
    }

    fun playOrPause(url: String) {

        if (streamUrl != null && streamUrl == url) {

            if (!isPlaying) {

                play(streamUrl!!)

            } else {

                pause()

            }

        } else {

            if (isPlaying) {

                pause()

            }

            play(url)

        }

    }

    private fun wifiLockRelease() {

        if (wifiLock != null && wifiLock!!.isHeld) {

            wifiLock!!.release()

        }

    }

    companion object {

        val ACTION_PLAY = "com.trien.player.ACTION_PLAY"
        val ACTION_PAUSE = "com.trien.player.ACTION_PAUSE"
        val ACTION_STOP = "com.trien.player.ACTION_STOP"
    }
}
