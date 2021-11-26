package com.zy.machine;

import android.content.Context;

import com.zy.machine.device.SQ800Machine;
import com.zy.machine.device.YiNuoMachine;

public class MachineFactroy {

    public static MachineManage init(int type, Context context) {
        MachineManage manage;
        if (type == 0){
            manage = new YiNuoMachine(context);//益诺
        }else {
            manage = new SQ800Machine();//鼎戟
        }
        return manage;
    }

}
