package com.coolwear.uniplugin;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.MyBleDevice;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_Mix_sport_Struct;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean initialized = false;
    private boolean listenersRegistered = false;
    private final Set<String> emittedDevices = new HashSet<>();

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
        if (callback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("code", -1);
        result.put("message", message != null ? message : "unknown error");
        invokeCallback(callback, result);
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
                fireEvent("onHeartRateDaily", toHeartRateArray(list));
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_SPORT_HRV_FOR_SHOW,
                (K6BleDataResult<ArrayList<K6_HrvStruct>>) list -> {
                    fireEvent("onHRV", toHrvArray(list));
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
                JSONObject payload = new JSONObject();
                payload.put("raw", String.valueOf(data));
                fireEvent("onSleep", payload);
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> list) {
                fireEvent("onBloodOxygen", toBloodOxygenArray(list));
                return false;
            }
        });

        rcv.addBleDataResultListener(K6_Action.RCVD.RCVD_DATA_TYPE_REAL_O2_HISTORY, new K6BleDataResult<ArrayList<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public boolean bleDataResult(ArrayList<K6_DATA_TYPE_REAL_O2> list) {
                fireEvent("onBloodOxygen", toBloodOxygenArray(list));
                return false;
            }
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
            obj.put("calories", item.getCalories());
            obj.put("distance", item.getDistance());
            obj.put("raw", String.valueOf(item));
            arr.add(obj);
        }
        return arr;
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
            emittedDevices.clear();
            BluetoothHelper.getInstance().scanningDeviceInit(context, scanDevListener);
            BluetoothHelper.getInstance().startScan();
            success(callback);
        } catch (Exception e) {
            fail(callback, e.getMessage());
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
            BluetoothHelper.getInstance().disConnect();
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
            BluetoothHelper.getInstance().getSendBlueData().sendSyncInfo();
            success(callback);
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
