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
	manualMeasure: false,
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
let lastSyncInfoAt = 0

function throttledSendSyncInfo(force = false) {
	const now = Date.now()
	if (!force && now - lastSyncInfoAt < 5000) return
	lastSyncInfoAt = now
	CoolWearBLE.sendSyncInfo().catch(() => {})
}

// ---------------------------------------------------------------------------
// 自动监测数据上报 / 失败补报 / 睡眠事件聚合
// ---------------------------------------------------------------------------

const PENDING_UPLOAD_KEY = 'blePendingUploads'
const SLEEP_STATE_KEY = 'bleSleepAccumulator'
const SLEEP_DONE_KEY = 'bleSleepUploadedKeys'
const MAX_PENDING_UPLOADS = 30
const SLEEP_MAX_EVENTS = 600
const AUTO_KEY_CACHE = new Set()

// 睡眠状态映射：以厂商文档为准（1开始/2深睡/3浅睡/4苏醒），5 兼容部分固件的体动
const SLEEP_STAGE_MAP = {
	1: 'light',
	2: 'deep',
	3: 'light',
	4: 'awake',
	5: 'awake'
}

function pad2(n) {
	return n < 10 ? '0' + n : String(n)
}

function formatMs(ms) {
	const d = new Date(ms)
	if (isNaN(d.getTime())) return ''
	return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' +
		pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds())
}

/** 秒/毫秒统一成毫秒；过小的数值（分钟偏移等）返回 0 表示无法当作绝对时间 */
function toTimeMs(value) {
	const n = Number(value)
	if (!n || Number.isNaN(n)) return 0
	if (n > 1e12) return n
	if (n > 1e9) return n * 1000
	return 0
}

function loadPendingUploads() {
	try {
		const arr = uni.getStorageSync(PENDING_UPLOAD_KEY)
		return Array.isArray(arr) ? arr : []
	} catch (e) {
		return []
	}
}

let pendingUploads = loadPendingUploads()

function savePendingUploads() {
	try {
		uni.setStorageSync(PENDING_UPLOAD_KEY, pendingUploads.slice(-MAX_PENDING_UPLOADS))
	} catch (e) {
		// ignore
	}
}

function enqueueUpload(payload) {
	if (!payload || !Array.isArray(payload.dataList) || !payload.dataList.length) return
	pendingUploads.push(payload)
	savePendingUploads()
}

/** 统一上报入口：未登录或失败时进补报队列，连接/回前台/登录后重放 */
function submitDeviceData(dataList, label) {
	const items = (dataList || []).filter(Boolean)
	if (!items.length) return
	const device = state.device || {}
	const payload = {
		deviceMac: device.mac || device.id || '',
		deviceVersion: getFirmware() || '',
		batteryLevel: getBatteryLevel(),
		dataList: items
	}
	if (!hasLogin()) {
		console.log(`[ble-service] 未登录，${label}数据先缓存，登录后补报`, items.length)
		enqueueUpload(payload)
		return
	}
	console.log(`[ble-service] 上报${label}:`, JSON.stringify(payload))
	uploadDeviceData(payload)
		.then((res) => {
			console.log(`[ble-service] 上报${label}成功:`, JSON.stringify(res))
		})
		.catch((e) => {
			console.log(`[ble-service] 上报${label}失败，进入补报队列:`, JSON.stringify({
				msg: e && (e.msg || e.message),
				err: e && (e.errMsg || String(e))
			}))
			enqueueUpload(payload)
		})
}

async function flushPendingUploads() {
	if (!pendingUploads.length || !hasLogin()) return
	const batch = pendingUploads.slice(0, 10)
	pendingUploads = pendingUploads.slice(10)
	savePendingUploads()
	for (const payload of batch) {
		try {
			await uploadDeviceData(payload)
			console.log('[ble-service] 补报成功:', payload.dataList.length)
		} catch (e) {
			enqueueUpload(payload)
			console.log('[ble-service] 补报失败，留待下次:', (e && (e.msg || e.message)) || e)
			break
		}
	}
}

function markAutoKey(key) {
	if (AUTO_KEY_CACHE.size > 4000) AUTO_KEY_CACHE.clear()
	if (AUTO_KEY_CACHE.has(key)) return false
	AUTO_KEY_CACHE.add(key)
	return true
}

/** 把 iOS 包装结构 / 单条对象 / 数组统一拍平成数据条目列表 */
function extractSeries(data) {
	const out = []
	const pushItem = (it) => {
		if (!it || typeof it !== 'object' || Array.isArray(it)) return
		out.push(it)
	}
	const walk = (it) => {
		if (!it) return
		if (Array.isArray(it)) {
			it.forEach(walk)
			return
		}
		if (typeof it !== 'object') return
		const inner = it.heartInfos || it.sportInfos || it.data || it.list || it.items
		if (Array.isArray(inner)) {
			inner.forEach(pushItem)
			return
		}
		pushItem(it)
	}
	walk(data)
	return out
}

function pickNumber(item, keys) {
	for (let i = 0; i < keys.length; i++) {
		const k = keys[i]
		if (item[k] != null) {
			const n = Number(item[k])
			if (!Number.isNaN(n)) return n
		}
	}
	return null
}

function buildSeriesItems(series, dataType, valueKeys, range) {
	const items = []
	;(series || []).forEach((it) => {
		if (!it || typeof it !== 'object') return
		const v = pickNumber(it, valueKeys)
		if (v == null || v <= 0) return
		if (range && (v < range[0] || v > range[1])) return
		const ms = toTimeMs(it.time != null ? it.time : it.measureTime)
		const measureTime = formatMs(ms || Date.now())
		const key = `${dataType}|${measureTime}|${v}`
		if (!markAutoKey(key)) return
		if (dataType === 'blood_oxygen') {
			items.push({
				dataType: 'blood_oxygen',
				oxygen: v,
				measureTime,
				source: '1'
			})
		} else {
			items.push({
				dataType,
				heartNum: v,
				measureTime,
				source: '1'
			})
		}
	})
	return items
}

/** 自动心率（戒指按间隔自动记录的历史心率） */
function reportDailyHeart(data) {
	console.log('[ble-service] onHeartRateDaily raw=', data)
	const items = buildSeriesItems(extractSeries(data), 'heart_rate', ['heartNums', 'heartNum', 'heart', 'hr', 'bpm', 'value'], [25, 220])
	if (items.length) submitDeviceData(items, '自动心率')
}

let lastRealtimeHeartAt = 0
/** 实时心率流（onHeartRate，心率检测开关打开后设备持续推送）；手动测量期间由测量页处理，这里节流上报 */
function reportRealtimeHeart(data) {
	if (state.manualMeasure) return
	const now = Date.now()
	if (now - lastRealtimeHeartAt < 60000) return
	lastRealtimeHeartAt = now
	const items = buildSeriesItems(extractSeries(data), 'heart_rate', ['heartNums', 'heartNum', 'heart', 'hr', 'bpm', 'value'], [25, 220])
	if (items.length) submitDeviceData(items, '实时心率')
}

/** 自动血氧（历史血氧；手动测量期间由页面自行上报，避免重复） */
function reportAutoOxygen(data) {
	if (state.manualMeasure) return
	const items = buildSeriesItems(extractSeries(data), 'blood_oxygen', ['o2', 'oxygen', 'spo2', 'bloodOxygen', 'oxygenValue', 'o2Value', 'value'], [70, 100])
	if (items.length) submitDeviceData(items, '自动血氧')
}

/** 自动 HRV / 压力 */
function reportAutoHrv(data) {
	if (state.manualMeasure) return
	const rawSeries = extractSeries(data)
	// iOS 的 HRV 数据有时没有独立 hrv 字段，原生会把 time 误当成 value（变成 10 位时间戳），
	// 这里对明显是时间戳的 value 用 hr/heart/heartNum 兜底，避免被 [1,300] 区间过滤掉。
	const series = rawSeries.map((it) => {
		if (!it || typeof it !== 'object') return it
		const v = pickNumber(it, ['hrvNums', 'hrv', 'hrvValue', 'rmssd', 'sdnn', 'value'])
		if (v == null || v > 1000) {
			const alt = pickNumber(it, ['hr', 'heart', 'heartNum'])
			if (alt != null && alt > 0 && alt <= 300) return { ...it, value: alt }
		}
		return it
	})
	const hrvItems = buildSeriesItems(series, 'hrv', ['hrvNums', 'hrv', 'hrvValue', 'rmssd', 'sdnn', 'value'], [1, 300])
	if (hrvItems.length) submitDeviceData(hrvItems, '自动HRV')
	const stressItems = []
	series.forEach((it) => {
		const sv = pickNumber(it, ['stress', 'pressure', 'stressValue', 'press'])
		if (sv == null || sv < 0 || sv > 100) return
		const ms = toTimeMs(it.time)
		const measureTime = formatMs(ms || Date.now())
		const key = `stress|${measureTime}|${sv}`
		if (!markAutoKey(key)) return
		stressItems.push({
			dataType: 'stress',
			heartNum: sv,
			measureTime,
			source: '1'
		})
	})
	if (stressItems.length) submitDeviceData(stressItems, '自动压力')
}

/** 从 iOS 原始睡眠事件流 / Android 聚合结构里提取睡眠事件 {type,time} */
function extractSleepEvents(data) {
	const events = []
	const addEvent = (ev) => {
		if (!ev || typeof ev !== 'object') return
		const type = pickNumber(ev, ['SleepType', 'sleepType', 'type'])
		const time = pickNumber(ev, ['SleepStartTime', 'sleepStartTime', 'sleepTime', 'time'])
		if (type == null || time == null) return
		events.push({
			type,
			time
		})
	}
	const handle = (it) => {
		if (!it) return
		if (Array.isArray(it)) {
			it.forEach(handle)
			return
		}
		if (typeof it !== 'object') return
		if (Array.isArray(it.sleepInfos)) it.sleepInfos.forEach(addEvent)
		else if (it.sleepInfos && typeof it.sleepInfos === 'object') addEvent(it.sleepInfos)
		if (Array.isArray(it.sleeps)) it.sleeps.forEach(addEvent)
		if (it.SleepType != null || it.SleepStartTime != null) addEvent(it)
	}
	handle(data)
	if (data && Array.isArray(data.sessions)) data.sessions.forEach(handle)
	if (data && data.raw && typeof data.raw === 'object') handle(data.raw)
	return events
}

function loadSleepAccumulator() {
	try {
		const v = uni.getStorageSync(SLEEP_STATE_KEY)
		return v && typeof v === 'object' && Array.isArray(v.events) ? v : null
	} catch (e) {
		return null
	}
}

function saveSleepAccumulator(acc) {
	try {
		uni.setStorageSync(SLEEP_STATE_KEY, acc)
	} catch (e) {
		// ignore
	}
}

function loadUploadedSleepKeys() {
	try {
		const arr = uni.getStorageSync(SLEEP_DONE_KEY)
		return Array.isArray(arr) ? arr : []
	} catch (e) {
		return []
	}
}

function markSleepKey(key) {
	const keys = loadUploadedSleepKeys()
	if (keys.indexOf(key) !== -1) return false
	keys.push(key)
	try {
		uni.setStorageSync(SLEEP_DONE_KEY, keys.slice(-60))
	} catch (e) {
		// ignore
	}
	return true
}

/** 把累计的睡眠事件收尾成一个完整会话（分钟数由相邻事件时间差计算） */
function closeSleepAccumulator(acc) {
	const events = (acc && acc.events ? acc.events : []).slice()
	if (events.length < 2) return null
	events.sort((a, b) => a.t - b.t)
	const startMs = events[0].t
	const endMs = events[events.length - 1].t
	const totalMin = Math.round((endMs - startMs) / 60000)
	if (totalMin <= 0) return null
	const sums = {
		deep: 0,
		light: 0,
		rem: 0,
		awake: 0
	}
	const segments = []
	for (let i = 0; i < events.length - 1; i++) {
		const cur = events[i]
		const next = events[i + 1]
		const dur = Math.max(1, Math.round((next.t - cur.t) / 60000))
		if (sums[cur.stage] == null) continue
		sums[cur.stage] += dur
		segments.push({
			stage: cur.stage,
			dur
		})
	}
	const segTotal = segments.reduce((s, x) => s + x.dur, 0)
	if (totalMin > segTotal) {
		const lastStage = events[events.length - 1].stage
		if (sums[lastStage] != null) sums[lastStage] += totalMin - segTotal
		segments.push({
			stage: lastStage,
			dur: totalMin - segTotal
		})
	}
	return {
		startMs,
		endMs,
		totalMin,
		deepMin: sums.deep,
		lightMin: sums.light,
		remMin: sums.rem,
		awakeMin: sums.awake,
		segments
	}
}

/**
 * iOS 原始睡眠事件流聚合：
 * 固件发的是 { SleepStartTime, SleepType } 事件（同一段只发一次），
 * 这里跨推送累计事件、按相邻事件时间差算分段时长，收到苏醒事件后结算一晚睡眠。
 */
function ingestSleepEvents(events) {
	const GAP_MS = 3 * 3600 * 1000
	const completed = []
	let acc = loadSleepAccumulator()
	if (!acc) acc = { events: [], startMs: 0, lastMs: 0, lastAt: 0 }
	events.forEach((ev) => {
		const nowAt = Date.now()
		// 距上一条事件超过 3 小时（跨天/长时间未连上）先结算上一段，避免跨夜时长被合并
		if (acc.events.length > 1 && acc.lastAt && nowAt - acc.lastAt > GAP_MS) {
			const session = closeSleepAccumulator(acc)
			if (session) completed.push(session)
			acc = { events: [], startMs: 0, lastMs: 0, lastAt: 0 }
		}
		const stage = SLEEP_STAGE_MAP[ev.type] || 'light'
		let ms = toTimeMs(ev.time)
		if (!ms) {
			// 非绝对时间时用递推兜底，保证事件可累计
			ms = (acc.lastMs || nowAt - 60000) + 60000
		}
		if (acc.lastMs && ms <= acc.lastMs) ms = acc.lastMs + 60000
		if (!acc.startMs || ev.type === 1) {
			if (acc.events.length > 1 && acc.startMs) {
				const session = closeSleepAccumulator(acc)
				if (session) completed.push(session)
				acc = { events: [], startMs: 0, lastMs: 0, lastAt: 0 }
			}
			acc.startMs = ms
		}
		acc.lastMs = ms
		acc.lastAt = nowAt
		const key = ms + '|' + stage
		if (!acc.events.some((e) => e.key === key)) {
			acc.events.push({
				key,
				t: ms,
				stage,
				type: ev.type
			})
		}
		if (acc.events.length > SLEEP_MAX_EVENTS) acc.events = acc.events.slice(-SLEEP_MAX_EVENTS)
		if (ev.type === 4 || ev.type === 5) {
			const session = closeSleepAccumulator(acc)
			if (session) completed.push(session)
			acc = { events: [], startMs: 0, lastMs: 0, lastAt: 0 }
		}
	})
	// 历史数据补报时，最后一条事件可能不是 WAKEUP（固件只发到 LIGHT/DEEP）。
	// 事件时间已经过去 4 小时以上，说明这一晚已经结束，直接结算，避免永远等不到结束事件。
	if (acc.events.length > 1 && acc.lastMs && Date.now() - acc.lastMs > 4 * 3600 * 1000) {
		const session = closeSleepAccumulator(acc)
		if (session) completed.push(session)
		acc = { events: [], startMs: 0, lastMs: 0, lastAt: 0 }
	}
	saveSleepAccumulator(acc)
	return completed
}

function sleepTypeOfStage(stage) {
	if (stage === 'deep') return 2
	if (stage === 'awake') return 4
	return 3
}

function buildSleepDataList(session) {
	const startMs = Number(session.startMs) || 0
	const totalMin = Number(session.totalMin) || 0
	if (!startMs || totalMin <= 0) return []
	const endMs = Number(session.endMs) || startMs + totalMin * 60000
	if (endMs <= startMs) return []
	const list = [{
		dataType: 'sleep',
		sleepType: 1,
		measureTime: formatMs(startMs),
		endTime: formatMs(endMs),
		source: '1'
	}]
	let cursor = startMs
	;(session.segments || []).forEach((seg) => {
		const durMs = Math.max(0, Math.round(Number(seg.dur) || 0) * 60000)
		if (!durMs) return
		const from = cursor
		const to = Math.min(endMs, cursor + durMs)
		list.push({
			dataType: 'sleep',
			sleepType: sleepTypeOfStage(seg.stage),
			measureTime: formatMs(from),
			endTime: formatMs(to),
			source: '1'
		})
		cursor = to
	})
	return list
}

/** 上报一晚完整的睡眠会话（按开始时间+总时长去重，持久化到本地避免重复上报） */
function uploadSleepSession(session) {
	if (!session) return
	const startMs = Number(session.startMs) || 0
	const totalMin = Number(session.totalMin) || 0
	if (!startMs || totalMin <= 0) return
	const key = `${formatMs(startMs)}|${totalMin}`
	if (!markSleepKey(key)) {
		console.log('[ble-service] 睡眠会话已上报过，跳过:', key)
		return
	}
	const dataList = buildSleepDataList(session)
	if (!dataList.length) return
	submitDeviceData(dataList, '睡眠')
}

/** 判断某条睡眠 payload 是否是聚合结构（Android 正常路径），否则按 iOS 原始事件流处理 */
function sleepPayloadHasAggregate(data) {
	const sessions = Array.isArray(data && data.sessions) ? data.sessions : (data ? [data] : [])
	return sessions.some((s) => s && typeof s === 'object' && (
		s.deepTime != null || s.lightTime != null || s.sleepTime != null ||
		s.sleepDetail != null || s.startTime != null || s.endTime != null
	))
}

function handleSleepPayload(data) {
	if (!data) return
	if (!sleepPayloadHasAggregate(data)) {
		const events = extractSleepEvents(data)
		if (events.length) {
			const completed = ingestSleepEvents(events)
			console.log('[ble-service] 睡眠事件聚合：收到事件', events.length, '条，结算会话', completed.length, '个')
			completed.forEach(uploadSleepSession)
			return
		}
	}
	const sessions = Array.isArray(data.sessions) && data.sessions.length ? data.sessions : [data]
	let latest = null
	sessions.forEach((s) => {
		const sleep = normalizeSleep({
			...s,
			typeMap: data.typeMap,
			sessions: []
		})
		if (!sleep) return
		latest = sleep
		reportSleepToCloud(sleep)
	})
	if (latest) {
		state.sleep = latest
		emit()
	}
}

async function pullCachedHealthEvents() {
	try {
		const res = await CoolWearBLE.pullCachedHealthEvents()
		const events = res && Array.isArray(res.events) ? res.events : []
		if (!events.length) return
		console.log('[ble-service] 回放原生缓存健康事件:', events.length)
		events.forEach((ev) => {
			console.log(121131312,ev)
			const type = ev && ev.type
			const data = ev && ev.data
			if (type === 'sleep') handleSleepPayload(data)
			else if (type === 'dailyHeart') reportDailyHeart(data)
			else if (type === 'hrv') reportAutoHrv(data)
			else if (type === 'spo2') reportAutoOxygen(data)
		})
	} catch (e) {
		console.log('[ble-service] 拉取原生缓存事件失败', e && (e.message || e))
	}
}

let postConnectTimer = null

function cancelPostConnectInit() {
	if (postConnectTimer) {
		clearTimeout(postConnectTimer)
		postConnectTimer = null
	}
}

/**
 * 连接就绪后统一下发初始化（延迟 1.5s 等设备配对完成）：
 * 同步时间 -> 打开 sensor 上传开关 -> 同步用户信息 -> 开启自动监测 -> 拉基础信息
 * iOS connected/completed 双回调、重连重复回调都通过这里去重，只执行一次。
 */
function schedulePostConnectInit() {
	if (postConnectTimer) return
	postConnectTimer = setTimeout(async () => {
		postConnectTimer = null
		if (!state.connected) return
		console.log('[ble-service] 连接初始化：同步时间/传感器开关/用户信息/自动监测')
		try {
			await CoolWearBLE.syncTime()
		} catch (e) {}
		try {
			await CoolWearBLE.setSensorOn({ open: true })
		} catch (e) {}
		// 先同步设备基础信息/时间，再写用户信息，避免设备侧用默认用户信息覆盖真实资料
		await CoolWearBLE.sendSyncInfo().catch(() => {})
		try {
			await BleService.syncUserInfo()
		} catch (e) {}
		await BleService.enableAutoMeasure().catch(() => {})
		flushPendingUploads()
		pullCachedHealthEvents()
	}, 1500)
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
	let startMs = toTimeMs(session.startTime)
	let endMs = toTimeMs(session.endTime)
	let startSec = startMs ? Math.round(startMs / 1000) : 0
	let endSec = endMs ? Math.round(endMs / 1000) : 0
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

	// 设备偶发不带上/下半段绝对时间：用总时长补算，避免上报到 1970 或“当前时间”
	if (!startMs && endMs && totalMin > 0) {
		startMs = endMs - totalMin * 60000
		startSec = Math.round(startMs / 1000)
	}
	if (!endMs && startMs && totalMin > 0) {
		endMs = startMs + totalMin * 60000
		endSec = Math.round(endMs / 1000)
	}

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
		startMs,
		endMs,
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
	if (!sport) return
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
	if (!hasLogin()) {
		enqueueUpload(payload)
		return
	}
	console.log('[reportSportToCloud] 上报步数请求:', JSON.stringify(payload))
	uploadDeviceData(payload)
		.then((res) => {
			console.log('[reportSportToCloud] 上报步数响应:', JSON.stringify(res))
		})
		.catch((e) => {
			if (lastUploadedSportKey === key) lastUploadedSportKey = ''
			enqueueUpload(payload)
			console.log('[reportSportToCloud] 上报步数失败，进入补报队列:', JSON.stringify({
				msg: e && (e.msg || e.message),
				err: e && (e.errMsg || String(e))
			}))
		})
}

function reportSleepToCloud(sleep) {
	if (!sleep) return
	const startMs = Number(sleep.startMs) || 0
	const totalMin = Number(sleep.totalMin) || 0
	if (!startMs || totalMin <= 0) {
		console.log('[ble-service] 睡眠数据不完整，跳过上报:', JSON.stringify({
			startMs,
			totalMin
		}))
		return
	}
	uploadSleepSession({
		startMs,
		endMs: Number(sleep.endMs) || startMs + totalMin * 60000,
		totalMin,
		segments: sleep.segments || []
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
				return normalizeDeviceInfo({
					raw: data
				})
			}
		}
		const raw = data.raw
		const rawText = typeof raw === 'string' ? raw : ''
		const rawSoftwareVer = raw && typeof raw === 'object'
			? (raw.softwareVer != null ? raw.softwareVer : raw.softwareVersion)
			: (rawText.match(/softwareVer\s*=\s*['"]?([^,'"\]\s]+)/i) || [])[1]
		const firmwareValue = data.firmware != null
			? data.firmware
			: (data.softwareVer != null
				? data.softwareVer
				: (data.softwareVersion != null
					? data.softwareVersion
					: (data.SoftwareVer != null
						? data.SoftwareVer
						: (data.version != null ? data.version : rawSoftwareVer))))
		const firmware = firmwareValue != null ? String(firmwareValue) : null
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
			// 用户主动断开后，SDK 可能还会补发一次 connected/completed；直接忽略，避免状态被改回去
			if (status === 1 && userDisconnecting) {
				return
			}
			state.connectStatus = status
			if (status === 1) {
				state.connected = true
				// 连接就绪后统一下发初始化（内含去重：iOS connected/completed 双回调只执行一次）
				schedulePostConnectInit()
				CoolWearBLE.requestBattery().catch(() => {})
				CoolWearBLE.requestDeviceInfo().catch(() => {})
				persistDevice()
			} else if (status === 0) {
				// 只有明确的“断开”状态才重置连接态，避免“连接中(2)”误杀已建立的连接
				state.connected = false
				cancelPostConnectInit()
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
			console.log('[ble-service] onSleep raw=', data)
			handleSleepPayload(data)
		})

		// 自动监测数据上报（此前只上报了步数和睡眠，心率/血氧/HRV 收到后直接丢了）
		CoolWearBLE.on('onHeartRate', (data) => {
			reportRealtimeHeart(data)
		})

		CoolWearBLE.on('onHeartRateDaily', (data) => {
			reportDailyHeart(data)
		})

		CoolWearBLE.on('onBloodOxygen', (data) => {
			reportAutoOxygen(data)
		})

		CoolWearBLE.on('onHRV', (data) => {
			reportAutoHrv(data)
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

	let userDisconnecting = false
	let userDisconnectingTimer = null

	function setUserDisconnecting(value, holdMs = 8000) {
		userDisconnecting = value
		if (userDisconnectingTimer) {
			clearTimeout(userDisconnectingTimer)
			userDisconnectingTimer = null
		}
		if (value && holdMs > 0) {
			userDisconnectingTimer = setTimeout(() => {
				userDisconnecting = false
				userDisconnectingTimer = null
			}, holdMs)
		}
	}

	function isBluetoothOffError(e) {
		if (!e) return false
		if (e.code === -100 || e.code === '-100') return true
		const msg = String(e.message || e.msg || '')
		return msg === 'bluetooth_off' || /bluetooth_off|蓝牙.*(未开启|没有打开|已关闭)/i.test(msg)
	}

	function promptEnableBluetooth() {
		uni.showModal({
			title: '蓝牙未开启',
			content: '请先打开手机蓝牙，再搜索/连接设备',
			confirmText: '去开启',
			cancelText: '知道了',
			success: (res) => {
				if (res.confirm) {
					CoolWearBLE.openBluetoothSettings().catch(() => {})
				}
			}
		})
	}

	async function waitForIosBluetoothReady(timeoutMs = 4000) {
		if (!isIos()) return true
		const start = Date.now()
		while (Date.now() - start < timeoutMs) {
			try {
				const res = await CoolWearBLE.checkBluetoothPermission()
				const s = res && (res.state ?? res.status)
				const auth = res && res.authorization
				// 明确的“关/拒绝”才判失败，别把 Unknown(0) 当没开
				if (s === 4 || s === '4' || s === 'poweredOff') return false
				if (s === 3 || s === '3' || s === 'unauthorized') return false
				if (auth === 'denied' || auth === 'restricted') return false
				// 已就绪
				if (s === 5 || s === '5' || s === 'poweredOn' || s === 'on' || (res && res.poweredOn === true)) {
					return true
				}
				// 权限已授予，只是 state 还在 unknown（刚初始化）→ 视为就绪
				if (auth === 'allowedAlways') return true
				// 老基座没有该方法，走 fallback
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
					schedulePostConnectInit()
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
		async tryAutoReconnect(options = {}) {
			// #ifndef APP-PLUS
			return false
			// #endif
			// #ifdef APP-PLUS
			// force=true：仅安卓生效。安卓上系统/SDK 残留的"已连接"不会让设备重新 dump 历史数据，
			// 表现为已连接却收不到心率/血氧/HRV/睡眠；而 iOS 的 sendSyncInfo 会主动 requestAllInfo，
			// 冷启动自动重连也能拿到数据，故 iOS 保持原逻辑不动。
			const force = !!(options && options.force) && !isIos()
			loadStoredDevice()
			const device = state.device
			if (!device || !(device.mac || device.identifier || device.id)) {
				return false
			}
			if (state.connected && !force) return true
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

				// force 时先主动断开残留的"假连接"，确保后面是真正的全新连接
				if (force) {
					try {
						await CoolWearBLE.disconnect()
					} catch (e) {}
				}

				// 非强制：优先复用已连接状态，或走 SDK 快速重连
				if (!force) {
					try {
						const res = await CoolWearBLE.isConnected()
						if (res && res.connected) {
							state.connected = true
							state.connectStatus = 1
							emit()
							schedulePostConnectInit()
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
				}

				// 回退 / 强制：用本地缓存的 mac / identifier 主动全新连接
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
				schedulePostConnectInit()
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
				pullCachedHealthEvents()
			} finally {
				state.initing = false
				emit()
			}
		},

		async startScan(options = {}) {
			const timeoutMs = options.timeoutMs != null ? options.timeoutMs : 10000
			await BleService.ensureInit()
			if (isIos()) {
				const ready = await waitForIosBluetoothReady(4000)
				if (!ready) {
					promptEnableBluetooth()
					throw new Error('蓝牙未开启，请打开手机蓝牙后重试')
				}
			}
			if (scanTimer) {
				clearTimeout(scanTimer)
				scanTimer = null
			}
			state.devices = []
			state.scanning = true
			emit()
			try {
				await CoolWearBLE.startScan()
			} catch (e) {
				state.scanning = false
				emit()
				if (isBluetoothOffError(e)) {
					promptEnableBluetooth()
				}
				throw e
			}
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
			setUserDisconnecting(false)
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
			try {
				await CoolWearBLE.connect({
					mac: device.mac,
					identifier: device.identifier,
					name: device.name,
					deviceId: device.deviceId
				})
			} catch (e) {
				state.connectStatus = 0
				state.connected = false
				emit()
				if (isBluetoothOffError(e)) {
					promptEnableBluetooth()
				}
				throw e
			}
			const ok = await pollConnected(30000)
			if (!ok) {
				state.connectStatus = 0
				state.connected = false
				emit()
				throw new Error('连接超时，请确认戒指在附近且未被其他手机占用')
			}
			schedulePostConnectInit()
			persistDevice()
			uni.setStorageSync('onboardingDone', true)
			uni.setStorageSync('isFirstLogin', false)
			emit()
			notifyBindToCloud()
			return cloneState()
		},

		beginManualMeasure() {
			state.manualMeasure = state.connected
			return state.manualMeasure
		},

		endManualMeasure() {
			state.manualMeasure = false
			return true
		},

		async restoreAutoMonitor() {
			if (!state.connected) return false
			return BleService.enableAutoMeasure()
		},

		async enableAutoMeasure() {
			if (!state.connected) return false
			// 仅配置设备的自动监测计划；实时心率、HRV、血氧开关只由对应手动测量页面控制。
			try {
				await CoolWearBLE.setHeartAutoSwitch({
					heart: true,
					o2: true,
					intervalMin: 30
				})
				return true
			} catch (e) {
				console.log('[ble-service] setHeartAutoSwitch failed', e)
				return false
			}
		},

		/** 同步用户信息到设备（睡眠/自动监测判定依赖设备端用户信息，官方连接流程要求） */
		async syncUserInfo() {
			if (!state.connected) return false
			let profile = {}
			try {
				profile = uni.getStorageSync('userBasicInfo') || {}
			} catch (e) {}
			let userId = 0
			try {
				userId = Number(uni.getStorageSync('userId')) || 0
			} catch (e) {}
			const toInt = (v, def) => {
				const n = Number(v)
				return Number.isNaN(n) || !n ? def : Math.round(n)
			}
			let age = 30
			if (profile.birthday) {
				const d = new Date(String(profile.birthday).replace(/-/g, '/'))
				if (!isNaN(d.getTime())) {
					const now = new Date()
					age = Math.max(1, now.getFullYear() - d.getFullYear())
				}
			}
			const params = {
				userId,
				sex: profile.gender === 'female' ? 1 : 0, // 设备协议：1 女，0 男
				age,
				height: toInt(profile.height, 170),
				weight: toInt(profile.weight, 60),
				lrHand: 1
			}
			try {
				await CoolWearBLE.syncUserInfo(params)
				console.log('[ble-service] syncUserInfo 已发送', params)
				return true
			} catch (e) {
				console.log('[ble-service] syncUserInfo skipped', e && (e.message || e))
				return false
			}
		},

		/** App 回前台：重开 sensor 上传开关、主动触发设备重传缓存、补报队列、回放原生缓存事件 */
		async onAppShow() {
			if (state.connected) {
				try {
					await CoolWearBLE.setSensorOn({ open: true })
				} catch (e) {}
				// 对齐官方 demo 的 MainActivity.onResume：回前台时主动 sendSyncInfo，
				// 让设备把后台缓存的心率/血氧/HRV/睡眠重新上传。（仅安卓；iOS 原有逻辑已正常，不动）
				if (!isIos()) {
					await CoolWearBLE.sendSyncInfo().catch(() => {})
				}
			}
			flushPendingUploads()
			pullCachedHealthEvents()
		},

		/** App 进后台：按官方建议关闭 sensor 上传开关（设备会缓存数据，前台/重连后补发） */
		async onAppHide() {
			if (!state.connected) return
			try {
				await CoolWearBLE.setSensorOn({ open: false })
			} catch (e) {}
		},

		async disconnect(unbind = false) {
			let bindId = ''
			if (unbind) bindId = await resolveBindId()
			cancelPostConnectInit()
			setUserDisconnecting(true)
			try {
				if (state.inited) {
					await BleService.stopScan().catch(() => {})
					if (unbind) {
						await CoolWearBLE.unbind()
					} else {
						await CoolWearBLE.disconnect()
					}
				}
			} finally {
				state.connected = false
				state.connectStatus = 0
				state.manualMeasure = false
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