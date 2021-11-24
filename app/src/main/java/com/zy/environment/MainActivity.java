package com.zy.environment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lake.banner.BannerConfig;
import com.lake.banner.BannerStyle;
import com.lake.banner.HBanner;
import com.lake.banner.ImageGravityType;
import com.lake.banner.Transformer;
import com.lake.banner.VideoGravityType;
import com.lake.banner.loader.ViewItemBean;
import com.sesxh.okwebsocket.OkWebSocket;
import com.sesxh.okwebsocket.WebSocketInfo;
import com.sesxh.okwebsocket.annotation.WebSocketStatus;
import com.sesxh.rxpermissions.RxPermissions;
import com.sesxh.yzsspeech.YSpeech;
import com.zy.environment.base.BaseActivity;
import com.zy.environment.bean.AdvBean;
import com.zy.environment.bean.MsgBean;
import com.zy.environment.bean.MsgType;
import com.zy.environment.config.GlobalSetting;
import com.zy.environment.utils.FileStorage;
import com.zy.environment.utils.FylToast;
import com.zy.environment.utils.ToolsUtils;
import com.zy.environment.utils.Validate;
import com.zy.environment.utils.log.Logger;
import com.zy.environment.widget.DialogUtils;
import com.zy.machine.MachineFactroy;
import com.zy.machine.MachineManage;
import com.zy.machine.OnDataListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

import static com.lake.banner.BannerConfig.IMAGE;
import static com.lake.banner.BannerConfig.VIDEO;


public class MainActivity extends BaseActivity {

    private static final String TAG="MainActivity";
    private Activity context;

    private ImageView ivScanCode;
    private TextView tvDeviceid;

    private final Gson gson = new Gson();
    private MachineManage machineManage;//硬件连接
    private String order_sn = "";
    public boolean mAdvStop = false;//定时任务停止标记
    private List<AdvBean> mAdvList;//广告列表
    private HBanner banner;//轮播

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        initPermission();//权限申请
        findViewById();
        initView();
        initData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        banner.onResume();
    }

    @Override
    protected void onPause() {
        banner.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        banner.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdvStop = true;
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
        //todo 测试
//        List<AdvBean> advList = new ArrayList<>();
//        advList.add(new AdvBean("5", "贵宾3广告", "2", "https://bag.cnwinall.cn/storage/upload/20211105/ac3b508a0466e0730e3a5a93075605ad.mp4"));
//        advList.add(new AdvBean("7", "534543", "1", "https://bag.cnwinall.cn/storage/upload/20211108/c58ad4b0e634ad0b93b4da23e08440da.jpg"));
//        advList.add(new AdvBean("13", "655633", "1", "https://bag.cnwinall.cn/storage/upload/20211108/da6bea145272b59238c3dceff3b3df08.png"));
//        advList.add(new AdvBean("16", "233424", "2", "https://bag.cnwinall.cn/storage/upload/20211109/d219678ae0aedd19d198766168e6a9f3.mp4"));
//        FileStorage.saveToFile(GlobalSetting.AdvPath, GlobalSetting.AdFile, gson.toJson(advList));

        GlobalSetting.deviceid = ToolsUtils.getDeviceId(context);
        tvDeviceid.setText("设备号："+ GlobalSetting.deviceid);
        tvDeviceid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToolsUtils.continuousClick(activity);//进入设置页面
            }
        });
        //轮播初始化
        List<ViewItemBean> list = new ArrayList<>();
        banner.setViews(list)
                .setBannerAnimation(Transformer.Default)//换场方式
                .setBannerStyle(BannerStyle.CIRCLE_INDICATOR_TITLE)//指示器模式
                .setCache(true)//可以不用设置，默认为true
//                .setCachePath(getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + "hbanner")
                .setCachePath(GlobalSetting.AdvPath + File.separator + "hbanner")
                .setVideoGravity(VideoGravityType.CENTER)//视频布局方式
                .setImageGravity(ImageGravityType.FIT_XY)//图片布局方式
                .setPageBackgroundColor(Color.BLACK)//设置背景
                .setShowTitle(false)//是否显示标题
                .setViewPagerIsScroll(true)//是否支持手滑
                .start();

        //本地广告获取
        String advJson = FileStorage.getFileText(GlobalSetting.AdvPath, GlobalSetting.AdFile);
        if (Validate.noNull(advJson)){
            Type listType = new TypeToken<List<AdvBean>>() {}.getType();
            mAdvList = gson.fromJson(advJson, listType);
            Logger.i(TAG,"本地广告列表："+advJson);
        }else {
            mAdvList = new ArrayList<>();
        }
        updateAdv(mAdvList);
    }

    private void initData() {
        GlobalSetting.getSetting(context);
        websocketConnect();
        //获取硬件控制
        machineManage = MachineFactroy.init(GlobalSetting.machineType, context);
        machineManage.setOutLength(GlobalSetting.outLen);
        machineManage.openDevice(mListener);
    }

    /*
    * 设备登录
    * */
    private void deviceLogin(){
        MsgBean msgBean = new MsgBean("login");
        socketSend(msgBean);
    }

    /*
    * 定时拉取广告
    * */
    @SuppressLint("CheckResult")
    private void getAdv(){
        mAdvStop = false;
        //定时获取广告信息 5分钟一次
        Flowable.interval(2, 5, TimeUnit.MINUTES).takeWhile(aLong -> !mAdvStop).subscribe(aLong -> {
            Logger.i(TAG,"定时任务 拉取广告："+aLong);
            MsgBean msgBean = new MsgBean("qrcode");
            socketSend(msgBean);
        });
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
                        getAdv();
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
                updateQRcode(msgBean.getQrcode_url());

                break;
            case MsgType.TYPE_OUTBACK://出货回调

                break;
            case MsgType.TYPE_QRMSG://二维码广告
                updateQRcode(msgBean.getQrcode_url());
                //todo 合并广告
                updateAdv(msgBean.getAdv());
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

    /*
    * 更新广告
    * */
    private void updateAdv(List<AdvBean> advList) {
        if (advList != null && advList.size()>0 ){
            List<ViewItemBean> bannerList = new ArrayList<>();
            for (int i = 0; i < advList.size(); i++) {
                AdvBean advBean = advList.get(i);
                if (advBean.isVideo()){
                    int time = ToolsUtils.getVideoTime(context, Uri.parse(advBean.getUrl()));
                    time = time !=0 ? time:BannerConfig.TIME;
                    bannerList.add(new ViewItemBean(VIDEO, advBean.getScreen_name(), advBean.getUrl(), time));
                }else {
                    bannerList.add(new ViewItemBean(IMAGE, advBean.getScreen_name(), Uri.parse(advBean.getUrl()), BannerConfig.TIME));
                }
            }
            banner.update(bannerList);
            banner.setVisibility(View.VISIBLE);
        }else {
            banner.setVisibility(View.GONE);
        }
    }

    /*
    * 更新二维码
    * */
    private void updateQRcode(String qrcode){
        Glide.with(context)
                .load(qrcode)
                .placeholder(R.drawable.qrcode_bg)
                .error(R.drawable.qrcode_bg)
                .centerCrop()
                .into(ivScanCode);
    }

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
        banner = (HBanner) findViewById(R.id.banner);
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    @SuppressLint("CheckResult")
    private void initPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe();
    }

}