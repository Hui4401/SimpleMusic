package com.example.musicplayer.useLitepal;

import org.litepal.crud.LitePalSupport;

public class PlayingMusic  extends LitePalSupport {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public long duration;      //时长
    public String imgUrl;
    public boolean isOnlineMusic;

    public PlayingMusic(String songUrl, String title, String artist, long duration, String imgUrl, boolean isOnlineMusic) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.duration = duration;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
    }
}
