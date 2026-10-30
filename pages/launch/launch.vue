<template>
	<!-- 图片地址自行补充 -->
	<view class="">
		<image class="launch-bg" src="/static/lanchImg.png" mode=""></image>
	</view>
</template>

<script>
	import {
		hasLogin,
		LOGIN_PATH,
		HOME_PATH
	} from '@/common/auth.js'

	// 启动页停留时长（毫秒）
	const SPLASH_DURATION = 1000

	export default {
		data() {
			return {
				redirecting: false
			}
		},
		onLoad() {
			this.redirectTimer = setTimeout(() => {
				this.redirectTimer = null
				this.goNext()
			}, SPLASH_DURATION)
		},
		onUnload() {
			if (this.redirectTimer) {
				clearTimeout(this.redirectTimer)
				this.redirectTimer = null
			}
		},
		methods: {
			goNext() {
				if (this.redirecting) return
				this.redirecting = true
				// 登录判断统一放在启动页：有登录态进首页，否则去登录页
				const target = hasLogin() ? HOME_PATH : LOGIN_PATH
				uni.reLaunch({
					url: target,
					complete: () => {
						this.redirecting = false
					}
				})
			}
		}
	}
</script>

<style scoped>
	.launch-bg {
		display: block;
		width: 100vw;
		height: 100vh;
	}
</style>
