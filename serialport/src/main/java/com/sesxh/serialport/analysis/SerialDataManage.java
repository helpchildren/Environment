package com.sesxh.serialport.analysis;


import android.util.Log;

import com.sesxh.serialport.utils.CodeTools;

import java.nio.charset.StandardCharsets;

import static com.sesxh.serialport.device.DevSerialPortMng.DebugLog;

public class SerialDataManage {

    private static SerialDataManage mInstance;
    public static SerialDataManage getInstance() {
        if (mInstance == null) {
            mInstance = new SerialDataManage();
        }
        return mInstance;
    }


    public String stringData(byte[] readData, int size) {
        return new String(readData,0,size, StandardCharsets.UTF_8);
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }

    public String hexStringData(byte[] b, int size) {
        if (b == null) {
            return "";
        }
        byte[] bRec = new byte[size];
        System.arraycopy(b, 0, bRec, 0, size);
        char[] _16 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bRec.length; i++) {
            sb.append(_16[bRec[i] >> 4 & 0xf])
                    .append(_16[bRec[i] & 0xf]);
        }
        return sb.toString();
    }


    final String HEAD = "534473457300";//数据头：6个固定字节。0x53 0x44 0x73 0x45 0x73   (SDsEs)
    final String CMDA = "FE23";//操作符
    private final String STATE = "90";//标志位 90 成功 6D 失败
    boolean isLongData = false;
    int datalen = 0;
    StringBuffer cmdBuffer = new StringBuffer();


    public String sesData(byte[] readData, int size) {
        String hexString = hexStringData(readData, size);
        if (isLongData){//数据过长时需要拼接
            int nowlen = cmdBuffer.toString().length()+hexString.length();
            cmdBuffer.append(hexString);
            if (DebugLog)Log.e("debuglog", "拼接中:"+nowlen);
            if (nowlen == datalen){//拼接完成
                if (DebugLog)Log.e("debuglog", "拼接完成:"+nowlen);
                isLongData = false;
                String _hexString = cmdBuffer.toString();
                _hexString = _hexString.substring(26,_hexString.length()-4);
                _hexString = CodeTools.hexStr2Str(_hexString);
                if (DebugLog)Log.i("debuglog", "转化为string:" + _hexString);
                return hexString;
            }else if (nowlen>datalen){
                if (DebugLog)Log.i("debuglog", "串口数据长度错误:" +nowlen);
                isLongData = false;
            }
        }else if (hexString.length()>=26 && HEAD.equals(hexString.substring(0,12)) && CMDA.equals(hexString.substring(20,24)) && STATE.equals(hexString.substring(24,26)) ){
            if (DebugLog)Log.i("debuglog", "本次接收到串口数据:" + hexString);
            datalen = Integer.parseInt(hexString.substring(12,20),16)*2+20;//串口数据总长度
            if (datalen == hexString.length()){//如果数据是一包全过来过来则直接解析
                hexString = hexString.substring(26,hexString.length()-4);
                hexString = CodeTools.hexStr2Str(hexString);
                if (DebugLog)Log.i("debuglog", "转化为string:" + hexString);
                return hexString;
            }else if (datalen > hexString.length()){//长包数据则开始拼接
                if (DebugLog)Log.e("debuglog", "拼接开始:"+hexString);
                isLongData = true;
                cmdBuffer = new StringBuffer();
                cmdBuffer.append(hexString);
            }else {
                if (DebugLog)Log.i("debuglog", "接收数据长度异常:" + hexString);
                return null;
            }
        }else {
            if (DebugLog)Log.i("debuglog", "接收异常数据:" + hexString);
            return null;
        }
        return null;
    }


}
