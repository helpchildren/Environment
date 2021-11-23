package com.zy.machine;

import com.zy.machine.device.SQ800Machine;

public class MachineFactroy {

    public static MachineManage init(int type) {
        MachineManage manage;
        if (type == 0){
            manage = new SQ800Machine();
        }else {
            manage = new SQ800Machine();
        }
        return manage;
    }

}
