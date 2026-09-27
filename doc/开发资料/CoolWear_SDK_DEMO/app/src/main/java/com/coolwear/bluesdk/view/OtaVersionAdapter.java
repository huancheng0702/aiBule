package com.coolwear.bluesdk.view;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.coolwear.bluesdk.MyApplication;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.bean.DevVersionBean;
import com.coolwear.bluesdk.utils.WordUtil;

import java.io.File;


/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2022/8/8 0008 18:24
 */
public class OtaVersionAdapter extends CommonAdapter<DevVersionBean.VersionList.ImgBean> {
    private String version;

    public OtaVersionAdapter() {
        super(R.layout.item_otaversion);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    protected void convert(BaseViewHolder holder, DevVersionBean.VersionList.ImgBean item) {
        holder.setText(R.id.tv_otaname, WordUtil.getString(R.string.package_name) + ":" + item.getName());
        holder.setText(R.id.tv_size, WordUtil.getString(R.string.package_size) + ":" + getFileSize(item.getFileSize()) + "");
        File otaFile = new File(MyApplication.getInstance().getExternalFilesDir("otaTest").getAbsolutePath() + File.separator + "V"+version + File.separator + item.getName());
        if (otaFile.exists()) {
            holder.setVisible(R.id.tv_exists, true);
            holder.setVisible(R.id.iv_download, false);
            holder.setText(R.id.tv_exists, WordUtil.getString(R.string.download_complete));
            item.setDownload(true);
            item.setOtaPath(otaFile.getAbsolutePath());
        } else {
            holder.setVisible(R.id.tv_exists, false);
            holder.setVisible(R.id.iv_download, true);
            item.setDownload(false);
            item.setOtaPath("");
        }
    }

    private String getFileSize(int fileSize) {
        if (fileSize / 1204 > 100 && fileSize / 1204 < 1204) {
            return fileSize / 1204 + "KB";
        } else if (fileSize / 1204 > 1024) {
            return fileSize / 1204 / 1024 + "MB";
        }
        return "0";
    }

}
