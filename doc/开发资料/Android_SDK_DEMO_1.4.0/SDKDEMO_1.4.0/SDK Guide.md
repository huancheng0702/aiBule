#### 				            SDK User Guide（SDK使用指南）						



- 1.compilation environment(编译环境)

```
Android Studio (2025 and above)
agp = "8.10.1" above
```

- 2.Initialization in Application (初始化  在Application中)


```java
BluetoothHelper.getInstance().init();
BluetoothHelper.getInstance().initProxy(this);


在 AndroidManifest.xml 中至少加入：
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

- 3.Bluetooth scanning(蓝牙扫描)


```java
BluetoothHelper.getInstance().startScan();//Start scanning
BluetoothHelper.getInstance().stopScan();//Stop scanning
BluetoothHelper.getInstance().scanningDeviceInit(this, scanDevListener);//Scan results
```

- 4.Connecting devices(连接设备)


```java
BluetoothHelper.getInstance().connectDev(devMac, "");
```

- 5.Reconnect(重连)


```java
BluetoothHelper.getInstance().reConnectDirect()
```

- 6.Disconnect device(断开设备)


```java
BluetoothHelper.getInstance().disConnect();
```

- 7.Unbinding device(解绑设备)


```java
BluetoothHelper.getInstance().unbindDevice();
```

- 8.Synchronized information such as: mobile phone time(同步信息如：手机时间)

```
BluetoothHelper.getInstance().getSendDataManager().sendAsynInfo();
```

- 9.Configure whether the device transmits data(设置设备是否传输数据)

```
BluetoothHelper.getInstance().setEnableGsDataTrans(true);
```



- 10.Registered data receiver(注册数据接收器)

```
BluetoothManager bluetoothManager = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(BluetoothManager.class);
bluetoothManager.registerBLE();
```

##### 

- 11.Heart rate measurement instructions(心率测量指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.OPEN);//start
BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.CLOSE);//stop
```

- 12.Blood oxygen measurement instructions(血氧测量指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.OPEN);//start
BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.CLOSE);//stop
```

- 13.HRV(rri) measurement command(HRV(rri)测量指令）

```java
BluetoothHelper.getInstance().getSendBlueData().sendRriHrvCmd(CEBC.OPENSTATUS.OPEN);//start
BluetoothHelper.getInstance().getSendBlueData().sendRriHrvCmd(CEBC.OPENSTATUS.CLOSE);//stop
```

- 14.Pressure measurement command(压力测量指令）

```java
BluetoothHelper.getInstance().getSendBlueData().sendStressSwitch(CEBC.OPENSTATUS.OPEN);//start
BluetoothHelper.getInstance().getSendBlueData().sendStressSwitch(CEBC.OPENSTATUS.CLOSE);//stop
```

- 15.Automatic monitoring frequency setting command(自动监测频率设置指令)


```java
//parameter1 Heart rate switch; parameter2 Blood oxygen switch; parameter3 Rate interval (minutes) 5-minute interval  eg:5、10、15...
BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(CEBC.OPENSTATUS.OPEN, CEBC.OPENSTATUS.OPEN, 60);

```

- 16.app exercise start command(app运动开始指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
        K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_START, 0, 0);
```

- 17.app exercise continue command(app运动继续指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
        K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_PAUSE, 0, 0);
```

- 18.app exercise pause command(app运动暂停指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
        K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_CONTINUE, 0, 0);
```

- 19.app motion stop command(app运动停止指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
        K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
```

- 20.Shake to take a photo command(摇一摇拍照指令)


```java
//true on; false off
BluetoothHelper.getInstance().getSendBlueData().sendPhotoSwitch(true);
```

- 21.Send notification message(发送通知消息)

```java
BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_MSG, "This is a SMS message");
```

- 22.Incoming call notification(来电消息通知)

```java
//来电消息
BleFactory.getInstance().getK6Proxy().getSendHelper().sendMessage_notice(TimeUtil.now(), name, phoneNumber, K6_MessageNoticeStruct.TYPE_PHONE); 

//接听电话
BluetoothHelper.getInstance().getSendBlueData().sendCall(CEBC.CALLTYPE.ANSWERPHONE);

//挂断电话
 BluetoothHelper.getInstance().getSendBlueData().sendCall(CEBC.CALLTYPE.HANGUPPHONE);

```

- 23.Sedentary reminder(久坐提醒)

```java
/**
* @param repeat Sunday to Saturday 
\\bit0->Sunday,bit1->monday,bit6->saturday.
*/
BluetoothHelper.getInstance().getSendBlueData().sendSittingRemind(start_h, start_m, end_h, end_m, repeat, sedentarySwitch, 1);


repeat rule
if (id == R.id.cb_sunday) {
            position = 0;
            repeat = isChecked ? setBit(repeat, 0) : resetBit(repeat, 0);
        } else if (id == R.id.cb_monday) {
            position = 1;
            repeat = isChecked ? setBit(repeat, 1) : resetBit(repeat, 1);
        } else if (id == R.id.cb_tuesday) {
            position = 2;
            repeat = isChecked ? setBit(repeat, 2) : resetBit(repeat, 2);
        } else if (id == R.id.cb_wednesday) {
            position = 3;
            repeat = isChecked ? setBit(repeat, 3) : resetBit(repeat, 3);
        } else if (id == R.id.cb_thursday) {
            position = 4;
            repeat = isChecked ? setBit(repeat, 4) : resetBit(repeat, 4);
        } else if (id == R.id.cb_friday) {
            position = 5;
            repeat = isChecked ? setBit(repeat, 5) : resetBit(repeat, 5);
        } else if (id == R.id.cb_saturday) {
            position = 6;
            repeat = isChecked ? setBit(repeat, 6) : resetBit(repeat, 6);
        }
        repeat = setBit(repeat, 7);
    }

 public static byte setBit(byte data, int position) {
        return (byte) (data | (1 << position));
    }

    public static byte resetBit(byte data, int position) {
        return (byte) (data & ~(1 << position));
    }
```

- 24.Drinking water reminder(喝水提醒)

```java
BluetoothHelper.getInstance().getSendBlueData().sendDrinkRemindstart_h, start_m, end_h, end_m, frequency, drinkSwitch);

```

- 25.Equipment shutdown command(设备关机指令)


```java
BluetoothHelper.getInstance().getSendBlueData().sendShutDown();
```

- 26.OTA Invocation Method(OTA调用方式)


```java
// localPath: The OTA package path can be downloaded from the server to a local file.
   BluetoothHelper.getInstance().getTransfer().otaUpgrade(localPath, "", "");
```

- 27.HR PPG collection(HR PPG采集)


```java
//true: On; false: Off
BluetoothHelper.getInstance().getSendBlueData().sendOffOnPpg(true);
```

- 28.Gensor PPG Collection(Gensor PPG采集)


```java
//true: On; false: Off
BluetoothHelper.getInstance().getSendBlueData().sendOffOnGensor(true);
```

- 29.Enable gesture control commands(打开手势控制指令)


```java
SendBlueToothData.createBond(CEBlueSharedPreference.getDevAddress());
BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(1);
```

- 30.Turn off gesture control commands(关闭手势控制指令)


```java
SendBlueToothData.removeBond(CEBlueSharedPreference.getDevAddress());
BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(0);
```



##### 监听设备数据

- 31.Receive device data monitoring(接收设备数据监听)

```java

k6AnalysiDevManager =  BluetoothHelper.getInstance().getRcvDataManager()
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
            heartRate2.postValue(results);
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
            return false;
        }
    });

    //Sleep data (睡眠数据)
    //Upload the data in multiple packets; the app layer needs to piece them together for      complete display.(数据分多包上传，app层要拼接起来完整显示)  
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


    //Blood oxygen data (received during automatic monitoring) （血氧数据(自动监测时接收)）
    k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2_HISTORY, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
        @Override
        public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> result) {
            bloodOxygenData.postValue(result);
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
```

