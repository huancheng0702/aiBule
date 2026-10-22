// 闪验一键登录，写法参照创蓝闪验官方示例 pages/login/alogin.vue
import { oneKeyLogin, wxLogin } from '@/common/api/auth.js'

const shanYanSDKModule = uni.requireNativePlugin('CLSDK-ShanYanSDKModule'); //调用

//appID与包名/bundleID绑定
const SHANYAN_APPID = {
	android: 'nDHG1A3T',
	ios: 'a3dqMjNX'
}

//隐私协议链接
const PRIVACY_LINKS = {
	privacyPolicy: { title: '隐私政策', url: 'https://fx.tailunkaluo.com/h5/content.html?id=5' },
	userAgreement: { title: '用户协议', url: 'https://fx.tailunkaluo.com/h5/content.html?id=1' }
}

//自定义控件ID（微信登录）
const WX_WIDGET_ID = 'customView_wx'

let ios_uiConfigure = {}
let inited = false

export function isPrivacyAgreed() {
	return !!uni.getStorageSync('privacyAgreed')
}

export function setPrivacyAgreed(value = true) {
	uni.setStorageSync('privacyAgreed', !!value)
}

//用闪验SDK返回的token换登录态
export function exchangeLoginByShanYanToken(token) {
	return oneKeyLogin({
		token: token,
		platform: uni.getSystemInfoSync().platform
	})
}

//闪验SDK 初始化，建议在app启动时调用，即App.vue的onLaunch方法中
function init(callback) {
	let platform = uni.getSystemInfoSync().platform
	let appid
	if (platform == 'android') {
		appid = SHANYAN_APPID.android
	} else if (platform == 'ios') {
		appid = SHANYAN_APPID.ios
	}
	// shanYanSDKModule.setDebug(true)
	//闪验SDK 初始化
	shanYanSDKModule.init({
		appid: appid
	}, result => {
		console.log(JSON.stringify(result))
		callback && callback(result)
	})
}

//闪验SDK 配置授权页
function setAuthThemeConfig(hooks) {
	let platform = uni.getSystemInfoSync().platform
	if (platform == 'android') {
		let sys = uni.getSystemInfoSync()
		let width = sys.windowWidth || 375
		let side = Math.round(width * 30 / 750) //左右边距
		let buttonWidth = String(Math.round(width - side * 2)) //按钮宽度
		//闪验SDK  配置授权页方法
		shanYanSDKModule.setAuthThemeConfig({
			//授权页已有元素配置
			uiConfig: {
				setAuthBGImgPath: 'static/loginBanner.png',
				setFullScreen: false,
				setTextSizeIsdp: true,
				setStatusBarHidden: false,
				setStatusBarColor: '#ffffff',
				setLightColor: false,
				setAuthNavHidden: false,
				setNavTextColor:'#ffffff', //设置导航栏标题文字颜色
				setAuthNavTransparent: true,
				setNavReturnImgHidden: true,
				setLogoHidden: true,
				setNumberColor: '#09090D',
				setNumberSize: '28',
				setNumberBold: true,
				setNumFieldWidth: buttonWidth,
				setNumFieldOffsetBottomY: '220',
				setSloganHidden: true,
				setShanYanSloganHidden: true,
				setLogBtnText: '本机号码一键登录',
				setLogBtnTextColor: '#FFFFFF',
				setLogBtnTextSize: '16',
				setLogBtnTextBold: false,
				setLogBtnBackgroundColor: '#2C89FF',
				setLogBtnWidth: buttonWidth,
				setLogBtnHeight: '48',
				setLogBtnOffsetX: String(side),
				setLogBtnOffsetBottomY: '138',
				setPrivacyState: false,
				setPrivacySmhHidden: false,
				setPrivacyTextBold: false,
				setPrivacyNameUnderline: false,
				setPrivacyTextSize: '12',
				setAppPrivacyColor: {
					privacyBaseColor: '#666666',
					privacyColor: '#2C89FF'
				},
				setPrivacyOffsetBottomY: '14',
				setPrivacyOffsetX: String(side),
				setPrivacyOffsetGravityLeft: true,
				setPrivacyGravityHorizontalCenter: false,
				setCheckedImgPath: 'static/celiangSuccess.png',
				setUncheckedImgPath: 'static/checkCircle.png',
				setCheckBoxWH: {
					width: '18',
					height: '18'
				},
				setCheckBoxMargin: {
					marginLeft: '6',
					marginTop: '2',
					marginRight: '8',
					marginBottom: '2'
				},
				setPrivacyCustomToastText: '请先同意用户协议',
				setPrivacyText: {
					privacyTextOne: '我已阅读并同意',
					privacyTextTwo: '及健康',
					privacyTextThree: '',
					privacyTextFour: '',
					privacyTextFive: '并授权获取本机号码'
				},
				setAppPrivacyOne: {
					title: PRIVACY_LINKS.privacyPolicy.title,
					url: PRIVACY_LINKS.privacyPolicy.url
				},
				setAppPrivacyTwo: {
					title: PRIVACY_LINKS.userAgreement.title,
					url: PRIVACY_LINKS.userAgreement.url
				}
			}, //授权页添加自定义控件元素
			widgets: {
				widget1: {
					widgetId: "customView_wx",
					type: 'TextView',
					left: '160',
					bottom: '105',
					width: '',
					height: '',
					textContent: '微信登录',
					textFont: 15,
					textColor: '#2C89FF',
					backgroundColor: '#FFFFFF',
					isFinish: true
				}
			}
		}, result => {
			console.log(JSON.stringify(result))
			//自定义控件点击回调（控件ID可能在 widgetId 或 result 字段里）
			let widgetId = String((result && (result.widgetId || result.result)) || '')
			if (widgetId.indexOf(WX_WIDGET_ID) != -1) {
				wxLoginInAuthPage(hooks)
			}
		})
	} else if (platform == 'ios') { 
		//iOS 全屏模式
		ios_uiConfigure = buildIosConfigure()

		//设置自定义控件点击回调（控件ID可能在 widgetId 或 result 字段里）
		shanYanSDKModule.setCustomInterface(customInterface => {
			console.log(JSON.stringify(customInterface))
			let widgetId = String((customInterface && (customInterface.widgetId || customInterface.result)) || '')
			if (widgetId.indexOf(WX_WIDGET_ID) != -1) {
				wxLoginInAuthPage(hooks)
			}
		})
	}
}

//闪验SDK 拉起授权页
export function openOneKeyLogin(hooks = {}) {
	const open = () => {
		setAuthThemeConfig(hooks)
		let platform = uni.getSystemInfoSync().platform
		if (platform == 'android') {
			/*************************Android*************************/
			//闪验SDK  拉起授权页方法
			shanYanSDKModule.openLoginAuth(true,
				result => {
					//拉起授权页面回调
					console.log(JSON.stringify(result))
					hooks.onOpen && hooks.onOpen(result)
				},
				result => {
					//点一键登录回调
					console.log(JSON.stringify(result))
					if (result.code == 1000) {
						//SDK获取Token成功，result.result 为token的JSON字符串
						let data = result.result
						if (typeof data == 'string') {
							data = JSON.parse(data)
						}
						hooks.onSuccess && hooks.onSuccess(data.token, result)
					} else if (result.code == 1011) {
						//用户取消登录（点返回）
						hooks.onCancel && hooks.onCancel(result)
					} else {
						//处理建议：其他错误代码表示闪验通道无法继续，可以统一走开发者自己的其他登录方式
						hooks.onFail && hooks.onFail(result)
					}
				})
		} else if (platform == 'ios') {
			/*************************iOS*************************/
			//闪验SDK  拉起授权页方法
			shanYanSDKModule.quickAuthLoginWithConfigure(ios_uiConfigure,
				openLoginAuthListenerResult => {
					//拉起授权页面回调
					console.log(JSON.stringify(openLoginAuthListenerResult))
					hooks.onOpen && hooks.onOpen(openLoginAuthListenerResult)
				},
				oneKeyLoginListenerResult => {
					//点一键登录回调
					console.log(JSON.stringify(oneKeyLoginListenerResult))
					if (oneKeyLoginListenerResult.errorCode != null) {
						//iOS errorCode存在即为失败
						if (oneKeyLoginListenerResult.code == 1011) {
							//用户取消登录（点返回）
							hooks.onCancel && hooks.onCancel(oneKeyLoginListenerResult)
						} else {
							//处理建议：其他错误代码表示闪验通道无法继续，可以统一走开发者自己的其他登录方式
							hooks.onFail && hooks.onFail(oneKeyLoginListenerResult)
						}
					} else {
						//SDK获取Token成功
						let data = oneKeyLoginListenerResult.result
						if (typeof data == 'string') {
							try {
								data = JSON.parse(data)
							} catch (e) {}
						}
						let token = (data && data.token) || oneKeyLoginListenerResult.token
						hooks.onSuccess && hooks.onSuccess(token, oneKeyLoginListenerResult)
					}
				})
		}
	}
	if (inited) {
		open()
		return
	}
	init(() => {
		inited = true
		open()
	})
}

//关闭授权页
export function finishAuthPage() {
	let platform = uni.getSystemInfoSync().platform
	try {
		if (platform == 'android') {
			//部分插件版本没有 finishAuthActivity 方法，不能直接调用
			shanYanSDKModule.finishAuthActivity && shanYanSDKModule.finishAuthActivity()
		} else if (platform == 'ios') {
			shanYanSDKModule.finishAuthControllerCompletion && shanYanSDKModule.finishAuthControllerCompletion(result => {
				console.log(JSON.stringify(result))
			})
		}
	} catch (e) {
		console.log(JSON.stringify(e))
	}
}

//授权页内点击“微信登录”控件，直接发起微信登录（参考 pages/login.vue 的微信登录写法）
function wxLoginInAuthPage(hooks) {
	console.log('[shanyan] wx widget clicked')
	//先关闭授权页（控件 isFinish:true 时SDK也会自动关闭，这里兜底）
	uni.login({
		provider: 'weixin',
		onlyAuthorize: true,
		success: async (loginRes) => {
			try {
				let code = loginRes && loginRes.code
				if (!code) throw new Error('未获取到微信code')
				//获取微信昵称头像，失败不影响登录
				let nickName = ''
				let avatar = ''
				try {
					let prof = await new Promise((resolve) => {
						let timer = setTimeout(() => resolve(null), 1500)
						uni.getUserProfile({
							desc: '用于完善账号昵称头像',
							success: (r) => {
								clearTimeout(timer)
								resolve(r)
							},
							fail: () => {
								clearTimeout(timer)
								resolve(null)
							}
						})
					})
					let u = (prof && prof.userInfo) || {}
					nickName = u.nickName || ''
					avatar = u.avatarUrl || ''
				} catch (e) {}
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				let res = await wxLogin({
					code: code,
					nickName: nickName,
					avatar: avatar
				})
				uni.hideLoading()
				//登录结果交给页面处理：保存登录态、跳转
				hooks.onWxLoginSuccess && hooks.onWxLoginSuccess(res)
			} catch (e) {
				uni.hideLoading()
				let msg = (e && (e.msg || e.message)) || '微信登录失败'
				uni.showToast({
					title: String(msg).slice(0, 40),
					icon: 'none'
				})
			}
		},
		fail: (err) => {
			console.log(err)
			uni.showToast({
				title: '微信授权失败',
				icon: 'none'
			})
		}
	})
}

//iOS 授权页UI配置（参照示例的竖屏布局写法）
function buildIosConfigure() {
	let screenWidth_Portrait = plus.screen.resolutionWidth; //竖屏宽

	var screenScale = screenWidth_Portrait / 375.0; //相对iphone6屏幕
	if (screenScale > 1) {
		screenScale = 1; //大屏的无需放大
	}

	//竖屏
	let clLayoutPhoneLeft_Portrait = 30 * screenScale;
	let clLayoutPhoneRight_Portrait = -30 * screenScale;
	let clLayoutPhoneHeight_Portrait = 40 * screenScale;
	let clLayoutPhoneBottom_Portrait = -220 * screenScale;

	let clLayoutLoginBtnLeft_Portrait = 30 * screenScale;
	let clLayoutLoginBtnRight_Portrait = -30 * screenScale;
	let clLayoutLoginBtnHeight_Portrait = 48 * screenScale;
	let clLayoutLoginBtnBottom_Portrait = -138 * screenScale;

	let clLayoutAppPrivacyLeft_Portrait = 40 * screenScale;
	let clLayoutAppPrivacyRight_Portrait = -16 * screenScale;
	let clLayoutAppPrivacyHeight_Portrait = 56 * screenScale;
	let clLayoutAppPrivacyBottom_Portrait = -18 * screenScale;

	let clLayoutSloganLeft_Portrait = 0;
	let clLayoutSloganRight_Portrait = 0;
	let clLayoutSloganHeight_Portrait = 0;
	let clLayoutSloganBottom_Portrait = 0;

	//微信登录控件
	let clLayoutWxBtnLeft_Portrait = 30 * screenScale;
	let clLayoutWxBtnRight_Portrait = -30 * screenScale;
	let clLayoutWxBtnHeight_Portrait = 48 * screenScale;
	let clLayoutWxBtnBottom_Portrait = -65 * screenScale;

	return {
		clBackgroundImg: 'static/loginBanner.png',

		shouldAutorotate: false,
		/**支持方向
		 * 0:UIInterfaceOrientationMaskPortrait
		 * */
		supportedInterfaceOrientations: 0,

		clNavigationBarHidden: true,
		clNavigationBackgroundClear: true, //导航栏透明
		clNavigationBackBtnHidden: false,

		clLogoHiden: true,

		clPhoneNumberColor: [0.035, 0.035, 0.05, 1.0], //rgba
		clPhoneNumberFont: 28 * screenScale,

		clLoginBtnText: '本机号码一键登录', //一键登录按钮文字
		clLoginBtnTextColor: [1, 1, 1, 1.0], //rgba
		clLoginBtnBgColor: [0.17, 0.54, 1.0, 1.0], //rgba
		clLoginBtnTextFont: 15 * screenScale,
		clLoginBtnCornerRadius: 16 * screenScale,


		clCheckBoxHidden: false,
		clCheckBoxValue: false,
		clCheckBoxVerticalAlignmentToAppPrivacyCenterY:true,
		clSloganTextHidden:false,
		clCheckBoxSize: [30 * screenScale, 30 * screenScale], //2 item, width and height
		// clCheckBoxSize: [40, 40],
		clCheckBoxUncheckedImage: 'static/checkCircle.png',
		clCheckBoxCheckedImage: 'static/celiangSuccess.png',
		clCheckBoxImageEdgeInsets:[5,5,5,5],
		clAppPrivacyColor: [
			[0.4, 0.4, 0.4, 1.0],
			[0.17, 0.54, 1.0, 1.0]
		], //2 item,commomTextColor and appPrivacyTextColor
		clAppPrivacyTextFont: 11 * screenScale,
		clAppPrivacyTextAlignment:1, //0: center 1: left 2: right
		clAppPrivacyFirst: [PRIVACY_LINKS.privacyPolicy.title, PRIVACY_LINKS.privacyPolicy.url], // 2 item, name and url
		clAppPrivacySecond: [PRIVACY_LINKS.userAgreement.title, PRIVACY_LINKS.userAgreement.url], // 2 item, name and url
		clAppPrivacyNormalDesTextFirst: '我已阅读并同意',
		clAppPrivacyNormalDesTextSecond: '及',
		clAppPrivacyNormalDesTextThird: '',
		clAppPrivacyNormalDesTextFourth: '并授权获取本机号码',

		//竖屏布局对象
		clOrientationLayOutPortrait: {
			//控件
			clLayoutPhoneLeft: clLayoutPhoneLeft_Portrait,
			clLayoutPhoneRight: clLayoutPhoneRight_Portrait,
			clLayoutPhoneHeight: clLayoutPhoneHeight_Portrait,
			clLayoutPhoneBottom: clLayoutPhoneBottom_Portrait,

			clLayoutLoginBtnLeft: clLayoutLoginBtnLeft_Portrait,
			clLayoutLoginBtnRight: clLayoutLoginBtnRight_Portrait,
			clLayoutLoginBtnHeight: clLayoutLoginBtnHeight_Portrait,
			clLayoutLoginBtnBottom: clLayoutLoginBtnBottom_Portrait,

			clLayoutAppPrivacyLeft: clLayoutAppPrivacyLeft_Portrait,
			clLayoutAppPrivacyRight: clLayoutAppPrivacyRight_Portrait,
			clLayoutAppPrivacyBottom: clLayoutAppPrivacyBottom_Portrait,
			clLayoutAppPrivacyHeight: clLayoutAppPrivacyHeight_Portrait,

			clLayoutSloganLeft: clLayoutSloganLeft_Portrait,
			clLayoutSloganRight: clLayoutSloganRight_Portrait,
			clLayoutSloganHeight: clLayoutSloganHeight_Portrait,
			clLayoutSloganBottom: clLayoutSloganBottom_Portrait
		},
		//自定义控件
		widgets: {
			widgetWx: {
				widgetId: WX_WIDGET_ID, //字符标记
				type: 'Button', // 类型：Button：按钮，ImageView:图片 TextView:文本
				textContent: '微信登录', //文字
				textFont: 15 * screenScale, //字体
				textColor: [0.17, 0.54, 1.0, 1.0], //文字颜色[r,g,b,a]
				backgroundColor: [1, 1, 1, 1.0], //控件背景色[r,g,b,a]
				cornerRadius: 16 * screenScale, //圆角，设置圆角时请设置masksToBounds:true
				masksToBounds: true, //圆角相关
				isFinish: true, //点击销毁授权页

				clLayoutLeft: clLayoutWxBtnLeft_Portrait,
				clLayoutRight: clLayoutWxBtnRight_Portrait,
				clLayoutHeight: clLayoutWxBtnHeight_Portrait,
				clLayoutBottom: clLayoutWxBtnBottom_Portrait
			}
		}
	}
}
