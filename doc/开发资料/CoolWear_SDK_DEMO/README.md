2024/11/11 更新
1.增加hrv  --> com/coolwear/bluesdk/BluetoothManager.java

//接收手动，自动HRV数据    压力是HRV数据通过算法计算
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HRV_FOR_SHOW,
(K6BleDataResult<ArrayList<K6_HrvStruct>>) k6_heartStructs -> {
hrvLiveData.postValue(k6_heartStructs);
return false;
});


2.增加手势  -->com/coolwear/bluesdk/BluetoothManager.java
//接收设备手势开关状态
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_GESTURE_DATA,
(K6BleDataResult<K6_DATA_TYPE_GESTURE_CONFIG>) gesture_config -> {
GESTURE_OFF = gesture_config.getControlType();
return false;
});

3.增加GSensor    -->com/coolwear/bluesdk/BluetoothManager.java
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


4.电量协议增加 充电状态 -->com/coolwear/bluesdk/BluetoothManager.java

k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_BATTERY, new K6BleDataResult<K6_DATA_TYPE_BATTERY_INFO>() {
@Override
public boolean bleDataResult(K6_DATA_TYPE_BATTERY_INFO battery_info) {
gBatteryLiveData.postValue(battery_info);
if (dataCallBack != null) {
dataCallBack.updateBattery(battery_info.getBattery());
}
return false;
}
});

5.增加温度  -->com/coolwear/bluesdk/BluetoothManager.java
//接收体温数据
k6AnalysiDevManager.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_TEMP_FOR_SHOW,
(K6BleDataResult<ArrayList<K6_TempStruct>>) tempStructs -> {
tempLiveData.postValue(tempStructs);
return false;
});


11/26

1.增加自动检测开关 timeInterval:以分钟为单位
BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(autoHeart,autoO2,timeInterval);
2.增加清除数据协议
BluetoothHelper.getInstance().getSendBlueData().sendClearData();


