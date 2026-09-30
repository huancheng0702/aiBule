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
			<view class="conPhone">
				{{ maskPhone || '本机号码' }}
			</view>
			<view class="oneBtn" @click="onPhoneLogin">
				<image src="/static/btnBan.png" class="benBan" mode=""></image>
				<view class="btntext">
					本机号码一键登录
				</view>
			</view>
			<view class="wxLogin" @click="onWxLogin">
				微信登录
			</view>
			<view class="agreement" @click="agreed = !agreed">
				<image :src="agreed ? '/static/check-circle-fil.png' : '/static/checkCircle.png'" class="agreImg" mode=""></image>
				<view class="agreText">
					我已阅读并同意 <text>《用户协议与隐私政策》</text>及健康 <text>《数据处理说明》</text>
				</view>
			</view>
		</view>
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
				launching: false
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
			// 进页直接拉起闪验授权页（UI 已按本页样式配置）
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
				uni.showToast({
					title: '请在 App 真机使用一键登录',
					icon: 'none'
				})
				// #endif
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
						const msg = (err && (err.message || err.innerDesc)) || '一键登录暂不可用'
						uni.showToast({
							title: String(msg).slice(0, 40),
							icon: 'none'
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
					const res = await exchangeLoginByShanYanToken(shanyanToken)
					uni.hideLoading()
					const isFirstLogin = !uni.getStorageSync('onboardingDone')
					uni.setStorageSync('token', (res && res.token) || shanyanToken)
					uni.setStorageSync('loginType', 'phone')
					uni.setStorageSync('isFirstLogin', isFirstLogin)
					if (res && res.phone) {
						uni.setStorageSync('phone', res.phone)
					}
					this.afterLogin()
				} catch (e) {
					uni.hideLoading()
					this.showFallback = true
					uni.showToast({
						title: (e && e.message) || '登录失败',
						icon: 'none'
					})
				}
			},
			doWxLogin() {
				uni.showLoading({
					title: '登录中...',
					mask: true
				})
				// 微信登录后续对接；先保持原 mock 流程
				setTimeout(() => {
					uni.hideLoading()
					const isFirstLogin = !uni.getStorageSync('onboardingDone')
					uni.setStorageSync('token', 'mock-token-wechat')
					uni.setStorageSync('loginType', 'wechat')
					uni.setStorageSync('isFirstLogin', isFirstLogin)
					this.afterLogin()
				}, 600)
			},
			afterLogin() {
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
			height: 30%;
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
				margin: 30rpx 0;
				box-sizing: border-box;
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
</style>
