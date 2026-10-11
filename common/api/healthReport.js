export function getWeeklyList(params = {}) {
	return uni.$get('/appApi/healthReport/weeklyList', params)
}

export function getWeeklyDetail(date) {
	return uni.$get('/appApi/healthReport/weekly', date ? { date } : {})
}

export function getDailyDetail(date) {
	return uni.$get('/appApi/healthReport/daily', date ? { date } : {})
}
