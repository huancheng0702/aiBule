<template>
	<view class="page">
		<!-- 头部 -->
		<view class="header">
			<view class="header-left">
				<text class="title">{{ fromMine ? '个人资料' : '完善基础信息' }}</text>
				<text class="subtitle">{{ fromMine ? '编辑个人资料信息' : '完善个人资料信息' }}</text>
			</view>
			<view v-if="!fromMine" class="step-badge">
				<view class="step-badge-text">首次使用 · <text style="color: #2C89FF;">1</text> /2</view>
			</view>
		</view>

		<!-- 头像 -->
		<view class="avatar-row" @click="onChooseAvatar">
			<image class="avatar-img" :src="avatar || '/static/defaultAvatar.png'" mode="aspectFill"></image>
			<view class="avatar-tip">
				<text class="avatar-title">设置头像</text>
				<text class="avatar-sub">{{ uploading ? '上传中...' : '点击更换头像' }}</text>
			</view>
			<text class="avatar-arrow">›</text>
		</view>

		<!-- 表单 -->
		<view class="form">
			<!-- 昵称 -->
			<view class="field">
				<text class="field-label">昵称</text>
				<input
					class="field-input"
					type="text"
					v-model="nickname"
					placeholder="请输入昵称"
					placeholder-class="field-placeholder"
					maxlength="20"
				/>
			</view>

			<!-- 性别 -->
			<view class="field">
				<text class="field-label">性别</text>
				<view class="gender-row">
					<view
						class="gender-item"
						:class="{ active: gender === 'male' }"
						@click="gender = 'male'"
					>
						<image class="gender-icon" src="/static/nan.png" mode=""></image>
						<text class="gender-text">男</text>
					</view>
					<view
						class="gender-item"
						:class="{ active: gender === 'female' }"
						@click="gender = 'female'"
					>
						<image class="gender-icon" src="/static/nv.png" mode=""></image>
						<text class="gender-text">女</text>
					</view>
				</view>
			</view>

			<!-- 出生日期 -->
			<view class="field">
				<text class="field-label">出生日期</text>
				<picker mode="date" :value="birthday || todayStr" :end="todayStr" @change="onBirthdayChange">
					<view class="field-picker">
						<text class="field-picker-text" :class="{ 'field-placeholder': !birthday }">
							{{ birthday || '请选择出生日期' }}
						</text>
						<image class="field-calendar" src="/static/rili.png" mode=""></image>
					</view>
				</picker>
			</view>

			<!-- 身高 -->
			<view class="field">
				<text class="field-label">身高 (Cm)</text>
				<input
					class="field-input"
					type="digit"
					v-model="height"
					placeholder="请输入身高"
					placeholder-class="field-placeholder"
				/>
			</view>

			<!-- 体重 -->
			<view class="field">
				<text class="field-label">体重 (Kg)</text>
				<input
					class="field-input"
					type="digit"
					v-model="weight"
					placeholder="请输入体重"
					placeholder-class="field-placeholder"
				/>
			</view>
		</view>

		<!-- 底部操作 -->
		<view class="footer">
			<view class="save-btn" @click="onSave">
				<image class="save-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="save-btn-text">{{ fromMine ? '保存' : '保存并继续' }}</text>
			</view>
			<text v-if="!fromMine" class="skip-link" @click="onSkip">稍后完善</text>
		</view>
	</view>
</template>

<script>
	import { getUserInfo, editUserInfo } from '@/common/api/user.js'
	import { uploadFile } from '@/common/api/file.js'
	import { saveLoginSession, USER_ID_KEY, USER_INFO_KEY } from '@/common/auth.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function toDateStr(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}

	function mapGenderToUi(gender) {
		const g = String(gender == null ? '' : gender)
		if (g === '1' || g === 'male' || g === '男') return 'male'
		if (g === '2' || g === 'female' || g === '女') return 'female'
		return ''
	}

	function mapGenderToApi(gender) {
		if (gender === 'male') return '1'
		if (gender === 'female') return '2'
		return '0'
	}

	export default {
		data() {
			return {
				fromMine: false,
				saving: false,
				uploading: false,
				userId: null,
				avatar: '',
				nickname: '',
				gender: '',
				birthday: '',
				height: '',
				weight: '',
				todayStr: toDateStr(new Date())
			}
		},
		onLoad(query) {
			this.fromMine = !!(query && (query.from === 'mine' || query.mode === 'edit'))
			this.applyCached()
			this.loadUser()
		},
		methods: {
			applyCached() {
				const cached = uni.getStorageSync('userBasicInfo') || {}
				const userInfo = uni.getStorageSync(USER_INFO_KEY) || {}
				const src = Object.keys(cached).length ? cached : userInfo
				this.userId = uni.getStorageSync(USER_ID_KEY) || userInfo.userId || null
				this.avatar = src.avatar || uni.getStorageSync('avatar') || this.avatar
				this.nickname = src.nickName || src.nickname || this.nickname
				this.gender = mapGenderToUi(src.gender) || this.gender
				this.birthday = src.birthday || this.birthday
				if (src.height != null && src.height !== '') {
					this.height = String(src.height)
				}
				if (src.weight != null && src.weight !== '') {
					this.weight = String(src.weight)
				}
			},
			async loadUser() {
				try {
					const info = await getUserInfo()
					if (!info) return
					saveLoginSession(info)
					this.userId = info.userId != null ? info.userId : this.userId
					this.avatar = info.avatar || this.avatar
					this.nickname = info.nickName || info.nickname || this.nickname
					this.gender = mapGenderToUi(info.gender) || this.gender
					this.birthday = info.birthday || this.birthday
					if (info.height != null && info.height !== '') {
						this.height = String(info.height)
					}
					if (info.weight != null && info.weight !== '') {
						this.weight = String(info.weight)
					}
				} catch (e) {}
			},
			onBirthdayChange(e) {
				this.birthday = e.detail.value
			},
			onChooseAvatar() {
				if (this.uploading) return
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => {
						const filePath = res.tempFilePaths && res.tempFilePaths[0]
						if (!filePath) return
						this.uploadAvatar(filePath)
					}
				})
			},
			async uploadAvatar(filePath) {
				this.uploading = true
				uni.showLoading({ title: '上传中...', mask: true })
				try {
					const data = await uploadFile(filePath)
					const url = typeof data === 'string' ? data : (data && (data.url || data.fileUrl || data.path))
					if (!url) {
						uni.showToast({ title: '上传失败，请重试', icon: 'none' })
						return
					}
					this.avatar = String(url)
					uni.setStorageSync('avatar', this.avatar)
				} catch (e) {
					// uploadFile 内部已 toast
				} finally {
					this.uploading = false
					uni.hideLoading()
				}
			},
			validate() {
				if (!this.nickname || !String(this.nickname).trim()) {
					uni.showToast({ title: '请输入昵称', icon: 'none' })
					return false
				}
				if (!this.gender) {
					uni.showToast({ title: '请选择性别', icon: 'none' })
					return false
				}
				if (!this.birthday) {
					uni.showToast({ title: '请选择出生日期', icon: 'none' })
					return false
				}
				const h = Number(this.height)
				const w = Number(this.weight)
				if (!h || h < 50 || h > 250) {
					uni.showToast({ title: '请输入有效身高', icon: 'none' })
					return false
				}
				if (!w || w < 20 || w > 300) {
					uni.showToast({ title: '请输入有效体重', icon: 'none' })
					return false
				}
				return true
			},
			buildPayload() {
				const nickName = String(this.nickname).trim()
				const height = Number(this.height)
				const weight = Number(this.weight)
				return {
					userId: this.userId || uni.getStorageSync(USER_ID_KEY),
					nickName,
					avatar: this.avatar || undefined,
					gender: mapGenderToApi(this.gender),
					birthday: this.birthday,
					height,
					weight
				}
			},
			persistLocal(payload) {
				uni.setStorageSync('userBasicInfo', {
					nickname: payload.nickName,
					nickName: payload.nickName,
					avatar: this.avatar,
					gender: this.gender,
					birthday: payload.birthday,
					height: payload.height,
					weight: payload.weight
				})
				saveLoginSession({
					userId: payload.userId,
					nickName: payload.nickName,
					avatar: this.avatar,
					gender: payload.gender,
					birthday: payload.birthday,
					height: payload.height,
					weight: payload.weight
				})
			},
			async onSave() {
				if (this.saving || !this.validate()) return
				const payload = this.buildPayload()
				if (payload.userId == null || payload.userId === '') {
					uni.showToast({ title: '用户信息缺失，请重新登录', icon: 'none' })
					return
				}
				this.saving = true
				try {
					await editUserInfo(payload)
					this.persistLocal(payload)
					uni.setStorageSync('needCompleteInfo', false)
					if (this.fromMine) {
						uni.showToast({ title: '已保存', icon: 'none' })
						setTimeout(() => {
							uni.navigateBack()
						}, 400)
						return
					}
					uni.redirectTo({
						url: '/pages/mine/bluePower'
					})
				} catch (e) {
					// http 层已 toast
				} finally {
					this.saving = false
				}
			},
			onSkip() {
				uni.setStorageSync('needCompleteInfo', false)
				uni.redirectTo({
					url: '/pages/mine/bluePower'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 32rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
		display: flex;
		flex-direction: column;
	}

	.header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
	}

	.header-left {
		flex: 1;
		min-width: 0;
		padding-right: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.title {
		font-size: 40rpx;
		font-weight: 700;
		line-height: 56rpx;
		color: #ffffff;
	}

	.subtitle {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.step-badge {
		flex-shrink: 0;
		padding: 10rpx 20rpx;
		border-radius: 999rpx;
		background: #1a1b25;
	}

	.step-badge-text {
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.avatar-row {
		margin-top: 48rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
	}

	.avatar-img {
		width: 120rpx;
		height: 120rpx;
		border-radius: 60rpx;
		background: #2a2b38;
		flex-shrink: 0;
	}

	.avatar-tip {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
	}

	.avatar-title {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.avatar-sub {
		margin-top: 6rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.avatar-arrow {
		font-size: 36rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.3);
		flex-shrink: 0;
	}

	.form {
		margin-top: 48rpx;
		flex: 0.5;
	}

	.field {
		margin-bottom: 32rpx;
	}

	.field-label {
		display: block;
		margin-bottom: 16rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.field-input {
		width: 100%;
		height: 96rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		box-sizing: border-box;
		font-size: 30rpx;
		color: #ffffff;
	}

	.field-placeholder {
		color: rgba(255, 255, 255, 0.3);
	}

	.gender-row {
		display: flex;
		align-items: center;
	}

	.gender-item {
		flex: 1;
		height: 96rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.gender-item + .gender-item {
		margin-left: 20rpx;
	}

	.gender-item.active {
		background: #2f7bff;
	}

	.gender-icon {
		width: 36rpx;
		height: 36rpx;
		margin-right: 12rpx;
	}

	.gender-text {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.field-picker {
		height: 96rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.field-picker-text {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.field-calendar {
		width: 36rpx;
		height: 36rpx;
		flex-shrink: 0;
	}

	.footer {
		padding-top: 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.save-btn {
		position: relative;
		width: 100%;
		height: 96rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.save-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.save-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.skip-link {
		margin-top: 28rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}
</style>
