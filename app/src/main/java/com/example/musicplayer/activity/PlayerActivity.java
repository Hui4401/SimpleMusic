package com.example.musicplayer.activity;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Music;
import com.example.musicplayer.PlayingMusicAdapter;
import com.example.musicplayer.R;
import com.example.musicplayer.Utils;
import com.example.musicplayer.service.MusicService;

import java.util.List;
import java.util.Objects;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView musicTitleView;
    private TextView musicArtistView;
    private ImageView musicImgView;
    private ImageView btnPlayMode;
    private ImageView btnPlayPre;
    private ImageView btnPlayOrPause;
    private ImageView btnPlayNext;
    private ImageView btnPlayingList;
    private TextView nowTimeView;
    private TextView totalTimeView;
    private SeekBar seekBar;
    private com.example.musicplayer.widget.RotateAnimator rotateAnimator;
    private MusicService.MusicServiceBinder serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //初始化
        initActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    // 控件监听
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_mode:
                // 改变播放模式
                int mode = serviceBinder.getPlayMode();
                switch (mode){
                    case Utils.TYPE_ORDER:
                        serviceBinder.setPlayMode(Utils.TYPE_SINGLE);
                        Toast.makeText(PlayerActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_singlerecycler);
                        break;
                    case Utils.TYPE_SINGLE:
                        serviceBinder.setPlayMode(Utils.TYPE_RANDOM);
                        Toast.makeText(PlayerActivity.this, "随机播放", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_random);
                        break;
                    case Utils.TYPE_RANDOM:
                        serviceBinder.setPlayMode(Utils.TYPE_ORDER);
                        Toast.makeText(PlayerActivity.this, "列表循环", Toast.LENGTH_SHORT).show();
                        btnPlayMode.setImageResource(R.drawable.ic_playrecycler);
                        break;
                    default:
                }
                break;
            case R.id.play_pre:
                // 上一首
                serviceBinder.playPre();
                break;
            case R.id.play_next:
                // 下一首
                serviceBinder.playNext();
                break;
            case R.id.play_or_pause:
                // 播放或暂停
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                // 播放列表
                showPlayList();
                break;
            default:
        }
    }

    private void initActivity() {
        musicTitleView = findViewById(R.id.title);
        musicArtistView = findViewById(R.id.artist);
        musicImgView = findViewById(R.id.imageView);
        btnPlayMode = findViewById(R.id.play_mode);
        btnPlayOrPause = findViewById(R.id.play_or_pause);
        btnPlayPre = findViewById(R.id.play_pre);
        btnPlayNext = findViewById(R.id.play_next);
        btnPlayingList = findViewById(R.id.playing_list);
        seekBar = findViewById(R.id.seekbar);
        nowTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);
        ImageView needleView = findViewById(R.id.ivNeedle);

        // ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        // 设置监听
        btnPlayMode.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btnPlayPre.setOnClickListener(this);
        btnPlayNext.setOnClickListener(this);
        btnPlayingList.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //拖动进度条时
                nowTimeView.setText(Utils.formatTime((long) progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                serviceBinder.seekTo(seekBar.getProgress());
            }
        });

        //初始化动画
        rotateAnimator = new com.example.musicplayer.widget.RotateAnimator(this, musicImgView, needleView);
        rotateAnimator.set_Needle();

        // 绑定service
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }

    //显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("播放列表");

        //获取播放列表
        final List<Music> playingList = serviceBinder.getPlayingList();

        if(playingList.size() > 0) {
            //播放列表有曲目，显示所有音乐
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            builder.setAdapter(playingAdapter, new DialogInterface.OnClickListener() {
                //监听列表项点击事件
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    serviceBinder.addPlayList(playingList.get(which));
                }
            });

            //列表项中删除按钮的点击事件
            playingAdapter.setOnDeleteButtonListener(new PlayingMusicAdapter.onDeleteButtonListener() {
                @Override
                public void onClick(int i) {
                    serviceBinder.removeMusic(i);
                    playingAdapter.notifyDataSetChanged();
                }
            });
        }
        else {
            //播放列表没有曲目，显示没有音乐
            builder.setMessage("没有正在播放的音乐");
        }
        builder.setCancelable(true);
        builder.create().show();
    }

    //定义与服务的连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            //注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            //获得当前音乐
            Music item = serviceBinder.getCurrentMusic();

            if(item == null) {
                //当前音乐为空, seekbar不可拖动
                seekBar.setEnabled(false);
            }
            else if (serviceBinder.isPlaying()){
                //如果正在播放音乐, 更新信息
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_pause);
                rotateAnimator.playAnimator();
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
            }
            else {
                //当前有可播放音乐但没有播放
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_play);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(musicImgView);
                }
            }

            // 获取当前播放模式
            int mode = (serviceBinder.getPlayMode());
            switch (mode){
                case Utils.TYPE_ORDER:
                    btnPlayMode.setImageResource(R.drawable.ic_playrecycler);
                    break;
                case Utils.TYPE_SINGLE:
                    btnPlayMode.setImageResource(R.drawable.ic_singlerecycler);
                    break;
                case Utils.TYPE_RANDOM:
                    btnPlayMode.setImageResource(R.drawable.ic_random);
                    break;
                default:
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接之后, 注销监听器
            serviceBinder.unregisterOnStateChangeListener(listenr);
        }
    };

    //实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(long played, long duration) {
            seekBar.setMax((int) duration);
            totalTimeView.setText(Utils.formatTime(duration));
            nowTimeView.setText(Utils.formatTime(played));
            seekBar.setProgress((int) played);
        }

        @Override
        public void onPlay(final Music item) {
            //变为播放状态时
            musicTitleView.setText(item.title);
            musicArtistView.setText(item.artist);
            btnPlayOrPause.setImageResource(R.drawable.ic_pause);
            rotateAnimator.playAnimator();
            if (item.isOnlineMusic){
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
            else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                Glide.with(getApplicationContext())
                        .load(img)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
        }

        @Override
        public void onPause() {
            //变为暂停状态时
            btnPlayOrPause.setImageResource(R.drawable.ic_play);
            rotateAnimator.pauseAnimator();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        //界面退出时的动画
        overridePendingTransition(R.anim.bottom_silent,R.anim.bottom_out);
    }
}
