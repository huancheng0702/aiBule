export const LOGIN_TOKEN_KEY = 'token'
export const USER_ID_KEY = 'userId'
export const USER_INFO_KEY = 'userInfo'
export const LOGIN_PATH = '/pages/login'
export const HOME_PATH = '/pages/index/index'
export const ONBOARDING_PATH = '/pages/mine/myInfo'

const WHITE_LIST = [LOGIN_PATH]

let bypassAuth = false

export function hasLogin() {
	return !!uni.getStorageSync(LOGIN_TOKEN_KEY)
}

export function saveLoginSession(data = {}, loginType = '') {
	const token = data.token || data.accessToken
	if (token) {
		uni.setStorageSync(LOGIN_TOKEN_KEY, token)
	}
	if (data.userId != null) {
		uni.setStorageSync(USER_ID_KEY, data.userId)
	}
	if (data.phone) {
		uni.setStorageSync('phone', data.phone)
	}
	const nickName = data.nickName || data.nickname
	if (nickName) {
		uni.setStorageSync('nickName', nickName)
	}
	if (data.avatar) {
		uni.setStorageSync('avatar', data.avatar)
	}
	if (loginType) {
		uni.setStorageSync('loginType', loginType)
	}
	const prev = uni.getStorageSync(USER_INFO_KEY) || {}
	uni.setStorageSync(USER_INFO_KEY, Object.assign({}, prev, data))
}

export function clearLoginSession() {
	;[LOGIN_TOKEN_KEY, USER_ID_KEY, USER_INFO_KEY, 'phone', 'nickName', 'avatar', 'loginType'].forEach((key) => {
		uni.removeStorageSync(key)
	})
}

export function getEntryPath() {
	if (!hasLogin()) {
		return LOGIN_PATH
	}
	if (!uni.getStorageSync('onboardingDone')) {
		return ONBOARDING_PATH
	}
	return HOME_PATH
}

function normalizePath(url = '') {
	const path = String(url).split('?')[0]
	if (!path) {
		return ''
	}
	return path.charAt(0) === '/' ? path : '/' + path
}

function reLaunchBypass(url) {
	bypassAuth = true
	uni.reLaunch({
		url,
		complete: () => {
			setTimeout(() => {
				bypassAuth = false
			}, 300)
		}
	})
}

function allowNavigate(url) {
	const path = normalizePath(url)
	if (!hasLogin()) {
		return WHITE_LIST.indexOf(path) !== -1
	}
	return path !== LOGIN_PATH
}

export function applyLaunchAuth() {
	const target = getEntryPath()
	if (target === LOGIN_PATH) {
		return
	}
	reLaunchBypass(target)
}

export function installAuthInterceptor() {
	;['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'].forEach((method) => {
		uni.addInterceptor(method, {
			invoke(args) {
				if (bypassAuth) {
					return args
				}
				if (allowNavigate(args && args.url)) {
					return args
				}
				reLaunchBypass(getEntryPath())
				return false
			}
		})
	})
}
