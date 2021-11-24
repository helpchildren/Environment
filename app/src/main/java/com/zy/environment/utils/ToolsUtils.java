package com.zy.environment.utils;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.zy.environment.widget.SettingDialog;

import java.io.IOException;

public class ToolsUtils {


    /*
    * 获取设备id
    * */
    public static String getDeviceId(Context context){
        String str = Settings.System.getString(context.getContentResolver(), "android_id").toUpperCase();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DJ");
        stringBuilder.append(str.substring(str.length() - 8, str.length()));
        return stringBuilder.toString();
    }

    /*
    * 进入设置页面
    * */
    final static int COUNTS = 3;// 点击次数
    final static long DURATION = 1000;// 规定有效时间
    static long[] mHits = new long[COUNTS];
    public static void continuousClick(Activity activity) {
        //每次点击时，数组向前移动一位
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        //为数组最后一位赋值
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
            mHits = new long[COUNTS];//重新初始化数组
            //弹出密码框
            SettingDialog dialog = new SettingDialog(activity);
            dialog.show();
        }
    }

    /*
    * 获取视频时长
    * */
    public static int getVideoTime(Context context, Uri uri){
        int time = 0;
        MediaPlayer mediaPlayer = new MediaPlayer();
        long startT = System.currentTimeMillis();
        try {
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepare();
            time = mediaPlayer.getDuration();
            Log.e("lfntest","获取视频时长："+time+"  耗时："+(System.currentTimeMillis()-startT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return time;
    }

}
