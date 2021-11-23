package com.zy.environment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
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
import com.zy.environment.base.BaseActivity;
import com.zy.environment.bean.MsgBean;
import com.zy.environment.bean.MsgType;
import com.zy.environment.config.GlobalSetting;
import com.zy.environment.utils.log.Logger;
import com.zy.environment.utils.FylToast;
import com.zy.environment.utils.ToolsUtils;
import com.zy.environment.widget.DialogUtils;

import java.lang.reflect.Type;

import io.reactivex.functions.Consumer;


public class MainActivity extends BaseActivity {

    private static final String TAG="MainActivity";
    private ImageView ivScanCode;
    private TextView tvDeviceid;
    private  Gson gson = new Gson();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();//权限申请
        findViewById();
        initView();
        initData();

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

    }

    /*
    * 设备登录
    * */
    @SuppressLint("CheckResult")
    private void deviceLogin(){
//        MsgBean msgBean = new MsgBean("login",GlobalSetting.deviceid);
        MsgBean msgBean = new MsgBean("login","DJEF9BD9A12");
        Logger.e(TAG, "客户端发送消息：" + gson.toJson(msgBean));
        OkWebSocket.send(GlobalSetting.wsurl, gson.toJson(msgBean)).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                Log.e(TAG, "accept  aBoolean："+aBoolean);
            }
        });
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
                        FylToast.makeText(activity, "服务器连接成功", Toast.LENGTH_SHORT).show();
                        break;
                     case WebSocketStatus.STATUS_ON_CLOSED://断开连接
                        DialogUtils.getInstance().closeDialog();
                        FylToast.makeText(activity, "服务器断开连接", Toast.LENGTH_SHORT).show();
                        break;
                    case WebSocketStatus.STATUS_ON_FAILURE://连接异常
                        DialogUtils.getInstance().closeDialog();
                        FylToast.makeText(activity, "服务器连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case WebSocketStatus.STATUS_ON_REPLY://收到服务端消息
                        Type listType = new TypeToken<MsgBean>() {}.getType();
                        try {
                            MsgBean msgBean = gson.fromJson(webSocketInfo.getStringMsg(), listType);
                            cmdHandle(msgBean);
                        }catch (Exception e){
                            FylToast.makeText(activity, "服务器参数错误", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        DialogUtils.getInstance().closeDialog();
                        break;
                }

            }
        });
    }

    private void cmdHandle(MsgBean msgBean){
        switch(msgBean.getType()){
            case MsgType.TYPE_OUT://出货

                break;
            case MsgType.TYPE_HEART://心跳

                break;
            case MsgType.TYPE_LOGIN://登录
                DialogUtils.getInstance().closeDialog();
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
                FylToast.makeText(activity, "消息提示："+msgBean.getMsg(), Toast.LENGTH_SHORT).show();
                break;

        }
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