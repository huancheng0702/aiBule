import {
	$http
} from '@escook/request-miniprogram'
import {
	LOGIN_TOKEN_KEY,
	LOGIN_PATH,
	clearLoginSession,
	AUTH_GUARD_ENABLED
} from '@/common/auth.js'

const LOGIN_EXPIRED_CODE = 401
const SUCCESS_CODES = [0, 1, 200]

let isHandlingLoginExpired = false

function getCurrentPath() {
	const pages = getCurrentPages()
	if (!pages.length) {
		return ''
	}
	const page = pages[pages.length - 1]
	return '/' + (page.route || '')
}

function redirectToLogin(message = '登录失效') {
	if (!AUTH_GUARD_ENABLED) {
		return
	}
	if (isHandlingLoginExpired) {
		return
	}
	if (getCurrentPath() === LOGIN_PATH) {
		clearLoginSession()
		return
	}
	isHandlingLoginExpired = true
	clearLoginSession()
	uni.showToast({
		title: message,
		icon: 'none'
	})
	setTimeout(() => {
		uni.reLaunch({
			url: LOGIN_PATH,
			complete: () => {
				setTimeout(() => {
					isHandlingLoginExpired = false
				}, 300)
			}
		})
	}, 1200)
}

export function getHttpBaseUrl() {
	let baseURL = 'https://fx.tailunkaluo.com'
	// #ifdef H5
	if (process.env.NODE_ENV === 'development') {
		baseURL = ''
	}
	// #endif
	return baseURL
}

export function buildApiUrl(path) {
	return getHttpBaseUrl() + path
}

function isBizSuccess(code) {
	if (code === undefined || code === null || code === '') {
		return true
	}
	return SUCCESS_CODES.indexOf(Number(code)) !== -1
}

function unwrap(response) {
	const body = response && response.data
	if (body && Number(body.code) === LOGIN_EXPIRED_CODE) {
		redirectToLogin(body.msg || '登录失效')
		return Promise.reject(body)
	}
	if (body && !isBizSuccess(body.code)) {
		const msg = body.msg || body.message || '请求失败'
		uni.showToast({
			title: String(msg).slice(0, 40),
			icon: 'none'
		})
		return Promise.reject(body)
	}
	if (body && body.data !== undefined) {
		return body.data
	}
	return body
}

function send(method, url, data = {}) {
	return $http[method](url, data).then(unwrap)
}

export function installHttp() {
	uni.$http = $http
	$http.baseUrl = getHttpBaseUrl()

	$http.beforeRequest = function(options) {
		const header = options.header || {}
		const token = uni.getStorageSync(LOGIN_TOKEN_KEY)
		if (token) {
			header.token = token
		}
		const method = String(options.method || 'GET').toUpperCase()
		if (method === 'POST' || method === 'PUT' || method === 'DELETE') {
			header['Content-Type'] = 'application/json'
		}
		options.header = header
	}

	$http.afterRequest = function(response) {
		const res = response && response.data
		if (res && Number(res.code) === LOGIN_EXPIRED_CODE) {
			redirectToLogin(res.msg || '登录失效')
		}
	}

	uni.$get = function(url, data = {}) {
		return send('get', url, data)
	}
	uni.$post = function(url, data = {}) {
		return send('post', url, data)
	}
	uni.$put = function(url, data = {}) {
		return send('put', url, data)
	}
	uni.$delete = function(url, data = {}) {
		return send('delete', url, data)
	}
}
