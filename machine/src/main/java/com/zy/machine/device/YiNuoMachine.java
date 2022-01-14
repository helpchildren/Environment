package com.zy.machine.device;

import android.content.Context;
import android.os.SystemClock;

import com.example.selllibrary.SellManager;
import com.zy.machine.MachineManage;
import com.zy.machine.OnDataListener;

/*
* 机器管理 --易诺
* */
public class YiNuoMachine extends MachineManage {

    private static final String TAG="YiNuoMachine";

    private Thread receiveThread = null;
    private boolean flag = false;

    private final SellManager sellManager;//控制板管理器

    private int baudRate = 9600;//波特率
    private int Timeout = 20000;//超时时间 单位毫秒
    private String devicesPort = "/dev/ttyS0";//串口号
    private OnDataListener listener;

    public YiNuoMachine(Context context) {
        sellManager = new SellManager(context);
    }

    //设置波特率
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    //设置打开串口号
    public void setDevicesPort(String devicesPort) {
        this.devicesPort = devicesPort;
    }

    @Override
    public void setOutLength(int outLength) {
    }

    public void openDevice(OnDataListener listener) {
        this.listener = listener;
        if(sellManager.openSerialPort(devicesPort,baudRate)){
            flag = true;
            receiveThread();
            if (listener != null) listener.onConnect();
        }else {
            if (listener != null) listener.onError(1000,"串口"+devicesPort+" 打开失败");
        }
    }

    public void closeDevice() {
        flag = false;
        isOutGoodsFlag = false;
        if (receiveThread != null)
            receiveThread = null;
        sellManager.closeSerialPort();
        if (listener != null)
            listener.onDisConnect();
    }

    private boolean isOutGoodsFlag = false;//出货标志

    public void outGoods(){
        if (flag){
            isOutGoodsFlag = true;
        }else {
            if (listener != null) listener.onError(1000,"控制机头未连接");
        }
    }

    /**
     * 接收数据线程
     */
    private void receiveThread(){
        if(receiveThread != null){
            return;
        }
        receiveThread = new Thread(){
            @Override
            public void run() {
                while (flag){
                    if (isOutGoodsFlag){
                        sellManager.sendSell(1, 1, new SellManager.OnSellState() {
                            @Override
                            public void OnRead(boolean state) {
                            }
                        },300);//发送出货后 300毫秒后去检测状态
                        checkState();
                        isOutGoodsFlag = false;
                    }
                    SystemClock.sleep(1000);
                }
            }
        };
        //启动接收线程
        receiveThread.start();
    }


    private void checkState(){
        sellManager.checkState(1000, Timeout, System.currentTimeMillis(),3000, new SellManager.OnCheckState() {
            @Override
            public void OnFree() {
                //空闲中
            }

            @Override
            public void OnOpening() {
                //正在出货
            }

            @Override
            public void OnOvertime() {
                listener.onError(1007,"出袋超时");
                sellManager.closeCheckState();//关闭检测任务
            }

            @Override
            public void OnAbnormal() {
                listener.onError(1006,"出袋失败");
                sellManager.closeCheckState();//关闭检测任务
            }

            @Override
            public void OnSuccess() {
                listener.onSuccess();
                sellManager.closeCheckState();//关闭检测任务
            }

            @Override
            public void OnLose() {
                //发现丢包
            }

            @Override
            public void OnEnd() {
                //检测完成  此处日过查询超时 则也返回出袋成功
                listener.onSuccess();
            }
        });
    }

}
