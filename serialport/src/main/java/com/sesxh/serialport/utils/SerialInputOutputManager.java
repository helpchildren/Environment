/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.sesxh.serialport.utils;

import android.os.Process;
import android.util.Log;


import com.sesxh.serialport.usbserial.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which services a {@link UsbSerialPort} in its {@link #run()} method.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialInputOutputManager implements Runnable {

    private static final String TAG = SerialInputOutputManager.class.getSimpleName();
    public static boolean DEBUG = false;
    private static final int BUFSIZ = 4096;

    /**
     * default read timeout is infinite, to avoid data loss with bulkTransfer API
     */
    private int mReadTimeout = 0;
    private int mWriteTimeout = 0;

    private final Object mReadBufferLock = new Object();
    private final Object mWriteBufferLock = new Object();

    private ByteBuffer mReadBuffer; // default size = getReadEndpoint().getMaxPacketSize()
    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

    public enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    private int mThreadPriority = Process.THREAD_PRIORITY_URGENT_AUDIO;
    private State mState = State.STOPPED; // Synchronized by 'this'
    private Listener mListener; // Synchronized by 'this'
    private final UsbSerialPort mSerialPort;

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        void onNewData(byte[] data, int size);

        /**
         * Called when {@link SerialInputOutputManager#run()} aborts due to an error.
         */
        void onRunError(Exception e);
    }

    public SerialInputOutputManager(UsbSerialPort serialPort) {
        mSerialPort = serialPort;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
    }

    public SerialInputOutputManager(UsbSerialPort serialPort, Listener listener) {
        mSerialPort = serialPort;
        mListener = listener;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
    }

    public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

    /**
     * setThreadPriority. By default a higher priority than UI thread is used to prevent data loss
     *
     * @param threadPriority see {@link Process#setThreadPriority(int)}
     */
    public void setThreadPriority(int threadPriority) {
        if (mState != State.STOPPED)
            throw new IllegalStateException("threadPriority only configurable before SerialInputOutputManager is started");
        mThreadPriority = threadPriority;
    }

    /**
     * read/write timeout
     */
    public void setReadTimeout(int timeout) {
        // when set if already running, read already blocks and the new value will not become effective now
        if (mReadTimeout == 0 && timeout != 0 && mState != State.STOPPED)
            throw new IllegalStateException("readTimeout only configurable before SerialInputOutputManager is started");
        mReadTimeout = timeout;
    }

    public int getReadTimeout() {
        return mReadTimeout;
    }

    public void setWriteTimeout(int timeout) {
        mWriteTimeout = timeout;
    }

    public int getWriteTimeout() {
        return mWriteTimeout;
    }

    /**
     * read/write buffer size
     */
    public void setReadBufferSize(int bufferSize) {
        if (getReadBufferSize() == bufferSize)
            return;
        synchronized (mReadBufferLock) {
            mReadBuffer = ByteBuffer.allocate(bufferSize);
        }
    }

    public int getReadBufferSize() {
        return mReadBuffer.capacity();
    }

    public void setWriteBufferSize(int bufferSize) {
        if (getWriteBufferSize() == bufferSize)
            return;
        synchronized (mWriteBufferLock) {
            ByteBuffer newWriteBuffer = ByteBuffer.allocate(bufferSize);
            if (mWriteBuffer.position() > 0)
                newWriteBuffer.put(mWriteBuffer.array(), 0, mWriteBuffer.position());
            mWriteBuffer = newWriteBuffer;
        }
    }

    public int getWriteBufferSize() {
        return mWriteBuffer.capacity();
    }

    /**
     * when using writeAsync, it is recommended to use readTimeout != 0,
     * else the write will be delayed until read data is available
     */
    public void writeAsync(byte[] data) {
        synchronized (mWriteBufferLock) {
            mWriteBuffer.put(data);
        }
    }

    /**
     * start SerialInputOutputManager in separate thread
     */
    public void start() {
        if (mState != State.STOPPED)
            throw new IllegalStateException("already started");
        new Thread(this, this.getClass().getSimpleName()).start();
    }

    /**
     * stop SerialInputOutputManager thread
     * <p>
     * when using readTimeout == 0 (default), additionally use usbSerialPort.close() to
     * interrupt blocking read
     */
    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    public synchronized State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running");
            }
            mState = State.RUNNING;
        }
        Log.i(TAG, "Running ...");
        try {
            if (mThreadPriority != Process.THREAD_PRIORITY_DEFAULT)
                Process.setThreadPriority(mThreadPriority);
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step();
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final Listener listener = getListener();
            if (listener != null) {
                listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = State.STOPPED;
                Log.i(TAG, "Stopped");
            }
        }
    }

    boolean isStartRead = false;//是否开始读取串口数据
    List<byte[]> cmdDataList = new ArrayList<>();

    private void step() throws IOException {
        // Handle incoming data.
//        byte[] buffer;
//        synchronized (mReadBufferLock) {
//            buffer = mReadBuffer.array();
//        }
//        int len = mSerialPort.read(buffer, mReadTimeout);
//        if (len > 0) {
//            if (DEBUG) Log.d(TAG, "Read data len=" + len);
//            final Listener listener = getListener();
//            if (listener != null) {
//                final byte[] data = new byte[len];
//                System.arraycopy(buffer, 0, data, 0, len);
//                listener.onNewData(data);
//            }
//        }

        byte[] buffer;
        synchronized (mReadBufferLock) {
            buffer = mReadBuffer.array();
        }
        // 此延迟会导致长数据接收不对
//        if (isStartRead){
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            Log.d("lfntest", "延时1秒");
//        }
        int len = mSerialPort.read(buffer, 20);
        if (len > 0) {
            if (!isStartRead) {
//                Log.d("lfntest", "开始接收数据:" + len);
                isStartRead = true;
                cmdDataList = new ArrayList<>();
            }
            byte[] data = new byte[len];
            System.arraycopy(buffer, 0, data, 0, len);
//            Log.d("lfntest", "读取数据长度:" + len + " 数组:" + Arrays.toString(data));
            cmdDataList.add(data);
        } else {
            if (isStartRead) {
//                Log.d("lfntest", "读取完成");
                int cmdLen = 0;
                for (int i = 0; i < cmdDataList.size(); i++) {
                    cmdLen += cmdDataList.get(i).length;
                }
                final byte[] cmdDataByte = new byte[cmdLen];
                int desPost = 0;
                for (int i = 0; i < cmdDataList.size(); i++) {
                    if (i == 0) {
                        System.arraycopy(cmdDataList.get(i), 0, cmdDataByte, 0, cmdDataList.get(i).length);
                    } else {
                        desPost += cmdDataList.get(i - 1).length;
                        System.arraycopy(cmdDataList.get(i), 0, cmdDataByte, desPost, cmdDataList.get(i).length);
                    }
//                    Log.e("lfntest", "拼接第" + i + "串: 数组:" + Arrays.toString(cmdDataByte));
                }
//                Log.d("lfntest", "读取完成后拼接指令长度:" + cmdLen + " 数组:" + Arrays.toString(cmdDataByte));
                final Listener listener = getListener();
                if (listener != null) {
                    listener.onNewData(cmdDataByte, cmdLen);
                }
            }
            isStartRead = false;
        }

        // Handle outgoing data.
        buffer = null;
        synchronized (mWriteBufferLock) {
            len = mWriteBuffer.position();
            if (len > 0) {
                buffer = new byte[len];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buffer, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (buffer != null) {
            if (DEBUG) {
                Log.d(TAG, "Writing data len=" + len);
            }
            mSerialPort.write(buffer, mWriteTimeout);
        }
    }

}
