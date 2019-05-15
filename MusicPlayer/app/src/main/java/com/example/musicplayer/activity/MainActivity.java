package com.example.musicplayer.activity;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.musicplayer.Music;
import com.example.musicplayer.MusicAdapter;
import com.example.musicplayer.PlayingMusicAdapter;
import com.example.musicplayer.R;
import com.example.musicplayer.Utils;
import com.example.musicplayer.service.MusicService;
import com.example.musicplayer.useLitepal.MyMusic;
import com.jaeger.library.StatusBarUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Toolbar toolbar;
    private ListView musicListView;
    private ImageView playingImgView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView btn_playOrPause;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView musicCountView;

    private static List<Music> musicList;
    private MusicAdapter musicAdapter;
    private MusicService.MusicServiceIBinder service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化
        initActivity();

        // 点击列表项播放音乐
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Music music = musicList.get(position);
                service.addPlayList(music);
            }
        });

        // 列表项中更多按钮的点击事件
        musicAdapter.setOnMoreButtonListener(new MusicAdapter.onMoreButtonListener() {
            @Override
            public void onClick(final int i) {
                final Music music = musicList.get(i);

                //弹出操作对话框
                final String[] items = new String[] {"添加到播放列表", "删除"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(music.title+"-"+music.artist);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                service.addPlayList(music);
                                break;
                            case 1:
                                //从列表和数据库中删除
                                musicList.remove(i);
                                LitePal.deleteAll(MyMusic.class, "title=?", music.title);
                                musicAdapter.notifyDataSetChanged();
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        musicCountView.setText("累计听歌"+Integer.toString(Utils.count)+"首");
        musicAdapter.notifyDataSetChanged(); //刷新列表
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清空列表
        musicList.clear();
        //解除与Servie的绑定
        unbindService(mServiceConnection);
        //存储累计听歌数量
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        editor.putInt("count", Utils.count);
        editor.apply();
    }

    // 显示菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 菜单点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.local_music:
                Intent intent1 = new Intent(MainActivity.this, LocalMusicActivity.class);
                startActivity(intent1);
                break;
            case R.id.online_music:
                Intent intent2 = new Intent(MainActivity.this, OnlineMusicActivity.class);
                startActivity(intent2);
                break;
            default:
        }
        return true;
    }

    // 监听组件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.player:
                // 进入播放器
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                // 播放或暂停
                if (service.isPlaying()){
                    service.pause();

                }else {
                    service.play();
                }
                break;
            case R.id.playing_list:
                // 显示正在播放列表
                showPlayingList();
                break;
            default:
        }
    }

    // 初始化活动
    private void initActivity(){
        //绑定控件
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btn_playOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);
        drawerLayout = this.findViewById(R.id.drawer);
        navigationView = this.findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        musicCountView = headerView.findViewById(R.id.nav_num);

        // 设置监听
        playerToolView.setOnClickListener(this);
        btn_playOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);

        // 申请读写权限
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{WRITE_EXTERNAL_STORAGE}, 1);
        
        // 使用ToolBar
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // 状态栏沉浸
        StatusBarUtil.setTransparentForDrawerLayout(this, drawerLayout);

        // 设置侧边栏
        setNavigationView();

        // 启动service并绑定
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        //从数据库获取我的音乐
        musicList = new ArrayList<>();
        List<MyMusic> list = LitePal.findAll(MyMusic.class);
        for (MyMusic s:list){
            Music m = new Music(s.songUrl, s.title, s.artist, s.duration, 0, s.imgUrl, s.isOnlineMusic);
            musicList.add(m);
        }

        // 音乐列表绑定适配器
        musicAdapter = new MusicAdapter(this, R.layout.music_item, musicList);
        musicListView.setAdapter(musicAdapter);

        //读取累计听歌数量
        SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
        Utils.count = preferences.getInt("count", 0);
    }

    // 设置侧边栏
    private void setNavigationView(){

        // 使用toggle控制侧边栏弹出:
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,toolbar,R.string.app_name,R.string.app_name);
        toggle.syncState();
        drawerLayout.addDrawerListener(toggle);

        //侧边栏菜单项点击事件
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()){
                    case R.id.local:
                        // 进入本地音乐
                        Intent intent1 = new Intent(MainActivity.this, LocalMusicActivity.class);
                        startActivity(intent1);
                        break;
                    case R.id.online:
                        // 进入在线音乐
                        Intent intent2 = new Intent(MainActivity.this, OnlineMusicActivity.class);
                        startActivity(intent2);
                        break;
                    case R.id.exit:
                        // 退出
                        finish();
                        break;
                }
                return true;
            }
        });
    }

    // 显示当前正在播放的音乐
    private void showPlayingList(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //设计对话框的显示标题
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

    // 与后台服务连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //绑定成功后，取得MusicSercice提供的接口
            MainActivity.this.service = (MusicService.MusicServiceIBinder) service;

            //注册监听器
            ((MusicService.MusicServiceIBinder) service).registerOnStateChangeListener(listenr);

            Music item = ((MusicService.MusicServiceIBinder) service).getCurrentMusic();
            if(item != null){
                //当前有可播放音乐
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接之后, 注销监听器
            service.unregisterOnStateChangeListener(listenr);
        }
    };

    // 实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(Music item) {
        }

        @Override
        public void onPlay(Music item) {
            //播放状态变为播放时
            btn_playOrPause.setImageResource(R.drawable.zanting);
            playingTitleView.setText(item.title);
            playingArtistView.setText(item.artist);
        }

        @Override
        public void onPause(Music item) {

            //播放状态变为暂停时
            btn_playOrPause.setImageResource(R.drawable.bofang);
        }

        @Override
        public void onMusicPicFinish(final Bitmap bitmap) {
            Log.d("aaa", "onMusicPicFinish: ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playingImgView.setImageBitmap(bitmap);
                }
            });
        }
    };

    // 对外接口, 插入一首歌曲
    public static  void addMymusic(Music item){
        if (musicList.contains(item))
            return;
        //添加到列表和数据库
        musicList.add(0, item);
        MyMusic myMusic = new MyMusic(item.songUrl, item.title, item.artist, item.duration, item.played, item.imgUrl, item.isOnlineMusic);
        myMusic.save();
    }

//    // 对外接口, 插入多首歌曲
//    public static void addMymusic(List<Music> lists){
//
//        for (Music item:lists){
//            addMymusic(item);
//        }
//    }
}
