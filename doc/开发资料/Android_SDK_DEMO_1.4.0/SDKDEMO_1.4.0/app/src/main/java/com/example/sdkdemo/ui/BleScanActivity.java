package com.example.sdkdemo.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sdkdemo.BluetoothManager;
import com.example.sdkdemo.R;
import com.example.sdkdemo.SPUtils;
import com.example.sdkdemo.data.BleDeviceItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ce.com.cenewbluesdk.entity.MyBleDevice;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.proxy.interfaces.OnScanDevListener;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;

/**
 * Author:null
 * Date:2026/8/18 18:23
 * Description:添加您的描述
 */
public class BleScanActivity extends AppCompatActivity {

    private RecyclerView rvDevices;
    private TextView tvScanStatus;
    private TextView tvEmpty;
    private TextView btnScan;

    private BleDeviceAdapter adapter;

    private boolean isScanning = false;

    private String devName, devMac, devId;
    private final List<MyBleDevice> deviceList = new ArrayList<>();

    OnScanDevListener scanDevListener = new OnScanDevListener() {
        @Override
        public void onFindDev(ScanResult scanRecord) {
        }

        @Override
        public void onFindDevList(ScanResult scanRecord, List<MyBleDevice> deviceList, MyBleDevice myBleDevice) {
            Message msg = Message.obtain();
            msg.what = 1000;
            msg.obj = deviceList;
//            mHandler.sendMessage(msg);
            adapter.setData(deviceList);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        initView();
        requestBlePermission();
        //Registration status monitoring
        BluetoothManager bluetoothManager = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(BluetoothManager.class);
        bluetoothManager.registerBLE();

        // Initialize scan
        BluetoothHelper.getInstance().scanningDeviceInit(this, scanDevListener);


        //Connection status update
        BluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    FitLoader.stopLoading(BleScanActivity.this);
                    Bundle bundle = new Bundle();
                    if (!TextUtils.isEmpty(devMac)) {
                        BluetoothHelper.getInstance().getPersistent().setBlueAddress(devMac);
                    } else {
                        bundle.putString("address", SPUtils.getString("address", ""));
                    }
                    if (!TextUtils.isEmpty(devName)) {
                        BluetoothHelper.getInstance().getPersistent().setDevName(devName);
                    } else {
                        bundle.putString("name", SPUtils.getString("name", ""));
                    }
                    if (!TextUtils.isEmpty(devId)) {
                        BluetoothHelper.getInstance().getPersistent().setDeviceId(devId);
                    } else {
                        bundle.putString("devId", SPUtils.getString("devId", ""));
                    }
                    Intent intent = new Intent(BleScanActivity.this, SdkDemoActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    finish();
                }

            }
        });
        if (BluetoothHelper.getInstance().reConnectDirect()) {
            FitLoader.showLoading(this, getResources().getString(R.string.connecting_back));
        } else {
            FitLoader.stopLoading(this);
        }
    }

    private void initView() {

        rvDevices =
                findViewById(R.id.rv_devices);

        tvScanStatus =
                findViewById(R.id.tv_scan_status);

        tvEmpty =
                findViewById(R.id.tv_empty);

        btnScan =
                findViewById(R.id.btn_scan);

        findViewById(R.id.iv_back)
                .setOnClickListener(v -> finish());

        adapter = new BleDeviceAdapter();

        rvDevices.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvDevices.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            stopScan();
            BluetoothHelper.getInstance().disConnect();
            BluetoothDevice dev = item.getmBluetoothDevice();
            devName = dev.getName();
            devMac = dev.getAddress();
            devId = item.getmK6ManufacturerInfo().name;
            pairing(devMac);
        });

        btnScan.setOnClickListener(v -> {

            if (isScanning) {
                stopScan();
            } else {
                startScan();
            }
        });
    }

    private void startScan() {

        isScanning = true;

        deviceList.clear();

        adapter.setData(deviceList);

        tvEmpty.setVisibility(View.GONE);

        tvScanStatus.setText(
                "Scanning for nearby Bluetooth devices..."
        );

        btnScan.setText(
                "Stop scanning"
        );

        // TODO: Call your BLE SDK to start scanning.
        BluetoothHelper.getInstance().startScan();
    }

    private void stopScan() {

        isScanning = false;
        // TODO: Call your BLE SDK to stop scanning.
        BluetoothHelper.getInstance().stopScan();
        tvScanStatus.setText(
                "Scan complete," + deviceList.size() + " devices detected."
        );

        btnScan.setText(
                "Start scanning"
        );

        if (deviceList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void pairing(String devMac) {
        FitLoader.showLoading(this, getString(R.string.connecting));
        BluetoothHelper.getInstance().connectDev(devMac, "");
    }

    @Override
    protected void onDestroy() {
        stopScan();
        super.onDestroy();
    }

    public boolean requestBlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                        }, 1);
                return false;
            } else {
                return true;
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            } else {
                return true;
            }
        }

    }
}
