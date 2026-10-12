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

export function saveDeviceConfig(data) {
	return uni.$post('/appApi/device/config/save', data)
}

export function getDeviceConfig(configType) {
	return uni.$get('/appApi/device/config/' + configType)
}

export function getDeviceConfigList() {
	return uni.$get('/appApi/device/config/list')
}
