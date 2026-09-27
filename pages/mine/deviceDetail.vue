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
					<text v-if="connected && batteryCharging" class="device-estimate">充电中</text>
					<text v-else-if="connected && estimateDays" class="device-estimate">预计可用{{ estimateDays }}天</text>
				</view>
			</view>
		</view>

		<!-- 设备名称 -->
		<view class="section">
			<text class="section-title">设备名称</text>
			<view class="menu-card">
				<view class="menu-row" @click="onEditName">
					<text class="menu-label">设备名称</text>
					<view class="menu-right">
						<text class="menu-value">{{ deviceName }}</text>
						<image class="menu-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
					</view>
				</view>
				<view class="menu-divider"></view>
				<view class="menu-row" @click="onCheckFirmware">
					<text class="menu-label">固件版本</text>
					<view class="menu-right">
						<text class="menu-value muted">{{ firmware }}</text>
						<!-- 检测升级图标，自行补充 src -->
						<image class="fw-icon" src="/static/shuaxin.png" mode=""></image>
					</view>
				</view>
			</view>
		</view>

		<!-- 设备关系 -->
		<view class="section">
			<text class="section-title">设备关系</text>
			<view class="menu-card">
				<view class="menu-row unbind-row" @click="openUnbindPopup">
					<view class="unbind-left">
						<text class="unbind-title">断开并解绑</text>
						<text class="unbind-desc">解绑后停止同步健康数据</text>
					</view>
					<image class="menu-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
				</view>
			</view>
		</view>

		<!-- 调试：蓝牙全量信息 -->
		<view class="section">
			<view class="debug-head">
				<text class="section-title debug-title">蓝牙调试信息</text>
				<text class="debug-refresh" @click="dumpBleInfo">刷新打印</text>
			</view>
			<view class="debug-card">
				<text class="debug-line" selectable>{{ bleDebugText }}</text>
			</view>
		</view>

		<!-- 断开确认弹窗 -->
		<view v-if="showUnbindPopup" class="unbind-mask" @click="closeUnbindPopup">
			<view class="unbind-popup" @click.stop>
				<image class="unbind-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="unbind-popup-body">
					<image class="unbind-icon" src="/static/jinggao.png" mode="aspectFit"></image>
					<text class="unbind-popup-title">断开 {{ deviceName }}</text>
					<text class="unbind-popup-desc">历史数据会保留，但无法继续测量</text>
					<view class="unbind-btns">
						<view class="unbind-btn ghost" @click="closeUnbindPopup">
							<text class="unbind-btn-text">取消</text>
						</view>
						<view class="unbind-btn solid" @click="confirmUnbind">
							<image class="unbind-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="unbind-btn-text">确认断开</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'
	import CoolWearBLE from '@/common/coolwear-ble.js'

	const BLE_EVENT_NAMES = [
		'onDeviceFound',
		'onConnectStateChange',
		'onPairFinish',
		'onBattery',
		'onHeartRate',
		'onHeartRateDaily',
		'onHRV',
		'onTemperature',
		'onBloodOxygen',
		'onSleep',
		'onSport',
		'onMixSport',
		'onDeviceInfo'
	]

	export default {
		data() {
			return {
				deviceName: '智能戒指',
				battery: null,
				batteryCharging: false,
				connected: false,
				estimateDays: '',
				firmware: '--',
				latestFirmware: '--',
				showUnbindPopup: false,
				checkingFirmware: false,
				unsub: null,
				eventUnsubs: [],
				bleSnapshot: null,
				bleEvents: {},
				isConnectedRaw: null
			}
		},
		computed: {
			statusMeta() {
				if (!this.connected) return '未连接'
				if (this.battery != null) {
					return this.batteryCharging
						? `已连接 · 电量 ${this.battery}% · 充电中`
						: `已连接 · 电量 ${this.battery}%`
				}
				return this.batteryCharging ? '已连接 · 充电中' : '已连接'
			},
			bleDebugText() {
				const bat = (this.bleSnapshot && this.bleSnapshot.battery) || null
				const eventBat = this.bleEvents.onBattery || null
				const payload = {
					pluginOk: CoolWearBLE.isAvailable(),
					isConnectedRaw: this.isConnectedRaw,
					batteryLevel: this.battery,
					batteryCharging: this.batteryCharging,
					firmware: this.firmware,
					batteryFromService: bat,
					onBatteryEvent: eventBat,
					onDeviceInfoEvent: this.bleEvents.onDeviceInfo || null,
					bleServiceState: this.bleSnapshot,
					storageConnectedDevice: uni.getStorageSync('connectedDevice') || null,
					latestEvents: this.bleEvents
				}
				try {
					return JSON.stringify(payload, null, 2)
				} catch (e) {
					return String(payload)
				}
			}
		},
		onLoad(options) {
			if (options && options.firmware) {
				this.firmware = decodeURIComponent(options.firmware)
			}
			if (options && options.days) {
				this.estimateDays = Number(options.days) || this.estimateDays
			}
			this.unsub = BleService.subscribe((s) => {
				this.connected = !!s.connected
				this.battery = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				this.deviceName = (s.device && s.device.name) || '智能戒指'
				this.firmware = s.firmware || (s.device && s.device.firmware) || this.firmware || '--'
				this.bleSnapshot = s
				if (this.battery != null && this.battery > 0) {
					this.estimateDays = Math.max(1, Math.round(this.battery / 20))
				}
				console.log('[deviceDetail] BleService state 更新', JSON.parse(JSON.stringify(s)))
			})
			this.bindBleEventLogs()
			this.dumpBleInfo()
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
			this.eventUnsubs.forEach((off) => off && off())
			this.eventUnsubs = []
		},
		methods: {
			bindBleEventLogs() {
				BLE_EVENT_NAMES.forEach((event) => {
					const off = CoolWearBLE.on(event, (data) => {
						this.$set(this.bleEvents, event, data)
						console.log(`[deviceDetail][BLE事件] ${event}`, data)
					})
					this.eventUnsubs.push(off)
				})
			},
			async dumpBleInfo() {
				const state = BleService.getState()
				this.bleSnapshot = state
				const stored = uni.getStorageSync('connectedDevice') || null
				console.log('========== [deviceDetail] 蓝牙全量信息 ==========')
				console.log('[1] 插件可用:', CoolWearBLE.isAvailable())
				console.log('[2] BleService.getState():', JSON.parse(JSON.stringify(state)))
				console.log('[3] storage connectedDevice:', stored)

				try {
					const res = await CoolWearBLE.isConnected()
					this.isConnectedRaw = res
					console.log('[4] CoolWearBLE.isConnected():', res)
				} catch (e) {
					this.isConnectedRaw = { error: (e && e.message) || String(e) }
					console.log('[4] CoolWearBLE.isConnected() 失败:', e)
				}

				if (state.connected) {
					try {
						await CoolWearBLE.sendSyncInfo()
						console.log('[5] sendSyncInfo 已发送，等待 onDeviceInfo / 健康数据事件...')
					} catch (e) {
						console.log('[5] sendSyncInfo 失败:', e)
					}
					try {
						await CoolWearBLE.requestBattery()
						console.log('[6] requestBattery 已发送，等待 onBattery...')
					} catch (e) {
						console.log('[6] requestBattery 失败:', e)
					}
					try {
						await CoolWearBLE.requestDeviceInfo()
						console.log('[7] requestDeviceInfo 已发送，等待 onDeviceInfo / firmware...')
					} catch (e) {
						console.log('[7] requestDeviceInfo 失败(可能旧基座无此方法):', e)
					}
				} else {
					console.log('[5/6/7] 当前未连接，跳过同步指令')
				}
				console.log('[7] 页面最新事件缓存 latestEvents:', JSON.parse(JSON.stringify(this.bleEvents)))
				console.log('========== [deviceDetail] 打印结束 ==========')
			},
			onEditName() {
				uni.showModal({
					title: '修改设备名称',
					editable: true,
					placeholderText: '请输入设备名称',
					content: this.deviceName,
					success: (res) => {
						if (!res.confirm) return
						const name = (res.content || '').trim()
						if (!name) {
							uni.showToast({
								title: '名称不能为空',
								icon: 'none'
							})
							return
						}
						this.deviceName = name
						BleService.updateDeviceName(name)
						uni.showToast({
							title: '已修改',
							icon: 'success'
						})
					}
				})
			},
			onCheckFirmware() {
				if (this.checkingFirmware) return
				this.checkingFirmware = true
				uni.showLoading({
					title: '检测中...',
					mask: true
				})
				setTimeout(() => {
					uni.hideLoading()
					this.checkingFirmware = false
					if (this.firmware === this.latestFirmware) {
						uni.showToast({
							title: '已是最新版本',
							icon: 'none'
						})
						return
					}
					uni.showModal({
						title: '发现新固件',
						content: `当前 ${this.firmware}，最新 ${this.latestFirmware}，是否立即升级？`,
						confirmText: '升级',
						success: (res) => {
							if (!res.confirm) return
							uni.showLoading({
								title: '升级中...',
								mask: true
							})
							setTimeout(() => {
								uni.hideLoading()
								this.firmware = this.latestFirmware
								uni.showToast({
									title: '升级完成',
									icon: 'success'
								})
							}, 1200)
						}
					})
				}, 1000)
			},
			openUnbindPopup() {
				this.showUnbindPopup = true
			},
			closeUnbindPopup() {
				this.showUnbindPopup = false
			},
			async confirmUnbind() {
				this.showUnbindPopup = false
				uni.showLoading({
					title: '断开中...',
					mask: true
				})
				try {
					await BleService.disconnect(true)
					uni.hideLoading()
					uni.showToast({
						title: '已断开并解绑',
						icon: 'none'
					})
					setTimeout(() => {
						uni.redirectTo({
							url: '/pages/mine/deviceManage'
						})
					}, 500)
				} catch (e) {
					uni.hideLoading()
					uni.showToast({
						title: (e && e.message) || '断开失败',
						icon: 'none'
					})
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.status-card {
		position: relative;
		width: 100%;
		height: 169.22rpx;
		border-radius: 28rpx;
		overflow: hidden;
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

	.device-estimate {
		margin-top: 4rpx;
		padding-left: 30rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.section {
		margin-top: 36rpx;
	}

	.section-title {
		display: block;
		margin-bottom: 16rpx;
		padding-left: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.debug-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
		padding-left: 8rpx;
		padding-right: 8rpx;
	}

	.debug-title {
		margin-bottom: 0;
		padding-left: 0;
	}

	.debug-refresh {
		font-size: 24rpx;
		line-height: 34rpx;
		color: #4da3ff;
	}

	.debug-card {
		padding: 24rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		box-sizing: border-box;
		max-height: 560rpx;
		overflow-y: auto;
	}

	.debug-line {
		display: block;
		font-size: 22rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.7);
		white-space: pre-wrap;
		word-break: break-all;
	}

	.menu-card {
		padding: 0 28rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		box-sizing: border-box;
	}

	.menu-row {
		min-height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.menu-divider {
		height: 1rpx;
		background: rgba(255, 255, 255, 0.08);
	}

	.menu-label {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.menu-right {
		display: flex;
		align-items: center;
		flex-shrink: 0;
	}

	.menu-value {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.menu-value.muted {
		color: rgba(255, 255, 255, 0.55);
	}

	.menu-arrow {
		width: 24rpx;
		height: 24rpx;
		margin-left: 8rpx;
		opacity: 0.45;
	}

	.fw-icon {
		width: 32rpx;
		height: 32rpx;
		margin-left: 12rpx;
		opacity: 0.7;
	}

	.unbind-row {
		padding: 28rpx 0;
		min-height: auto;
	}

	.unbind-left {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.unbind-title {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #4da3ff;
	}

	.unbind-desc {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.unbind-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0 48rpx;
		box-sizing: border-box;
	}

	.unbind-popup {
		position: relative;
		width: 100%;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
	}

	.unbind-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.unbind-popup-body {
		position: relative;
		z-index: 1;
		padding: 48rpx 32rpx 36rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-sizing: border-box;
	}

	.unbind-icon {
		width: 88rpx;
		height: 88rpx;
	}

	.unbind-popup-title {
		margin-top: 24rpx;
		font-size: 34rpx;
		font-weight: 600;
		line-height: 48rpx;
		color: #ffffff;
		text-align: center;
	}

	.unbind-popup-desc {
		margin-top: 12rpx;
		font-size: 26rpx;
		line-height: 38rpx;
		color: rgba(255, 255, 255, 0.45);
		text-align: center;
	}

	.unbind-btns {
		margin-top: 40rpx;
		width: 100%;
		display: flex;
		align-items: center;
	}

	.unbind-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		border-radius: 20rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.unbind-btn.ghost {
		margin-right: 20rpx;
		background: transparent;
		border: 2rpx solid rgba(77, 163, 255, 0.7);
	}

	.unbind-btn.solid {
		background: transparent;
	}

	.unbind-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.unbind-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 500;
		line-height: 42rpx;
		color: #ffffff;
	}
</style>
