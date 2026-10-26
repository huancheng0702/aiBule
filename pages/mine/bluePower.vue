<template>
	<view class="page">
		<!-- 头部 -->
		<view class="header">
			<view class="header-left">
				<text class="title">连接戒指</text>
				<text class="subtitle">仅在连接戒指时使用蓝牙附近设备权限，你可以在系统设置中随时调整</text>
			</view>
			<view class="step-badge">
				<view class="step-badge-text">首次使用 · <text class="step-num">2</text> /2</view>
			</view>
		</view>

		<!-- 蓝牙权限卡 --> 
		<view class="perm-card">
			<image class="perm-card-bg" src="/static/ringStatusBg.png" mode=""></image>
			<view class="perm-card-body">
				<view class="perm-left">
					<view class="perm-name-row">
						<view class="status-dot"></view>
						<text class="perm-name">蓝牙附近设备</text>
					</view>
					<text class="perm-desc">连接戒指时按需申请</text>
				</view>
				<!-- 戒指示意图，自行补充 src -->
				<image class="perm-ring" mode="aspectFit"></image>
				<view class="use-btn" @click="onUsePermission">
					<!-- 使用按钮背景图，自行补充 src -->
					<image class="use-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
					<text class="use-btn-text">使用</text>
				</view>
			</view>
		</view>

		<!-- 说明卡 -->
		<view class="tip-card">
			<image src="/static/jinshenqing.png" style="width: 100%;height: 142rpx;" mode=""></image>
		</view>

		<!-- 底部按钮 -->
		<view class="footer">
			<view class="action-btn" @click="onAllowContinue">
				<image class="action-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="action-btn-text">允许蓝牙并继续</text>
			</view>
			<text class="skip-link" @click="onSkipConnect">暂不连接</text>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'

	export default {
		data() {
			return {
				requesting: false
			}
		},
		methods: {
			onUsePermission() {
				this.requestBluetooth()
			},
			onAllowContinue() {
				this.requestBluetooth(true)
			},
			onSkipConnect() {
				uni.setStorageSync('onboardingDone', true)
				uni.setStorageSync('isFirstLogin', false)
				uni.reLaunch({
					url: '/pages/index/index'
				})
			},
			async requestBluetooth(goNext) {
				if (this.requesting) return
				this.requesting = true
				uni.showLoading({
					title: '申请中...',
					mask: true
				})
				try {
					// #ifdef APP-PLUS
					if (!BleService.isAvailable()) {
						throw new Error('原生插件未加载，请重新制作并安装自定义调试基座')
					}
					await BleService.requestPermissions()
					await BleService.ensureInit()
					// #endif
					// #ifndef APP-PLUS
					uni.setStorageSync('blePermissionGranted', true)
					// #endif
					uni.hideLoading()
					this.requesting = false
					uni.showToast({
						title: '已开启蓝牙',
						icon: 'success'
					})
					if (goNext) {
						setTimeout(() => {
							this.goDeviceManage()
						}, 400)
					}
				} catch (e) {
					uni.hideLoading()
					this.requesting = false
					const msg = (e && e.message) ? e.message : '需要蓝牙权限'
					// 非 App / 模拟环境：仍允许进入搜索页（搜索页会再提示）
					// #ifndef APP-PLUS
					if (goNext) {
						uni.setStorageSync('blePermissionGranted', true)
						this.goDeviceManage()
						return
					}
					// #endif
					uni.showModal({
						title: '需要蓝牙权限',
						content: msg + '\n\n请在系统设置中开启蓝牙与附近设备/定位权限后重试',
						confirmText: '去设置',
						success: (res) => {
							if (res.confirm) {
								// #ifdef APP-PLUS
								uni.openAppAuthorizeSetting()
								// #endif
								// #ifndef APP-PLUS
								uni.showToast({
									title: '请在系统设置中开启',
									icon: 'none'
								})
								// #endif
							}
						}
					})
				}
			},
			goDeviceManage() {
				uni.redirectTo({
					url: '/pages/mine/deviceManage?from=onboarding',
					fail: () => {
						uni.navigateTo({
							url: '/pages/mine/deviceManage?from=onboarding'
						})
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
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
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 36rpx;
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

	.step-num {
		color: #2c89ff;
	}

	.perm-card {
		position: relative;
		width: 100%;
		height: 169.22rpx;
		margin-top: 48rpx;
		border-radius: 28rpx;
		overflow: hidden;
		flex-shrink: 0;
	}

	.perm-card-bg {
		width: 100%;
		height: 100%;
	}

	.perm-card-body {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		padding: 0 28rpx;
		box-sizing: border-box;
		display: flex;
		align-items: center;
		z-index: 1;
	}

	.perm-left {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.perm-name-row {
		display: flex;
		align-items: center;
	}

	.status-dot {
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		background: #34c759;
		box-shadow: 0 0 12rpx rgba(52, 199, 89, 0.8);
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.perm-name {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.perm-desc {
		margin-top: 8rpx;
		padding-left: 30rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.perm-ring {
		width: 100rpx;
		height: 90rpx;
		margin: 0 12rpx;
		flex-shrink: 0;
	}

	.use-btn {
		position: relative;
		width: 120rpx;
		height: 56rpx;
		border-radius: 999rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.use-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.use-btn-text {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		line-height: 36rpx;
		color: #ffffff;
	}

	.tip-card {
		margin-top: 28rpx;
	}

	.tip-icon {
		width: 44rpx;
		height: 44rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
	}

	.tip-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.tip-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.tip-desc {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.footer {
		margin-top: 100%;
		padding-top: 40rpx;
	}

	.action-btn {
		position: relative;
		width: 100%;
		height: 96rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.action-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.action-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.skip-link {
		margin-top: 28rpx;
		display: block;
		text-align: center;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}
</style>
