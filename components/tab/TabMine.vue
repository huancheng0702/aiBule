<template>
	<view class="tab-page">
		<!-- 用户信息 -->
		<view class="profile" :style="{ paddingTop: statusBarHeight + 'px' }" @click="onProfile">
			<image class="avatar" :src="avatar || '/static/morentouxiang.png'" mode="aspectFill"></image>
			<view class="profile-info">
				<text class="nickname">{{ nickName || '未设置昵称' }}</text>
				<text class="profile-tip">{{ profileTip }}</text>
			</view>
			<image src="/static/rightIcon.png" class="rightIcon" mode=""></image>
			
		</view>

		<!-- 设备状态卡 -->
		<view class="device-card" @click="onManage">
			<image src="/static/ringStatusBg.png" class="ringBg" mode=""></image>
			<view class="devCon">
				<view class="device-left">
					<view class="device-name-row">
						<image class="battery-icon" src="/static/lvdian.png" mode="aspectFit"></image>
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

		<!-- 紧急联系人弹窗 -->
		<view v-if="showContactPopup" class="contact-mask" @click="closeContactPopup">
			<view class="contact-popup" @click.stop>
				<image class="contact-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="contact-popup-body">
					<text class="contact-popup-title">紧急联系人</text>
					<text class="contact-popup-sub">添加/编辑联系人</text>

					<view v-if="contacts.length" class="contact-list">
						<view v-for="(item, index) in contacts" :key="item.id || index" class="contact-card">
							<image class="contact-avatar" :src="item.avatar || '/static/morentouxiang.png'" mode="aspectFill"></image>
							<view class="contact-info">
								<text class="contact-name">{{ item.name }}</text>
								<text class="contact-meta">{{ item.relation }} · {{ maskPhone(item.phone) }}</text>
							</view>
							<view class="contact-card-actions">
								<image class="contact-edit-btn" @click="openContactForm(item, index)" src="/static/bianjiImg.png" mode=""></image>
								<!-- <view class="contact-edit-btn" @click="openContactForm(item, index)">编辑</view> -->
								<view class="contact-del-btn" @click="confirmDeleteContact(item, index)">删除</view>
							</view>
						</view>
					</view>
					<view v-else class="contact-empty">
						<text class="contact-empty-text">请先添加紧急联系人</text>
					</view>

					<view class="contact-actions">
						<view class="contact-btn solid" @click="openContactForm(null, -1)">
							<image class="contact-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="contact-btn-text">添加/编辑联系人</text>
						</view>
						<view class="contact-btn outline" @click="closeContactPopup">
							<text class="contact-btn-text">完成</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 添加/编辑联系人表单弹窗 -->
		<view v-if="showContactForm" class="contact-mask" @click="closeContactForm">
			<view class="contact-popup form-popup" @click.stop>
				<image class="contact-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="contact-popup-body">
					<text class="contact-popup-title">{{ editingIndex >= 0 ? '编辑联系人' : '添加联系人' }}</text>
					<text class="contact-popup-sub">信息仅用于紧急联络</text>

					<view class="form-field">
						<text class="form-label">姓名</text>
						<input class="form-input" v-model="form.name" maxlength="20" placeholder="请输入姓名"
							placeholder-class="form-placeholder" />
					</view>
					<view class="form-field">
						<text class="form-label">关系</text>
						<view v-if="relationOptions.length" class="form-input form-picker" @click="openRelationPicker">
							<text :class="{ 'form-placeholder': !form.relation }">{{ form.relation || '请选择关系' }}</text>
							<image class="form-picker-arrow" src="/static/minRight.png" mode="aspectFit"></image>
						</view>
						<input v-else class="form-input" v-model="form.relation" maxlength="10"
							placeholder="如：母亲、配偶" placeholder-class="form-placeholder" />
					</view>
					<view class="form-field">
						<text class="form-label">手机号</text>
						<input class="form-input" v-model="form.phone" type="number" maxlength="11"
							placeholder="请输入手机号" placeholder-class="form-placeholder" />
					</view>

					<view class="contact-actions">
						<view class="contact-btn solid" @click="saveContactForm">
							<image class="contact-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="contact-btn-text">保存</text>
						</view>
						<view class="contact-btn outline" @click="closeContactForm">
							<text class="contact-btn-text">取消</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 关系选择，避免原生 picker 被自定义弹窗遮挡 -->
		<view v-if="showRelationPicker" class="relation-mask" @click="closeRelationPicker">
			<view class="relation-sheet" @click.stop>
				<text class="relation-title">选择关系</text>
				<view class="relation-list">
					<view v-for="item in relationOptions" :key="item" class="relation-item"
						:class="{ active: form.relation === item }" @click="selectRelation(item)">
						<text class="relation-item-text">{{ item }}</text>
					</view>
				</view>
				<view class="relation-cancel" @click="closeRelationPicker">
					<text class="relation-cancel-text">取消</text>
				</view>
			</view>
		</view>

		<!-- SOS 紧急联络弹窗 -->
		<view v-if="showSosPopup" class="contact-mask" @click="closeSosPopup">
			<view class="contact-popup sos-popup" @click.stop>
				<image class="contact-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="contact-popup-body sos-popup-body">
					<text class="contact-popup-title">SOS 紧急联络</text>
					<text class="contact-popup-sub">向紧急联系人发送求助</text>

					<!-- SOS 插画，自行补充 src -->
					<image class="sos-illust" src="/static/sosImg.png" mode=""></image>

					<text class="sos-ask">联系 {{ sosTargetName }}?</text>
					<view class="sos-desc-wrap">
						<text class="sos-desc">将发送位置与求助信息，并拨打</text>
						<text class="sos-phone">{{ sosTargetPhone }}</text>
					</view>

					<view class="sos-btns">
						<view class="sos-btn ghost" @click="closeSosPopup">
							<text class="sos-btn-text">取消</text>
						</view>
						<view class="sos-btn solid" @click="confirmSos">
							<image class="sos-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
							<text class="sos-btn-text">确认发起SOS</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import BleService from '@/common/ble-service.js'
	import { getUserInfo } from '@/common/api/user.js'
	import {
		getEmergencyList,
		getEmergencyRelations,
		addEmergencyContact,
		editEmergencyContact,
		removeEmergencyContact
	} from '@/common/api/emergency.js'
	import { saveLoginSession } from '@/common/auth.js'

	const PHONE_REG = /^1\d{10}$/

	function asArray(data) {
		if (Array.isArray(data)) return data
		if (!data || typeof data !== 'object') return []
		if (Array.isArray(data.list)) return data.list
		if (Array.isArray(data.rows)) return data.rows
		if (Array.isArray(data.records)) return data.records
		return []
	}

	function normalizeContact(raw) {
		if (!raw || typeof raw !== 'object') return null
		const id = raw.contactId != null ? raw.contactId : raw.id
		const name = raw.contactName || raw.name || ''
		const phone = raw.phone || raw.mobile || ''
		if (!name && !phone && id == null) return null
		return {
			id,
			name,
			relation: raw.relation || '',
			phone: String(phone || ''),
			avatar: raw.avatar || '/static/morentouxiang.png',
			remark: raw.remark || ''
		}
	}

	function normalizeRelations(data) {
		return asArray(data).map((item) => {
			if (typeof item === 'string' || typeof item === 'number') return String(item)
			if (!item || typeof item !== 'object') return ''
			return item.relation || item.name || item.label || item.value || item.dictLabel || ''
		}).filter(Boolean)
	}

	export default {
		name: 'TabMine',
		data() {
			return {
				statusBarHeight: 20,
				nickName: '',
				avatar: '',
				connected: false,
				deviceName: '未连接设备',
				batteryLevel: null,
				batteryCharging: false,
				boundDevice: null,
				serverDevice: null,
				unsub: null,
				showContactPopup: false,
				showContactForm: false,
				showRelationPicker: false,
				showSosPopup: false,
				editingIndex: -1,
				contacts: [],
				relationOptions: [],
				savingContact: false,
				sosTarget: null,
				form: {
					name: '',
					relation: '',
					phone: '',
					avatar: '/static/morentouxiang.png'
				},
				featureList: [{
						title: '紧急联系人',
						desc: '未设置',
						key: 'contact',
						icon: '/static/jinjilianxi.png'
					},
					{
						title: 'SOS 紧急联络',
						desc: '向联系人发送求助',
						key: 'sos',
						icon: '/static/sos.png'
					},
					{
						title: '我的报告',
						desc: '健康报告与 AI 解读',
						key: 'report',
						icon: '/static/minBao.png'
					}
				],
				settingList: [{
						title: '设置隐私',
						desc: '权限、隐私、账号安全',
						key: 'privacy',
						icon: '/static/setting.png'
					},
					{
						title: '帮助与反馈',
						desc: '常见问题与设备指引',
						key: 'help',
						icon: '/static/bangzhu.png'
					}
				]
			}
		},
		computed: {
			profileTip() {
				return this.nickName ? '点击查看个人资料' : '点击完善个人资料'
			},
			hasBoundDevice() {
				return !!(this.boundDevice || this.serverDevice || this.connected)
			},
			deviceStatusText() {
				if (!this.connected && !this.hasBoundDevice) {
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
			},
			sosTargetName() {
				return (this.sosTarget && this.sosTarget.name) || '紧急联系人'
			},
			sosTargetPhone() {
				return this.maskPhone(this.sosTarget && this.sosTarget.phone)
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight + 20 || 20
			this.nickName = uni.getStorageSync('nickName') || ''
			this.avatar = uni.getStorageSync('avatar') || ''
			this.unsub = BleService.subscribe((s) => {
				this.connected = !!s.connected
				this.batteryLevel = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				this.boundDevice = s.device
				this.syncDeviceName()
			})
		},
		beforeDestroy() {
			if (this.unsub) {
				this.unsub()
				this.unsub = null
			}
		},
		methods: {
			onTabShow() {
				this.loadMine()
			},
			syncDeviceName() {
				if (this.connected && this.boundDevice && this.boundDevice.name) {
					this.deviceName = this.boundDevice.name
					return
				}
				if (this.connected) {
					this.deviceName = '智能戒指'
					return
				}
				this.deviceName = '未连接设备'
			},
			async loadMine() {
				await Promise.all([
					this.loadUser(),
					this.loadContacts(),
					this.loadRelations(),
					this.loadServerDevice()
				])
			},
			async loadUser() {
				try {
					const info = await getUserInfo()
					if (!info) return
					saveLoginSession(info)
					this.nickName = info.nickName || info.nickname || this.nickName
					this.avatar = info.avatar || this.avatar
				} catch (e) {}
			},
			async loadServerDevice() {
				// 仅用本地：不请求接口，未连接守备时显示未连接设备
				try {
					const s = BleService.getState()
					this.boundDevice = s.device || this.boundDevice
					this.connected = !!s.connected
					if (s.batteryLevel != null) this.batteryLevel = s.batteryLevel
					this.batteryCharging = !!s.batteryCharging
					this.serverDevice = null
				} catch (e) {
					this.serverDevice = null
				}
				this.syncDeviceName()
			},
			async loadContacts() {
				try {
					const data = await getEmergencyList()
					this.contacts = asArray(data).map(normalizeContact).filter(Boolean)
				} catch (e) {
					// 保留当前列表，避免闪空
				}
				this.syncContactDesc()
			},
			async loadRelations() {
				try {
					const data = await getEmergencyRelations()
					this.relationOptions = normalizeRelations(data)
				} catch (e) {
					this.relationOptions = []
				}
			},
			syncContactDesc() {
				const item = this.featureList.find((f) => f.key === 'contact')
				if (!item) return
				item.desc = this.contacts.length ? `已设置${this.contacts.length}人` : '未设置'
			},
			maskPhone(phone) {
				const p = String(phone || '')
				if (p.length < 7) return p || '--'
				return `${p.slice(0, 3)}****${p.slice(-4)}`
			},
			openRelationPicker() {
				if (!this.relationOptions.length) {
					this.loadRelations()
					return
				}
				this.showRelationPicker = true
			},
			closeRelationPicker() {
				this.showRelationPicker = false
			},
			selectRelation(item) {
				if (!item) return
				this.form.relation = item
				this.closeRelationPicker()
			},
			onProfile() {
				uni.navigateTo({
					url: '/pages/mine/myInfo?from=mine'
				})
			},
			onManage() {
				const s = BleService.getState()
				if (s.device || s.connected || this.serverDevice) {
					const name = encodeURIComponent(
						(s.device && s.device.name) ||
						(this.serverDevice && (this.serverDevice.deviceName || this.serverDevice.name)) ||
						'智能戒指'
					)
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
			onMenu(item) {
				if (!item) return
				if (item.key === 'contact') {
					this.openContactPopup()
					return
				}
				if (item.key === 'sos') {
					this.openSosPopup()
					return
				}
				if (item.key === 'privacy') {
					uni.navigateTo({
						url: '/pages/mine/settingPrivacy'
					})
					return
				}
				if (item.key === 'report') {
					uni.navigateTo({
						url: '/pages/health/aiReport'
					})
					return
				}
				
				if (item.key === 'help') {
					uni.navigateTo({
						url: '/pages/mine/helpFeedback'
					})
					return
				}
				uni.showToast({
					title: '功能开发中',
					icon: 'none'
				})
			},
			async openSosPopup() {
				await this.loadContacts()
				if (!this.contacts.length) {
					uni.showToast({
						title: '请先添加紧急联系人',
						icon: 'none'
					})
					this.openContactPopup()
					return
				}
				this.sosTarget = this.contacts[0]
				this.showSosPopup = true
			},
			closeSosPopup() {
				this.showSosPopup = false
			},
			confirmSos() {
				const phone = this.sosTarget && this.sosTarget.phone
				if (!phone) {
					uni.showToast({ title: '联系人手机号无效', icon: 'none' })
					return
				}
				this.closeSosPopup()
				// #ifdef APP-PLUS
				uni.makePhoneCall({
					phoneNumber: String(phone),
					fail: () => {
						uni.showToast({ title: '拨打失败', icon: 'none' })
					}
				})
				// #endif
				// #ifndef APP-PLUS
				uni.showToast({ title: '已发起SOS求助', icon: 'none' })
				// #endif
			},
			async openContactPopup() {
				this.showContactPopup = true
				await Promise.all([this.loadContacts(), this.loadRelations()])
			},
			closeContactPopup() {
				this.showContactPopup = false
			},
			openContactForm(item, index) {
				if (item && index >= 0) {
					this.editingIndex = index
					this.form = {
						name: item.name || '',
						relation: item.relation || '',
						phone: item.phone || '',
						avatar: item.avatar || '/static/morentouxiang.png'
					}
				} else {
					this.editingIndex = -1
					this.form = {
						name: '',
						relation: this.relationOptions[0] || '',
						phone: '',
						avatar: '/static/morentouxiang.png'
					}
				}
				this.showContactForm = true
				if (!this.relationOptions.length) {
					this.loadRelations()
				}
			},
			closeContactForm() {
				this.showRelationPicker = false
				this.showContactForm = false
				this.editingIndex = -1
			},
			confirmDeleteContact(item, index) {
				if (!item) return
				const name = item.name || '该联系人'
				uni.showModal({
					title: '删除确认',
					content: `确定删除紧急联系人「${name}」吗？`,
					confirmText: '删除',
					confirmColor: '#ff4d4f',
					success: (res) => {
						if (res.confirm) this.deleteContact(item, index)
					}
				})
			},
			async deleteContact(item, index) {
				uni.showLoading({ title: '删除中', mask: true })
				try {
					if (item.id != null && item.id !== '') {
						await removeEmergencyContact(item.id)
					}
					this.contacts.splice(index, 1)
					if (this.sosTarget && item.id != null && this.sosTarget.id === item.id) {
						this.sosTarget = this.contacts[0] || null
					}
					this.syncContactDesc()
					uni.showToast({ title: '已删除', icon: 'none' })
				} catch (e) {
					// http 层已 toast
				} finally {
					uni.hideLoading()
				}
			},
			async saveContactForm() {
				if (this.savingContact) return
				const name = (this.form.name || '').trim()
				const relation = (this.form.relation || '').trim()
				const phone = String(this.form.phone || '').trim()
				if (!name) {
					uni.showToast({ title: '请输入姓名', icon: 'none' })
					return
				}
				if (!relation) {
					uni.showToast({ title: '请输入关系', icon: 'none' })
					return
				}
				if (!PHONE_REG.test(phone)) {
					uni.showToast({ title: '请输入正确手机号', icon: 'none' })
					return
				}
				const editing = this.editingIndex >= 0 ? this.contacts[this.editingIndex] : null
				this.savingContact = true
				try {
					if (editing && editing.id != null) {
						await editEmergencyContact({
							contactId: editing.id,
							contactName: name,
							relation,
							phone
						})
					} else {
						await addEmergencyContact({
							contactName: name,
							relation,
							phone
						})
					}
					await this.loadContacts()
					this.closeContactForm()
					uni.showToast({ title: '已保存', icon: 'none' })
				} catch (e) {
					// http 层已 toast
				} finally {
					this.savingContact = false
				}
			}
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
	.battery-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 8rpx;
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
		font-size: 30rpx;
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

	.contact-mask {
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

	.contact-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
	}

	.contact-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.contact-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.contact-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.contact-popup-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.contact-list {
		margin-top: 36rpx;
	}

	.contact-card {
		display: flex;
		align-items: center;
		padding: 24rpx 24rpx 24rpx 20rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.contact-card + .contact-card {
		margin-top: 16rpx;
	}

	.contact-avatar {
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.1);
		flex-shrink: 0;
	}

	.contact-info {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.contact-name {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.contact-meta {
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.contact-edit-btn {
		width: 142rpx;
		height: 64rpx;
	}

	.contact-card-actions {
		flex-shrink: 0;
		margin-left: 16rpx;
		display: flex;
		align-items: center;
	}

	.contact-card-actions .contact-edit-btn {
		margin-left: 0;
	}

	.contact-del-btn {
		flex-shrink: 0;
		margin-left: 12rpx;
		height: 52rpx;
		width: 142rpx;
		height: 64rpx;
		padding: 0 28rpx;
		border-radius: 999rpx;
		border: 2rpx solid #ff4d4f;
		font-size: 24rpx;
		line-height:64rpx;
		color: #ff5a5a;
		text-align: center;
		box-sizing: border-box;
	}

	.contact-empty {
		margin-top: 36rpx;
		padding: 48rpx 24rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.06);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.contact-empty-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.contact-actions {
		margin-top: 40rpx;
		display: flex;
		flex-direction: column;
	}

	.contact-btn {
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

	.contact-btn + .contact-btn {
		margin-top: 20rpx;
	}

	.contact-btn.outline {
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.contact-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.contact-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.form-field {
		margin-top: 28rpx;
	}

	.form-label {
		display: block;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.7);
	}

	.form-input {
		margin-top: 12rpx;
		height: 88rpx;
		padding: 0 28rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		font-size: 28rpx;
		color: #ffffff;
		box-sizing: border-box;
	}

	.form-placeholder {
		color: #636366;
	}

	.form-picker {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.form-picker-arrow {
		width: 28rpx;
		height: 28rpx;
		flex-shrink: 0;
		margin-left: 16rpx;
		opacity: 0.55;
	}

	.relation-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 3000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.relation-sheet {
		width: 100%;
		margin: 0 30rpx 60rpx;
		padding: 32rpx 28rpx 28rpx;
		border-radius: 32rpx;
		background: #1a1b25;
		box-sizing: border-box;
	}

	.relation-title {
		display: block;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
		text-align: center;
	}

	.relation-list {
		margin-top: 24rpx;
		max-height: 560rpx;
		overflow-y: auto;
	}

	.relation-item {
		height: 88rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.04);
	}

	.relation-item + .relation-item {
		margin-top: 12rpx;
	}

	.relation-item.active {
		background: #2f7bff;
	}

	.relation-item-text {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.relation-cancel {
		margin-top: 20rpx;
		height: 88rpx;
		border-radius: 16rpx;
		border: 2rpx solid #3b82f6;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.relation-cancel-text {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.sos-popup-body {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
	}

	.sos-illust {
		display: block;
		width: 125.03rpx;
		height: 148rpx;
		margin: 40rpx auto 0;
	}

	.sos-ask {
		display: block;
		width: 100%;
		margin-top: 28rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
		text-align: center;
	}

	.sos-desc-wrap {
		width: 100%;
		margin-top: 12rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.sos-desc {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.55);
		text-align: center;
	}

	.sos-phone {
		margin-top: 4rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #4da3ff;
		text-align: center;
	}

	.sos-btns {
		margin-top: 40rpx;
		width: 100%;
		display: flex;
		align-items: center;
	}

	.sos-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.sos-btn.ghost {
		margin-right: 20rpx;
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.sos-btn.solid {
		background: transparent;
	}

	.sos-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.sos-btn-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}
</style>