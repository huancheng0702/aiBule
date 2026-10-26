package com.example.sdkdemo;

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
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_StressStruct;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_MOTION_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_GSENSOR_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_RRI_HRV_DATA;
import ce.com.cenewbluesdk.proxy.interfaces.IK6AnalysiDevRcvDataManager;
import ce.com.cenewbluesdk.proxy.interfaces.IK6SendDataManager;
import ce.com.cenewbluesdk.proxy.interfaces.K6BleDataResult;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Lg;

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
    public static MutableLiveData<K6_DevInfoStruct> devInfoStructLive = new MutableLiveData<>();
    public static MutableLiveData<List<K6_HeartStruct>> heartRate = new MutableLiveData<>();
    public static MutableLiveData<K6_DATA_TYPE_HR_CONTROL> disposalStatus = new MutableLiveData<>();
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
    public static MutableLiveData<List<Integer>> heartPpgLiveData = new MutableLiveData<>();
    public static MutableLiveData<k6_DATA_TYPE_LEAK_LIGHT_TEST_DATA> leakLightTestLiveData = new MutableLiveData<>();
    private ArrayList<K6_Sport> sportList = new ArrayList<>();
    public static MutableLiveData<List<k6_RRI_HRV_DATA>> hrvLiveData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_StressStruct>> stressLiveData = new MutableLiveData<>();
    public static MutableLiveData<List<K6_TempStruct>> tempLiveData = new MutableLiveData<>();
    public static MutableLiveData<Integer> hidLiveData = new MutableLiveData<>();
    public static MutableLiveData<List<k6_GSENSOR_DATA>> gSonerDataLiveData = new MutableLiveData<>();
    public static DataCallBack dataCallBack;

    public BluetoothManager(@NonNull Application application) {
        super(application);
    }

    public void registerBLE() {
        setBlueToothDataReceiveListener(BluetoothHelper.getInstance().getRcvDataManager());
    }

    private void setBlueToothDataReceiveListener(IK6AnalysiDevRcvDataManager k6AnalysiDevManager) {
        //Bluetooth connection status （蓝牙连接状态)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BLUE_CONNECT_STATE_CHANGE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer status) {
                connectStatus.postValue(status);
                return false;
            }
        });
        //OTA progress (ota进度)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_OTA_PROGRESS, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer progress) {
                otaFileProgress.postValue(progress);
                return false;
            }
        });

        //Get automatic detection status (获取自动检测状态)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_HEART_AUTO_SWITCH, new K6BleDataResult<K6_DATA_TYPE_HEART_AUTO_SWITCH>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_HEART_AUTO_SWITCH result) {
                heartAutoSwitch.postValue(result);
                return false;
            }
        });

        //Manual heart rate monitoring will record your heart rate during exercise.
        // 手动心率检测，锻炼时的心率都会收到
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HEART_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HeartStruct>>) k6_heartStructs -> {
                    heartRate.postValue(k6_heartStructs);
                    return false;
                });

        //自动心率监测数据
        //Automatic heart rate monitoring data
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DAILY_HEART, new K6BleDataResult<ArrayList<K6_HeartStruct>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_HeartStruct> results) {
                heartRate.postValue(results);
                return false;
            }
        });

        //Release status detection (脱手状态检测)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_HR_CONTROL,
                new K6BleDataResult<K6_DATA_TYPE_HR_CONTROL>() {
                    @Override
                    public boolean bleDataResult(K6_DATA_TYPE_HR_CONTROL control) {

                        disposalStatus.postValue(control);
                        return false;
                    }
                });


        //Exercise data: steps, distance, calories
        //运动数据 步数、距离、卡路里
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_DATA, new K6BleDataResult<ArrayList<K6_Sport>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_Sport> k6_sports) {
                sportData.postValue(k6_sports);
                sportList.addAll(k6_sports);
                return false;
            }
        });

        //Sleep data (睡眠数据)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_SLEEP_DATA, new K6BleDataResult<K6_sleepData>() {
            @Override
            public boolean bleDataResult(K6_sleepData k6_sleepData) {
                sleepData.postValue(k6_sleepData);
                return false;
            }
        });

        //Blood oxygen data (received during manual measurement) (血氧数据(手动测量时接收))
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> result) {
                bloodOxygenData.postValue(result);
                return false;
            }
        });


        //Blood oxygen data (received during automatic monitoring) (血氧数据(自动监测时接收))
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2_HISTORY, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> result) {
                bloodOxygenData.postValue(result);
                return false;
            }
        });

        //(Receive manual and automatic HRV(RRI) data) (接收手动、自动 HRV(RRI)数据)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_RRI_HRV,
                (K6BleDataResult<ArrayList<k6_RRI_HRV_DATA>>) k6RriHrvData -> {
                    hrvLiveData.postValue(k6RriHrvData);
                    return false;

                });
        //Receive manual and automatic pressure data (接收手动、自动 压力数据)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_STRESS_SHOW,
                (K6BleDataResult<ArrayList<K6_StressStruct>>) k6StressStructs -> {
                    stressLiveData.postValue(k6StressStructs);
                    return false;
                });

        //Blood pressure data generally does not support （血压数据一般不支持）
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_BP, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_BP>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_BP> result) {
                bloodPressureData.postValue(result);
                return false;
            }
        });

        //Shake to take a photo (摇一摇拍照)
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

        //Received lost phone (收到找手机)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_FIND_PHONE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                Log.i(TAG, "Find My Phone");
                return false;
            }
        });
        //Equipment Information (设备信息)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DEVINFO, new K6BleDataResult<K6_DevInfoStruct>() {
            @Override
            public boolean bleDataResult(K6_DevInfoStruct devInfoStruct) {
                Log.i(TAG, "Equipment information");
                equipmentInfo.postValue(devInfoStruct);
                Constant.customId = devInfoStruct.getCustomer_id() + "";
                devInfoStructLive.postValue(devInfoStruct);
                return false;
            }
        });
        //Compliance reminder （达标提醒）
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_TYPE_TARGET_ALARM, new K6BleDataResult<K6_DATA_TYPE_TARGET_ALARM>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_TARGET_ALARM data) {
                targetAlarmLiveData.postValue(data);
                return false;
            }
        });
        //Drinking water reminder （喝水提醒）
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_DATA_TYPE_DRINK_ALARM, new K6BleDataResult<K6_DATA_TYPE_DRINK_ALARM>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_DRINK_ALARM data) {
                drinkAlarmLiveData.postValue(data);
                return false;
            }
        });
        //prolonged sitting (久坐)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SITTING_REMIND, new K6BleDataResult<K6_SittingRemind>() {
            @Override
            public boolean bleDataResult(K6_SittingRemind data) {
                data.getSwitch_flag();//开启状态
                data.getRepeat();//重复周期
                sittingLiveData.postValue(data);
                return false;
            }
        });
        //Do not disturb (勿扰)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_NO_DISTURB, new K6BleDataResult<K6NoDisturbSummary>() {
            @Override
            public boolean bleDataResult(K6NoDisturbSummary k6NoDisturbSummary) {
                k6NoDisturbSummary.getSwit();//开启状态
                k6NoDisturbSummary.getList();//勿扰时间，一般只有一个
                noDisturbLiveData.postValue(k6NoDisturbSummary);
                return false;
            }
        });
        //Raise your wrist (抬腕)
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
        //Device supported functions （设备支持的功能项）
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


        //Volume control operation
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_PHONE_CONTROL, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer integer) {
                if (integer == CEBC.K6.CALL_CMD_SOUND_OFF) {  //控制静音
                } else if (integer == CEBC.K6.CALL_CMD_HANGUP_CALL) {//挂断操作
                }
                return false;
            }
        });


        //Battery Information (电池信息)
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

        //HR PPG
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_HEART_PPG, new K6BleDataResult<List<Integer>>() {
            @Override
            public boolean bleDataResult(List<Integer> payload) {
                heartPpgLiveData.postValue(payload);
                return false;
            }
        });



        //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
        //接收设备手势开关状态 0：表示设备hid为关闭状态 大于0表示设备hid为开启状态
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GESTURE_DATA,
                (K6BleDataResult<K6_DATA_TYPE_GESTURE_CONFIG>) gesture_config -> {
                    int GESTURE_OFF = gesture_config.getControlType();
                    hidLiveData.postValue(GESTURE_OFF);
                    return false;
                });


        //Motion-sensing game XYZ axis information: First, turn on the XYZ axis switch; turn it off when not in use.
        // 体感游戏XYZ轴信息 先要打开xyz轴开关 不用时关闭
        //xyz轴开关
        //BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(CEBC.OPENSTATUS.CLOSE);
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_MOTION_DATA,
                (K6BleDataResult<k6_DATA_TYPE_MOTION_DATA>) motion_data -> {
                    int X = motion_data.getX_Move_Distance();
                    int Y = motion_data.getY_Move_Distance();
                    int Z = motion_data.getSpeed_Throw();
                    return false;
                });

        //Receive temperature data (接收温度数据)
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_TEMP_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_TempStruct>>) tempStructs -> {
                    tempLiveData.postValue(tempStructs);
                    return false;
                });

        //GSENSOR PPG
        k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GSENSOR_RAW,
                (K6BleDataResult<List<k6_GSENSOR_DATA>>) datas -> {
                    gSonerDataLiveData.postValue(datas);
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
