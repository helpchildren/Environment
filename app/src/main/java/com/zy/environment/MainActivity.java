package com.zy.environment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sesxh.okwebsocket.OkWebSocket;
import com.sesxh.okwebsocket.WebSocketInfo;
import com.sesxh.okwebsocket.annotation.WebSocketStatus;
import com.sesxh.rxpermissions.RxPermissions;
import com.sesxh.yzsspeech.YSpeech;
import com.zy.environment.base.BaseActivity;
import com.zy.environment.bean.MsgBean;
import com.zy.environment.bean.MsgType;
import com.zy.environment.config.GlobalSetting;
import com.zy.environment.utils.Validate;
import com.zy.environment.utils.log.Logger;
import com.zy.environment.utils.FylToast;
import com.zy.environment.utils.ToolsUtils;
import com.zy.environment.widget.DialogUtils;
import com.zy.machine.MachineFactroy;
import com.zy.machine.MachineManage;
import com.zy.machine.OnDataListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import io.reactivex.functions.Consumer;


public class MainActivity extends BaseActivity {

    private static final String TAG="MainActivity";
    private ImageView ivScanCode;
    private TextView tvDeviceid;
    private final Gson gson = new Gson();
    private MachineManage machineManage;//硬件连接
    private String order_sn = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();//权限申请
        findViewById();
        initView();
        initData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (machineManage != null)
            machineManage.closeDevice();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handlerEvent(String event) {
        Logger.e("MainActivity", "handlerEvent---event:"+event);
        OkWebSocket.closeAllNow();
        if (machineManage != null)
            machineManage.closeDevice();
        initData();//刷新连接
    }


    private void initView() {
        GlobalSetting.deviceid = ToolsUtils.getDeviceId(this);
        tvDeviceid.setText("设备号："+ GlobalSetting.deviceid);
        tvDeviceid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToolsUtils.continuousClick(activity);//进入设置页面
            }
        });
    }

    private void initData() {
        GlobalSetting.getSetting(this);
        websocketConnect();
        machineManage = MachineFactroy.init(GlobalSetting.MachineType);
        machineManage.setOutLength(GlobalSetting.outLen);
        machineManage.openDevice(mListener);
    }


    /*
    * 设备登录
    * */
    @SuppressLint("CheckResult")
    private void deviceLogin(){
        MsgBean msgBean = new MsgBean("login");
        socketSend(msgBean);
    }

    private void socketSend(MsgBean msgBean){
        Logger.e(TAG, "客户端发送消息：" + gson.toJson(msgBean));
        OkWebSocket.send(GlobalSetting.wsurl, gson.toJson(msgBean)).subscribe();
    }

    /*
     * 连接服务器
     * */
    @SuppressLint("CheckResult")
    private void websocketConnect() {
        DialogUtils.getInstance().showLoadingDialog(activity,"设备连接中...");
        OkWebSocket.get(GlobalSetting.wsurl).subscribe(new Consumer<WebSocketInfo>() {
            @Override
            public void accept(WebSocketInfo webSocketInfo) throws Exception {
                Logger.e(TAG, "客户端收到消息：" + webSocketInfo.toString());
                switch(webSocketInfo.getStatus()){
                    case WebSocketStatus.STATUS_CONNECTED://连接成功
                        deviceLogin();
                        showText("服务器连接成功");
                        break;
                     case WebSocketStatus.STATUS_ON_CLOSED://断开连接
                        DialogUtils.getInstance().closeLoadingDialog();
                        break;
                    case WebSocketStatus.STATUS_ON_FAILURE://连接异常
                        DialogUtils.getInstance().closeLoadingDialog();
                        showText("服务器连接失败", true);
                        break;
                    case WebSocketStatus.STATUS_ON_REPLY://收到服务端消息
                        Type listType = new TypeToken<MsgBean>() {}.getType();
                        try {
                            MsgBean msgBean = gson.fromJson(webSocketInfo.getStringMsg(), listType);
                            cmdHandle(msgBean);
                        }catch (Exception e){
                            showText("服务器参数错误");
                        }
                        break;
                    default:
                        DialogUtils.getInstance().closeLoadingDialog();
                        break;
                }

            }
        });
    }

    /*
    * 解析服务器指令
    * */
    private void cmdHandle(MsgBean msgBean){
        switch(msgBean.getType()){
            case MsgType.TYPE_OUT://出货
                YSpeech.getInstance().toSpeech("正在出货，请稍后");
                order_sn = msgBean.getOrder_sn();
                //调用硬件部分
                machineManage.outGoods();
                break;
            case MsgType.TYPE_HEART://心跳

                break;
            case MsgType.TYPE_LOGIN://登录
                DialogUtils.getInstance().closeLoadingDialog();
                Glide.with(this)
                        .load(msgBean.getQrcode_url())
                        .placeholder(R.drawable.qrcode_bg)
                        .error(R.drawable.qrcode_bg)
                        .centerCrop()
                        .into(ivScanCode);

                break;
            case MsgType.TYPE_OUTBACK://出货回调

                break;
            case MsgType.TYPE_QRMSG://二维码广告

                break;
            case MsgType.TYPE_OTHER://其他
                showText("消息提示："+msgBean.getMsg());
                break;

        }
    }

    /*
    * 设备控制回调
    * */
    private final OnDataListener mListener =
            new OnDataListener() {

                @Override
                public void onConnect() {
                    Logger.d(TAG,"device onConnect");
                }

                @Override
                public void onDisConnect() {
                    Logger.d(TAG,"device onDisConnect");
                }

                @Override
                public void onError(int errcode,String err) {
                    Logger.e(TAG,"onError:"+err);
                    if (Validate.noNull(order_sn)){
                        MsgBean msgBean = new MsgBean("back");
                        msgBean.setOrder_sn(order_sn);
                        msgBean.setResult("2");
                        order_sn = "";
                        socketSend(msgBean);
                    }
                    if (errcode == 1000 || errcode == 1001 || errcode == 1003){
                        showText(err, true);
                    }else {
                        showText(err);
                    }
                }

                @Override
                public void onSuccess() {
                    Logger.e(TAG,"出袋成功");
                    showText("出袋成功");
                    MsgBean msgBean = new MsgBean("back");
                    msgBean.setOrder_sn(order_sn);
                    msgBean.setResult("1");
                    socketSend(msgBean);

                }
            };


    private void showText(String msg){
        showText(msg, false);
    }

    private void showText(String msg, boolean isErr){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isErr){
                    DialogUtils.getInstance().showErrDialog(activity,msg);
                }else {
                    FylToast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void findViewById() {
        ivScanCode = (ImageView) findViewById(R.id.iv_scan_code);
        tvDeviceid = (TextView) findViewById(R.id.tv_deviceid);
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    @SuppressLint("CheckResult")
    private void initPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    //表示用户同意权限
                    Toast.makeText(MainActivity.this, "用户同意使用权限", Toast.LENGTH_SHORT).show();
                } else {
                    //表示用户不同意权限
                    Toast.makeText(MainActivity.this, "用户拒绝使用权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}