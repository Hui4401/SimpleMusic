package com.example.musicplayer;

import android.graphics.Bitmap;

import java.util.Objects;

public class Music {

    public String artist;   //歌手
    public String title;     //歌曲名
    public String songUrl;     //歌曲地址
    public long duration;      //总时长
    public long played; //    进度
    public String imgUrl;
    public Bitmap img;    //专辑图片

    public Music(String songUrl, String title, String artist, long duration, long played, String imgUrl, Bitmap img) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.duration = duration;
        this.played = played;
        this.imgUrl = imgUrl;
        this.img = img;
    }

    //重写equals方法, 使得可以用contains方法来判断列表中是否存在Music类的实例
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        return Objects.equals(title, music.title);
    }
}
