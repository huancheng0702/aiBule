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
  'setHeartAutoSwitch'
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

  requestBattery() {
    return callNative('requestBattery')
  },

  requestDeviceInfo() {
    return callNative('requestDeviceInfo')
  },

  reconnect() {
    return callNative('reconnect')
  }
}

export default CoolWearBLE
