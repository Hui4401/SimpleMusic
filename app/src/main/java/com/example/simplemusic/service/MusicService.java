package com.example.simplemusic.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.example.simplemusic.bean.Music;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.db.PlayingMusic;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer player;
    private List<Music> playingMusicList;
    private List<OnStateChangeListenr> listenrList;
    private MusicServiceBinder binder;
    private AudioManager audioManager;
    private Music currentMusic; // 当前就绪的音乐
    private  boolean autoPlayAfterFocus;    // 获取焦点之后是否自动播放
    private boolean isNeedReload;     // 播放时是否需要重新加载
    private int playMode;  // 播放模式
    private SharedPreferences spf;

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayList();     //初始化播放列表
        listenrList = new ArrayList<>();    //初始化监听器列表
        player = new MediaPlayer();   //初始化播放器
        player.setOnCompletionListener(onCompletionListener);   //设置播放完成的监听器
        binder = new MusicServiceBinder();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE); //获得音频管理服务
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();

        playingMusicList.clear();
        listenrList.clear();
        handler.removeMessages(66);
        audioManager.abandonAudioFocus(audioFocusListener); //注销音频管理服务
    }

    //对外监听器接口
    public interface OnStateChangeListenr {
        void onPlayProgressChange(long played, long duration);  //播放进度变化
        void onPlay(Music item);    //播放状态变化
        void onPause();   //播放状态变化
    }

    //定义binder与活动通信
    public class MusicServiceBinder extends Binder {

        // 添加一首歌曲
        public void addPlayList(Music item) {
            addPlayListInner(item);
        }

        // 添加多首歌曲
        public void addPlayList(List<Music> items) {
            addPlayListInner(items);
        }

        // 移除一首歌曲
        public void removeMusic(int i) {
            removeMusicInner(i);
        }

        public void playOrPause(){
            if (player.isPlaying()){
                pauseInner();
            }
            else {
                playInner();
            }
        }

        // 下一首
        public void playNext() {
            playNextInner();
        }

        // 上一首
        public void playPre() {
            playPreInner();
        }

        // 获取当前播放模式
        public int getPlayMode(){
            return getPlayModeInner();
        }

        // 设置播放模式
        public void setPlayMode(int mode){
            setPlayModeInner(mode);
        }

        // 设置播放器进度
        public void seekTo(int pos) {
            seekToInner(pos);
        }

        // 获取当前就绪的音乐
        public Music getCurrentMusic() {
            return getCurrentMusicInner();
        }

        // 获取播放器播放状态
        public boolean isPlaying() {
            return isPlayingInner();
        }

        // 获取播放列表
        public List<Music> getPlayingList() {
            return getPlayingListInner();
        }

        // 注册监听器
        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.add(l);
        }

        // 注销监听器
        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.remove(l);
        }
    }

    private void addPlayListInner(Music music){
        if (!playingMusicList.contains(music)) {
            playingMusicList.add(0, music);
            PlayingMusic playingMusic = new PlayingMusic(music.songUrl, music.title, music.artist, music.imgUrl, music.isOnlineMusic);
            playingMusic.save();
        }
        currentMusic = music;
        isNeedReload = true;
        playInner();
    }

    private void addPlayListInner(List<Music> musicList){
        playingMusicList.clear();
        LitePal.deleteAll(PlayingMusic.class);
        playingMusicList.addAll(musicList);
        for (Music i: musicList){
            PlayingMusic playingMusic = new PlayingMusic(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic);
            playingMusic.save();
        }
        currentMusic = playingMusicList.get(0);
        playInner();
    }

    private void removeMusicInner(int i){
        LitePal.deleteAll(PlayingMusic.class, "title=?", playingMusicList.get(i).title);
        playingMusicList.remove(i);
    }

    private void playInner() {

        //获取音频焦点
        audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        //如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if (currentMusic == null && playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }

        playMusicItem(currentMusic, isNeedReload);


    }

    private void pauseInner(){
        player.pause();

        for (OnStateChangeListenr l : listenrList) {
            l.onPause();
        }
        // 暂停后不需要重新加载
        isNeedReload = false;
    }

    private void playPreInner(){
        //获取当前播放（或者被加载）音乐的上一首音乐
        //如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
        int currentIndex = playingMusicList.indexOf(currentMusic);
        if (currentIndex - 1 >= 0) {
            currentMusic = playingMusicList.get(currentIndex - 1);
            isNeedReload = true;
            playInner();
        }
    }

    private void playNextInner() {

        if (playMode == Utils.TYPE_RANDOM){
            //随机播放
            int i = (int) (0 + Math.random() * (playingMusicList.size() + 1));
            currentMusic = playingMusicList.get(i);
        }
        else {
            //列表循环
            int currentIndex = playingMusicList.indexOf(currentMusic);
            if (currentIndex < playingMusicList.size() - 1) {
                currentMusic = playingMusicList.get(currentIndex + 1);
            } else {
                currentMusic = playingMusicList.get(0);
            }
        }
        isNeedReload = true;
        playInner();
    }

    private void seekToInner(int pos){
        //将音乐拖动到指定的时间
        player.seekTo(pos);
    }

    private Music getCurrentMusicInner(){
        return currentMusic;
    }

    private boolean isPlayingInner(){
        return player.isPlaying();
    }

    public List<Music> getPlayingListInner(){
        return playingMusicList;
    }

    private int getPlayModeInner(){
        return playMode;
    }

    private void setPlayModeInner(int mode){
        playMode = mode;
    }

    // 将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(Music item) {
        try {
            player.reset();
            //设置播放音乐的地址
            player.setDataSource(MusicService.this, Uri.parse(item.songUrl));
            //准备播放音乐
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(Music item, boolean reload) {

        if (item == null) {
            return;
        }

        if (reload) {
            //需要重新加载音乐
            prepareToPlay(item);
        }
        player.start();
        for (OnStateChangeListenr l : listenrList) {
            l.onPlay(item);
        }
        isNeedReload = true;

        //移除现有的更新消息，重新启动更新
        handler.removeMessages(66);
        handler.sendEmptyMessage(66);
    }

    // 初始化播放列表
    private void initPlayList() {
        playingMusicList = new ArrayList<>();
        List<PlayingMusic> list = LitePal.findAll(PlayingMusic.class);
        for (PlayingMusic i : list) {
            Music m = new Music(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic);
            playingMusicList.add(m);
        }
        if (playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }
    }

    //当前歌曲播放完成的监听器
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            Utils.count ++; //累计听歌数量+1

            if (playMode == Utils.TYPE_SINGLE) {
                //单曲循环
                isNeedReload = true;
                playInner();
            }
            else {
                playNextInner();
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 66:
                    //通知监听者当前的播放进度
                    long played = player.getCurrentPosition();
                    long duration = player.getDuration();
                    for (OnStateChangeListenr l : listenrList) {
                        l.onPlayProgressChange(played, duration);
                    }
                    //间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(66, 1000);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //当组件bindService()之后，将这个Binder返回给组件使用
        return binder;
    }

    //焦点控制
    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener(){

        public void onAudioFocusChange(int focusChange) {
            switch(focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(player.isPlaying()){
                        //会长时间失去，所以告知下面的判断，获得焦点后不要自动播放
                        autoPlayAfterFocus = false;
                        pauseInner();//因为会长时间失去，所以直接暂停
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(player.isPlaying()){
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点，且符合播放条件，开始播放
                    if(!player.isPlaying()&& autoPlayAfterFocus){
                        autoPlayAfterFocus = false;
                        playInner();
                    }
                    break;
            }
        }
    };
}
