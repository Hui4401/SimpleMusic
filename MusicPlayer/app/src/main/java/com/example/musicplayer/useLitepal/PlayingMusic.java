package com.example.musicplayer.useLitepal;

import org.litepal.crud.LitePalSupport;

public class PlayingMusic  extends LitePalSupport {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public long duration;      //时长
    public long played; //    进度
    public byte[] img;//   专辑图片

    public PlayingMusic(String songUrl, String title, String artist, long duration, long played, byte[] img) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.duration = duration;
        this.played = played;
        this.img = img;
    }
}
