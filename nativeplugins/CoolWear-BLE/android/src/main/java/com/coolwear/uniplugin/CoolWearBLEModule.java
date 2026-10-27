package com.coolwear.uniplugin;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.MyBleDevice;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_CESyncTime;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_FUNCTION_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_Mix_sport_Struct;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_SendUserInfo;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.proxy.interfaces.IK6AnalysiDevRcvDataManager;
import ce.com.cenewbluesdk.proxy.interfaces.K6BleDataResult;
import ce.com.cenewbluesdk.proxy.interfaces.OnScanDevListener;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class CoolWearBLEModule extends UniModule {

    private static final String TAG = "CoolWearBLEModule";
    private static final String EVENT_CHANNEL = "CoolWearBLE";
    private static final int STATUS_DISCONNECT = 0;
    private static final int STATUS_CONNECTED = 1;
    private static final int STATUS_CONNECTING = 2;

    private static final int CODE_BLUETOOTH_OFF = -100;
    private static final int CODE_BLUETOOTH_DENIED = -101;
    private static final int MAX_HEALTH_CACHE = 300;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean initialized = false;
    private boolean listenersRegistered = false;
    private final Set<String> emittedDevices = new HashSet<>();
    private final List<HashMap<String, Object>> healthCache = new ArrayList<>();
    private volatile boolean o2AutoSwitchSeparate = false;

    private void success(UniJSCallback callback) {
        success(callback, null);
    }

    private void success(UniJSCallback callback, Object data) {
        if (callback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("code", 0);
        result.put("message", "ok");
        if (data != null) {
            result.put("data", data);
        }
        invokeCallback(callback, result);
    }

    private void fail(UniJSCallback callback, String message) {
        fail(callback, -1, message);
    }

    private void fail(UniJSCallback callback, int code, String message) {
        if (callback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("message", message != null ? message : "unknown error");
        invokeCallback(callback, result);
    }

    private boolean isBluetoothOn() {
        try {
            return BluetoothHelper.getInstance().isEnabled();
        } catch (Throwable e) {
            Log.w(TAG, "check bluetooth enabled failed", e);
            return true;
        }
    }

    private void cacheHealthEvent(String type, Object data) {
        if (type == null || data == null) return;
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("data", data);
        event.put("at", System.currentTimeMillis());
        synchronized (healthCache) {
            healthCache.add(event);
            while (healthCache.size() > MAX_HEALTH_CACHE) {
                healthCache.remove(0);
            }
        }
    }

    private void invokeCallback(UniJSCallback callback, JSONObject result) {
        Runnable task = () -> {
            try {
                callback.invoke(result);
            } catch (Exception e) {
                Log.e(TAG, "callback invoke failed", e);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler.post(task);
        }
    }

    private Object getUniInstance() {
        Class<?> cls = getClass().getSuperclass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                String typeName = field.getType().getName();
                if (typeName.contains("UniSDKInstance") || typeName.contains("WXSDKInstance")) {
                    try {
                        field.setAccessible(true);
                        return field.get(this);
                    } catch (Exception e) {
                        Log.w(TAG, "read sdk instance field failed: " + field.getName(), e);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private Context resolveContext() {
        try {
            Method getContext = UniModule.class.getMethod("getContext");
            Object ctx = getContext.invoke(this);
            if (ctx instanceof Context) {
                return (Context) ctx;
            }
        } catch (Exception ignored) {
        }
        Object inst = getUniInstance();
        if (inst == null) {
            return null;
        }
        try {
            Method getContext = inst.getClass().getMethod("getContext");
            Object ctx = getContext.invoke(inst);
            return ctx instanceof Context ? (Context) ctx : null;
        } catch (Exception e) {
            Log.e(TAG, "resolveContext failed", e);
            return null;
        }
    }

    private void fireEvent(String event, Object data) {
        Object inst = getUniInstance();
        if (inst == null) {
            Log.w(TAG, "fireEvent skipped, no sdk instance: " + event);
            return;
        }
        Runnable task = () -> {
            HashMap<String, Object> params = new HashMap<>();
            params.put("event", event);
            params.put("data", data);
            if (invokeGlobalEvent(inst, params)) {
                Log.d(TAG, "fireEvent ok: " + event);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler.post(task);
        }
    }

    private boolean invokeGlobalEvent(Object inst, HashMap<String, Object> params) {
        Class<?> cls = inst.getClass();
        while (cls != null) {
            for (Method method : cls.getMethods()) {
                if (!"fireGlobalEventCallback".equals(method.getName())) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types.length != 2 || types[0] != String.class) {
                    continue;
                }
                try {
                    Object arg1 = types[1].isInstance(params) ? params : new HashMap<>(params);
                    method.invoke(inst, EVENT_CHANNEL, arg1);
                    return true;
                } catch (Exception e) {
                    Log.w(TAG, "fireGlobalEventCallback failed: " + method, e);
                }
            }
            cls = cls.getSuperclass();
        }
        Log.e(TAG, "fireGlobalEventCallback method not found");
        return false;
    }

    private void fireConnectState(int status) {
        JSONObject payload = new JSONObject();
        payload.put("status", status);
        fireEvent("onConnectStateChange", payload);
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        IK6AnalysiDevRcvDataManager rcv = BluetoothHelper.getInstance().getRcvDataManager();

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_BLUE_CONNECT_STATE_CHANGE, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer status) {
                Log.d(TAG, "connect state changed: " + status);
                if (status != null && status == K6_Action.RCVD.BLUE_CONNECTED) {
                    BluetoothHelper.getInstance().devicePairFinish(1);
                    BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
                }
                JSONObject payload = new JSONObject();
                payload.put("status", status);
                fireEvent("onConnectStateChange", payload);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_PAIR_FINISH, new K6BleDataResult<Integer>() {
            @Override
            public boolean bleDataResult(Integer value) {
                JSONObject payload = new JSONObject();
                payload.put("result", value);
                fireEvent("onPairFinish", payload);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_BATTERY, new K6BleDataResult<K6_DATA_TYPE_BATTERY_INFO>() {
            @Override
            public boolean bleDataResult(K6_DATA_TYPE_BATTERY_INFO info) {
                // 使用 HashMap，避免 JSONObject 过桥后 JS 拿不到 charging 等字段
                HashMap<String, Object> payload = new HashMap<>();
                int level = info.getBattery();
                boolean charging = info.isChargerStatus();
                payload.put("level", level);
                payload.put("charging", charging);
                payload.put("chargerStatus", charging);
                payload.put("isChargerStatus", charging);
                Log.d(TAG, "onBattery level=" + level + ", charging=" + charging);
                fireEvent("onBattery", payload);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HEART_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HeartStruct>>) list -> {
                    fireEvent("onHeartRate", toHeartRateArray(list));
                    return false;
                });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DAILY_HEART, new K6BleDataResult<ArrayList<K6_HeartStruct>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_HeartStruct> list) {
                JSONArray arr = toHeartRateArray(list);
                cacheHealthEvent("dailyHeart", arr);
                fireEvent("onHeartRateDaily", arr);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HRV_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HrvStruct>>) list -> {
                    JSONArray arr = toHrvArray(list);
                    cacheHealthEvent("hrv", arr);
                    fireEvent("onHRV", arr);
                    return false;
                });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_TEMP_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_TempStruct>>) list -> {
                    fireEvent("onTemperature", toTempArray(list));
                    return false;
                });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_K6_SLEEP_DATA, new K6BleDataResult<K6_sleepData>() {
            @Override
            public boolean bleDataResult(K6_sleepData data) {
                JSONObject payload = toSleepPayload(data);
                cacheHealthEvent("sleep", payload);
                fireEvent("onSleep", payload);
                return false;
            }
        });

        // 兼容不同固件：睡眠数据可能经由其它接收类型上报（RCVD_SLEEP_DATA / RCVD_DATA_TYPE_SLEEP_MONITOR），
        // 这里统一做诊断 + 兜底解析，避免"设备有睡眠数据但 App 收不到"。
        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_SLEEP_MONITOR, new K6BleDataResult<Object>() {
            @Override
            public boolean bleDataResult(Object data) {
                Log.d(TAG, "RCVD_DATA_TYPE_SLEEP_MONITOR raw class=" + (data == null ? "null" : data.getClass().getName())
                        + " toString=" + String.valueOf(data));
                if (data instanceof K6_sleepData) {
                    JSONObject payload = toSleepPayload((K6_sleepData) data);
                    cacheHealthEvent("sleep", payload);
                    fireEvent("onSleep", payload);
                } else if (data instanceof java.util.List) {
                    // 兜底：把列表里每个元素尝试转成睡眠事件
                    Log.d(TAG, "RCVD_DATA_TYPE_SLEEP_MONITOR list size=" + ((java.util.List<?>) data).size());
                }
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SLEEP_DATA, new K6BleDataResult<Object>() {
            @Override
            public boolean bleDataResult(Object data) {
                Log.d(TAG, "RCVD_SLEEP_DATA raw class=" + (data == null ? "null" : data.getClass().getName())
                        + " toString=" + String.valueOf(data));
                if (data instanceof K6_sleepData) {
                    JSONObject payload = toSleepPayload((K6_sleepData) data);
                    cacheHealthEvent("sleep", payload);
                    fireEvent("onSleep", payload);
                }
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> list) {
                JSONArray arr = toBloodOxygenArray(list);
                cacheHealthEvent("spo2", arr);
                fireEvent("onBloodOxygen", arr);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2_HISTORY, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> list) {
                JSONArray arr = toBloodOxygenArray(list);
                cacheHealthEvent("spo2", arr);
                fireEvent("onBloodOxygen", arr);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_FUNCTION_CONTROL,
                (K6BleDataResult<K6_DATA_TYPE_FUNCTION_CONTROL>) caps -> {
                    if (caps != null) {
                        o2AutoSwitchSeparate = caps.isO2AutoSwitch();
                        Log.d(TAG, "function control: hasO2=" + caps.isHasO2()
                                + ", hasHR24H=" + caps.isHasHR24H()
                                + ", o2AutoSwitch=" + o2AutoSwitchSeparate);
                    }
                    return false;
                });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_DATA, new K6BleDataResult<ArrayList<K6_Sport>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_Sport> list) {
                fireEvent("onSport", toSportArray(list));
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_MIX_SPORT_DATA, new K6BleDataResult<ArrayList<K6_Mix_sport_Struct>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_Mix_sport_Struct> list) {
                JSONArray arr = new JSONArray();
                if (list != null) {
                    for (K6_Mix_sport_Struct item : list) {
                        JSONObject obj = new JSONObject();
                        obj.put("raw", String.valueOf(item));
                        arr.add(obj);
                    }
                }
                fireEvent("onMixSport", arr);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DEVINFO, new K6BleDataResult<K6_DevInfoStruct>() {
            @Override
            public boolean bleDataResult(K6_DevInfoStruct info) {
                fireEvent("onDeviceInfo", toDeviceInfoMap(info));
                return false;
            }
        });

        listenersRegistered = true;
    }

    private JSONArray toHeartRateArray(List<K6_HeartStruct> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (K6_HeartStruct item : list) {
            JSONObject obj = new JSONObject();
            obj.put("value", item.getHeartNums());
            obj.put("time", item.getTime());
            arr.add(obj);
        }
        return arr;
    }

    private JSONArray toHrvArray(List<K6_HrvStruct> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (K6_HrvStruct item : list) {
            JSONObject obj = new JSONObject();
            obj.put("value", item.getHrvNums());
            obj.put("time", item.getTime());
            // 部分固件/结构体会直接带压力值；没有则由 JS 侧按 HRV 估算
            Object stress = invokeQuiet(item, "getStress");
            if (stress == null) stress = invokeQuiet(item, "getPressure");
            if (stress == null) stress = invokeQuiet(item, "getStressValue");
            if (stress == null) stress = invokeQuiet(item, "getPress");
            if (stress != null) {
                obj.put("stress", stress);
                obj.put("pressure", stress);
            }
            arr.add(obj);
        }
        return arr;
    }

    private JSONArray toTempArray(List<K6_TempStruct> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (K6_TempStruct item : list) {
            JSONObject obj = new JSONObject();
            obj.put("value", item.getTempValue());
            obj.put("time", item.getTime());
            arr.add(obj);
        }
        return arr;
    }

    private JSONArray toBloodOxygenArray(List<K6_DATA_TYPE_REAL_O2> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (K6_DATA_TYPE_REAL_O2 item : list) {
            JSONObject obj = new JSONObject();
            Object value = invokeQuiet(item, "getO2");
            if (value == null) value = invokeQuiet(item, "getOxygen");
            if (value == null) value = invokeQuiet(item, "getSpo2");
            if (value == null) value = invokeQuiet(item, "getBloodOxygen");
            if (value == null) value = invokeQuiet(item, "getOxygenValue");
            if (value == null) value = invokeQuiet(item, "getValue");
            if (value == null) value = invokeQuiet(item, "getO2Value");
            if (value != null) {
                obj.put("value", value);
                obj.put("o2", value);
            }
            Object time = invokeQuiet(item, "getTime");
            if (time != null) {
                obj.put("time", time);
            }
            obj.put("raw", String.valueOf(item));
            arr.add(obj);
        }
        return arr;
    }

    private JSONArray toSportArray(List<K6_Sport> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (K6_Sport item : list) {
            JSONObject obj = new JSONObject();
            // Demo：getWalkSteps / getDistance(米) / getCalories(千卡*1000)
            Object walkSteps = invokeQuiet(item, "getWalkSteps");
            if (walkSteps == null) walkSteps = invokeQuiet(item, "getSteps");
            if (walkSteps == null) walkSteps = invokeQuiet(item, "getStep");
            if (walkSteps != null) {
                obj.put("walkSteps", walkSteps);
                obj.put("steps", walkSteps);
            }
            obj.put("calories", item.getCalories());
            obj.put("distance", item.getDistance());
            Object time = invokeQuiet(item, "getTime");
            if (time != null) {
                obj.put("time", time);
            }
            obj.put("raw", String.valueOf(item));
            arr.add(obj);
        }
        return arr;
    }

    /**
     * 睡眠数据：SDK Demo 字段 deepTime/lightTime/movementTime/sleepTime(分钟)
     * startTime/endTime(秒)、sleepDetail(JSON 分段)、getK6SleepData() 多晚列表
     */
    private JSONObject toSleepPayload(K6_sleepData data) {
        JSONObject payload = new JSONObject();
        if (data == null) {
            return payload;
        }

        JSONObject typeMap = new JSONObject();
        try {
            typeMap.put("start", CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_START);
            typeMap.put("light", CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_LIGHT);
            typeMap.put("deep", CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_DEEP);
            typeMap.put("movement", CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_MOVEMENT);
            typeMap.put("wakeup", CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_WAKEUP);
        } catch (Throwable t) {
            Log.w(TAG, "read SLEEPSTATUS constants failed", t);
        }
        // 部分固件可能带 REM
        Object remConst = readStaticQuiet("ce.com.cenewbluesdk.CEBC$SLEEPSTATUS", "SLEEPDATA_SLEEP_REM");
        if (remConst == null) {
            remConst = readStaticQuiet("ce.com.cenewbluesdk.CEBC$SLEEPSTATUS", "SLEEPDATA_SLEEP_EYEMOVE");
        }
        if (remConst != null) {
            typeMap.put("rem", remConst);
        }
        payload.put("typeMap", typeMap);

        putSleepFields(payload, data);

        Object nested = invokeQuiet(data, "getK6SleepData");
        JSONArray sessions = new JSONArray();
        if (nested instanceof List) {
            for (Object item : (List<?>) nested) {
                if (item == null) continue;
                JSONObject session = new JSONObject();
                putSleepFields(session, item);
                sessions.add(session);
            }
        }
        payload.put("sessions", sessions);

        // 顶层无汇总时长时，用最近一晚补齐，方便 JS 直接读
        if ((payload.get("sleepTime") == null || toInt(payload.get("sleepTime")) <= 0) && sessions.size() > 0) {
            JSONObject last = sessions.getJSONObject(sessions.size() - 1);
            if (last != null) {
                for (String key : last.keySet()) {
                    if (!payload.containsKey(key) || payload.get(key) == null) {
                        payload.put(key, last.get(key));
                    }
                }
            }
        }

        payload.put("raw", String.valueOf(data));
        Log.d(TAG, "onSleep sleepTime=" + payload.get("sleepTime")
                + ", deep=" + payload.get("deepTime")
                + ", light=" + payload.get("lightTime")
                + ", sessions=" + sessions.size());
        return payload;
    }

    private void putSleepFields(JSONObject obj, Object item) {
        if (obj == null || item == null) return;
        putIfPresent(obj, "deepTime", invokeQuiet(item, "getDeepTime"));
        putIfPresent(obj, "lightTime", invokeQuiet(item, "getLightTime"));
        putIfPresent(obj, "movementTime", invokeQuiet(item, "getMovementTime"));
        Object rem = invokeQuiet(item, "getRemTime");
        if (rem == null) rem = invokeQuiet(item, "getEyeMoveTime");
        if (rem == null) rem = invokeQuiet(item, "getRem");
        putIfPresent(obj, "remTime", rem);
        putIfPresent(obj, "sleepTime", invokeQuiet(item, "getSleepTime"));
        putIfPresent(obj, "startTime", invokeQuiet(item, "getStartTime"));
        putIfPresent(obj, "endTime", invokeQuiet(item, "getEndTime"));
        putIfPresent(obj, "sleepDay", invokeQuiet(item, "getSleepDay"));
        putIfPresent(obj, "uploadTime", invokeQuiet(item, "getUploadTime"));
        Object detail = invokeQuiet(item, "getSleepDetail");
        if (detail != null) {
            obj.put("sleepDetail", String.valueOf(detail));
        }
        Object original = invokeQuiet(item, "getOriginalSleepDetail");
        if (original != null) {
            obj.put("originalSleepDetail", String.valueOf(original));
        }
        Object sleeps = invokeQuiet(item, "getK6_sleeps");
        if (sleeps == null) sleeps = invokeQuiet(item, "getK6Sleeps");
        if (sleeps instanceof List) {
            JSONArray arr = new JSONArray();
            for (Object s : (List<?>) sleeps) {
                if (s == null) continue;
                JSONObject one = new JSONObject();
                putIfPresent(one, "sleepType", invokeQuiet(s, "getSleepType"));
                putIfPresent(one, "type", invokeQuiet(s, "getSleepType"));
                putIfPresent(one, "sleepTime", invokeQuiet(s, "getSleepTime"));
                putIfPresent(one, "time", invokeQuiet(s, "getSleepTime"));
                one.put("raw", String.valueOf(s));
                arr.add(one);
            }
            obj.put("sleeps", arr);
        }
    }

    private void putIfPresent(JSONObject obj, String key, Object value) {
        if (obj == null || key == null || value == null) return;
        obj.put(key, value);
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception e) {
            return 0;
        }
    }

    private Object readStaticQuiet(String className, String fieldName) {
        try {
            Class<?> cls = Class.forName(className);
            Field field = cls.getField(fieldName);
            return field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private final OnScanDevListener scanDevListener = new OnScanDevListener() {
        @Override
        public void onFindDev(ScanResult scanRecord) {
        }

        @Override
        public void onFindDevList(ScanResult scanRecord, List<MyBleDevice> deviceList, MyBleDevice myBleDevice) {
            if (deviceList == null) {
                return;
            }
            for (MyBleDevice device : deviceList) {
                if (device == null || device.getmBluetoothDevice() == null) {
                    continue;
                }
                BluetoothDevice bt = device.getmBluetoothDevice();
                String mac = bt.getAddress();
                if (mac == null || emittedDevices.contains(mac)) {
                    continue;
                }
                emittedDevices.add(mac);
                JSONObject payload = new JSONObject();
                payload.put("name", bt.getName() != null ? bt.getName() : "");
                payload.put("mac", mac);
                payload.put("identifier", mac);
                if (scanRecord != null) {
                    payload.put("rssi", scanRecord.getRssi());
                }
                if (device.getmK6ManufacturerInfo() != null) {
                    payload.put("deviceId", device.getmK6ManufacturerInfo().name);
                }
                fireEvent("onDeviceFound", payload);
            }
        }
    };

    @UniJSMethod(uiThread = true)
    public void init(UniJSCallback callback) {
        try {
            Log.d(TAG, "init start, thread=" + Thread.currentThread().getName());
            Context context = resolveContext();
            if (context == null) {
                fail(callback, "Context unavailable");
                return;
            }
            Context appContext = context.getApplicationContext();
            if (!initialized) {
                Log.d(TAG, "init BluetoothHelper");
                BluetoothHelper.getInstance().init();
                BluetoothHelper.getInstance().initProxy(appContext);
                initialized = true;
                Log.d(TAG, "initProxy done");
            }
            registerListeners();
            Log.d(TAG, "init success");
            success(callback);
        } catch (Throwable e) {
            Log.e(TAG, "init failed", e);
            fail(callback, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @UniJSMethod(uiThread = true)
    public void startScan(UniJSCallback callback) {
        try {
            Context context = resolveContext();
            if (context == null) {
                fail(callback, "Context unavailable");
                return;
            }
            if (!isBluetoothOn()) {
                fail(callback, CODE_BLUETOOTH_OFF, "bluetooth_off");
                return;
            }
            emittedDevices.clear();
            BluetoothHelper.getInstance().scanningDeviceInit(context, scanDevListener);
            BluetoothHelper.getInstance().startScan();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void resetBluetooth(UniJSCallback callback) {
        // 蓝牙从关闭→开启后，BluetoothHelper 内部状态可能残留为"未就绪"，
        // 导致 startScan 静默扫不到设备（需杀进程重进才恢复）。这里强制重新初始化。
        try {
            Context context = resolveContext();
            if (context == null) {
                fail(callback, "Context unavailable");
                return;
            }
            Context appContext = context.getApplicationContext();
            initialized = false;
            Log.d(TAG, "resetBluetooth: re-init BluetoothHelper");
            BluetoothHelper.getInstance().init();
            BluetoothHelper.getInstance().initProxy(appContext);
            initialized = true;
            emittedDevices.clear();
            success(callback);
        } catch (Throwable e) {
            Log.e(TAG, "resetBluetooth failed", e);
            fail(callback, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @UniJSMethod(uiThread = true)
    public void stopScan(UniJSCallback callback) {
        try {
            BluetoothHelper.getInstance().stopScan();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void connect(JSONObject options, UniJSCallback callback) {
        try {
            if (!isBluetoothOn()) {
                fail(callback, CODE_BLUETOOTH_OFF, "bluetooth_off");
                return;
            }
            String mac = options != null ? options.getString("mac") : null;
            if (mac == null && options != null) {
                mac = options.getString("identifier");
            }
            if (mac == null || mac.isEmpty()) {
                fail(callback, "mac/identifier is required");
                return;
            }
            String name = options != null ? options.getString("name") : "";
            String deviceId = options != null ? options.getString("deviceId") : null;

            BluetoothHelper.getInstance().stopScan();
            BluetoothHelper.getInstance().disConnect();
            BluetoothHelper.getInstance().getPersistent().setBlueAddress(mac);
            if (name != null && !name.isEmpty()) {
                BluetoothHelper.getInstance().getPersistent().setDevName(name);
            }
            if (deviceId != null && !deviceId.isEmpty()) {
                BluetoothHelper.getInstance().getPersistent().setDeviceId(deviceId);
            }

            fireConnectState(K6_Action.RCVD.BLUE_CONNECTING);
            Log.d(TAG, "connectDev mac=" + mac + ", savedName=" + name);
            // 第二个参数为过滤名，Demo 传空字符串；传设备名可能导致连接失败
            BluetoothHelper.getInstance().connectDev(mac, "");
            success(callback);
        } catch (Exception e) {
            Log.e(TAG, "connect failed", e);
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void disconnect(UniJSCallback callback) {
        try {
            BluetoothHelper.getInstance().stopScan();
            BluetoothHelper.getInstance().disConnect();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void unbind(UniJSCallback callback) {
        try {
            BluetoothHelper.getInstance().stopScan();
            BluetoothHelper.getInstance().unbindDevice();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void openBluetoothSettings(UniJSCallback callback) {
        try {
            Context context = resolveContext();
            if (context == null) {
                fail(callback, "Context unavailable");
                return;
            }
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = false)
    public void isConnected(UniJSCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("connected", BluetoothHelper.getInstance().isConnectOk());
            success(callback, data);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void devicePairFinish(JSONObject options, UniJSCallback callback) {
        try {
            int vibrate = 1;
            if (options != null && options.containsKey("vibrate")) {
                vibrate = options.getIntValue("vibrate");
            }
            BluetoothHelper.getInstance().devicePairFinish(vibrate);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void sendSyncInfo(UniJSCallback callback) {
        try {
            // 官方 demo 连接后调用 sendAsynInfo（同步用户信息/时间/语言等），
            // 历史睡眠数据还需要 sendSleepMonitor 打开设备侧睡眠监测。
            BluetoothHelper.getInstance().getSendDataManager().sendAsynInfo();
            // 关键：真正让设备"重新上传缓存数据"（心率/血氧/HRV/睡眠）的是这条 sendSyncInfo，
            // 官方 demo 的 MainActivity.onResume 就是靠它触发重传。sendAsynInfo 只同步时间/语言，不拉数据。
            BluetoothHelper.getInstance().getSendBlueData().sendSyncInfo();
            BluetoothHelper.getInstance().getSendDataManager().sendSleepMonitor();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    /**
     * 同步手机时间到设备。设备时间决定睡眠/运动数据的时间归属，
     * 不同步时固件可能判定不到「夜晚」，导致睡眠数据为空或日期错位。
     */
    @UniJSMethod(uiThread = true)
    public void syncTime(UniJSCallback callback) {
        try {
            K6_CESyncTime time = K6_CESyncTime.getCurrentTime();
            if (time == null) {
                long now = System.currentTimeMillis();
                int offsetSeconds = TimeZone.getDefault().getOffset(now) / 1000;
                time = new K6_CESyncTime(now / 1000, offsetSeconds, (byte) 1, (byte) 0);
            }
            BluetoothHelper.getInstance().getSendBlueData().sendDateTimeFormat(time);
            HashMap<String, Object> data = new HashMap<>();
            data.put("absTime", System.currentTimeMillis() / 1000);
            success(callback, data);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void setHeartRateSwitch(JSONObject options, UniJSCallback callback) {
        try {
            boolean open = options == null || options.getBooleanValue("open");
            int status = open ? CEBC.OPENSTATUS.OPEN : CEBC.OPENSTATUS.CLOSE;
            BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(status);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void setHRVSwitch(JSONObject options, UniJSCallback callback) {
        try {
            boolean open = options == null || options.getBooleanValue("open");
            int status = open ? CEBC.OPENSTATUS.OPEN : CEBC.OPENSTATUS.CLOSE;
            BluetoothHelper.getInstance().getSendBlueData().sendHRVSwitch(status);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void setBloodOxygenSwitch(JSONObject options, UniJSCallback callback) {
        try {
            boolean open = options == null || options.getBooleanValue("open");
            int status = open ? CEBC.OPENSTATUS.OPEN : CEBC.OPENSTATUS.CLOSE;
            BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(status);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void setHeartAutoSwitch(JSONObject options, UniJSCallback callback) {
        try {
            boolean heart = options == null || options.getBooleanValue("heart");
            boolean o2 = options == null || options.getBooleanValue("o2");
            int interval = options != null ? options.getIntValue("intervalMin") : 30;
            if (interval <= 0) {
                interval = 30;
            }
            int heartStatus = heart ? CEBC.OPENSTATUS.OPEN : CEBC.OPENSTATUS.CLOSE;
            int o2Status = o2 ? CEBC.OPENSTATUS.OPEN : CEBC.OPENSTATUS.CLOSE;
            BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(heartStatus, o2Status, interval);
            // 部分固件把自动血氧做成独立开关（FUNCTION_CONTROL.isO2AutoSwitch=true），
            // 只在 HeartAutoSwitch 里带 O2 位不会生效，需要再单独打开一次 O2 检测。
            if (o2 && o2AutoSwitchSeparate) {
                BluetoothHelper.getInstance().getSendDataManager().sendK6_DATA_TYPE_REAL_O2(CEBC.OPENSTATUS.OPEN);
            }
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void setSensorOn(JSONObject options, UniJSCallback callback) {
        try {
            boolean open = options == null || options.getBooleanValue("open");
            BluetoothHelper.getInstance().setEnableGsDataTrans(open);
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void syncUserInfo(JSONObject options, UniJSCallback callback) {
        try {
            int userId = options != null ? options.getIntValue("userId") : 0;
            int sex = options != null ? options.getIntValue("sex") : 0;
            int age = options != null ? options.getIntValue("age") : 30;
            int height = options != null ? options.getIntValue("height") : 170;
            int weight = options != null ? options.getIntValue("weight") : 60;
            int lrHand = options != null ? options.getIntValue("lrHand") : 1;
            age = Math.min(120, Math.max(1, age));
            height = Math.min(250, Math.max(50, height));
            weight = Math.min(250, Math.max(20, weight));
            sex = sex == 1 ? 1 : 0;
            lrHand = lrHand == 0 ? 0 : 1;
            BluetoothHelper.getInstance().getSendDataManager()
                    .sendUserInfo(new K6_SendUserInfo(userId, sex, age, height, weight, lrHand));
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void pullCachedHealthEvents(UniJSCallback callback) {
        try {
            JSONArray events = new JSONArray();
            synchronized (healthCache) {
                for (HashMap<String, Object> event : healthCache) {
                    events.add(new JSONObject(event));
                }
                healthCache.clear();
            }
            HashMap<String, Object> data = new HashMap<>();
            data.put("events", events);
            success(callback, data);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    /**
     * 修改设备（蓝牙）名称，会写入戒指硬件
     */
    @UniJSMethod(uiThread = true)
    public void setDeviceName(JSONObject options, UniJSCallback callback) {
        try {
            String name = options != null ? options.getString("name") : null;
            if (name == null || name.trim().isEmpty()) {
                fail(callback, "name is required");
                return;
            }
            BluetoothHelper.getInstance().getSendBlueData().sendChangeName(name.trim());
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void requestBattery(UniJSCallback callback) {
        try {
            BluetoothHelper.getInstance().getSendBlueData().sendSyncInfo();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    /**
     * 请求设备信息（含固件版本），结果通过 onDeviceInfo 事件回调
     */
    @UniJSMethod(uiThread = true)
    public void requestDeviceInfo(UniJSCallback callback) {
        try {
            BluetoothHelper.getInstance().getSendBlueData().sendSyncInfo();
            // 同步附带一次本地已缓存的软件版本，便于 JS 立刻展示
            HashMap<String, Object> cached = new HashMap<>();
            try {
                Object persistent = BluetoothHelper.getInstance().getPersistent();
                Object softwareVersion = invokeQuiet(persistent, "getSoftwareVersion");
                Object deviceId = invokeQuiet(persistent, "getDeviceId");
                Object devName = invokeQuiet(persistent, "getDevName");
                if (softwareVersion != null) {
                    String ver = String.valueOf(softwareVersion);
                    cached.put("softwareVer", ver);
                    cached.put("firmware", ver);
                }
                if (deviceId != null) cached.put("deviceId", String.valueOf(deviceId));
                if (devName != null) cached.put("name", String.valueOf(devName));
            } catch (Exception ignore) {
                // ignore
            }
            if (!cached.isEmpty()) {
                fireEvent("onDeviceInfo", cached);
            }
            success(callback, cached);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    @UniJSMethod(uiThread = true)
    public void reconnect(UniJSCallback callback) {
        try {
            boolean ok = BluetoothHelper.getInstance().reConnectDirect();
            JSONObject data = new JSONObject();
            data.put("started", ok);
            success(callback, data);
        } catch (Exception e) {
            fail(callback, e.getMessage());
        }
    }

    private HashMap<String, Object> toDeviceInfoMap(K6_DevInfoStruct info) {
        HashMap<String, Object> payload = new HashMap<>();
        if (info == null) {
            return payload;
        }
        try {
            payload.put("customerId", info.getCustomer_id());
        } catch (Exception e) {
            Log.w(TAG, "getCustomer_id failed", e);
        }
        // Demo 使用 getSoftwareVer() 作为固件版本
        Object softwareVer = invokeQuiet(info, "getSoftwareVer");
        if (softwareVer == null) {
            softwareVer = invokeQuiet(info, "getSoftwareVersion");
        }
        if (softwareVer != null) {
            String ver = String.valueOf(softwareVer);
            payload.put("softwareVer", ver);
            payload.put("firmware", ver);
        }
        Object hardwareVer = invokeQuiet(info, "getHardwareVer");
        if (hardwareVer == null) {
            hardwareVer = invokeQuiet(info, "getHardWareVer");
        }
        if (hardwareVer != null) {
            payload.put("hardwareVer", String.valueOf(hardwareVer));
        }
        try {
            Object persistent = BluetoothHelper.getInstance().getPersistent();
            Object cachedVer = invokeQuiet(persistent, "getSoftwareVersion");
            if ((softwareVer == null || String.valueOf(softwareVer).isEmpty()) && cachedVer != null) {
                String ver = String.valueOf(cachedVer);
                payload.put("softwareVer", ver);
                payload.put("firmware", ver);
            }
            Object deviceId = invokeQuiet(persistent, "getDeviceId");
            Object devName = invokeQuiet(persistent, "getDevName");
            if (deviceId != null) payload.put("deviceId", String.valueOf(deviceId));
            if (devName != null) payload.put("name", String.valueOf(devName));
        } catch (Exception e) {
            Log.w(TAG, "read persistent device info failed", e);
        }
        payload.put("raw", String.valueOf(info));
        Log.d(TAG, "onDeviceInfo firmware=" + payload.get("firmware") + ", customerId=" + payload.get("customerId"));
        return payload;
    }

    private Object invokeQuiet(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }
}
