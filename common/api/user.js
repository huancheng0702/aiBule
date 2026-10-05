export function getUserInfo() {
	return uni.$get('/appApi/user/info')
}

export function editUserInfo(data) {
	return uni.$post('/appApi/user/editInfo', data)
}

export function cancelAccount() {
	return uni.$post('/appApi/user/cancel')
}
