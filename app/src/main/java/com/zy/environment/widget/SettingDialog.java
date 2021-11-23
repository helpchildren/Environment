package com.zy.environment.widget;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zy.environment.R;
import com.zy.environment.base.BaseDialog;
import com.zy.environment.config.GlobalSetting;
import com.zy.environment.utils.EventBusUtils;
import com.zy.environment.utils.FylToast;
import com.zy.environment.utils.Validate;

import androidx.annotation.NonNull;

public class SettingDialog extends BaseDialog {

    private EditText ed_input, eDeviceserialPort, eWsurl, eOutlen;
    private LinearLayout llPassword, llSetting;
    private Button btnCancle, btnOk;

    public static boolean isShowSet = false;

    public SettingDialog(@NonNull Context context) {
        super(context, R.style.BaseDialog, R.layout.setting_items);
        isShowSet = false;
    }

    @Override
    public void bindView(View v) {
        ed_input = (EditText) findViewById(R.id.ed_input);
        btnCancle = (Button) findViewById(R.id.btn_cancle);
        btnOk = (Button) findViewById(R.id.btn_ok);
        llPassword = (LinearLayout) findViewById(R.id.ll_password);
        llSetting = (LinearLayout) findViewById(R.id.ll_setting);
        eDeviceserialPort = (EditText) findViewById(R.id.e_deviceserialPort);
        eWsurl  = (EditText) findViewById(R.id.e_wsurl);
        eOutlen  = (EditText) findViewById(R.id.e_outlen);

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
                        FylToast.makeText(getContext(), "配置项不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GlobalSetting.wsurl = eWsurl.getText().toString();
                    GlobalSetting.serialPort = eDeviceserialPort.getText().toString();
                    GlobalSetting.outLen = Integer.parseInt(eOutlen.getText().toString());
                    //保存
                    GlobalSetting.putSetting(getContext());
                    FylToast.makeText(getContext(), "设置成功", Toast.LENGTH_SHORT).show();
                    EventBusUtils.post("Refresh Main");
                    dismiss();
                }else {
                    String password = ed_input.getText().toString();
                    if ("zy123".equals(password)) {
                        isShowSet = true;
                        llPassword.setVisibility(View.GONE);
                        llSetting.setVisibility(View.VISIBLE);
                    } else {
                        FylToast.makeText(getContext(), "密码错误，请重新输入！", Toast.LENGTH_SHORT).show();
                        ed_input.setText("");
                    }
                }
            }
        });
        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }



}