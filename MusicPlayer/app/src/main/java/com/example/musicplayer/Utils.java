package com.example.musicplayer;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    //累计听歌数量
    public static int count;

    //播放模式
    public static final int TYPE_ORDER = 4212;  //顺序播放
    public static final int TYPE_SINGLE = 4313; //单曲循环
    public static final int TYPE_RANDOM = 4414; //随机播放

    // 创建封面图片
    public static Bitmap getBmp(ContentResolver res, Uri albumUri) {
        InputStream in;
        Bitmap bmp = null;
        try {
            in = res.openInputStream(albumUri);
            BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
            bmp = BitmapFactory.decodeStream(in, null, sBitmapOptions);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }

    //图片转换为字节流
    public static byte[] byteImg(Bitmap bitmap){
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    //格式化歌曲时间
    public static String formatTime(long time) {

        SimpleDateFormat mSDF = new SimpleDateFormat("mm:ss");
        Date date = new Date(time);

        return mSDF.format(date);
    }
}