<template>
	<view class="page">
		<!-- 头部 -->
		<view class="header">
			<view class="header-left">
				<text class="title">完善基础信息</text>
				<text class="subtitle">完善个人资料信息</text>
			</view>
			<view class="step-badge">
				<view class="step-badge-text">首次使用 · <text style="color: #2C89FF;">1</text> /2</view>
			</view>
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
						<!-- 男性图标，自行补充 src -->
						<image class="gender-icon" src="/static/nan.png" mode=""></image>
						<text class="gender-text">男</text>
					</view>
					<view
						class="gender-item"
						:class="{ active: gender === 'female' }"
						@click="gender = 'female'"
					>
						<!-- 女性图标，自行补充 src -->
						<image class="gender-icon" src="/static/nv.png" mode=""></image>
						<text class="gender-text">女</text>
					</view>
				</view>
			</view>

			<!-- 出生日期 -->
			<view class="field">
				<text class="field-label">出生日期</text>
				<picker mode="date" :value="birthday" :end="todayStr" @change="onBirthdayChange">
					<view class="field-picker">
						<text class="field-picker-text">{{ birthday }}</text>
						<!-- 日历图标 -->
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
				<text class="save-btn-text">保存并继续</text>
			</view>
			<text class="skip-link" @click="onSkip">稍后完善</text>
		</view>
	</view>
</template>

<script>
	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function toDateStr(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}

	export default {
		data() {
			return {
				nickname: '小安',
				gender: 'male',
				birthday: '2003-01-24',
				height: '165',
				weight: '52',
				todayStr: toDateStr(new Date())
			}
		},
		onLoad() {
			const cached = uni.getStorageSync('userBasicInfo')
			if (!cached) return
			this.nickname = cached.nickname || this.nickname
			this.gender = cached.gender || this.gender
			this.birthday = cached.birthday || this.birthday
			this.height = cached.height != null ? String(cached.height) : this.height
			this.weight = cached.weight != null ? String(cached.weight) : this.weight
		},
		methods: {
			onBirthdayChange(e) {
				this.birthday = e.detail.value
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
			saveProfile() {
				uni.setStorageSync('userBasicInfo', {
					nickname: String(this.nickname).trim(),
					gender: this.gender,
					birthday: this.birthday,
					height: Number(this.height),
					weight: Number(this.weight)
				})
			},
			onSave() {
				if (!this.validate()) return
				this.saveProfile()
				uni.redirectTo({
					url: '/pages/mine/bluePower'
				})
			},
			onSkip() {
				// 稍后完善：同样进入蓝牙权限申请页
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
