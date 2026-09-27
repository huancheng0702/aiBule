<template>
	<view class="page">
		<view class="plugin-banner" :class="pluginOk ? 'ok' : 'fail'">
			<text class="banner-text">{{ pluginStatusText }}</text>
		</view>

		<view class="header">
			<text class="title">CoolWear 蓝牙测试</text>
			<text class="status">连接状态: {{ connectStatusText }}</text>
		</view>

		<view class="actions">
			<button class="btn" type="primary" @click="handleInit">初始化 SDK</button>
			<button class="btn" @click="handleStartScan" :disabled="!inited">开始扫描</button>
			<button class="btn" @click="handleStopScan" :disabled="!inited">停止扫描</button>
			<button class="btn" type="warn" @click="handleDisconnect" :disabled="!connected">断开连接</button>
		</view>

		<view class="section">
			<text class="section-title">扫描到的设备</text>
			<view v-if="devices.length === 0" class="empty">暂无设备，请点击开始扫描</view>
			<view
				v-for="(item, index) in devices"
				:key="item.identifier || item.mac || index"
				class="device-item"
				@click="handleConnect(item)"
			>
				<text class="device-name">{{ item.name || '未知设备' }}</text>
				<text class="device-meta">{{ item.mac || item.identifier }} · RSSI {{ item.rssi || '-' }}</text>
			</view>
		</view>

		<view class="section">
			<text class="section-title">健康数据控制</text>
			<view class="control-row">
				<button size="mini" @click="toggleHeartRate(true)" :disabled="!connected">开心率</button>
				<button size="mini" @click="toggleHeartRate(false)" :disabled="!connected">关心率</button>
				<button size="mini" @click="toggleHRV(true)" :disabled="!connected">开 HRV</button>
				<button size="mini" @click="toggleHRV(false)" :disabled="!connected">关 HRV</button>
			</view>
			<view class="control-row">
				<button size="mini" @click="toggleBloodOxygen(true)" :disabled="!connected">开血氧</button>
				<button size="mini" @click="toggleBloodOxygen(false)" :disabled="!connected">关血氧</button>
			</view>
			<view class="control-row">
				<button size="mini" @click="handleSync" :disabled="!connected">同步数据</button>
				<button size="mini" @click="handleBattery" :disabled="!connected">请求电量</button>
			</view>
		</view>

		<view class="section">
			<text class="section-title">实时数据</text>
			<view class="data-block">
				<text class="label">电量</text>
				<text class="value">{{ batteryText }}</text>
			</view>
			<view class="data-block">
				<text class="label">心率</text>
				<text class="value">{{ heartRateText }}</text>
			</view>
			<view class="data-block">
				<text class="label">HRV</text>
				<text class="value">{{ hrvText }}</text>
			</view>
			<view class="data-block">
				<text class="label">体温</text>
				<text class="value">{{ tempText }}</text>
			</view>
			<view class="data-block">
				<text class="label">血氧</text>
				<text class="value">{{ o2Text }}</text>
			</view>
			<view class="data-block">
				<text class="label">睡眠</text>
				<text class="value">{{ sleepText }}</text>
			</view>
			<view class="data-block">
				<text class="label">运动</text>
				<text class="value">{{ sportText }}</text>
			</view>
		</view>

		<view class="section">
			<text class="section-title">日志</text>
			<scroll-view scroll-y class="log-box">
				<text v-for="(log, idx) in logs" :key="idx" class="log-line">{{ log }}</text>
			</scroll-view>
		</view>
	</view>
</template>

<script>
import CoolWearBLE from '@/common/coolwear-ble.js'

const STATUS_MAP = {
	0: '已断开',
	1: '已连接',
	2: '连接中'
}

export default {
	data() {
		return {
			pluginOk: false,
			pluginStatusText: '正在检测原生插件...',
			inited: false,
			connected: false,
			connectStatus: 0,
			devices: [],
			battery: null,
			heartRate: [],
			hrv: [],
			temperature: [],
			bloodOxygen: [],
			sleep: null,
			sport: [],
			logs: [],
			unsubs: []
		}
	},
	computed: {
		connectStatusText() {
			return STATUS_MAP[this.connectStatus] || '未知'
		},
		batteryText() {
			if (!this.battery) return '-'
			return `${this.battery.level}%${this.battery.charging ? ' (充电中)' : ''}`
		},
		heartRateText() {
			return this.formatList(this.heartRate, 'value')
		},
		hrvText() {
			return this.formatList(this.hrv, 'value')
		},
		tempText() {
			return this.formatList(this.temperature, 'value')
		},
		o2Text() {
			if (!this.bloodOxygen.length) return '-'
			return JSON.stringify(this.bloodOxygen.slice(-3))
		},
		sleepText() {
			if (!this.sleep) return '-'
			return typeof this.sleep === 'string' ? this.sleep : JSON.stringify(this.sleep)
		},
		sportText() {
			if (!this.sport.length) return '-'
			const last = this.sport[this.sport.length - 1]
			if (last.distance !== undefined || last.calories !== undefined) {
				return `距离 ${last.distance || 0}m · 卡路里 ${last.calories || 0}`
			}
			return JSON.stringify(last)
		}
	},
	onLoad() {
		console.log('[ble-test] onLoad')
		this.bindEvents()
		this.checkPlugin()
	},
	onReady() {
		console.log('[ble-test] onReady, pluginOk=', this.pluginOk)
	},
	onUnload() {
		this.unsubs.forEach((off) => off && off())
		this.unsubs = []
	},
	methods: {
		checkPlugin() {
			// #ifdef APP-PLUS
			const mod = CoolWearBLE.getNativeModule()
			const ok = CoolWearBLE.isAvailable()
			this.pluginOk = ok
			console.log('[ble-test] checkPlugin mod=', mod, 'ok=', ok)
			if (!ok) {
				this.pluginStatusText = '原生插件未加载 — 请重新制作自定义基座'
				this.addLog('未检测到原生插件 CoolWear-BLE')
				uni.showModal({
					title: '插件未加载',
					content: '当前自定义基座可能不含 CoolWear-BLE 插件。\n\n请：\n1. 运行 → 运行到手机 → 制作自定义调试基座\n2. 安装新基座后再选「使用自定义基座运行」',
					showCancel: false
				})
			} else {
				this.pluginStatusText = '原生插件已就绪，可点击「初始化 SDK」'
				this.addLog('原生插件已加载')
			}
			// #endif
			// #ifndef APP-PLUS
			this.pluginStatusText = '非 App 环境，插件不可用'
			this.addLog('当前不是 App 环境，蓝牙插件不可用')
			// #endif
		},
		addLog(msg) {
			console.log('[ble-test]', msg)
			const time = new Date().toLocaleTimeString()
			this.logs.unshift(`[${time}] ${msg}`)
			if (this.logs.length > 100) {
				this.logs.pop()
			}
		},
		formatList(list, key) {
			if (!list || !list.length) return '-'
			return list.map((item) => item[key]).join(', ')
		},
		bindEvents() {
			const events = {
				onDeviceFound: (data) => {
					const id = data.identifier || data.mac
					const exists = this.devices.find((d) => (d.identifier || d.mac) === id)
					if (!exists) {
						this.devices.push(data)
					}
					this.addLog(`发现设备: ${data.name || id}`)
				},
				onConnectStateChange: (data) => {
					this.connectStatus = data.status
					this.connected = data.status === 1
					this.addLog(`连接状态变更: ${STATUS_MAP[data.status] || data.status}`)
					if (this.connected) {
						CoolWearBLE.sendSyncInfo().catch(() => {})
					}
				},
				onPairFinish: (data) => {
					this.addLog(`配对完成: ${JSON.stringify(data)}`)
				},
				onBattery: (data) => {
					this.battery = data
				},
				onHeartRate: (data) => {
					this.heartRate = Array.isArray(data) ? data : [data]
				},
				onHRV: (data) => {
					this.hrv = Array.isArray(data) ? data : [data]
				},
				onTemperature: (data) => {
					this.temperature = Array.isArray(data) ? data : [data]
				},
				onBloodOxygen: (data) => {
					this.bloodOxygen = Array.isArray(data) ? data : [data]
				},
				onSleep: (data) => {
					this.sleep = data
				},
				onSport: (data) => {
					this.sport = Array.isArray(data) ? data : [data]
				},
				onDeviceInfo: (data) => {
					this.addLog(`设备信息: ${JSON.stringify(data)}`)
				}
			}
			Object.keys(events).forEach((event) => {
				this.unsubs.push(CoolWearBLE.on(event, events[event]))
			})
		},
		async handleInit() {
			console.log('[ble-test] handleInit clicked')
			uni.showToast({ title: '正在初始化...', icon: 'loading', duration: 2000 })
			this.addLog('点击初始化 SDK...')
			if (!CoolWearBLE.isAvailable()) {
				const msg = '原生插件未加载，请先重新制作自定义基座'
				this.addLog(msg)
				uni.showToast({ title: msg, icon: 'none' })
				return
			}
			try {
				// #ifdef APP-PLUS
				await this.requestAndroidPermissions()
				// #endif
				await CoolWearBLE.init()
				this.inited = true
				this.addLog('SDK 初始化成功')
				uni.showToast({ title: '初始化成功', icon: 'success' })
			} catch (e) {
				const msg = (e && e.message) ? e.message : String(e)
				this.addLog(`初始化失败: ${msg}`)
				uni.showToast({ title: msg, icon: 'none' })
			}
		},
		requestAndroidPermissions() {
			return new Promise((resolve, reject) => {
				// #ifdef APP-PLUS
				const permissions = [
					'android.permission.ACCESS_FINE_LOCATION',
					'android.permission.ACCESS_COARSE_LOCATION',
					'android.permission.BLUETOOTH_SCAN',
					'android.permission.BLUETOOTH_CONNECT'
				]
				const timer = setTimeout(() => {
					reject(new Error('权限请求超时，请到系统设置中手动授予蓝牙和定位权限'))
				}, 15000)
				plus.android.requestPermissions(permissions, (e) => {
					clearTimeout(timer)
					if (e.deniedAlways && e.deniedAlways.length) {
						reject(new Error('蓝牙/定位权限被永久拒绝，请到系统设置开启'))
					} else if (e.deniedPresent && e.deniedPresent.length) {
						reject(new Error('蓝牙/定位权限被拒绝'))
					} else {
						resolve()
					}
				}, (err) => {
					clearTimeout(timer)
					reject(new Error((err && err.message) || '权限请求失败'))
				})
				// #endif
				// #ifndef APP-PLUS
				resolve()
				// #endif
			})
		},
		async handleStartScan() {
			this.addLog('点击开始扫描...')
			try {
				this.devices = []
				await CoolWearBLE.startScan()
				this.addLog('开始扫描')
			} catch (e) {
				this.addLog(`扫描失败: ${(e && e.message) || e}`)
			}
		},
		async handleStopScan() {
			this.addLog('点击停止扫描...')
			try {
				await CoolWearBLE.stopScan()
				this.addLog('停止扫描')
			} catch (e) {
				this.addLog(`停止扫描失败: ${(e && e.message) || e}`)
			}
		},
		async handleConnect(device) {
			try {
				this.connectStatus = 2
				this.connected = false
				await CoolWearBLE.stopScan()
				await CoolWearBLE.connect({
					mac: device.mac,
					identifier: device.identifier,
					name: device.name,
					deviceId: device.deviceId
				})
				this.addLog(`正在连接: ${device.name || device.mac || device.identifier}`)
				this.pollConnection(30000)
			} catch (e) {
				this.connectStatus = 0
				this.addLog(`连接失败: ${e.message}`)
			}
		},
		async pollConnection(timeoutMs = 30000) {
			const start = Date.now()
			while (Date.now() - start < timeoutMs) {
				try {
					const res = await CoolWearBLE.isConnected()
					if (res && res.connected) {
						this.connected = true
						this.connectStatus = 1
						this.addLog('轮询确认：设备已连接')
						await CoolWearBLE.sendSyncInfo().catch(() => {})
						return
					}
				} catch (e) {
					// ignore polling errors
				}
				await new Promise((resolve) => setTimeout(resolve, 1000))
			}
			if (!this.connected) {
				this.addLog('连接超时，请确认戒指在附近且未被其他手机占用')
			}
		},
		async handleDisconnect() {
			try {
				await CoolWearBLE.disconnect()
				this.connected = false
				this.connectStatus = 0
				this.addLog('已断开连接')
			} catch (e) {
				this.addLog(`断开失败: ${e.message}`)
			}
		},
		async toggleHeartRate(open) {
			try {
				await CoolWearBLE.setHeartRateSwitch(open)
				this.addLog(open ? '已开启心率检测' : '已关闭心率检测')
			} catch (e) {
				this.addLog(e.message)
			}
		},
		async toggleHRV(open) {
			try {
				await CoolWearBLE.setHRVSwitch(open)
				this.addLog(open ? '已开启 HRV 检测' : '已关闭 HRV 检测')
			} catch (e) {
				this.addLog(e.message)
			}
		},
		async toggleBloodOxygen(open) {
			try {
				await CoolWearBLE.setBloodOxygenSwitch(open)
				this.addLog(open ? '已开启血氧检测' : '已关闭血氧检测')
			} catch (e) {
				this.addLog(e.message)
			}
		},
		async handleSync() {
			try {
				await CoolWearBLE.sendSyncInfo()
				this.addLog('已发送同步指令')
			} catch (e) {
				this.addLog(e.message)
			}
		},
		async handleBattery() {
			try {
				await CoolWearBLE.requestBattery()
				this.addLog('已请求电量')
			} catch (e) {
				this.addLog(e.message)
			}
		}
	}
}
</script>

<style>
.page {
	padding: 24rpx;
	background: #f5f6f8;
	min-height: 100vh;
}

.plugin-banner {
	padding: 20rpx 24rpx;
	border-radius: 12rpx;
	margin-bottom: 20rpx;
}

.plugin-banner.ok {
	background: #e8ffea;
}

.plugin-banner.fail {
	background: #ffece8;
}

.banner-text {
	font-size: 26rpx;
	line-height: 1.5;
	color: #1d2129;
}

.header {
	margin-bottom: 24rpx;
}

.title {
	display: block;
	font-size: 40rpx;
	font-weight: 600;
	color: #1d2129;
}

.status {
	display: block;
	margin-top: 8rpx;
	font-size: 28rpx;
	color: #4e5969;
}

.actions {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	margin-bottom: 24rpx;
}

.btn {
	flex: 1 1 40%;
	font-size: 28rpx;
}

.section {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 24rpx;
}

.section-title {
	display: block;
	font-size: 30rpx;
	font-weight: 600;
	margin-bottom: 16rpx;
	color: #1d2129;
}

.empty {
	color: #86909c;
	font-size: 26rpx;
}

.device-item {
	padding: 20rpx 0;
	border-bottom: 1px solid #f2f3f5;
}

.device-name {
	display: block;
	font-size: 30rpx;
	color: #1d2129;
}

.device-meta {
	display: block;
	margin-top: 6rpx;
	font-size: 24rpx;
	color: #86909c;
}

.control-row {
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
	margin-bottom: 12rpx;
}

.data-block {
	display: flex;
	justify-content: space-between;
	padding: 12rpx 0;
	border-bottom: 1px solid #f2f3f5;
}

.label {
	font-size: 28rpx;
	color: #4e5969;
}

.value {
	font-size: 28rpx;
	color: #1d2129;
	max-width: 70%;
	text-align: right;
	word-break: break-all;
}

.log-box {
	max-height: 320rpx;
}

.log-line {
	display: block;
	font-size: 22rpx;
	color: #86909c;
	line-height: 1.6;
}
</style>
