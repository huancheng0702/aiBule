# core-upload (文件上传组件)

## 🎯 核心特性

- **UI 与逻辑彻底解耦**：使用 Vue3 作用域插槽 (Scope Slot) 进行 UI 控制，如果不传插槽，默认提供极简的回显和上传按钮。
- **是匹配所有平台：APP + H5 + 小程序**

---

## 💻 基本用法

### 1. 使用默认 UI

```vue
<template>
  <core-upload
    v-model="fileList"
    action="https://www.example.com/api/upload"
    :headers="{ token: '123' }"
    @success="handleSuccess"
  />
</template>

<script setup>
import { ref } from "vue";
const fileList = ref([]);
const handleSuccess = ({ file, response }) => {
  console.log("上传成功", response);
};
</script>
```

### 2. 自定义 UI

通过 `#item` 和 `#default` 插槽渲染。

```vue
<template>
  <core-upload
    v-model="fileList"
    action="https://www.example.com/api/upload"
    accept="image"
  >
    <!-- 1. 定制文件列表展示 -->
    <template #item="{ file, index, deleteItem }">
      <view class="my-custom-file">
        <image :src="file.url" mode="aspectFill"></image>
        <text>进度: {{ file.progress }}%</text>
        <button size="mini" @click="deleteItem">删除</button>
      </view>
    </template>

    <!-- 2. 定制触发上传按钮 -->
    <template #default="{ disabled, triggerPick }">
      <button :disabled="disabled" type="primary" @click="triggerPick">
        点击上传图片
      </button>
    </template>
  </core-upload>
</template>
```

### 3. 完全手动控制上传 (复杂业务逻辑)

当你的业务有极其复杂的上传流程（如需要先算 MD5、处理秒传逻辑、或者分片上传）时，可以关闭自动上传，通过监听 `@change` 手动接管。

```vue
<template>
  <core-upload v-model="fileList" :autoUpload="false" @change="onChange">
    <!-- 这里同样可以像上面一样完全自定义 UI -->
  </core-upload>
</template>

<script setup>
import { ref } from "vue";

const fileList = ref([]);

const onChange = (newList) => {
  // 找出刚刚由 core-upload 加入队列，状态为 pending 的新文件
  const pendingFiles = newList.filter((f) => f.status === "pending");

  pendingFiles.forEach(async (file) => {
    // 1. 手动标记为上传中，UI 也会跟着转圈圈
    file.status = "uploading";

    // 2. 走你自己的复杂上传逻辑 (比如 uni.uploadFile 或者云储存 SDK)
    try {
      const res = await myCustomUploadLogic(file.url);
      file.status = "success";
      file.url = res.serverUrl; // 回填服务器地址
    } catch (err) {
      file.status = "failed";
    }
  });
};
</script>
```

---

## 🛠 参数说明 (Props)

| 参数名       | 类型      | 默认值      | 说明                                                  |
| ------------ | --------- | ----------- | ----------------------------------------------------- |
| `v-model`    | `Array`   | `[]`        | 组件绑定的标准文件列表数据                            |
| `action`     | `String`  | `''`        | 上传的接口地址（当 `autoUpload` 为 `true` 时必填）    |
| `headers`    | `Object`  | `{}`        | 请求头（例如可以携带 Token）                          |
| `formData`   | `Object`  | `{}`        | 额外的 formData 业务参数                              |
| `name`       | `String`  | `'file'`    | 发送到后端的 file 对应的 key 名称                     |
| `accept`     | `String`  | `'all'`     | 允许的文件类型：`image` \| `video` \| `file` \| `all` |
| `multiple`   | `Boolean` | `true`      | 是否允许多选                                          |
| `limit`      | `Number`  | `9`         | 最大上传文件数量                                      |
| `maxSize`    | `Number`  | `MAX_VALUE` | 单个文件体积上限 (单位：MB)                           |
| `autoUpload` | `Boolean` | `true`      | 是否在选中文件后立刻自动触发上传                      |
| `disabled`   | `Boolean` | `false`     | 是否禁用组件                                          |

---

## ⚡ 事件说明 (Events)

| 事件名      | 参数                          | 说明                                                 |
| ----------- | ----------------------------- | ---------------------------------------------------- |
| `@change`   | `(fileList)`                  | 绑定的文件列表发生变化时触发（选中、删除、状态变化） |
| `@success`  | `({ file, response, index })` | 单个文件上传成功时触发，`response` 为服务端返回结果  |
| `@error`    | `({ file, error, index })`    | 单个文件上传失败时触发                               |
| `@delete`   | `({ file, index })`           | 用户主动删除某个文件时触发                           |
| `@oversize` | `(file)`                      | 当选择的文件超出 `maxSize` 限制时触发                |

---

## 🔧 Expose 暴露方法

如果希望在外部利用 `ref`使用：

- `triggerPick()`: 主动调起底层设备的文件选择弹窗。
- `deleteItem(index)`: 手动删除指定索引的文件。
