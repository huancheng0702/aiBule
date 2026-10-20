<template>
	<view class="page">
		<!-- 搜索提示卡 -->
		<view class="search-card">
			<!-- 卡片背景图，自行补充 src -->
			<image class="search-card-bg" src="/static/ringStatusBg.png" mode=""></image>
			<view class="search-card-body">
				<view class="search-left">
					<!-- 状态点，自行补充 src -->
					<image class="search-dot" mode="aspectFit"></image>
					<view class="search-info">
						<text class="search-title">搜索你的戒指</text>
						<text class="search-desc">请将戒指放在手机附近，并保持蓝牙开启</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 设备列表 -->
		<scroll-view v-if="deviceList.length" class="device-list" scroll-y :show-scrollbar="false">
			<view v-for="(item, index) in deviceList" :key="item.id || index" class="device-card"
				:class="{ active: selectedId === item.id }" @click="onSelectDevice(item)">
				<!-- 连接图标，自行补充 src -->
				<image class="device-link" src="/static/linkIcon.png" mode=""></image>
				<view class="device-info">
					<text class="device-name">{{ item.name }}</text>
					<text class="device-meta">{{ item.meta }}</text>
				</view>
				<!-- 选中勾选图标，自行补充 src -->
				<image v-if="selectedId === item.id" class="device-check" src="/static/check-circle-fil.png"
					mode="aspectFit"></image>
				<image v-else class="device-check" src="/static/checkCircle.png" mode=""></image>
			</view>
		</scroll-view>

		<view v-else class="device-empty">
			<text class="device-empty-text">{{ searching ? '正在搜索附近设备...' : '暂未发现设备' }}</text>
		</view>

		<!-- 底部操作（固定） -->
		<view class="footer">
			<view class="action-btn solid" @click="onPrimaryAction">
				<!-- 按钮背景图，自行补充 src -->
				<image class="action-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="action-btn-text">{{ selectedId ? '连接' : '开始搜索' }}</text>
			</view>
			<view class="action-btn outline" @click="onRescan">
				<text class="action-btn-text outline-text">重新搜索</text>
			</view>
			<text class="help-link" @click="onHelp">搜索不到设备?</text>
		</view>

		<!-- 未发现戒指弹窗 -->
		<view v-if="showNotFoundPopup" class="notfound-mask" @click="closeNotFoundPopup">
			<view class="notfound-popup" @click.stop>
				<!-- 弹窗背景图 -->
				<image class="notfound-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="notfound-popup-body">
					<text class="notfound-title">未发现戒指</text>
					<text class="notfound-sub">请按顺序检查后重新搜索</text>
					<view class="notfound-tips">
						<text v-for="(tip, index) in notFoundTips" :key="index" class="notfound-tip">{{ tip }}</text>
					</view>
					<view class="action-btn solid notfound-btn" @click="onNotFoundRescan">
						<image class="action-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
						<text class="action-btn-text">我已检查，重新搜索</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'

	export default {
		data() {
			return {
				searching: false,
				selectedId: '',
				deviceList: [],
				showNotFoundPopup: false,
				fromOnboarding: false,
				connecting: false,
				permRequesting: false,
				unsub: null,
				emptyCheckTimer: null,
				notFoundTips: [
					'1、确认戒指有电并靠近手机',
					'2、打开蓝牙与附近设备权限',
					'3、若已被其他手机连接，请先断开'
				]
			}
		},
		onLoad(options) {
			this.fromOnboarding = !!(options && options.from === 'onboarding')
			this.ensurePermission()
			this.unsub = BleService.subscribe((s) => {
				this.searching = s.scanning
				this.deviceList = s.devices || []
				if (this.deviceList.length && !this.selectedId) {
					const best = this.deviceList.slice().sort((a, b) => (b.rssi || -999) - (a.rssi || -999))[0]
					this.selectedId = best ? best.id : ''
				}
				if (this.selectedId && !this.deviceList.find((d) => d.id === this.selectedId)) {
					this.selectedId = this.deviceList.length ? this.deviceList[0].id : ''
				}
			})
		},
		onUnload() {
			if (this.emptyCheckTimer) {
				clearTimeout(this.emptyCheckTimer)
				this.emptyCheckTimer = null
			}
			if (this.unsub) {
				this.unsub()
				this.unsub = null
			}
			BleService.stopScan().catch(() => {})
		},
		methods: {
			/**
			 * 该页面也可能被直接打开（不经过蓝牙权限引导页），这里补一次权限申请
			 */
			async ensurePermission() {
				if (this.permRequesting) return
				// 已授权过（如从蓝牙权限引导页跳转过来）则不重复申请
				if (uni.getStorageSync('blePermissionGranted')) return
				this.permRequesting = true
				uni.showLoading({
					title: '申请中...',
					mask: true
				})
				try {
					// #ifdef APP-PLUS
					if (!BleService.isAvailable()) {
						throw new Error('原生插件未加载，请重新制作并安装自定义调试基座')
					}
					await BleService.requestPermissions()
					await BleService.ensureInit()
					// #endif
					// #ifndef APP-PLUS
					uni.setStorageSync('blePermissionGranted', true)
					// #endif
					uni.hideLoading()
					this.permRequesting = false
				} catch (e) {
					uni.hideLoading()
					this.permRequesting = false
					const msg = (e && e.message) ? e.message : '需要蓝牙权限'
					uni.showModal({
						title: '需要蓝牙权限',
						content: msg + '\n\n请在系统设置中开启蓝牙与附近设备/定位权限后重试',
						confirmText: '去设置',
						success: (res) => {
							if (res.confirm) {
								// #ifdef APP-PLUS
								uni.openAppAuthorizeSetting()
								// #endif
								// #ifndef APP-PLUS
								uni.showToast({
									title: '请在系统设置中开启',
									icon: 'none'
								})
								// #endif
							}
						}
					})
				}
			},
			onPrimaryAction() {
				if (this.selectedId) {
					this.onConnect()
					return
				}
				this.startSearch()
			},
			async onConnect() {
				const device = this.deviceList.find((item) => item.id === this.selectedId)
				if (!device) {
					uni.showToast({
						title: '请先选择设备',
						icon: 'none'
					})
					return
				}
				if (this.connecting) return
				this.connecting = true
				uni.showLoading({
					title: '连接中...',
					mask: true
				})
				try {
					await BleService.connect(device)
					uni.hideLoading()
					uni.showToast({
						title: '连接成功',
						icon: 'success'
					})
					setTimeout(() => {
						uni.reLaunch({
							url: '/pages/index/index'
						})
					}, 400)
				} catch (e) {
					uni.hideLoading()
					uni.showToast({
						title: (e && e.message) || '连接失败',
						icon: 'none',
						duration: 2500
					})
				} finally {
					this.connecting = false
				}
			},
			onRescan() {
				this.selectedId = ''
				this.startSearch()
			},
			async startSearch() {
				if (this.emptyCheckTimer) {
					clearTimeout(this.emptyCheckTimer)
					this.emptyCheckTimer = null
				}
				this.showNotFoundPopup = false
				uni.showLoading({
					title: '搜索中...',
					mask: true
				})
				try {
					// #ifndef APP-PLUS
					throw new Error('请在 App 自定义基座中运行以使用蓝牙')
					// #endif
					// #ifdef APP-PLUS
					if (!BleService.isAvailable()) {
						throw new Error('原生插件未加载，请重新制作自定义基座')
					}
					await BleService.startScan({ timeoutMs: 10000 })
					uni.hideLoading()
					this.emptyCheckTimer = setTimeout(() => {
						const list = BleService.getState().devices || []
						if (!list.length) {
							this.showNotFoundPopup = true
						} else {
							uni.showToast({
								title: `发现 ${list.length} 台设备`,
								icon: 'none'
							})
						}
						this.emptyCheckTimer = null
					}, 10000)
					// #endif
				} catch (e) {
					uni.hideLoading()
					this.searching = false
					uni.showToast({
						title: (e && e.message) || '搜索失败',
						icon: 'none',
						duration: 2500
					})
				}
			},
			onSelectDevice(item) {
				this.selectedId = item.id
			},
			onHelp() {
				this.showNotFoundPopup = true
			},
			closeNotFoundPopup() {
				this.showNotFoundPopup = false
			},
			onNotFoundRescan() {
				this.showNotFoundPopup = false
				this.selectedId = ''
				this.startSearch()
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		padding: 24rpx 28rpx 0;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.search-card {
		position: relative;
		width: 100%;
		height: 169.22rpx;
		border-radius: 28rpx;
	}

	.search-card-bg {
		width: 100%;
		height: 100%;
	}

	.search-card-body {
		position: absolute;
		left: 0;
		top: 0;
		padding: 36rpx;
		box-sizing: border-box;
		z-index: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.search-left {
		flex: 1;
		min-width: 0;
		padding-right: 20rpx;
		display: flex;
		align-items: flex-start;
	}

	.search-dot {
		width: 16rpx;
		height: 16rpx;
		margin-top: 14rpx;
		margin-right: 16rpx;
		border-radius: 50%;
		background: #32d74b;
		box-shadow: 0 0 12rpx rgba(50, 215, 75, 0.8);
		flex-shrink: 0;
	}

	.search-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.search-title {
		font-size: 34rpx;
		font-weight: 700;
		line-height: 48rpx;
		color: #ffffff;
	}

	.search-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.search-ring {
		width: 160rpx;
		height: 160rpx;
		flex-shrink: 0;
		border-radius: 20rpx;
		background: rgba(255, 255, 255, 0.06);
	}

	.device-list {
		height:62%;
		margin-top: 28rpx;
		padding-bottom: 20rpx;
		box-sizing: border-box;
	}

	.device-card {
		position: relative;
		overflow: hidden;
		margin-bottom: 20rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.device-card.active {
		border-color: rgba(77, 163, 255, 0.45);
		background: rgba(30, 48, 90, 0.45);
	}

	.device-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.device-link {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		margin-right: 20rpx;
		border-radius: 12rpx;
	}

	.device-info {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.device-name {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.device-meta {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.device-check {
		position: relative;
		z-index: 1;
		width: 44rpx;
		height: 44rpx;
		margin-left: 16rpx;
		flex-shrink: 0;
	}

	.device-empty {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.device-empty-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.footer {
		flex-shrink: 0;
		width: 100%;
		padding: 24rpx 0 48rpx;
		box-sizing: border-box;
		background: #000000;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.action-btn {
		position: relative;
		width: 100%;
		height: 96rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.action-btn.solid {
		background: transparent;
	}

	.action-btn.outline {
		margin-top: 20rpx;
		background: transparent;
		border: 2rpx solid rgba(77, 163, 255, 0.7);
	}

	.action-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.action-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.action-btn-text.outline-text {
		color: #4da3ff;
	}

	.help-link {
		margin-top: 28rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.notfound-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.notfound-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
	}

	.notfound-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.notfound-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 40rpx;
		box-sizing: border-box;
	}

	.notfound-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.notfound-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.notfound-tips {
		margin-top: 36rpx;
		display: flex;
		flex-direction: column;
	}

	.notfound-tip {
		display: block;
		margin-bottom: 20rpx;
		font-size: 28rpx;
		line-height: 40rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.notfound-tip:last-child {
		margin-bottom: 0;
	}

	.notfound-btn {
		margin-top: 48rpx;
	}
</style>
