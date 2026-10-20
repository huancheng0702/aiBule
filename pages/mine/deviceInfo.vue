<template>
	<view class="page">
		<!-- 设备状态卡 -->
		<view class="status-card">
			<image class="status-card-bg" src="/static/ringStatusBg.png" mode=""></image>
			<view class="status-card-body">
				<view class="status-left">
					<view class="name-row">
						<view class="status-dot" :class="{ off: !connected }"></view>
						<text class="device-name">{{ deviceName }}</text>
					</view>
					<text class="device-meta">{{ statusMeta }}</text>
				</view>
			</view>
		</view>

		<!-- 信息列表 -->
		<view class="info-card">
			<view class="info-row">
				<text class="info-label">连接状态</text>
				<view class="info-value-row">
					<view class="info-dot" :class="{ off: !connected }"></view>
					<text class="info-value">{{ connectStatus }}</text>
				</view>
			</view>
			<view class="info-divider"></view>
			<view class="info-row">
				<text class="info-label">固件版本</text>
				<text class="info-value muted">{{ firmware }}</text>
			</view>
		</view>

		<!-- 底部按钮 -->
		<view class="footer">
			<view class="action-btn" @click="onDeviceSetting">
				<image class="action-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="action-btn-text">设备详情与设置</text>
			</view>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'

	export default {
		data() {
			return {
				deviceName: '智能戒指',
				battery: null,
				batteryCharging: false,
				connected: false,
				connectStatus: '未连接',
				firmware: '--',
				unsub: null
			}
		},
		computed: {
			statusMeta() {
				if (!this.connected) return this.connectStatus
				if (this.battery != null) {
					return this.batteryCharging
						? `已连接 · 电量 ${this.battery}% · 充电中`
						: `已连接 · 电量 ${this.battery}%`
				}
				return this.batteryCharging ? '已连接 · 充电中' : '已连接'
			}
		},
		onLoad() {
			this.unsub = BleService.subscribe((s) => {
				this.connected = !!s.connected
				this.battery = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				this.deviceName = (s.device && s.device.name) || '智能戒指'
				this.connectStatus = s.connected ? '正常' : (s.statusText || '未连接')
				this.firmware = s.firmware || (s.device && s.device.firmware) || '--'
			})
			if (this.connected) {
				BleService.refreshBattery().catch(() => {})
				BleService.refreshDeviceInfo().catch(() => {})
			}
		},
		onUnload() {
			if (this.unsub) {
				this.unsub()
				this.unsub = null
			}
		},
		methods: {
			onDeviceSetting() {
				const name = encodeURIComponent(this.deviceName)
				const battery = this.battery != null ? this.battery : ''
				uni.navigateTo({
					url: `/pages/mine/deviceDetail?name=${name}&battery=${battery}&firmware=${encodeURIComponent(this.firmware)}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		padding: 24rpx 28rpx 0;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
		display: flex;
		flex-direction: column;
	}

	.status-card {
		position: relative;
		width: 100%;
		height: 169.22rpx;
		border-radius: 28rpx;
		overflow: hidden;
		flex-shrink: 0;
	}

	.status-card-bg {
		width: 100%;
		height: 100%;
	}

	.status-card-body {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		padding: 0 36rpx;
		box-sizing: border-box;
		display: flex;
		align-items: center;
		z-index: 1;
	}

	.status-left {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.name-row {
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

	.status-dot.off {
		background: rgba(255, 255, 255, 0.35);
		box-shadow: none;
	}

	.device-name {
		font-size: 34rpx;
		font-weight: 700;
		line-height: 48rpx;
		color: #ffffff;
	}

	.device-meta {
		margin-top: 8rpx;
		padding-left: 30rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.info-card {
		margin-top: 28rpx;
		padding: 0 28rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		box-sizing: border-box;
	}

	.info-row {
		min-height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.info-divider {
		height: 1rpx;
		background: rgba(255, 255, 255, 0.08);
	}

	.info-label {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.info-value-row {
		display: flex;
		align-items: center;
	}

	.info-dot {
		width: 12rpx;
		height: 12rpx;
		border-radius: 50%;
		background: #34c759;
		margin-right: 12rpx;
	}

	.info-dot.off {
		background: rgba(255, 255, 255, 0.35);
	}

	.info-value {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.info-value.muted {
		color: rgba(255, 255, 255, 0.55);
	}

	.footer {
		margin-top: auto;
		padding: 24rpx 0 48rpx;
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
</style>
