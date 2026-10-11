export function getHealthManager() {
	return uni.$get('/appApi/configInfo/healthManager')
}

export function getHealthTypeInfoList(dataType) {
	return uni.$get('/appApi/healthTypeInfo/list', { dataType })
}
