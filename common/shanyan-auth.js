/**
 * 创蓝闪验一键登录封装（云端插件版）
 * 插件：CLSDK-ShanYanSDKModule（插件市场 id=645，v2.3.7.2）
 * 集成文档：https://doc.chuanglan.com/docs/HT606M7VPC0IXQVF
 *
 * 引用方式：const m = uni.requireNativePlugin('CLSDK-ShanYanSDKModule')
 * Android：init / getPhoneInfo / setAuthThemeConfig / openLoginAuth(true, openCb, loginCb) / finishAuthActivity
 * iOS：init / preGetPhonenumber / quickAuthLoginWithConfigure(clUIConfigure, openCb, loginCb) / finishAuthControllerCompletion
 */

import { oneKeyLogin } from '@/common/api/auth.js'

export const SHANYAN_APP_IDS = {
	ios: 'a3dqMjNX',
	android: 'nDHG1A3T'
}
export const SHANYAN_APP_ID = SHANYAN_APP_IDS.android

export function getShanYanAppId() {
	// #ifdef APP-PLUS
	try {
		if (typeof plus !== 'undefined' && plus.os && plus.os.name) {
			return String(plus.os.name).toLowerCase() === 'ios' ? SHANYAN_APP_IDS.ios : SHANYAN_APP_IDS.android
		}
	} catch (e) {}
	// #endif
	try {
		const sys = uni.getSystemInfoSync() || {}
		const osName = String(sys.osName || sys.platform || '').toLowerCase()
		if (osName.indexOf('ios') !== -1 || osName.indexOf('iphone') !== -1) return SHANYAN_APP_IDS.ios
	} catch (e) {}
	return SHANYAN_APP_IDS.android
}

function getOS() {
	// #ifdef APP-PLUS
	try {
		if (typeof plus !== 'undefined' && plus.os && plus.os.name) {
			return String(plus.os.name).toLowerCase() === 'ios' ? 'ios' : 'android'
		}
	} catch (e) {}
	// #endif
	try {
		const sys = uni.getSystemInfoSync() || {}
		return String(sys.platform || '').toLowerCase() === 'ios' ? 'ios' : 'android'
	} catch (e) {}
	return 'android'
}

export const PRIVACY_LINKS = {
	privacyPolicy: { name: '《隐私政策》', url: 'https://fx.tailunkaluo.com/h5/content.html?id=5' },
	userAgreement: { name: '《用户协议》', url: 'https://fx.tailunkaluo.com/h5/content.html?id=1' }
}

const STORAGE_PRIVACY = 'privacyAgreed'
let inited = false
let opening = false

function getSdk() {
	// #ifdef APP-PLUS
	try {
		return uni.requireNativePlugin('CLSDK-ShanYanSDKModule')
	} catch (e) {
		return null
	}
	// #endif
	// #ifndef APP-PLUS
	return null
	// #endif
}

export function isPrivacyAgreed() {
	return !!uni.getStorageSync(STORAGE_PRIVACY)
}
export function setPrivacyAgreed(val = true) {
	uni.setStorageSync(STORAGE_PRIVACY, !!val)
}

export function exchangeLoginByShanYanToken(shanyanToken) {
	const platform = getOS()
	return oneKeyLogin({ token: shanyanToken, platform })
}

function rpx2dp(rpx) {
	return Math.round(rpx / 2)
}

function parseToken(result) {
	if (!result) return ''
	if (typeof result === 'string') {
		try {
			const o = JSON.parse(result)
			return o.token || o.data?.token || ''
		} catch (e) { return result }
	}
	if (typeof result === 'object') {
		if (typeof result.result === 'string') {
			try {
				const o = JSON.parse(result.result)
				if (o && o.token) return o.token
			} catch (e) {}
		}
		if (result.result && typeof result.result === 'object' && result.result.token) return result.result.token
		if (result.data && result.data.token) return result.data.token
		if (result.token) return result.token
	}
	return ''
}

function isInitOk(res) {
	if (!res) return false
	if (res.code === 1022 || res.code === 1021) return true
	// iOS 统一 CLComplete：errorCode 存在即失败
	if (res.errorCode !== undefined && res.errorCode !== null && res.errorCode !== '') return false
	if (res.code === 1000) return true
	return false
}

/**
 * Android 授权页主题（setAuthThemeConfig 的 uiConfig 部分）
 * 注意：图片路径走 static/ 下文件，插件内部自动取包内资源
 */
function buildAndroidUIConfig() {
	const sys = uni.getSystemInfoSync()
	const btnWidth = Math.floor(sys.screenWidth - rpx2dp(60))
	return {
		uiConfig: {
			setAuthBGImgPath: 'static/loginBanner.png',
			setFullScreen: 'true',
			setLightColor: false,
			setAuthNavHidden: true,
			setAuthNavTransparent: true,
			setNavReturnImgHidden: true,
			setLogoHidden: true,
			setNumberColor: '#09090D',
			setNumberSize: '28',
			setNumberBold: true,
			setNumFieldOffsetBottomY: String(rpx2dp(40 + 80 + 30 + 96 + 30 + 96 + 40)),
			setNumFieldWidth: String(btnWidth),
			setSloganHidden: 'true',
			setShanYanSloganHidden: true,
			setLogBtnText: '本机号码一键登录',
			setLogBtnTextColor: '#FFFFFF',
			setLogBtnTextSize: '15',
			setLogBtnWidth: String(btnWidth),
			setLogBtnHeight: String(rpx2dp(96)),
			setLogBtnOffsetX: String(rpx2dp(30)),
			setLogBtnOffsetBottomY: String(rpx2dp(40 + 80 + 30 + 96 + 30)),
			setLogBtnBackgroundColor: '#2C89FF',
			setPrivacyState: 'false',
			setPrivacyTextSize: '12',
			setAppPrivacyColor: { privacyBaseColor: '#666666', privacyColor: '#2C89FF' },
			setPrivacyOffsetBottomY: String(rpx2dp(40)),
			setPrivacyOffsetX: String(rpx2dp(30)),
			setPrivacyOffsetGravityLeft: true,
			setPrivacyGravityHorizontalCenter: false,
			setCheckedImgPath: 'static/celiangSuccess.png',
			setUncheckedImgPath: 'static/checkCircle.png',
			setCheckBoxWH: { width: '18', height: '18' },
			setPrivacyCustomToastText: '请先同意用户协议',
			setPrivacyText: {
				privacyTextOne: '我已阅读并同意',
				privacyTextTwo: '及健康',
				privacyTextThree: '',
				privacyTextFour: '',
				privacyTextFive: '并授权获取本机号码'
			},
			setAppPrivacyOne: { title: PRIVACY_LINKS.privacyPolicy.name, url: PRIVACY_LINKS.privacyPolicy.url },
			setAppPrivacyTwo: { title: PRIVACY_LINKS.userAgreement.name, url: PRIVACY_LINKS.userAgreement.url }
		},
		widgets: {
			widgetWx: {
				widgetId: 'customView_wx',
				type: 'TextView',
				width: String(btnWidth),
				height: String(rpx2dp(96)),
				bottom: String(rpx2dp(40 + 80 + 30)),
				textContent: '微信登录',
				textFont: '15',
				textColor: '#2C89FF',
				backgroundColor: '#00000000',
				isFinish: 'true'
			}
		}
	}
}

/** iOS 授权页配置（quickAuthLoginWithConfigure 的 clUIConfigure） */
function buildIOSUIConfig() {
	const sys = uni.getSystemInfoSync() || {}
	const sw = sys.screenWidth || 375
	const scale = Math.min(sw / 375, 1)
	const sidePad = 30 * scale
	const btnH = 48 * scale
	const loginBottom = (40 + 80 + 30 + 96 + 30) / 2 * scale
	const numberBottom = (40 + 80 + 30 + 96 + 30 + 96 + 40) / 2 * scale
	const wxBottom = (40 + 80 + 30) / 2 * scale
	return {
		clBackgroundImg: 'static/loginBanner.png',
		shouldAutorotate: false,
		supportedInterfaceOrientations: 0,
		clNavigationBarHidden: true,
		clNavigationBackgroundClear: true,
		clNavigationBackBtnHidden: true,
		clLogoHiden: true,
		clPhoneNumberColor: [0.035, 0.035, 0.05, 1.0],
		clPhoneNumberFont: 28,
		clLoginBtnText: '本机号码一键登录',
		clLoginBtnTextColor: [1, 1, 1, 1.0],
		clLoginBtnBgColor: [0.17, 0.54, 1.0, 1.0],
		clLoginBtnTextFont: 15,
		clLoginBtnCornerRadius: 16,
		clSloganTextHidden: true,
		clCheckBoxHidden: false,
		clCheckBoxValue: false,
		clCheckBoxSize: [18, 18],
		clCheckBoxCheckedImage: 'static/celiangSuccess.png',
		clCheckBoxUncheckedImage: 'static/checkCircle.png',
		clAppPrivacyColor: [[0.4, 0.4, 0.4, 1.0], [0.17, 0.54, 1.0, 1.0]],
		clAppPrivacyTextFont: 12 * scale,
		clAppPrivacyTextAlignment: 1,
		clAppPrivacyFirst: [PRIVACY_LINKS.privacyPolicy.name, PRIVACY_LINKS.privacyPolicy.url],
		clAppPrivacySecond: [PRIVACY_LINKS.userAgreement.name, PRIVACY_LINKS.userAgreement.url],
		clAppPrivacyNormalDesTextFirst: '我已阅读并同意',
		clAppPrivacyNormalDesTextSecond: '及健康',
		clAppPrivacyNormalDesTextThird: '',
		clAppPrivacyNormalDesTextFourth: '并授权获取本机号码',
		clOrientationLayOutPortrait: {
			clLayoutPhoneLeft: sidePad,
			clLayoutPhoneRight: -sidePad,
			clLayoutPhoneHeight: 40,
			clLayoutPhoneBottom: -numberBottom,
			clLayoutLoginBtnLeft: sidePad,
			clLayoutLoginBtnRight: -sidePad,
			clLayoutLoginBtnHeight: btnH,
			clLayoutLoginBtnBottom: -loginBottom,
			clLayoutAppPrivacyLeft: sidePad,
			clLayoutAppPrivacyRight: -sidePad,
			clLayoutAppPrivacyHeight: 50,
			clLayoutAppPrivacyBottom: -40 / 2 * scale,
			clLayoutSloganHeight: 0,
			clLayoutSloganBottom: 0
		},
		widgets: {
			widgetWx: {
				widgetId: 'customView_wx',
				type: 'Button',
				textContent: '微信登录',
				textFont: 15,
				textColor: [0.17, 0.54, 1.0, 1.0],
				cornerRadius: 16,
				masksToBounds: true,
				isFinish: true,
				clLayoutLeft: sidePad,
				clLayoutRight: -sidePad,
				clLayoutHeight: btnH,
				clLayoutBottom: -wxBottom
			}
		}
	}
}

function initSdk() {
	return new Promise((resolve, reject) => {
		const m = getSdk()
		if (!m) return reject(new Error('当前端不支持闪验一键登录'))
		if (inited) return resolve()
		try { m.setDebug && m.setDebug(true) } catch (e) {}
		try { m.setTimeOutForPreLogin && m.setTimeOutForPreLogin(6) } catch (e) {}
		try {
			m.init({ appid: getShanYanAppId() }, (res) => {
				if (isInitOk(res)) {
					inited = true
					resolve(res)
				} else {
					reject(new Error((res && (res.message || res.result)) || '闪验初始化失败'))
				}
			})
		} catch (e) {
			reject(e)
		}
	})
}

export function openOneKeyLogin(hooks = {}) {
	const { onOpen, onSuccess, onCancel, onFail, onWxLogin } = hooks
	if (opening) return Promise.resolve()
	const m = getSdk()
	if (!m) {
		onFail && onFail({ code: -1, message: '当前端不支持闪验一键登录' })
		return Promise.resolve()
	}
	opening = true
	return initSdk().then(() => {
		const os = getOS()
		if (os === 'ios') {
			openIOS(m, { onOpen, onSuccess, onCancel, onFail, onWxLogin })
		} else {
			openAndroid(m, { onOpen, onSuccess, onCancel, onFail, onWxLogin })
		}
	}).catch((err) => {
		opening = false
		onFail && onFail({ code: -1, message: (err && err.message) || '闪验初始化失败' })
	})
}

function openAndroid(m, hooks) {
	const { onOpen, onSuccess, onCancel, onFail, onWxLogin } = hooks
	const cfg = buildAndroidUIConfig()
	try {
		m.setAuthThemeConfig(cfg, (wres) => {
			// 自定义微信按钮点击
			const wid = (wres && (wres.widgetId || wres.result)) || ''
			if (String(wid).indexOf('wx') !== -1 || wres?.code === 1001) {
				try { m.finishAuthActivity && m.finishAuthActivity() } catch (e) {}
				opening = false
				onWxLogin && onWxLogin()
			}
		})
	} catch (e) {
		console.log('[shanyan] setAuthThemeConfig', e)
	}
	try {
		// true：点击一键登录自动销毁授权页
		m.openLoginAuth(true,
			(openRes) => {
				const code = openRes && openRes.code
				if (code === 1000) {
					setPrivacyAgreed(true)
					onOpen && onOpen(openRes)
					return
				}
				opening = false
				saveErr(openRes)
				onFail && onFail(openRes || { code: -1, message: '拉起授权页失败' })
			},
			(loginRes) => {
				opening = false
				const code = loginRes && loginRes.code
				if (code === 1000) {
					onSuccess && onSuccess(parseToken(loginRes.result ?? loginRes), loginRes)
					return
				}
				if (code === 1011) {
					onCancel && onCancel(loginRes)
					return
				}
				saveErr(loginRes)
				onFail && onFail(loginRes || { code: -1, message: '一键登录失败' })
			}
		)
	} catch (e) {
		opening = false
		onFail && onFail({ code: -1, message: String((e && e.message) || e) })
	}
}

function openIOS(m, hooks) {
	const { onOpen, onSuccess, onCancel, onFail, onWxLogin } = hooks
	const cfg = buildIOSUIConfig()
	try {
		m.setCustomInterface && m.setCustomInterface((wres) => {
			const wid = (wres && (wres.widgetId || JSON.stringify(wres))) || ''
			if (String(wid).indexOf('wx') !== -1) {
				try { m.finishAuthControllerCompletion && m.finishAuthControllerCompletion(() => {}) } catch (e) {}
				opening = false
				onWxLogin && onWxLogin()
			}
		})
	} catch (e) {}
	try {
		m.quickAuthLoginWithConfigure(cfg,
			(openRes) => {
				if (!openRes || !openRes.errorCode) {
					setPrivacyAgreed(true)
					onOpen && onOpen(openRes)
				} else if (openRes.code === 1011) {
					opening = false
					onCancel && onCancel(openRes)
				} else {
					opening = false
					saveErr(openRes)
					onFail && onFail({ code: openRes.code, message: openRes.message || '拉起授权页失败', raw: openRes })
				}
			},
			(loginRes) => {
				opening = false
				if (loginRes && !loginRes.errorCode && loginRes.code === 1000) {
					const token = (loginRes.data && loginRes.data.token) || parseToken(loginRes)
					onSuccess && onSuccess(token, loginRes)
					return
				}
				if (loginRes && loginRes.code === 1011) {
					onCancel && onCancel(loginRes)
					return
				}
				saveErr(loginRes)
				onFail && onFail({ code: (loginRes && loginRes.code) || -1, message: (loginRes && loginRes.message) || '一键登录失败', raw: loginRes })
			}
		)
	} catch (e) {
		opening = false
		onFail && onFail({ code: -1, message: String((e && e.message) || e) })
	}
}

function saveErr(o) {
	try {
		console.error('[shanyan] fail full:', JSON.stringify(o))
		uni.setStorageSync('shanyanLastError', JSON.stringify(o || {}))
	} catch (e) {}
}

export function finishAuthPage() {
	try {
		const m = getSdk()
		if (!m) return
		if (getOS() === 'ios') {
			m.finishAuthControllerCompletion && m.finishAuthControllerCompletion(() => {})
		} else {
			m.finishAuthActivity && m.finishAuthActivity()
		}
	} catch (e) {}
	opening = false
}
