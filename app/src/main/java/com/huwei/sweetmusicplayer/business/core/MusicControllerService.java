package com.huwei.sweetmusicplayer.business.core;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.huwei.sweetmusicplayer.IMusicControllerService;
import com.huwei.sweetmusicplayer.business.main.MainActivity;
import com.huwei.sweetmusicplayer.R;
import com.huwei.sweetmusicplayer.data.api.RetrofitFactory;
import com.huwei.sweetmusicplayer.data.api.SimpleObserver;
import com.huwei.sweetmusicplayer.data.api.baidu.BaiduMusicService;
import com.huwei.sweetmusicplayer.data.models.AbstractMusic;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Song;
import com.huwei.sweetmusicplayer.data.models.baidumusic.resp.SongPlayResp;
import com.huwei.sweetmusicplayer.data.contants.Contants;
import com.huwei.sweetmusicplayer.business.recievers.BringToFrontReceiver;
import com.huwei.sweetmusicplayer.frameworks.image.GlideApp;
import com.huwei.sweetmusicplayer.util.Environment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.huwei.sweetmusicplayer.util.ext.ExtKt.toast;

/**
 * 后台控制播放音乐的service
 */
public class MusicControllerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, Contants {

    private String TAG = "MusicControllerService";
    private int musicIndex = -1;
    private List<AbstractMusic> musicList;

    private MediaPlayer mp;
    private boolean mIsPrepared;

    NotificationManager mNoticationManager;
    Notification mNotification;
    RemoteViews reViews;

    private boolean isForeground;

    public static final int MSG_CURRENT = 0;
    public static final int MSG_BUFFER_UPDATE = 1;

    public static final int MSG_NOTICATION_UPDATE = 2;

    public static final int MSG_PLAY = 101;

    public static final String PLAYPRO_EXIT = "com.huwei.intent.PLAYPRO_EXIT_ACTION";
    public static final String NEXTSONG = "com.intent.action.NEXTSONG";
    public static final String PRESONG = "com.intent.action.PRESONG";
    public static final String PLAY_OR_PASUE = "com.intent.action.PLAY_OR_PASUE";

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_CURRENT:
                    if (!mIsPrepared) return;
//                    Log.i("currentTime", "currentTime Called");
                    Intent intent = new Intent(CURRENT_UPDATE);
                    int currentTime = mp.getCurrentPosition();

                    intent.putExtra("currentTime", currentTime);
                    sendBroadcast(intent);

                    handler.sendEmptyMessageDelayed(MSG_CURRENT, 500);
                    break;
                case MSG_BUFFER_UPDATE:

                    intent = new Intent(BUFFER_UPDATE);
                    int bufferTime = msg.arg1;
                    Log.i("bufferTime", bufferTime + "");
                    intent.putExtra("bufferTime", bufferTime);
                    sendBroadcast(intent);
                    break;
                case MSG_NOTICATION_UPDATE:
                    reViews.setImageViewBitmap(R.id.img_album, (Bitmap) msg.obj);
                    break;
                case MSG_PLAY:
                    AbstractMusic music = (AbstractMusic) msg.obj;
                    playMusic(music);
                    break;
            }
        }
    };

    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PLAYPRO_EXIT:
                    stopSelf();
                    mNoticationManager.cancel(NT_PLAYBAR_ID);

                    Process.killProcess(Process.myPid());
                    break;
                case NEXTSONG:
                    try {
                        mBinder.nextSong();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;
                case PRESONG:
                    try {
                        mBinder.preSong();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case PLAY_OR_PASUE:
                    try {
                        if (mBinder.isPlaying()) {
                            //暂停

                            mBinder.pause();
                        } else {
                            //播放

                            mBinder.play();
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    private IMusicControllerService.Stub mBinder = new IMusicControllerService.Stub() {
        @Override
        public int getPid() throws RemoteException {
            return Process.myPid();
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void play() throws RemoteException {
            if (!mIsPrepared) return;
            reViews.setViewVisibility(R.id.button_play_notification_play, View.GONE);
            reViews.setViewVisibility(R.id.button_pause_notification_play, View.VISIBLE);

            mNoticationManager.notify(NT_PLAYBAR_ID, mNotification);

            //准备播放源，准备后播放
            AbstractMusic music = musicList.get(getPlayingSongIndex());

            Log.i(TAG, "play()->" + music.getTitle());
            if (!mp.isPlaying()) {
                Log.i(TAG, "Enterplay()");
                mp.start();
                updatePlayStaute(true);
            }
        }

        @Override
        public void pause() throws RemoteException {
            if (!mIsPrepared) return;
            reViews.setViewVisibility(R.id.button_play_notification_play, View.VISIBLE);
            reViews.setViewVisibility(R.id.button_pause_notification_play, View.GONE);

            mNoticationManager.notify(NT_PLAYBAR_ID, mNotification);

            mp.pause();
            handler.removeMessages(MSG_CURRENT);

            updatePlayStaute(false);
        }

        @Override
        public void stop() throws RemoteException {
            stopForeground(true);
        }

        @Override
        public void seekTo(int mesc) throws RemoteException {
            if (!mIsPrepared) return;
            mp.seekTo(mesc);
        }

        @Override
        public void preparePlayingList(int index, List list) throws RemoteException {
            musicIndex = index;
            musicList = list;

            Log.d(TAG, "musicList:" + list + " musicIndex:" + index + "now title:" + ((AbstractMusic) list.get(index)).getTitle());

            if (musicList == null || musicList.size() == 0) {
                toast(getBaseContext(), "播放列表为空", Toast.LENGTH_LONG);
                return;
            }

            AbstractMusic song = musicList.get(musicIndex);
            prepareSong(song);
        }

        @Override
        public boolean isPlaying() {
            return mIsPrepared && mp != null && mp.isPlaying();
        }

        @Override
        public int getPlayingSongIndex() throws RemoteException {
            return musicIndex;
        }

        @Override
        public AbstractMusic getNowPlayingSong() throws RemoteException {
            try {
                return musicList.get(musicIndex);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Environment.getRecentMusic();
        }

        @Override
        public boolean isForeground() throws RemoteException {
            return isForeground;
        }

        @Override
        public void nextSong() throws RemoteException {
            musicIndex = (musicIndex + 1) % musicList.size();
            prepareSong(musicList.get(musicIndex));
        }

        @Override
        public void preSong() throws RemoteException {
            musicIndex = (musicIndex - 1) % musicList.size();
            prepareSong(musicList.get(musicIndex));
        }

        @Override
        public void randomSong() throws RemoteException {
            musicIndex = new Random().nextInt(musicList.size());
            prepareSong(musicList.get(musicIndex));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mNoticationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotification = new Notification();

        if (mp != null) {
            mp.release();
            mp.reset();
        }

        if (mp == null) {
            mp = getMediaPlayer(getBaseContext());
        }
        mp.setOnCompletionListener(this);
        mp.setOnBufferingUpdateListener(this);
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                Log.i(TAG, "onPrepared");
                mIsPrepared = true;

                player.start();
                updatePlayStaute(true);
                handler.sendEmptyMessage(MSG_CURRENT);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(PLAYPRO_EXIT);
        filter.addAction(PRESONG);
        filter.addAction(NEXTSONG);
        filter.addAction(PLAY_OR_PASUE);
        filter.addAction(BringToFrontReceiver.ACTION_BRING_TO_FRONT);
        registerReceiver(controlReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "mBinder:" + mBinder);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.removeMessages(MSG_CURRENT);
        mp.release();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        isForeground = false;

        unregisterReceiver(controlReceiver);
        super.onDestroy();
    }

    /**
     * 和上一次操作的歌曲不同，代表新播放的歌曲
     *
     * @param isNewPlayMusic
     */
    private void updatePlayBar(boolean isNewPlayMusic, AbstractMusic music) {
        Intent intent = new Intent(PLAYBAR_UPDATE);
        intent.putExtra("isNewPlayMusic", isNewPlayMusic);
        intent.putExtra(NOW_PLAYMUSIC, (Parcelable) music);

        sendBroadcast(intent);
    }

    private void updatePlayStaute(boolean isPlaying) {
        Intent intent = new Intent(PLAY_STATUS_UPDATE);
        intent.putExtra("isPlaying", isPlaying);

        sendBroadcast(intent);
    }

    /**
     * 准备音乐并播放
     *
     * @param music
     */
    private void prepareSong(AbstractMusic music) {
        Log.d(TAG, "prepareSong music:" + new Gson().toJson(music));

        showMusicPlayerNotification(music);
        updatePlayBar(!music.isOnlineMusic(), music);

        //如果是网络歌曲,而且未从网络获取详细信息，则需要获取歌曲的详细信息
        if (music.getType() == AbstractMusic.MusicType.Online) {
            final Song song = (Song) music;
            if (!song.hasGetDetailInfo()) {

                //同步请求到歌曲信息
                RetrofitFactory.Companion.create(BaiduMusicService.class)
                        .querySong(song.song_id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SimpleObserver<SongPlayResp>() {
                            @Override
                            public void onSuccess(SongPlayResp resp) {
                                song.bitrate = resp.bitrate;
                                song.songinfo = resp.songinfo;

                                Log.i(TAG, "song hasGetDetailInfo:" + song);

                                updatePlayBar(true, song);

                                Message msg = Message.obtain();
                                msg.what = MSG_PLAY;
                                msg.obj = song;
                                handler.sendMessage(msg);

                                updateArtistView(song);
                            }
                        });
            } else {
                playMusic(music);
            }
        } else {
            playMusic(music);
        }
    }

    private void playMusic(AbstractMusic music) {
        if (mp != null) {
            mIsPrepared = false;
            mp.reset();
        }

        try {
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Log.i(TAG, "datasoure:" + music.getDataSoure());
            mp.setDataSource(getBaseContext(), music.getDataSoure());

            mp.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        AbstractMusic music = null;
        try {
            music = mBinder.getNowPlayingSong();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Message msg = Message.obtain();
        msg.what = MSG_BUFFER_UPDATE;
        msg.arg1 = percent * music.getDuration() / 100;

        handler.sendMessage(msg);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            Log.i(TAG, "onCompletion");
            mBinder.nextSong();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在通知栏显示音乐播放信息
     *
     * @param music
     */
    void showMusicPlayerNotification(AbstractMusic music) {
        String title = music.getTitle();
        String artist = music.getArtist();
        if (reViews == null) {
            reViews = new RemoteViews(getPackageName(), R.layout.notification_play);
        }

        mNotification.icon = R.drawable.sweet;
        mNotification.tickerText = title + "-" + artist;
        mNotification.when = System.currentTimeMillis();
        mNotification.flags = Notification.FLAG_NO_CLEAR;
        mNotification.contentView = reViews;

        reViews.setTextViewText(R.id.title, title);
        reViews.setTextViewText(R.id.text, artist);

        reViews.setImageViewResource(R.id.img_album, R.drawable.img_album_background);
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);
        reViews.setOnClickPendingIntent(R.id.nt_container, pendingIntent);

        Intent exitIntent = new Intent(PLAYPRO_EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, exitIntent, 0);
        reViews.setOnClickPendingIntent(R.id.button_exit_notification_play, exitPendingIntent);

        Intent nextInent = new Intent(NEXTSONG);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, nextInent, 0);
        reViews.setOnClickPendingIntent(R.id.button_next_notification_play, nextPendingIntent);

        Intent preInent = new Intent(PRESONG);
        PendingIntent prePendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, preInent, 0);
        reViews.setOnClickPendingIntent(R.id.button_previous_notification_play, prePendingIntent);

        Intent playInent = new Intent(PLAY_OR_PASUE);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, playInent, 0);
        reViews.setOnClickPendingIntent(R.id.button_play_notification_play, playPendingIntent);
        reViews.setOnClickPendingIntent(R.id.button_pause_notification_play, playPendingIntent);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setContent(reViews).setSmallIcon(NT_PLAYBAR_ID).setTicker(title).setOngoing(true);

        updateArtistView(music);

        mNoticationManager.notify(NT_PLAYBAR_ID, mNotification);
    }

    void updateArtistView(final AbstractMusic music) {
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    GlideApp.with(getBaseContext()).
                            asBitmap().load(music.getArtPic()).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            if (reViews != null) {
                                reViews.setImageViewBitmap(R.id.img_album, resource);

                                mNoticationManager.notify(NT_PLAYBAR_ID, mNotification);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static MediaPlayer getMediaPlayer(Context context) {

        MediaPlayer mediaplayer = new MediaPlayer();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return mediaplayer;
        }

        try {
            Class<?> cMediaTimeProvider = Class.forName("android.media.MediaTimeProvider");
            Class<?> cSubtitleController = Class.forName("android.media.SubtitleController");
            Class<?> iSubtitleControllerAnchor = Class.forName("android.media.SubtitleController$Anchor");
            Class<?> iSubtitleControllerListener = Class.forName("android.media.SubtitleController$Listener");

            Constructor constructor = cSubtitleController.getConstructor(new Class[]{Context.class, cMediaTimeProvider, iSubtitleControllerListener});

            Object subtitleInstance = constructor.newInstance(context, null, null);

            Field f = cSubtitleController.getDeclaredField("mHandler");

            f.setAccessible(true);
            try {
                f.set(subtitleInstance, new Handler());
            } catch (IllegalAccessException e) {
                return mediaplayer;
            } finally {
                f.setAccessible(false);
            }

            Method setsubtitleanchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);

            setsubtitleanchor.invoke(mediaplayer, subtitleInstance, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mediaplayer;
    }


}
