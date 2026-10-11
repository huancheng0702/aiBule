export function getHealthCenterIndex(date) {
	return uni.$get('/appApi/healthCenter/index', date ? { date } : {})
}

export function getPortraitList(days = 7) {
	return uni.$get('/appApi/healthCenter/portraitList', { days })
}

export function getOrganArchiveCount(days = 7) {
	return uni.$get('/appApi/healthCenter/organArchiveCount', { days })
}
