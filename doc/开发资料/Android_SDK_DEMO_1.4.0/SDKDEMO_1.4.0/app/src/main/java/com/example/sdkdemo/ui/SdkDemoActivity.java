package com.example.sdkdemo.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sdkdemo.BluetoothManager;
import com.example.sdkdemo.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HEART_AUTO_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HR_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_MixSportType;
import ce.com.cenewbluesdk.entity.k6.K6_SEND_APP_SPORT_STRUCT;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_StressStruct;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.entity.k6.k6_GSENSOR_DATA;
import ce.com.cenewbluesdk.entity.k6.k6_RRI_HRV_DATA;
import ce.com.cenewbluesdk.proxy.interfaces.K6BleDataResult;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.proxy.sdkhelper.SendBlueToothData;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * Author:null
 * Date:2026/8/19 9:13
 * Description:添加您的描述
 */
public class SdkDemoActivity extends AppCompatActivity {

    private final static String TGA = "SdkDemoActivity";
    private LinearLayout tabReceive;
    private LinearLayout tabSend;

    private TextView tvReceive;
    private TextView tvSend;
    private TextView tvLog;

    private View lineReceive;
    private View lineSend;

    private ScrollView layoutReceive;
    private RecyclerView rvSend;

    private final List<String> sendItems = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TGA, "--onCreate--");
        setContentView(R.layout.activity_sdk_demo);
        initView();
        initSendData();
        showReceivePage();
        //Listen for received data
        receiveData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TGA, "--onResume--");
    }

    private void initView() {

        tabReceive =
                findViewById(R.id.tab_receive);

        tabSend =
                findViewById(R.id.tab_send);

        tvReceive =
                findViewById(R.id.tv_receive);

        tvSend =
                findViewById(R.id.tv_send);

        tvLog =
                findViewById(R.id.tv_log);

        lineReceive =
                findViewById(R.id.line_receive);

        lineSend =
                findViewById(R.id.line_send);

        layoutReceive =
                findViewById(R.id.layout_receive);

        rvSend =
                findViewById(R.id.rv_send);

        findViewById(R.id.iv_back)
                .setOnClickListener(v -> finish());

        tabReceive.setOnClickListener(v ->
                showReceivePage()
        );

        tabSend.setOnClickListener(v ->
                showSendPage()
        );
    }

    private void receiveData() {

        //Connection status
        BluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    appendLog(CEBlueSharedPreference.getDeviceName() + "——" + CEBlueSharedPreference.getDevAddress() + " : Connected");
//                    //Data transmission channel is open; by default, the device does not upload data.
//                    BluetoothHelper.getInstance().setEnableGsDataTrans(true);
                    //Synchronize information to the device: e.g., phone time zone and time
                    BluetoothHelper.getInstance().getSendDataManager().sendAsynInfo();
                } else if (integer == K6_Action.RCVD.BLUE_CONNECTING) {
                    appendLog(CEBlueSharedPreference.getDeviceName() + " : Connecting...");
                } else if (integer == K6_Action.RCVD.BLUE_DISCONNECT) {
                    appendLog(CEBlueSharedPreference.getDeviceName() + " : Disconnected");
                }

            }
        });

        //Heart rate monitoring is enabled
        BluetoothManager.heartAutoSwitch.observe(this, new Observer<K6_DATA_TYPE_HEART_AUTO_SWITCH>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HEART_AUTO_SWITCH result) {
                appendLog("Heart rate monitoring is enabled : " + result.toString());
            }
        });

        //Battery Information
        BluetoothManager.batteryLiveData.observe(this, new Observer<K6_DATA_TYPE_BATTERY_INFO>() {
            @Override
            public void onChanged(K6_DATA_TYPE_BATTERY_INFO battery_info) {
                appendLog("Received Battery：" + battery_info.getBattery() + "%" + "   Charging：" +
                        (battery_info.isChargerStatus() ? "Y" : "N"));
            }
        });

        //devInfo
        BluetoothManager.devInfoStructLive.observe(this, new Observer<K6_DevInfoStruct>() {
            @Override
            public void onChanged(K6_DevInfoStruct devInfoStruct) {
                appendLog("Received device information：" + devInfoStruct.toString());
            }
        });

        //Heart rate data
        BluetoothManager.heartRate.observe(this, new Observer<List<K6_HeartStruct>>() {
            @Override
            public void onChanged(List<K6_HeartStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_HeartStruct heartStruct : result) {
                    stringBuilder.append(heartStruct.toString());
                }
                appendLog("Received heart rate data：" + stringBuilder);

            }
        });

        //Blood oxygen data
        BluetoothManager.bloodOxygenData.observe(this, new Observer<List<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public void onChanged(List<K6_DATA_TYPE_REAL_O2> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_DATA_TYPE_REAL_O2 struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog("Received blood oxygen data：" + stringBuilder);

            }
        });


        //hrv data
        BluetoothManager.hrvLiveData.observe(this, new Observer<List<k6_RRI_HRV_DATA>>() {
            @Override
            public void onChanged(List<k6_RRI_HRV_DATA> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (k6_RRI_HRV_DATA struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog("Received hrv data：" + stringBuilder);
            }
        });

        //stress data
        BluetoothManager.stressLiveData.observe(this, new Observer<List<K6_StressStruct>>() {
            @Override
            public void onChanged(List<K6_StressStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_StressStruct struct : result) {
                    stringBuilder.append(struct.toString());
                }
                appendLog("Received pressure data：" + stringBuilder);
            }
        });


        //Temperature data
        BluetoothManager.tempLiveData.observe(this, new Observer<List<K6_TempStruct>>() {
            @Override
            public void onChanged(List<K6_TempStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_TempStruct tempStruct : result) {
                    stringBuilder.append(tempStruct.toString());
                }
                appendLog("Received temperature data：" + stringBuilder);
            }
        });

        //Sleep data
        BluetoothManager.sleepData.observe(this, new Observer<K6_sleepData>() {
            @Override
            public void onChanged(K6_sleepData result) {
                appendLog("Receive sleep data：" + result.toString());
            }
        });

        //  exercise data
        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
            @Override
            public void onChanged(List<K6_Sport> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_Sport sport : result) {
                    stringBuilder.append(sport.toString());
                }
                appendLog("Receive motion data：" + stringBuilder);
            }
        });

        //OTA progress
        BluetoothManager.otaFileProgress.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                appendLog("OTA progress received：" + integer);

            }
        });

        //Release status detection
        BluetoothManager.disposalStatus.observe(this, new Observer<K6_DATA_TYPE_HR_CONTROL>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HR_CONTROL control) {
                if (control.controlState == 0) {
                    appendLog("The device has been removed from your hands. Please wear it correctly!");
                } else if (control.controlState == 1) {
                    appendLog("Measurement complete!");
                }
            }
        });

        //hr ppg
        BluetoothManager.heartPpgLiveData.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> payload) {
                appendLog("Received HR PPG data:" + payload.toString());
            }
        });


        //gSoner
        BluetoothManager.gSonerDataLiveData.observe(this, new Observer<List<k6_GSENSOR_DATA>>() {
            @Override
            public void onChanged(List<k6_GSENSOR_DATA> payload) {
                appendLog("Data received from gSoner:" + payload.toString());
            }
        });
        //hid
        BluetoothManager.hidLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer payload) {
                //Receiving device gesture switch status: 0: indicates the device HID is off; greater than 0: indicates the device HID is on.
                appendLog("Received HID switch:" + payload.toString());
            }
        });
    }

    private void initSendData() {
        sendItems.clear();
        sendItems.add("1. Heart rate test");
        sendItems.add("2. Blood oxygen test");
        sendItems.add("3. HRV test");
        sendItems.add("4. Stress test");
        sendItems.add("5. Set automatic detection switch");
        sendItems.add("6. App exercise started");
        sendItems.add("7. App activity pause");
        sendItems.add("8. App campaign continues");
        sendItems.add("9. App activity stopped");
        sendItems.add("10. Shake to take a picture");
        sendItems.add("11. Equipment shutdown");
        sendItems.add("12. OTA Invocation Method");
        sendItems.add("13. HR PPG");
        sendItems.add("14. Gensor PPG");
        sendItems.add("15. CreateHid");
        sendItems.add("16. CloseHid");

        SdkSendAdapter adapter =
                new SdkSendAdapter(sendItems);

        rvSend.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvSend.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {

            switch (position) {

                case 0:
                    testHeartRate();
                    break;

                case 1:
                    testSpo2();
                    break;
                case 2:
                    testHRV();
                    break;

                case 3:
                    testStress();
                    break;

                case 4:
                    setAutoMonitor();
                    break;

                case 5:
                    appSportStart();
                    break;

                case 6:
                    appSportPause();
                    break;

                case 7:
                    appSportResume();
                    break;

                case 8:
                    appSportStop();
                    break;

                case 9:
                    shakeCamera();
                    break;

                case 10:
                    shutdownDevice();
                    break;

                case 11:
                    startOta();
                    break;
                case 12:
                    startHrPPg();
                    break;
                case 13:
                    startGensor();
                    break;
                case 14:
                    createHid();
                    break;
                case 15:
                    closeHid();
                    break;
            }
            showReceivePage();
        });
    }

    private void showReceivePage() {

        layoutReceive.setVisibility(View.VISIBLE);
        rvSend.setVisibility(View.GONE);

        tvReceive.setTextColor(Color.WHITE);
        tvReceive.setTypeface(null, Typeface.BOLD);

        tvSend.setTextColor(Color.parseColor("#BDBDBD"));

        tvSend.setTypeface(null, Typeface.NORMAL);

        lineReceive.setVisibility(View.VISIBLE);
        lineSend.setVisibility(View.INVISIBLE);
    }

    private void showSendPage() {

        layoutReceive.setVisibility(View.GONE);
        rvSend.setVisibility(View.VISIBLE);

        tvSend.setTextColor(Color.WHITE);
        tvSend.setTypeface(null, Typeface.BOLD);

        tvReceive.setTextColor(Color.parseColor("#BDBDBD"));

        tvReceive.setTypeface(null, Typeface.NORMAL);

        lineReceive.setVisibility(View.INVISIBLE);
        lineSend.setVisibility(View.VISIBLE);
    }

    /**
     * Add receive logs
     */
    public void appendLog(String log) {
        runOnUiThread(() -> {
            String old = tvLog.getText().toString();
            String newLog =
                    old + "\n" + getCurrentTime() + "  " + log;
            tvLog.setText(newLog);
            layoutReceive.post(() ->
                    layoutReceive.fullScroll(View.FOCUS_DOWN)
            );
        });
    }

    private String getCurrentTime() {

        return new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindDevice();
        Logger.d(TGA, "--onDestroy--");
    }

    /**
     * Unbinding device
     *
     */
    private void unbindDevice() {
        BluetoothHelper.getInstance().unbindDevice();
    }

    /**
     * disConnect device
     *
     */
    private void disConnect() {
        BluetoothHelper.getInstance().disConnect();
    }

    private void testHeartRate() {
        appendLog("Send: Start Heart Rate Test");
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.OPEN);
    }

    private void testSpo2() {
        appendLog("Send: Start Blood Oxygen Test");
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.OPEN);
    }

    private void testStress() {
        appendLog("Send: Start stress test");
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendStressSwitch(CEBC.OPENSTATUS.OPEN);
    }

    private void testHRV() {
        appendLog("Send: Start hrv test");
        //Start test: CEBC.OPENSTATUS.OPEN Stop test: CEBC.OPENSTATUS.CLOSE
        BluetoothHelper.getInstance().getSendBlueData().sendRriHrvCmd(CEBC.OPENSTATUS.OPEN);
    }

    private void setAutoMonitor() {
        appendLog("Send: Set automatic detection switch");
        //parameter1 Heart rate switch; parameter2 Blood oxygen switch; parameter3 Rate interval (minutes)
        BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(CEBC.OPENSTATUS.OPEN, CEBC.OPENSTATUS.OPEN, 5);
    }

    private void appSportStart() {
        appendLog("Send: Start App workout");
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_START, 0, 0);
    }

    private void appSportPause() {
        appendLog("Send: App exercise pause");
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_PAUSE, 0, 0);
    }

    private void appSportResume() {
        appendLog("Send: Continue exercising via App");
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_CONTINUE, 0, 0);
    }

    private void appSportStop() {
        appendLog("Send: App activity stopped");
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.MIX_SPORT_RUN, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
    }

    private void shakeCamera() {
        appendLog("Send: Shake to take photo switch");
        //true 开 ；false 关
        BluetoothHelper.getInstance().getSendBlueData().sendPhotoSwitch(true);

    }

    private void shutdownDevice() {
        appendLog("Send: Device shutdown");
        BluetoothHelper.getInstance().getSendBlueData().sendShutDown();
    }

    private void startOta() {
        appendLog("Send: OTA call");
        // localPath: The OTA package path can be downloaded from the server to a local file.
        //   BluetoothHelper.getInstance().getTransfer().otaUpgrade(localPath, "", "");
    }

    private void startHrPPg() {
        appendLog("Send: Heart rate PPG");
        //true: On; false: Off
        BluetoothHelper.getInstance().getSendBlueData().sendOffOnPpg(true);
    }

    private void startGensor() {
        appendLog("Send: Gensor Collection");
        //true: On; false: Off
        BluetoothHelper.getInstance().getSendBlueData().sendOffOnGensor(true);
    }

    private void createHid() {
        appendLog("Send: Enable HID pairing");
        SendBlueToothData.createBond(CEBlueSharedPreference.getDevAddress());
        BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(1);

    }

    private void closeHid() {
        appendLog("Send: Disable HID pairing");
        SendBlueToothData.removeBond(CEBlueSharedPreference.getDevAddress());
        BluetoothHelper.getInstance().getSendDataManager().sendGestureConfig(0);
    }
}
