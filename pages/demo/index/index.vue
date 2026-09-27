<template>
	<view class="content">
		<image class="logo" src="/static/logo.png"></image>
		<view class="text-area">
			<text class="title">AI 戒指</text>
			<text class="desc">CoolWear 蓝牙本地插件</text>
		</view>
		<button class="entry-btn" type="primary" @click="goBleTest">进入蓝牙测试</button>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				title: 'AI 戒指'
			}
		},
		methods: {
			goBleTest() {
				uni.navigateTo({
					url: '/pages/ble-test/index'
				})
			}
		}
	}
</script>

<style>
	.content {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 100vh;
		padding: 40rpx;
	}

	.logo {
		height: 200rpx;
		width: 200rpx;
		margin-bottom: 40rpx;
	}

	.text-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 60rpx;
	}

	.title {
		font-size: 40rpx;
		font-weight: 600;
		color: #1d2129;
	}

	.desc {
		margin-top: 12rpx;
		font-size: 28rpx;
		color: #86909c;
	}

	.entry-btn {
		width: 400rpx;
	}
</style>
