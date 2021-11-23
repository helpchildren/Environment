package com.sesxh.serialport;

import android.content.Context;

import com.sesxh.serialport.device.DevSerialPortMng;
import com.sesxh.serialport.device.UsbSerialPortMng;

public class SerialPortFactroy {

    public static SerialPortMng init(Context context, String type) {
        SerialPortMng serialPortMng;
        if ("usb".equals(type)){
            serialPortMng = new UsbSerialPortMng();
            serialPortMng.initDevice(context);
        }else {
            serialPortMng = new DevSerialPortMng();
        }
        return serialPortMng;
    }

}
