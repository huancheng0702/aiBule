/**
 * 创蓝闪验一键登录封装
 * 文档：https://ext.dcloud.net.cn/plugin?id=23914
 *
 * 使用前请替换 SHANYAN_APP_ID，并准备服务端置换手机号接口。
 */

import { oneKeyLogin } from '@/common/api/auth.js'

// #ifdef APP-PLUS
import { ShanYanSDKModule } from '@/uni_modules/cl-shanyan'
// #endif

// 创蓝控制台申请的 appId（iOS / Android 各自独立）
// 注意：appKey 只配在服务端（置换手机号时用），不要写进 App 端
export const SHANYAN_APP_IDS = {
	ios: 'a3dqMjNX',
	android: 'nDHG1A3T'
}

// 兼容旧引用：默认取 Android
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
		if (osName.indexOf('ios') !== -1 || osName.indexOf('iphone') !== -1) {
			return SHANYAN_APP_IDS.ios
		}
	} catch (e) {}
	return SHANYAN_APP_IDS.android
}

// TODO: 替换为你们真实的用户协议 / 数据处理说明地址
export const PRIVACY_LINKS = {
	userAgreement: {
		name: '《用户协议与隐私政策》',
		url: 'https://www.example.com/user-agreement'
	},
	dataProcess: {
		name: '《数据处理说明》',
		url: 'https://www.example.com/data-process'
	}
}

const STORAGE_PRIVACY = 'privacyAgreed'

// Android 自定义控件（微信登录按钮）点击回调：提为模块级具名函数，
// 避免内联闭包在 UTS 反射调用时报 IllegalArgumentException: method... not found
let wxWidgetHandler = null
function handleWxWidgetClick() {
	if (typeof wxWidgetHandler === 'function') wxWidgetHandler()
}

let sdk = null
let inited = false
let opening = false

function getSdk() {
	// #ifdef APP-PLUS
	if (!sdk) {
		sdk = new ShanYanSDKModule()
	}
	return sdk
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

/**
 * 置换业务登录态：闪验 token -> 服务端 oneKeyLogin
 * 后端新版要求同时传 platform（ios/android），用于匹配对应 appId/appKey
 */
export function exchangeLoginByShanYanToken(shanyanToken) {
	let platform = 'android'
	try {
		const sys = uni.getSystemInfoSync() || {}
		const osName = String(sys.osName || sys.platform || '').toLowerCase()
		const ua = typeof navigator !== 'undefined' ? String(navigator.userAgent || '').toLowerCase() : ''
		if (osName.indexOf('ios') !== -1 || osName.indexOf('iphone') !== -1 || ua.indexOf('iphone') !== -1 || ua.indexOf('ipad') !== -1) {
			platform = 'ios'
		}
	} catch (e) {}
	// #ifdef APP-PLUS
	try {
		if (typeof plus !== 'undefined' && plus.os && plus.os.name) {
			platform = String(plus.os.name).toLowerCase() === 'ios' ? 'ios' : 'android'
		}
	} catch (e) {}
	// #endif
	return oneKeyLogin({
		token: shanyanToken,
		platform
	})
}

function rpx2dp(rpx) {
	// 设计稿 750rpx ≈ 屏幕逻辑宽度；闪验 Android 侧会再按 dp 转 px
	return Math.round(rpx / 2)
}

/**
 * 授权页 UI：对齐 pages/login.vue 底部登录区
 * 原生资源（SDK 读不到 /static，必须放 nativeResources）：
 * - Android: nativeResources/android/res/drawable-nodpi/{login_bg,btn_ban,check_circle_fil,check_circle}.png
 * - iOS: nativeResources/ios/ 同名 .png（引用时带 .png 后缀）
 * 来源映射：login_bg=static/loginBanner.png，btn_ban=static/btnBan.png，
 * check_circle_fil=static/celiangSuccess.png（选中），check_circle=static/checkCircle.png（未选中）
 * 注意：资源名必须全小写字母/数字/下划线，缺失或带连字符都会导致 Android IllegalArgumentException 闪退
 */
export function buildShanYanUIConfig(options = {}) {
	const { onWxLogin } = options
	wxWidgetHandler = (typeof onWxLogin === 'function') ? onWxLogin : null
	const sys = uni.getSystemInfoSync()
	const screenWidth = sys.screenWidth
	const btnWidth = Math.floor(screenWidth - rpx2dp(60))
	const btnHeight = rpx2dp(96)
	const sidePad = rpx2dp(30)

	// 自下而上：协议 → 微信 → 一键登录 → 号码（单位 dp，对齐现页间距）
	const privacyBottom = rpx2dp(40)
	const wxBottom = rpx2dp(40 + 80 + 30)
	const loginBottom = rpx2dp(40 + 80 + 30 + 96 + 30)
	const numberBottom = rpx2dp(40 + 80 + 30 + 96 + 30 + 96 + 40)

	// Android 自定义微信按钮已下掉：实测 uni-app JS 侧传 callback 进 UTS WidgetItem
	// 在 Android 会被装箱成 Integer（argument 17 Function0 mismatch），反复闪退。
	// 授权页只保留「本机号码一键登录」；要走微信时点返回键关闭授权页，
	// onCancel 会切到登录页兜底 UI，那里有点“微信登录”入口。
	const androidWidgets = []

	// iOS 自定义控件：iOS 不走 callback（走 setCallEventUIBlock），只保留 iOS 专属边框字段
	const iosWidgets = [
		{
			type: 'Button',
			width: btnWidth,
			height: btnHeight,
			leftOffsetX: sidePad,
			bottomOffsetY: -wxBottom,
			textContent: '微信登录',
			textColor: '#2C89FF',
			textFont: 15,
			backgroundColor: '#00000000',
			cornerRadius: 16,
			borderColor: '#2C89FF',
			borderWidth: 1,
			isFinish: true
		}
	]

	const androidConfig = {
		verticalConfig: {
			baseUIConfig: {
				authBGImgPath: 'login_bg',
				statusBarConfig: {
					isLightColor: false,
					statusBarColor: '#00000000'
				},
				systemNavBarConfig: {
					isBackPressedAvailable: true,
					isFullScreen: false
				},
				navBarConfig: {
					authNavHidden: true,
					authNavTransparent: true,
					navReturnImgHidden: true
				},
				logoConfig: {
					logoHidden: true
				},
				numberConfig: {
					numberColor: '#09090D',
					isNumberBold: true,
					numberSize: 28,
					numFieldOffsetBottomY: numberBottom
				},
				sloganConfig: {
					sloganHidden: true
				},
				loginButtonConfig: {
					logBtnText: '本机号码一键登录',
					logBtnTextColor: '#FFFFFF',
					logBtnTextSize: 15,
					logBtnWidth: btnWidth,
					logBtnHeight: btnHeight,
					logBtnOffsetX: sidePad,
					logBtnOffsetBottomY: loginBottom,
					logBtnBackgroundPath: 'btn_ban',
					logBtnBackgroundColor: '#2C89FF',
					isFinishLoginPage: true
				},
				privacyConfig: {
					privacyState: false,
					privacyTextSize: 12,
					privacyBaseColor: '#666666',
					privacyNameColor: '#2C89FF',
					privacyOffsetBottomY: privacyBottom,
					privacyOffsetX: sidePad,
					privacyWidth: btnWidth,
					privacyOffsetGravityLeft: true,
					privacyGravityHorizontalCenter: false,
					checkboxWidth: 18,
					checkboxHeight: 18,
					checkedImgPath: 'check_circle_fil',
					uncheckedImgPath: 'check_circle',
					toastText: '请先同意用户协议',
					privacyText: {
						privacyTextHead: '我已阅读并同意',
						privacyTextFirst: '及健康',
						privacyTextEnd: '并授权获取本机号码'
					},
					morePrivacy: [
						{
							name: PRIVACY_LINKS.userAgreement.name,
							url: PRIVACY_LINKS.userAgreement.url,
							content: '',
							color: '#2C89FF'
						},
						{
							name: PRIVACY_LINKS.dataProcess.name,
							url: PRIVACY_LINKS.dataProcess.url,
							content: '',
							color: '#2C89FF'
						}
					]
				}
			},
			widgetsConfig: androidWidgets
		}
	}

	const iOSConfig = {
		clNavigationBarHidden: 1,
		clNavigationBackgroundClear: 1,
		clNavigationBackBtnHidden: 1,
		clBackgroundImg: 'login_bg.png',
		clLogoHiden: 1,
		clPhoneNumberColor: '#09090D',
		clPhoneNumberFontSize: 28,
		clPhoneNumberFontFamily: 'PingFangSC-Semibold',
		clLoginBtnText: '本机号码一键登录',
		clLoginBtnTextColor: '#FFFFFF',
		clLoginBtnTextFontSize: 15,
		clLoginBtnNormalBgImage: 'btn_ban.png',
		clLoginBtnCornerRadius: 16,
		clSloganTextHidden: 1,
		clShanYanSloganTextHidden: 1,
		clCheckBoxHidden: 0,
		clCheckBoxValue: 0,
		clCheckBoxSize: { width: 18, height: 18 },
		clCheckBoxCheckedImage: 'check_circle_fil.png',
		clCheckBoxUncheckedImage: 'check_circle.png',
		clCheckBoxTipMsg: '请先同意用户协议',
		clAppPrivacyNormalDesTextFirst: '我已阅读并同意',
		clAppPrivacyNormalDesTextLast: '并授权获取本机号码',
		clAppPrivacyTextFontSize: 12,
		clAppPrivacyTextColor: ['#666666', '#2C89FF'],
		clAppPrivacyPunctuationMarks: 0,
		clAppMorePrivacyArray: [
			{
				decollator: '',
				lastDecollator: 'NO',
				privacyName: PRIVACY_LINKS.userAgreement.name,
				privacyURL: PRIVACY_LINKS.userAgreement.url
			},
			{
				decollator: '及健康',
				lastDecollator: 'NO',
				privacyName: PRIVACY_LINKS.dataProcess.name,
				privacyURL: PRIVACY_LINKS.dataProcess.url
			}
		],
		clOrientationLayOutPortrait: {
			clPhoneLayout: {
				centerX: 0,
				width: btnWidth,
				height: 40,
				bottom: -numberBottom
			},
			clLoginBtnLayout: {
				left: sidePad,
				right: -sidePad,
				height: btnHeight,
				bottom: -loginBottom
			},
			clPrivacyLayout: {
				left: sidePad,
				right: -sidePad,
				height: 50,
				bottom: -privacyBottom
			},
			clSloganLayout: {
				height: 0,
				bottom: 0
			},
			clShanSloganLayout: {
				height: 0,
				bottom: 0
			},
			clLogonLayout: {
				width: 0,
				height: 0,
				top: 0
			}
		},
		widgetsConfig: iosWidgets
	}

	return {
		androidConfig,
		iOSConfig
	}
}

function initSdk() {
	return new Promise((resolve, reject) => {
		const module = getSdk()
		if (!module) {
			reject(new Error('当前端不支持闪验一键登录'))
			return
		}
		if (inited) {
			resolve()
			return
		}
		try {
			module.setDebug(true)
			module.setTimeOutForPreLogin(6)
		} catch (e) {
			console.log('[shanyan] setDebug/timeout', e)
		}
		module.initWithAppId(getShanYanAppId(), (response) => {
			const code = response && response.code
			if (code === 1000) {
				inited = true
				resolve(response)
				return
			}
			reject(new Error((response && (response.message || response.innerDesc)) || '闪验初始化失败'))
		})
	})
}

/**
 * 拉起授权页（启动即登录场景：不做预取号，避免间隔过短异常）
 */
export function openOneKeyLogin(hooks = {}) {
	const {
		onOpen,
		onSuccess,
		onCancel,
		onFail,
		onWxLogin
	} = hooks

	if (opening) {
		return Promise.resolve()
	}

	const module = getSdk()
	if (!module) {
		onFail && onFail({ code: -1, message: '当前端不支持闪验一键登录' })
		return Promise.resolve()
	}

	opening = true
	return initSdk()
		.then(() => {
			const uiConfig = buildShanYanUIConfig({ onWxLogin })
			// iOS 自定义控件点击
			try {
				if (typeof module.setCallEventUIBlock === 'function') {
					module.setCallEventUIBlock(() => {
						if (typeof onWxLogin === 'function') onWxLogin()
					})
				}
			} catch (e) {
				console.log('[shanyan] setCallEventUIBlock', e)
			}

			module.quickAuthLoginWithConfigure(
				uiConfig,
				(openRes) => {
					const code = openRes && openRes.code
					if (code === 1000) {
						setPrivacyAgreed(true)
						onOpen && onOpen(openRes)
						return
					}
					opening = false
					try {
						console.error('[shanyan] openAuthPage fail full:', JSON.stringify(openRes))
					} catch (e) {
						console.error('[shanyan] openAuthPage fail:', openRes)
					}
					try {
						uni.setStorageSync('shanyanLastError', JSON.stringify(openRes || {}))
					} catch (e) {}
					onFail && onFail(openRes || { code: -1, message: '拉起授权页失败' })
				},
				(loginRes) => {
					opening = false
					const code = loginRes && loginRes.code
					if (code === 1000) {
						const token = loginRes.data && loginRes.data.token
						onSuccess && onSuccess(token, loginRes)
						return
					}
					if (code === 1011) {
						onCancel && onCancel(loginRes)
						return
					}
					try {
						console.error('[shanyan] oneKeyLogin fail full:', JSON.stringify(loginRes))
					} catch (e) {
						console.error('[shanyan] oneKeyLogin fail:', loginRes)
					}
					try {
						uni.setStorageSync('shanyanLastError', JSON.stringify(loginRes || {}))
					} catch (e) {}
					onFail && onFail(loginRes || { code: -1, message: '一键登录失败' })
				}
			)
		})
		.catch((err) => {
			opening = false
			onFail && onFail({
				code: -1,
				message: (err && err.message) || '闪验初始化失败'
			})
		})
}

export function finishAuthPage() {
	try {
		const module = getSdk()
		if (module && typeof module.finishAuthActivity === 'function') {
			module.finishAuthActivity()
		}
	} catch (e) {
		console.log('[shanyan] finishAuthActivity', e)
	}
	opening = false
}
