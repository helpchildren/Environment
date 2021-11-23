package com.zy.environment.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.zy.environment.R;
import com.zy.environment.base.BaseDialog;
import com.zy.environment.config.GlobalSetting;

public class ToolsUtils {


    public static String getDeviceId(Context context){
        String str = Settings.System.getString(context.getContentResolver(), "android_id").toUpperCase();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DJ");
        stringBuilder.append(str.substring(str.length() - 8, str.length()));
        return stringBuilder.toString();
    }

    final static int COUNTS = 3;// 点击次数
    final static long DURATION = 1000;// 规定有效时间
    static long[] mHits = new long[COUNTS];
    public static void continuousClick(Activity activity) {
        //每次点击时，数组向前移动一位
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        //为数组最后一位赋值
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
            mHits = new long[COUNTS];//重新初始化数组
            //弹出密码框
            showSettingDialog(activity);
        }
    }

    public static boolean isShowSet = false;

    public static void showSettingDialog(Activity activity) {
        isShowSet = false;
        final BaseDialog baseDialog = new BaseDialog(activity, R.style.BaseDialog, R.layout.setting_items);
        EditText ed_input = (EditText) baseDialog.getView(R.id.ed_input);
        Button btnCancle = (Button) baseDialog.getView(R.id.btn_cancle);
        Button btnOk = (Button) baseDialog.getView(R.id.btn_ok);
        LinearLayout llPassword = (LinearLayout) baseDialog.getView(R.id.ll_password);
        LinearLayout llSetting = (LinearLayout) baseDialog.getView(R.id.ll_setting);
        EditText eDeviceserialPort = (EditText) baseDialog.getView(R.id.e_deviceserialPort);
        EditText eWsurl = (EditText) baseDialog.getView(R.id.e_wsurl);
        EditText eOutlen = (EditText) baseDialog.getView(R.id.e_outlen);
        eWsurl.setText(GlobalSetting.wsurl);
        eDeviceserialPort.setText(GlobalSetting.serialPort);
        eOutlen.setText(GlobalSetting.outLen+"");


        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowSet){
                    if (Validate.isNull(eWsurl.getText().toString())
                            || Validate.isNull(eDeviceserialPort.getText().toString())
                            || Validate.isNull(eOutlen.getText().toString())){
                        FylToast.makeText(activity, "配置项不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GlobalSetting.wsurl = eWsurl.getText().toString();
                    GlobalSetting.serialPort = eDeviceserialPort.getText().toString();
                    GlobalSetting.outLen = Integer.parseInt(eOutlen.getText().toString());
                    //保存
                    GlobalSetting.putSetting(activity);
                    FylToast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();
                    baseDialog.dismiss();
                }else {
                    String password = ed_input.getText().toString();
                    if ("zy123".equals(password)) {
                        isShowSet = true;
                        llPassword.setVisibility(View.GONE);
                        llSetting.setVisibility(View.VISIBLE);
                    } else {
                        FylToast.makeText(activity, "密码错误，请重新输入！", Toast.LENGTH_SHORT).show();
                        ed_input.setText("");
                    }
                }
            }
        });
        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseDialog.dismiss();
            }
        });
        baseDialog.show();
    }
}
