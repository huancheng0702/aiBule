<template>
	<view class="page">
		<!-- 设备权限 -->
		<view class="section">
			<text class="section-label">设备权限</text>
			<view class="menu-card">
				<view class="menu-item" @click="onSystemPermission">
					<view class="menu-info">
						<text class="menu-title">系统权限管理</text>
						<text class="menu-desc">仅蓝牙附近设备</text>
					</view>
					<image class="menu-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
				</view>
			</view>
		</view>

		<!-- 账号与隐私 -->
		<view class="section">
			<text class="section-label">账号与隐私</text>
			<view class="menu-card">
				<view
					v-for="(item, index) in accountList"
					:key="item.key"
					class="menu-item"
					:class="{ 'no-border': index === accountList.length - 1 }" 
					@click="onAccountItem(item)"
				>
					<view class="menu-info">
						<text class="menu-title">{{ item.title }}</text>
						<text v-if="item.desc" class="menu-desc">{{ item.desc }}</text>
					</view>
					<image class="menu-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
				</view>
			</view>
		</view>

		<!-- 底部说明 -->
		<view class="tip-row">
			<view class="tip-icon">
				<text class="tip-icon-text">!</text>
			</view>
			<text class="tip-text">当前仅使用蓝牙附近设备，用于发现并连接戒指</text>
		</view>

		<!-- 账号与安全弹窗 -->
		<view v-if="showSecurityPopup" class="popup-mask" @click="closeSecurityPopup">
			<view class="popup-sheet" @click.stop>
				<image class="popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="popup-body">
					<text class="popup-title">账号与安全</text>
					<text class="popup-sub">手机号、登录与账号状态</text>

					<view class="info-grid">
						<view v-for="(item,index) in securityCards" :key="indexy" class="info-card" @click="onSecurityCard(item)">
							<!-- 图标，自行补充 src -->
							<image class="info-icon" :src="item.icon" mode=""></image>
							<text class="info-label">{{ item.label }}</text>
							<text class="info-value">{{ item.value }}</text>
						</view>
					</view>

					<view class="popup-btns">
						<view class="popup-btn ghost" @click="closeSecurityPopup">
							<text class="popup-btn-text">完成</text>
						</view>
						<view class="popup-btn solid" @click="openCancelPopup">
							<image class="popup-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="popup-btn-text">申请注销账号</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 注销账号确认弹窗 -->
		<view v-if="showCancelPopup" class="popup-mask" @click="closeCancelPopup">
			<view class="popup-sheet" @click.stop>
				<image class="popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="popup-body cancel-body">
					<text class="popup-title">账号与安全</text>
					<text class="popup-sub">手机号、登录与账号状态</text>

					<!-- 注销插画，自行补充 src -->
					<image class="cancel-illust" src="/static/delAccImg.png" mode=""></image>

					<text class="cancel-title">注销后将无法恢复</text>
					<text class="cancel-desc">注销后所有数据将会清除</text>

					<view class="popup-btns">
						<view class="popup-btn ghost" @click="closeCancelPopup">
							<text class="popup-btn-text">返回</text>
						</view>
						<view class="popup-btn solid" @click="confirmCancelAccount">
							<image class="popup-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="popup-btn-text">确认注销</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { clearLoginSession, LOGIN_PATH, USER_INFO_KEY, saveLoginSession } from '@/common/auth.js'
	import { getUserInfo, cancelAccount } from '@/common/api/user.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function formatLoginTime(raw) {
		if (!raw) return '--'
		const str = String(raw).replace(/-/g, '/')
		const d = new Date(str)
		if (Number.isNaN(d.getTime())) return String(raw)
		const now = new Date()
		const sameDay = d.getFullYear() === now.getFullYear() &&
			d.getMonth() === now.getMonth() &&
			d.getDate() === now.getDate()
		const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
		if (sameDay) return `今天 ${hm}`
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${hm}`
	}

	function mapAccountStatus(info) {
		const raw = info.accountStatus != null ? info.accountStatus : info.status
		if (raw == null || raw === '') return '正常'
		const text = String(raw)
		if (/[\u4e00-\u9fa5]/.test(text)) return text
		const n = Number(raw)
		if (n === 1 || n === 0) return '正常'
		if (n === 2 || n === -1) return '已停用'
		return text
	}

	function mapLoginProtect(info) {
		const raw = info.loginProtect != null ? info.loginProtect : info.protectStatus
		if (raw == null || raw === '') return '已开启'
		if (raw === true || raw === 1 || raw === '1' || raw === 'true') return '已开启'
		if (raw === false || raw === 0 || raw === '0' || raw === 'false') return '未开启'
		if (/[\u4e00-\u9fa5]/.test(String(raw))) return String(raw)
		return '已开启'
	}

	export default {
		data() {
			return {
				showSecurityPopup: false,
				showCancelPopup: false,
				cancelling: false,
				phone: '',
				lastLoginText: '--',
				accountStatus: '正常',
				loginProtect: '已开启',
				accountList: [
					{
						key: 'policy',
						title: '隐私政策',
						desc: '查看数据处理说明'
					},
					{
						key: 'security',
						title: '账号与安全',
						desc: '手机号、注销账号'
					},
					{
						key: 'about',
						title: '关于 AI 智能戒指',
						desc: '版本、协议与主体信息'
					},
					{
						key: 'logout',
						title: '退出登录',
						desc: ''
					}
				]
			}
		},
		computed: {
			securityCards() {
				return [
					{
						key: 'phone',
						icon:'/static/settingPhone.png',
						label: '登录手机号',
						value: this.maskPhone(this.phone)
					},
					{
						key: 'recent',
						icon:'/static/settingZuijin.png',
						label: '最近登录',
						value: this.lastLoginText
					},
					{
						key: 'status',
						label: '账号状态',
						icon:'/static/settingAccStatus.png',
						value: this.accountStatus
					}
					// {
					// 	key: 'protect',
					// 	label: '登录保护',
					// 	icon:'/static/xiaoyusan.png',
					// 	value: this.loginProtect
					// }
				]
			}
		},
		onShow() {
			this.applyCachedUser()
			this.loadAccountInfo()
		},
		methods: {
			applyUser(info) {
				if (!info || typeof info !== 'object') return
				this.phone = info.phone || this.phone
				this.lastLoginText = info.loginDate
				this.accountStatus = mapAccountStatus(info)
				this.loginProtect = mapLoginProtect(info)
			},
			applyCachedUser() {
				const phone = uni.getStorageSync('phone') || ''
				const userInfo = uni.getStorageSync(USER_INFO_KEY) || {}
				this.phone = phone || userInfo.phone || ''
				this.applyUser(userInfo)
				if (!this.phone) this.phone = phone
			},
			async loadAccountInfo() {
				try {
					const info = await getUserInfo()
					if (!info) return
					saveLoginSession(info)
					this.applyUser(info)
				} catch (e) {}
			},
			maskPhone(phone) {
				const p = String(phone || '')
				if (p.length < 7) return p || '--'
				return `${p.slice(0, 3)}****${p.slice(-4)}`
			},
			onSystemPermission() {
				// #ifdef APP-PLUS
				if (typeof uni.openAppAuthorizeSetting === 'function') {
					uni.openAppAuthorizeSetting({
						fail: () => {
							uni.showToast({ title: '无法打开系统设置', icon: 'none' })
						}
					})
					return
				}
				// #endif
				uni.showToast({ title: '请在系统设置中管理权限', icon: 'none' })
			},
			onAccountItem(item) {
				if (!item) return
				if (item.key === 'logout') {
					this.onLogout()
					return
				}
				if (item.key === 'security') {
					this.openSecurityPopup()
					return
				}
				if (item.key === 'policy') {
					uni.navigateTo({
						url: '/pages/mine/minewebview?contentId=5'
					})
					return
				}
				if (item.key === 'about') {
					uni.navigateTo({
						url: '/pages/mine/aboutRing'
					})
					return
				}
			},
			openSecurityPopup() {
				this.applyCachedUser()
				this.showSecurityPopup = true
				this.loadAccountInfo()
			},
			onSecurityCard(item) {
				if (item && item.key === 'phone') {
					this.showSecurityPopup = false
					uni.navigateTo({ url: '/pages/mine/bindPhone' })
				}
			},
			closeSecurityPopup() {
				this.showSecurityPopup = false
			},
			openCancelPopup() {
				this.showSecurityPopup = false
				this.showCancelPopup = true
			},
			closeCancelPopup() {
				this.showCancelPopup = false
				this.showSecurityPopup = true
			},
			async confirmCancelAccount() {
				if (this.cancelling) return
				this.cancelling = true
				try {
					await cancelAccount()
					this.showCancelPopup = false
					this.showSecurityPopup = false
					clearLoginSession()
					uni.showToast({ title: '账号已注销', icon: 'none' })
					setTimeout(() => {
						uni.reLaunch({
							url: LOGIN_PATH
						})
					}, 400)
				} catch (e) {
					// http 层已 toast
				} finally {
					this.cancelling = false
				}
			},
			onLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确认退出当前账号？',
					confirmText: '退出',
					cancelText: '取消',
					success: (res) => {
						if (!res.confirm) return
						clearLoginSession()
						uni.reLaunch({
							url: LOGIN_PATH
						})
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		padding: 24rpx 28rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.section {
		margin-bottom: 36rpx;
	}

	.section-label {
		display: block;
		margin-bottom: 16rpx;
		padding-left: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
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
		padding: 28rpx 0;
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.menu-item.no-border {
		border-bottom: none;
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
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.menu-arrow {
		width: 24rpx;
		height: 24rpx;
		margin-left: 16rpx;
		flex-shrink: 0;
		opacity: 0.45;
	}

	.tip-row {
		display: flex;
		align-items: flex-start;
		padding: 0 8rpx;
	}

	.tip-icon {
		width: 28rpx;
		height: 28rpx;
		margin-top: 4rpx;
		border-radius: 50%;
		background: #3b82f6;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.tip-icon-text {
		font-size: 20rpx;
		font-weight: 700;
		line-height: 20rpx;
		color: #ffffff;
	}

	.tip-text {
		flex: 1;
		min-width: 0;
		margin-left: 12rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.popup-mask {
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

	.popup-sheet {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
	}

	.popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.popup-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.info-grid {
		margin-top: 36rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.info-card {
		width: 48%;
		margin-bottom: 16rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
	}

	.info-icon {
		width: 48rpx;
		height: 48rpx;
	}

	.info-label {
		margin-top: 20rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.info-value {
		margin-top: 8rpx;
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.popup-btns {
		margin-top: 28rpx;
		width: 100%;
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.popup-btn {
		position: relative;
		flex: 1;
		min-width: 0;
		height: 88rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.popup-btn.ghost {
		margin-right: 20rpx;
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.popup-btn.solid {
		background: transparent;
	}

	.popup-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.popup-btn-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
		white-space: nowrap;
	}

	.cancel-body {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
	}

	.cancel-illust {
		display: block;
		width: 240rpx;
		height: 240rpx;
		margin: 48rpx auto 0;
	}

	.cancel-title {
		display: block;
		width: 100%;
		margin-top: 28rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
		text-align: center;
	}

	.cancel-desc {
		display: block;
		width: 100%;
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
		text-align: center;
	}
</style>
