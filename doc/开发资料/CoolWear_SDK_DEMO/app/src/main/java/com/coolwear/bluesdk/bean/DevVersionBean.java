package com.coolwear.bluesdk.bean;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2022/8/9 0009 10:56
 */
public class DevVersionBean implements Serializable {
    private int customerID;
    private String btName;
    private List<VersionList> versionList;

    public class VersionList implements Serializable {
        private String version;
        private List<ImgBean> imgList;
        private boolean isOpen;
        public class ImgBean implements Serializable {
            private String name;
            private String url;
            private int fileSize;
            private boolean isDownload;
            private String otaPath;

            public String getOtaPath() {
                return otaPath;
            }

            public void setOtaPath(String otaPath) {
                this.otaPath = otaPath;
            }

            public boolean isDownload() {
                return isDownload;
            }

            public void setDownload(boolean download) {
                isDownload = download;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public int getFileSize() {
                return fileSize;
            }

            public void setFileSize(int fileSize) {
                this.fileSize = fileSize;
            }
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void setOpen(boolean open) {
            isOpen = open;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<ImgBean> getImgList() {
            return imgList;
        }

        public void setImgList(List<ImgBean> imgList) {
            this.imgList = imgList;
        }
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    public String getBtName() {
        return btName;
    }

    public void setBtName(String btName) {
        this.btName = btName;
    }

    public List<VersionList> getVersionList() {
        return versionList;
    }

    public void setVersionList(List<VersionList> versionList) {
        this.versionList = versionList;
    }
}
