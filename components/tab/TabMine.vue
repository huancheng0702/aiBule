<template>
	<view class="tab-page">
		<!-- 用户信息 -->
		<view class="profile" :style="{ paddingTop: statusBarHeight + 'px' }" @click="onProfile">
			<image class="avatar" src="/static/hic.png" mode="aspectFill"></image>
			<view class="profile-info">
				<text class="nickname">爱吃的兔子</text>
				<text class="profile-tip">完善档案获得个性化建议</text>
			</view>
			<image src="/static/rightIcon.png" class="rightIcon" mode=""></image>
		</view>

		<!-- 设备状态卡 -->
		<view class="device-card" @click="onManage">
			<image src="/static/ringStatusBg.png" class="ringBg" mode=""></image>
			<view class="devCon">
				<view class="device-left">
					<view class="device-name-row">
						<view class="status-dot" :class="{ off: !connected }"></view>
						<text class="device-name">{{ deviceName }}</text>
					</view>
					<text class="device-status">{{ deviceStatusText }}</text>
				</view>
				<image class="device-img" mode="aspectFit"></image>
				<view class="manage-btn" @click.stop="onManage">
					<image src="/static/manBtn.png" class="manBtn" mode=""></image>
				</view>
			</view>
			
		</view>

		<!-- 功能 -->
		<view class="section">
			<text class="section-title">功能</text>
			<view class="menu-card">
				<view v-for="(item, index) in featureList" :key="item.title" class="menu-item"
					:class="{ 'no-border': index === featureList.length - 1 }" @click="onMenu(item)">
					<image class="menu-icon" :src="item.icon" mode="aspectFit"></image>
					<view class="menu-info">
						<text class="menu-title">{{ item.title }}</text>
						<text class="menu-desc">{{ item.desc }}</text>
					</view>
					<image src="/static/minRight.png" class="minRight" mode=""></image>
				</view>
			</view>
		</view>

		<!-- 设置与帮助 -->
		<view class="section">
			<text class="section-title">设置与帮助</text>
			<view class="menu-card">
				<view v-for="(item, index) in settingList" :key="item.title" class="menu-item"
					:class="{ 'no-border': index === settingList.length - 1 }" @click="onMenu(item)">
					<image class="menu-icon" :src="item.icon" mode="aspectFit"></image>
					<view class="menu-info">
						<text class="menu-title">{{ item.title }}</text>
						<text class="menu-desc">{{ item.desc }}</text>
					</view>
					<image src="/static/minRight.png" class="minRight" mode=""></image>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'

	export default {
		name: 'TabMine',
		data() {
			return {
				statusBarHeight: 20,
				connected: false,
				deviceName: '未连接设备',
				batteryLevel: null,
				batteryCharging: false,
				boundDevice: null,
				unsub: null,
				featureList: [{
						title: '紧急联系人',
						desc: '已设置1人',
						key: 'contact',
						icon:'/static/jinjilianxi.png'
					},
					{
						title: 'SOS 紧急联络',
						desc: '向联系人发送求助',
						key: 'sos',
						icon:'/static/sos.png'
					},
					{
						title: '我的报告',
						desc: '健康报告与 AI 解读',
						key: 'report',
						icon:'/static/minBao.png'
					}
				],
				settingList: [{
						title: '设置隐私',
						desc: '权限、隐私、账号安全',
						key: 'privacy',
						icon:'/static/setting.png'
					},
					{
						title: '帮助与反馈',
						desc: '常见问题与设备指引',
						key: 'help',
						icon:'/static/bangzhu.png'
					}
				]
			}
		},
		computed: {
			deviceStatusText() {
				if (!this.connected && !this.boundDevice) {
					return '未连接 · 点击添加设备'
				}
				if (!this.connected) {
					return '未连接'
				}
				if (this.batteryLevel != null) {
					return this.batteryCharging
						? `已连接 · 电量 ${this.batteryLevel}% · 充电中`
						: `已连接 · 电量 ${this.batteryLevel}%`
				}
				return this.batteryCharging ? '已连接 · 充电中' : '已连接'
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
			this.unsub = BleService.subscribe((s) => {
				this.connected = !!s.connected
				this.batteryLevel = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				this.boundDevice = s.device
				this.deviceName = (s.device && s.device.name) || (s.connected ? '智能戒指' : '未连接设备')
			})
		},
		beforeDestroy() {
			if (this.unsub) {
				this.unsub()
				this.unsub = null
			}
		},
		methods: {
			onProfile() {
				uni.navigateTo({
					url: '/pages/mine/myInfo'
				})
			},
			onManage() {
				const s = BleService.getState()
				if (s.device || s.connected) {
					const name = encodeURIComponent((s.device && s.device.name) || '智能戒指')
					const battery = s.batteryLevel != null ? s.batteryLevel : ''
					uni.navigateTo({
						url: `/pages/mine/deviceInfo?name=${name}&battery=${battery}`
					})
					return
				}
				uni.navigateTo({
					url: '/pages/mine/deviceManage'
				})
			},
			onMenu() {}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		min-height: 100%;
		padding: 0 28rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.profile {
		display: flex;
		align-items: center;
		padding-bottom: 36rpx;
	}

	.avatar {
		width: 164rpx;
		height: 164rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.12);
		border:2rpx solid rgba(255, 255, 255, 0.12);
		flex-shrink: 0;
	}

	.profile-info {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
	}

	.nickname {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.profile-tip {
		margin-top: 6rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.arrow {
		flex-shrink: 0;
		margin-left: 12rpx;
		font-size: 40rpx;
		line-height: 40rpx;
		color: rgba(255, 255, 255, 0.35);
	}

	.device-card {
		height: 174.58rpx;
		width: 100%;
		position: relative;
	}
	.ringBg{
		width: 100%;
		height: 100%;
		border-radius: 24rpx;
	}
	.devCon{
		width: 100%;
		height: 100%;
		position: absolute;
		top: 0;
		left: 0;
		border-radius: 24rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0 36rpx;
		box-sizing: border-box;
	}

	.device-left {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		z-index: 2;
	}

	.device-name-row {
		display: flex;
		align-items: center;
	}

	.status-dot {
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		background: #34c759;
		margin-right: 38rpx;
		flex-shrink: 0;
	}

	.status-dot.off {
		background: rgba(255, 255, 255, 0.35);
		box-shadow: none;
	}

	.device-name {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.device-status {
		margin-top: 8rpx;
		padding-left: 50rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.device-img {
		width: 120rpx;
		height: 100rpx;
		margin: 0 20rpx;
		flex-shrink: 0;
		z-index: 1;
	}

	.manage-btn {
		width: 140rpx;
		height: 64rpx;
	}
	.manBtn{
		width: 140rpx;
		height: 64rpx;
	}

	.manage-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: #ffffff;
		font-weight: 500;
	}

	.section {
		margin-top: 40rpx;
	}

	.section-title {
		display: block;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
		margin-bottom: 20rpx;
	}

	.menu-card {
		background: #1a1b25;
		border-radius: 24rpx;
		overflow: hidden;
		padding: 0 28rpx;
	}

	.menu-item {
		display: flex;
		align-items: center;
		padding:28rpx 0;
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.menu-item.no-border {
		border-bottom: none;
	}

	.menu-icon {
		width: 48rpx;
		height: 48rpx;
		border-radius: 18rpx;
		margin-right: 20rpx;
	}

	.menu-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.menu-title {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.menu-desc {
		margin-top: 4rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}
	.rightIcon{
		width: 76rpx;
		height: 64rpx;
	}
	.minRight{
		width: 34rpx;
		height: 34rpx;
	}
</style>