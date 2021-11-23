//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sesxh.serialport.utils;


public class CodeTools {

    public CodeTools() {
    }

    /*
     * ASCII码转16进制
     * */
    public static String convertASCIIToHex(String str) {

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }

        return hex.toString();
    }

    /*
     * 16进制转ASCII码
     * */
    public static String convertHexToASCII(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }

        return sb.toString();
    }

    /**
     * 字符串转换成为16进制
     *
     * @param str
     * @return
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            // sb.append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * 16进制字符串直接转换成为正常字符串
     *
     * @param hexStr
     * @return
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    /*
     * byte转16进制
     * */
    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 255);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }

        return hex;
    }

    /*
     * byte[] 转16进制
     * */
    public static String bytesToHex(byte[] b, int size) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < size; ++i) {
            String hex = Integer.toHexString(b[i] & 255);
            if (hex.length() < 2) {
                hex = "0" + hex;
            }

            sb.append(hex.toUpperCase());
        }

        return sb.toString();
    }

    /*
     * 16进制 转byte
     * */
    public static byte hexTobyte(String hex) {
        return (byte) Integer.parseInt(hex, 16);
    }

    /*
     * 16进制 转byte[]
     * */
    public static byte[] hexTobytes(String hex) throws Exception {
        if (hex.length() < 1) {
            return null;
        } else {
            byte[] result = new byte[hex.length() / 2];
            int j = 0;

            for (int i = 0; i < hex.length(); i += 2) {
                result[j++] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }

            return result;
        }
    }

    /*
     * 16进制字符串转字节数组
     */
    public static byte[] hexString2Bytes(String hex) throws Exception {
        if ((hex == null) || (hex.equals(""))){
            return null;
        }
        else if (hex.length()%2 != 0){
            return null;
        }
        else{
            hex = hex.toUpperCase();
            int len = hex.length()/2;
            byte[] b = new byte[len];
            char[] hc = hex.toCharArray();
            for (int i=0; i<len; i++){
                int p=2*i;
                b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p+1]));
            }
            return b;
        }
    }
    /*
     * 字符转换为字节
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /*
     * CRC校验
     * */
    public static String CRC_16_XMODEM(byte[] bytes) {
        int crc = 0;
        int polynomial = 4129;

        for (int index = 0; index < bytes.length; ++index) {
            byte b = bytes[index];

            for (int i = 0; i < 8; ++i) {
                boolean bit = (b >> 7 - i & 1) == 1;
                boolean c15 = (crc >> 15 & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }

        crc &= 65535;
        String strCrc = Integer.toHexString(crc).toUpperCase();
        return strCrc;
    }

    //神思指令协议组合
    public static String dataDombination(String data) throws Exception {
        String HEAD = "534473457300";//数据头：5个固定字节。0x53 0x44 0x73 0x45 0x73   (SDsEs)
        String LEN = "";//长度值： 4个字节 高字节在前，低字节在后。(CMDR+DATA+CRC)
        String CMDR = "7E23";//命令字：2个字节，高字节在前，低字节在后。比如：0x1100===0x11,0x00.
        String DATA = data;
        DATA = str2HexStr(DATA);//转16进制
//        Log.e("lfntest", "转成16进制的数据==" + DATA);
        if (DATA.length() % 2 == 1) {
            DATA = "0" + DATA;
        }
        String CRC = "0000";//校验字节：2字节。采用CRC-16校验。（LEN+CMDR+DATA）
        int len = (CMDR.length() + DATA.length() + CRC.length()) / 2;

        LEN = Integer.toString(len, 16);
        LEN = addZeroForNum(LEN, 8);//高位补0

        String crcstr = LEN + CMDR + DATA;
        byte[] crcbytes = hexString2Bytes(crcstr);
        CRC = CRC_16_XMODEM(crcbytes);
        if (CRC.length() == 2) {
            CRC = "00" + CRC;
        } else if (CRC.length() == 3) {
            CRC = "0" + CRC;
        }

        return HEAD + LEN + CMDR + DATA + CRC;
    }

    /**
     * str 原字符串
     * strLength 字符串总长
     * */
    public static String addZeroForNum(String str, int strLength) {
        int strLen = str.length();
        if (strLen < strLength) {
            while (strLen < strLength) {
                StringBuffer sb = new StringBuffer();
                sb.append("0").append(str);// 左补0
                // sb.append(str).append("0");//右补0
                str = sb.toString();
                strLen = str.length();
            }
        }
        return str;
    }

    /**
     * int数组转成byte数组
     * @param iSource
     * @return
     */
    public static byte[] toByteArray(int iSource[]) {
        int len = iSource.length;
        byte[] bLocalArr = new byte[len];

        for (int i = 0; i < len; i++) {
            bLocalArr[i] = (byte) (iSource[i] & 0xFF);
        }
        return bLocalArr;
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }

}
