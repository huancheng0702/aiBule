export function getPeriodStatistics() {
	return uni.$get('/appApi/period/statistics')
}

export function getPeriodCalendar(params = {}) {
	return uni.$get('/appApi/period/calendar', params)
}

export function initPeriod(data) {
	return uni.$post('/appApi/period/init', data)
}

export function togglePeriodToday(data) {
	return uni.$post('/appApi/period/today', data)
}
