package com.sesxh.serialport.device;

import android.util.Log;

import com.sesxh.serialport.OnSerialPortListener;
import com.sesxh.serialport.SerialPortMng;
import com.sesxh.serialport.analysis.DataType;
import com.sesxh.serialport.analysis.SerialDataAnalysis;
import com.sesxh.serialport.utils.CodeTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import android_serialport_api.SerialPort;

public class DevSerialPortMng extends SerialPortMng {

    private SerialPort serialPort = null;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;
    private Thread receiveThread = null;
    private boolean flag = false;
    private DataType dataType = DataType.HexData;//返回数据格式类型
    private int baudRate = 115200;//波特率
    private int dataSize = 1024;//一包数据大小 默认 1024
    private String devicesPort = "/dev/ttyS2";//串口号
    private OnSerialPortListener listener;

    //设置返回数据格式类型
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
    //设置波特率
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }
    //设置一包数据大小
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }
    //设置打开串口号
    public void setDevicesPort(String devicesPort) {
        this.devicesPort = devicesPort;
    }

    public static boolean isDevPortFind(String devicesPort) {
        File file = new File(devicesPort);
        return file.exists();
    }

    public void openSerialPort(OnSerialPortListener listener){
        openSerialPort(devicesPort, listener);
    }

    /**
     * 打开串口的方法
     * devicesPort 串口号
     * dataType 解析类型 对应 SerialPortFactroy中类型
     */
    public void openSerialPort(String devicesPort, OnSerialPortListener listener)  {
        this.listener = listener;
        try{
            serialPort = new SerialPort(new File(devicesPort),baudRate,0);
            //获取打开的串口中的输入输出流，以便于串口数据的收发
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            flag = true;
            receiveSerialPort();
            if (listener != null) listener.onConnect();
        }catch (IOException e){
            if (listener != null) listener.onError(1000,"串口"+devicesPort+" 打开失败");
        }
    }


    /**
     *关闭串口的方法
     * 关闭串口中的输入输出流
     * 然后将flag的值设为flag，终止接收数据线程
     */
    public void closeSerialPort(){
        Log.i("UsbSerialPortMng","关闭串口");
        try {
            if(inputStream != null) inputStream.close();
            if(outputStream != null) outputStream.close();
            flag = false;
            if (serialPort != null) serialPort.close();
            if (receiveThread != null) receiveThread = null;
            if (listener != null) listener.onDisConnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送串口数据的方法
     * @param data 要发送的数据
     */
    public void sendSerialPort(String data){
        if (outputStream == null){
            if (listener != null) listener.onError(1003,"串口尚未初始化");
        }
        try {
            byte[] bytes = null;
            if (dataType == DataType.HexData){
                bytes = CodeTools.hexString2Bytes(data);
            }else  if (dataType == DataType.StringData){
                bytes = data.getBytes(StandardCharsets.UTF_8);
            }else  if (dataType == DataType.SesData){
                String cmd = CodeTools.dataDombination(data);
                bytes = CodeTools.hexString2Bytes(cmd);
            }else {
                bytes = data.getBytes(StandardCharsets.UTF_8);
            }
            sendSerialPort(bytes);
        } catch (Exception e) {
            if (listener != null){
                listener.onError(1001,"sendData failed："+e.getMessage());
            }
        }
    }
    /**
     * 发送串口数据的方法
     */
    public void sendSerialPort(byte[] sendData){
        if (outputStream == null){
            if (listener != null) listener.onError(1003,"串口尚未初始化");
        }
        try {
            outputStream.write(sendData);
            outputStream.flush();
        } catch (IOException e) {
            if (listener != null) listener.onError(1001,"sendData failed："+e.getMessage());
        }
    }

    public int readSerialPort(byte[] readData) {
        if (outputStream == null){
            if (listener != null) listener.onError(1003,"串口尚未初始化");
        }
        try {
            return inputStream.read(readData);
        } catch (IOException e) {
            return 0;
        }
    }


    /**
     * 接收串口数据的方法
     */
    private void receiveSerialPort(){
        if(receiveThread != null){
            return;
        }
        /*创建子线程接收串口数据
         */
        receiveThread = new Thread(){
            @Override
            public void run() {
                while (flag) {
                    try {
                        if (inputStream == null) {
                            if (listener != null) listener.onError(1003,"串口尚未初始化");
                            return;
                        }
                        int available = inputStream.available();
                        if (available == 0){
                            Thread.sleep(500);
                            continue;
                        }
                        byte[] readData = new byte[dataSize];
                        int size = inputStream.read(readData);
                        if (size>0 && flag) {
                            SerialDataAnalysis factroy = new SerialDataAnalysis();
                            Object serialData = factroy.dataHandling(dataType, readData, size);
                            if (serialData != null &&listener != null) {
                                listener.onData(serialData);
                            }
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        if (listener != null) listener.onError(1002,"串口接收数据异常");
                        break;
                    }
                }
            }
        };
        //启动接收线程
        receiveThread.start();
    }

}
