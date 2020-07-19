package com.example.simplemusic.bean;

import java.util.Objects;

public class Music {

    public String artist;     //歌手
    public String title;      //歌曲名
    public String songUrl;    //歌曲地址
    public String imgUrl;     //专辑图片地址
    public boolean isOnlineMusic;

    public Music(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
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
