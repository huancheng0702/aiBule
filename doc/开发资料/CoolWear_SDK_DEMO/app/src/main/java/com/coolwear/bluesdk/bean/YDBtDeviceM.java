package com.coolwear.bluesdk.bean;

import static android.bluetooth.BluetoothDevice.PHY_LE_1M_MASK;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.queue.CEProtocolB;
import ce.com.cenewbluesdk.uitl.BleSystemUtils;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Lg;
import ce.com.cenewbluesdk.uitl.Logger;

public class YDBtDeviceM {
    public static final String Uuid = "0000f618-0000-1000-8000-00805f9b34fb";
    public String devMacAddress;
    public String devName;
    public int devGsensorX;
    public int devGsensorY;
    public int devGsensorZ;
    public int devRssi;
    public boolean disconnectByService;
    /**
     * 0未连接 1正在连接 2已连接
     */
    public int devConnectState =2;
    private boolean mConnected = false;
    private BluetoothGatt mBluetoothGatt;
    public BluetoothGattCharacteristic writeChar;
    private Handler postHandler = new Handler(Looper.getMainLooper());
    private List<BluetoothGattService> mBluetoothGattServices = null;
    private List<BluetoothGattCharacteristic> writeCharas = new ArrayList<BluetoothGattCharacteristic>();
    private List<BluetoothGattCharacteristic> readCharas = new ArrayList<BluetoothGattCharacteristic>();
    private static int MTU = 23;
    public YDBtDeviceM(BluetoothDevice btDev){
        this.devName = btDev.getName();
        this.devMacAddress = btDev.getAddress();
        this.devGsensorX = 0;
        this.devGsensorY = 0;
        this.devGsensorZ = 0;
        devConnectState = 0;
    }

//    public int getDevColor(){
//        if (devConnectState == 0){
//            return R.color.colorPrimary_disconnect;
//        }
//        else if (devConnectState == 2){
//            return R.color.colorPrimary_connect;
//        }
//        return R.color.colorPrimary_disconnect;
//    }
    public String getDevConnState(){
        if (devConnectState == 0){
            return "未连接";
        }
        else if (devConnectState == 2){
            return "已连接";
        }
        else {
            return "连接中...";
        }
    }
    public String getXyz(){
        return String.format("X:%d Y:%d Z:%d", devGsensorX, devGsensorY, devGsensorZ);
    }

    public void sendEnterBackgroupData(){
        if (writeChar != null){
            byte[] tSmsByte = {0x00, 0x01, 0x00, 0x00, 0x01, 0x6d, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendSmsNtf(){
        if (writeChar != null){
            byte[] tSmsByte = {0x00, 0x01, 0x00, 0x00, 0x01, 0x6b, 0x00, 0x00, 0x1C, 0x00, 0x7d, (byte) 0x85, (byte)0xf1, 0x5a, 0x01, 0x01, 0x00, 0x02, 0x38, 0x38};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendSmsNtf(int index){
        if (index == 0){
            byte[] tSmsByte = {0x01, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
        else if (index == 1){
            byte[] tSmsByte = {0x02, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
        else if (index == 2){
            byte[] tSmsByte = {0x03, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38, 0x38};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendTestDevData(){
        if (writeChar != null){
            byte[] tSmsByte = {0x00, 0x01, 0x00, 0x00, 0x01, (byte) 0xCC, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendTestDevData1(){
        if (writeChar != null){
            byte[] tSmsByte = {0, 47, 0, 0, 1, 11, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendFactoryTest(){
        if (writeChar != null){
            byte[] tSmsByte = {0, -26, 0, 0, 1, -51, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendShutDown(){
        if (writeChar != null){
            byte[] tSmsByte = {0, -26, 0, 0, 1, 119, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendPushSwitchData(){



//        K6_DATA_TYPE_CALL_ALARM k6_data_type_call_alarm = new K6_DATA_TYPE_CALL_ALARM(1);
//        k6_data_type_call_alarm.getBytes()
        if (writeChar != null){
            byte[] tSmsByte = {0x00, 0x05, 0x00, 0x00, 0x01, 122, 0x00, 0x00, 0x01, 0x00, (byte) 0x01,0x00, 0x00, 0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            writeChar.setValue(tSmsByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }
    public void sendStartExer(){
        if (writeChar != null){
            byte[] tExerByte = {0x00, 0x01, 0x00, 0x00, 0x01, 0x7a, 0x00, 0x00, 0x0a, 0x00, 0x07, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            writeChar.setValue(tExerByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }
    public void sendGetGsensorData(){
        if (writeChar != null){
            byte[] tExerByte = {0x00, 0x01, 0x00, 0x00, 0x01, 18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            writeChar.setValue(tExerByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendPoweroffData(){
        Log.d("zhou","devMacAddress ="+devMacAddress+" mBluetoothGatt ="+mBluetoothGatt
        + " devConnectState="+devConnectState+"writeChar="+writeChar);
        if (writeChar != null){
            byte[] tExerByte = {0x00, 0x01, 0x00, 0x00, 0x01, 119, 0x00, 0x00, 0x00, 0x00,0x00, 0x00, 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            writeChar.setValue(tExerByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void sendAckData(int ackN, int ackDataType){
        //00 01 00 05 04 06 00 00 01 00 01 00 00 00 00 00 00 00 00 00
        if (writeChar != null){
            byte[] tExerByte = {0x00, 0x01, 0x00, (byte) ackN, 0x04, (byte) ackDataType, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            writeChar.setValue(tExerByte);
            if (mBluetoothGatt != null && devConnectState == 2) {
                mBluetoothGatt.writeCharacteristic(writeChar);
            }
        }
    }

    public void disconnect() {
        if (mBluetoothGatt == null) {
            Log.e("qob", "BluetoothAdapter not initialized");
            return;
        }
        if(writeChar!=null){
            writeChar = null;
        }

        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;


    }

    public boolean connect(BluetoothAdapter btAdapter, Context mContext ,YDCallBack ydCallBack) {
        if (btAdapter == null || devMacAddress == null) {
            Log.e("qob", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        this.mYDCallBack = ydCallBack;
        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
            disconnect();
        }

        BluetoothDevice mBluetoothDevice = btAdapter.getRemoteDevice(devMacAddress);

        if (mBluetoothDevice == null) {
            Log.e("qob", "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        postHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mContext != null && BleSystemUtils.isBlePermission(mContext)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE, PHY_LE_1M_MASK);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                    }
                }
//                mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                if (mBluetoothGatt == null) {
                } else {
                    //刷新缓存
//                    try {
//                        Method refresh = BluetoothGatt.class.getMethod("refresh");
//                        if (refresh != null) {
//                            refresh.setAccessible(true);
//                            refresh.invoke(mBluetoothGatt, new Object[0]);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
            }
        });

        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        /**
         * 连接状态变化时回调
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, final int status, int newState) {

//            Lg.e("外部：" + String.format("%s != %s", gatt == null ? "null" : gatt.getDevice(), mBluetoothGatt == null ? "null" : mBluetoothGatt.getDevice()));
            if (gatt != mBluetoothGatt) {
                String logStr = String.format("%s != %s", gatt == null ? "null" : gatt.getDevice(), mBluetoothGatt == null ? "null" : mBluetoothGatt.getDevice());
                Lg.e(logStr);
                Lg.e("New_First!!!");
//                internalDisConnect();
//                BleFactory.getInstance().getK6Proxy().reConnectDirect();
                return;
            }

            try {

                // status 用于返回操作是否成功,会返回异常码。
                // newState 返回连接状态，如BluetoothProfile#STATE_DISCONNECTED、BluetoothProfile#STATE_CONNECTED
                //操作成功的情况下
                if (status == BluetoothGatt.GATT_SUCCESS){
                    //判断是否连接码
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        devConnectState = 2;
                        if (mYDCallBack != null) {
                            mYDCallBack.connStatusChange();
                        }
                        mConnected = true;
                        try {
                            mBluetoothGatt.requestMtu(512);//修改mtu值
                        } catch (Exception e) {
                            startServicesDiscovery();
                        } catch (Error error) {
                            startServicesDiscovery();
                        }
                    }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                        //判断是否断开连接码
                        // disConnect();
                        //connect(CEBlueSharedPreference.getDevAddress());
                        mConnected = false;
                    }else {
                        mConnected = false;
                    }
                }else{
                    //异常码
                    postHandler.post(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 发现蓝牙服务，在蓝牙连接的时候会调用
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
//            clearMtuHandler();

            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现蓝牙服务成功
                // now, when services discovery is finished, we can call getServices() for Gatt
                getSupportedServices();
                processOldRWCharacteristic(mBluetoothGattServices);
            } else {
            }
        }

        /**
         * 当接收到数据时
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // we got response regarding our request to fetch characteristic value
            if (status == BluetoothGatt.GATT_SUCCESS) {// 接收数据成功时
                // and it success, so we can get the value
                //Log.e("CEBlueTooth_5","data5-接收时间1："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
                getCharacteristicValue(characteristic);
            }
        }

        /**
         * 当开始写入数据时
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //获取写入到外设的特征值
//            Logger.i("characteristic.getValue="+ByteUtil.byte2hex(characteristic.getValue()));
//            Logger.i("currentBytes="+ByteUtil.byte2hex(currentBytes));
            if (status == BluetoothGatt.GATT_SUCCESS) {// 写入数据成功时
//                Log.e("CEBlueTooth_5","data5-写入成功："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
//                dataSendSucceed(currentBytes);
             //   dataSendSucceed(characteristic.getValue());

            } else {
//                Log.e("CEBlueTooth_5","data5-写入失败："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
//                dataSendFailed(currentBytes);
                Logger.i("写入数据时失败");
               // dataSendFailed(characteristic.getValue());
            }
        }

        /**
         * 数据接收会调用 当特征值发生变化时
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            getCharacteristicValue(characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
//            BleConnectTool bleConnectTool = new BleConnectTool();
//            bleConnectTool.pair(gatt.getDevice().getAddress(), false);
        }

        /**
         * 当传输最大数据包大小发生变化时
         */

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
//            clearMtuHandler();
            try {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    MTU = mtu - 3;
                    MTU = (MTU / 20) * 20;
                    if (MTU <= 100) {
                        MTU = 20;
                    }
                    CEProtocolB.MTU = MTU;

                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//            try{
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    if(adapter.isLe2MPhySupported()){
//                        mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_S2);
//                    }
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }

            startServicesDiscovery();
        }

        /**
         * 物理层改变回调
         *
         * @param gatt   gatt
         * @param txPhy  发送速率  1M 2M
         * @param rxPhy  接收速率  1M 2M
         * @param status 更新操作的状态
         */
//        @Override
//        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
//            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
//            Logger.e("txPhy=" + txPhy + " rxPhy=" + rxPhy + " status=" + status);
//        }
    };




//    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            Log.e("qob", "onConnectionStateChange status " + status + " newState " + newState + " mac: " + gatt.getDevice().getAddress());
//
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//
//                Log.i("qob", "Connected to GATT server.");
//                // Attempts to discover services after successful connection.
//            } else{
//                disconnectByService = true;
//                YDBtDeviceM.this.devConnectState = 0;
//                Log.e("qob", "Disconnected from GATT server.");
//                if(mYDCallBack!=null){
//                    mYDCallBack.connStatusChange();
//                }
//            }
//
//
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//
//            Log.e("qob", "connStatusChange status " + status);
//            final YDBtDeviceM tConnDev =   YDBtDeviceM.this;
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (gatt.getServices() == null) {
//                    return;
//                }
//                String tUUID = "0000f618-0000-1000-8000-00805f9b34fb";
//                for (BluetoothGattService gattService : gatt.getServices()) {
//                    // 判断当前服务的UUID是否和预定的服务的UUID相等
//                    Log.e("qob", gattService.getUuid().toString());
//                    if (gattService
//                            .getUuid()
//                            .toString()
//                            .toUpperCase()
//                            .equals(UUID.fromString(tUUID).toString().toUpperCase())) {
//                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService
//                                .getCharacteristics();
//                        // 获取到写的特征
//                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                            final int charaProp = gattCharacteristic.getProperties();
//                            Log.e("qob", "charaProp = " + charaProp);
//                            if (charaProp == 16) {
//                                boolean success = gatt.setCharacteristicNotification(gattCharacteristic, true);
//                                if (!success) {
//                                    Log.e("------", "Setting proper notification status for characteristic failed!");
//                                }
//                                // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
//                                // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
//                                BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//                                if (descriptor != null) {
//                                    byte[] val = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
//                                    descriptor.setValue(val);
//                                    gatt.writeDescriptor(descriptor);
//                                    tConnDev.writeChar = gattCharacteristic;
//                                }
//                            } else {
//                                Log.e("qob", "发现的写特征：" + gattCharacteristic.getUuid().toString());
//                                tConnDev.writeChar = gattCharacteristic;
////                                mSendDelayHandler.postDelayed(new Runnable() {
////                                    @Override
////                                    public void run() {
////                                        tConnDev.sendEnterBackgroupData();
////                                    }
////                                }, 200);
//                            }
//                        }
//
//                    }
//                }
//
//            } else {
//                Log.e("qob", "connStatusChange received: " + status);
//            }
//            tConnDev.devConnectState = 2;
////            runOnUiThread(new Runnable() {
////                @Override
////                public void run() {
////                    mDeviceAdapter.notifyDataSetChanged();
////                    hidProgress();
////                }
////            });
//            if(mYDCallBack!=null){
//                mYDCallBack.connStatusChange();
//            }
//
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                BluetoothGattCharacteristic characteristic,
//                int status) {
//            Log.e("qob", "onCharacteristicRead");
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//
//            }
//        }
//
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,
//                BluetoothGattCharacteristic characteristic) {
//
//            YDBtDeviceM tConnDev = YDBtDeviceM.this;
//
////            if (characteristic.getValue().length > 0) {
////                byte[] tRecvBtye = characteristic.getValue();
////                int tN = tRecvBtye[3] & 0xff;
////                int tCmdType = tRecvBtye[4] & 0xff;
////                int tDataType = tRecvBtye[5] & 0xff;
////                if (tCmdType == 4){ //ack
////                    if (tDataType == 0x6d){ //前后台开关回复
////                        tConnDev.sendSmsNtf();
////                    }
////                }
////                else {
////                    if (tDataType == 18){
////                        int tX = (tRecvBtye[10] & 0xff) + ((tRecvBtye[11] & 0xff) << 8);
////                        int tY = (tRecvBtye[12] & 0xff) + ((tRecvBtye[13] & 0xff) << 8);
////                        int tC = (tRecvBtye[14] & 0xff) + ((tRecvBtye[15] & 0xff) << 8);
////                        tConnDev.devGsensorX = tX;
////                        tConnDev.devGsensorY = tY;
////                        tConnDev.devGsensorZ = tC;
////
//////                        runOnUiThread(new Runnable() {
//////                            @Override
//////                            public void run() {
//////                                mDeviceAdapter.notifyDataSetChanged();
//////                            }
//////                        });
////
////                        if(mYDCallBack!=null){
////                            mYDCallBack.onCharacteristicChanged();
////                        }
////                    }
////                    tConnDev.sendAckData(tN, tDataType);
////                }
////            }
//        }
//
//        @Override
//        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
////            byte[] tWriteBtye = characteristic.getValue();
////            int tIndex = tWriteBtye[0] & 0xff;
////            int tDataType = tWriteBtye[5] & 0xff;
////            if (tDataType == 0x6b && tIndex == 0){
////                YDBtDeviceM tConnDev =  YDBtDeviceM.this;
////                if (tConnDev != null) {
////                    tConnDev.sendPushSwitchData();
////                }
////            }
////            else if (tDataType == 0x38 && tIndex < 3){
////                YDBtDeviceM tConnDev = bleDevice(gatt.getDevice().getAddress());
////                if (tConnDev != null) {
////                    tConnDev.sendSmsNtf(tIndex);
////                }
////            }
//        }
//
//        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
//            Log.e("qob", "onDescriptorWrite status " + status);
//            YDBtDeviceM tConnDev =  YDBtDeviceM.this;
//            if (tConnDev != null) {
//                tConnDev.sendTestDevData();
//            }
//        }
//    };
    YDCallBack mYDCallBack;

    public void setYDCallBack(YDCallBack YDCallBack) {
        mYDCallBack = YDCallBack;
    }

    public interface YDCallBack{
        void onCharacteristicChanged();
        void connStatusChange();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YDBtDeviceM that = (YDBtDeviceM) o;
        return Objects.equals(devMacAddress, that.devMacAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(devMacAddress);
    }


    public void startServicesDiscovery() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.discoverServices();
        } else {
        }
    }

    public void getSupportedServices() {
        if (mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) {
            mBluetoothGattServices.clear();
        }
        // keep reference to all services in local array:
        if (mBluetoothGatt != null) {
            mBluetoothGattServices = mBluetoothGatt.getServices();
        }
    }

    private void processOldRWCharacteristic(List<BluetoothGattService> gattServices) {
        writeCharas.clear();
        if (gattServices == null) {
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            // 判断当前服务的UUID是否和预定的服务的UUID相等
            Logger.e("aaa","A1="+gattService
                    .getUuid()
                    .toString()
                    .toUpperCase());

            Logger.e("aaa","A2="+UUID.fromString(Uuid).toString().toUpperCase());
            if (gattService
                    .getUuid()
                    .toString()
                    .toUpperCase()
                    .equals(UUID.fromString(Uuid).toString().toUpperCase())) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                        .getCharacteristics();
                // 获取到写的特征
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    final int charaProp = gattCharacteristic.getProperties();
                    if (charaProp == 16) {
                        setNotificationForCharacteristic(
                                gattCharacteristic, true);
                        readCharas.add(gattCharacteristic);
                    } else {
                        if (!writeCharas.contains(gattCharacteristic)) {
                            writeCharas.add(gattCharacteristic);
                            writeChar = gattCharacteristic;
                        }
                    }
                }
            }
        }
    }

    /* 为characteristic启用/禁用通知 */
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothGatt == null) {
            return;
        }

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if (!success) {
        }
        // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothGatt == null || ch == null) {
            return;
        }
        // lets read and do real parsing of some characteristic to get meaningful value from it
//        UUID uuid = ch.getUuid();
//        Lg.e("Received data5: " + receivedData);

        try {
//            Log.e("CEBlueTooth_5", "Rec data5-check: " + CEBC.byte2hex(ch.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            if(BuildConfig.DEBUG){
//                if ((rawValue[5] & 0xff) != CEBC.K6.DATA_TYPE_WATCH_FACE_SYNC) {//过滤同步表盘数据
//                    FileLogPrint.printSendData(CEBC.byte2hex(rawValue),true);
//                }
//            }
//        } catch (Exception e) {
//        } catch (Error error) {
//        }
    }
}
