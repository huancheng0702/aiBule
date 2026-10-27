<template>
	<view class="page">
		<view class="header">
			<text class="title">设置经期</text>
			<text class="desc">请填写你的经期数据，用于智能预测经期。每月持续记录可以提高预测准确性。</text>
		</view>

		<view class="form-card">
			<!-- 最近经期开始日 -->
			<view class="form-row border" @click="openPicker('date')">
				<!-- 粉色爱心图标，自行补充 src -->
				<image class="row-icon" src="/static/xinlv.png" mode=""></image>
				<view class="row-main">
					<text class="row-label">最近经期开始日</text>
					<text class="row-hint">选择最近一次开始日期</text>
				</view>
				<view class="row-right">
					<text class="row-value">{{ startDateText }}</text>
					<image class="row-calendar" src="/static/dateIcon.png" mode=""></image>
				</view>
			</view>

			<!-- 经期持续 -->
			<view class="form-row border" @click="openPicker('duration')">
				<!-- 紫色水滴图标，自行补充 src -->
				<image class="row-icon" src="/static/peiShuidi.png" mode=""></image>
				<view class="row-main">
					<text class="row-label">经期持续</text>
					<text class="row-hint">通常为3-7天</text>
				</view>
				<view class="row-right">
					<text class="row-value">{{ duration }} 天</text>
					<image class="row-arrow" src="/static/rightLitterIcon.png" mode=""></image>
				</view>
			</view>

			<!-- 平均周期 -->
			<view class="form-row" @click="openPicker('cycle')">
				<!-- 青色日历图标，自行补充 src -->
				<image class="row-icon" src="/static/peiDate.png" mode=""></image>
				<view class="row-main">
					<text class="row-label">平均周期</text>
					<text class="row-hint">通常为21-35天</text>
				</view>
				<view class="row-right">
					<text class="row-value">{{ cycle }} 天</text>
					<image class="row-arrow" src="/static/rightLitterIcon.png" mode=""></image>
				</view>
			</view>
		</view>

		<view class="footer">
			<view class="start-btn" @click="onStart">
				<!-- 按钮背景图，自行补充 src -->
				<image class="start-btn-bg" src="/static/btnBan.png" mode=""></image>
				<text class="start-btn-text">开始使用</text>
			</view>
		</view>

		<!-- 自定义滚轮选择器：安卓/iOS 表现一致，不依赖系统 picker -->
		<view v-if="pickerVisible" class="dp-mask" @click="closePicker" @touchmove.stop.prevent>
			<view class="dp-sheet" @click.stop @touchmove.stop>
				<view class="dp-toolbar">
					<text class="dp-toolbar-btn" @click="closePicker">取消</text>
					<text class="dp-toolbar-title">{{ pickerTitle }}</text>
					<text class="dp-toolbar-btn dp-toolbar-confirm" @click="confirmPicker">确定</text>
				</view>
				<view class="dp-wheel">
					<view class="dp-indicator"></view>
					<view class="dp-fade dp-fade-top"></view>
					<view class="dp-fade dp-fade-bottom"></view>
					<view class="dp-columns">
						<scroll-view
							v-for="col in pickerCols"
							:key="col.key"
							class="dp-col"
							scroll-y
							:show-scrollbar="false"
							:scroll-with-animation="wheelAnimate"
							:scroll-top="col.scrollTop"
							@scroll="onWheelScroll(col.key, $event)"
						>
							<view class="dp-pad"></view>
							<view
								v-for="(item, idx) in col.items"
								:key="col.key + idx"
								class="dp-item"
								:class="{ 'dp-item-active': idx === col.index }"
								@click="onItemTap(col.key, idx)"
							>{{ item.label }}</view>
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
	import { getPeriodStatistics, initPeriod } from '@/common/api/period.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function toPickerValue(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}

	export default {
		data() {
			const now = new Date()
			return {
				startDate: toPickerValue(now),
				duration: 5,
				cycle: 28,
				todayStr: toPickerValue(now),
				saving: false,
				// 自定义滚轮选择器
				pickerVisible: false,
				pickerType: '',
				pickerTitle: '',
				wheelAnimate: false,
				itemH: 44,
				pickerCols: []
			}
		},
		computed: {
			startDateText() {
				const parts = this.startDate.split('-')
				if (parts.length !== 3) return this.startDate
				return `${parts[0]}/${parts[1]}/${parts[2]}`
			}
		},
		onShow() {
			this.todayStr = toPickerValue(new Date())
			this.loadStatistics()
		},
		onUnload() {
			if (this._snapTimers) {
				Object.keys(this._snapTimers).forEach((k) => clearTimeout(this._snapTimers[k]))
			}
		},
		methods: {
			async loadStatistics() {
				try {
					const stats = await getPeriodStatistics()
					if (!stats) return
					const start = stats.lastStartDate || stats.startDate
					if (start) this.startDate = String(start).slice(0, 10)
					if (Number(stats.duration) > 0) this.duration = Number(stats.duration)
					if (Number(stats.cycleLength || stats.cycle) > 0) {
						this.cycle = Number(stats.cycleLength || stats.cycle)
					}
				} catch (e) {
					console.log('[setPeriod] statistics failed', e && (e.msg || e.message))
				}
			},
			parseDate(str) {
				const m = String(str || '').match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
				if (m) {
					return { y: Number(m[1]), mo: Number(m[2]), d: Number(m[3]) }
				}
				const now = new Date()
				return { y: now.getFullYear(), mo: now.getMonth() + 1, d: now.getDate() }
			},
			buildDateCols() {
				const picked = this.parseDate(this.startDate)
				const curYear = new Date().getFullYear()
				const years = []
				for (let y = curYear - 5; y <= curYear; y++) {
					years.push({ label: y + '年', value: y })
				}
				const months = []
				for (let m = 1; m <= 12; m++) {
					months.push({ label: m + '月', value: m })
				}
				const max = new Date(picked.y, picked.mo, 0).getDate()
				const days = []
				for (let d = 1; d <= max; d++) {
					days.push({ label: d + '日', value: d })
				}
				const yearIndex = Math.max(0, years.findIndex((it) => it.value === picked.y))
				const monthIndex = Math.max(0, months.findIndex((it) => it.value === picked.mo))
				const dayIndex = Math.max(0, Math.min(days.length - 1, picked.d - 1))
				return [
					{ key: 'year', items: years, index: yearIndex, scrollTop: 0 },
					{ key: 'month', items: months, index: monthIndex, scrollTop: 0 },
					{ key: 'day', items: days, index: dayIndex, scrollTop: 0 }
				]
			},
			buildNumberCols(type) {
				const start = type === 'duration' ? 1 : 15
				const end = type === 'duration' ? 15 : 45
				const current = type === 'duration' ? this.duration : this.cycle
				const items = []
				for (let n = start; n <= end; n++) {
					items.push({ label: n + ' 天', value: n })
				}
				const index = Math.max(0, items.findIndex((it) => it.value === current))
				return [{ key: type, items, index, scrollTop: 0 }]
			},
			openPicker(type) {
				this.pickerType = type
				this.itemH = uni.upx2px(88) || 44
				if (type === 'date') {
					this.pickerTitle = '选择开始日期'
					this.pickerCols = this.buildDateCols()
				} else if (type === 'duration') {
					this.pickerTitle = '经期持续天数'
					this.pickerCols = this.buildNumberCols('duration')
				} else {
					this.pickerTitle = '平均周期天数'
					this.pickerCols = this.buildNumberCols('cycle')
				}
				if (this._snapTimers) {
					Object.keys(this._snapTimers).forEach((k) => clearTimeout(this._snapTimers[k]))
				}
				this.wheelAnimate = false
				this._wheelLive = {}
				this.pickerCols.forEach((col) => {
					this._wheelLive[col.key] = col.index * this.itemH
					col.scrollTop = col.index * this.itemH
				})
				this.pickerVisible = true
				this.$nextTick(() => {
					this.pickerCols.forEach((col) => {
						this.applyWheelScroll(col.key, col.index * this.itemH)
					})
					setTimeout(() => {
						this.wheelAnimate = true
					}, 60)
				})
			},
			closePicker() {
				this.pickerVisible = false
			},
			confirmPicker() {
				if (this.pickerType === 'date') {
					const y = this.colValue('year', new Date().getFullYear())
					const mo = this.colValue('month', 1)
					const d = this.colValue('day', 1)
					let date = new Date(y, mo - 1, d)
					const now = new Date()
					if (date > now) date = now
					this.startDate = toPickerValue(date)
				} else if (this.pickerType === 'duration') {
					this.duration = this.colValue('duration', this.duration)
				} else if (this.pickerType === 'cycle') {
					this.cycle = this.colValue('cycle', this.cycle)
				}
				this.pickerVisible = false
			},
			colValue(key, fallback) {
				const col = this.pickerCols.find((c) => c.key === key)
				if (!col || !col.items[col.index]) return fallback
				return col.items[col.index].value
			},
			wheelCol(key) {
				return this.pickerCols.find((c) => c.key === key)
			},
			wheelIndexFromTop(top, len) {
				if (!len) return 0
				const h = this.itemH || 44
				const idx = Math.round((Number(top) || 0) / h)
				return Math.max(0, Math.min(len - 1, idx))
			},
			onWheelScroll(key, e) {
				const top = e && e.detail ? Number(e.detail.scrollTop) : 0
				if (!this._wheelLive) this._wheelLive = {}
				this._wheelLive[key] = top < 0 ? 0 : top
				const col = this.wheelCol(key)
				if (!col) return
				col.index = this.wheelIndexFromTop(top, col.items.length)
				if (!this._snapTimers) this._snapTimers = {}
				if (this._snapTimers[key]) clearTimeout(this._snapTimers[key])
				this._snapTimers[key] = setTimeout(() => this.snapWheel(key), 150)
			},
			snapWheel(key) {
				const col = this.wheelCol(key)
				if (!col) return
				const live = (this._wheelLive && this._wheelLive[key]) || 0
				const idx = this.wheelIndexFromTop(live, col.items.length)
				const target = idx * this.itemH
				col.index = idx
				if (Math.abs(live - target) > 1.5) {
					this.applyWheelScroll(key, target)
				}
				if (this.pickerType === 'date' && (key === 'year' || key === 'month')) {
					this.syncDayList()
				}
			},
			applyWheelScroll(key, target) {
				// 用极小的正负偏移保证每次赋值都能触发 scroll-view 的滚动
				this._wheelJitter = (this._wheelJitter || 0) + 1
				const eps = this._wheelJitter % 2 === 0 ? 0.5 : -0.5
				const col = this.wheelCol(key)
				if (!col) return
				col.scrollTop = Math.max(0, target + eps)
				if (!this._wheelLive) this._wheelLive = {}
				this._wheelLive[key] = target
			},
			syncDayList() {
				const yearCol = this.wheelCol('year')
				const monthCol = this.wheelCol('month')
				const dayCol = this.wheelCol('day')
				if (!yearCol || !monthCol || !dayCol) return
				const y = yearCol.items[yearCol.index] ? yearCol.items[yearCol.index].value : new Date().getFullYear()
				const m = monthCol.items[monthCol.index] ? monthCol.items[monthCol.index].value : 1
				const max = new Date(y, m, 0).getDate()
				if (dayCol.items.length === max) return
				const dayIdx = Math.min(dayCol.index, max - 1)
				const days = []
				for (let d = 1; d <= max; d++) {
					days.push({ label: d + '日', value: d })
				}
				dayCol.items = days
				dayCol.index = dayIdx
				this.$nextTick(() => this.applyWheelScroll('day', dayIdx * this.itemH))
			},
			onItemTap(key, idx) {
				const col = this.wheelCol(key)
				if (!col) return
				col.index = idx
				this.applyWheelScroll(key, idx * this.itemH)
				if (this.pickerType === 'date' && (key === 'year' || key === 'month')) {
					this.syncDayList()
				}
			},
			async onStart() {
				if (this.saving) return
				if (!this.startDate) {
					uni.showToast({ title: '请选择最近经期开始日', icon: 'none' })
					return
				}
				this.saving = true
				try {
					await initPeriod({
						lastStartDate: this.startDate,
						duration: this.duration,
						cycleLength: this.cycle
					})
					const pages = getCurrentPages()
					const prev = pages.length > 1 ? pages[pages.length - 2] : null
					if (prev && prev.route === 'pages/index/period') {
						uni.navigateBack()
					} else {
						uni.redirectTo({
							url: '/pages/index/period'
						})
					}
				} catch (e) {
					console.log('[setPeriod] init failed', e && (e.msg || e.message))
				} finally {
					this.saving = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx 32rpx 160rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.header {
		padding: 12rpx 4rpx 0;
	}

	.title {
		display: block;
		font-size: 48rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 68rpx;
	}

	.desc {
		display: block;
		margin-top: 16rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.55);
		line-height: 40rpx;
	}

	.form-card {
		margin-top: 40rpx;
		padding: 8rpx 28rpx;
		background: #1A1B25;
		border-radius: 24rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.form-row {
		display: flex;
		align-items: center;
		padding: 32rpx 0;
	}

	.form-row.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.row-icon {
		width: 56rpx;
		height: 56rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
	}

	.row-main {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.row-label {
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 42rpx;
	}

	.row-hint {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.row-right {
		display: flex;
		align-items: center;
		flex-shrink: 0;
		margin-left: 16rpx;
	}

	.row-value {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.row-calendar {
		width: 36rpx;
		height: 36rpx;
		margin-left: 10rpx;
	}

	.row-arrow {
		width: 24rpx;
		height: 24rpx;
		margin-left: 10rpx;
		transform: rotate(90deg);
	}

	.footer {
		margin-top: 610rpx;
		box-sizing: border-box;
		background: #000000;
	}

	.start-btn {
		position: relative;
		height: 96rpx;
		border-radius: 20rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.start-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.start-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	/* ===== 自定义滚轮选择器 ===== */
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
