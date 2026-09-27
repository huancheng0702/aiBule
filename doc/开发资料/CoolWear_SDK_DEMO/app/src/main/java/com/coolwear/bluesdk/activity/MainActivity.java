package com.coolwear.bluesdk.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.BuildConfig;
import com.coolwear.bluesdk.Constant;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.databinding.ActivityMainBinding;
import com.coolwear.bluesdk.dialog.CustomEditDialog;
import com.coolwear.bluesdk.utils.ToastUtil;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.WeatherData;
import ce.com.cenewbluesdk.entity.k6.K6NoDisturbSummary;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_BATTERY_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_DRINK_ALARM;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_FUNCTION_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HAND_RISE_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HEART_AUTO_SWITCH;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_TARGET_ALARM;
import ce.com.cenewbluesdk.entity.k6.K6_MessageNoticeStruct;
import ce.com.cenewbluesdk.entity.k6.K6_MixSportType;
import ce.com.cenewbluesdk.entity.k6.K6_NoDisturb;
import ce.com.cenewbluesdk.entity.k6.K6_SEND_APP_SPORT_STRUCT;
import ce.com.cenewbluesdk.entity.k6.K6_SendAlarmInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_SendRealTimeWeather;
import ce.com.cenewbluesdk.entity.k6.K6_SittingRemind;
import ce.com.cenewbluesdk.entity.k6.K6_Sleep;
import ce.com.cenewbluesdk.proxy.BleFactory;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.proxy.sdkhelper.SendBlueToothData;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Lg;
import ce.com.cenewbluesdk.uitl.Logger;
import ce.com.cenewbluesdk.uitl.TimeUtil;
import ce.com.cenewbluesdk.uitl.fileprint.FileLogPrint;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding activityMainBinding;
    private String address;
    private String name;
    private String devId;
    private BluetoothManager bluetoothManager;
    private int autoHeart;
    private int heart24;
    private int targetSwitch;
    private int drinkSwitch;
    private int sedentarySwitch;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isGsersor;
    private K6_DATA_TYPE_FUNCTION_CONTROL control;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(R.layout.activity_main);
        setContentView(activityMainBinding.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bluetoothManager = ViewModelProviders.of(this).get(BluetoothManager.class);
        bluetoothManager.initBLE();
        address = getIntent().getStringExtra("address");
        name = getIntent().getStringExtra("name");
        devId = getIntent().getStringExtra("devId");

        if (BuildConfig.BUILDTYPE == 3) {
            activityMainBinding.btnOta.setVisibility(View.VISIBLE);
            activityMainBinding.btnOta2.setVisibility(View.GONE);
//            appSport.setVisibility(View.GONE);
        } else if (BuildConfig.BUILDTYPE == 2) {
            activityMainBinding.scrollView.setVisibility(View.GONE);
            activityMainBinding.btnOta2.setVisibility(View.VISIBLE);
        } else if (BuildConfig.BUILDTYPE == 1) {
            activityMainBinding.scrollView.setVisibility(View.VISIBLE);
            activityMainBinding.btnOta2.setVisibility(View.GONE);
            activityMainBinding.btnOta.setVisibility(View.GONE);
        }
        activityMainBinding.btnFindDevice.setOnClickListener(this);
        activityMainBinding.btn12Format.setOnClickListener(this);
        activityMainBinding.btn24Format.setOnClickListener(this);
        activityMainBinding.btnMdFormat.setOnClickListener(this);
        activityMainBinding.btnDmFormat.setOnClickListener(this);
        activityMainBinding.btnAnswer.setOnClickListener(this);
        activityMainBinding.btnCancelCall.setOnClickListener(this);
        activityMainBinding.btnIncoming.setOnClickListener(this);
        activityMainBinding.btnSdkPush.setOnClickListener(this);
        activityMainBinding.btnSend.setOnClickListener(this);
        activityMainBinding.btnModifyDial.setOnClickListener(this);
        activityMainBinding.btnDevTool.setOnClickListener(this);
        activityMainBinding.btnReceiveData.setOnClickListener(this);
        activityMainBinding.btnOta.setOnClickListener(this);
        activityMainBinding.btnWeather.setOnClickListener(this);
        activityMainBinding.btnChinese.setOnClickListener(this);
        activityMainBinding.btnEnglish.setOnClickListener(this);
        activityMainBinding.btnSystemLanguage.setOnClickListener(this);
        activityMainBinding.btnUnit.setOnClickListener(this);
        activityMainBinding.btnGoals.setOnClickListener(this);
        activityMainBinding.swAutoHeart.setOnClickListener(this);
        activityMainBinding.swTfhr.setOnClickListener(this);
        activityMainBinding.swTakePhoto.setOnClickListener(this);
        activityMainBinding.swHid.setOnClickListener(this);
        activityMainBinding.btnOta2.setOnClickListener(this);
        activityMainBinding.btnTelephone.setOnClickListener(this);
        activityMainBinding.btnStart.setOnClickListener(this);
        activityMainBinding.btnPause.setOnClickListener(this);
        activityMainBinding.btnStop.setOnClickListener(this);
        activityMainBinding.btnContinue.setOnClickListener(this);
        activityMainBinding.btnRestart.setOnClickListener(this);
        activityMainBinding.btnOpenBle.setOnClickListener(this);
        activityMainBinding.btnRealWeather.setOnClickListener(this);
        activityMainBinding.btnOldota.setOnClickListener(this);
        activityMainBinding.btnLg.setOnClickListener(this);
        activityMainBinding.btnAlarm.setOnClickListener(this);
        activityMainBinding.btnGsensor.setOnClickListener(this);
        activityMainBinding.btnSleepMonitor.setOnClickListener(this);
        activityMainBinding.btnShutdown.setOnClickListener(this);
        activityMainBinding.btnDataCollection.setOnClickListener(this);
        activityMainBinding.btnChangeName.setOnClickListener(this);
        activityMainBinding.tvDevInfo.setText("ID:" + devId + "  " + name);
        activityMainBinding.tvMac.setText("mac:" + address);

        //自动心率开关监听
        activityMainBinding.swAutoHeart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    autoHeart = CEBC.OPENSTATUS.OPEN;
                } else {
                    autoHeart = CEBC.OPENSTATUS.CLOSE;
                }
                BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(autoHeart, CEBC.OPENSTATUS.OPEN, 1);
            }
        });
//        //24小时心率开关监听
//        activityMainBinding.swTfhr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (b) {
//                    heart24 = CEBC.OPENSTATUS.OPEN;
//                } else {
//                    heart24 = CEBC.OPENSTATUS.CLOSE;
//                }
//                BluetoothHelper.getInstance().getSendBlueData().sendHeartAutoSwitch(autoHeart, heart24);
//            }
//        });

        //智拍开关监听
        activityMainBinding.swTakePhoto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BluetoothHelper.getInstance().getSendBlueData().sendPhotoSwitch(b);
            }
        });

        //手势控制
        activityMainBinding.swHid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BluetoothHelper.getInstance().getSendBlueData().sendGestureConfig(b ? 1 : 0);
                if (b) {
                    SendBlueToothData.createBond(CEBlueSharedPreference.getDevAddress());
                }
            }
        });
        //表盘选择
        activityMainBinding.dialSwitch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (position > 0) {
                    BluetoothHelper.getInstance().getSendBlueData().sendDialSwitch(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //语言选择
        activityMainBinding.spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                BluetoothHelper.getInstance().getSendBlueData().sendSwitchLanguage(position - 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //达标提醒开关监听
        activityMainBinding.swMarkRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                if (isOpen) {
                    targetSwitch = CEBC.OPENSTATUS.OPEN;
                } else {
                    targetSwitch = CEBC.OPENSTATUS.CLOSE;
                }
                BluetoothHelper.getInstance().getSendBlueData().sendTargetRemind(targetSwitch);
            }
        });
        //喝水提醒开关监听
        activityMainBinding.swDrinkRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                if (isOpen) {
                    drinkSwitch = CEBC.OPENSTATUS.OPEN;
                } else {
                    drinkSwitch = CEBC.OPENSTATUS.CLOSE;
                }
                //0: 30min, 1: 1Hour, 2: 2H, 3: 3H.
                BluetoothHelper.getInstance().getSendBlueData().sendDrinkRemind(8, 0, 22, 0, 0, drinkSwitch);
            }
        });
        //久坐提醒开关监听
        activityMainBinding.swSedentaryRemind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                if (isOpen) {
                    sedentarySwitch = CEBC.OPENSTATUS.OPEN;
                } else {
                    sedentarySwitch = CEBC.OPENSTATUS.CLOSE;
                }
                byte repeat = (byte) (6 + 128);
                BluetoothHelper.getInstance().getSendBlueData().sendSittingRemind(8, 0, 22, 0, repeat, sedentarySwitch, 1);
            }
        });
        //勿扰提醒开关监听
        activityMainBinding.swNotDisturb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                //开始时间8:00  结束时间22:00   1为打开0为关闭
                BluetoothHelper.getInstance().getSendBlueData().sendNoDisturb(8, 0, 22, 0, isOpen ? 1 : 0);
            }
        });
        //抬腕提醒开关监听
        activityMainBinding.swLiftWristBright.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOpen) {
                BluetoothHelper.getInstance().getSendBlueData().sendHandRiseWitch(8, 0, 22, 0, isOpen ? 1 : 0);
            }
        });

        //连接状态
        bluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                            /*
             Device connection status.
                STATUS           VALUE
                disconnected      0       K6_Action.RCVD.BLUE_DISCONNECT
                connected         1       K6_Action.RCVD.BLUE_CONNECTED
                connecting        2       K6_Action.RCVD.BLUE_CONNECTING
            */
                String name = BluetoothHelper.getInstance().getPersistent().getDevName();
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    activityMainBinding.tvStatus.setText(getResources().getString(R.string.connected));
                } else if (integer == K6_Action.RCVD.BLUE_CONNECTING) {
                    activityMainBinding.tvStatus.setText("连接中...");
                } else {
                    activityMainBinding.tvStatus.setText("已断开");
                }
            }
        });
        //心率检测开启状态
        bluetoothManager.heartAutoSwitch.observe(this, new Observer<K6_DATA_TYPE_HEART_AUTO_SWITCH>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HEART_AUTO_SWITCH result) {
                activityMainBinding.swAutoHeart.setChecked(result.getAutoHROnoff() == CEBC.OPENSTATUS.OPEN);
                activityMainBinding.swTfhr.setChecked(result.getHr24hOnoff() == CEBC.OPENSTATUS.OPEN);
            }
        });
        //达标提醒开启状态
        bluetoothManager.targetAlarmLiveData.observe(this, new Observer<K6_DATA_TYPE_TARGET_ALARM>() {
            @Override
            public void onChanged(K6_DATA_TYPE_TARGET_ALARM k6_data_type_target_alarm) {
                activityMainBinding.swMarkRemind.setChecked(k6_data_type_target_alarm.getOnoff() == CEBC.OPENSTATUS.OPEN);
            }
        });
        //喝水提醒开启状态
        bluetoothManager.drinkAlarmLiveData.observe(this, new Observer<K6_DATA_TYPE_DRINK_ALARM>() {
            @Override
            public void onChanged(K6_DATA_TYPE_DRINK_ALARM k6_data_type_drink_alarm) {
                activityMainBinding.swDrinkRemind.setChecked(k6_data_type_drink_alarm.getOnoff() == CEBC.OPENSTATUS.OPEN);
            }
        });
        //久坐提醒
        bluetoothManager.sittingLiveData.observe(this, new Observer<K6_SittingRemind>() {
            @Override
            public void onChanged(K6_SittingRemind k6_sittingRemind) {
                activityMainBinding.swSedentaryRemind.setChecked(k6_sittingRemind.getSwitch_flag() == CEBC.OPENSTATUS.OPEN);
            }
        });
        //勿扰
        bluetoothManager.noDisturbLiveData.observe(this, new Observer<K6NoDisturbSummary>() {
            @Override
            public void onChanged(K6NoDisturbSummary data) {
                activityMainBinding.swNotDisturb.setChecked(data.getSwit() == CEBC.OPENSTATUS.OPEN);
            }
        });
        bluetoothManager.brightScreenLiveData.observe(this, new Observer<K6_DATA_TYPE_HAND_RISE_SWITCH>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HAND_RISE_SWITCH data) {
                activityMainBinding.swLiftWristBright.setChecked(data.getOnoff() == CEBC.OPENSTATUS.OPEN);
            }
        });
        bluetoothManager.controlLiveData.observe(this, new Observer<K6_DATA_TYPE_FUNCTION_CONTROL>() {
            @Override
            public void onChanged(K6_DATA_TYPE_FUNCTION_CONTROL k6_data_type_function_control) {
                if (k6_data_type_function_control == null) {
                    return;
                }
                control = k6_data_type_function_control;
                if (k6_data_type_function_control.isHasContacts()) {
                    activityMainBinding.btnTelephone.setVisibility(View.VISIBLE);
                } else {
                    activityMainBinding.btnTelephone.setVisibility(View.GONE);
                }
                if (k6_data_type_function_control.isHasWeatherNow()) {
                    activityMainBinding.btnRealWeather.setVisibility(View.VISIBLE);
                } else {
                    activityMainBinding.btnRealWeather.setVisibility(View.GONE);
                }
                if (k6_data_type_function_control.isBtedr_call()) {
                    activityMainBinding.btnOpenBle.setVisibility(View.VISIBLE);
                } else {
                    activityMainBinding.btnOpenBle.setVisibility(View.GONE);
                }
                if (k6_data_type_function_control.isHasGame()) {
                    isGsersor = true;
                    activityMainBinding.btnGsensor.setVisibility(View.VISIBLE);
                } else {
                    isGsersor = false;
                    activityMainBinding.btnGsensor.setVisibility(View.GONE);
                }

                if (k6_data_type_function_control.isHasGestureSupported()) {
                    activityMainBinding.swHid.setVisibility(View.VISIBLE);
                } else {
                    activityMainBinding.swHid.setVisibility(View.GONE);
                }
            }
        });

        bluetoothManager.batteryLiveData.observe(this, new Observer<K6_DATA_TYPE_BATTERY_INFO>() {
            @Override
            public void onChanged(K6_DATA_TYPE_BATTERY_INFO battery_info) {
                activityMainBinding.tvBattery.setText("电量：" + battery_info.getBattery() + "%" + "   充电中：" +
                        (battery_info.isChargerStatus() ? "是" : "否"));
            }
        });

        bluetoothManager.hidLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer offOn) {
                activityMainBinding.swHid.setChecked(offOn > 0);
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_find_device://查找手环
                BluetoothHelper.getInstance().getSendBlueData().sendFindDevice();
                test();
                break;
            case R.id.btn_12_format://12小时制
                BluetoothHelper.getInstance().getSendBlueData().sendTimeFormat(CEBC.TIMEDATEFORMAT.HOURS12);
                break;
            case R.id.btn_24_format://24小时制
                BluetoothHelper.getInstance().getSendBlueData().sendTimeFormat(CEBC.TIMEDATEFORMAT.HOURS24);
                break;
            case R.id.btn_md_format://月日
                BluetoothHelper.getInstance().getSendBlueData().sendDateFormat(CEBC.TIMEDATEFORMAT.MONTHDAY);
                break;
            case R.id.btn_dm_format://日月
                BluetoothHelper.getInstance().getSendBlueData().sendDateFormat(CEBC.TIMEDATEFORMAT.DAYMONTH);
                break;
            case R.id.btn_incoming://来电
                String phoneNumber = "12345678901";
                String name = "Andy";
                BluetoothHelper.getInstance().getSendBlueData().sendIncomingCall(name, phoneNumber);
                break;
            case R.id.btn_answer:// 接电话
                BluetoothHelper.getInstance().getSendBlueData().sendCall(CEBC.CALLTYPE.ANSWERPHONE);
                break;
            case R.id.btn_cancel_call://挂断
                BluetoothHelper.getInstance().getSendBlueData().sendCall(CEBC.CALLTYPE.HANGUPPHONE);
                break;
            case R.id.btn_sdk_push://SDK推送
                startActivity(new Intent(this, SDKPushSettingActivity.class));
                break;
            case R.id.btn_openBle://打开手表蓝牙
                BluetoothHelper.getInstance().getSendBlueData().sendOpenSysBle();
                break;
            case R.id.btn_send://发送消息
                switch (activityMainBinding.spinnerMessage.getSelectedItemPosition()) {
                    case 0:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_MSG, "This is a SMS message");
                        break;
                    case 1:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_QQ, "This is a QQ message");
                        break;
                    case 2:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_WX, "This is a WeChat message");
                        break;
                    case 3:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_EMAIL, "This is a EMAIL message");
                        break;
                    case 4:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_FACEBOOK, "This is a FACEBOOK message");
                        break;
                    case 5:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_TWITTER, "This is a TWITTER message");
                        break;
                    case 6:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_WHATSAPP, "This is a WHATSAPP message");
                        break;
                    case 7:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_SKYPE, "This is a SKYPE message");
                        break;
                    case 8:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_LINKED_IN, "This is a LINKED IN message");
                        break;
                    case 9:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_LINE, "This is a LINE message");
                        break;
                    case 10:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_OTHER, "This is a other message");
                        break;
                    case 11:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_KAKAO, "This is a KAKAO message");
                        break;
                    case 12:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_INSTAGRAM, "This is a instagram message");
                        break;
                    case 13:
                        BluetoothHelper.getInstance().getSendBlueData().sendPushMessage("msgName", K6_MessageNoticeStruct.TYPE_TELEGRAM, "This is a telegram message");
                        break;
                }
                break;
            case R.id.btn_modify_dial://修改表盘
//                startActivity(new Intent(this, ModifyDialActivity.class));
                break;
            case R.id.btn_dev_tool://表盘工具
//                startActivity(new Intent(this, DeveloperToolActivity.class));
                break;
            case R.id.btn_receiveData://接收数据
                startActivity(new Intent(this, ReceiveDataActivity.class));
                break;
            case R.id.btn_oldota://ota升级
                startActivity(new Intent(this, OTAUpgradeActivity.class));
                break;
            case R.id.btn_ota://ota升级
            case R.id.btn_ota2://ota升级
                startActivity(new Intent(this, UpgradeOtaActivity.class));
                break;
            case R.id.btn_weather://天气 Weather
                List<WeatherData> weatherDatas = new ArrayList<>();

                WeatherData today = new WeatherData(10, -2, -1);
                WeatherData tomorrow = new WeatherData(11, 20, 30);
                WeatherData afterTomorrow = new WeatherData(12, 25, 35);
//                WeatherData weatherNow = new WeatherData(10, 15, 15);


                weatherDatas.add(today);//今天
                weatherDatas.add(tomorrow);//明天
                weatherDatas.add(afterTomorrow);//后天
//                weatherDatas.add(weatherNow);//实时天气

                BluetoothHelper.getInstance().getSendBlueData().sendWeatherInfo(weatherDatas);
                break;
            case R.id.btn_realWeather://实时天气
                K6_SendRealTimeWeather weatherNow = new K6_SendRealTimeWeather(10, 20, 1, 60);
                BluetoothHelper.getInstance().getSendBlueData().sendRealTimeWeather(weatherNow);
                break;
            case R.id.btn_systemLanguage://系统语言 SystemLanguage
                BluetoothHelper.getInstance().getSendBlueData().sendSystemLanguage();
                break;
            case R.id.btn_english://英语 English
                BluetoothHelper.getInstance().getSendBlueData().sendSwitchLanguage(CEBC.LANGUAGE.LAN_ENGLISH);
                break;
            case R.id.btn_chinese://中文 Chinese
                BluetoothHelper.getInstance().getSendBlueData().sendSwitchLanguage(CEBC.LANGUAGE.LAN_CHINESE_SIMPLE);
                break;
            case R.id.btn_lg:
                if (TextUtils.isEmpty(activityMainBinding.etLg.getText().toString().trim())) {
                    return;
                }
                try {
                    BluetoothHelper.getInstance().getSendBlueData().sendSwitchLanguage(Integer.parseInt(activityMainBinding.etLg.getText().toString().trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_unit://单位设置 Setting unit
//                0: Kilogram(千克)，1：lb(磅)，2：Stone(英石)
//                0: Meter(米)，1：Mile(英里)
//                0：Celsius(摄氏度)，1：Fahrenheit(华氏度)
                BluetoothHelper.getInstance().getSendBlueData().sendUnitSetting(
                        CEBC.UNITTYPE.WEIGHT_KILOGRAM,
                        CEBC.UNITTYPE.DIS_METER,
                        CEBC.UNITTYPE.WEATHER_CELSIUS);
                break;
            case R.id.btn_goals://每日目标 Daily goals
                EditText editText = new EditText(this);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.set_number))
                        .setView(editText)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BluetoothHelper.getInstance().getSendBlueData().sendTargetRemind(CEBC.OPENSTATUS.OPEN);
                                String steps = editText.getText().toString();
                                if (!TextUtils.isEmpty(steps)) {
                                    BluetoothHelper.getInstance().getSendBlueData().sendDailyGoals(Integer.parseInt(steps), 200, 300, 400, 500);
                                    Toast.makeText(MainActivity.this, "TargetStep：" +
                                                    BluetoothHelper.getInstance().getPersistent().getTargetStep() + "\nTargetDistance：" +
                                                    BluetoothHelper.getInstance().getPersistent().getTargetDistance() + "\nTargetCalorie：" +
                                                    BluetoothHelper.getInstance().getPersistent().getTargetCalorie() + "\nTargetSleep：" +
                                                    BluetoothHelper.getInstance().getPersistent().getTargetSleep() + "\nTargetDuration：" +
                                                    BluetoothHelper.getInstance().getPersistent().getTargetDuration(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();

                break;
            case R.id.btn_telephone://电话簿
                startActivity(new Intent(this, ContactsActivity.class));
//                BluetoothHelper.getInstance().getSendBlueData().sendContacts("柯沃","13688888888");
                break;
            case R.id.btn_alarm://闹钟
                String hour = activityMainBinding.etHour.getText().toString().trim();
                String min = activityMainBinding.etMin.getText().toString().trim();
                if (TextUtils.isEmpty(hour)) return;
                if (TextUtils.isEmpty(min)) return;

                K6_SendAlarmInfoStruct alarm = new K6_SendAlarmInfoStruct(127, Integer.parseInt(hour), Integer.parseInt(min), 1, "");
                ArrayList<K6_SendAlarmInfoStruct> sendAlarm = new ArrayList<>();
                sendAlarm.add(alarm);
                BluetoothHelper.getInstance().getSendBlueData().sendAlarmInfo(sendAlarm);
                break;
            case R.id.btn_start://运动开始
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_START, 23, 32);
                break;
            case R.id.btn_pause://运动暂停
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_PAUSE, 0, 0);
                break;
            case R.id.btn_stop://运动停止
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
                break;
            case R.id.btn_continue://运动继续
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_CONTINUE, 30, 100);
                break;
            case R.id.btn_restart://设备重启
                BluetoothHelper.getInstance().getSendBlueData().sendDeviceRestart();
                break;
            case R.id.btn_gsensor://gsensor
                FileLogPrint.init();
                startActivity(new Intent(this, GsensorActivity.class));
                break;
            case R.id.btn_sleep_monitor://睡眠监测
                BluetoothHelper.getInstance().getSendBlueData().sendSleepMonitor();
                startActivity(new Intent(this, SleepMonitorActivity.class));
                break;
            case R.id.btn_shutdown://设备关机
                BluetoothHelper.getInstance().getSendBlueData().sendShutDown();
                break;
            case R.id.btn_data_collection://数据采集
                startActivity(new Intent(this, DataCollectionActivity.class));
                break;
            case R.id.btn_change_name://更改名称
                getChangeNameDialog(this, "更改蓝牙名称",
                        CEBlueSharedPreference.getDeviceName(),
                        "取消",
                        "确定");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BluetoothHelper.getInstance().isConnectOk()) {
            BluetoothHelper.getInstance().setEnableGsDataTrans(false);
            BluetoothHelper.getInstance().getSendBlueData().sendSyncInfo();
        } else {
            BluetoothHelper.getInstance().reConnectDirect();
            if (TextUtils.isEmpty(BluetoothHelper.getInstance().getPersistent().getBlueAddress())) {
                finish();
            }
        }
        Logger.i("BluetoothHelper.getInstance().isConnectOk()=" + BluetoothHelper.getInstance().isConnectOk());
        requestPermission();
//        i++;
//        Lg.e("MainActivity","i="+i,true);
    }

    private void requestPermission() {
//        BluetoothHelper.getInstance().getPersistent().getAppTimeDisplay();
//        BluetoothHelper.getInstance().getPersistent().getAppDateDisplay();
//        AndPermission.with(this)
//                .runtime()
//                .permission(new String[]{Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE})
//                .onGranted(new Action<List<String>>() {
//                    @Override
//                    public void onAction(List<String> data) {
//
//                    }
//                }).onDenied(new Action<List<String>>() {
//            @Override
//            public void onAction(List<String> data) {
//                Toast.makeText(MainActivity.this, "APP need permission for normal use", Toast.LENGTH_LONG).show();
//            }
//        }).rationale(new Rationale<List<String>>() {
//            @Override
//            public void showRationale(Context context, List<String> data, RequestExecutor executor) {
//                Toast.makeText(MainActivity.this, "请到APP设置授予相关权限", Toast.LENGTH_LONG).show();
//                new AlertDialog.Builder(MainActivity.this)
//                        .setTitle("Please go to settings to grant related permissions")
//                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                toSelfSetting(MainActivity.this);
//                            }
//                        }).show();
//
//            }
//
//        }).start();

    }

    private void toSelfSetting(Context context) {
        Intent mIntent = new Intent();
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        mIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(mIntent);
    }

    private void disconnect() {
        BluetoothHelper.getInstance().disConnect();
        BluetoothHelper.getInstance().getPersistent().setBlueAddress("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isGsersor) {
            BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(0);
        }
        disconnect();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask = null;
        }
        Log.e("TAG_GATT", "isConnectOk()=" + BluetoothHelper.getInstance().isConnectOk());

    }

    private CustomEditDialog getChangeNameDialog(Context context, String title, String content, String leftText, String rightText) {
        final CustomEditDialog dialog = new CustomEditDialog(context);
        dialog.setText(title, content, leftText, rightText);
        dialog.setHint("请输入蓝牙名称");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnLeftBtnListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setOnRightBtnListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(dialog.getEditText().getText().toString())) {
                    BluetoothHelper.getInstance().getSendBlueData().sendChangeName(dialog.getEditText().getText().toString());
                    dialog.dismiss();
                    ToastUtil.showToast(MainActivity.this, "修改成功,需断开后重新配对");
                } else {
                    ToastUtil.showToast(MainActivity.this, "请输入蓝牙名称");
                }
            }
        });
        dialog.show();
        return dialog;
    }


    private void test() {
        //byte[] datas = new byte[]{1, 0, 1, 8, 1, -68, 23, 120, 104, 5, 12, 34, 120, 104, 3, 100, 36, 120, 104, 5, -68, 38, 120, 104, 3, 20, 41, 120, 104, 5, 108, 43, 120, 104, 3, -60, 45, 120, 104, 4, 40, 51, 120, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
       // byte[] datas = new byte[]{7, 0, 1, 15, 1, 100, -116, 106, 104, 2, 98, -114, 106, 104, 3, -54, -113, 106, 104, 5, 50, -111, 106, 104, 3, 78, -109, 106, 104, 2, -22, 20, 107, 104, 3, -14, 27, 107, 104, 5, -106, 29, 107, 104, 3, -70, 38, 107, 104, 2, -66, 94, 107, 104, 3, -22, 95, 107, 104, 5, -118, 101, 107, 104, 3, -110, 108, 107, 104, 2, -42, -96, 107, 104, 4, 2, -94, 107, 104};
       // byte[] datas = new byte[]{6, 0, 1, 3, 1, 2, -94, 107, 104, 3, 2, -94, 107, 104, 4, 62, -94, 107, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //byte[] datas = new byte[]{5, 0, 1, 15, 1, -80, -43, 107, 104, 2, -82, -41, 107, 104, 3, 30, -32, 107, 104, 5, -34, -29, 107, 104, 3, -86, -22, 107, 104, 2, -34, -75, 108, 104, 3, -26, -68, 108, 104, 5, 42, -76, 109, 104, 3, 50, -69, 109, 104, 2, 50, -39, 109, 104, 3, 94, -38, 109, 104, 5, 14, -3, 109, 104, 3, 58, -2, 109, 104, 2, 38, 3, 110, 104, 4, 82, 4, 110, 104};
        //byte[] datas = new byte[]{4, 0, 1, 4, 1, 82, 4, 110, 104, 3, 82, 4, 110, 104, 5, 76, 55, 110, 104, 4, 84, 62, 110, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //byte[] datas = new byte[]{3, 0, 1, 15, 1, 36, 125, 110, 104, 2, -102, -69, 110, 104, 3, -94, -62, 110, 104, 5, 86, 14, 111, 104, 3, 94, 21, 111, 104, 2, -30, 8, 112, 104, 3, 14, 10, 112, 104, 5, -102, 49, 113, 104, 3, -58, 50, 113, 104, 2, -14, -70, 113, 104, 3, 30, -68, 113, 104, 5, -90, 96, 114, 104, 3, -46, 97, 114, 104, 2, -78, 114, 114, 104, 4, -34, 115, 114, 104};
       // byte[] datas = new byte[]{2, 0, 1, 15, 1, -34, 115, 114, 104, 3, -34, 115, 114, 104, 5, -66, 117, 114, 104, 3, -42, 123, 114, 104, 2, 46, -70, 114, 104, 3, 54, -63, 114, 104, 5, 98, 13, 115, 104, 3, 106, 20, 115, 104, 2, 126, 75, 115, 104, 3, -86, 76, 115, 104, 5, 74, -114, 115, 104, 3, 118, -113, 115, 104, 2, 102, -67, 115, 104, 3, -110, -66, 115, 104, 4, 54, -64, 115, 104};
        //byte[] datas = new byte[]{1, 0, 1, 3, 1, 54, -64, 115, 104, 5, 56, 9, 116, 104, 4, 64, 16, 116, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //byte[] datas = new byte[]{1, 0, 1, 10, 1, -60, -15, 119, 104, 2, -62, -13, 119, 104, 3, 42, -11, 119, 104, 5, 114, -8, 119, 104, 3, -38, -7, 119, 104, 2, -30, 0, 120, 104, 3, -22, 7, 120, 104, 5, -10, 10, 120, 104, 3, -118, 13, 120, 104, 4, -74, 14, 120, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
       // byte[] datas = new byte[]{1, 0, 1, 7, 1, -108, 18, 120, 104, 2, -110, 20, 120, 104, 3, -6, 21, 120, 104, 2, -10, 25, 120, 104, 3, 94, 27, 120, 104, 5, -16, 31, 120, 104, 4, -8, 38, 120, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //byte[] datas = new byte[]{1, 0, 1, 5, 1, 20, 41, 120, 104, 2, 30, 46, 120, 104, 3, -122, 47, 120, 104, 5, -18, 48, 120, 104, 4, -34, 49, 120, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] datas = new byte[]{1, 0, 1, 6, 1, -116, 71, 120, 104, 2, -118, 73, 120, 104, 3, -14, 74, 120, 104, 5, 90, 76, 120, 104, 3, 102, 79, 120, 104, 4, -20, 80, 120, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        byte[] datas1 = new byte[]{3, 0, 1, 15, 1, 28, -41, -108, 104, 2, 24, -37, -108, 104, 3, 4, -32, -108, 104, 2, 28, -26, -108, 104, 3, 116, -24, -108, 104, 2, -52, -22, -108, 104, 3, 32, -15, -108, 104, 2, 68, -6, -108, 104, 3, -100, -4, -108, 104, 2, 16, 1, -107, 104, 3, 104, 3, -107, 104, 2, 124, 13, -107, 104, 3, -44, 15, -107, 104, 5, 44, 18, -107, 104, 4, -112, 23, -107, 104};
        byte[] datas2 = new byte[]{2, 0, 1, 5, 1, -112, 23, -107, 104, 3, -112, 23, -107, 104, 5, 80, 27, -107, 104, 3, -88, 29, -107, 104, 4, 104, 33, -107, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] datas3 = new byte[]{1, 0, 1, 10, 1, 72, 35, -107, 104, 5, 36, 41, -107, 104, 3, 48, 44, -107, 104, 5, -16, 47, -107, 104, 3, 100, 52, -107, 104, 5, -116, 57, -107, 104, 3, -28, 59, -107, 104, 5, 60, 62, -107, 104, 3, 72, 65, -107, 104, 4, -80, 66, -107, 104, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};



        K6_sleepData k1 = realProcess(datas1);
        K6_sleepData k2 = realProcess(datas2);
        K6_sleepData k3 = realProcess(datas3);
        K6_sleepData k6 = realProcess(datas);
    }

    private K6_sleepData k6_sleepData = new K6_sleepData();

    public K6_sleepData realProcess(byte[] datas) {
//        ceDevK6Proxy.dealTrueSleepData(payload);

//        int allSleepItems = ByteUtil.byte2ToInt(new byte[]{datas[0], datas[1]});

//        datas = new byte[]{3, 0, 3, 4, 1, -48, -110, 86, 100, 2, -112, -106, 86, 100, 3, -111, -106, 86, 100, 4, 45, -96, 86, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 1, 40, -33, 87, 100, 2, -100, -29, 87, 100, 3, -12, -27, 87, 100, 2, -91, -22, 87, 100, 3, -31, -22, 87, 100, 2, 20, -13, 87, 100, 3, 64, -12, 87, 100, 4, 5, 18, 88, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 1, 96, 31, 88, 100, 2, -43, 35, 88, 100, 3, -120, 36, 88, 100, 4, -4, 40, 88, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        int allItem = datas[2] & 0xff;
        int index = 3;
        try {
            Lg.e("TYPE_SLEEP", "设备端上报的睡眠源数据=" + Arrays.toString(datas), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int tSleepType = 0;
        int tSleepTime = 0;
        byte[] sleepTimeData = new byte[4];
        ArrayList<K6_Sleep> sleeps = new ArrayList();

        for (int j = 0; j < allItem; j++) {
            int items = datas[index++] & 0xff; //总条目数

            for (int i = 0; i < 15; i++) {
                tSleepType = (datas[index++] & 0xff);
                System.arraycopy(datas, index, sleepTimeData, 0, 4);
                tSleepTime = ByteUtil.byte4ToInt(sleepTimeData);
                K6_Sleep tTrueSleep = new K6_Sleep(tSleepType, tSleepTime);
                index += 4;
                //入睡时间与当前时间超过3天表示数据不对，丢弃
//                if (i < items && System.currentTimeMillis() - tSleepTime * 1000L < 3 * 24 * 60 * 60 * 1000) {
//                    sleeps.add(tTrueSleep);
//                }
                sleeps.add(tTrueSleep);
//                if (tSleepType == K6_Sleep.SLEEP_START && System.currentTimeMillis() - tSleepTime * 1000L > 3 * 24 * 60 * 60 * 1000) {
//                    break;
//                }
            }
            try {
                Lg.e("TYPE_SLEEP", "sleep 睡眠总段数:" + allItem + "  items:" + items + "  当前段数:" + j + "  设备睡眠数据:" + sleeps.toString() + "  datas=" + Arrays.toString(datas), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Lg.e("TYPE_SLEEP", "睡眠数据：" + new Gson().toJson(sleeps), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sleeps != null) {
            k6_sleepData.getK6SleepData().clear();
//            k6_sleepData.getK6_sleeps().clear();
//            k6_sleepData.setK6_sleeps(sleeps);//未处理前的原始数据

            ArrayList<K6_Sleep> arrayList = new ArrayList<>();
            K6_Sleep k6_sleep = null;
            for (int i = 0; i < sleeps.size(); i++) {
                k6_sleep = sleeps.get(i);
                if (k6_sleep.getSleepType() == K6_Sleep.SLEEP_START) {
                    arrayList = new ArrayList<>();
                    arrayList.add(k6_sleep);
                } else if (k6_sleep.getSleepType() == K6_Sleep.SLEEP_WAKEUP) {
                    arrayList.add(k6_sleep);
                    processSleepData(arrayList);
                    try {
                        if (k6_sleepData.getK6SleepData() != null && k6_sleepData.getK6SleepData().size() > 0) {
                            k6_sleepData.getK6SleepData().get(k6_sleepData.getK6SleepData().size() - 1).setK6_sleeps(arrayList);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    arrayList.add(k6_sleep);
                }
            }
        }
        try {
            Lg.e("睡眠数据2：" + new Gson().toJson(k6_sleepData));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return k6_sleepData;
    }


    private void processSleepData(ArrayList<K6_Sleep> sleepStateStructs) {

        if (sleepStateStructs != null && sleepStateStructs.size() > 0) {
            K6_sleepData tSleepData = new K6_sleepData();
//            tSleepData.setUserId(SharedPreferenceUtils.getInstance().getUserId() + "");
            tSleepData.setUploadTime(0);
            int lightTime = 0;
            int deepTime = 0;
            int sleepTime = 0;
            int movementTime = 0;
            long startSecs = 0;
            long endSecs = 0;
            long startTime = 0;
            long endTime = 0;
//            String tDateStr = DateTimeUtil
//                    .getFormatDate(new Date(
//                            sleepStateStructs.get(sleepStateStructs.size() - 1)
//                                    .getSleepTime() * 1000L));
//            Lg.e("qob", "tDateStr DateTimeUtil.getFormatDate " + tDateStr + " time "
//                    + sleepStateStructs.get(0).getSleepTime() * 1000L);
            K6_Sleep lastE = null;
            int state = CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_LIGHT;
            JSONArray tSleepDetailJsonArr = new JSONArray();
            JSONArray tOriginalSleepDetailJsonArr = new JSONArray();
            for (K6_Sleep entity : sleepStateStructs) {
                if (lastE == null) {
                    lastE = entity;
                    startSecs = entity.getSleepTime() * 1000L;
                    startTime = entity.getSleepTime();
                    continue;
                }
                state = lastE.getSleepType();
                if (state == CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_START)
                    state = CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_LIGHT;
                int keep = (int) ((entity.getSleepTime() - lastE.getSleepTime()) / 60);
                if (state == CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_LIGHT) {
                    lightTime += keep;
                } else if (state == CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_DEEP) {
                    deepTime += keep;
                } else if (state == CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_MOVEMENT) {
                    movementTime += keep;
                }
                JSONObject ob = new JSONObject();
                try {
                    ob.put(state + "", keep);
                    tSleepDetailJsonArr.put(ob);
                } catch (JSONException e) {
                }
                if (entity.getSleepType() == CEBC.SLEEPSTATUS.SLEEPDATA_SLEEP_WAKEUP) {
                    endTime = entity.getSleepTime();
                    endSecs = entity.getSleepTime() * 1000;
                    sleepTime = (int) ((endSecs - startSecs) / 1000 / 60);
                }
                lastE = entity;
                JSONObject originalSleep = new JSONObject();
                try {
                    originalSleep.put(state + "", entity.getSleepTime() * 1000L);
                    tOriginalSleepDetailJsonArr.put(originalSleep);
                } catch (JSONException e) {
                }
            }
            tSleepData.setDeepTime(deepTime);
            tSleepData.setLightTime(lightTime);
            tSleepData.setMovementTime(movementTime);
            tSleepData.setStartTime(startTime);
            tSleepData.setEndTime(endTime);
            tSleepData.setSleepTime(sleepTime);
            tSleepData.setSleepDetail(tSleepDetailJsonArr.toString());
            //tSleepData.setOriginalSleepDetail(tOriginalSleepDetailJsonArr.toString());
            //如果结束时间小于今晚8点 且 大于等于结束时间昨晚8点，即为今天的睡眠数据
            if (tSleepData.getEndTime() * 1000l < (TimeUtil.long2longDayStart(tSleepData.getEndTime() * 1000l) + 20 * 60 * 60 * 1000l) && tSleepData.getEndTime() * 1000l >= TimeUtil.long2longDayStart(tSleepData.getEndTime() * 1000l) - TimeUtil.SLEEP_TIME_OFFSET) {
                tSleepData.setSleepDay(TimeUtil.long2longDayStart(tSleepData.getEndTime() * 1000l) / 1000l);
            } else if (tSleepData.getEndTime() * 1000l >= TimeUtil.long2longDayStart(tSleepData.getEndTime() * 1000l) + 20 * 60 * 60 * 1000l) {
                //如果结束时间大于等于结束当天晚8点,即为第2天睡眠数据
                tSleepData.setSleepDay(TimeUtil.long2longDayStart((tSleepData.getEndTime() * 1000l) + (24 * 60 * 60 * 1000l)) / 1000l);
            }
            tSleepData.setDevId(BleFactory.getInstance().getK6Proxy().getBlueAddress());
            k6_sleepData.getK6SleepData().add(tSleepData);
        }
    }
}