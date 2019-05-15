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

import com.example.musicplayer.Music;
import com.example.musicplayer.service.MusicService;
import com.example.musicplayer.PlayingMusicAdapter;
import com.example.musicplayer.R;
import com.example.musicplayer.Utils;

import java.util.List;

public class PlayerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private TextView title;
    private TextView artist;
    private ImageView ivmode;
    private ImageView playPre;
    private ImageView ivplayorpause;
    private ImageView playNext;
    private ImageView playingList;
    private ImageView musicImg;
    private ImageView ivNeedle;
    private com.example.musicplayer.widget.RotateAnimator rotateAnimator;
    private TextView now_time;
    private TextView total_time;
    private SeekBar seekBar;
    private MusicService.MusicServiceIBinder service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //初始化
        init();

        //播放控制点击事件
        ivmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MusicService.mode ==Utils.TYPE_ORDER) {
                    Toast.makeText(PlayerActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                    MusicService.mode = Utils.TYPE_SINGLE;
                    ivmode.setImageResource(R.drawable.ic_singlerecycler);
                }else if(MusicService.mode == Utils.TYPE_SINGLE) {
                    Toast.makeText(PlayerActivity.this, "随机播放", Toast.LENGTH_SHORT).show();
                    MusicService.mode = Utils.TYPE_RANDOM;
                    ivmode.setImageResource(R.drawable.ic_random);
                }else if(MusicService.mode == Utils.TYPE_RANDOM) {
                    Toast.makeText(PlayerActivity.this, "列表循环", Toast.LENGTH_SHORT).show();
                    MusicService.mode = Utils.TYPE_ORDER;
                    ivmode.setImageResource(R.drawable.ic_playrecycler);
                }
            }
        });

        //播放按钮点击事件
        ivplayorpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service.isPlaying())
                    service.pause();
                else service.play();
            }
        });

        //上一曲下一曲
        playPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.playPre();
            }
        });
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.playNext();
            }
        });

        //播放列表
        playingList.setOnClickListener(new View.OnClickListener() {
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

    private void init() {

        // 使用ToolBar
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        // 标题
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);

        //seekbar
        seekBar = findViewById(R.id.musicSeekBar);
        now_time = findViewById(R.id.tvCurrentTime);
        total_time = findViewById(R.id.tvTotalTime);
        seekBar.setOnSeekBarChangeListener(this);

        //初始化播放模式
        ivmode = findViewById(R.id.ivPlayControl);

        //初始化控制播放播放的控件
        ivplayorpause = findViewById(R.id.ivPlayOrPause);
        playPre = findViewById(R.id.ivLast);
        playNext = findViewById(R.id.ivNext);
        playingList = findViewById(R.id.ivMenu);

        //初始化转针
        ivNeedle = findViewById(R.id.ivNeedle);

        //初始化动画
        musicImg = findViewById(R.id.imageView);
        rotateAnimator = new com.example.musicplayer.widget.RotateAnimator(this, musicImg, ivNeedle);
        rotateAnimator.set_Needle();

        // 启动service并绑定
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }

    //显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //对话框的显示标题
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
        //设置该对话框是可以自动取消的，例如当用户在空白处随便点击一下，对话框就会关闭消失
        builder.setCancelable(true);

        //创建并显示对话框
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
                title.setText(item.title);
                artist.setText(item.artist);
                ivplayorpause.setImageResource(R.drawable.ic_pause);
                rotateAnimator.playAnimator();
                seekBar.setMax((int) item.duration);
                seekBar.setProgress((int) item.played);
                now_time.setText(Utils.formatTime(item.played));
                total_time.setText(Utils.formatTime(item.duration));
                if (item.img != null)   musicImg.setImageBitmap(item.img);
            }

            else {
                //当前有可播放音乐但没有播放
                title.setText(item.title);
                artist.setText(item.artist);
                ivplayorpause.setImageResource(R.drawable.ic_play);
                seekBar.setMax((int) item.duration);
                seekBar.setProgress((int) item.played);
                now_time.setText(Utils.formatTime(item.played));
                total_time.setText(Utils.formatTime(item.duration));
                if (item.img != null)   musicImg.setImageBitmap(item.img);
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
        public void onPlayProgressChange(Music item) {
            //每隔一秒通知播放进度
            now_time.setText(Utils.formatTime(item.played));
            seekBar.setProgress((int) item.played);
        }

        @Override
        public void onPlay(Music item) {
            //变为播放状态时
            title.setText(item.title);
            artist.setText(item.artist);
            ivplayorpause.setImageResource(R.drawable.ic_pause);
            rotateAnimator.playAnimator();
            seekBar.setMax((int) item.duration);
            seekBar.setProgress((int) item.played);
            now_time.setText(Utils.formatTime(item.played));
            total_time.setText(Utils.formatTime(item.duration));
            if (item.img != null)   musicImg.setImageBitmap(item.img);
        }

        @Override
        public void onPause(Music item) {
            //变为暂停状态时
            ivplayorpause.setImageResource(R.drawable.ic_play);
            rotateAnimator.pauseAnimator();
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //拖动进度条时
        now_time.setText(Utils.formatTime((long) progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //停止拖动时
        service.seekTo(seekBar.getProgress());
    }

    //显示返回按钮
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
