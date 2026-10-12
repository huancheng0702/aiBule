<template>
	<view class="content">
		<image
			v-for="(src, index) in banners"
			:key="src"
			:src="src"
			class="banner"
			:class="{ active: currentBanner === index }"
			mode="aspectFill"
		></image>
		<!-- 授权页失败 / 非 App / 用户返回时，展示与授权页一致的兜底 UI -->
		<view v-if="showFallback" class="container">
		<!-- 	<view class="conPhone">
				{{ maskPhone || '本机号码' }}
			</view> -->
			<view class="oneBtn" @click="onOpenSmsLogin">
				<image src="/static/btnBan.png" class="benBan" mode=""></image>
				<view class="btntext">
					验证码登录
				</view>
			</view>
		<!-- 	<view class="oneBtn" @click="onPhoneLogin">
				<image src="/static/btnBan.png" class="benBan" mode=""></image>
				<view class="btntext">
					本机号码一键登录
				</view>
			</view> -->
			<view class="wxLogin" @click="onWxLogin">
				微信登录
			</view>
		<!-- 	<view class="smsEntry" @click="onOpenSmsLogin">
				验证码登录
			</view> -->
			<view class="agreement" @click="agreed = !agreed">
				<image :src="agreed ? '/static/check-circle-fil.png' : '/static/checkCircle.png'" class="agreImg" mode=""></image>
				<view class="agreText">
					我已阅读并同意 <text>《用户协议与隐私政策》</text>及健康 <text>《数据处理说明》</text>
				</view>
			</view>
		</view>

		<u-popup :show="showSmsLogin" mode="bottom" round="24" :safeAreaInsetBottom="true" @close="closeSmsLogin">
			<view class="sms-sheet">
				<view class="sms-title">手机验证码登录</view>
				<view class="sms-sub">未注册手机号验证后将自动创建账号</view>
				<view class="sms-field">
					<input
						v-model="smsPhone"
						type="number"
						maxlength="11"
						placeholder="请输入手机号"
						placeholder-class="sms-placeholder"
					/>
				</view>
				<view class="sms-field sms-code-row">
					<input
						v-model="smsCode"
						type="number"
						maxlength="6"
						placeholder="请输入验证码"
						placeholder-class="sms-placeholder"
					/>
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
	import { sendSmsCode, phoneLogin } from '@/common/api/auth.js'
	import { getUserInfo } from '@/common/api/user.js'
	import { saveLoginSession } from '@/common/auth.js'

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
			// #endif
			// #ifndef APP-PLUS
			this.showFallback = true
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
			onPhoneLogin() {
				if (!this.ensureAgree()) return
				setPrivacyAgreed(true)
				// #ifdef APP-PLUS
				this.openShanYanAuth()
				// #endif
				// #ifndef APP-PLUS
				this.showSmsLogin = true
				// #endif
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
					// 用户信息接口一般不带 phoneBound，避免覆盖一键登录返回的绑定态
					if (infoData && loginData && loginData.phoneBound !== undefined && infoData.phoneBound === undefined) {
						infoData.phoneBound = loginData.phoneBound
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
				if (this.launching) return
				this.launching = true
				this.showFallback = false
				uni.showLoading({
					title: '正在唤起...',
					mask: true
				})
				openOneKeyLogin({
					onWxLogin: () => {
						finishAuthPage()
						this.launching = false
						this.showFallback = true
						this.doWxLogin()
					},
					onOpen: () => {
						uni.hideLoading()
						this.launching = false
					},
					onSuccess: (shanyanToken) => {
						uni.hideLoading()
						this.launching = false
						this.handleShanYanSuccess(shanyanToken)
					},
					onCancel: () => {
						uni.hideLoading()
						this.launching = false
						this.showFallback = true
					},
					onFail: (err) => {
						uni.hideLoading()
						this.launching = false
						this.showFallback = true
						// 调试：完整打印原生错误（屏幕 toast 只能看半截）
						try {
							console.error('[shanyan] onFail full:', JSON.stringify(err))
						} catch (e) {
							console.error('[shanyan] onFail:', err)
						}
						try {
							uni.setStorageSync('shanyanLastError', JSON.stringify(err || {}))
						} catch (e) {}
						const code = (err && (err.code ?? err.innerCode)) || '-'
						const msg = (err && (err.message || err.innerDesc)) || '一键登录暂不可用'
						const inner = (err && (err.innerDesc || err.innerCode)) || ''
						uni.showModal({
							title: '一键登录失败 ' + code,
							content: String(msg) + (inner ? '\n' + String(inner) : '') + '\n\n完整信息已存，可在控制台看 [shanyan] onFail full',
							showCancel: false,
							confirmText: '知道了'
						})
					}
				})
			},
			async handleShanYanSuccess(shanyanToken) {
				if (!shanyanToken) {
					this.showFallback = true
					uni.showToast({
						title: '未获取到登录凭证',
						icon: 'none'
					})
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
					const msg = (e && (e.msg || e.message)) || '登录失败'
					uni.showToast({
						title: String(msg).slice(0, 40),
						icon: 'none'
					})
				} finally {
					uni.hideLoading()
				}
			},
			doWxLogin() {
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				setTimeout(() => {
					uni.hideLoading()
					const isFirstLogin = !uni.getStorageSync('onboardingDone')
					uni.setStorageSync('token', 'mock-token-wechat')
					uni.setStorageSync('loginType', 'wechat')
					uni.setStorageSync('isFirstLogin', isFirstLogin)
					this.afterLogin()
				}, 600)
			},
			afterLogin(loginData) {
				let data = (loginData && loginData.data) || loginData || {}
				if (data.phoneBound === undefined) {
					const stored = uni.getStorageSync('phoneBound')
					if (stored !== '' && stored !== undefined && stored !== null) {
						data = Object.assign({}, data, { phoneBound: !!stored })
					}
				}
				// phoneBound=false（未绑定手机号）：停在登录页引导走验证码绑定/登录
				if (data.phoneBound === false) {
					this.showFallback = true
					this.showSmsLogin = true
					uni.showToast({
						title: '请绑定手机号',
						icon: 'none'
					})
					return
				}
				if (!uni.getStorageSync('onboardingDone')) {
					uni.reLaunch({
						url: '/pages/mine/myInfo'
					})
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
	.content{
		position: relative;
		width: 100%;
		height: 100vh;
		.banner{
			position: absolute;
			top: 0;
			left: 0;
			width: 100%;
			height: 100%;
			opacity: 0;
			transition: opacity 0.8s ease;
			&.active{
				opacity: 1;
			}
		}
		.container{
			width: 100%;
			position: absolute;
			text-align: center;
			bottom: 60rpx;
			left: 0;
			padding: 0 30rpx;
			box-sizing: border-box;
			.conPhone{
				font-size:56rpx;
				font-weight: bold;
				color: #09090D;
			}
			.oneBtn{
				width: 100%;
				height: 96rpx;
				text-align: center;
				position: relative;
				margin-top: 40rpx;
				image{
					width: 100%;
					height: 100%;
				}
				.btntext{
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
			.wxLogin{
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
			.smsEntry{
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
	.agreement{
		display: flex;
		justify-content: flex-start;
		align-items: flex-start;
		image{
			width: 36rpx;
			height: 36rpx;
			margin-right: 15rpx;
		}
		.agreText{
			font-size: 24rpx;
			color: #666;
			flex: 1;
			text-align: left;
			text{
				color:#2C89FF;
			}
		}
	}
	.sms-sheet{
		padding: 40rpx 32rpx 48rpx;
		.sms-title{
			font-size: 36rpx;
			font-weight: bold;
			color: #09090D;
			text-align: center;
		}
		.sms-sub{
			margin-top: 12rpx;
			font-size: 24rpx;
			color: #999;
			text-align: center;
		}
		.sms-field{
			margin-top: 28rpx;
			height: 96rpx;
			background: #F5F6F8;
			border-radius: 24rpx;
			padding: 0 28rpx;
			display: flex;
			align-items: center;
			input{
				flex: 1;
				height: 96rpx;
				font-size: 30rpx;
				color: #09090D;
			}
		}
		.sms-code-row{
			.sms-send{
				flex-shrink: 0;
				padding-left: 20rpx;
				font-size: 26rpx;
				color: #2C89FF;
				&.disabled{
					color: #999;
				}
			}
		}
		.sms-submit{
			margin-top: 40rpx;
			width: 100%;
			height: 96rpx;
			position: relative;
			.sms-submit-bg{
				width: 100%;
				height: 100%;
			}
			.sms-submit-text{
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
	.sms-placeholder{
		color: #B8B8B8;
	}
</style>
