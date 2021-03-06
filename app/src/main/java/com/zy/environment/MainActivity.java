package com.zy.environment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.sesxh.appupdata.UpdataController;
import com.sesxh.appupdata.UpdateInfoService;
import com.sesxh.appupdata.bean.UpdateInfo;
import com.sesxh.appupdata.callback.UpdataCallback;
import com.sesxh.okwebsocket.OkWebSocket;
import com.sesxh.okwebsocket.WebSocketInfo;
import com.sesxh.okwebsocket.annotation.WebSocketStatus;
import com.sesxh.rxpermissions.RxPermissions;
import com.zy.environment.base.BaseActivity;
import com.zy.environment.bean.AdvBean;
import com.zy.environment.bean.MsgBean;
import com.zy.environment.bean.MsgType;
import com.zy.environment.config.GlobalSetting;
import com.zy.environment.utils.DownloadUtil;
import com.zy.environment.utils.FileStorage;
import com.zy.environment.utils.FylToast;
import com.zy.environment.utils.SpStorage;
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
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.lake.banner.BannerConfig.IMAGE;
import static com.lake.banner.BannerConfig.VIDEO;


public class MainActivity extends BaseActivity {

    private static final String TAG="MainActivity";

    private ImageView ivScanCode;
    private TextView tvDeviceid;
    private TextView tvVersion;

    private final Gson gson = new Gson();
    private MachineManage machineManage;//????????????
    private String order_sn = "";

    private List<AdvBean> mAdvList = new ArrayList<>();//??????????????????
    private HBanner banner;//??????

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();//????????????
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
        OkWebSocket.closeAllNow();
        if (machineManage != null)
            machineManage.closeDevice();
        DialogUtils.getInstance().releaseDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handlerEvent(String event) {
        Logger.d("MainActivity", "????????????");
        OkWebSocket.closeAllNow();
        if (machineManage != null)
            machineManage.closeDevice();
        initData();//????????????
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        GlobalSetting.deviceid = ToolsUtils.getDeviceId(activity);
        String versionName = ToolsUtils.getVersionName(activity);
        Logger.d("MainActivity", "???????????? ????????????"+versionName+" ????????????"+ GlobalSetting.deviceid);
        tvVersion.setText("????????????v" + versionName);
        tvDeviceid.setText("????????????"+ GlobalSetting.deviceid);
        tvDeviceid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToolsUtils.continuousClick(activity);//??????????????????
            }
        });
        //???????????????
        List<ViewItemBean> list = new ArrayList<>();
        banner.setViews(list)
                .setBannerAnimation(Transformer.Default)//????????????
                .setBannerStyle(BannerStyle.CIRCLE_INDICATOR_TITLE)//???????????????
                .setCache(false)//??????????????????????????????true
//                .setCachePath(GlobalSetting.AdvPath + File.separator + "hbanner")
                .setVideoGravity(VideoGravityType.CENTER)//??????????????????
                .setImageGravity(ImageGravityType.FIT_XY)//??????????????????
                .setPageBackgroundColor(Color.BLACK)//????????????
                .setShowTitle(false)//??????????????????
                .setViewPagerIsScroll(false)//??????????????????
                .start();

        //??????????????????
        String advJson = FileStorage.getFileText(GlobalSetting.AdvPath, GlobalSetting.AdFile);
        if (Validate.noNull(advJson)){
            Type listType = new TypeToken<List<AdvBean>>() {}.getType();
            mAdvList = gson.fromJson(advJson, listType);
            Logger.d(TAG,"???????????????????????? mAdvList???"+ Arrays.toString(mAdvList.toArray()));
        }else {
            mAdvList = new ArrayList<>();
        }
        updateAdv();
    }

    private void initData() {
        GlobalSetting.getSetting(activity);
        websocketConnect();
        //??????????????????
        machineManage = MachineFactroy.init(GlobalSetting.machineType, activity);
        machineManage.setOutLength(GlobalSetting.outLen);
        machineManage.setDevicesPort(GlobalSetting.serialPort);
        machineManage.openDevice(mListener);
    }

    /*
    * ????????????
    * */
    private void deviceLogin(){
        MsgBean msgBean = new MsgBean("login");
        socketSend(msgBean);
    }

    /*
    * ??????????????????
    * */
    @SuppressLint("CheckResult")
    private void getAdv(){
        //????????????????????????5????????????
        Flowable.interval(1, 5, TimeUnit.MINUTES).takeWhile(aLong -> isSocketConn).subscribe(aLong -> {
            Logger.i(TAG,"???????????? ???????????????"+aLong);
            MsgBean msgBean = new MsgBean("qrcode");
            socketSend(msgBean);
        });
    }

    /*
     * ??????????????????
     * */
    @SuppressLint("CheckResult")
    private void putHeart(){
        //????????????????????????5????????????
        Flowable.interval(5, 20, TimeUnit.SECONDS).takeWhile(aLong -> isSocketConn).subscribe(aLong -> {
            MsgBean msgBean = new MsgBean("heartbeat");
            socketSend(msgBean);
        });
    }

    private void socketSend(MsgBean msgBean){
        if (isSocketConn){
            Logger.d(TAG, "????????????????????????" + gson.toJson(msgBean));
            OkWebSocket.send(GlobalSetting.wsurl, gson.toJson(msgBean)).subscribe(new Observer<Boolean>() {
                @Override
                public void onSubscribe(@NonNull Disposable d) { }

                @Override
                public void onNext(@NonNull Boolean aBoolean) { }

                @Override
                public void onError(@NonNull Throwable e) { }

                @Override
                public void onComplete() { }
            });
        }else {
            Logger.d(TAG, "??????????????????????????????socket?????? type:"+msgBean.getType());
        }
    }

    /*
     * ???????????????
     * */
    private boolean isSocketConn = false;
    @SuppressLint("CheckResult")
    private void websocketConnect() {
        OkWebSocket.get(GlobalSetting.wsurl).subscribe(new Consumer<WebSocketInfo>() {
            @Override
            public void accept(WebSocketInfo webSocketInfo) throws Exception {
                Logger.d(TAG, "????????????????????????" + webSocketInfo.toString());
                switch(webSocketInfo.getStatus()){
                    case WebSocketStatus.STATUS_CONNECTED://????????????
                    case WebSocketStatus.STATUS_RE_CONNECTED://????????????
                        isSocketConn = true;
                        DialogUtils.getInstance().closeErrDialog();
                        deviceLogin();
                        getAdv();
                        putHeart();
                        showText("?????????????????????");
                        break;
                    case WebSocketStatus.STATUS_ON_CLOSED://??????
                        isSocketConn = false;
                        break;
                    case WebSocketStatus.STATUS_ON_FAILURE://????????????
                        isSocketConn = false;
                        showText("???????????????????????????????????????...", true);
                        break;
                    case WebSocketStatus.STATUS_ON_REPLY://?????????????????????
                        Type listType = new TypeToken<MsgBean>() {}.getType();
                        try {
                            MsgBean msgBean = gson.fromJson(webSocketInfo.getStringMsg(), listType);
                            cmdHandle(msgBean);
                        }catch (Exception e){
                            showText("?????????????????????");
                        }
                        break;
                    default:
                        break;
                }

            }
        });
    }

    /*
    * ?????????????????????
    * */
    private void cmdHandle(MsgBean msgBean){
        switch(msgBean.getType()){
            case MsgType.TYPE_OUT://??????
                order_sn = msgBean.getOrder_sn();
                //??????????????????
                machineManage.outGoods();
                Logger.d(TAG, "????????????");
                break;
            case MsgType.TYPE_HEART://??????

                break;
            case MsgType.TYPE_LOGIN://??????
                updateQRcode(msgBean.getQrcode_url());
                //????????????????????????
                getAppUpDataInfo();
                break;
            case MsgType.TYPE_OUTBACK://????????????

                break;
            case MsgType.TYPE_QRMSG://???????????????
                updateQRcode(msgBean.getQrcode_url());//???????????????
                comparisonList(msgBean.getAdv());//????????????
                break;
            case MsgType.TYPE_UPLOG://????????????log
                uploadLog(msgBean.getMsg());
                break;
            case MsgType.TYPE_OTHER://??????
                showText("???????????????"+msgBean.getMsg());
                break;
            default:

                break;

        }
    }

    /*
    * ??????????????????
    * */
    private final OnDataListener mListener =
            new OnDataListener() {

                @Override
                public void onConnect() {
                    Logger.d(TAG,"?????? onConnect");
                }

                @Override
                public void onDisConnect() {
                    Logger.d(TAG,"?????? onDisConnect");
                }

                @Override
                public void onError(int errcode,String err) {
                    Logger.d(TAG,"onError:"+err);
                    if (Validate.noNull(order_sn)){
                        MsgBean msgBean = new MsgBean("back");
                        msgBean.setOrder_sn(order_sn);
                        msgBean.setResult("2");
                        order_sn = "";
                        socketSend(msgBean);
                    }
                    if (errcode == 1000 || errcode == 1001 || errcode == 1003){
                        showText(err+",?????????????????????", true);
                    }else {
                        showText(err);
                    }
                }

                @Override
                public void onSuccess() {
                    Logger.d(TAG,"????????????");
                    showText("????????????");
                    MsgBean msgBean = new MsgBean("back");
                    msgBean.setOrder_sn(order_sn);
                    msgBean.setResult("1");
                    socketSend(msgBean);

                }
            };




    private int downCount = 0;//????????????
    /*
    * ????????????????????????
     * */
    private void comparisonList(List<AdvBean> advList){
        //?????????????????????????????????
        if (!ToolsUtils.isListEqual(mAdvList, advList)){
            //?????????????????????
            // ?????????
            mAdvList.removeAll(advList);
            List<AdvBean> delList = new ArrayList<>(mAdvList);
            if (delList.size() > 0){
                //?????????????????????
                for (int i = 0; i < delList.size(); i++) {
                    FileStorage.delete(GlobalSetting.AdvPath +File.separator+ delList.get(i).getDirName());
                }
            }
            //????????????
            mAdvList = new ArrayList<>(advList);
            //??????????????????????????????
            List<AdvBean> downAdvList = new ArrayList<>();//???????????????
            for (int i = 0; i < mAdvList.size(); i++) {
                AdvBean advBean = mAdvList.get(i);
                String dirName;
                if (advBean.isVideo()){
                    dirName = "Video_"+advBean.getId()+ DownloadUtil.VIDEO;
                }else {
                    dirName = "Image_"+advBean.getId()+ DownloadUtil.IMAGE;
                }
                advBean.setDirName(dirName);
                if (!DownloadUtil.fileIsExists(dirName, GlobalSetting.AdvPath, advBean.isVideo()?DownloadUtil.VIDEO:DownloadUtil.IMAGE)) {
                    downAdvList.add(advBean);//?????????????????????
                }
            }

            if (downAdvList.size() > 0){
                downCount = 0;
                for (int i = 0; i < downAdvList.size(); i++) {
                    AdvBean advBean = downAdvList.get(i);
                    Logger.d(TAG, "????????????:"+advBean.getId()+" "+downCount);
                    //????????????????????????
                    DownloadUtil.get().download(advBean.getUrl(), GlobalSetting.AdvPath, advBean.getDirName(), new DownloadUtil.OnDownloadListener() {
                        @Override
                        public void onDownloadSuccess(File file, String fileName) {
                            downCount++;
                            Logger.d(TAG, "????????????:"+fileName+" "+downCount);
                            if (downCount == downAdvList.size()){
                                composeList();
                            }
                        }

                        @Override
                        public void onDownloading(int progress) {
                            // ?????????
                        }

                        @Override
                        public void onDownloadFailed(Exception e, String fileName) {
                            e.printStackTrace();
                            downCount++;
                            Logger.d(TAG, "????????????:"+e.getMessage()+" "+downCount);
                            AdvBean faildAdvBean = getFaildAdvBean(downAdvList, fileName);
                            if (faildAdvBean != null) {
                                mAdvList.remove(faildAdvBean);
                            }
                            if (downCount == downAdvList.size()){
                                composeList();
                            }
                        }
                    });

                }
            }else {
                composeList();
            }
        }
    }

    private AdvBean getFaildAdvBean(List<AdvBean> downAdvList, String fileName){
        if (downAdvList!=null && downAdvList.size()>0){
            for (int i = 0; i < downAdvList.size(); i++) {
                if (fileName.equals(downAdvList.get(i).getDirName())){
                    return downAdvList.get(i);
                }
            }
        }
        return null;
    }

    /*
    * ??????List
    * */
    private void composeList(){
        Logger.e("lfntest","?????????????????? mAdvList???"+ Arrays.toString(mAdvList.toArray()));
        //???????????????
        FileStorage.saveToFile(GlobalSetting.AdvPath, GlobalSetting.AdFile, gson.toJson(mAdvList));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAdv();
            }
        });
    }

    /*
    * ????????????
    * */
    private void updateAdv() {
        if (mAdvList != null && mAdvList.size()>0 ){
            List<ViewItemBean> bannerList = new ArrayList<>();
            for (int i = 0; i < mAdvList.size(); i++) {
                AdvBean advBean = mAdvList.get(i);
//                Uri uri = ToolsUtils.getUriForFileName(advBean.getDirName());
                if (advBean.isVideo()){
                    int time = ToolsUtils.getVideoTime(advBean.getDirPath());
                    time = time !=0 ? time:BannerConfig.TIME;
                    bannerList.add(new ViewItemBean(VIDEO, advBean.getScreen_name(), advBean.getDirPath(), time));
                }else {
                    bannerList.add(new ViewItemBean(IMAGE, advBean.getScreen_name(), advBean.getDirPath(), BannerConfig.TIME));
                }
            }
            banner.update(bannerList);
            banner.setVisibility(View.VISIBLE);
        }else {
            banner.onPause();
            banner.setVisibility(View.GONE);
        }
    }

    /*
    * ???????????????
    * */
    private void updateQRcode(String qrcode){
        Glide.with(activity)
                .load(qrcode)
//                .placeholder(R.drawable.qrcode_bg)
//                .error(R.drawable.qrcode_bg)
                .centerCrop()
                .into(ivScanCode);
    }

    /*
    * ??????apk??????
    * */
    private void getAppUpDataInfo() {
        UpdataController.getUpDataInfo(activity, new UpdataCallback() {
            @Override
            public void onUpdate(boolean isNeed, UpdateInfo info) {
                Logger.d("lfntest", "???????????????????????????" + info.toString());
                if (isNeed){//????????????????????????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateInfoService.getInstance().downLoadFile(info.getApkurl());//????????????
                        }
                    });
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("lfntest", "?????????????????????" + msg);
            }
        });
    }

    /**
     * ?????????????????????????????????
     */
    public void uploadLog(String filename){
        File file = new File(GlobalSetting.externpath+"/zy/"+getPackageName()+"/ToolXLog/"+filename);
        // ?????????????????????????????????????????????????????????????????? ????????????
        if (file.exists() && file.isFile()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DownloadUtil.get().upload(GlobalSetting.uploadUrl, file);
                    } catch (IOException e) {
                        Logger.d("uploadLog","??????????????????"+e.toString());
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void showText(String msg){
        showText(msg, false);
    }

    private void showText(String msg, boolean isErr){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isErr){
                    Logger.d("ErrDialog", "showErrDialog:"+msg);
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
        tvVersion = (TextView) findViewById(R.id.tv_version);
        banner = (HBanner) findViewById(R.id.banner);
    }

    /**
     * android 6.0 ??????????????????????????????
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