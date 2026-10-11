/**
 * 全局蓝牙服务：封装 CoolWearBLE，供业务页共用连接态 / 扫描 / 电量
 */
import CoolWearBLE from '@/common/coolwear-ble.js'
import { hasLogin } from '@/common/auth.js'
import { uploadDeviceData } from '@/common/api/deviceData.js'

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
	sport: null,
	sleep: null,
	devices: [],
	pluginOk: false
}

const stateListeners = new Set()
let eventsBound = false
let scanTimer = null
let reconnecting = false
let lastUploadedSportKey = ''
let lastUploadedSleepKey = ''

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
		sport: state.sport ? { ...state.sport } : null,
		sleep: state.sleep ? { ...state.sleep } : null,
		devices: state.devices.map((d) => ({ ...d })),
		statusText: STATUS_MAP[state.connectStatus] || '未知',
		batteryLevel: getBatteryLevel(),
		batteryCharging: getBatteryCharging(),
		firmware: getFirmware()
	}
}

/** 解析戒指运动数据：步数、距离(米)、卡路里(原始值，通常需 /1000) */
function normalizeSport(data) {
	const list = Array.isArray(data) ? data : (data != null ? [data] : [])
	if (!list.length) return null
	const item = list[list.length - 1]
	if (!item || typeof item !== 'object') return null

	let steps = Number(
		item.walkSteps != null ? item.walkSteps
			: (item.steps != null ? item.steps
				: (item.step != null ? item.step : NaN))
	)
	let distanceM = Number(item.distance)
	let caloriesRaw = Number(item.calories)

	const raw = item.raw != null ? String(item.raw) : ''
	if ((Number.isNaN(steps) || steps < 0) && raw) {
		const m = raw.match(/walkSteps[=:\s]*(\d+)/i) || raw.match(/steps?[=:\s]*(\d+)/i)
		if (m) steps = Number(m[1])
	}
	if ((Number.isNaN(distanceM) || distanceM < 0) && raw) {
		const m = raw.match(/distance[=:\s]*([\d.]+)/i)
		if (m) distanceM = Number(m[1])
	}
	if ((Number.isNaN(caloriesRaw) || caloriesRaw < 0) && raw) {
		const m = raw.match(/calories[=:\s]*([\d.]+)/i)
		if (m) caloriesRaw = Number(m[1])
	}

	if (Number.isNaN(steps) || steps < 0) steps = 0
	if (Number.isNaN(distanceM) || distanceM < 0) distanceM = 0
	if (Number.isNaN(caloriesRaw) || caloriesRaw < 0) caloriesRaw = 0

	// SDK Demo：calories / 1000 = 千卡
	const caloriesKcal = caloriesRaw >= 1000
		? Math.round(caloriesRaw / 1000)
		: Math.round(caloriesRaw)

	return {
		steps: Math.round(steps),
		distanceM: Math.round(distanceM),
		distanceKm: Math.round((distanceM / 1000) * 10) / 10,
		caloriesRaw,
		calories: caloriesKcal,
		time: item.time != null ? item.time : null,
		updatedAt: Date.now(),
		raw: item
	}
}

function toMinutes(value) {
	const n = Number(value)
	if (Number.isNaN(n) || n < 0) return 0
	return Math.round(n)
}

function parseSleepDetail(detail, typeMap) {
	if (!detail) return []
	let list = detail
	if (typeof detail === 'string') {
		try {
			list = JSON.parse(detail)
		} catch (e) {
			return []
		}
	}
	if (!Array.isArray(list)) return []
	const map = typeMap || {}
	const deepCode = Number(map.deep)
	const lightCode = Number(map.light)
	const remCode = Number(map.rem)
	const moveCode = Number(map.movement)
	const wakeCode = Number(map.wakeup)
	const startCode = Number(map.start)

	return list.map((item) => {
		if (item == null) return null
		if (typeof item === 'number') {
			return { stage: 'light', dur: Math.max(1, item) }
		}
		if (typeof item !== 'object') return null
		const keys = Object.keys(item)
		if (!keys.length) return null
		const code = keys[0]
		const dur = Math.max(1, toMinutes(item[code]))
		const n = Number(code)
		let stage = 'light'
		if (!Number.isNaN(deepCode) && n === deepCode) stage = 'deep'
		else if (!Number.isNaN(lightCode) && n === lightCode) stage = 'light'
		else if (!Number.isNaN(remCode) && n === remCode) stage = 'rem'
		else if ((!Number.isNaN(moveCode) && n === moveCode) || (!Number.isNaN(wakeCode) && n === wakeCode)) stage = 'awake'
		else if (!Number.isNaN(startCode) && n === startCode) stage = 'light'
		else if (code === 'deep' || code === 'light' || code === 'rem' || code === 'awake') stage = code
		else if (n === 3) stage = 'deep'
		else if (n === 2) stage = 'light'
		else if (n === 5 || n === 4) stage = 'awake'
		else if (n === 6) stage = 'rem'
		return { stage, dur }
	}).filter(Boolean)
}

function pickSleepSession(data) {
	if (!data || typeof data !== 'object') return null
	const sessions = Array.isArray(data.sessions) ? data.sessions.filter(Boolean) : []
	if (sessions.length) {
		// 取最近一晚（endTime / sleepDay 最大）
		return sessions.slice().sort((a, b) => {
			const ea = Number(a.endTime || a.sleepDay || a.startTime || 0)
			const eb = Number(b.endTime || b.sleepDay || b.startTime || 0)
			return ea - eb
		})[sessions.length - 1]
	}
	return data
}

/** 解析戒指睡眠数据（分钟制） */
function normalizeSleep(data) {
	if (data == null) return null
	if (typeof data === 'string') {
		try {
			return normalizeSleep(JSON.parse(data))
		} catch (e) {
			return null
		}
	}
	if (typeof data !== 'object') return null

	const typeMap = data.typeMap || {}
	const session = pickSleepSession(data)
	if (!session) return null

	let deepMin = toMinutes(session.deepTime)
	let lightMin = toMinutes(session.lightTime)
	let remMin = toMinutes(session.remTime)
	let awakeMin = toMinutes(session.movementTime)
	let totalMin = toMinutes(session.sleepTime)
	const startSec = Number(session.startTime) || 0
	const endSec = Number(session.endTime) || 0
	const sleepDay = Number(session.sleepDay) || 0

	let segments = parseSleepDetail(session.sleepDetail || data.sleepDetail, typeMap)
	if (!segments.length && Array.isArray(session.sleeps) && session.sleeps.length > 1) {
		const sleeps = session.sleeps
		for (let i = 1; i < sleeps.length; i++) {
			const prev = sleeps[i - 1]
			const cur = sleeps[i]
			const prevType = prev.sleepType != null ? prev.sleepType : prev.type
			const prevTime = Number(prev.sleepTime != null ? prev.sleepTime : prev.time) || 0
			const curTime = Number(cur.sleepTime != null ? cur.sleepTime : cur.time) || 0
			const keep = Math.max(1, Math.round((curTime - prevTime) / 60))
			segments.push(...parseSleepDetail([{ [String(prevType)]: keep }], typeMap))
		}
	}

	if (segments.length) {
		const sum = { deep: 0, light: 0, rem: 0, awake: 0 }
		segments.forEach((s) => {
			if (sum[s.stage] != null) sum[s.stage] += s.dur
		})
		if (!deepMin) deepMin = sum.deep
		if (!lightMin) lightMin = sum.light
		if (!remMin) remMin = sum.rem
		if (!awakeMin) awakeMin = sum.awake
		if (!totalMin) totalMin = sum.deep + sum.light + sum.rem + sum.awake
	}

	if (!totalMin && startSec && endSec && endSec > startSec) {
		totalMin = Math.round((endSec - startSec) / 60)
	}
	if (!totalMin) totalMin = deepMin + lightMin + remMin + awakeMin

	if (!segments.length && totalMin > 0) {
		const parts = [
			{ stage: 'deep', dur: deepMin },
			{ stage: 'light', dur: lightMin },
			{ stage: 'rem', dur: remMin },
			{ stage: 'awake', dur: awakeMin }
		].filter((p) => p.dur > 0)
		segments = parts.length ? parts : [{ stage: 'light', dur: Math.max(1, totalMin) }]
	}

	const hours = Math.floor(totalMin / 60)
	const minutes = totalMin % 60

	function fmtClock(sec) {
		if (!sec) return '--:--'
		const d = new Date(sec * 1000)
		const h = String(d.getHours()).padStart(2, '0')
		const m = String(d.getMinutes()).padStart(2, '0')
		return `${h}:${m}`
	}

	const structureBase = totalMin > 0 ? totalMin : 1
	const structure = {
		deep: deepMin,
		light: lightMin,
		rem: remMin,
		awake: awakeMin
	}

	// 简易得分：时长接近 7.5h 且深睡占比合理更高
	const durationScore = Math.max(0, 100 - Math.abs(totalMin - 450) / 4.5)
	const deepRatio = deepMin / structureBase
	const deepScore = Math.max(0, 100 - Math.abs(deepRatio - 0.22) * 250)
	const score = Math.round(durationScore * 0.55 + deepScore * 0.45)

	let qualityText = '睡眠质量一般'
	let recoverStatus = '恢复一般'
	if (score >= 85) {
		qualityText = '睡眠质量很好'
		recoverStatus = '恢复良好'
	} else if (score >= 70) {
		qualityText = '睡眠质量较好'
		recoverStatus = '恢复良好'
	} else if (score >= 55) {
		qualityText = '睡眠质量一般'
		recoverStatus = '恢复一般'
	} else {
		qualityText = '睡眠质量偏差'
		recoverStatus = '恢复不足'
	}

	const allSessions = Array.isArray(data.sessions)
		? data.sessions.map((s) => normalizeSleep({ ...s, typeMap, sessions: [] })).filter(Boolean)
		: []

	return {
		totalMin,
		hours,
		minutes,
		deepMin,
		lightMin,
		remMin,
		awakeMin,
		startTime: startSec,
		endTime: endSec,
		sleepDay,
		sleepStart: fmtClock(startSec),
		wakeTime: fmtClock(endSec),
		segments,
		structure,
		score: Math.max(0, Math.min(100, score)),
		qualityText,
		recoverStatus,
		qualityMarkerPercent: Math.max(4, Math.min(96, score)),
		sessions: allSessions,
		typeMap,
		updatedAt: Date.now(),
		raw: data
	}
}

function formatMeasureTime(value) {
	if (!value) {
		const d = new Date()
		const p = (n) => (n < 10 ? '0' + n : String(n))
		return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
	}
	if (typeof value === 'number') {
		const d = new Date(value < 1e12 ? value * 1000 : value)
		if (!isNaN(d.getTime())) {
			const p = (n) => (n < 10 ? '0' + n : String(n))
			return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
		}
	}
	return String(value)
}

function reportSportToCloud(sport) {
	if (!sport || !hasLogin()) return
	const key = [sport.steps, sport.distanceM, sport.calories].join('|')
	if (key === lastUploadedSportKey) return
	lastUploadedSportKey = key
	const device = state.device || {}
	uploadDeviceData({
		deviceMac: device.mac || device.id || '',
		deviceVersion: getFirmware() || '',
		batteryLevel: getBatteryLevel(),
		dataList: [{
			dataType: 'sport',
			walkSteps: Number(sport.steps) || 0,
			walkDistance: Number(sport.distanceM) || 0,
			walkCalories: Number(sport.calories) || 0,
			measureTime: formatMeasureTime(sport.time || sport.updatedAt),
			source: '1'
		}]
	}).catch((e) => {
		if (lastUploadedSportKey === key) lastUploadedSportKey = ''
		console.log('[ble-service] upload sport failed', e && (e.msg || e.message))
	})
}

function sleepTypeOfStage(stage) {
	if (stage === 'deep') return 2
	if (stage === 'awake') return 4
	return 3
}

function buildSleepUploadList(sleep) {
	const list = []
	const start = formatMeasureTime(sleep.startTime || sleep.updatedAt)
	const end = formatMeasureTime(sleep.endTime || sleep.updatedAt)
	list.push({
		dataType: 'sleep',
		sleepType: 1,
		measureTime: start,
		endTime: end,
		source: '1'
	})
	let cursor = Number(sleep.startTime) || 0
	;(sleep.segments || []).forEach((seg) => {
		const durSec = Math.max(0, Math.round(Number(seg.dur) || 0) * 60)
		if (!durSec) return
		const from = cursor
		const to = cursor + durSec
		list.push({
			dataType: 'sleep',
			sleepType: sleepTypeOfStage(seg.stage),
			measureTime: formatMeasureTime(from),
			endTime: formatMeasureTime(to),
			source: '1'
		})
		cursor = to
	})
	return list
}

function reportSleepToCloud(sleep) {
	if (!sleep || !hasLogin()) return
	if (!(Number(sleep.totalMin) > 0) && !sleep.startTime) return
	const key = [sleep.startTime, sleep.endTime, sleep.totalMin].join('|')
	if (key === lastUploadedSleepKey) return
	lastUploadedSleepKey = key
	const device = state.device || {}
	uploadDeviceData({
		deviceMac: device.mac || device.id || '',
		deviceVersion: getFirmware() || '',
		batteryLevel: getBatteryLevel(),
		dataList: buildSleepUploadList(sleep)
	}).catch((e) => {
		if (lastUploadedSleepKey === key) lastUploadedSleepKey = ''
		console.log('[ble-service] upload sleep failed', e && (e.msg || e.message))
	})
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
	state.sport = null
	state.sleep = null
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

	CoolWearBLE.on('onSport', (data) => {
		const sport = normalizeSport(data)
		console.log('[ble-service] onSport raw=', data, 'normalized=', sport)
		if (sport) {
			state.sport = sport
			emit()
			reportSportToCloud(sport)
		}
	})

	CoolWearBLE.on('onSleep', (data) => {
		const sleep = normalizeSleep(data)
		console.log('[ble-service] onSleep raw=', data, 'normalized=', sleep)
		if (sleep) {
			state.sleep = sleep
			emit()
			reportSleepToCloud(sleep)
		}
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

	/** 同步戒指运动数据（步数/距离/卡路里），结果通过 onSport 回传 */
	async syncSport() {
		if (!state.connected) return state.sport
		await CoolWearBLE.sendSyncInfo().catch(() => {})
		return state.sport
	},

	getSport() {
		return state.sport ? { ...state.sport } : null
	},

	/** 同步戒指睡眠数据，结果通过 onSleep 回传 */
	async syncSleep() {
		if (!state.connected) return state.sleep
		await CoolWearBLE.sendSyncInfo().catch(() => {})
		return state.sleep
	},

	getSleep() {
		return state.sleep ? { ...state.sleep } : null
	},

	updateDeviceName(name) {
		if (!state.device || !name) return
		state.device.name = name
		persistDevice()
		emit()
	}
}

export default BleService
