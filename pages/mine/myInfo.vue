<template>
	<view class="page">
		<!-- 头部 -->
		<view class="header">
			<view class="header-left">
				<text class="title">{{ fromMine ? '个人资料' : '完善基础信息' }}</text>
				<text class="subtitle">{{ fromMine ? '编辑个人资料信息' : '完善个人资料信息' }}</text>
			</view>
			<view v-if="!fromMine" class="step-badge">
				<view class="step-badge-text">首次使用 · <text style="color: #2C89FF;">1</text> /2</view>
			</view>
		</view>

		<!-- 头像 -->
		<view class="avatar-row" @click="onChooseAvatar">
			<image class="avatar-img" :src="avatar || '/static/defaultAvatar.png'" mode="aspectFill"></image>
			<view class="avatar-tip">
				<text class="avatar-title">设置头像</text>
				<text class="avatar-sub">{{ uploading ? '上传中...' : '点击更换头像' }}</text>
			</view>
			<text class="avatar-arrow">›</text>
		</view>

		<!-- 表单 -->
		<view class="form">
			<!-- 昵称 -->
			<view class="field">
				<text class="field-label">昵称</text>
				<input
					class="field-input"
					type="text"
					v-model="nickname"
					placeholder="请输入昵称"
					placeholder-class="field-placeholder"
					maxlength="20"
				/>
			</view>

			<!-- 性别 -->
			<view class="field">
				<text class="field-label">性别</text>
				<view class="gender-row">
					<view
						class="gender-item"
						:class="{ active: gender === 'male' }"
						@click="gender = 'male'"
					>
						<image class="gender-icon" src="/static/nan.png" mode=""></image>
						<text class="gender-text">男</text>
					</view>
					<view
						class="gender-item"
						:class="{ active: gender === 'female' }"
						@click="gender = 'female'"
					>
						<image class="gender-icon" src="/static/nv.png" mode=""></image>
						<text class="gender-text">女</text>
					</view>
				</view>
			</view>

			<!-- 出生日期 -->
			<view class="field">
				<text class="field-label">出生日期</text>
				<view class="field-picker" @click="openBirthdayPicker">
					<text class="field-picker-text" :class="{ 'field-placeholder': !birthday }">
						{{ birthday || '请选择出生日期' }}
					</text>
					<image class="field-calendar" src="/static/rili.png" mode=""></image>
				</view>
			</view>

			<!-- 身高 -->
			<view class="field">
				<text class="field-label">身高 (Cm)</text>
				<input
					class="field-input"
					type="digit"
					v-model="height"
					placeholder="请输入身高"
					placeholder-class="field-placeholder"
				/>
			</view>

			<!-- 体重 -->
			<view class="field">
				<text class="field-label">体重 (Kg)</text>
				<input
					class="field-input"
					type="digit"
					v-model="weight"
					placeholder="请输入体重"
					placeholder-class="field-placeholder"
				/>
			</view>
		</view>

		<!-- 底部操作 -->
		<view class="footer">
			<view class="save-btn" @click="onSave">
				<image class="save-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="save-btn-text">{{ fromMine ? '保存' : '保存并继续' }}</text>
			</view>
			<text v-if="!fromMine" class="skip-link" @click="onSkip">稍后完善</text>
		</view>

		<!-- 自定义日期选择器：三列滚轮，安卓/iOS 表现一致，不依赖系统 picker -->
		<view v-if="datePickerVisible" class="dp-mask" @click="closeBirthdayPicker" @touchmove.stop.prevent>
			<view class="dp-sheet" @click.stop @touchmove.stop>
				<view class="dp-toolbar">
					<text class="dp-toolbar-btn" @click="closeBirthdayPicker">取消</text>
					<text class="dp-toolbar-title">选择出生日期</text>
					<text class="dp-toolbar-btn dp-toolbar-confirm" @click="confirmBirthdayPicker">确定</text>
				</view>
				<view class="dp-wheel">
					<view class="dp-indicator"></view>
					<view class="dp-fade dp-fade-top"></view>
					<view class="dp-fade dp-fade-bottom"></view>
					<view class="dp-columns">
						<scroll-view
							class="dp-col"
							scroll-y
							:show-scrollbar="false"
							:scroll-with-animation="wheelAnimate"
							:scroll-top="scrollTop.year"
							@scroll="onWheelScroll('year', $event)"
						>
							<view class="dp-pad"></view>
							<view
								v-for="(item, idx) in yearList"
								:key="'y' + item"
								class="dp-item"
								:class="{ 'dp-item-active': idx === yearIndex }"
								@click="onItemTap('year', idx)"
							>{{ item }}年</view>
							<view class="dp-pad"></view>
						</scroll-view>
						<scroll-view
							class="dp-col"
							scroll-y
							:show-scrollbar="false"
							:scroll-with-animation="wheelAnimate"
							:scroll-top="scrollTop.month"
							@scroll="onWheelScroll('month', $event)"
						>
							<view class="dp-pad"></view>
							<view
								v-for="(item, idx) in monthList"
								:key="'m' + item"
								class="dp-item"
								:class="{ 'dp-item-active': idx === monthIndex }"
								@click="onItemTap('month', idx)"
							>{{ item }}月</view>
							<view class="dp-pad"></view>
						</scroll-view>
						<scroll-view
							class="dp-col"
							scroll-y
							:show-scrollbar="false"
							:scroll-with-animation="wheelAnimate"
							:scroll-top="scrollTop.day"
							@scroll="onWheelScroll('day', $event)"
						>
							<view class="dp-pad"></view>
							<view
								v-for="(item, idx) in dayList"
								:key="'d' + item"
								class="dp-item"
								:class="{ 'dp-item-active': idx === dayIndex }"
								@click="onItemTap('day', idx)"
							>{{ item }}日</view>
							<view class="dp-pad"></view>
						</scroll-view>
					</view>
				</view>
				<view class="dp-safe"></view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getUserInfo, editUserInfo } from '@/common/api/user.js'
	import { uploadFile } from '@/common/api/file.js'
	import { saveLoginSession, USER_ID_KEY, USER_INFO_KEY } from '@/common/auth.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function toDateStr(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}

	function mapGenderToUi(gender) {
		const g = String(gender == null ? '' : gender)
		if (g === '1' || g === 'male' || g === '男') return 'male'
		if (g === '2' || g === 'female' || g === '女') return 'female'
		return ''
	}

	function mapGenderToApi(gender) {
		if (gender === 'male') return '1'
		if (gender === 'female') return '2'
		return '0'
	}

	export default {
		data() {
			return {
				fromMine: false,
				saving: false,
				uploading: false,
				userId: null,
				avatar: '',
				nickname: '',
				gender: '',
				birthday: '',
				height: '',
				weight: '',
				// 自定义日期选择器
				datePickerVisible: false,
				wheelAnimate: false,
				itemH: 44,
				yearList: [],
				monthList: [],
				dayList: [],
				yearIndex: 0,
				monthIndex: 0,
				dayIndex: 0,
				scrollTop: { year: 0, month: 0, day: 0 }
			}
		},
		onLoad(query) {
			this.fromMine = !!(query && (query.from === 'mine' || query.mode === 'edit'))
			this.applyCached()
			this.loadUser()
		},
		onUnload() {
			if (this._snapTimers) {
				Object.keys(this._snapTimers).forEach((k) => clearTimeout(this._snapTimers[k]))
			}
		},
		methods: {
			applyCached() {
				const cached = uni.getStorageSync('userBasicInfo') || {}
				const userInfo = uni.getStorageSync(USER_INFO_KEY) || {}
				const src = Object.keys(cached).length ? cached : userInfo
				this.userId = uni.getStorageSync(USER_ID_KEY) || userInfo.userId || null
				this.avatar = src.avatar || uni.getStorageSync('avatar') || this.avatar
				this.nickname = src.nickName || src.nickname || this.nickname
				this.gender = mapGenderToUi(src.gender) || this.gender
				this.birthday = src.birthday || this.birthday
				if (src.height != null && src.height !== '') {
					this.height = String(src.height)
				}
				if (src.weight != null && src.weight !== '') {
					this.weight = String(src.weight)
				}
			},
			async loadUser() {
				try {
					const info = await getUserInfo()
					if (!info) return
					saveLoginSession(info)
					this.userId = info.userId != null ? info.userId : this.userId
					this.avatar = info.avatar || this.avatar
					this.nickname = info.nickName || info.nickname || this.nickname
					this.gender = mapGenderToUi(info.gender) || this.gender
					this.birthday = info.birthday || this.birthday
					if (info.height != null && info.height !== '') {
						this.height = String(info.height)
					}
					if (info.weight != null && info.weight !== '') {
						this.weight = String(info.weight)
					}
				} catch (e) {}
			},
			parseBirthdayDate() {
				const str = String(this.birthday || '')
				const m = str.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
				if (m) {
					const y = Number(m[1])
					const mo = Number(m[2])
					const d = Number(m[3])
					if (y && mo && d) return { y, mo, d }
				}
				const now = new Date()
				// 默认 2000-01-01，若晚于今天则回退到今天
				if (new Date(2000, 0, 1) > now) {
					return { y: now.getFullYear(), mo: now.getMonth() + 1, d: now.getDate() }
				}
				return { y: 2000, mo: 1, d: 1 }
			},
			buildDateLists() {
				const curYear = new Date().getFullYear()
				const picked = this.parseBirthdayDate()
				const years = []
				for (let y = 1920; y <= curYear; y++) years.push(y)
				const months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
				this.yearList = years
				this.monthList = months
				const yi = years.indexOf(picked.y)
				const mi = months.indexOf(picked.mo)
				this.yearIndex = yi >= 0 ? yi : years.length - 1
				this.monthIndex = mi >= 0 ? mi : 0
				const max = new Date(picked.y, picked.mo, 0).getDate()
				const days = []
				for (let d = 1; d <= max; d++) days.push(d)
				this.dayList = days
				this.dayIndex = Math.max(0, Math.min(days.length - 1, picked.d - 1))
			},
			openBirthdayPicker() {
				this.itemH = uni.upx2px(88) || 44
				this.buildDateLists()
				if (this._snapTimers) {
					Object.keys(this._snapTimers).forEach((k) => clearTimeout(this._snapTimers[k]))
				}
				this.wheelAnimate = false
				this._wheelLive = {
					year: this.yearIndex * this.itemH,
					month: this.monthIndex * this.itemH,
					day: this.dayIndex * this.itemH
				}
				this.scrollTop = {
					year: this.yearIndex * this.itemH,
					month: this.monthIndex * this.itemH,
					day: this.dayIndex * this.itemH
				}
				this.datePickerVisible = true
				this.$nextTick(() => {
					// 部分机型 scroll-top 初始值不生效，挂载后再定位一次
					this.applyWheelScroll('year', this.yearIndex * this.itemH)
					this.applyWheelScroll('month', this.monthIndex * this.itemH)
					this.applyWheelScroll('day', this.dayIndex * this.itemH)
					setTimeout(() => {
						this.wheelAnimate = true
					}, 60)
				})
			},
			closeBirthdayPicker() {
				this.datePickerVisible = false
			},
			confirmBirthdayPicker() {
				const y = this.yearList[this.yearIndex] || 2000
				const m = this.monthList[this.monthIndex] || 1
				const d = this.dayList[this.dayIndex] || 1
				let date = new Date(y, m - 1, d)
				const now = new Date()
				if (date > now) date = now
				this.birthday = toDateStr(date)
				this.datePickerVisible = false
			},
			wheelList(type) {
				if (type === 'year') return this.yearList
				if (type === 'month') return this.monthList
				return this.dayList
			},
			wheelIndexFromTop(top, len) {
				if (!len) return 0
				const h = this.itemH || 44
				const idx = Math.round((Number(top) || 0) / h)
				return Math.max(0, Math.min(len - 1, idx))
			},
			onWheelScroll(type, e) {
				const top = e && e.detail ? Number(e.detail.scrollTop) : 0
				if (!this._wheelLive) this._wheelLive = {}
				this._wheelLive[type] = top < 0 ? 0 : top
				const idx = this.wheelIndexFromTop(top, this.wheelList(type).length)
				if (type === 'year') this.yearIndex = idx
				else if (type === 'month') this.monthIndex = idx
				else this.dayIndex = idx
				if (!this._snapTimers) this._snapTimers = {}
				if (this._snapTimers[type]) clearTimeout(this._snapTimers[type])
				// 滚动停止后吸附到最近的一格
				this._snapTimers[type] = setTimeout(() => this.snapWheel(type), 150)
			},
			snapWheel(type) {
				const list = this.wheelList(type)
				const live = (this._wheelLive && this._wheelLive[type]) || 0
				const idx = this.wheelIndexFromTop(live, list.length)
				const target = idx * this.itemH
				if (type === 'year') this.yearIndex = idx
				else if (type === 'month') this.monthIndex = idx
				else this.dayIndex = idx
				if (Math.abs(live - target) > 1.5) {
					this.applyWheelScroll(type, target)
				}
				if (type !== 'day') this.syncDayList()
			},
			applyWheelScroll(type, target) {
				// 用极小的正负偏移保证每次赋值都能触发 scroll-view 的滚动（目标值相同时也能吸附）
				this._wheelJitter = (this._wheelJitter || 0) + 1
				const eps = this._wheelJitter % 2 === 0 ? 0.5 : -0.5
				const next = { ...this.scrollTop }
				next[type] = Math.max(0, target + eps)
				this.scrollTop = next
				if (!this._wheelLive) this._wheelLive = {}
				this._wheelLive[type] = target
			},
			syncDayList() {
				const y = this.yearList[this.yearIndex]
				const m = this.monthList[this.monthIndex]
				if (!y || !m) return
				const max = new Date(y, m, 0).getDate()
				if (this.dayList.length === max) return
				const dayIdx = Math.min(this.dayIndex, max - 1)
				const days = []
				for (let d = 1; d <= max; d++) days.push(d)
				this.dayList = days
				this.dayIndex = dayIdx
				this.$nextTick(() => this.applyWheelScroll('day', dayIdx * this.itemH))
			},
			onItemTap(type, idx) {
				if (type === 'year') this.yearIndex = idx
				else if (type === 'month') this.monthIndex = idx
				else this.dayIndex = idx
				this.applyWheelScroll(type, idx * this.itemH)
				if (type !== 'day') this.syncDayList()
			},
			onChooseAvatar() {
				if (this.uploading) return
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => {
						const filePath = res.tempFilePaths && res.tempFilePaths[0]
						if (!filePath) return
						this.uploadAvatar(filePath)
					}
				})
			},
			async uploadAvatar(filePath) {
				this.uploading = true
				uni.showLoading({ title: '上传中...', mask: true })
				try {
					const data = await uploadFile(filePath)
					const url = typeof data === 'string' ? data : (data && (data.url || data.fileUrl || data.path))
					if (!url) {
						uni.showToast({ title: '上传失败，请重试', icon: 'none' })
						return
					}
					this.avatar = String(url)
					uni.setStorageSync('avatar', this.avatar)
				} catch (e) {
					// uploadFile 内部已 toast
				} finally {
					this.uploading = false
					uni.hideLoading()
				}
			},
			validate() {
				if (!this.nickname || !String(this.nickname).trim()) {
					uni.showToast({ title: '请输入昵称', icon: 'none' })
					return false
				}
				if (!this.gender) {
					uni.showToast({ title: '请选择性别', icon: 'none' })
					return false
				}
				if (!this.birthday) {
					uni.showToast({ title: '请选择出生日期', icon: 'none' })
					return false
				}
				const h = Number(this.height)
				const w = Number(this.weight)
				if (!h || h < 50 || h > 250) {
					uni.showToast({ title: '请输入有效身高', icon: 'none' })
					return false
				}
				if (!w || w < 20 || w > 300) {
					uni.showToast({ title: '请输入有效体重', icon: 'none' })
					return false
				}
				return true
			},
			buildPayload() {
				const nickName = String(this.nickname).trim()
				const height = Number(this.height)
				const weight = Number(this.weight)
				return {
					userId: this.userId || uni.getStorageSync(USER_ID_KEY),
					nickName,
					avatar: this.avatar || undefined,
					gender: mapGenderToApi(this.gender),
					birthday: this.birthday,
					height,
					weight
				}
			},
			persistLocal(payload) {
				uni.setStorageSync('userBasicInfo', {
					nickname: payload.nickName,
					nickName: payload.nickName,
					avatar: this.avatar,
					gender: this.gender,
					birthday: payload.birthday,
					height: payload.height,
					weight: payload.weight
				})
				saveLoginSession({
					userId: payload.userId,
					nickName: payload.nickName,
					avatar: this.avatar,
					gender: payload.gender,
					birthday: payload.birthday,
					height: payload.height,
					weight: payload.weight
				})
			},
			async onSave() {
				if (this.saving || !this.validate()) return
				const payload = this.buildPayload()
				if (payload.userId == null || payload.userId === '') {
					uni.showToast({ title: '用户信息缺失，请重新登录', icon: 'none' })
					return
				}
				this.saving = true
				try {
					await editUserInfo(payload)
					this.persistLocal(payload)
					uni.setStorageSync('needCompleteInfo', false)
					if (this.fromMine) {
						uni.showToast({ title: '已保存', icon: 'none' })
						setTimeout(() => {
							uni.navigateBack()
						}, 400)
						return
					}
					uni.redirectTo({
						url: '/pages/mine/bluePower'
					})
				} catch (e) {
					// http 层已 toast
				} finally {
					this.saving = false
				}
			},
			onSkip() {
				uni.setStorageSync('needCompleteInfo', false)
				uni.redirectTo({
					url: '/pages/mine/bluePower'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 32rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
		display: flex;
		flex-direction: column;
	}

	.header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
	}

	.header-left {
		flex: 1;
		min-width: 0;
		padding-right: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.title {
		font-size: 40rpx;
		font-weight: 700;
		line-height: 56rpx;
		color: #ffffff;
	}

	.subtitle {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.step-badge {
		flex-shrink: 0;
		padding: 10rpx 20rpx;
		border-radius: 999rpx;
		background: #1a1b25;
	}

	.step-badge-text {
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.avatar-row {
		margin-top: 48rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
	}

	.avatar-img {
		width: 120rpx;
		height: 120rpx;
		border-radius: 60rpx;
		background: #2a2b38;
		flex-shrink: 0;
	}

	.avatar-tip {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
	}

	.avatar-title {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.avatar-sub {
		margin-top: 6rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.avatar-arrow {
		font-size: 36rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.3);
		flex-shrink: 0;
	}

	.form {
		margin-top: 48rpx;
		flex: 0.5;
	}

	.field {
		margin-bottom: 32rpx;
	}

	.field-label {
		display: block;
		margin-bottom: 16rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.field-input {
		width: 100%;
		height: 96rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		box-sizing: border-box;
		font-size: 30rpx;
		color: #ffffff;
	}

	.field-placeholder {
		color: rgba(255, 255, 255, 0.3);
	}

	.gender-row {
		display: flex;
		align-items: center;
	}

	.gender-item {
		flex: 1;
		height: 96rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.gender-item + .gender-item {
		margin-left: 20rpx;
	}

	.gender-item.active {
		background: #2f7bff;
	}

	.gender-icon {
		width: 36rpx;
		height: 36rpx;
		margin-right: 12rpx;
	}

	.gender-text {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.field-picker {
		height: 96rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.field-picker-text {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.field-calendar {
		width: 36rpx;
		height: 36rpx;
		flex-shrink: 0;
	}

	.footer {
		padding-top: 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.save-btn {
		position: relative;
		width: 100%;
		height: 96rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.save-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.save-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.skip-link {
		margin-top: 28rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	/* ===== 自定义日期选择器 ===== */
	.dp-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 999;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
	}

	.dp-sheet {
		width: 100%;
		background: #14151d;
		border-top-left-radius: 32rpx;
		border-top-right-radius: 32rpx;
		overflow: hidden;
	}

	.dp-toolbar {
		position: relative;
		height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 32rpx;
		box-sizing: border-box;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
	}

	.dp-toolbar-title {
		position: absolute;
		left: 0;
		right: 0;
		text-align: center;
		font-size: 30rpx;
		font-weight: 600;
		color: #ffffff;
		pointer-events: none;
	}

	.dp-toolbar-btn {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		color: rgba(255, 255, 255, 0.5);
	}

	.dp-toolbar-confirm {
		color: #2c89ff;
		font-weight: 600;
	}

	.dp-wheel {
		position: relative;
		height: 440rpx;
		overflow: hidden;
	}

	.dp-indicator {
		position: absolute;
		left: 24rpx;
		right: 24rpx;
		top: 176rpx;
		height: 88rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		border-top: 1px solid rgba(255, 255, 255, 0.1);
		border-bottom: 1px solid rgba(255, 255, 255, 0.1);
		z-index: 1;
		pointer-events: none;
	}

	.dp-fade {
		position: absolute;
		left: 0;
		right: 0;
		height: 150rpx;
		z-index: 2;
		pointer-events: none;
	}

	.dp-fade-top {
		top: 0;
		background: linear-gradient(180deg, #14151d 0%, rgba(20, 21, 29, 0) 100%);
	}

	.dp-fade-bottom {
		bottom: 0;
		background: linear-gradient(0deg, #14151d 0%, rgba(20, 21, 29, 0) 100%);
	}

	.dp-columns {
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		display: flex;
		z-index: 0;
	}

	.dp-col {
		flex: 1;
		height: 440rpx;
	}

	.dp-pad {
		height: 176rpx;
	}

	.dp-item {
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 30rpx;
		color: rgba(255, 255, 255, 0.35);
	}

	.dp-item-active {
		color: #ffffff;
		font-weight: 600;
	}

	.dp-safe {
		height: constant(safe-area-inset-bottom);
		height: env(safe-area-inset-bottom);
	}
</style>
