package com.zy.environment.config;

import android.content.Context;
import android.os.Environment;

import com.zy.environment.utils.SpStorage;

public class GlobalSetting {

    public static final String externpath = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String AdvPath = externpath + "/zy/adv";
    public static final String AdFile = "adv.txt";

    public static String wsurl = "ws://47.93.97.68:2348";//服务器地址
    public static String uploadUrl = "http://bag.cnwinall.cn/wechat/device/upload";//日志上传地址

    public static String deviceid;//设备id
    public static String serialPort = "dev/ttyS0";//设备id
    public static int outLen= 9;//出货长度
    public static int machineType = MachineType.DQ.getCode();//机头类型 0：益诺 1：鼎戟

    public static boolean isDugLog = true;//是否开启文件日志


    public static void getSetting(Context context){
        SpStorage mSp = new SpStorage(context, "zy-environment");
        wsurl = (String) mSp.getSharedPreference("wsurl", wsurl);
        serialPort = (String) mSp.getSharedPreference("serialPort", serialPort);
        outLen = (Integer) mSp.getSharedPreference("outLen", outLen);
        machineType = (Integer) mSp.getSharedPreference("machineType", machineType);
//        isDugLog = (Boolean) mSp.getSharedPreference("isDugLog", isDugLog);
        putSetting(context);
    }

    public static void putSetting(Context context){
        SpStorage mSp = new SpStorage(context, "zy-environment");
        mSp.put("wsurl", wsurl);
        mSp.put("serialPort", serialPort);
        mSp.put("outLen", outLen);
        mSp.put("machineType", machineType);
//        mSp.put("isDugLog", isDugLog);
        mSp.apply();
    }

    /*
     * 机头型号
     * */
    public enum MachineType{
        YN(0, "益诺"),
        DQ(1,"鼎戟");

        int code;
        String type;
        MachineType(int code, String type){
            this.code = code;
            this.type = type;
        }
        public String getType() {
            return type;
        }

        public int getCode() {
            return code;
        }
    }


}
