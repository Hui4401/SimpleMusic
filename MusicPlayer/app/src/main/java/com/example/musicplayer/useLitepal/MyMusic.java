package com.example.musicplayer.useLitepal;

import org.litepal.crud.LitePalSupport;

public class MyMusic extends LitePalSupport {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public long duration;      //时长
    public long played; //    进度
    public String imgUrl;
    public byte[] img;//   专辑图片

    public MyMusic(String songUrl, String title, String artist, long duration, long played, String imgUrl, byte[] img) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.duration = duration;
        this.played = played;
        this.imgUrl = imgUrl;
        this.img = img;
    }
}