<template>
	<view class="page">
		<view class="hero">
			<image class="logo" src="/static/logo.png" mode=""></image>
			<text class="app-name">{{ appName || 'AI智能戒指' }}</text>
			<text class="app-version">V版本{{ version }}</text>
		</view>

		<view class="about-card">
			<text class="about-text">{{ aboutText || '暂无介绍' }}</text>
			<view v-if="contactName || contact" class="contact-block">
				<text v-if="contactName" class="contact-line">联系人：{{ contactName }}</text>
				<text v-if="contact" class="contact-line">联系方式：{{ contact }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAboutUs } from '@/common/api/about.js'

	export default {
		data() {
			return {
				logo: '',
				appName: 'AI智能戒指',
				version: '1.0.0',
				aboutText: '',
				contactName: '',
				contact: ''
			}
		},
		onLoad() {
			this.loadVersion()
			this.loadAbout()
		},
		methods: {
			loadVersion() {
				try {
					// #ifdef APP-PLUS
					const info = plus.runtime
					if (info && info.version) {
						this.version = info.version
						return
					}
					// #endif
					const sys = uni.getSystemInfoSync() || {}
					if (sys.appWgtVersion) {
						this.version = sys.appWgtVersion
						return
					}
					if (sys.appVersion) {
						this.version = sys.appVersion
					}
				} catch (e) {}
			},
			async loadAbout() {
				try {
					const data = await getAboutUs()
					const info = data && typeof data === 'object' ? data : {}
					this.logo = info.logo || info.logoUrl || info.image || info.icon || this.logo
					this.appName = info.name || info.appName || info.title || this.appName
					this.aboutText = info.intro || info.description || info.content || info.about || info.remark || ''
					this.contactName = info.contactName || info.contacts || info.linkman || ''
					this.contact = info.contact || info.contactWay || info.phone || info.mobile || info.email || ''
					if (info.version) {
						this.version = String(info.version)
					}
				} catch (e) {}
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		padding: 80rpx 40rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.hero {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.logo {
		width: 140rpx;
		height: 140rpx;
		border-radius: 32rpx;
	}

	.app-name {
		margin-top: 36rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.app-version {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.about-card {
		margin-top: 56rpx;
		padding: 36rpx 32rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		box-sizing: border-box;
	}

	.about-text {
		font-size: 26rpx;
		line-height: 42rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.contact-block {
		margin-top: 28rpx;
		padding-top: 24rpx;
		border-top: 1rpx solid rgba(255, 255, 255, 0.08);
		display: flex;
		flex-direction: column;
	}

	.contact-line {
		font-size: 26rpx;
		line-height: 40rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.contact-line + .contact-line {
		margin-top: 8rpx;
	}
</style>
