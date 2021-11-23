package com.zy.environment.config;

import android.content.Context;

import com.zy.environment.utils.SpStorage;

public class GlobalSetting {

    public static String wsurl = "ws://47.93.97.68:2348";//服务器地址

    public static String deviceid;//设备id
    public static String serialPort = "dev/ttyS0";//设备id
    public static int outLen= 9;//出货长度
    public static int MachineType = 0;//机头类型 0：易诺 1：鼎旗


    public static void getSetting(Context context){
        SpStorage mSp = new SpStorage(context, "zy-environment");
        wsurl = (String) mSp.getSharedPreference("wsurl", wsurl);
        serialPort = (String) mSp.getSharedPreference("serialPort", serialPort);
        outLen = (Integer) mSp.getSharedPreference("outLen", outLen);
    }

    public static void putSetting(Context context){
        SpStorage mSp = new SpStorage(context, "zy-environment");
        mSp.put("wsurl", wsurl);
        mSp.put("serialPort", serialPort);
        mSp.put("outLen", outLen);
        mSp.apply();
    }


}
