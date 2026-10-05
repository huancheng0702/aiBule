<template>
	<view class="page">
		<view class="page-body">
			<TabHome v-show="current === 0" />
			<TabHealth v-show="current === 1" />
			<TabAi v-show="current === 2" />
			<TabMine v-show="current === 3" />
		</view>

		<view class="tabbar-wrap">
			<view class="tabbar">
				<view v-for="(item, index) in tabList" :key="item.text" class="tabbar-item" @click="switchTab(index)">
					<view class="icon-wrap" :class="{ active: current === index }">
						<image class="tab-icon" :src="current === index ? item.selectedIconPath : item.iconPath"
							mode="aspectFit" />
					</view>
					<text class="tab-text" :class="{ active: current === index }">{{ item.text }}</text>
				</view>
			</view>
			<view class="safe-area"></view>
		</view>
	</view>
</template>

<script>
	import TabHome from '@/components/tab/TabHome.vue'
	import TabHealth from '@/components/tab/TabHealth.vue'
	import TabAi from '@/components/tab/TabAi.vue'
	import TabMine from '@/components/tab/TabMine.vue'

	export default {
		components: {
			TabHome,
			TabHealth,
			TabAi,
			TabMine
		},
		data() {
			return {
				current: 0,
				tabList: [{
						text: '首页',
						iconPath: '/static/tabbar/index.png',
						selectedIconPath: '/static/tabbar/indexSelect.png'
					},
					{
						text: '健康',
						iconPath: '/static/tabbar/jiankang.png',
						selectedIconPath: '/static/tabbar/jiankangSelect.png'
					},
					{
						text: 'AI助手',
						iconPath: '/static/tabbar/ai.png',
						selectedIconPath: '/static/tabbar/aiSelect.png'
					},
					{
						text: '我的',
						iconPath: '/static/tabbar/mine.png',
						selectedIconPath: '/static/tabbar/mineSelect.png'
					}
				]
			}
		},
		onLoad() {
			this.checkEntry()
		},
		methods: {
			checkEntry() {
				const token = uni.getStorageSync('token')
				if (!token) {
					uni.reLaunch({
						url: '/pages/login'
					})
					return
				}
				// 首次登录未完成引导：完善信息 → 蓝牙权限
				if (!uni.getStorageSync('onboardingDone')) {
					uni.reLaunch({
						url: '/pages/mine/myInfo'
					})
				}
			},
			switchTab(index) {
				if (index === this.current) return
				this.current = index
			}
		}
	}
</script>

<style scoped>
	.page {
		height: 100vh;
		background: #000000;
		box-sizing: border-box;
		padding-bottom: calc(128rpx + constant(safe-area-inset-bottom));
		padding-bottom: calc(128rpx + env(safe-area-inset-bottom));
	}

	.page-body {
		height: 100%;
	}

	.tabbar-wrap {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 999;
		padding: 0 24rpx 16rpx;
		box-sizing: border-box;
	}

	.tabbar {
		display: flex;
		align-items: center;
		justify-content: space-around;
		height: 108rpx;
		background: #262732;
		border-radius: 60rpx;
		border: 2rpx solid rgba(255, 255, 255, 0.1);
		padding: 0 12rpx;
		box-sizing: border-box;
	}

	.tabbar-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 100%;
	}

	.icon-wrap {
		width: 56rpx;
		height: 56rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
	}

	.tab-icon {
		width: 48rpx;
		height: 48rpx;
	}

	.tab-text {
		margin-top: 4rpx;
		font-size: 20rpx;
		line-height: 28rpx;
		color: #8e8e93;
	}

	.tab-text.active {
		color: #fff;
	}

	.safe-area {
		height: constant(safe-area-inset-bottom);
		height: env(safe-area-inset-bottom);
	}
</style>