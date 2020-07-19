package com.example.simplemusic.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.simplemusic.bean.Music;
import com.example.simplemusic.adapter.MusicAdapter;
import com.example.simplemusic.adapter.PlayingMusicAdapter;
import com.example.simplemusic.R;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.service.MusicService;
import com.example.simplemusic.db.LocalMusic;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class LocalMusicActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView musicCountView;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;

    private List<Music> localMusicList;
    private MusicAdapter adapter;
    private MusicService.MusicServiceBinder serviceBinder;
    private MusicUpdateTask updateTask;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localmusic);

        //初始化
        initActivity();

        // 列表项点击事件
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Music music = localMusicList.get(position);
                serviceBinder.addPlayList(music);
            }
        });

        //列表项中更多按钮的点击事件
        adapter.setOnMoreButtonListener(new MusicAdapter.onMoreButtonListener() {
            @Override
            public void onClick(final int i) {
                final Music music = localMusicList.get(i);
                final String[] items = new String[] {"收藏到我的音乐", "添加到播放列表", "删除"};
                AlertDialog.Builder builder = new AlertDialog.Builder(LocalMusicActivity.this);
                builder.setTitle(music.title+"-"+music.artist);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                MainActivity.addMymusic(music);
                                break;
                            case 1:
                                serviceBinder.addPlayList(music);
                                break;
                            case 2:
                                //从列表和数据库中删除
                                localMusicList.remove(i);
                                LitePal.deleteAll(LocalMusic.class, "title=?", music.title);
                                adapter.notifyDataSetChanged();
                                musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_all:
                serviceBinder.addPlayList(localMusicList);
                break;
            case R.id.refresh:
                localMusicList.clear();
                LitePal.deleteAll(LocalMusic.class);
                updateTask = new MusicUpdateTask();
                updateTask.execute();
                break;
            case R.id.player:
                Intent intent = new Intent(LocalMusicActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                showPlayList();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(updateTask != null && updateTask.getStatus() == AsyncTask.Status.RUNNING) {
            updateTask.cancel(true);
        }
        updateTask = null;
        localMusicList.clear();
        unbindService(mServiceConnection);
    }

    private void initActivity(){
        //初始化控件
        ImageView btn_playAll = this.findViewById(R.id.play_all);
        musicCountView = this.findViewById(R.id.play_all_title);
        ImageView btn_refresh = this.findViewById(R.id.refresh);
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);

        btn_playAll.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);

        localMusicList = new ArrayList<>();

        //绑定播放服务
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("本地音乐");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        //从数据库获取保存的本地音乐列表
        List<LocalMusic> list = LitePal.findAll(LocalMusic.class);
        for (LocalMusic s:list){
            Music m = new Music(s.songUrl, s.title, s.artist, s.imgUrl, s.isOnlineMusic);
            localMusicList.add(m);
        }

        // 本地音乐列表绑定适配器
        adapter = new MusicAdapter(this, R.layout.music_item, localMusicList);
        musicListView.setAdapter(adapter);

        musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
    }

    // 显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //设计对话框的显示标题
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

        //设置该对话框是可以自动取消的，例如当用户在空白处随便点击一下，对话框就会关闭消失
        builder.setCancelable(true);

        //创建并显示对话框
        builder.create().show();
    }

    // 定义与服务的连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        // 绑定成功时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // 绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            // 注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            Music item = serviceBinder.getCurrentMusic();

            if (serviceBinder.isPlaying()){
                // 如果正在播放音乐, 更新控制栏信息
                btnPlayOrPause.setImageResource(R.drawable.zanting);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
            else if (item != null){
                // 当前有可播放音乐但没有播放
                btnPlayOrPause.setImageResource(R.drawable.bofang);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic){
                    Glide.with(getApplicationContext())
                            .load(item.imgUrl)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 断开连接时注销监听器
            serviceBinder.unregisterOnStateChangeListener(listenr);
        }
    };

    // 实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(long played, long duration) {}

        @Override
        public void onPlay(Music item) {
            // 播放状态变为播放时
            btnPlayOrPause.setImageResource(R.drawable.zanting);
            playingTitleView.setText(item.title);
            playingArtistView.setText(item.artist);
            btnPlayOrPause.setEnabled(true);
            if (item.isOnlineMusic){
                Glide.with(getApplicationContext())
                        .load(item.imgUrl)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
            else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                Glide.with(getApplicationContext())
                        .load(img)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
        }

        @Override
        public void onPause() {
            // 播放状态变为暂停时
            btnPlayOrPause.setImageResource(R.drawable.bofang);
            btnPlayOrPause.setEnabled(true);
        }
    };

    // 异步获取本地所有音乐
    @SuppressLint("StaticFieldLeak")
    private class MusicUpdateTask extends AsyncTask<Object, Music, Void> {

        // 开始获取, 显示一个进度条
        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(LocalMusicActivity.this);
            progressDialog.setMessage("获取本地音乐中...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        // 子线程中获取音乐
        @Override
        protected Void doInBackground(Object... params) {

            String[] searchKey = new String[]{
                    MediaStore.Audio.Media._ID,     //对应文件在数据库中的检索ID
                    MediaStore.Audio.Media.TITLE,   //标题
                    MediaStore.Audio.Media.ARTIST,  //歌手
                    MediaStore.Audio.Albums.ALBUM_ID,   //专辑ID
                    MediaStore.Audio.Media.DURATION,     //播放时长
                    MediaStore.Audio.Media.IS_MUSIC     //是否为音乐文件
            };

            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, searchKey, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    //通过URI和ID，组合出改音乐特有的Uri地址
                    Uri musicUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    int isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                    if (isMusic != 0 && duration/(500*60) >= 2) {
                        //再通过专辑Id组合出音乐封面的Uri地址
                        Uri musicPic = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                        Music data = new Music(musicUri.toString(), title, artist, musicPic.toString(), false);
                        //切换到主线程进行更新
                        publishProgress(data);
                    }
                }
                cursor.close();
            }
            return null;
        }

        //主线程
        @Override
        protected void onProgressUpdate(Music... values) {
            Music data = values[0];
            //判断列表中是否已存在当前音乐
            if (!localMusicList.contains(data)){
                //添加到列表和数据库
                localMusicList.add(data);
                LocalMusic music = new LocalMusic(data.songUrl, data.title, data.artist, data.imgUrl, data.isOnlineMusic);
                music.save();
            }
            //刷新UI界面
            MusicAdapter adapter = (MusicAdapter) musicListView.getAdapter();
            adapter.notifyDataSetChanged();
            musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
        }

        //任务结束, 关闭进度条
        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
        }
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
