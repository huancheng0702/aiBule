package com.coolwear.bluesdk.view;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.MyApplication;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.activity.OTAUpgradeActivity;
import com.coolwear.bluesdk.activity.UpgradeOtaActivity;
import com.coolwear.bluesdk.bean.ClockDialData;
import com.coolwear.bluesdk.bean.DevVersionBean;
import com.coolwear.bluesdk.listenters.OnDownLoadListener;
import com.coolwear.bluesdk.listenters.OnRxJavaBackProcessorListenter;
import com.coolwear.bluesdk.utils.DownloadFileUtil;
import com.coolwear.bluesdk.utils.FitLoader;
import com.coolwear.bluesdk.utils.RXJavaUtils;
import com.coolwear.bluesdk.utils.WordUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import ce.com.cenewbluesdk.entity.k6.K6_Action;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.Lg;
import ce.com.cenewbluesdk.uitl.Logger;
import ce.com.cenewbluesdk.uitl.ThreadPoolManager;
import io.reactivex.ObservableEmitter;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2022/8/8 0008 18:12
 */
public class UpgradeOtaAdapter extends CommonAdapter<DevVersionBean.VersionList> {
    private static final int DOWNLOAD_PROGRESS = 1;
    private static final int OTA_UPGRADE = 2;
    private static final int MESSAGE_TYPE_COUNT_STOP = 3;
    private int selPosition = 0;
    private int temp = 0;
    private int timeCount;
    private boolean isDownload;

    public UpgradeOtaAdapter(UpgradeOtaActivity upgradeOtaActivity) {
        super(R.layout.item_upgradeota);
        BluetoothManager.otaFileProgress.observe(upgradeOtaActivity, new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                if (!isDownload) return;
                if (progressDialog == null) {
                    progressDialog = new DownloadProgressDialog(getContext());
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                }
                progressDialog.setTitle("OTA...");
                progressDialog.setMax(10000);// progress 为int类型，为了能够获取足够的精度，把数值扩大100倍
                if (progress < 100) {
                    progressDialog.setProgress((int) (progress * 100));
                    progressDialog.show();
                } else {
                    progressDialog.dismiss();
                    handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
                    BluetoothHelper.getInstance().disConnect();
                    BluetoothManager.otaFileProgress.setValue(0);
                    Toast.makeText(getContext(), "OTA Success!", Toast.LENGTH_LONG).show();
                }
            }
        });
        BluetoothManager.connectStatus.observe(upgradeOtaActivity, new Observer<Integer>() {
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


    @Override
    protected void convert(BaseViewHolder holder, DevVersionBean.VersionList item) {
        holder.setText(R.id.tv_verName, "version:" + item.getVersion());
        try {
            if (item != null && item.getImgList() != null && item.getImgList().size() > 0) {
                holder.setText(R.id.tv_path, item.getImgList().get(0).getUrl().substring(0, item.getImgList().get(0).getUrl().lastIndexOf("/")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getItemPosition(item) == temp && temp != selPosition) {
            item.setOpen(false);
            holder.getView(R.id.recyclerView).setVisibility(View.GONE);
        }
        if (getItemPosition(item) == 0 && getItem(0).isOpen()) {
            item.setOpen(true);
            holder.getView(R.id.recyclerView).setVisibility(View.VISIBLE);
        } else {
            item.setOpen(false);
            holder.getView(R.id.recyclerView).setVisibility(View.GONE);
        }
        //将点击事件移到外面
        holder.getView(R.id.rootView).setOnClickListener(v -> {
            if (temp == selPosition) {
                if (item.isOpen()) {
                    item.setOpen(false);
                    holder.getView(R.id.recyclerView).setVisibility(View.GONE);
                } else {
                    item.setOpen(true);
                    holder.getView(R.id.recyclerView).setVisibility(View.VISIBLE);
                }
            }
            //将旧的位置保存下来
            temp = selPosition;
            //设置新的位置
            selPosition = getItemPosition(item);
            if (temp != selPosition) {
                item.setOpen(true);
                holder.getView(R.id.recyclerView).setVisibility(View.VISIBLE);
            }
            //更新旧位置
            if (temp < 0) return;
            notifyItemChanged(temp);
        });


        OtaVersionAdapter otaVersionAdapter = new OtaVersionAdapter();
        otaVersionAdapter.setVersion(item.getVersion());
        ((RecyclerView) holder.getView(R.id.recyclerView)).setAdapter(otaVersionAdapter);
        otaVersionAdapter.setList(item.getImgList());
        otaVersionAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                if (otaVersionAdapter.getItem(position).isDownload()) {//已下载
                    isDownload = true;
                    otaUpgrade(otaVersionAdapter.getItem(position).getOtaPath());
                } else {//未下载
                    showDownLoadDialog(otaVersionAdapter.getItem(position), otaVersionAdapter, position);
                }
            }
        });
//        holder.getView(R.id.recyclerView).setNestedScrollingEnabled(true);
//        ((RecyclerView) holder.getView(R.id.recyclerView)).addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
//        ((DefaultItemAnimator) ((RecyclerView) holder.getView(R.id.recyclerView)).getItemAnimator()).setSupportsChangeAnimations(false);
    }

    private void showDownLoadDialog(DevVersionBean.VersionList.ImgBean item, OtaVersionAdapter otaVersionAdapter, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(WordUtil.getString(R.string.download));
        builder.setMessage(WordUtil.getString(R.string.whether_download) + item.getName());
        builder.setPositiveButton(WordUtil.getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface pDialogInterface, int pI) {
                doDownLoad(item, otaVersionAdapter, position);
            }
        });
//        builder.setNeutralButton("忽略", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface pDialogInterface, int pI) {
//                Toast.makeText(MainActivity.this, "忽略", Toast.LENGTH_SHORT).show();
//            }
//        });
        builder.setNegativeButton(WordUtil.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface pDialogInterface, int pI) {

            }
        });
        builder.create().show();
    }

    private String otaFilePath;
    private DownloadProgressDialog progressDialog;

    private void doDownLoad(DevVersionBean.VersionList.ImgBean item, OtaVersionAdapter otaVersionAdapter, int position) {
        otaFilePath = MyApplication.getInstance().getExternalFilesDir("otaTest").getAbsolutePath() + File.separator + "V" + otaVersionAdapter.getVersion() + File.separator + item.getName();
        RXJavaUtils.backProcessor(getContext(), new OnRxJavaBackProcessorListenter<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> emitter) {
                DownloadFileUtil.execute(item.getUrl(), otaFilePath, new OnDownLoadListener() {
                    @Override
                    public void onSuccess(File result) {
                        try {
                            emitter.onNext(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                            emitter.onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable ex, boolean isOnCallback) {
                        FitLoader.stopLoading(getContext());
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isDownloading) {
                        Message message = Message.obtain();
                        message.arg1 = (int) (((float) current / (float) total) * 100l);
                        message.what = DOWNLOAD_PROGRESS;
                        if (handler != null) {
                            handler.sendMessage(message);
                        }
                    }
                });
            }

            @Override
            public void onNext(File file) {
                Logger.i(Lg.getClassName(UpgradeOtaAdapter.class), "otaFilePath=" + file.getAbsolutePath());
                otaVersionAdapter.notifyItemChanged(position);
                otaUpgrade(file.getAbsolutePath());
            }

            @Override
            public void onError(Throwable e) {

            }
        });

    }

    private void showUpgradeDialog(String otaFilePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(WordUtil.getString(R.string.prompt));
        builder.setMessage(WordUtil.getString(R.string.upgrade_now));
        builder.setPositiveButton(WordUtil.getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface pDialogInterface, int pI) {
                if (BluetoothHelper.getInstance().isConnectOk()) {
                    BluetoothHelper.getInstance().getTransfer().otaUpgrade(otaFilePath, "1", "1");
                    if (handler != null) {
                        handler.sendEmptyMessage(OTA_UPGRADE);
                    }
                } else {
                    Toast.makeText(getContext(), "Not Connected", Toast.LENGTH_LONG).show();
                }
            }
        });
//        builder.setNeutralButton("忽略", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface pDialogInterface, int pI) {
//                Toast.makeText(MainActivity.this, "忽略", Toast.LENGTH_SHORT).show();
//            }
//        });
        builder.setNegativeButton(WordUtil.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface pDialogInterface, int pI) {

            }
        });
        builder.create().show();
    }

    private void otaUpgrade(String otaFilePath) {
        if (otaFilePath != null) {
            try {
                showUpgradeDialog(otaFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (handler != null) {
                handler.sendEmptyMessage(MESSAGE_TYPE_COUNT_STOP);
            }
            Toast.makeText(MyApplication.getInstance(), "Can't read OTA file.", Toast.LENGTH_SHORT).show();
        }
    }


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == DOWNLOAD_PROGRESS) {
                isDownload = true;
                if (progressDialog == null) {
                    progressDialog = new DownloadProgressDialog(getContext());
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    // progress 为int类型，为了能够获取足够的精度，把数值扩大100倍
                }
                progressDialog.setTitle("Download...");
                progressDialog.setMax(100);
                if (msg.arg1 < 100) {
                    progressDialog.setProgress(msg.arg1);
                    progressDialog.show();
                } else {
                    progressDialog.dismiss();
                }
            } else if (msg.what == OTA_UPGRADE) {
                if (progressDialog != null) {
                    int hours, minutes, seconds;
                    hours = timeCount / (60 * 60);
                    minutes = (timeCount / 60) % 60;
                    seconds = timeCount % (60);
                    progressDialog.setSubText(String.format("Time  %02d:%02d:%02d", hours, minutes, seconds));
                }else{

                }
                if (handler != null) {
                    handler.sendEmptyMessageDelayed(OTA_UPGRADE, 1000);
                }
                timeCount++;
            } else if (msg.what == MESSAGE_TYPE_COUNT_STOP) {
                if (handler != null) {
                    handler.removeMessages(OTA_UPGRADE);
                }
                timeCount = 0;
            }
        }
    };

    public void onDestroy() {
        if (handler != null) {
            handler.removeMessages(OTA_UPGRADE);
            handler = null;
        }
    }
}
