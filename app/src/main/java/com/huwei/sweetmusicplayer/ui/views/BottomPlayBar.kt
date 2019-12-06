package com.huwei.sweetmusicplayer.ui.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.LinearLayout
import com.huwei.sweetmusicplayer.business.playmusic.PlayMusicActivity
import com.huwei.sweetmusicplayer.R
import com.huwei.sweetmusicplayer.data.models.AbstractMusic
import com.huwei.sweetmusicplayer.data.contants.Contants
import com.huwei.sweetmusicplayer.business.core.MusicManager
import com.huwei.sweetmusicplayer.frameworks.image.BlurBitmapTransformation
import com.huwei.sweetmusicplayer.frameworks.image.GlideApp
import kotlinx.android.synthetic.main.bottom_action_bar.view.*

/**
 *
 * @author Ezio
 * @date 2017/06/04
 */
class BottomPlayBar(context: Context?) : LinearLayout(context) {

    val TAG = "BottomPlayBar"

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            when (action) {
                Contants.PLAY_STATUS_UPDATE -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    btn_play.isChecked = isPlaying
                }
                Contants.PLAYBAR_UPDATE -> {

                    val music = MusicManager.get().nowPlayingSong
                    updateBottomBar(music, MusicManager.get().isPlaying)
                }
                Contants.CURRENT_UPDATE -> pro_music.progress = intent.getIntExtra("currentTime", 0)
            }
        }

    }

    init {
        View.inflate(context!!, R.layout.bottom_action_bar, this)

        initListener()
        initRecievers()
    }

    fun initListener() {
        btn_next.setOnClickListener {
            // TODO Auto-generated method stub
            MusicManager.get().nextSong()
        }

        btn_play.setOnCheckedChangeListener { buttonView, isChecked ->
            // TODO Auto-generated method stub
            if (isChecked != MusicManager.get().isPlaying) {
                //播放意图
                if (isChecked) {
                    MusicManager.get().play()
                } else {
                    MusicManager.get().pause()
                }
            }
        }

        setOnClickListener {
            context.startActivity(PlayMusicActivity.getStartActIntent(context))
        }
    }

    fun initRecievers() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Contants.PLAYBAR_UPDATE)
        intentFilter.addAction(Contants.CURRENT_UPDATE)
        intentFilter.addAction(Contants.PLAY_STATUS_UPDATE)
        context.registerReceiver(receiver, intentFilter)
    }

    fun unRegisterRecievers() {
        context.unregisterReceiver(receiver)
    }

    internal fun updateBottomBar(music: AbstractMusic?, isPlaying: Boolean = true) {

        if (music != null) {
            tv_title.text = music.title
            tv_artist.text = music.artist
            btn_play.isChecked = isPlaying
            pro_music.max = music.duration!!

            val requst = GlideApp.with(context).load(music.artPic)
            requst.into(img_album)
            requst.clone().transform(BlurBitmapTransformation(music.blurValueOfPlaying())).into(blurBgView)
        }

    }
}