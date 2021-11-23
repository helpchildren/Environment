package com.sesxh.serialport.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;

import com.sesxh.serialport.OnSerialPortListener;
import com.sesxh.serialport.SerialPortMng;
import com.sesxh.serialport.analysis.DataType;
import com.sesxh.serialport.analysis.SerialDataAnalysis;
import com.sesxh.serialport.usbserial.UsbSerialDriver;
import com.sesxh.serialport.usbserial.UsbSerialPort;
import com.sesxh.serialport.usbserial.UsbSerialProber;
import com.sesxh.serialport.utils.CodeTools;
import com.sesxh.serialport.utils.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UsbSerialPortMng extends SerialPortMng {

    private enum UsbPermission {Unknown, Requested, Granted, Denied}//usb权限状态
    private UsbPermission usbPermission = UsbPermission.Unknown;

    private DataType dataType = DataType.HexData;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;

    private static final String INTENT_ACTION_GRANT_USB = "com.ssyl.usbserialprot.GRANT_USB";
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;

    private Context context;
    private UsbDevice usbDevice;
    private OnSerialPortListener listener;
    private int baudRate = 115200;//波特率
    private boolean isconnected = false;//标志位
    private static final int WRITE_WAIT_MILLIS = 2000;//读写大小
    private int dataSize = 0;//一包数据大小 不设置此参数时默认最大

    public UsbSerialPortMng() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    //设置波特率
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    //设置指定usb设备
    public void setUsbDevice(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    //设置返回数据格式
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    //设置一包数据大小 不设置此参数时默认最大
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    @Override
    public void setDevicesPort(String devicesPort) {

    }

    /**
     * 初始化设备
     * @param context 上下文
     *
     * */
    public void initDevice(Context context){
        this.context = context;
        context.registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
    }

    /**
     * 打开设备连接
     * @param listener 串口数据监听返回
     * */
    public void openSerialPort(OnSerialPortListener listener){
        this.listener = listener;
        if (context == null){
            if (listener != null){
                listener.onError(1003,"context is null");
            }
            return;
        }
        //首先判断设备是否已经连接
        if (!isconnected)
            mainLooper.post(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            });
    }

    /**
     * 关闭设备连接
     * */
    public void closeSerialPort(){
        mainLooper.post(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
    }

    /**
     * 发送串口数据的方法
     */
    public void sendSerialPort(String data){
        if (!isconnected) {
            if (listener != null){
                listener.onError(1003,"device not connected");
            }
            return;
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
            usbSerialPort.write(bytes, WRITE_WAIT_MILLIS);
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
        if (!isconnected) {
            if (listener != null){
                listener.onError(1003,"device not connected");
            }
            return;
        }
        try {
            usbSerialPort.write(sendData, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            if (listener != null){
                listener.onError(1001,"sendData failed："+e.getMessage());
            }
        }
    }

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    if (listener != null){
                        listener.onError(1003,"onRunError："+e.getMessage());
                    }
                }

                @Override
                public void onNewData(final byte[] data, int size) {
                    SerialDataAnalysis factroy = new SerialDataAnalysis();
                    Object serialData = factroy.dataHandling(dataType, data, size);
                    if (serialData != null && listener != null) {
                        listener.onData(serialData);
                    }
                }
            };

    /**
     * 获取串口设备驱动
     * */
    private UsbSerialDriver getUsbSerialDriver(UsbManager usbManager){
        UsbSerialDriver driver = null;
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        if (usbDevice != null){
            driver = usbDefaultProber.probeDevice(usbDevice);
        }else {
            for(UsbDevice device : usbManager.getDeviceList().values()) {
                driver = usbDefaultProber.probeDevice(device);
                if(driver != null) {//找到第一个有驱动的设备后返回
                    break;
                }
            }
        }
        return driver;
    }

    /**
     * 连接设备
    * */
    private void connect(){
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        //获取设备驱动
        UsbSerialDriver driver = getUsbSerialDriver(usbManager);
        if (driver == null) {
            if (listener != null){
                listener.onError(1003,"connection failed: no driver for device");
            }
            return;
        }

        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        //判断是否未获取权限
        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            //开始请求usb权限
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            if (listener != null){
                if (!usbManager.hasPermission(driver.getDevice()))
                    listener.onError(1003,"connection failed: permission denied");
                else
                    listener.onError(1003,"connection failed: open failed");
            }
            return;
        }
        usbSerialPort = driver.getPorts().get(0);
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            usbIoManager = new SerialInputOutputManager(usbSerialPort, mListener);
            if (dataSize > 0){
                usbIoManager.setReadBufferSize(dataSize);
            }
            usbIoManager.start();
            isconnected = true;
            if (listener != null){
                listener.onConnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mListener != null){
                mListener.onRunError(new Exception("connection failed: " + e.getMessage()));
            }
            disconnect();
        }
    }


    private void disconnect() {
        isconnected = false;
        if (usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        usbSerialPort = null;
        if (listener != null){
            listener.onDisConnect();
        }
    }

}
