/**
 * CoolWear BLE Plugin - Unified JS API
 * 业务代码请从此文件 import，不要从 nativeplugins 目录 import
 */

const PLUGIN_ID = 'CoolWear-BLE'
const EVENT_CHANNEL = 'CoolWearBLE'

const METHODS_WITH_OPTIONS = new Set([
  'connect',
  'devicePairFinish',
  'setHeartRateSwitch',
  'setHRVSwitch',
  'setBloodOxygenSwitch',
  'setHeartAutoSwitch',
  'setDeviceName'
])

let nativeModule = null
const listeners = new Map()
let eventBound = false

function getNativeModule() {
  // #ifdef APP-PLUS
  if (!nativeModule) {
    nativeModule = uni.requireNativePlugin(PLUGIN_ID)
    console.log('[CoolWear-BLE] requireNativePlugin:', PLUGIN_ID, nativeModule)
  }
  return nativeModule
  // #endif
  // #ifndef APP-PLUS
  return null
  // #endif
}

// iOS SDK 包一层 {curItemCount, sportInfos/heartInfos/data:[...], remainItemCount}，安卓是平铺数组，这里统一拆平
function flattenIOSWrapper(data) {
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const inner = data.sportInfos || data.heartInfos || data.data || data.list || data.items
    if (Array.isArray(inner)) return inner
    return data
  }
  if (Array.isArray(data)) {
    const out = []
    data.forEach((it) => {
      if (it && typeof it === 'object' && !Array.isArray(it)) {
        const inner = it.sportInfos || it.heartInfos || it.data
        if (Array.isArray(inner)) {
          inner.forEach((s) => out.push(s))
          return
        }
      }
      out.push(it)
    })
    return out
  }
  return data
}

function aliasValueItem(item) {
  if (!item || typeof item !== 'object' || Array.isArray(item)) return item
  if (item.value != null) return item
  const v = item.heartNum != null ? item.heartNum
    : (item.heartNums != null ? item.heartNums
    : (item.oxygen != null ? item.oxygen
    : (item.hrv != null ? item.hrv
    : (item.hrvNums != null ? item.hrvNums
    : (item.tempValue != null ? item.tempValue
    : (item.walkSteps != null ? item.walkSteps : null))))))
  if (v == null) return item
  const out = { ...item, value: v }
  // 时间：秒转毫秒
  if (out.time != null && Number(out.time) < 1e12) {
    const n = Number(out.time)
    if (n > 1e9 && n < 1e11) out.time = n * 1000
  }
  if (out.oxygen != null) out.o2 = out.oxygen
  if (out.heartNum != null) { out.heart = out.heartNum; out.hr = out.heartNum }
  if (out.walkSteps != null) out.steps = out.walkSteps
  if (out.walkDistance != null) out.distance = out.walkDistance
  if (out.walkCalories != null) out.calories = out.walkCalories
  return out
}

function normalizeStreamData(event, data) {
  const flat = flattenIOSWrapper(data)
  if (Array.isArray(flat) && (event === 'onHeartRate' || event === 'onHeartRateDaily' || event === 'onHRV' || event === 'onBloodOxygen' || event === 'onTemperature' || event === 'onSport')) {
    return flat.map(aliasValueItem)
  }
  return flat
}

function bindGlobalEvent() {
  if (eventBound) return
  // #ifdef APP-PLUS
  plus.globalEvent.addEventListener(EVENT_CHANNEL, (payload) => {
    const event = payload && payload.event
    if (!event) return
    let data = payload && payload.data
    // 原生侧偶发把 data 序列化成 JSON 字符串
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data)
      } catch (e) {
        // keep string
      }
    }
    data = normalizeStreamData(event, data)
    const handlers = listeners.get(event)
    if (handlers) {
      handlers.forEach((fn) => {
        try {
          fn(data)
        } catch (e) {
          console.error('[CoolWear-BLE] event handler error:', event, e)
        }
      })
    }
  })
  eventBound = true
  // #endif
}

function normalizeResult(result) {
  if (result == null) {
    return { code: 0 }
  }
  if (typeof result === 'string') {
    try {
      return JSON.parse(result)
    } catch (e) {
      return { code: -1, message: result }
    }
  }
  return result
}

function callNative(method, args = {}, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    const mod = getNativeModule()
    if (!mod) {
      reject(new Error('未找到原生插件，请重新制作并安装自定义调试基座'))
      return
    }
    if (typeof mod[method] !== 'function') {
      reject(new Error(`原生方法不存在: ${method}，请重新制作自定义基座`))
      return
    }

    bindGlobalEvent()

    let settled = false
    const timer = timeoutMs > 0 ? setTimeout(() => {
      if (settled) return
      settled = true
      reject(new Error(`原生调用超时: ${method}（${timeoutMs / 1000}s），请重新制作自定义基座或查看 Android logcat`))
    }, timeoutMs) : null

    const callback = (result) => {
      if (settled) return
      settled = true
      if (timer) clearTimeout(timer)
      console.log('[CoolWear-BLE] callback', method, result)
      const res = normalizeResult(result)
      if (res.code === 0 || res.code === '0') {
        resolve(res.data !== undefined ? res.data : res)
      } else {
        reject(new Error(res.message || `Native call ${method} failed`))
      }
    }

    try {
      console.log('[CoolWear-BLE] call', method, METHODS_WITH_OPTIONS.has(method) ? args : '(no args)')
      if (METHODS_WITH_OPTIONS.has(method)) {
        mod[method](args, callback)
      } else {
        mod[method](callback)
      }
    } catch (e) {
      if (timer) clearTimeout(timer)
      settled = true
      console.error('[CoolWear-BLE] call error', method, e)
      reject(e)
    }
  })
}

const CoolWearBLE = {
  isAvailable() {
    const mod = getNativeModule()
    return !!(mod && typeof mod.init === 'function')
  },

  getNativeModule() {
    return getNativeModule()
  },

  on(event, handler) {
    bindGlobalEvent()
    if (!listeners.has(event)) {
      listeners.set(event, new Set())
    }
    listeners.get(event).add(handler)
    return () => CoolWearBLE.off(event, handler)
  },

  off(event, handler) {
    const handlers = listeners.get(event)
    if (handlers) {
      handlers.delete(handler)
    }
  },

  init() {
    return callNative('init')
  },

  startScan() {
    return callNative('startScan')
  },

  stopScan() {
    return callNative('stopScan')
  },

  connect(options = {}) {
    return callNative('connect', options)
  },

  disconnect() {
    return callNative('disconnect')
  },

  isConnected() {
    return callNative('isConnected')
  },

  devicePairFinish(options = { vibrate: 1 }) {
    return callNative('devicePairFinish', options)
  },

  sendSyncInfo() {
    return callNative('sendSyncInfo')
  },

  setHeartRateSwitch(open = true) {
    return callNative('setHeartRateSwitch', { open })
  },

  setHRVSwitch(open = true) {
    return callNative('setHRVSwitch', { open })
  },

  setBloodOxygenSwitch(open = true) {
    return callNative('setBloodOxygenSwitch', { open })
  },

  setHeartAutoSwitch(options = {}) {
    return callNative('setHeartAutoSwitch', {
      heart: options.heart !== false,
      o2: options.o2 !== false,
      intervalMin: options.intervalMin || 30
    })
  },

  /** 修改设备蓝牙名称，写入戒指硬件（修改后通常需断开重连生效） */
  setDeviceName(name) {
    return callNative('setDeviceName', { name })
  },

  requestBattery() {
    return callNative('requestBattery')
  },

  requestDeviceInfo() {
    return callNative('requestDeviceInfo')
  },

  reconnect() {
    return callNative('reconnect')
  },

  checkBluetoothPermission() {
    // #ifdef APP-PLUS
    try {
      const mod = getNativeModule()
      if (mod && typeof mod.checkBluetoothPermission === 'function') {
        return callNative('checkBluetoothPermission', {}, 8000)
      }
    } catch (e) {}
    return Promise.resolve({ authorization: 'unknown', state: -1, fallback: true })
    // #endif
    // #ifndef APP-PLUS
    return Promise.resolve({ authorization: 'unknown', fallback: true })
    // #endif
  },

  requestBluetoothPermission() {
    // #ifdef APP-PLUS
    try {
      const mod = getNativeModule()
      // 老基座没有该原生方法时不抛错：iOS在init/startScan时系统会自动弹框
      if (!mod || typeof mod.requestBluetoothPermission !== 'function') {
        console.log('[CoolWear-BLE] requestBluetoothPermission not in native, skip')
        return Promise.resolve({ granted: true, fallback: true })
      }
    } catch (e) {}
    return callNative('requestBluetoothPermission', {}, 20000)
    // #endif
    // #ifndef APP-PLUS
    return Promise.resolve({ granted: true, fallback: true })
    // #endif
  },

  openAppSettings() {
    // #ifdef APP-PLUS
    try {
      const mod = getNativeModule()
      if (!mod || typeof mod.openAppSettings !== 'function') {
        // 降级：用系统设置意图
        const UIApplication = plus.ios.importClass('UIApplication')
        const NSURL = plus.ios.importClass('NSURL')
        const app = UIApplication.sharedApplication()
        const url = NSURL.URLWithString('app-settings:')
        if (url && app.canOpenURL(url)) app.openURL(url)
        plus.ios.deleteObject(app)
        return Promise.resolve({})
      }
    } catch (e) {}
    return callNative('openAppSettings', {}, 8000)
    // #endif
    // #ifndef APP-PLUS
    return Promise.resolve({})
    // #endif
  }
}

export default CoolWearBLE
