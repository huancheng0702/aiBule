export function sendSmsCode(data) {
	return uni.$post('/appApi/auth/sendSmsCode', data)
}

export function phoneLogin(data) {
	return uni.$post('/appApi/auth/phoneLogin', data)
}

export function oneKeyLogin(data) {
	return uni.$post('/appApi/auth/oneKeyLogin', data)
}

export function wxLogin(data) {
	return uni.$post('/appApi/auth/wxLogin', data)
}

export function bindPhone(data) {
	return uni.$post('/appApi/auth/bindPhone', data)
}

export function changePhone(data) {
	return uni.$post('/appApi/auth/changePhone', data)
}
