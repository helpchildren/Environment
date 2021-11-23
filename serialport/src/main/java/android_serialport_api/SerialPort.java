/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android_serialport_api;



import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//获取串口的类(其实就是获取输入输出流) http://blog.csdn.net/qiwenmingshiwo/article/details/49557889
public class SerialPort {

    private static final String TAG = "SerialPort";

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;//文件描述
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    /**
     * 获得一个串口 
     * @param device 设备
     * @param baudrate 波特率
     * @param flags 标志
     * @throws SecurityException
     * @throws IOException
     */
    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        Log.d(TAG, "canRead:"+device.canRead()+",canWrite:"+device.canWrite());
        /* Check access permission 检查权限 */
        if (!device.canRead() || !device.canWrite()) {

            throw new IOException("获取读写权限失败");

//            String cmd = "chmod 777 " + device.getAbsolutePath();
//            if (!RootCommand(cmd)){
//                throw new IOException("获取读写权限失败");
//            }
        }
        //打开设备
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * command 命令：String apkRoot="chmod 777 "+getPackageCodePath(); RootCommand(apkRoot);
     * @return 应用程序是/否获取Root权限
     */
//    private static boolean RootCommand(String command) {
//        Process process = null;
//        DataOutputStream os = null;
//        try {
//            process = Runtime.getRuntime().exec("su");
//            os = new DataOutputStream(process.getOutputStream());
//            os.writeBytes(command + "\n");
//            os.writeBytes("exit\n");
//            os.flush();
//            process.waitFor();
//        } catch (Exception e) {
//            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
//            return false;
//        } finally {
//            try {
//                if (os != null) {
//                    os.close();
//                }
//                process.destroy();
//            } catch (Exception e) {
//            }
//        }
//        Log.d("*** DEBUG ***", "Root SUC ");
//        return true;
//    }

    // JNI
    private native static FileDescriptor open(String path, int baudrate, int flags);
    public native void close();
    static {
        System.loadLibrary("serial_port");
    }
}
