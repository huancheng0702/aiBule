package com.coolwear.bluesdk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.BuildConfig;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.utils.FitLoader;
import com.coolwear.bluesdk.utils.SPUtils;
import com.coolwear.bluesdk.view.DeviceListAdapter;
import com.coolwear.bluesdk.view.ScanDeviceListAdapter;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.entity.MyBleDevice;
import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.proxy.interfaces.OnScanDevListener;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/17 0017 11:00
 */
public class ScanningActivity extends AppCompatActivity {
    private static final String TAG = "ScanningActivity";
    private Button scan;
    private Button btn_stopScan;
    private Button btn_download;
    private RecyclerView recyclerView;
    private ScanDeviceListAdapter adapter;
    private String devName, devMac, devId;
    private BluetoothManager bluetoothManager;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    List<MyBleDevice> deviceList = (List<MyBleDevice>) msg.obj;
                    adapter.setList(deviceList);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);
        Logger.i("连接状态=" + BluetoothHelper.getInstance().isConnectOk());
        scan = findViewById(R.id.btn_scan);
        btn_stopScan = findViewById(R.id.btn_stopScan);
        recyclerView = findViewById(R.id.recyclerView);
        btn_download = findViewById(R.id.btn_download);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        adapter = new ScanDeviceListAdapter(this);
        recyclerView.setAdapter(adapter);
//        bluetoothManager = ViewModelProviders.of(this).get(BluetoothManager.class);
        bluetoothManager = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(BluetoothManager.class);
        bluetoothManager.initBLE();
        // CEBlueSharedPreference.setDevUUid("");
        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothHelper.getInstance().stopScan();
            }
        });
        //初始化扫描 Initialize scan
        BluetoothHelper.getInstance().scanningDeviceInit(this, scanDevListener);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isOk = BluetoothHelper.getInstance().isConnectOk();
                adapter.setList(new ArrayList<>());
//                BluetoothHelper.getInstance().stopScan();
                BluetoothHelper.getInstance().startScan();
            }
        });
        btn_stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothHelper.getInstance().stopScan();
            }
        });
        if (BuildConfig.BUILDTYPE == 2) {
            btn_download.setVisibility(View.VISIBLE);
        } else {
            btn_download.setVisibility(View.GONE);
        }
        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScanningActivity.this, UpgradeOtaActivity.class);
                startActivity(intent);
            }
        });
        adapter.setOnItemClickListener(new ScanDeviceListAdapter.ItemClickListener() {
            @Override
            public void onItemClickListener(int position) {
                BluetoothHelper.getInstance().stopScan();
                BluetoothHelper.getInstance().disConnect();
                BluetoothDevice dev = adapter.getList().get(position).getmBluetoothDevice();
                devName = dev.getName();
                devMac = dev.getAddress();
                devId = adapter.getList().get(position).getmK6ManufacturerInfo().name;
                pairing(devMac, devName);
            }
        });


        bluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Log.i(TAG, "status=" + integer);
                if (integer == K6_Action.RCVD.BLUE_CONNECTED) {
                    FitLoader.stopLoading(ScanningActivity.this);
                    Bundle bundle = new Bundle();
                    if (!TextUtils.isEmpty(devMac)) {
                        bundle.putString("address", devMac);
                        SPUtils.put("address", devMac);
                        BluetoothHelper.getInstance().getPersistent().setBlueAddress(devMac);
//                        CEBlueSharedPreference.setDevAddress(devMac);
                    } else {
                        bundle.putString("address", SPUtils.getString("address", ""));
                    }
                    if (!TextUtils.isEmpty(devName)) {
                        bundle.putString("name", devName);
                        SPUtils.put("name", devName);
                        BluetoothHelper.getInstance().getPersistent().setDevName(devName);
                    } else {
                        bundle.putString("name", SPUtils.getString("name", ""));
                    }
                    if (!TextUtils.isEmpty(devId)) {
                        bundle.putString("devId", devId);
                        SPUtils.put("devId", devId);
                        BluetoothHelper.getInstance().getPersistent().setDeviceId(devId);
                    } else {
                        bundle.putString("devId", SPUtils.getString("devId", ""));
                    }
                    if (BuildConfig.BUILDTYPE == 2) {
                        Intent intent = new Intent(ScanningActivity.this, UpgradeOtaActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(ScanningActivity.this, MainActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }

                }

            }
        });
        if (BluetoothHelper.getInstance().reConnectDirect()) {
            FitLoader.showLoading(this, getResources().getString(R.string.connecting_back));
        } else {
            FitLoader.stopLoading(this);
        }
    }

    private void pairing(String devMac, String devName) {
        FitLoader.showLoading(this, getString(R.string.connecting));
        BluetoothHelper.getInstance().connectDev(devMac, "");
//        BluetoothHelper.getInstance().connectDev(devMac, devName);//测试过滤的设备

//        BluetoothHelper.getInstance().connectDev(devMac);
    }

    OnScanDevListener scanDevListener = new OnScanDevListener() {
        @Override
        public void onFindDev(ScanResult scanRecord) {
        }

        @Override
        public void onFindDevList(ScanResult scanRecord, List<MyBleDevice> deviceList, MyBleDevice myBleDevice) {
            Message msg = Message.obtain();
            msg.what = 1000;
            msg.obj = deviceList;
            mHandler.sendMessage(msg);
//            adapter.setList(deviceList);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        requesttakePhotoPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FitLoader.stopLoading(this);
    }

    public boolean requesttakePhotoPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
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

    public class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public WrapContentLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
