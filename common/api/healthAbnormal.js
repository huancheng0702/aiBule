export function getAbnormalList(params = {}) {
	return uni.$get('/appApi/healthAbnormal/list', params)
}

export function getAbnormalDetail(abnormalId) {
	return uni.$get(`/appApi/healthAbnormal/${abnormalId}`)
}
