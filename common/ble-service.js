/**
 * 全局蓝牙服务：封装 CoolWearBLE，供业务页共用连接态 / 扫描 / 电量
 */
import CoolWearBLE from '@/common/coolwear-ble.js'

const STORAGE_KEY = 'connectedDevice'
const STATUS_MAP = {
	0: '已断开',
	1: '已连接',
	2: '连接中'
}

const state = {
	inited: false,
	initing: false,
	connectStatus: 0,
	connected: false,
	scanning: false,
	device: null,
	battery: null,
	deviceInfo: null,
	devices: [],
	pluginOk: false
}

const stateListeners = new Set()
let eventsBound = false
let scanTimer = null
let reconnecting = false

function cloneState() {
	return {
		inited: state.inited,
		initing: state.initing,
		connectStatus: state.connectStatus,
		connected: state.connected,
		scanning: state.scanning,
		pluginOk: state.pluginOk,
		device: state.device ? { ...state.device } : null,
		battery: state.battery ? { ...state.battery } : null,
		deviceInfo: state.deviceInfo ? { ...state.deviceInfo } : null,
		devices: state.devices.map((d) => ({ ...d })),
		statusText: STATUS_MAP[state.connectStatus] || '未知',
		batteryLevel: getBatteryLevel(),
		batteryCharging: getBatteryCharging(),
		firmware: getFirmware()
	}
}

function emit() {
	const snapshot = cloneState()
	stateListeners.forEach((fn) => {
		try {
			fn(snapshot)
		} catch (e) {
			console.error('[ble-service] listener error', e)
		}
	})
}

function toBool(v) {
	if (v === true || v === 1 || v === '1' || v === 'true') return true
	if (v === false || v === 0 || v === '0' || v === 'false' || v == null) return false
	return !!v
}

function normalizeBattery(data) {
	if (data == null) return null
	if (typeof data === 'number') {
		return { level: data, charging: false, raw: data }
	}
	if (typeof data === 'string') {
		try {
			return normalizeBattery(JSON.parse(data))
		} catch (e) {
			return null
		}
	}
	const levelRaw = data.level != null
		? data.level
		: (data.battery != null ? data.battery : (data.Battery != null ? data.Battery : null))
	const chargingRaw = data.charging != null
		? data.charging
		: (data.chargerStatus != null
			? data.chargerStatus
			: (data.isChargerStatus != null
				? data.isChargerStatus
				: (data.isCharging != null ? data.isCharging : data.deviceChargerStatus)))
	return {
		level: levelRaw != null && levelRaw !== '' ? Number(levelRaw) : null,
		charging: toBool(chargingRaw),
		raw: data
	}
}

function getBatteryLevel() {
	if (state.battery && state.battery.level != null && !Number.isNaN(Number(state.battery.level))) {
		return Number(state.battery.level)
	}
	if (state.device && state.device.battery != null) return Number(state.device.battery)
	return null
}

function getBatteryCharging() {
	return !!(state.battery && state.battery.charging)
}

function getFirmware() {
	if (state.device && state.device.firmware) return String(state.device.firmware)
	if (state.deviceInfo && state.deviceInfo.firmware) return String(state.deviceInfo.firmware)
	if (state.deviceInfo && state.deviceInfo.softwareVer) return String(state.deviceInfo.softwareVer)
	return null
}

function normalizeDeviceInfo(data) {
	if (!data) return null
	if (typeof data === 'string') {
		try {
			return normalizeDeviceInfo(JSON.parse(data))
		} catch (e) {
			return { raw: data }
		}
	}
	const firmware = data.firmware != null
		? String(data.firmware)
		: (data.softwareVer != null
			? String(data.softwareVer)
			: (data.softwareVersion != null
				? String(data.softwareVersion)
				: (data.SoftwareVer != null ? String(data.SoftwareVer) : (data.version != null ? String(data.version) : null))))
	return {
		...data,
		firmware,
		softwareVer: data.softwareVer != null ? String(data.softwareVer) : firmware,
		hardwareVer: data.hardwareVer != null ? String(data.hardwareVer) : (data.hardwareVersion != null ? String(data.hardwareVersion) : null),
		customerId: data.customerId != null ? data.customerId : data.customer_id,
		name: data.name || data.devName || null,
		deviceId: data.deviceId != null ? String(data.deviceId) : null,
		mac: data.mac || null
	}
}

function deviceKey(item) {
	return item.identifier || item.mac || item.deviceId || item.id || ''
}

function rssiToSignal(rssi) {
	if (rssi == null || rssi === '') return '信号未知'
	const n = Number(rssi)
	if (Number.isNaN(n)) return '信号未知'
	if (n >= -55) return '信号良好'
	if (n >= -70) return '信号一般'
	return '信号较弱'
}

function toListItem(raw) {
	const id = deviceKey(raw)
	const name = raw.name || '未知设备'
	const rssi = raw.rssi
	return {
		id,
		name,
		mac: raw.mac || '',
		identifier: raw.identifier || '',
		deviceId: raw.deviceId || '',
		rssi,
		meta: `${rssiToSignal(rssi)}${rssi != null ? ` · RSSI ${rssi}` : ''}`,
		raw
	}
}

function loadStoredDevice() {
	try {
		const saved = uni.getStorageSync(STORAGE_KEY)
		if (saved && (saved.id || saved.mac || saved.identifier)) {
			state.device = {
				id: saved.id || saved.mac || saved.identifier,
				name: saved.name || '智能戒指',
				mac: saved.mac || '',
				identifier: saved.identifier || '',
				battery: saved.battery != null ? Number(saved.battery) : null,
				firmware: saved.firmware || null,
				customerId: saved.customerId != null ? saved.customerId : null
			}
		}
	} catch (e) {
		// ignore
	}
}

function persistDevice() {
	if (!state.device) return
	const level = getBatteryLevel()
	uni.setStorageSync(STORAGE_KEY, {
		id: state.device.id,
		name: state.device.name,
		mac: state.device.mac || '',
		identifier: state.device.identifier || '',
		battery: level != null ? level : state.device.battery,
		firmware: state.device.firmware || getFirmware() || '',
		customerId: state.device.customerId != null ? state.device.customerId : ''
	})
}

function clearStoredDevice() {
	try {
		uni.removeStorageSync(STORAGE_KEY)
	} catch (e) {
		// ignore
	}
	state.device = null
	state.battery = null
	state.deviceInfo = null
}

function bindEvents() {
	if (eventsBound) return
	eventsBound = true

	CoolWearBLE.on('onDeviceFound', (data) => {
		if (!data) return
		const item = toListItem(data)
		if (!item.id) return
		const idx = state.devices.findIndex((d) => d.id === item.id)
		if (idx >= 0) {
			state.devices.splice(idx, 1, item)
		} else {
			state.devices.push(item)
		}
		emit()
	})

	CoolWearBLE.on('onConnectStateChange', (data) => {
		const status = data && data.status != null ? Number(data.status) : 0
		state.connectStatus = status
		state.connected = status === 1
		if (state.connected) {
			CoolWearBLE.sendSyncInfo().catch(() => {})
			CoolWearBLE.requestBattery().catch(() => {})
			CoolWearBLE.requestDeviceInfo().catch(() => {})
			persistDevice()
		}
		emit()
	})

	CoolWearBLE.on('onBattery', (data) => {
		const normalized = normalizeBattery(data)
		console.log('[ble-service] onBattery raw=', data, 'normalized=', normalized)
		state.battery = normalized
		if (state.device && normalized && normalized.level != null && !Number.isNaN(normalized.level)) {
			state.device.battery = normalized.level
			persistDevice()
		}
		emit()
	})

	CoolWearBLE.on('onDeviceInfo', (data) => {
		const info = normalizeDeviceInfo(data)
		console.log('[ble-service] onDeviceInfo raw=', data, 'normalized=', info)
		state.deviceInfo = info
		if (!state.device) {
			emit()
			return
		}
		if (info) {
			if (info.name) state.device.name = info.name
			if (info.mac) state.device.mac = info.mac
			if (info.firmware) state.device.firmware = info.firmware
			if (info.customerId != null) state.device.customerId = info.customerId
			if (info.deviceId) state.device.id = info.deviceId
		}
		persistDevice()
		emit()
	})
}

function requestAndroidPermissions() {
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
		plus.android.requestPermissions(
			permissions,
			(e) => {
				clearTimeout(timer)
				if (e.deniedAlways && e.deniedAlways.length) {
					reject(new Error('蓝牙/定位权限被永久拒绝，请到系统设置开启'))
				} else if (e.deniedPresent && e.deniedPresent.length) {
					reject(new Error('蓝牙/定位权限被拒绝'))
				} else {
					resolve()
				}
			},
			(err) => {
				clearTimeout(timer)
				reject(new Error((err && err.message) || '权限请求失败'))
			}
		)
		// #endif
		// #ifndef APP-PLUS
		resolve()
		// #endif
	})
}

async function sleep(ms) {
	return new Promise((resolve) => setTimeout(resolve, ms))
}

async function pollConnected(timeoutMs = 30000) {
	const start = Date.now()
	while (Date.now() - start < timeoutMs) {
		try {
			const res = await CoolWearBLE.isConnected()
			if (res && res.connected) {
				state.connected = true
				state.connectStatus = 1
				emit()
				await CoolWearBLE.sendSyncInfo().catch(() => {})
				await CoolWearBLE.requestBattery().catch(() => {})
				return true
			}
		} catch (e) {
			// ignore
		}
		await sleep(1000)
	}
	return state.connected
}

const BleService = {
	STATUS_MAP,

	bootstrap() {
		loadStoredDevice()
		state.pluginOk = CoolWearBLE.isAvailable()
		bindEvents()
		emit()
	},

	/**
	 * 有本地绑定设备且未解绑时自动重连；解绑会清 STORAGE，不会触发
	 */
	async tryAutoReconnect() {
		// #ifndef APP-PLUS
		return false
		// #endif
		// #ifdef APP-PLUS
		loadStoredDevice()
		const device = state.device
		if (!device || !(device.mac || device.identifier || device.id)) {
			return false
		}
		if (state.connected) return true
		if (reconnecting || state.connectStatus === 2) return false

		reconnecting = true
		state.connectStatus = 2
		state.connected = false
		emit()

		try {
			if (!CoolWearBLE.isAvailable()) {
				throw new Error('原生插件未加载')
			}
			await BleService.ensureInit()

			try {
				const res = await CoolWearBLE.isConnected()
				if (res && res.connected) {
					state.connected = true
					state.connectStatus = 1
					emit()
					await CoolWearBLE.sendSyncInfo().catch(() => {})
					await CoolWearBLE.requestBattery().catch(() => {})
					persistDevice()
					return true
				}
			} catch (e) {
				// ignore, continue reconnect
			}

			// 优先走 SDK 自动重连（依赖上次连接记录）
			try {
				await CoolWearBLE.reconnect()
				const ok = await pollConnected(20000)
				if (ok) {
					persistDevice()
					return true
				}
			} catch (e) {
				console.log('[ble-service] reconnect() failed, fallback connect', e)
			}

			// 回退：用本地缓存的 mac / identifier 主动连接
			await CoolWearBLE.connect({
				mac: device.mac,
				identifier: device.identifier,
				name: device.name,
				deviceId: device.id
			})
			const ok = await pollConnected(30000)
			if (!ok) {
				state.connectStatus = 0
				state.connected = false
				emit()
				return false
			}
			persistDevice()
			return true
		} catch (e) {
			console.error('[ble-service] tryAutoReconnect', e)
			state.connectStatus = 0
			state.connected = false
			emit()
			return false
		} finally {
			reconnecting = false
		}
		// #endif
	},

	isAvailable() {
		return CoolWearBLE.isAvailable()
	},

	getState() {
		return cloneState()
	},

	subscribe(fn) {
		if (typeof fn !== 'function') return () => {}
		stateListeners.add(fn)
		fn(cloneState())
		return () => stateListeners.delete(fn)
	},

	async requestPermissions() {
		// #ifdef APP-PLUS
		await requestAndroidPermissions()
		// #endif
		uni.setStorageSync('blePermissionGranted', true)
	},

	async ensureInit() {
		if (state.inited) return
		if (state.initing) {
			while (state.initing) {
				await sleep(100)
			}
			if (state.inited) return
		}
		state.pluginOk = CoolWearBLE.isAvailable()
		if (!state.pluginOk) {
			throw new Error('原生插件未加载，请使用包含 CoolWear-BLE 的自定义基座运行')
		}
		state.initing = true
		emit()
		try {
			await BleService.requestPermissions()
			await CoolWearBLE.init()
			state.inited = true
		} finally {
			state.initing = false
			emit()
		}
	},

	async startScan(options = {}) {
		const timeoutMs = options.timeoutMs != null ? options.timeoutMs : 10000
		await BleService.ensureInit()
		if (scanTimer) {
			clearTimeout(scanTimer)
			scanTimer = null
		}
		state.devices = []
		state.scanning = true
		emit()
		await CoolWearBLE.startScan()
		if (timeoutMs > 0) {
			scanTimer = setTimeout(() => {
				BleService.stopScan().catch(() => {})
			}, timeoutMs)
		}
	},

	async stopScan() {
		if (scanTimer) {
			clearTimeout(scanTimer)
			scanTimer = null
		}
		try {
			if (state.inited) {
				await CoolWearBLE.stopScan()
			}
		} finally {
			state.scanning = false
			emit()
		}
	},

	async connect(device) {
		if (!device) throw new Error('请先选择设备')
		await BleService.ensureInit()
		await BleService.stopScan()
		state.connectStatus = 2
		state.connected = false
		state.device = {
			id: device.id || deviceKey(device),
			name: device.name || '智能戒指',
			mac: device.mac || '',
			identifier: device.identifier || '',
			battery: device.battery != null ? Number(device.battery) : null
		}
		emit()
		await CoolWearBLE.connect({
			mac: device.mac,
			identifier: device.identifier,
			name: device.name,
			deviceId: device.deviceId
		})
		const ok = await pollConnected(30000)
		if (!ok) {
			state.connectStatus = 0
			state.connected = false
			emit()
			throw new Error('连接超时，请确认戒指在附近且未被其他手机占用')
		}
		persistDevice()
		uni.setStorageSync('onboardingDone', true)
		uni.setStorageSync('isFirstLogin', false)
		emit()
		return cloneState()
	},

	async disconnect(unbind = false) {
		try {
			if (state.inited) {
				await CoolWearBLE.disconnect()
			}
		} finally {
			state.connected = false
			state.connectStatus = 0
			if (unbind) {
				clearStoredDevice()
			}
			emit()
		}
	},

	async refreshBattery() {
		if (!state.connected) return null
		await CoolWearBLE.requestBattery()
		return state.battery
	},

	async refreshDeviceInfo() {
		if (!state.connected) return null
		try {
			await CoolWearBLE.requestDeviceInfo()
		} catch (e) {
			// 旧基座可能没有 requestDeviceInfo，回退 sendSyncInfo
			await CoolWearBLE.sendSyncInfo().catch(() => {})
		}
		return state.deviceInfo
	},

	updateDeviceName(name) {
		if (!state.device || !name) return
		state.device.name = name
		persistDevice()
		emit()
	}
}

export default BleService
