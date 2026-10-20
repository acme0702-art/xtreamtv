package com.example.xtreamtv

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var title: TextView
    private lateinit var sp: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var cur = ""
    private var triedTs = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sp = getSharedPreferences("pos", MODE_PRIVATE)

        val view = PlayerView(this).apply { useController = !PlayQueue.live }
        title = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 24f
            setBackgroundColor(0x99000000.toInt())
            setPadding(32, 16, 32, 16); visibility = View.GONE
        }
        setContentView(FrameLayout(this).apply {
            addView(view)
            addView(title, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        })

        val ds = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0").setAllowCrossProtocolRedirects(true)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ds)).build().also { p ->
                view.player = p
                p.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        if (PlayQueue.live && cur.endsWith(".m3u8") && !triedTs) {
                            triedTs = true
                            start(cur.removeSuffix(".m3u8") + ".ts")
                        } else {
                            Toast.makeText(this@PlayerActivity, "Error de reproducción", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED && !PlayQueue.live) {
                            sp.edit().remove(cur).apply()
                            if (PlayQueue.index + 1 < PlayQueue.urls.size) {
                                PlayQueue.index++; play()
                            }
                        }
                    }
                })
            }
        play()
    }

    private fun play() {
        triedTs = false
        showTitle(PlayQueue.titles[PlayQueue.index])
        start(PlayQueue.urls[PlayQueue.index])
    }

    private fun start(url: String) {
        cur = url
        val pos = if (PlayQueue.live) 0L else sp.getLong(url, 0L)
        player?.setMediaItem(MediaItem.fromUri(url), pos)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun showTitle(t: String) {
        title.text = t; title.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ title.visibility = View.GONE }, 3000)
    }

    private fun zap(d: Int) {
        val n = PlayQueue.urls.size
        PlayQueue.index = (PlayQueue.index + d + n) % n
        play()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (PlayQueue.live && e.action == KeyEvent.ACTION_DOWN) {
            when (e.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { zap(1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER ->
                    { showTitle(PlayQueue.titles[PlayQueue.index]); return true }
            }
        }
        return super.dispatchKeyEvent(e)
    }

    override fun onStart() { super.onStart(); player?.playWhenReady = true }

    override fun onStop() {
        player?.let { if (!PlayQueue.live) sp.edit().putLong(cur, it.currentPosition).apply(); it.pause() }
        super.onStop()
    }

    override fun onDestroy() { player?.release(); super.onDestroy() }
}
