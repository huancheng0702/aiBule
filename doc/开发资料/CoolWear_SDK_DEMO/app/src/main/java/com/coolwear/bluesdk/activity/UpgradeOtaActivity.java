package com.coolwear.bluesdk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.BuildConfig;
import com.coolwear.bluesdk.Constant;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.bean.DevVersionBean;
import com.coolwear.bluesdk.net.BaseEntity;
import com.coolwear.bluesdk.net.BaseUrlInterceptor;
import com.coolwear.bluesdk.net.GeneralObserver;
import com.coolwear.bluesdk.net.HttpInterface;
import com.coolwear.bluesdk.net.RetrofitFactory;
import com.coolwear.bluesdk.utils.WordUtil;
import com.coolwear.bluesdk.view.UpgradeOtaAdapter;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.entity.k6.K6_DevInfoStruct;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2022/8/8 0008 18:08
 */
public class UpgradeOtaActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UpgradeOtaAdapter upgradeOtaAdapter;
    private TextView tv_devInfo, tv_softVersion, tv_status;
    private EditText et_id;
    private EditText password;
    private AppCompatButton btn_find, btn_old, bt_findDev;
    private RadioGroup rg_net;
    private RadioButton rb_cloud, rb_local;
    private BluetoothManager bluetoothManager;
    private String address;
    private String name;
    private String devId;

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
        setContentView(R.layout.activity_upgradeota);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (BuildConfig.BUILDTYPE == 2) {
            bluetoothManager = ViewModelProviders.of(this).get(BluetoothManager.class);
            bluetoothManager.initBLE();
//            address = getIntent().getStringExtra("address");
//            name = getIntent().getStringExtra("name");
//            devId = getIntent().getStringExtra("devId");
//            BluetoothHelper.getInstance().getPersistent().getBlueAddress();
//            CEBlueSharedPreference.getDevAddress()
        }
        recyclerView = findViewById(R.id.recyclerView);
        tv_devInfo = findViewById(R.id.tv_devInfo);
        btn_find = findViewById(R.id.btn_find);
        tv_status = findViewById(R.id.tv_status);
        btn_old = findViewById(R.id.bt_old);
        et_id = findViewById(R.id.et_id);
        password = findViewById(R.id.password);
        rg_net = findViewById(R.id.rg_net);
        rb_cloud = findViewById(R.id.rb_cloud);
        rb_local = findViewById(R.id.rb_local);
        tv_softVersion = findViewById(R.id.tv_softVersion);
        bt_findDev = findViewById(R.id.bt_findDev);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        upgradeOtaAdapter = new UpgradeOtaAdapter(this);
        recyclerView.setAdapter(upgradeOtaAdapter);
//        upgradeOtaAdapter.setOnItemClickListener(new OnItemClickListener() {
//            @Override
//            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
//
//            }
//        });
        et_id.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Constant.customId = s.toString();
            }
        });
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData(et_id.getText().toString().trim(), password.getText().toString().trim());
            }
        });
        btn_old.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UpgradeOtaActivity.this, OTAUpgradeActivity.class));
            }
        });
        bt_findDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothHelper.getInstance().getSendBlueData().sendFindDevice();
            }
        });
        rb_cloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseUrlInterceptor.URLID = 1;
                upgradeOtaAdapter.setList(null);
                loadData(et_id.getText().toString().trim(), password.getText().toString().trim());
            }
        });
        rb_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseUrlInterceptor.URLID = 2;
                upgradeOtaAdapter.setList(null);
                loadData(et_id.getText().toString().trim(), password.getText().toString().trim());
            }
        });
        BluetoothManager.equipmentInfo.observe(this, new Observer<K6_DevInfoStruct>() {
            @Override
            public void onChanged(K6_DevInfoStruct devInfoStruct) {
                et_id.setText(devInfoStruct.getCustomer_id() + "");
                tv_devInfo.setText(("ID:" + BluetoothHelper.getInstance().getPersistent().getDeviceId() + "  Name:" + BluetoothHelper.getInstance().getPersistent().getDevName() + "\nMAC：" + CEBlueSharedPreference.getDevAddress()));
                tv_softVersion.setText("Firmware version:" + devInfoStruct.getSoftwareVer());
                loadData(devInfoStruct.getCustomer_id() + "", "");
            }
        });
        bluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer status) {
                switch (status) {
                    case CEBC.BLUE_CONNECTED:
                        tv_status.setText(WordUtil.getString(R.string.connected));
                        break;
                    case CEBC.BLUE_CONNECTING:
                        tv_status.setText(WordUtil.getString(R.string.connecting));
                        break;
                    case CEBC.BLUE_DISCONNECT:
                        tv_status.setText(WordUtil.getString(R.string.disconnect));
                        break;
                }

            }
        });
    }

    private void loadData(String id, String password) {
        RetrofitFactory.getInstence().API(HttpInterface.class)
                .versionList(id, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new GeneralObserver<DevVersionBean>(UpgradeOtaActivity.this, true) {
                    @Override
                    protected void onSuccess(DevVersionBean o) {
                        if (o != null && o.getVersionList() != null && o.getVersionList().size() > 0) {
                            o.getVersionList().get(0).setOpen(true);
                            upgradeOtaAdapter.setList(o.getVersionList());
                        }
                    }

                    @Override
                    protected void onCodeError(BaseEntity<DevVersionBean> t) {
                        super.onCodeError(t);
                        upgradeOtaAdapter.setList(null);
                    }

                    @Override
                    protected void onFailure(Throwable e, boolean isNetWorkError) {

                    }
                });
    }

    private void disconnect() {
        BluetoothHelper.getInstance().disConnect();
        BluetoothHelper.getInstance().getPersistent().setBlueAddress("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BuildConfig.BUILDTYPE == 2) {
            disconnect();
        }
        if (upgradeOtaAdapter != null) {
            upgradeOtaAdapter.onDestroy();
        }

    }
}
