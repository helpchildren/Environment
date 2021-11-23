package com.sesxh.serialport;

public interface OnSerialPortListener {
    /*
    * 设备连接成功回调
    * */
    void onConnect();

    /*
     * 设备断开连接
     * */
    void onDisConnect();

    /*
     * 异常回调
     * errcode 1000 打开设备失败
     * errcode 1001 发送串口数据失败
     * errcode 1002 串口接收数据异常
     * errcode 1003 串口异常
     * */
    void onError(int errcode, String err);

    /*
    * 串口数据
    * */
    void onData(Object data);
}
