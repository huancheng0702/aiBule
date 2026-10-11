export function getDeviceDashboard(date) {
	return uni.$get('/appApi/deviceData/dashboard', date ? { date } : {})
}

export function getDeviceLatest(dataType) {
	return uni.$get('/appApi/deviceData/latest', { dataType })
}

export function getSportTrend(date) {
	return uni.$get('/appApi/deviceData/trend/sport', date ? { date } : {})
}

export function getSleepTrend(date, range) {
	const query = {}
	if (date) query.date = date
	if (range) query.range = range
	return uni.$get('/appApi/deviceData/trend/sleep', query)
}

export function getHeartRateTrend(date, range) {
	const query = {}
	if (date) query.date = date
	if (range) query.range = range
	return uni.$get('/appApi/deviceData/trend/heartRate', query)
}

export function getBloodOxygenTrend(date, range) {
	const query = {}
	if (date) query.date = date
	if (range) query.range = range
	return uni.$get('/appApi/deviceData/trend/bloodOxygen', query)
}

export function getHrvTrend(date, range) {
	const query = {}
	if (date) query.date = date
	if (range) query.range = range
	return uni.$get('/appApi/deviceData/trend/hrv', query)
}

export function getStressTrend(date, range) {
	const query = {}
	if (date) query.date = date
	if (range) query.range = range
	return uni.$get('/appApi/deviceData/trend/stress', query)
}

export function getDeviceDataList(params = {}) {
	return uni.$get('/appApi/deviceData/list', params)
}

export function getMeasureResult(measureId, dataType) {
	const query = { measureId }
	if (dataType) query.dataType = dataType
	return uni.$get('/appApi/deviceData/measureResult', query)
}

export function uploadDeviceData(payload) {
	return uni.$post('/appApi/deviceData/upload', payload)
}

