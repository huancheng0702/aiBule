<template>
  <view class="core-upload">
    <!-- 文件列表回显区 -->
    <template v-if="fileList.length > 0">
      <view
        class="core-upload__item"
        v-for="(file, index) in fileList"
        :key="file.id || index"
      >
        <!-- 作用域插槽：对外暴露单个文件的状态和操作方法 -->
        <slot
          name="item"
          :file="file"
          :index="index"
          :deleteItem="() => deleteItem(index)"
          :preview="() => previewItem(file)"
        >
          <!-- 默认的基础 UI（当业务没有传入 #item 时显示） -->
          <view class="default-item-ui">
            <text class="file-name">{{ file.name || '未命名文件' }}</text>
            <text class="file-status">
              {{ file.status === 'uploading' ? `上传中 ${file.progress}%` : file.status === 'success' ? '成功' : '失败' }}
            </text>
            <view class="delete-btn" @click.stop="deleteItem(index)">✕</view>
          </view>
        </slot>
      </view>
    </template>

    <!-- 触发区 (控制选择文件) -->
    <view
      class="core-upload__trigger"
      @click="triggerPick"
      v-if="!disabled && fileList.length < limit"
    >
      <!-- 作用域插槽：对外暴露触发方法和禁用状态 -->
      <slot :disabled="disabled" :triggerPick="triggerPick">
        <!-- 默认的基础触发 UI -->
        <view class="default-trigger-ui">
          <text>+</text>
          <text style="font-size: 24rpx; margin-top: 4rpx;">选择文件</text>
        </view>
      </slot>
    </view>
    <view class="core-action-sheet-wrap" v-if="actionSheetVisible" @touchmove.stop.prevent>
      <view class="core-action-sheet-mask" @click="closeActionSheet"></view>
      <view class="core-action-sheet-content" :class="{ 'is-show': actionSheetVisible }">
        <view class="core-action-sheet-title">请选择上传方式</view>
        <view 
          class="core-action-sheet-item" 
          v-for="(item, index) in actionSheetItems" 
          :key="index"
          @click="handleActionSelect(item)"
        >
          {{ item.name }}
        </view>
        <view class="core-action-sheet-cancel" @click="closeActionSheet">取消</view>
        <view class="core-safe-area"></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
let chooseNativeFilesApp: any = null;
let chooseNativeFileApp: any = null;
// #ifdef APP-PLUS
// @ts-ignore
import { chooseNativeFiles, chooseNativeFile } from '@/uni_modules/core-upload';
chooseNativeFilesApp = chooseNativeFiles;
chooseNativeFileApp = chooseNativeFile;
// #endif

interface FileItem {
  url: string;
  name: string;
  size?: number;
  type?: string;
  status: 'pending' | 'uploading' | 'success' | 'failed';
  progress: number;
  response?: any;
  id?: string | number;
  [key: string]: any;
}

interface Props {
  modelValue?: FileItem[];
  action?: string; // 上传的接口完整地址
  headers?: Record<string, string>; // 请求头，如 token
  formData?: Record<string, any>; // 额外的 formData 数据
  name?: string; // 文件对应的 key，默认 file
  accept?: 'image' | 'video' | 'file' | 'all' | 'media';
  multiple?: boolean;
  limit?: number; // 最大文件数
  maxSize?: number; // 最大限制 (MB)
  disabled?: boolean;
  autoUpload?: boolean; // 是否选择后自动上传
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  action: '',
  headers: () => ({}),
  formData: () => ({}),
  name: 'file',
  accept: 'all',
  multiple: true,
  limit: 9,
  maxSize: Number.MAX_VALUE,
  disabled: false,
  autoUpload: true
});

const emit = defineEmits(['update:modelValue', 'change', 'success', 'error', 'delete', 'oversize']);

// 内部状态列表
const fileList = ref<FileItem[]>([]);

// 监听外部传入的值回显
watch(
  () => props.modelValue,
  (val) => {
    if (val && Array.isArray(val)) {
      // 简单浅比较防止死循环，实际业务中最好比较内部元素
      if (JSON.stringify(val) !== JSON.stringify(fileList.value)) {
        fileList.value = [...val];
      }
    }
  },
  { immediate: true, deep: true }
);

const actionSheetVisible = ref(false);
const actionSheetItems = ref<{ name: string; value: string; icon?: string }[]>([]);
let actionSheetCallback: ((value: string) => void) | null = null;

const handleActionSelect = (item: any) => {
  actionSheetVisible.value = false;
  if (actionSheetCallback) {
    actionSheetCallback(item.value);
  }
};

const closeActionSheet = () => {
  actionSheetVisible.value = false;
};

/**
 * 触发文件选择逻辑
 */
const triggerPick = async () => {
  if (props.disabled) return;
  const maxCount = props.limit - fileList.value.length;
  if (maxCount <= 0) {
    uni.showToast({ title: '已达到最大上传数量', icon: 'none' });
    return;
  }

  // #ifdef APP-PLUS
  // APP端体验优化：如果是 accept === 'all'，提供直观的底层 ActionSheet 选项
  if (props.accept === 'all') {
    actionSheetItems.value = [
      { name: '相册选择图片', value: 'album', icon: 'image' },
      { name: '拍照', value: 'camera', icon: 'camera' },
      { name: '选择文件', value: 'file', icon: 'folder' }
    ];
    actionSheetVisible.value = true;
    actionSheetCallback = (val) => {
      if (val === 'album') pickImageWithSource(maxCount, ['album']);
      if (val === 'camera') pickImageWithSource(maxCount, ['camera']);
      if (val === 'file') pickNativeDocument(maxCount);
    };
    return;
  }
  // 如果 accept 只是 image，做一层拦截
  if (props.accept === 'image') {
    actionSheetItems.value = [
      { name: '从相册选择', value: 'album', icon: 'image' },
      { name: '拍照', value: 'camera', icon: 'camera' }
    ];
    actionSheetVisible.value = true;
    actionSheetCallback = (val) => {
      if (val === 'album') pickImageWithSource(maxCount, ['album']);
      if (val === 'camera') pickImageWithSource(maxCount, ['camera']);
    };
    return;
  }
  // 如果是专门的文件或其他，直接调起原生文件选择器
  pickNativeDocument(maxCount);
  return;
  // #endif

  // 非 APP 端逻辑
  if (props.accept === 'image') {
    pickImageWithSource(maxCount, ['album', 'camera']);
  } else {
    fallbackPick(maxCount);
  }
};

/**
 * 封装的图片选择逻辑 (直接调用系统相册/相机，体验最佳)
 */
const pickImageWithSource = (maxCount: number, sourceType: ('album' | 'camera')[]) => {
  uni.chooseImage({
    count: props.multiple ? maxCount : 1,
    sourceType,
    success: (res) => {
      const tempFiles = res.tempFiles as any[];
      const files: FileItem[] = tempFiles.map((f: any) => ({
        url: f.path || f.url,
        name: f.name || `image_${Date.now()}.jpg`,
        size: f.size,
        status: 'pending',
        progress: 0
      }));
      handleFilesSelected(files);
    }
  });
};

/**
 * 封装的原生文件选择器逻辑 (支持 Android/iOS 的纯净文档选择)
 */
const pickNativeDocument = async (maxCount: number) => {
  // #ifdef APP-PLUS
  if (chooseNativeFilesApp && chooseNativeFileApp) {
    try {
      let res: any[] = [];
      if (props.multiple) {
        res = await chooseNativeFilesApp({ multiple: true });
      } else {
        const single = await chooseNativeFileApp({ multiple: false });
        if (single) res = [single];
      }
      
      if (res && res.length > 0) {
        const selectedFiles = props.multiple ? res.slice(0, maxCount) : [res[0]];
        const files: FileItem[] = selectedFiles.map((f: any) => ({
          url: f.filePath.replace(/^file:\/\//, ''),
          name: f.fileName,
          size: f.fileSize,
          type: f.mimeType || 'file',
          status: 'pending',
          progress: 0
        }));
        handleFilesSelected(files);
      }
    } catch (err: any) {
      if (err.message && !err.message.includes('取消')) {
        console.error('选择文件失败:', err);
      }
    }
    return;
  }
  // #endif
  
  // 插件不可用的兜底
  fallbackPick(maxCount);
};

/**
 * 非 APP 端的全量兜底逻辑 (小程序/H5)
 */
const fallbackPick = (maxCount: number) => {
  // #ifdef MP-WEIXIN
  uni.chooseMessageFile({
    count: props.multiple ? maxCount : 1,
    type: props.accept === 'all' ? 'all' : (props.accept as any),
    success: (res) => {
      const tempFiles = res.tempFiles as any[];
      const files: FileItem[] = tempFiles.map((f: any) => ({
        url: f.path,
        name: f.name,
        size: f.size,
        status: 'pending',
        progress: 0
      }));
      handleFilesSelected(files);
    }
  });
  // #endif
  // #ifndef MP-WEIXIN
  uni.chooseFile({
    count: props.multiple ? maxCount : 1,
    type: props.accept === 'all' ? 'all' : (props.accept as any),
    success: (res) => {
      const tempFiles = res.tempFiles as any[];
      const files: FileItem[] = tempFiles.map((f: any) => ({
        url: f.path || f.url,
        name: f.name || `file_${Date.now()}`,
        size: f.size,
        status: 'pending',
        progress: 0
      }));
      handleFilesSelected(files);
    }
  });
  // #endif
};

/**
 * 处理选中的文件（包含体积校验与加入队列）
 */
const handleFilesSelected = (files: FileItem[]) => {
  const validFiles: FileItem[] = [];
  const maxByte = props.maxSize * 1024 * 1024;
  let hasOversize = false;

  files.forEach(f => {
    if (f.size && f.size > maxByte) {
      hasOversize = true;
      emit('oversize', f);
    } else {
      validFiles.push(f);
    }
  });

  if (hasOversize) {
    uni.showToast({ title: `部分文件超出 ${props.maxSize}MB 大小限制`, icon: 'none' });
  }

  if (validFiles.length > 0) {
    const startIndex = fileList.value.length;
    fileList.value.push(...validFiles);
    updateModel();

    if (props.autoUpload && props.action) {
      validFiles.forEach((f, i) => {
        doUpload(startIndex + i, f);
      });
    }
  }
};

/**
 * 执行网络上传
 */
const doUpload = (index: number, file: FileItem) => {
  if (!props.action) {
    console.error('[core-upload] 未配置 action，无法上传');
    return;
  }

  fileList.value[index].status = 'uploading';
  fileList.value[index].progress = 0;
  updateModel();

  const uploadTask = uni.uploadFile({
    url: props.action,
    filePath: file.url,
    name: props.name,
    header: props.headers,
    formData: {
      fileName: file.name,
      ...props.formData
    },
    success: (res) => {
      // 网络层成功不代表业务成功，需要业务方根据 response 自己判断，但组件底层认为已完成
      fileList.value[index].status = 'success';
      fileList.value[index].progress = 100;
      let responseData = res.data;
      try {
        responseData = JSON.parse(res.data);
      } catch (e) {}
      fileList.value[index].response = responseData;
      emit('success', { file: fileList.value[index], response: responseData, index });
      updateModel();
    },
    fail: (err) => {
      fileList.value[index].status = 'failed';
      emit('error', { file: fileList.value[index], error: err, index });
      updateModel();
    }
  });

  uploadTask.onProgressUpdate((res) => {
    if (fileList.value[index] && fileList.value[index].status === 'uploading') {
      fileList.value[index].progress = res.progress;
    }
  });
};

/**
 * 删除文件
 */
const deleteItem = (index: number) => {
  const deletedFile = fileList.value[index];
  fileList.value.splice(index, 1);
  emit('delete', { file: deletedFile, index });
  updateModel();
};

/**
 * 预览文件（如果开发者使用了默认 UI 才会被调用）
 */
const previewItem = (file: FileItem) => {
  const ext = file.name.split('.').pop()?.toLowerCase() || '';
  const imgExts = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'];
  if (imgExts.includes(ext)) {
    uni.previewImage({
      current: file.url,
      urls: fileList.value.filter(f => imgExts.includes(f.name.split('.').pop()?.toLowerCase() || '')).map(f => f.url)
    });
  } else {
    // 简易回退打开方案
    // #ifdef H5
    window.open(file.url, '_blank');
    // #endif
    // #ifndef H5
    uni.downloadFile({
      url: file.url,
      success: (res) => {
        if (res.statusCode === 200) uni.openDocument({ filePath: res.tempFilePath, showMenu: true });
      }
    });
    // #endif
  }
};

/**
 * 同步到外部
 */
const updateModel = () => {
  emit('update:modelValue', [...fileList.value]);
  emit('change', [...fileList.value]);
};

// 主动暴露给外部调用的方法
defineExpose({
  triggerPick,
  deleteItem,
  fileList
});
</script>

<style scoped lang="scss">
.core-upload {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

/* 默认自带的一套防退化保底 UI，仅在业务没有使用插槽时生效 */
.default-item-ui {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  background-color: #f4f5f7;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 8rpx;
  box-sizing: border-box;

  .file-name {
    font-size: 24rpx;
    color: #333;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .file-status {
    font-size: 20rpx;
    color: #909193;
    margin-top: 8rpx;
  }

  .delete-btn {
    position: absolute;
    top: -10rpx;
    right: -10rpx;
    width: 36rpx;
    height: 36rpx;
    background-color: rgba(0,0,0,0.5);
    color: #fff;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 20rpx;
    z-index: 2;
  }
}

.default-trigger-ui {
  width: 160rpx;
  height: 160rpx;
  background-color: #f4f5f7;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909193;
}

.core-action-sheet-wrap {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.core-action-sheet-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s;
}
.core-action-sheet-content {
  position: relative;
  background-color: #f7f8fa;
  border-radius: 32rpx 32rpx 0 0;
  overflow: hidden;
  transform: translateY(100%);
  animation: slideUp 0.3s forwards;
  padding-bottom: 20rpx;
}
.core-action-sheet-title {
  font-size: 26rpx;
  color: #909399;
  text-align: center;
  padding: 30rpx;
  background-color: #fff;
  border-bottom: 1px solid #f2f3f5;
}
.core-action-sheet-item {
  font-size: 32rpx;
  color: #333;
  text-align: center;
  padding: 30rpx;
  background-color: #fff;
  border-bottom: 1px solid #f2f3f5;
}
.core-action-sheet-item:active {
  background-color: #f2f3f5;
}
.core-action-sheet-cancel {
  margin-top: 16rpx;
  font-size: 32rpx;
  color: #333;
  text-align: center;
  padding: 30rpx;
  background-color: #fff;
}
.core-action-sheet-cancel:active {
  background-color: #f2f3f5;
}
.core-safe-area {
  height: constant(safe-area-inset-bottom);
  height: env(safe-area-inset-bottom);
  background-color: #fff;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

/* 横屏模式下，rpx会基于长边计算导致元素过大，改用固定的 px 优化展示 */
@media screen and (orientation: landscape) {
  .core-action-sheet-content {
    border-radius: 16px 16px 0 0;
    padding-bottom: 10px;
  }
  .core-action-sheet-title {
    font-size: 13px;
    padding: 12px;
  }
  .core-action-sheet-item {
    font-size: 15px;
    padding: 12px;
  }
  .core-action-sheet-cancel {
    margin-top: 8px;
    font-size: 15px;
    padding: 12px;
  }
}
</style>
