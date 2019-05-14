package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.example.musicplayer.useLitepal.PlayingMusic;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer player;
    private List<Music> playingmusic_list;
    private List<OnStateChangeListenr> listenrList;
    private IBinder binder;
    public static int mode = Utils.TYPE_ORDER;  //默认顺序播放模式

    private boolean mPaused;     //当前是否为播放暂停状态
    private Music currentMusic; //存放当前要播放的音乐
    private final int MSG_PROGRESS_UPDATE = 0;  //定义循环发送的消息

    private AudioManager mAudioManager;
    private  boolean mPausedByTransientLossOfFocus;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();   //初始化播放器
        player.setOnCompletionListener(onCompletionListener);   //设置播放完成的监听器

        //初始化播放列表
        initPlayList();

        listenrList = new ArrayList<>(); //初始化监听器列表
        binder = new MusicServiceIBinder();
        mPaused = false;

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE); //获得音频管理服务
        mPausedByTransientLossOfFocus = false;  //默认重新获得音频焦点时不自动播放
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();

        mAudioManager.abandonAudioFocus(mAudioFocusListener); //注销音频管理服务

        playingmusic_list.clear();
        listenrList.clear();
        //停止发送更新消息
        handler.removeMessages(MSG_PROGRESS_UPDATE);
    }

    //对外监听器接口
    public interface OnStateChangeListenr {
        void onPlayProgressChange(Music item);  //播放进度变化

        void onPlay(Music item);    //播放状态变化

        void onPause(Music item);   //播放状态变化
    }

    //定义binder与活动通信
    public class MusicServiceIBinder extends Binder {

        //添加一首歌曲
        public void addPlayList(Music item) {
            if (playingmusic_list.contains(item)) {
                currentMusic = item;
                mPaused = false;    //添加音乐之后一定需要重新加载
                playInner();
                return;
            }
            playingmusic_list.add(0, item);
            PlayingMusic playingMusic = new PlayingMusic(item.songUrl, item.title, item.artist, item.duration, item.played, item.imgUrl, Utils.byteImg(item.img));
            playingMusic.save();
            //添加完成后，开始播放
            currentMusic = playingmusic_list.get(0);
            mPaused = false;    //添加音乐之后一定需要重新加载
            playInner();
        }

        //添加多首歌曲
        public void addPlayList(List<Music> items) {
            playingmusic_list.clear();
            LitePal.deleteAll(PlayingMusic.class);
            playingmusic_list.addAll(items);
            for (Music i: items){
                PlayingMusic playingMusic = new PlayingMusic(i.songUrl, i.title, i.artist, i.duration, i.played, i.imgUrl, Utils.byteImg(i.img));
                playingMusic.save();
            }
            //添加完成后，开始播放
            currentMusic = playingmusic_list.get(0);
            playInner();
        }

        //移除一首歌曲
        public void removePlayList(int i) {
            playingmusic_list.remove(i);
            LitePal.deleteAll(PlayingMusic.class, "title=?", playingmusic_list.get(i).title);
        }

        public void play() {
            playInner();
        }

        public void playNext() {
            playNextInner();
        }

        public void playPre() {
            int currentIndex = playingmusic_list.indexOf(currentMusic);
            if (currentIndex - 1 >= 0) {
                //获取当前播放（或者被加载）音乐的上一首音乐
                //如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
                //并重新加载该音乐，开始播放
                currentMusic = playingmusic_list.get(currentIndex - 1);
                playMusicItem(currentMusic, true);
            }
        }

        public void pause() {
            pauseInner();
        }

        public void seekTo(int pos) {
            //将音乐拖动到指定的时间
            player.seekTo(pos);
        }

        //有活动注册监听器时将它加到监听器列表
        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.add(l);
        }

        //有活动注销监听器时将它从监听器列表移除
        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.remove(l);
        }

        public Music getCurrentMusic() {
            //返回当前正加载好的音乐
            return currentMusic;
        }

        public boolean isPlaying() {
            //返回当前的播放器是否正在播放音乐
            return player.isPlaying();
        }

        public List<Music> getPlayingList() {
            return playingmusic_list;
        }

    }

    //初始化播放列表
    private void initPlayList() {
        playingmusic_list = new ArrayList<>();
        List<PlayingMusic> list = LitePal.findAll(PlayingMusic.class);
        Bitmap img = null;
        for (PlayingMusic i : list) {
            if (i.img != null)  img = BitmapFactory.decodeByteArray(i.img,0, i.img.length);
            Music m = new Music(i.songUrl, i.title, i.artist, i.duration, 0, i.imgUrl, img);
            playingmusic_list.add(m);
        }
        if (playingmusic_list.size() > 0) {
            currentMusic = playingmusic_list.get(0);
        }
    }

    //将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(Music item) {
        try {
            //重置播放器状态
            player.reset();
            //设置播放音乐的地址
            player.setDataSource(MusicService.this, Uri.parse(item.songUrl));
            //准备播放音乐
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(Music item, boolean reload) {

        if (item == null) {
            return;
        }
        if (reload) {
            //需要重新加载音乐
            prepareToPlay(item);
        }
        //开始播放，如果之前只是暂停播放，那么音乐将继续播放
        player.start();

        //将播放的状态通过监听器通知给监听者
        for (OnStateChangeListenr l : listenrList) {
            item.played = player.getCurrentPosition();
            item.duration = player.getDuration();
            l.onPlay(item);
        }
        //设置为非暂停播放状态
        mPaused = false;

        //移除现有的更新消息，重新启动更新
        handler.removeMessages(MSG_PROGRESS_UPDATE);
        handler.sendEmptyMessage(MSG_PROGRESS_UPDATE);
    }

    private void playInner() {

        //获取音频焦点
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        //如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if (currentMusic == null && playingmusic_list.size() > 0) {
            currentMusic = playingmusic_list.get(0);
        }

        //如果是从暂停状态恢复播放音乐，那么不需要重新加载音乐；
        //如果是从完全没有播放过的状态开始播放音乐，那么就需要重新加载音乐
        if (mPaused) {
            playMusicItem(currentMusic, false);
        } else {
            playMusicItem(currentMusic, true);
        }
    }
    private void pauseInner(){
        //暂停当前正在播放的音乐
        player.pause();
        //将播放状态的改变通知给监听者
        for (OnStateChangeListenr l : listenrList) {
            l.onPause(currentMusic);
        }
        //设置为暂停播放状态
        mPaused = true;
        //停止发送更新消息
        handler.removeMessages(MSG_PROGRESS_UPDATE);
    }

    private void playNextInner() {

        if (mode == Utils.TYPE_RANDOM){
            //随机播放
            int i = (int) (0 + Math.random() * (playingmusic_list.size() + 1));
            currentMusic = playingmusic_list.get(i);
            playMusicItem(currentMusic, true);
        }
        else {
            //列表循环
            int currentIndex = playingmusic_list.indexOf(currentMusic);
            if (currentIndex < playingmusic_list.size() - 1) {
                //获取当前播放（或者被加载）音乐的下一首音乐
                //如果后面有要播放的音乐，把那首音乐设置成要播放的音乐
                //并重新加载该音乐，开始播放
                currentMusic = playingmusic_list.get(currentIndex + 1);
                playMusicItem(currentMusic, true);
            } else {
                currentMusic = playingmusic_list.get(0);
                playMusicItem(currentMusic, true);
            }
        }
    }

    //当前歌曲播放完成的监听器
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            Utils.count ++; //累计听歌数量+1

            if (mode == Utils.TYPE_SINGLE) {
                //单曲循环
                playMusicItem(currentMusic, true);
            }
            else {
                playNextInner();
            }
        }
    };

    //定义处理进度更新消息的Handler
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE: {
                    //将音乐的时长和当前播放的进度保存到数据结构中，
                    currentMusic.played = player.getCurrentPosition();
                    currentMusic.duration = player.getDuration();

                    //通知监听者当前的播放进度
                    for (OnStateChangeListenr l : listenrList) {
                        l.onPlayProgressChange(currentMusic);
                    }

                    //间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, 1000);
                }
                break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //当组件bindService()之后，将这个Binder返回给组件使用
        return binder;
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener(){

        public void onAudioFocusChange(int focusChange) {
            switch(focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(player.isPlaying()){
                        //会长时间失去，所以告知下面的判断，获得焦点后不要自动播放
                        mPausedByTransientLossOfFocus = false;
                        pauseInner();//因为会长时间失去，所以直接暂停
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        mPausedByTransientLossOfFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        mPausedByTransientLossOfFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点，且符合播放条件，开始播放
                    if(!player.isPlaying()&&mPausedByTransientLossOfFocus){
                        mPausedByTransientLossOfFocus = false;
                        playInner();
                    }
                    break;
            }
        }
    };
}
