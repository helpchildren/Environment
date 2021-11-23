package com.sesxh.serialport;

import android.content.Context;

import com.sesxh.serialport.analysis.DataType;

public abstract class SerialPortMng {

    public static boolean DebugLog = true;

    public abstract void setDevicesPort(String devicesPort);

    public abstract void setDataType(DataType dataType);

    public abstract void setBaudRate(int baudRate);

    public abstract void setDataSize(int dataSize);

    public void initDevice(Context context){

    }

    public abstract void openSerialPort(OnSerialPortListener listener);

    public abstract void closeSerialPort();

    public abstract void sendSerialPort(String data);

    public abstract void sendSerialPort(byte[] sendData);

}
