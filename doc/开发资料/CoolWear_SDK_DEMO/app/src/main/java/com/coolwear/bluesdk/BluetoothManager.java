package com.coolwear.bluesdk;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.BluetoothData;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.k6.K6NoDisturbSummary;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_CONTACT_SYNC;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_DRINK_ALARM;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_FUNCTION_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_GESTURE_CONFIG;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HAND_RISE_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HEART_AUTO_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HR_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_BP;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_TARGET_ALARM;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_WATCH_FACE_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_WATCH_FACE_START;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_Mix_sport_Struct;
import ce.com.cenewbluesdk.entity.k6.K6_SEND_APP_SPORT_STRUCT;
import ce.com.cenewbluesdk.entity.k6.K6_SittingRemind;
import ce.com.cenewbluesdk.entity.k6.K6_Sleep;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_MOTION_DATA;
import ce.com.cenewbluesdk.proxy.interfaces.IK6AnalysiDevRcvDataManager;
import ce.com.cenewbluesdk.proxy.interfaces.IK6SendDataManager;
import ce.com.cenewbluesdk.proxy.interfaces.K6BleDataResult;
import ce.com.cenewbluesdk.proxy.k6AnalysisDevData.AnalysisReceiveDataHelper.BlueProcessBean.ProcessDATA_TYPE_SlEEP_MONITOR;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Lg;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/22 0022 10:18
 */
public class BluetoothManager extends AndroidViewModel {
    private static final String TAG = "BluetoothManager";
    public static MutableLiveData<Integer> connectStatus = new MutableLiveData<>();
    public static MutableLiveData<Boolean> supportAccelerateTransmit = new MutableLiveData<>();
    public static MutableLiveData<Integer> progress = new MutableLiveData<>();
    public static MutableLiveData<Integer> otaFileProgress = new MutableLiveData<>();
    public static MutableLiveData<List<K6_HeartStruct>> heartRate = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_HR_CONTROL> heartRate1 = new MutableLiveData<>();
    public static MutableLiveData<List<K6_HeartStruct>> heartRate2 = new MutableLiveData<>();
    public static MutableLiveData<List<K6_HeartStruct>> heartRate3 = new MutableLiveData<>();
    public static MutableLiveData<List<K6_Sport>> sportData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_Mix_sport_Struct>> mixSportData = new MutableLiveData<>();
    public static MutableLiveData<K6_sleepData> sleepData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_DATA_TYPE_REAL_O2>> bloodOxygenData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_DATA_TYPE_REAL_BP>> bloodPressureData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_WATCH_FACE_INFO> watchFaceInfo = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_HEART_AUTO_SWITCH> heartAutoSwitch = new MutableLiveData<>();
    public static MutableLiveData<K6_DevInfoStruct> equipmentInfo = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_CONTACT_SYNC> contactLiveData = new MutableLiveData<>();
    public static MutableLiveData<Boolean> delContactLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_TARGET_ALARM> targetAlarmLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_DRINK_ALARM> drinkAlarmLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_SittingRemind> sittingLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6NoDisturbSummary> noDisturbLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_HAND_RISE_SWITCH> brightScreenLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_FUNCTION_CONTROL> controlLiveData = new MutableLiveData<>();
    public static MutableLiveData<k6_DATA_TYPE_MOTION_DATA> gSensorLiveData = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_BATTERY_INFO> batteryLiveData = new MutableLiveData<>();
    public static MutableLiveData<Integer> pairFinishLiveData = new MutableLiveData<>();
    public static MutableLiveData<Integer> sleepMonitorLiveData = new MutableLiveData<>();
    public static MutableLiveData<k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA> leakLightTestLiveData = new MutableLiveData<>();
    private ArrayList<K6_Sport> sportList = new ArrayList<>();
    public static MutableLiveData<List<K6_HrvStruct>> hrvLiveData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_TempStruct>> tempLiveData = new MutableLiveData<>();
    public static MutableLiveData<Integer> hidLiveData = new MutableLiveData<>();
    public static DataCallBack dataCallBack;

    public BluetoothManager(@NonNull Application application) {
        super(application);
    }

    public void initBLE() {
        setBlueToothDataReceiveListener(BluetoothHelper.getInstance().getRcvDataManager());
        setBlueToothDataSendResultListener(BluetoothHelper.getInstance().getSendDataManager());
    }


    private void setBlueToothDataSendResultListener(IK6SendDataManager sendHelper) {
        sendHelper.addSendDataResultListener(K6_Action.SEND_R.SEND_DATA_TYPE_CONTACT_DELETE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                if (integer == 1) {
                    //删除成功
                    delContactLiveData.postValue(true);
                } else {
                    delContactLiveData.postValue(false);
                }
                return false;
            }
        });
    }

    private void setBlueToothDataReceiveListener(IK6AnalysiDevRcvDataManager k6AnalysiDevManager) {
        //蓝牙连接状态
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BLUE_CONNECT_STATE_CHANGE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer status) {
                if (status == K6_Action.RCVD.BLUE_CONNECTED) {
//                    BluetoothHelper.getInstance().startPairSynData();//配对及同步，此方法不会再走配对完成回调
                    BluetoothHelper.getInstance().devicePairFinish(1);//配对，此方法配对完成后，会在底层自己完成数据同步
                }
                Logger.e("aaa","接收到连接状态 =" + status);
                connectStatus.postValue(status);
                return false;
            }
        });
        //配对完成
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_PAIR_FINISH, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer k6_pairStruct) {
                Log.i(TAG, "RCVD_PAIR_FINISH");
//                BluetoothHelper.getInstance().synDevData();//同步数据
                pairFinishLiveData.postValue(k6_pairStruct);
                return false;
            }
        });
        //表盘信息
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_WATCH_FACE_INFO, new K6BleDataResult<K6_DATA_TYPE_WATCH_FACE_INFO>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_WATCH_FACE_INFO result) {
                watchFaceInfo.postValue(result);
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplication(), result.toString(), Toast.LENGTH_LONG).show();
//                    }
//                });
                return false;
            }
        });
        //开始传表盘，是否支持高速传输
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_WATCH_FACE_START, new K6BleDataResult<K6_DATA_TYPE_WATCH_FACE_START>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_WATCH_FACE_START k6_data_type_watch_face_start) {
                if (!k6_data_type_watch_face_start.isTimeOut()) {
                    supportAccelerateTransmit.postValue(true);
                } else {
                    supportAccelerateTransmit.postValue(false);
                }
                return false;
            }
        });
        //表盘进度
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BLE_FILE_DOWNLOAD_PROGRESS, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                progress.postValue(integer);
                return false;
            }
        });
        //ota进度
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_OTA_PROGRESS, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer progress) {
                otaFileProgress.postValue(progress);
                return false;
            }
        });

        //获取自动检测状态
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_HEART_AUTO_SWITCH, new K6BleDataResult<K6_DATA_TYPE_HEART_AUTO_SWITCH>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_HEART_AUTO_SWITCH result) {
                heartAutoSwitch.postValue(result);
                return false;
            }
        });
//        k6AnalysiDevManager.addBleDataResultListner(K6_Action.RCVD.RCVD_SPORT_HEART_FOR_SHOW, (K6BleDataResult<ArrayList<K6_HeartStruct>>) k6_heartStructs -> {
//            Logger.d(TAG, "RCVD_SPORT_HEART_FOR_SHOW heart rate = $it");
//            return false;
//        });
        //心率数据，心率检测，锻炼时的心率都会收到
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HEART_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HeartStruct>>) k6_heartStructs -> {
                    heartRate.postValue(k6_heartStructs);
                    return false;
                });
        //自动心率检测数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DAILY_HEART, new K6BleDataResult<ArrayList<K6_HeartStruct>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_HeartStruct> results) {
                heartRate2.postValue(results);
                Logger.e("eeee", "hr=" + results);
                return false;
            }
        });
        //开启锻炼时会收到心率数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HEART, new K6BleDataResult<ArrayList<K6_HeartStruct>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_HeartStruct> results) {
                heartRate3.postValue(results);
                if (dataCallBack != null) {
                    dataCallBack.updateHeartRate(results);
                }
                return false;
            }
        });
        //运动数据 戒指晃动时就有数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_DATA, new K6BleDataResult<ArrayList<K6_Sport>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_Sport> k6_sports) {
                sportData.postValue(k6_sports);
                sportList.addAll(k6_sports);
                return false;
            }
        });
        //锻炼时的混杂运动数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_MIX_SPORT_DATA,
                new K6BleDataResult<ArrayList<K6_Mix_sport_Struct>>() {
                    @Override
                    public boolean bleDataResult(ArrayList<K6_Mix_sport_Struct> k6_mix_sport_structs) {
                        mixSportData.postValue(k6_mix_sport_structs);
                        return false;
                    }
                });
//        //睡眠数据
//        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SLEEP_DATA, new K6BleDataResult<ArrayList<K6_Sleep>>() {
//            @Override
//            public boolean bleDataResult(ArrayList<K6_Sleep> k6_sleeps) {
//                return false;
//            }
//        });
        //睡眠数据,二次封装
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_SLEEP_DATA, new K6BleDataResult<K6_sleepData>() {
            @Override
            public boolean bleDataResult(K6_sleepData k6_sleepData) {
                sleepData.postValue(k6_sleepData);
                Logger.e("eeee", "s=" + sleepData);
                return false;
            }
        });

        //血氧数据(手动测量时接收)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> result) {
                bloodOxygenData.postValue(result);
                return false;
            }
        });


        //血氧数据(自动测量时接收)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2_HISTORY, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> result) {
                bloodOxygenData.postValue(result);
                Logger.e("eeee", "o2=" + result);
                return false;
            }
        });
        //血压数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_BP, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_BP>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_BP> result) {
                bloodPressureData.postValue(result);
                return false;
            }
        });
        //检测指令类型
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_HR_CONTROL, new K6BleDataResult<K6_DATA_TYPE_HR_CONTROL>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_HR_CONTROL result) {
                heartRate1.postValue(result);
                return false;
            }
        });
        //收到拍照指令
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_TAKE_PHOTOS, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                if (integer == CEBC.OPENSTATUS.OPEN) {
                    Log.i(TAG, "open/Take photo");
                } else {
                    Log.i(TAG, "close");
                }
                return false;
            }
        });
        //收到找手机
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_FIND_PHONE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                Log.i(TAG, "Find My Phone");
                return false;
            }
        });
        //设备信息
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DEVINFO, new K6BleDataResult<K6_DevInfoStruct>() {
            @Override
            public boolean bleDataResult(K6_DevInfoStruct devInfoStruct) {
                Log.i(TAG, "Equipment information");
                equipmentInfo.postValue(devInfoStruct);
                Constant.customId = devInfoStruct.getCustomer_id() + "";
                return false;
            }
        });
        //联系人
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_CONTACT_SYNC_TYPE, new K6BleDataResult<K6_DATA_TYPE_CONTACT_SYNC>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_CONTACT_SYNC contacts) {
                if (contacts.getPosition() > 0) {
                    contactLiveData.postValue(contacts);
                } else {
                    contactLiveData.postValue(null);
                }
                return false;
            }
        });
        //达标提醒
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_TYPE_TARGET_ALARM, new K6BleDataResult<K6_DATA_TYPE_TARGET_ALARM>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_TARGET_ALARM data) {
                targetAlarmLiveData.postValue(data);
                return false;
            }
        });
        //喝水提醒
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_DRINK_ALARM, new K6BleDataResult<K6_DATA_TYPE_DRINK_ALARM>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_DRINK_ALARM data) {
                drinkAlarmLiveData.postValue(data);
                return false;
            }
        });
        //久坐
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SITTING_REMIND, new K6BleDataResult<K6_SittingRemind>() {
            @Override
            public boolean bleDataResult(K6_SittingRemind data) {
                data.getSwitch_flag();//开启状态
                data.getRepeat();//重复周期
                sittingLiveData.postValue(data);
                return false;
            }
        });
        //勿扰
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_NO_DISTURB, new K6BleDataResult<K6NoDisturbSummary>() {
            @Override
            public boolean bleDataResult(K6NoDisturbSummary k6NoDisturbSummary) {
                k6NoDisturbSummary.getSwit();//开启状态
                k6NoDisturbSummary.getList();//勿扰时间，一般只有一个
                noDisturbLiveData.postValue(k6NoDisturbSummary);
                return false;
            }
        });
        //抬腕
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_HAND_RISE_SWITCH, new K6BleDataResult<K6_DATA_TYPE_HAND_RISE_SWITCH>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_HAND_RISE_SWITCH data) {
                data.getOnoff();//开启状态
                data.getStart_hour();
                data.getStart_min();
                data.getEnd_hour();
                data.getEnd_min();
                brightScreenLiveData.postValue(data);
                return false;
            }
        });
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_FUNCTION_CONTROL, new K6BleDataResult<K6_DATA_TYPE_FUNCTION_CONTROL>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_FUNCTION_CONTROL data) {
                data.isHasContacts();//通讯录
                data.isHasO2();//血氧
                data.isHasBp();//血压
                data.isHasWomanStage();//女性生理周期功能
                data.isBtedr_call();//通话蓝牙功能
                data.isHasWeatherNow();//实时天气功能
                data.isHr_measure_button();
                data.isHasGame();//体感游戏
                data.isHasHrvSupported();//支持HRV
                data.isHasGestureSupported();//手势
                data.isO2AutoSwitch();//自动血氧开关是否与心率开关分开
                controlLiveData.postValue(data);
                return false;
            }
        });
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_APP_SPORT, new K6BleDataResult<K6_SEND_APP_SPORT_STRUCT>() {
            @Override
            public boolean bleDataResult(K6_SEND_APP_SPORT_STRUCT k6_send_app_sport_struct) {
                return false;
            }
        });
        //经典蓝牙地址及蓝牙名
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_BTEDR_ADDR, new K6BleDataResult<BluetoothData>() {
            @Override
            public boolean bleDataResult(BluetoothData bluetoothData) {
                Log.i(Lg.getClassName(BluetoothManager.class), "Address:" + bluetoothData.getBleAddress() + " Name:" + bluetoothData.getBleName());
                return false;
            }
        });
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_PHONE_CONTROL, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                if (integer == CEBC.K6.CALL_CMD_SOUND_OFF) {  //控制静音
                } else if (integer == CEBC.K6.CALL_CMD_HANGUP_CALL) {//挂断操作
                }
                return false;
            }
        });
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_MOTION_DATA, new K6BleDataResult<k6_DATA_TYPE_MOTION_DATA>() {
            @Override
            public boolean bleDataResult(k6_DATA_TYPE_MOTION_DATA data) {
                saveData(data);
                gSensorLiveData.postValue(data);
                if (dataCallBack != null) {
                    dataCallBack.updateMotion(data);
                }
                return false;
            }
        });

        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BATTERY, new K6BleDataResult<K6_DATA_TYPE_BATTERY_INFO>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_BATTERY_INFO battery_info) {
                batteryLiveData.postValue(battery_info);
                if (dataCallBack != null) {
                    dataCallBack.updateBattery(battery_info.getBattery());
                }
                return false;
            }
        });

        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_SLEEP_MONITOR, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                sleepMonitorLiveData.postValue(0);
                return false;
            }
        });

        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_LEAK_LIGHT_TEST, new K6BleDataResult<k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA>() {
            @Override
            public boolean bleDataResult(k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA data) {
                leakLightTestLiveData.postValue(data);
                return false;
            }
        });

        //心率数据，心率检测，锻炼时的心率都会收到
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HEART_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HeartStruct>>) k6_heartStructs -> {
                    heartRate.postValue(k6_heartStructs);
                    return false;
                });


        //接收手动，自动HRV数据    压力是HRV数据通过算法计算
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HRV_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HrvStruct>>) k6_heartStructs -> {
                    hrvLiveData.postValue(k6_heartStructs);
                    return false;
                });

        //接收设备手势开关状态 0：表示设备hid为关闭状态 大于0表示设备hid为开启状态
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GESTURE_DATA,
                (K6BleDataResult<K6_DATA_TYPE_GESTURE_CONFIG>) gesture_config -> {
                    int GESTURE_OFF = gesture_config.getControlType();
                    hidLiveData.postValue(GESTURE_OFF);
                    return false;
                });


        //体感游戏XYZ轴信息 先要打开xyz轴开关 不用时关闭
        //xyz轴开关
        //BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(CEBC.OPENSTATUS.CLOSE);
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_MOTION_DATA,
                (K6BleDataResult<k6_DATA_TYPE_MOTION_DATA>) motion_data -> {
                    int X = motion_data.getX_Move_Distance();
                    int Y = motion_data.getY_Move_Distance();
                    int Z = motion_data.getSpeed_Throw();
                    return false;
                });

        //接收体温数据
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_TEMP_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_TempStruct>>) tempStructs -> {
                    tempLiveData.postValue(tempStructs);
                    return false;
                });
    }

    private synchronized void saveData(k6_DATA_TYPE_MOTION_DATA data) {
        int len = data.getBytes().length / 6;
        for (int i = 0; i < len; i++) {
            k6_DATA_TYPE_MOTION_DATA dataTypeMotionData = new k6_DATA_TYPE_MOTION_DATA();
            dataTypeMotionData.setSpeed_Throw(ByteUtil.byte2ToInt2(new byte[]{data.getBytes()[0 + (i * 6)], data.getBytes()[1 + (i * 6)]}));
            dataTypeMotionData.setX_Move_Distance(ByteUtil.byte2ToInt2(new byte[]{data.getBytes()[2 + (i * 6)], data.getBytes()[3 + (i * 6)]}));
            dataTypeMotionData.setY_Move_Distance(ByteUtil.byte2ToInt2(new byte[]{data.getBytes()[4 + (i * 6)], data.getBytes()[5 + (i * 6)]}));
            // Lg.e_0316("BluetoothManager", dataTypeMotionData.getSpeed_Throw() + "," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance(), "GsenSor.log", true);
            // Logger.i(TAG, dataTypeMotionData.getSpeed_Throw() + "," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance());
        }
    }

    public interface DataCallBack {
        void updateMotion(k6_DATA_TYPE_MOTION_DATA data);

        void updateHeartRate(List<K6_HeartStruct> result);

        void updateBattery(Integer integer);
    }
}
