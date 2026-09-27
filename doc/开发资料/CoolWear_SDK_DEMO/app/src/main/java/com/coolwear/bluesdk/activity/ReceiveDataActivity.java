package com.coolwear.bluesdk.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.Constant;
import com.coolwear.bluesdk.R;

import java.util.List;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.K6_sleepData;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_HR_CONTROL;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_BP;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_REAL_O2;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_WATCH_FACE_INFO;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HeartStruct;
import ce.com.cenewbluesdk.entity.k6.K6_HrvStruct;
import ce.com.cenewbluesdk.entity.k6.K6_Mix_sport_Struct;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.K6_TempStruct;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/25 0025 17:19
 */
public class ReceiveDataActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_heartRate, tv_heartRateName, tv_sportData, tv_sportDataName, tv_mixSportDataName,
            tv_mixSportData, tv_sleepDataName, tv_sleepData, tv_bloodOxygenName, tv_bloodOxygen,
            tv_bloodPressure, tv_bloodPressureName, tv_watchInfoName, tv_watchInfo, tv_heartRate1,
            tv_heartRate2, tv_heartRate3, tv_comType, tv_autoHeart, tv_sportHeart, tv_devInfo, tv_devInfoName,
            tv_hrvName, tv_hrv, tv_tempName, tv_temp;
    private Button btn_bodetection, btn_bpdetection, btn_getWatchInfo, btn_heart, btn_hrv;
    private boolean isOpenBO, isOpenBP, isOpenHeart, isOpenHrv;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receivedata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tv_heartRate = findViewById(R.id.tv_heartRate);
        tv_sportData = findViewById(R.id.tv_sportData);
        tv_heartRateName = findViewById(R.id.tv_heartRateName);
        tv_sportDataName = findViewById(R.id.tv_sportDataName);
        tv_mixSportDataName = findViewById(R.id.tv_mixSportDataName);
        tv_mixSportData = findViewById(R.id.tv_mixSportData);
        tv_sleepDataName = findViewById(R.id.tv_sleepDataName);
        tv_sleepData = findViewById(R.id.tv_sleepData);
        tv_bloodOxygenName = findViewById(R.id.tv_bloodOxygenName);
        tv_bloodOxygen = findViewById(R.id.tv_bloodOxygen);
        btn_bodetection = findViewById(R.id.btn_bodetection);
        btn_bpdetection = findViewById(R.id.btn_bpdetection);
        tv_bloodPressure = findViewById(R.id.tv_bloodPressure);
        tv_bloodPressureName = findViewById(R.id.tv_bloodPressureName);
        tv_watchInfoName = findViewById(R.id.tv_watchInfoName);
        btn_getWatchInfo = findViewById(R.id.btn_getWatchInfo);
        tv_watchInfo = findViewById(R.id.tv_watchInfo);
        tv_heartRate1 = findViewById(R.id.tv_heartRate1);
        tv_heartRate2 = findViewById(R.id.tv_heartRate2);
        tv_heartRate3 = findViewById(R.id.tv_heartRate3);
        tv_comType = findViewById(R.id.tv_comType);
        tv_autoHeart = findViewById(R.id.tv_autoHeart);
        tv_sportHeart = findViewById(R.id.tv_sportHeart);
        tv_devInfo = findViewById(R.id.tv_devInfo);
        tv_devInfoName = findViewById(R.id.tv_devInfoName);
        btn_heart = findViewById(R.id.btn_heart);
        tv_hrvName = findViewById(R.id.tv_HrvName);
        btn_hrv = findViewById(R.id.btn_hrv);
        tv_hrv = findViewById(R.id.tv_hrv);
        tv_tempName = findViewById(R.id.tv_tempName);
        tv_temp = findViewById(R.id.tv_temp);

        btn_bodetection.setOnClickListener(this);
        btn_bpdetection.setOnClickListener(this);
        btn_getWatchInfo.setOnClickListener(this);
        btn_heart.setOnClickListener(this);
        btn_hrv.setOnClickListener(this);

        tv_heartRateName.setText(getResources().getString(R.string.heart_rate) + "\nCMD:RCVD_SPORT_HEART_FOR_SHOW");
        tv_sportDataName.setText(getResources().getString(R.string.motion_data) + "\nCMD:RCVD_SPORT_DATA");
        tv_mixSportDataName.setText(getResources().getString(R.string.mix_motion_data) + "\nCMD:RCVD_MIX_SPORT_DATA");
        tv_sleepDataName.setText(getResources().getString(R.string.sleep_data) + "\nCMD:RCVD_K6_SLEEP_DATA");
        tv_bloodOxygenName.setText(getResources().getString(R.string.blood_oxygen) + "\nCMD:RCVD_DATA_TYPE_REAL_O2");
        tv_bloodPressureName.setText(getResources().getString(R.string.blood_pressure) + "\nCMD:RCVD_DATA_TYPE_REAL_BP");
        tv_watchInfoName.setText(getResources().getString(R.string.dial_info) + "\nCMD:RCVD_DATA_TYPE_WATCH_FACE_INFO");
        tv_comType.setText(getResources().getString(R.string.detection_instruction_type) + "\nCMD:RCVD_DATA_TYPE_HR_CONTROL");
        tv_autoHeart.setText(getResources().getString(R.string.equipment_automatic_heart) + "\nCMD:RCVD_DAILY_HEART");
        tv_sportHeart.setText(getResources().getString(R.string.exercise_heart_rate) + "\nCMD:RCVD_SPORT_HEART");
        tv_devInfoName.setText(getResources().getString(R.string.dev_info) + "\nCMD:RCVD_DEVINFO");
        tv_hrvName.setText("HRV" + "\nCMD:RCVD_SPORT_HRV_FOR_SHOW");
        tv_tempName.setText("温度" + "\nCMD:RCVD_SPORT_TEMP_FOR_SHOW");

        BluetoothManager.heartRate.observe(this, new Observer<List<K6_HeartStruct>>() {
            @Override
            public void onChanged(List<K6_HeartStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_HeartStruct heartStruct : result) {
                    stringBuilder.append(heartStruct.toString() + "\n");
                }
                if (!TextUtils.isEmpty(stringBuilder.toString())) {
                    tv_heartRate.setText(stringBuilder.toString());
                }

            }
        });
        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
            @Override
            public void onChanged(List<K6_Sport> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_Sport sport : result) {
                    stringBuilder.append(sport.toString() + "\n");
                }
                if (!TextUtils.isEmpty(stringBuilder.toString())) {
                    tv_sportData.setText(stringBuilder.toString());
                }

            }
        });

        BluetoothManager.mixSportData.observe(this, new Observer<List<K6_Mix_sport_Struct>>() {
            @Override
            public void onChanged(List<K6_Mix_sport_Struct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_Mix_sport_Struct mixSport : result) {
                    stringBuilder.append(mixSport.toString() + "\n");
                }
                if (!TextUtils.isEmpty(stringBuilder.toString())) {
                    tv_mixSportData.setText(stringBuilder.toString());
                }

            }
        });

        BluetoothManager.sleepData.observe(this, new Observer<K6_sleepData>() {
            @Override
            public void onChanged(K6_sleepData result) {
                tv_sleepData.setText(result.toString());
            }
        });
        BluetoothManager.bloodOxygenData.observe(this, new Observer<List<K6_DATA_TYPE_REAL_O2>>() {
            @Override
            public void onChanged(List<K6_DATA_TYPE_REAL_O2> result) {
                tv_bloodOxygen.setText(result.toString());
            }
        });
        BluetoothManager.bloodPressureData.observe(this, new Observer<List<K6_DATA_TYPE_REAL_BP>>() {
            @Override
            public void onChanged(List<K6_DATA_TYPE_REAL_BP> result) {
                tv_bloodPressure.setText(result.toString());
            }
        });
        BluetoothManager.watchFaceInfo.observe(this, new Observer<K6_DATA_TYPE_WATCH_FACE_INFO>() {
            @Override
            public void onChanged(K6_DATA_TYPE_WATCH_FACE_INFO result) {
                tv_watchInfo.setText(result.toString());
            }
        });
        BluetoothManager.heartRate1.observe(this, new Observer<K6_DATA_TYPE_HR_CONTROL>() {
            @Override
            public void onChanged(K6_DATA_TYPE_HR_CONTROL result) {
                tv_heartRate1.setText(result.toString());
            }
        });
        BluetoothManager.heartRate2.observe(this, new Observer<List<K6_HeartStruct>>() {
            @Override
            public void onChanged(List<K6_HeartStruct> result) {
                tv_heartRate2.setText(result.toString());
            }
        });
        BluetoothManager.heartRate3.observe(this, new Observer<List<K6_HeartStruct>>() {
            @Override
            public void onChanged(List<K6_HeartStruct> result) {
                tv_heartRate3.setText(result.toString());
            }
        });
        BluetoothManager.equipmentInfo.observe(this, new Observer<K6_DevInfoStruct>() {
            @Override
            public void onChanged(K6_DevInfoStruct devInfoStruct) {
                tv_devInfo.setText(devInfoStruct.toString());
                Constant.customId = devInfoStruct.getCustomer_id() + "";
            }
        });

        BluetoothManager.hrvLiveData.observe(this, new Observer<List<K6_HrvStruct>>() {
            @Override
            public void onChanged(List<K6_HrvStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_HrvStruct heartStruct : result) {
                    stringBuilder.append(heartStruct.toString() + "\n");
                }
                if (!TextUtils.isEmpty(stringBuilder.toString())) {
                    tv_hrv.setText(stringBuilder.toString());
                }

            }
        });


        BluetoothManager.tempLiveData.observe(this, new Observer<List<K6_TempStruct>>() {
            @Override
            public void onChanged(List<K6_TempStruct> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (K6_TempStruct tempStruct : result) {
                    stringBuilder.append(tempStruct.toString() + "\n");
                }
                if (!TextUtils.isEmpty(stringBuilder.toString())) {
                    tv_temp.setText(stringBuilder.toString());
                }

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bodetection://血氧检测
                if (!isOpenBO) {
                    isOpenBO = true;
                    BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.OPEN);
                } else {
                    isOpenBO = false;
                    BluetoothHelper.getInstance().getSendBlueData().sendBloodOxygenDetection(CEBC.OPENSTATUS.CLOSE);
                }
                break;
            case R.id.btn_bpdetection://血压检测
                if (!isOpenBP) {
                    isOpenBP = true;
                    BluetoothHelper.getInstance().getSendBlueData().sendBloodPressureDetection(CEBC.OPENSTATUS.OPEN);
                } else {
                    isOpenBP = false;
                    BluetoothHelper.getInstance().getSendBlueData().sendBloodPressureDetection(CEBC.OPENSTATUS.CLOSE);
                }
                break;
            case R.id.btn_getWatchInfo:
                BluetoothHelper.getInstance().getSendBlueData().sendWatchInfo();
                break;
            case R.id.btn_heart://心率检测
                if (!isOpenHeart) {
                    isOpenHeart = true;
                    BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.OPEN);
                } else {
                    isOpenHeart = false;
                    BluetoothHelper.getInstance().getSendBlueData().sendHeartRateSwitch(CEBC.OPENSTATUS.CLOSE);
                }
                break;
            case R.id.btn_hrv://hrv检测
                if (!isOpenHrv) {
                    isOpenHrv = true;
                    BluetoothHelper.getInstance().getSendBlueData().sendHRVSwitch(CEBC.OPENSTATUS.OPEN);
                } else {
                    isOpenHrv = false;
                    BluetoothHelper.getInstance().getSendBlueData().sendHRVSwitch(CEBC.OPENSTATUS.CLOSE);
                }
                break;
        }
    }
}
