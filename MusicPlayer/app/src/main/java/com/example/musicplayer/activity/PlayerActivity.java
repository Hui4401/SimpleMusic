package com.example.musicplayer.activity;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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

public class PlayerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

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
    private MusicService.MusicServiceIBinder service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //初始化
        initActivity();

        //播放控制点击事件
        btnPlayMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(service.getPlayMode() ==Utils.TYPE_ORDER) {
                    Toast.makeText(PlayerActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                    service.setPlayMode(Utils.TYPE_SINGLE);
                    btnPlayMode.setImageResource(R.drawable.ic_singlerecycler);
                }else if(service.getPlayMode() == Utils.TYPE_SINGLE) {
                    Toast.makeText(PlayerActivity.this, "随机播放", Toast.LENGTH_SHORT).show();
                    service.setPlayMode(Utils.TYPE_RANDOM);
                    btnPlayMode.setImageResource(R.drawable.ic_random);
                }else if(service.getPlayMode() == Utils.TYPE_RANDOM) {
                    Toast.makeText(PlayerActivity.this, "列表循环", Toast.LENGTH_SHORT).show();
                    service.setPlayMode(Utils.TYPE_ORDER);
                    btnPlayMode.setImageResource(R.drawable.ic_playrecycler);
                }
            }
        });

        //播放按钮点击事件
        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service.isPlaying())
                    service.pause();
                else service.play();
            }
        });

        //上一曲下一曲
        btnPlayPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.playPre();
            }
        });
        btnPlayNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.playNext();
            }
        });

        //播放列表
        btnPlayingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayList();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void finish() {
        super.finish();
        //界面退出时的动画
        overridePendingTransition(R.anim.bottom_silent,R.anim.bottom_out);
    }

    private void initActivity() {

        // ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        musicTitleView = findViewById(R.id.title);
        musicArtistView = findViewById(R.id.artist);
        musicImgView = findViewById(R.id.imageView);
        btnPlayMode = findViewById(R.id.ivPlayControl);
        btnPlayOrPause = findViewById(R.id.ivPlayOrPause);
        btnPlayPre = findViewById(R.id.ivLast);
        btnPlayNext = findViewById(R.id.ivNext);
        btnPlayingList = findViewById(R.id.ivMenu);
        seekBar = findViewById(R.id.musicSeekBar);
        nowTimeView = findViewById(R.id.tvCurrentTime);
        totalTimeView = findViewById(R.id.tvTotalTime);
        ImageView needleView = findViewById(R.id.ivNeedle);

        // 设置监听
        seekBar.setOnSeekBarChangeListener(this);

        //初始化动画
        rotateAnimator = new com.example.musicplayer.widget.RotateAnimator(this, musicImgView, needleView);
        rotateAnimator.set_Needle();

        // 启动service并绑定
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }

    //显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("播放列表");

        //获取播放列表
        final List<Music> playingList = service.getPlayingList();

        if(playingList.size() > 0) {
            //播放列表有曲目，显示所有音乐
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            builder.setAdapter(playingAdapter, new DialogInterface.OnClickListener() {
                //监听列表项点击事件
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    service.addPlayList(playingList.get(which));
                }
            });

            //列表项中删除按钮的点击事件
            playingAdapter.setOnDeleteButtonListener(new PlayingMusicAdapter.onDeleteButtonListener() {
                @Override
                public void onClick(int i) {
                    service.removeMusic(i);
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
            PlayerActivity.this.service = (MusicService.MusicServiceIBinder) service;

            //注册监听器
            ((MusicService.MusicServiceIBinder) service).registerOnStateChangeListener(listenr);

            //获得当前音乐
            Music item = ((MusicService.MusicServiceIBinder) service).getCurrentMusic();

            if(item == null) {
                //当前音乐为空, seekbar不可拖动
                seekBar.setEnabled(false);
            }
            else if (((MusicService.MusicServiceIBinder) service).isPlaying()){
                //如果正在播放音乐, 更新信息
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_pause);
                rotateAnimator.playAnimator();
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
            else {
                //当前有可播放音乐但没有播放
                musicTitleView.setText(item.title);
                musicArtistView.setText(item.artist);
                btnPlayOrPause.setImageResource(R.drawable.ic_play);
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(musicImgView);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接之后, 注销监听器
            service.unregisterOnStateChangeListener(listenr);
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
            Glide.with(getApplicationContext())
                    .load(item.imgUrl)
                    .placeholder(R.drawable.defult_music_img)
                    .error(R.drawable.defult_music_img)
                    .into(musicImgView);
        }

        @Override
        public void onPause() {
            //变为暂停状态时
            btnPlayOrPause.setImageResource(R.drawable.ic_play);
            rotateAnimator.pauseAnimator();
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //拖动进度条时
        nowTimeView.setText(Utils.formatTime((long) progress));
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //停止拖动时
        service.seekTo(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
