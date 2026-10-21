/**
 * 全局蓝牙服务：封装 CoolWearBLE，供业务页共用连接态 / 扫描 / 电量
 */
import CoolWearBLE from '@/common/coolwear-ble.js'
import {
	hasLogin
} from '@/common/auth.js'
import {
	uploadDeviceData
} from '@/common/api/deviceData.js'
import {
	bindDevice,
	unbindDevice,
	getDeviceList
} from '@/common/api/device.js'

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
	autoMonitor: null,
	devices: [],
	pluginOk: false
}

const stateListeners = new Set()
let eventsBound = false
let scanTimer = null
let reconnecting = false
let lastUploadedSportKey = ''
let lastUploadedSleepKey = ''
let lastSyncInfoAt = 0

function throttledSendSyncInfo(force = false) {
	const now = Date.now()
	if (!force && now - lastSyncInfoAt < 5000) return
	lastSyncInfoAt = now
	CoolWearBLE.sendSyncInfo().catch(() => {})
}

/**
 * 连接后主动多请求几次设备全量数据（sport/sleep/...）。
 * 睡眠只在戒指「判定入睡→醒来」后才会在同步中回传，且可能滞后，
 * 单次 sendSyncInfo 容易错过，这里多拉几次提高成功率。
 */
function pullAllData(retry = 4, intervalMs = 2500) {
	let n = 0
	const tick = () => {
		if (n >= retry || !state.connected) return
		n++
		console.log('[ble-service] pullAllData #' + n)
		CoolWearBLE.sendSyncInfo().catch(() => {})
		if (n < retry) setTimeout(tick, intervalMs)
	}
	setTimeout(tick, 600)
}

function cloneState() {
	return {
		inited: state.inited,
		initing: state.initing,
		connectStatus: state.connectStatus,
		connected: state.connected,
		scanning: state.scanning,
		pluginOk: state.pluginOk,
		device: state.device ? {
			...state.device
		} : null,
		battery: state.battery ? {
			...state.battery
		} : null,
		deviceInfo: state.deviceInfo ? {
			...state.deviceInfo
		} : null,
		sport: state.sport ? {
			...state.sport
		} : null,
		sleep: state.sleep ? {
			...state.sleep
		} : null,
		autoMonitor: state.autoMonitor ? {
			...state.autoMonitor
		} : null,
		devices: state.devices.map((d) => ({
			...d
		})),
		statusText: STATUS_MAP[state.connectStatus] || '未知',
		batteryLevel: getBatteryLevel(),
		batteryCharging: getBatteryCharging(),
		firmware: getFirmware()
	}
}

/** 解析戒指运动数据：步数、距离(米)、卡路里(原始值，通常需 /1000) */
function normalizeSport(data) {
	let list = Array.isArray(data) ? data : (data != null ? [data] : [])
	// iOS 透传的是 {curItemCount, sportInfos:[{walkSteps,walkDistance,walkCalories...}], remainItemCount} 包裹结构，先拆开
	const flat = []
	list.forEach((it) => {
		if (it && typeof it === 'object' && Array.isArray(it.sportInfos)) {
			it.sportInfos.forEach((s) => flat.push(s))
		} else {
			flat.push(it)
		}
	})
	list = flat
	if (!list.length) return null
	const item = list[list.length - 1]
	if (!item || typeof item !== 'object') return null

	let steps = Number(
		item.walkSteps != null ? item.walkSteps :
		(item.steps != null ? item.steps :
			(item.step != null ? item.step : NaN))
	)
	let distanceM = Number(
		item.distance != null ? item.distance :
		(item.walkDistance != null ? item.walkDistance :
			(item.distanceM != null ? item.distanceM : NaN))
	)
	let caloriesRaw = Number(
		item.calories != null ? item.calories :
		(item.walkCalories != null ? item.walkCalories :
			(item.caloriesRaw != null ? item.caloriesRaw : NaN))
	)
	let time = item.time != null ? item.time :
		(item.startSecs != null ? item.startSecs * 1000 :
			(item.measureTime != null ? item.measureTime : null))

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
	const caloriesKcal = caloriesRaw >= 1000 ?
		Math.round(caloriesRaw / 1000) :
		Math.round(caloriesRaw)

	return {
		steps: Math.round(steps),
		distanceM: Math.round(distanceM),
		distanceKm: Math.round((distanceM / 1000) * 10) / 10,
		caloriesRaw,
		calories: caloriesKcal,
		time: time,
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
			return {
				stage: 'light',
				dur: Math.max(1, item)
			}
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
		else if ((!Number.isNaN(moveCode) && n === moveCode) || (!Number.isNaN(wakeCode) && n === wakeCode))
			stage = 'awake'
		else if (!Number.isNaN(startCode) && n === startCode) stage = 'light'
		else if (code === 'deep' || code === 'light' || code === 'rem' || code === 'awake') stage = code
		else if (n === 3) stage = 'deep'
		else if (n === 2) stage = 'light'
		else if (n === 5 || n === 4) stage = 'awake'
		else if (n === 6) stage = 'rem'
		return {
			stage,
			dur
		}
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
			segments.push(...parseSleepDetail([{
				[String(prevType)]: keep
			}], typeMap))
		}
	}

	if (segments.length) {
		const sum = {
			deep: 0,
			light: 0,
			rem: 0,
			awake: 0
		}
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
		const parts = [{
				stage: 'deep',
				dur: deepMin
			},
			{
				stage: 'light',
				dur: lightMin
			},
			{
				stage: 'rem',
				dur: remMin
			},
			{
				stage: 'awake',
				dur: awakeMin
			}
		].filter((p) => p.dur > 0)
		segments = parts.length ? parts : [{
			stage: 'light',
			dur: Math.max(1, totalMin)
		}]
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

	const allSessions = Array.isArray(data.sessions) ?
		data.sessions.map((s) => normalizeSleep({
			...s,
			typeMap,
			sessions: []
		})).filter(Boolean) :
		[]

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
		return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d
			.getMinutes()) + ':' + p(d.getSeconds())
	}
	if (typeof value === 'number') {
		const d = new Date(value < 1e12 ? value * 1000 : value)
		if (!isNaN(d.getTime())) {
			const p = (n) => (n < 10 ? '0' + n : String(n))
			return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(
				d.getMinutes()) + ':' + p(d.getSeconds())
		}
	}
	return String(value)
}

function reportSportToCloud(sport) {
	if (!sport || !hasLogin()) return
	const key = [sport.steps, sport.distanceM, sport.calories].join('|')
	if (key === lastUploadedSportKey) {
		console.log('[reportSportToCloud] 数据未变化，跳过上报:', key)
		return
	}
	lastUploadedSportKey = key
	const device = state.device || {}
	const payload = {
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
	}
	console.log('[reportSportToCloud] 上报步数请求:', JSON.stringify(payload))
	uploadDeviceData(payload)
		.then((res) => {
			console.log('[reportSportToCloud] 上报步数响应:', JSON.stringify(res))
		})
		.catch((e) => {
			if (lastUploadedSportKey === key) lastUploadedSportKey = ''
			console.log('[reportSportToCloud] 上报步数失败:', JSON.stringify({
				msg: e && (e.msg || e.message),
				err: e && (e.errMsg || String(e))
			}))
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
	let cursor = Number(sleep.startTime) || 0;
	(sleep.segments || []).forEach((seg) => {
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

// ---------- 自动监测数据上报 ----------
// 设备端自动采集的心率历史/血氧/HRV，只有在同步时才会回传（onHeartRateDaily 等），
// 这里统一去重后上报服务端，供心率/血氧/HRV 趋势页展示。
const AUTO_HEALTH_KEYS_MAX = 2000
const uploadedAutoHealthKeys = new Set()
let manualMeasureDepth = 0
let lastAutoMonitorAt = 0

function rememberAutoHealthKey(key) {
	if (uploadedAutoHealthKeys.size >= AUTO_HEALTH_KEYS_MAX) {
		const first = uploadedAutoHealthKeys.values().next().value
		if (first) uploadedAutoHealthKeys.delete(first)
	}
	uploadedAutoHealthKeys.add(key)
}

function pickAutoValue(item) {
	if (item == null) return 0
	if (typeof item === 'number') return Number(item) || 0
	if (typeof item !== 'object') return 0
	const v = item.value != null ? item.value
		: (item.heartNum != null ? item.heartNum
		: (item.heart != null ? item.heart
		: (item.hr != null ? item.hr
		: (item.oxygen != null ? item.oxygen
		: (item.o2 != null ? item.o2
		: (item.hrv != null ? item.hrv : null))))))
	return Number(v) || 0
}

function pickAutoTimeMs(item) {
	if (!item || typeof item !== 'object') return Date.now()
	const raw = item.time != null ? item.time : (item.timestamp != null ? item.timestamp : item.startSecs)
	const n = Number(raw)
	if (n > 0) return n < 1e12 ? n * 1000 : n
	return Date.now()
}

function reportAutoHealthToCloud(dataType, raw) {
	// 手动测量期间由页面自己上报（source=2），这里跳过避免重复
	if (!hasLogin() || manualMeasureDepth > 0) return
	const rows = Array.isArray(raw) ? raw : (raw ? [raw] : [])
	const entries = []
	rows.forEach((row) => {
		const value = pickAutoValue(row)
		if (!(value > 0)) return
		const ms = pickAutoTimeMs(row)
		const key = dataType + '|' + ms + '|' + value
		if (uploadedAutoHealthKeys.has(key)) return
		entries.push({
			key,
			ms,
			value
		})
	})
	if (!entries.length) return
	entries.forEach((it) => rememberAutoHealthKey(it.key))
	const device = state.device || {}
	const chunkSize = 50
	for (let i = 0; i < entries.length; i += chunkSize) {
		const chunk = entries.slice(i, i + chunkSize)
		const dataList = chunk.map((it) => {
			const row = {
				dataType,
				measureTime: formatMeasureTime(it.ms),
				source: '1'
			}
			if (dataType === 'blood_oxygen') row.oxygen = it.value
			else row.heartNum = it.value
			return row
		})
		uploadDeviceData({
				deviceMac: device.mac || device.id || '',
				deviceVersion: getFirmware() || '',
				batteryLevel: getBatteryLevel(),
				dataList
			})
			.then((res) => {
				console.log('[ble-service] 自动监测数据上报成功', dataType, dataList.length, JSON.stringify(res))
			})
			.catch((e) => {
				// 上报失败时移除去重标记，下次同步可重试
				chunk.forEach((it) => uploadedAutoHealthKeys.delete(it.key))
				console.log('[ble-service] 自动监测数据上报失败', dataType, (e && (e.msg || e.message)) || e)
			})
	}
}

/**
 * 重发自动监测相关开关（防抖）。
 * 手动测量结束会把心率/血氧/HRV 检测开关关掉，回前台/重连后必须重新打开，
 * 否则本次连接周期内设备的自动采集一直是关闭的。
 */
function throttledEnableAutoMeasure(minIntervalMs = 5000) {
	const now = Date.now()
	if (now - lastAutoMonitorAt < minIntervalMs) return
	lastAutoMonitorAt = now
	CoolWearBLE.setHeartAutoSwitch({
		heart: true,
		o2: true,
		hr24h: true,
		intervalMin: 30
	}).catch((e) => {
		console.log('[ble-service] setHeartAutoSwitch failed', e && (e.msg || e.message))
	})
	CoolWearBLE.setHeartRateSwitch(true).catch((e) => {
		console.log('[ble-service] setHeartRateSwitch failed', e && (e.msg || e.message))
	})
	CoolWearBLE.setHRVSwitch(true).catch((e) => {
		console.log('[ble-service] setHRVSwitch failed', e && (e.msg || e.message))
	})
	CoolWearBLE.setBloodOxygenSwitch(true).catch((e) => {
		console.log('[ble-service] setBloodOxygenSwitch failed', e && (e.msg || e.message))
	})
}

// 连接状态下定时同步：自动监测数据只存在戒指里，不主动同步的话白天在线期间也不会进 App
let autoSyncTimer = null

function startAutoSyncTimer() {
	if (autoSyncTimer) return
	autoSyncTimer = setInterval(() => {
		if (!state.connected || manualMeasureDepth > 0) return
		CoolWearBLE.sendSyncInfo().catch(() => {})
	}, 15 * 60 * 1000)
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
		return {
			level: data,
			charging: false,
			raw: data
		}
	}
	if (typeof data === 'string') {
		try {
			return normalizeBattery(JSON.parse(data))
		} catch (e) {
			return null
		}
	}
	const src = (data && typeof data === 'object' && data.raw && typeof data.raw === 'object') ? Object.assign({}, data.raw, data) : data
	const levelRaw = pickBatteryField(src, ['level', 'battery', 'Battery', 'battery_capacity', 'batteryCapacity', 'electricity', 'capacity', 'value', 'percent', 'percentage'])
	const chargingRaw = pickBatteryField(src, ['charging', 'chargerStatus', 'isChargerStatus', 'isCharging', 'deviceChargerStatus', 'chargeStatus', 'isCharge'])
	return {
		level: levelRaw !== null && levelRaw !== '' ? Number(levelRaw) : null,
		charging: toBool(chargingRaw),
		raw: data
	}
}

function pickBatteryField(o, keys) {
	if (!o) return null
	for (let i = 0; i < keys.length; i++) {
		const v = o[keys[i]]
		if (v !== undefined && v !== null) return v
	}
	return null
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
				return {
					raw: data
				}
			}
		}
		const firmware = data.firmware != null ?
			String(data.firmware) :
			(data.softwareVer != null ?
				String(data.softwareVer) :
				(data.softwareVersion != null ?
					String(data.softwareVersion) :
					(data.SoftwareVer != null ? String(data.SoftwareVer) : (data.version != null ? String(data
						.version) : null))))
		return {
			...data,
			firmware,
			softwareVer: data.softwareVer != null ? String(data.softwareVer) : firmware,
			hardwareVer: data.hardwareVer != null ? String(data.hardwareVer) : (data.hardwareVersion != null ? String(
				data.hardwareVersion) : null),
			customerId: data.customerId != null ? data.customerId : data.customer_id,
			name: data.name || data.devName || null,
			deviceId: data.deviceId != null ? String(data.deviceId) : null,
			mac: data.mac || null
		}
	}

	function normMac(v) {
		return String(v || '').trim().toUpperCase()
	}

	function deviceKey(item) {
		return normMac(item.mac) || String(item.identifier || item.deviceId || item.id || '')
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
		const mac = normMac(raw.mac)
		const identifier = String(raw.identifier || '')
		const deviceId = raw.deviceId != null ? String(raw.deviceId) : ''
		const id = mac || identifier || deviceId || String(raw.id || '')
		const name = raw.name || '未知设备'
		const rssi = raw.rssi
		return {
			id,
			name,
			mac,
			identifier,
			deviceId,
			rssi,
			meta: `${rssiToSignal(rssi)}${rssi != null ? ` · RSSI ${rssi}` : ''}`,
			raw
		}
	}

	function isSameDevice(a, b) {
		if (!a || !b) return false
		if (a.id && b.id && a.id === b.id) return true
		if (a.mac && b.mac && a.mac === b.mac) return true
		if (a.identifier && b.identifier && a.identifier === b.identifier) return true
		if (a.deviceId && b.deviceId && a.deviceId === b.deviceId) return true
		return false
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
		try {
			uni.removeStorageSync('deviceBindId')
		} catch (e) {}
		state.device = null
		state.battery = null
		state.deviceInfo = null
		state.sport = null
		state.sleep = null
	}

	function pickBindId(res) {
		if (res == null) return ''
		const d = res.data != null ? res.data : res
		const v = d.bindId || d.id || d.deviceId || (d.record && (d.record.bindId || d.record.id))
		return v != null ? String(v) : ''
	}

	async function notifyBindToCloud() {
		if (!hasLogin() || !state.device) return ''
		const mac = state.device.mac || state.device.id || ''
		if (!mac) return ''
		try {
			const res = await bindDevice({
				deviceMac: mac,
				deviceName: state.device.name || '智能戒指',
				deviceModel: state.device.name || '',
				deviceVersion: getFirmware() || state.device.firmware || '',
				batteryLevel: getBatteryLevel()
			})
			const bindId = pickBindId(res)
			if (bindId) uni.setStorageSync('deviceBindId', bindId)
			console.log('[ble-service] bindDevice 上报成功', mac, bindId)
			return bindId
		} catch (e) {
			console.log('[ble-service] bindDevice 上报失败', (e && (e.msg || e.message)) || e)
			return ''
		}
	}

	async function resolveBindId() {
		let bindId = ''
		try {
			bindId = uni.getStorageSync('deviceBindId') || ''
		} catch (e) {}
		if (bindId) return String(bindId)
		const mac = normMac(state.device && (state.device.mac || state.device.id))
		if (!hasLogin()) return ''
		try {
			const res = await getDeviceList()
			const list = Array.isArray(res) ? res : (res && (res.data || res.list || res.records)) || []
			if (Array.isArray(list)) {
				const hit = mac ? list.find((it) => normMac(it.deviceMac || it.mac) === mac) : list[0]
				const v = hit && (hit.bindId || hit.id)
				if (v != null) return String(v)
			}
		} catch (e) {}
		return ''
	}

	function bindEvents() {
		if (eventsBound) return
		eventsBound = true

		CoolWearBLE.on('onDeviceFound', (data) => {
			if (!data) return
			const item = toListItem(data)
			if (!item.id) return
			const idx = state.devices.findIndex((d) => isSameDevice(d, item))
			if (idx >= 0) {
				const prev = state.devices[idx]
				const merged = { ...item }
				if (!merged.mac) merged.mac = prev.mac || ''
				if (!merged.identifier) merged.identifier = prev.identifier || ''
				if (!merged.deviceId) merged.deviceId = prev.deviceId || ''
				if (merged.name === '未知设备' && prev.name && prev.name !== '未知设备') merged.name = prev.name
				if (prev.rssi != null && (merged.rssi == null || Number(prev.rssi) > Number(merged.rssi))) {
					merged.rssi = prev.rssi
					merged.meta = `${rssiToSignal(prev.rssi)} · RSSI ${prev.rssi}`
				}
				merged.id = merged.mac || merged.identifier || merged.deviceId || prev.id
				state.devices.splice(idx, 1, merged)
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
				throttledSendSyncInfo(true)
				setTimeout(() => {
					if (state.connected) throttledSendSyncInfo(true)
				}, 8000)
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

		// 设备端自动采集的历史数据（同步时回传，手动测量页不消费，必须由这里上报）
		CoolWearBLE.on('onHeartRateDaily', (data) => {
			console.log('[ble-service] onHeartRateDaily raw=', data)
			reportAutoHealthToCloud('heart_rate', data)
		})

		CoolWearBLE.on('onBloodOxygen', (data) => {
			console.log('[ble-service] onBloodOxygen raw=', data)
			reportAutoHealthToCloud('blood_oxygen', data)
		})

		CoolWearBLE.on('onHRV', (data) => {
			console.log('[ble-service] onHRV raw=', data)
			reportAutoHealthToCloud('hrv', data)
		})

		// 设备回传的自动监测开关状态，用于确认 setHeartAutoSwitch 是否真正生效
		CoolWearBLE.on('onHeartAutoSwitch', (data) => {
			state.autoMonitor = data && typeof data === 'object' ? {
				...data
			} : null
			console.log('[ble-service] 设备自动监测状态回传', JSON.stringify(data))
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

	function sleep(ms) {
		return new Promise((resolve) => setTimeout(resolve, ms))
	}

	function isIos() {
		// #ifdef APP-PLUS
		try {
			const sys = (uni.getSystemInfoSync && uni.getSystemInfoSync().platform) || ''
			return sys === 'ios' || sys === 'iOS'
		} catch (e) {
			return false
		}
		// #endif
		// #ifndef APP-PLUS
		return false
		// #endif
	}

	async function waitForIosBluetoothReady(timeoutMs = 4000) {
		if (!isIos()) return true
		const start = Date.now()
		while (Date.now() - start < timeoutMs) {
			try {
				const res = await CoolWearBLE.checkBluetoothPermission()
				const s = res && (res.state ?? res.status)
				if (s === 5 || s === '5' || s === 'poweredOn' || s === 'on' || (res && res.poweredOn === true)) {
					return true
				}
				if (res && res.fallback) {
					await sleep(1200)
					return true
				}
			} catch (e) {}
			await sleep(400)
		}
		return false
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
					// 先校准设备时间，再拉取历史数据
					await CoolWearBLE.syncTime().catch(() => {})
					throttledSendSyncInfo(true)
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
			startAutoSyncTimer()
			emit()
		},

		/**
		 * 有本地绑定设备且未解绑时自动重连；解绑会清 STORAGE，不会触发
		 * @param {Object} [options]
		 * @param {boolean} [options.force] 冷启动强制断开重连。进程被杀后系统可能残留“已连接”状态，
		 *        但 GATT 通知并未重新订阅，会出现“显示已连接却收不到心率/步数”的问题，必须彻底重连一次。
		 */
		async tryAutoReconnect(options = {}) {
			// #ifndef APP-PLUS
			return false
			// #endif
			// #ifdef APP-PLUS
			const force = options.force === true
			loadStoredDevice()
			const device = state.device
			if (!device || !(device.mac || device.identifier || device.id)) {
				return false
			}
			if (state.connected && !force) {
				// 已连接（例如从后台回前台）：手动测量结束后检测开关可能仍是关闭的，
				// 这里补发自动监测开关，保证全天自动采集不中断。
				throttledEnableAutoMeasure(30000)
				return true
			}
			if (reconnecting || (state.connectStatus === 2 && !force)) return false

			reconnecting = true
			state.connectStatus = 2
			state.connected = false
			emit()

			try {
				if (!CoolWearBLE.isAvailable()) {
					throw new Error('原生插件未加载')
				}
				await BleService.ensureInit()

				if (force) {
					// 冷启动：先彻底断开残留连接，避免“假连接”（连上了但没订阅通知）
					try {
						await CoolWearBLE.disconnect()
					} catch (e) {}
					await sleep(300)
				} else {
					try {
						const res = await CoolWearBLE.isConnected()
						if (res && res.connected) {
							state.connected = true
							state.connectStatus = 1
							emit()
							await BleService.restoreAfterReconnect()
							persistDevice()
							return true
						}
					} catch (e) {
						// ignore, continue reconnect
					}
				}

				// 非冷启动时优先走 SDK 自动重连（依赖上次连接记录）
				if (!force) {
					try {
						await CoolWearBLE.reconnect()
						const ok = await pollConnected(20000)
						if (ok) {
							await BleService.restoreAfterReconnect()
							persistDevice()
							return true
						}
					} catch (e) {
						console.log('[ble-service] reconnect() failed, fallback connect', e)
					}
				}

				// 回退 / 冷启动：用本地缓存的 mac / identifier 主动连接
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
				await BleService.restoreAfterReconnect()
				persistDevice()
				// 冷启动重连说明设备此前已绑定成功，不再重复上报；非强制重连时保留兜底重试
				if (!force) notifyBindToCloud()
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

		/**
		 * 连接/重连成功后恢复测量开关与数据同步。
		 * 重连路径不会重新下发这些开关，若不恢复，戒指不会主动回传心率/血氧等数据。
		 */
		async restoreAfterReconnect() {
			// 设备时间是睡眠/运动数据的时间基准，必须先同步
			await CoolWearBLE.syncTime().catch((e) => {
				console.log('[ble-service] syncTime failed', e)
			})
			await BleService.enableAutoMeasure().catch(() => {})
			throttledSendSyncInfo(true)
			CoolWearBLE.requestBattery().catch(() => {})
			CoolWearBLE.requestDeviceInfo().catch(() => {})
			pullAllData()
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
			const sys = (uni.getSystemInfoSync && uni.getSystemInfoSync().platform) || ''
			if (sys === 'ios' || sys === 'iOS') {
				try {
					await CoolWearBLE.requestBluetoothPermission()
				} catch (e) {
					const msg = (e && e.message) || '蓝牙权限被拒绝'
					uni.showModal({
						title: '需要蓝牙权限',
						content: msg + '，请前往“设置”开启后重试。',
						confirmText: '去设置',
						success: (r) => {
							if (r.confirm) CoolWearBLE.openAppSettings().catch(() => {})
						}
					})
					throw e
				}
				uni.setStorageSync('blePermissionGranted', true)
				return
			}
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
			if (isIos()) {
				await waitForIosBluetoothReady(4000)
			}
			if (scanTimer) {
				clearTimeout(scanTimer)
				scanTimer = null
			}
			state.devices = []
			state.scanning = true
			emit()
			await CoolWearBLE.startScan()
			if (isIos()) {
				setTimeout(async () => {
					if (!state.scanning) return
					if (state.devices.length > 0) return
					try {
						await CoolWearBLE.stopScan().catch(() => {})
						await CoolWearBLE.startScan()
					} catch (e) {}
				}, 1500)
			}
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
			// 设备时间决定睡眠/运动数据的日期归属，连接后必须先同步
			await CoolWearBLE.syncTime().catch((e) => {
				console.log('[ble-service] syncTime failed', e)
			})
			await BleService.enableAutoMeasure().catch(() => {})
			persistDevice()
			uni.setStorageSync('onboardingDone', true)
			uni.setStorageSync('isFirstLogin', false)
			emit()
			notifyBindToCloud()
			pullAllData()
			return cloneState()
		},

		async enableAutoMeasure() {
			if (!state.connected) return false
			try {
				await CoolWearBLE.setHeartAutoSwitch({
					heart: true,
					o2: true,
					hr24h: true,
					intervalMin: 30
				})
			} catch (e) {
				console.log('[ble-service] setHeartAutoSwitch failed', e)
			}
			try {
				await CoolWearBLE.setHeartRateSwitch(true)
			} catch (e) {
				console.log('[ble-service] setHeartRateSwitch failed', e)
			}
			try {
				await CoolWearBLE.setHRVSwitch(true)
			} catch (e) {}
			try {
				await CoolWearBLE.setBloodOxygenSwitch(true)
			} catch (e) {}
			return true
		},

		/** 手动测量开始/结束标记：测量期间由页面负责上报（source=2），全局自动上报跳过，避免重复 */
		beginManualMeasure() {
			manualMeasureDepth++
		},

		endManualMeasure() {
			manualMeasureDepth = Math.max(0, manualMeasureDepth - 1)
		},

		getAutoMonitor() {
			return state.autoMonitor ? {
				...state.autoMonitor
			} : null
		},

		/**
		 * 手动测量页退出/结束时调用：关闭实时推流后把后台自动监测重新下发，
		 * 包括自动监测配置和心率/血氧/HRV 检测开关——只发 setHeartAutoSwitch 是不够的，
		 * 因为测量结束已经把检测开关关掉了，本次连接内自动采集会一直停止。
		 */
		async restoreAutoMonitor() {
			if (!state.connected) return false
			throttledEnableAutoMeasure(3000)
			return true
		},

		async disconnect(unbind = false) {
			let bindId = ''
			if (unbind) bindId = await resolveBindId()
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
			if (unbind && bindId && hasLogin()) {
				try {
					await unbindDevice(bindId)
					console.log('[ble-service] unbindDevice 上报成功', bindId)
				} catch (e) {
					console.log('[ble-service] unbindDevice 上报失败', (e && (e.msg || e.message)) || e)
				}
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
			throttledSendSyncInfo(true)
			return state.sport
		},

		getSport() {
			return state.sport ? {
				...state.sport
			} : null
		},

		/** 同步戒指睡眠数据，结果通过 onSleep 回传 */
		async syncSleep() {
			if (!state.connected) return state.sleep
			await CoolWearBLE.sendSyncInfo().catch(() => {})
			return state.sleep
		},

		getSleep() {
			return state.sleep ? {
				...state.sleep
			} : null
		},

		/**
		 * 修改设备名称：写入戒指硬件（蓝牙名称）+ 更新本地缓存
		 * 注意：戒指改名后需断开重新配对/重连才会在广播名称中生效
		 */
		async updateDeviceName(name) {
			const trimmed = String(name == null ? '' : name).trim()
			if (!trimmed) return false
			if (state.connected) {
				try {
					await CoolWearBLE.setDeviceName(trimmed)
				} catch (e) {
					// iOS 当前 SDK 未提供改名命令时静默降级为仅本地修改
					console.log('[ble-service] setDeviceName 写入硬件失败', e && (e.message || e))
				}
			}
			if (state.device) {
				state.device.name = trimmed
			}
			persistDevice()
			emit()
			if (state.connected) notifyBindToCloud()
			return true
		}
	}

	export default BleService