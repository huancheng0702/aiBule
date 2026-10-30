export function getDeviceList() {
	return uni.$get('/appApi/device/list')
}

export function getDeviceDetail(bindId) {
	return uni.$get('/appApi/device/' + bindId)
}

export function bindDevice(data) {
	return uni.$post('/appApi/device/bind', data)
}

export function unbindDevice(bindId) {
	return uni.$delete('/appApi/device/unbind/' + bindId)
}

export function syncDeviceStatus(data) {
	return uni.$post('/appApi/device/syncStatus', data)
}

export function getLatestFirmware(params = {}) {
	// 固件检查接口：GET /appApi/firmware/latest，无请求参数
	// 返回 data: { version, downloadUrl, description, status, publishTime, ... }
	return uni.$get('/appApi/firmware/latest', params)
}

export function saveDeviceConfig(data) {
	return uni.$post('/appApi/device/config/save', data)
}

export function getDeviceConfig(configType) {
	return uni.$get('/appApi/device/config/' + configType)
}export function getDeviceConfigList() {
	return uni.$get('/appApi/device/config/list')
}