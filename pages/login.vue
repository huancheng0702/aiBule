<template>
	<view class="content">
		<view class="container">
			<view v-if="!shanyanOpened" class="oneBtn" @click="onRetryShanYan">
				<image src="/static/btnBan.png" class="benBan" mode=""></image>
				<view class="btntext">
					本机号码一键登录
				</view>
			</view>
			<view class="oneBtn" @click="onOpenSmsLogin">
				<image src="/static/btnBan.png" class="benBan" mode=""></image>
				<view class="btntext">
					验证码登录
				</view>
			</view>
			<view class="wxLogin" @click="onWxLogin">
				微信登录 
			</view>
			<view class="agreement">
				<image @click.stop="agreed = !agreed"
					:src="agreed ? '/static/check-circle-fil.png' : '/static/checkCircle.png'" class="agreImg" mode="">
				</image>
				<view class="agreText">
					我已阅读并同意 <text class="agreLink" @click="openContent(5)">《隐私政策》</text>及健康 <text class="agreLink"
						@click="openContent(1)">《用户协议》</text>
				</view>
			</view>
		</view>
		<image v-for="(src, index) in banners" :key="src" :src="src" class="banner"
			:class="{ active: currentBanner === index }" mode="aspectFill"></image>
		<!-- 授权页失败 / 非 App / 用户返回时，展示与授权页一致的兜底 UI -->


		<u-popup :show="showSmsLogin" mode="bottom" round="24" :safeAreaInsetBottom="true" @close="closeSmsLogin">
			<view class="sms-sheet">
				<view class="sms-title">手机验证码登录</view>
				<view class="sms-sub">未注册手机号验证后将自动创建账号</view>
				<view class="sms-field">
					<input v-model="smsPhone" type="number" maxlength="11" placeholder="请输入手机号"
						placeholder-class="sms-placeholder" />
				</view>
				<view class="sms-field sms-code-row">
					<input v-model="smsCode" type="number" maxlength="6" placeholder="请输入验证码"
						placeholder-class="sms-placeholder" />
					<view class="sms-send" :class="{ disabled: smsCountdown > 0 || smsSending }" @click="onSendSms">
						{{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
					</view>
				</view>
				<view class="sms-submit" @click="onSmsLogin">
					<image src="/static/btnBan.png" class="sms-submit-bg" mode="scaleToFill"></image>
					<text class="sms-submit-text">登录</text>
				</view>
			</view>
		</u-popup>
	</view>
</template>
 
<script>
	import {
		openOneKeyLogin,
		finishAuthPage,
		exchangeLoginByShanYanToken,
		isPrivacyAgreed,
		setPrivacyAgreed
	} from '@/common/shanyan-auth.js'
	import {
		sendSmsCode,
		phoneLogin,
		wxLogin
	} from '@/common/api/auth.js'
	import {
		getUserInfo
	} from '@/common/api/user.js'
	import {
		saveLoginSession
	} from '@/common/auth.js'

	const PHONE_REG = /^1\d{10}$/

	export default {
		data() {
			return {
				banners: [
					'/static/loginBanner.png',
					'/static/loginbg.png'
				],
				currentBanner: 0,
				bannerTimer: null,
				agreed: false,
				showFallback: false,
				maskPhone: '',
				launching: false,
				shanyanOpened: false,
				shanyanRetryCount: 0,
				showSmsLogin: false,
				smsPhone: '',
				smsCode: '',
				smsCountdown: 0,
				smsTimer: null,
				smsSending: false,
				smsLogging: false
			}
		},
		onLoad() {
			if (uni.getStorageSync('token')) {
				this.afterLogin()
				return
			}
			this.agreed = isPrivacyAgreed()
			this.startBannerSwitch()
			// #ifdef APP-PLUS
			this.$nextTick(() => {
				this.openShanYanAuth()
			})
			try {
				uni.onNetworkStatusChange((res) => {
					if (res && res.isConnected && !this.shanyanOpened && !this.launching) {
						this.shanyanRetryCount = (this.shanyanRetryCount || 0) + 1
						if (this.shanyanRetryCount <= 3) {
							setTimeout(() => this.openShanYanAuth(), 800)
						}
					}
				})
			} catch (e) {}
			// #endif
			// #ifndef APP-PLUS
			this.showFallback = true
			// #endif
		},
		onShow() {
			// #ifdef APP-PLUS
			if (!this.shanyanOpened && !this.launching && !uni.getStorageSync('token')) {
				this.shanyanRetryCount = (this.shanyanRetryCount || 0) + 1
				if (this.shanyanRetryCount <= 5) {
					this.openShanYanAuth()
				}
			}
			// #endif
		},
		onUnload() {
			this.clearBannerSwitch()
			this.clearSmsTimer()
		},
		methods: {
			startBannerSwitch() {
				this.clearBannerSwitch()
				this.bannerTimer = setInterval(() => {
					this.currentBanner = (this.currentBanner + 1) % this.banners.length
				}, 5000)
			},
			clearBannerSwitch() {
				if (this.bannerTimer) {
					clearInterval(this.bannerTimer)
					this.bannerTimer = null
				}
			},
			clearSmsTimer() {
				if (this.smsTimer) {
					clearInterval(this.smsTimer)
					this.smsTimer = null
				}
			},
			ensureAgree() {
				if (this.agreed) return true
				uni.showToast({
					title: '请先同意用户协议',
					icon: 'none'
				})
				return false
			},
			openContent(contentId) {
				uni.navigateTo({
					url: '/pages/webAgreement?contentId=' + contentId
				})
			},
			onOpenSmsLogin() {
				if (!this.ensureAgree()) return
				setPrivacyAgreed(true)
				this.showSmsLogin = true
			},
			closeSmsLogin() {
				this.showSmsLogin = false
			},
			startSmsCountdown() {
				this.clearSmsTimer()
				this.smsCountdown = 60
				this.smsTimer = setInterval(() => {
					if (this.smsCountdown <= 1) {
						this.clearSmsTimer()
						this.smsCountdown = 0
						return
					}
					this.smsCountdown -= 1
				}, 1000)
			},
			async onSendSms() {
				if (this.smsCountdown > 0 || this.smsSending) return
				if (!PHONE_REG.test(this.smsPhone)) {
					uni.showToast({
						title: '请输入正确手机号',
						icon: 'none'
					})
					return
				}
				this.smsSending = true
				try {
					await sendSmsCode({
						phone: this.smsPhone,
						scene: 'login'
					})
					this.startSmsCountdown()
					uni.showToast({
						title: '验证码已发送',
						icon: 'none'
					})
				} catch (e) {
					const msg = (e && (e.msg || e.message)) || ''
					if (!msg) {
						uni.showToast({
							title: '验证码发送失败',
							icon: 'none'
						})
					}
				} finally {
					this.smsSending = false
				}
			},
			async onSmsLogin() {
				if (this.smsLogging) return
				if (!PHONE_REG.test(this.smsPhone)) {
					uni.showToast({
						title: '请输入正确手机号',
						icon: 'none'
					})
					return
				}
				if (!this.smsCode) {
					uni.showToast({
						title: '请输入验证码',
						icon: 'none'
					})
					return
				}
				this.smsLogging = true
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				try {
					const res = await phoneLogin({
						phone: this.smsPhone,
						code: this.smsCode
					})
					const loginData = await this.applyLoginResult(res, 'phone')
					this.showSmsLogin = false
					this.afterLogin(loginData)
				} catch (e) {
					const msg = (e && (e.msg || e.message)) || ''
					if (!msg) {
						uni.showToast({
							title: '登录失败',
							icon: 'none'
						})
					}
				} finally {
					this.smsLogging = false
					uni.hideLoading()
				}
			},
			async applyLoginResult(res, loginType) {
				const loginData = (res && res.data) || res || {}
				saveLoginSession(loginData || {}, loginType)
				try {
					const info = await getUserInfo()
					const infoData = (info && info.data) || info || {}
					// 用户信息接口一般不带 phoneBound / needCompleteInfo，避免覆盖登录返回的状态
					if (infoData && loginData && loginData.phoneBound !== undefined && infoData.phoneBound ===
						undefined) {
						infoData.phoneBound = loginData.phoneBound
					}
					if (infoData && loginData && loginData.needCompleteInfo !== undefined && infoData.needCompleteInfo ===
						undefined) {
						infoData.needCompleteInfo = loginData.needCompleteInfo
					}
					saveLoginSession(infoData || {}, loginType)
				} catch (e) {}
				return loginData
			},
			onWxLogin() {
				if (this.showFallback && !this.ensureAgree()) return
				setPrivacyAgreed(true)
				this.doWxLogin()
			},
			openShanYanAuth() {
				if (this.launching || this.shanyanOpened) return
				this.launching = true
				this.showFallback = true
				openOneKeyLogin({
					onWxLogin: () => {
						finishAuthPage()
						this.launching = false
						this.showFallback = true
						this.doWxLogin()
					},
					onOpen: () => {
						this.launching = false
						this.shanyanOpened = true
					},
					onSuccess: (shanyanToken) => {
						this.launching = false
						this.shanyanOpened = true
						this.handleShanYanSuccess(shanyanToken)
					},
					onCancel: () => {
						this.launching = false
						this.showFallback = true
					},
					onFail: (err) => {
						this.launching = false
						this.showFallback = true
						try {
							console.log('[shanyan] onFail silent:', JSON.stringify(err))
						} catch (e) {
							console.log('[shanyan] onFail silent')
						}
						const msg = String((err && (err.message || err.msg)) || '')
						if (/网络|network|timeout|超时|1022|1015|1001/i.test(msg) && (this.shanyanRetryCount || 0) < 3) {
							this.shanyanRetryCount = (this.shanyanRetryCount || 0) + 1
							setTimeout(() => {
								if (!this.shanyanOpened && !this.launching) this.openShanYanAuth()
							}, 2000)
						}
					}
				})
			},
			onRetryShanYan() {
				this.shanyanRetryCount = 0
				this.openShanYanAuth()
			},
			async handleShanYanSuccess(shanyanToken) {
				if (!shanyanToken) {
					this.showFallback = true
					return
				}
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				try {
					// 新版 POST /appApi/auth/oneKeyLogin：{ token, platform: ios/android } -> WxLoginVO{ token,userId,nickName,avatar,phoneBound }
					// token 为闪验 SDK 返回的一次性运营商 token
					const loginData = await exchangeLoginByShanYanToken(shanyanToken)
					await this.applyLoginResult(loginData, 'phone')
					finishAuthPage()
					this.afterLogin(loginData)
				} catch (e) {
					finishAuthPage()
					this.showFallback = true
					console.log('[shanyan] exchange fail silent:', (e && (e.msg || e.message)) || e)
				} finally {
					uni.hideLoading()
				}
			},
			doWxLogin() {
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				uni.login({
					provider: 'weixin',
					success: async (loginRes) => {
						try {
							const code = loginRes && loginRes.code
							if (!code) throw new Error('未获取到微信code')
							let nickName = ''
							let avatar = ''
							try {
								const prof = await new Promise((resolve) => {
									const timer = setTimeout(() => resolve(null), 1500)
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
								const u = (prof && prof.userInfo) || {}
								nickName = u.nickName || ''
								avatar = u.avatarUrl || ''
							} catch (e) {}
							const res = await wxLogin({ code, nickName, avatar })
							const loginData = await this.applyLoginResult(res, 'wechat')
							uni.hideLoading()
							this.afterLogin(loginData)
						} catch (e) {
							uni.hideLoading()
							const msg = (e && (e.msg || e.message)) || '微信登录失败'
							uni.showToast({ title: String(msg).slice(0, 40), icon: 'none' })
						}
					},
					fail: () => {
						uni.hideLoading()
						uni.showToast({ title: '微信授权失败', icon: 'none' })
					}
				})
			},
			goBindPhone(loginData) {
				const data = (loginData && loginData.data) || loginData || {}
				const uid = data.userId || data.id || ''
				const need = data.needCompleteInfo === true
				uni.showToast({ title: '请绑定手机号', icon: 'none' })
				setTimeout(() => {
					uni.navigateTo({ url: '/pages/mine/bindPhone?from=login' + (uid ? '&userId=' + uid : '') + (need ? '&needCompleteInfo=1' : '') })
				}, 400)
			},
			goProfile() {
				uni.reLaunch({ url: '/pages/mine/myInfo' })
			},
			afterLogin(loginData) {
				const data = (loginData && loginData.data) || loginData || {}
				if (data.phoneBound === false) {
					this.showFallback = true
					this.goBindPhone(data)
					return
				}
				if (data.needCompleteInfo === true) {
					this.goProfile()
					return
				}
				uni.reLaunch({
					url: '/pages/index/index'
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.content {
		position: relative;
		width: 100%;
		height: 100vh;

		.banner {
			position: absolute;
			top: 0;
			left: 0;
			width: 100%;
			height: 100%;
			opacity: 0;
			transition: opacity 0.8s ease;
			z-index: 1;

			&.active {
				opacity: 1;
			}
		}

		.container {
			width: 100%;
			position: absolute;
			text-align: center;
			bottom: 60rpx;
			left: 0;
			padding: 0 30rpx;
			box-sizing: border-box;
			z-index: 999;

			.conPhone {
				font-size: 56rpx;
				font-weight: bold;
				color: #09090D;
			}

			.oneBtn {
				width: 100%;
				height: 96rpx;
				text-align: center;
				position: relative;
				margin-top: 40rpx;

				image {
					width: 100%;
					height: 100%;
				}

				.btntext {
					width: 100%;
					height: 100%;
					text-align: center;
					line-height: 96rpx;
					position: absolute;
					top: 0;
					left: 0;
					color: #fff;
					font-size: 30rpx;
				}
			}

			.wxLogin {
				width: 100%;
				height: 96rpx;
				border: 1px solid #2C89FF;
				color: #2C89FF;
				text-align: center;
				line-height: 96rpx;
				border-radius: 32rpx;
				margin: 30rpx 0 20rpx;
				box-sizing: border-box;
			}

			.smsEntry {
				width: 100%;
				height: 80rpx;
				color: #2C89FF;
				text-align: center;
				line-height: 80rpx;
				font-size: 28rpx;
				margin-bottom: 20rpx;
			}
		}
	}

	.agreement {
		display: flex;
		justify-content: flex-start;
		align-items: flex-start;

		image {
			width: 36rpx;
			height: 36rpx;
			margin-right: 15rpx;
		}

		.agreText {
			font-size: 24rpx;
			color: #666;
			flex: 1;
			text-align: left;

			text {
				color: #2C89FF;
			}

			.agreLink {
				padding: 10rpx 6rpx;
			}
		}
	}

	.sms-sheet {
		padding: 40rpx 32rpx 48rpx;

		.sms-title {
			font-size: 36rpx;
			font-weight: bold;
			color: #09090D;
			text-align: center;
		}

		.sms-sub {
			margin-top: 12rpx;
			font-size: 24rpx;
			color: #999;
			text-align: center;
		}

		.sms-field {
			margin-top: 28rpx;
			height: 96rpx;
			background: #F5F6F8;
			border-radius: 24rpx;
			padding: 0 28rpx;
			display: flex;
			align-items: center;

			input {
				flex: 1;
				height: 96rpx;
				font-size: 30rpx;
				color: #09090D;
			}
		}

		.sms-code-row {
			.sms-send {
				flex-shrink: 0;
				padding-left: 20rpx;
				font-size: 26rpx;
				color: #2C89FF;

				&.disabled {
					color: #999;
				}
			}
		}

		.sms-submit {
			margin-top: 40rpx;
			width: 100%;
			height: 96rpx;
			position: relative;

			.sms-submit-bg {
				width: 100%;
				height: 100%;
			}

			.sms-submit-text {
				position: absolute;
				left: 0;
				top: 0;
				width: 100%;
				height: 96rpx;
				line-height: 96rpx;
				text-align: center;
				color: #fff;
				font-size: 30rpx;
			}
		}
	}

	.sms-placeholder {
		color: #B8B8B8;
	}
</style>