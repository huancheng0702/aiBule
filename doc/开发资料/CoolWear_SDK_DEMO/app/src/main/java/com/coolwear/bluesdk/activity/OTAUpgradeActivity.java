package com.coolwear.bluesdk.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.bean.ClockDialData;
import com.coolwear.bluesdk.net.GeneralObserver;
import com.coolwear.bluesdk.net.HttpInterface;
import com.coolwear.bluesdk.net.RetrofitFactory;
import com.coolwear.bluesdk.utils.FileUtils2;
import com.coolwear.bluesdk.view.DownloadProgressDialog;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/26 0026 12:03
 */
public class OTAUpgradeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "OTAUpgradeActivity";
    private Button btn_choose_file,btn_ota;
    private TextView tv_file_name,tv_file_md5,tv_softVersion;
    private String otaFilePath;
    private DownloadProgressDialog progressDialog;
    private static final int MESSAGE_TYPE_COUNT_CONTINUE = 10000;
    private static final int MESSAGE_TYPE_COUNT_STOP = 10001;
    private int timeCount;
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
        setContentView(R.layout.activity_otaupgrade);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btn_choose_file = findViewById(R.id.btn_choose_file);
        tv_file_name = findViewById(R.id.tv_file_name);
        tv_file_md5 = findViewById(R.id.tv_file_md5);
        btn_ota = findViewById(R.id.btn_ota);
        tv_softVersion = findViewById(R.id.tv_softVersion);

        tv_softVersion.setText("Software version:"+BluetoothHelper.getInstance().getPersistent().getSoftwareVersion());
        btn_choose_file.setOnClickListener(this);
        btn_ota.setOnClickListener(this);

        BluetoothManager.otaFileProgress.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                Log.i(TAG, "进度 " + progress);
                if (progressDialog == null) {
                    progressDialog = new DownloadProgressDialog(OTAUpgradeActivity.this);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setTitle("OTA...");
                    // progress 为int类型，为了能够获取足够的精度，把数值扩大100倍
                    progressDialog.setMax(10000);
                }
                if (progress < 100) {
                    progressDialog.setProgress((int) (progress * 100));
                    progressDialog.show();
                } else {
                    progressDialog.dismiss();
                    handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
                    //BluetoothHelper.getInstance().disConnect();
                    BluetoothManager.otaFileProgress.setValue(0);
                    finish();
                    Toast.makeText(OTAUpgradeActivity.this, "OTA Success!", Toast.LENGTH_LONG).show();
                }
            }
        });
        BluetoothManager.connectStatus.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer == K6_Action.RCVD.BLUE_DISCONNECT) {
                    BluetoothHelper.getInstance().disConnect();
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
                }

            }
        });
    }
    private void readFile() {
        new ChooserDialog(this)
//                .withFilter(false, "")
//                .withStartFile(FileUtils.getSDPath(MainActivity.this)) //FileUtils.getSDPath()+"/"+"YFit-OTA"    getFilesDir().getAbsolutePath()
                .withStartFile(getExternalFilesDir("IMG_FILE").getPath())
                .withChosenListener((String path, File pathFile) -> {
                    otaFilePath = path;
                    String fileName = path.substring(path.lastIndexOf("/") + 1);
                    tv_file_name.setText(getString(R.string.file_name) + ": " + fileName);
                    tv_file_md5.setText(getString(R.string.md5) + ": " + FileUtils2.getFileMD5(pathFile));
                    Toast.makeText(OTAUpgradeActivity.this, "FILE: " + pathFile, Toast.LENGTH_SHORT).show();
                })
                // to handle the back key pressed or clicked outside the dialog:
                .withOnCancelListener((DialogInterface dialog) -> {
                    Log.d("CANCEL", "CANCEL");
                    dialog.cancel(); // MUST have
                })
                .build()
                .show();
    }

    private void otaUpgrade() {
        if (otaFilePath != null) {
            try {
                BluetoothHelper.getInstance().getTransfer().otaUpgrade(otaFilePath,"1","1");
                handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_CONTINUE);
            } catch (Exception e) {
                handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
                e.printStackTrace();
            }
        } else {
            handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
            Toast.makeText(this, "Can't read OTA file.", Toast.LENGTH_SHORT).show();
        }
    }
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MESSAGE_TYPE_COUNT_CONTINUE) {
                if (progressDialog != null) {
                    int hours, minutes, seconds;
                    hours = timeCount / (60 * 60);
                    minutes = (timeCount / 60) % 60;
                    seconds = timeCount % (60);
                    progressDialog.setSubText(String.format("Time  %02d:%02d:%02d", hours, minutes, seconds));
                }
                handler.sendEmptyMessageDelayed(MESSAGE_TYPE_COUNT_CONTINUE, 1000);
                timeCount++;

            } else if (msg.what == MESSAGE_TYPE_COUNT_STOP) {
                handler.removeMessages(MESSAGE_TYPE_COUNT_CONTINUE);
                timeCount = 0;
            }
            return false;
        }
    });
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_choose_file:
                readFile();
                break;
            case R.id.btn_ota:
                otaUpgrade();
                break;
        }

    }


}
