export function getHomeDisplay() {
	return uni.$get('/appApi/manualHealthRecord/display')
}

export function updateHomeDisplay(data) {
	return uni.$put('/appApi/manualHealthRecord/display', data)
}

export function getManualHealthList() {
	return uni.$get('/appApi/manualHealthRecord/list')
}

export function addManualHealthRecord(data) {
	return uni.$post('/appApi/manualHealthRecord/add', data)
}

export function getWeightTrend(params = {}) {
	return uni.$get('/appApi/manualHealthRecord/weight/trend', params)
}

export function getBloodSugarTrend(params = {}) {
	return uni.$get('/appApi/manualHealthRecord/bloodSugar/trend', params)
}

export function getBodyTemperatureTrend(params = {}) {
	return uni.$get('/appApi/manualHealthRecord/bodyTemperature/trend', params)
}

export function getBloodPressureTrend(params = {}) {
	return uni.$get('/appApi/manualHealthRecord/bloodPressure/trend', params)
}
