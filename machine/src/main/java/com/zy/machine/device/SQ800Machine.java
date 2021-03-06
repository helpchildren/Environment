package com.zy.machine.device;

import android.os.SystemClock;
import android.util.Log;

import com.dingee.tcmsdk.TicketModule;
import com.zy.machine.MachineManage;
import com.zy.machine.OnDataListener;

/*
* SQ800机器管理 --鼎旗
* */
public class SQ800Machine extends MachineManage {

    private static final String TAG="SQ800Machine";

    private Thread receiveThread = null;
    private boolean flag = false;

    private int fd;
    private final TicketModule obj_tcm;

    private int baudRate = 9600;//波特率
    private int outLength = 9;//出货长度
    private int iCurDevAddr	= 0;	// 当前选中的设备地址
    private String devicesPort = "/dev/ttyS0";//串口号
    private OnDataListener listener;

    public SQ800Machine() {
        obj_tcm = new TicketModule();
    }

    //设置波特率
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }
    //设置打开串口号
    public void setDevicesPort(String devicesPort) {
        this.devicesPort = devicesPort;
    }
    //设置出货长度
    public void setOutLength(int outLength) {
        this.outLength = outLength;
    }

    public void openDevice(OnDataListener listener) {
        this.listener = listener;
        fd = obj_tcm.dgOpenPort(devicesPort, baudRate);
        Log.d(TAG,"dgOpenPort :" + fd);
        if(fd > 0){
            flag = true;
            receiveThread();
            if (listener != null) listener.onConnect();
        }else{
            if (listener != null) listener.onError(1000,"串口"+devicesPort+" 打开失败");
        }
    }

    public void closeDevice() {
        flag = false;
        isOutGoodsFlag = false;
        if (receiveThread != null)
            receiveThread = null;
        obj_tcm.dgReleasePort(fd);
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
                        int ret = obj_tcm.dgCutTicket(fd, iCurDevAddr,TicketModule.TICKET_OUT_CAL_INCH ,outLength,9);
                        switch(ret) {
                            case TicketModule.RSLT_OUT_TICKET_SUCC:
                                listener.onSuccess();
                                break;
                            case TicketModule.RSLT_OUT_TICKET_NOPAPER:
                                listener.onError(1001,"出袋失败:无袋");
                                break;
                            case TicketModule.RSLT_OUT_TICKET_NOT_TAKEN:
                                listener.onError(1002,"出袋失败:出袋口有袋未取走");
                                break;
                            case TicketModule.RSLT_OUT_TICKET_KNIFE_ERR:
                                if(reset == 0){
                                    resetCMD(1);
                                    reset++;
                                    continue;
                                }
                                listener.onError(1003,"出袋失败:切刀故障");
                                break;
                            case TicketModule.RSLT_OUT_TICKET_PAPERJAM:
                                if(reset == 0){
                                    resetCMD(0);
                                    reset++;
                                    continue;
                                }
                                listener.onError(1004,"出袋失败:发生卡袋");
                                break;
                            default:
                                listener.onError(1005,"出袋失败:通讯错误");
                                break;
                        }
                        reset = 0;//成功返回 重置恢复标志
                        isOutGoodsFlag = false;
                    }
                    SystemClock.sleep(1000);
                }
            }
        };
        //启动接收线程
        receiveThread.start();
    }


    /*
    * 恢复指令
    * 0：恢复卡袋
    * 1：恢复切刀
    * */
    private int reset = 0;//恢复标志
    private void resetCMD(int type){
        if (type == 0){
            obj_tcm.dgResetPaperJam(fd, iCurDevAddr);
        }else {
            obj_tcm.dgResetKnife(fd, iCurDevAddr);
        }
    }

}
