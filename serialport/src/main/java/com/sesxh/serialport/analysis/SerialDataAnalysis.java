package com.sesxh.serialport.analysis;

public class SerialDataAnalysis {

    private final SerialDataManage dataManage = SerialDataManage.getInstance();

    public Object dataHandling(DataType type, byte[] readData, int size){
        switch (type){
            case StringData://转换utf-8字符串
                return  dataManage.stringData(readData, size);
            case HexData://转换16进制字符串
                return  dataManage.hexStringData(readData, size);
            case ByteData://不转换返回byte数组
                return  readData;
            case SesData://只返回神思格式的数据
                return  dataManage.sesData(readData, size);
            default:
                return  dataManage.stringData(readData, size);
        }
    }
}
