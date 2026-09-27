package com.coolwear.bluesdk.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.bean.CheckDataTypeBean;
import com.coolwear.bluesdk.databinding.ActivityDataCollectionDetailsBinding;
import com.coolwear.bluesdk.tcp.BatteryInfoBean;
import com.coolwear.bluesdk.tcp.ConnectionManager;
import com.coolwear.bluesdk.tcp.GsonSensorBean;
import com.coolwear.bluesdk.tcp.HeartRateBean;
import com.coolwear.bluesdk.tcp.SendMessageBean;
import com.coolwear.bluesdk.tcp.SendResultHandle;
import com.coolwear.bluesdk.tcp.SessionManager;
import com.coolwear.bluesdk.tcp.TcpClientService;
import com.coolwear.bluesdk.tcp.UserInfo;
import com.coolwear.bluesdk.utils.ToastUtil;
import com.gyf.immersionbar.ImmersionBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.k6.DateTimeUtil;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_MixSportType;
import ce.com.cenewbluesdk.entity.k6.K6_SEND_APP_SPORT_STRUCT;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_MOTION_DATA;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.JsonUtil;
import ce.com.cenewbluesdk.uitl.TimeUtil;

/**
 * 数据采集详情
 */
public class DataCollectionDetailsActivity extends AppCompatActivity implements SendResultHandle, BluetoothManager.DataCallBack {
    private ActivityDataCollectionDetailsBinding binding;
    private Activity mActivity;
    private boolean gsIsCheck;
    private int serverType = 1;
    private CheckDataTypeBean bean;
    private Bundle bundle;
    private int sendMessageCount;
    MessageBroadcastReceiver receiver = new MessageBroadcastReceiver();
    private boolean isOpenHeart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        registerBroadcast();
        ImmersionBar.with(this).statusBarDarkFont(true).init();
        mActivity = this;
        binding = ActivityDataCollectionDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bundle = getIntent().getExtras();
        BluetoothManager.dataCallBack = this;
        if (bundle != null) {
            gsIsCheck = bundle.getBoolean(TcpClientService.GS_IS_CHECKED_KEY);
            serverType = bundle.getInt(TcpClientService.SERVER_TYPE_KEY);
            bean = (CheckDataTypeBean) bundle.getSerializable(TcpClientService.CHECK_DATA_TYPE_KEY);
        }
        if (gsIsCheck) {
            binding.btnStartHr.setVisibility(View.GONE);
            binding.btnEndHr.setVisibility(View.GONE);
        }
        //  CheckDataTypeBean vv = JsonUtil.jsonToBean(JsonUtil.beanToJson(bean), CheckDataTypeBean.class);
        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(gsIsCheck ? 1 : 0);
        binding.includeTitle.tvCenter.setText("数据详情");
        binding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n");
        binding.tvUserName.setText("user：" + bean.getUserName());
        binding.tvStartTime.setText("开始时间：" + DateTimeUtil.getCurrDateTimeStr_CN());
        binding.tvDevVersion.setText("固件版本：" + CEBlueSharedPreference.getSoftwareVersion());
//        startShakeByView(binding.ivUp);
        binding.includeTitle.tvLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        binding.tvInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SendMessageBean body = new SendMessageBean(SendMessageBean.UserInfoData, JsonUtil.beanToJson(new UserInfo(bean.getUserName())));
//                SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));

                GsonSensorBean gs = new GsonSensorBean((short) 5, (short) 6, (short) 7);
                SendMessageBean body = new SendMessageBean(SendMessageBean.GsensorData, JsonUtil.beanToJson(gs));
                SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
            }
        });

        binding.tvConnectStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsgFail();
            }
        });

        binding.btnStartHr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gsIsCheck) {
                    BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(0);
                }
                isOpenHeart = true;
//                if(!isOpenHeart){
//                    isOpenHeart = true;
//                    binding.btnG.setText("心率测试中");
//                    BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.OPEN);
//                }else{
//                    binding.btnG.setText("开始心率测试");
//                    isOpenHeart = false;
//                    BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.CLOSE);
//                }
//                //executeAdbCommand("shell input swipe  540 1300 540 500 100");
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_START, 23, 32);
            }
        });

        binding.btnEndHr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isOpenHeart = false;
                BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                        K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
            }
        });
        //连接状态
        BluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                            /*
             Device connection status.
                STATUS           VALUE
                disconnected      0       K6_Action.RCVD.BLUE_DISCONNECT
                connected         1       K6_Action.RCVD.BLUE_CONNECTED
                connecting        2       K6_Action.RCVD.BLUE_CONNECTING
            */
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    binding.tvStatus.setText("蓝牙连接状态：已连接");
                    if (gsIsCheck) {
                        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
                    }
                } else if (integer == K6_Action.RCVD.BLUE_CONNECTING) {
                    binding.tvStatus.setText("蓝牙连接状态：蓝牙连接中...");
                } else {
                    binding.tvStatus.setText("蓝牙连接状态：已断开");
                }
            }
        });
//        BluetoothManager.gSensorLiveData.observe(this, new Observer<k6_DATA_TYPE_MOTION_DATA>() {
//            @Override
//            public void onChanged(k6_DATA_TYPE_MOTION_DATA dataTypeMotionData) {
//                if (bean != null && bean.isGsIsCheck()) {
//                    GsonSensorBean gs = new GsonSensorBean((short) dataTypeMotionData.getX_Move_Distance(), (short) dataTypeMotionData.getY_Move_Distance(), (short) dataTypeMotionData.getSpeed_Throw());
//                    SendMessageBean body = new SendMessageBean(SendMessageBean.GsensorData, JsonUtil.beanToJson(gs));
//                    SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
//                }
//                binding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n" + dataTypeMotionData.getSpeed_Throw() + "," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance());
//            }
//        });

        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
            @Override
            public void onChanged(List<K6_Sport> sportData) {
                if (sportData != null && sportData.size() > 0) {
                    binding.tvStep.setText("步数:  " + sportData.get(sportData.size() - 1).getWalkSteps() + "步");
                    binding.tvDistance.setText("距离:  " + sportData.get(sportData.size() - 1).getDistance() + "米");
                    binding.tvCalories.setText("卡路里:  " + sportData.get(sportData.size() - 1).getCalories() / 1000 + "千卡");
//                    String s = gSenSorBinding.tvStep.getText().toString();
//                    String d = gSenSorBinding.tvDistance.getText().toString();
//                    String c = gSenSorBinding.tvCalories.getText().toString();
//                    Lg.e_0316("BluetoothManager", dataTypeMotionData.getSpeed_Throw() +"," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance()  ,"GsenSor.log",true);

                }

            }
        });

        binding.tvBattery.setText("电量：" + CEBlueSharedPreference.getKEY_DEVICEBATTERY() + "%");
    }

    @Override
    public void onBackPressed() {
        //  ToastUtil.showToast(mActivity,"确认要退出数据采集吗？");
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (BluetoothHelper.getInstance().isConnectOk()) {
            BluetoothHelper.getInstance().setEnableGsDataTrans(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gsIsCheck) {
            BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(0);
        }
        BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.CLOSE);
        BluetoothHelper.getInstance().getSendBlueData().sendAppSport(
                K6_MixSportType.APP_SPORT_TYPE_TREADMILL, K6_SEND_APP_SPORT_STRUCT.SPORT_STATUS_STOP, 0, 0);
        unregisterBroadcast();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopService(new Intent(this, TcpClientService.class));
    }

    @Override
    public void sendMsgOk(Object msg) {
        if (msg instanceof String && ((String) msg).contains("Heartbeat") && !isOpenHeart) {
            if (gsIsCheck) {
                BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
            }
        } else {
            sendMessageCount++;
            if (System.currentTimeMillis() - reconnectTime > 1 * 1000) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.tvSendMessageCount.setText(sendMessageCount + "");
                    }
                });

                reconnectTime = System.currentTimeMillis();
            }
        }
    }

    private long reconnectTime;

    @Override
    public void sendMsgFail() {
        if (System.currentTimeMillis() - reconnectTime > 10 * 1000) {
            Intent tcpService = new Intent(mActivity, TcpClientService.class);
            tcpService.putExtras(bundle);
            startService(tcpService);
            reconnectTime = System.currentTimeMillis();
        }
    }

    @Override
    public void connectStatus(boolean isConnect) {
        if (!isConnect) {
            Intent tcpService = new Intent(mActivity, TcpClientService.class);
            tcpService.putExtras(bundle);
            startService(tcpService);
        }
    }

    @Override
    public void updateMotion(k6_DATA_TYPE_MOTION_DATA dataTypeMotionData) {
        if (bean != null && bean.isGsIsCheck()) {
            List<k6_DATA_TYPE_MOTION_DATA> list = dataTypeMotionData.getList();
            List<GsonSensorBean> bodyList = new ArrayList<>();
            for (k6_DATA_TYPE_MOTION_DATA data : list) {
                GsonSensorBean bean = new GsonSensorBean((short) data.getX_Move_Distance(), (short) data.getY_Move_Distance(), (short) data.getSpeed_Throw());
                bodyList.add(bean);
            }
            JSONArray array = JSONArray.parseArray(JSON.toJSONString(bodyList));
            JSONObject object = new JSONObject();
            object.put("DataTime", DateTimeUtil.getCurrDateTimeStrMill());
            object.put("GsensorDatas", array);
            SendMessageBean body = new SendMessageBean(SendMessageBean.GsensorData, object.toJSONString());
            SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
        }
        binding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n" + "X=" + dataTypeMotionData.getX_Move_Distance() + "   Y=" + dataTypeMotionData.getY_Move_Distance() + "  Z=" + dataTypeMotionData.getSpeed_Throw());
    }

    @Override
    public void updateHeartRate(List<K6_HeartStruct> result) {
        String str = "";
        // binding.btnG.setText("开始心率测试");
        isOpenHeart = false;
        for (K6_HeartStruct heartStruct : result) {
            str = heartStruct.getHeartNums() + "";
            if (bean != null && bean.isHrIsCheck()) {
                SendMessageBean body = new SendMessageBean(SendMessageBean.HeartRateData, JsonUtil.beanToJson(new HeartRateBean(TimeUtil.long2String(heartStruct.getTime()), heartStruct.getHeartNums())));
                SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));

            }
        }
        binding.tvHr.setText("心率：" + str);
        if (gsIsCheck) {
            BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
        }
    }

    @Override
    public void updateBattery(Integer integer) {
        binding.tvBattery.setText("电量：" + integer + "%");
        if (bean != null && bean.isBatteryIsCheck()) {
            SendMessageBean body = new SendMessageBean(SendMessageBean.BatteryData, JsonUtil.beanToJson(new BatteryInfoBean(0, integer)));
            SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
        }
    }

    //receive message and update ui
    private class MessageBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(SessionManager.TAG, "接收到回调");
            String action = intent.getAction();
            if (ConnectionManager.BROADCAST_ACTION.equals(action)) {
                String message = "";
                if (intent.hasExtra(ConnectionManager.MESSAGE)) {
                    message = intent.getStringExtra(ConnectionManager.MESSAGE);
                    Log.e(SessionManager.TAG, "接收到的消息是：" + message);
                }
                if (intent.hasExtra(ConnectionManager.CONNECT)) {
                    String connectStatus = intent.getStringExtra(ConnectionManager.CONNECT);
                    if ((ConnectionManager.CONNECT_SUCC_CODE + "").equals(connectStatus)) {
                        binding.tvConnectStatus.setText("服务器连接状态:已连接");
                        SessionManager.getInstance().sendHeart();
                        Log.e(SessionManager.TAG, "发送用户消息");
                        SendMessageBean body = new SendMessageBean(SendMessageBean.UserInfoData, JsonUtil.beanToJson(new UserInfo(bean.getUserName())));
                        SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
                    } else {
                        binding.tvConnectStatus.setText("服务器连接状态: 已断开");
                        ToastUtil.showToast(context, "连接服务器失败：" + message);
//                        Intent tcpService = new Intent(mActivity, TcpClientService.class);
//                        tcpService.putExtras(bundle);
//                        startService(tcpService);
                    }
                }
            }
        }

    }

    public void registerBroadcast() {
        IntentFilter filter = new IntentFilter(ConnectionManager.BROADCAST_ACTION);
        registerReceiver(receiver, filter);
        SessionManager.getInstance().setHandle(this);
    }

    public void unregisterBroadcast() {
        unregisterReceiver(receiver);
    }

    private void test() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                SendMessageBean body = new SendMessageBean(SendMessageBean.UserInfoData, JsonUtil.beanToJson(new UserInfo(bean.getUserName())));
//                SessionManager.getInstance().writeToServer(JsonUtil.beanToJson(body));
//                test();
//            }
//        }, 3 * 1000);
    }

//    private void startShakeByView(View view) {
//        //从左向右
//        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "translationY", -50, 50);
//        objectAnimator.setDuration(500);
//        objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
//        objectAnimator.setRepeatCount(30);
//        objectAnimator.start();
//    }

    public static String executeAdbCommand(String command) {
        try {
            // 创建运行时实例
            Runtime runtime = Runtime.getRuntime();
            // 执行adb命令
            Process process = runtime.exec("adb " + command);
            // 读取命令执行结果
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            // 等待命令执行完成
            process.waitFor();
            // 关闭流
            reader.close();
            // 返回执行结果
            return output.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
