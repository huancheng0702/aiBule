export function saveHealthArchive(data) {
	return uni.$post('/appApi/healthArchive/save', data)
}

export function getHealthArchiveWeekly(date) {
	return uni.$get('/appApi/healthArchive/weekly', date ? { date } : {})
}

export function getHealthArchiveOrgans() {
	return uni.$get('/appApi/healthArchive/organs')
}

export function getHealthArchiveOptions(archiveType) {
	return uni.$get('/appApi/healthArchive/options', { archiveType })
}

export function getHealthArchiveDetail(archiveType = 0) {
	return uni.$get('/appApi/healthArchive/detail', { archiveType })
}

export function getHealthArchiveByOrgan(categoryId) {
	return uni.$get('/appApi/healthArchive/byOrgan', { categoryId })
}
