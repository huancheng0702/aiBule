export function getDeviceList() {
	return uni.$get('/appApi/device/list')
}

export function saveDeviceConfig(data) {
	return uni.$post('/appApi/device/config/save', data)
}

