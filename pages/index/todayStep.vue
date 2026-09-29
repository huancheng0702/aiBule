<template>
	<view class="page">
		<!-- 日期选择 -->
		<view class="week-bar">
			<view v-for="(item, index) in weekList" :key="index" class="week-item" :class="{ active: item.active }"
				@click="onSelectDay(index)">
				<!-- 选中背景图，自行补充 src -->
				<image v-if="item.active" src="/static/daySelect.png" class="week-bg" mode="aspectFill"></image>
				<text class="week-day">{{ item.day }}</text>
				<text class="week-date">{{ item.label }}</text>
			</view>
		</view>

		<!-- 活动概览 -->
		<view class="summary-card">
			<view class="summary-top">
				<!-- 真实三环进度图：底部起笔、渐变、外粗内细 -->
				<view class="rings">
					<image class="rings-svg" :src="ringsImageSrc" mode="aspectFit" />
				</view>

				<view class="summary-metrics">
					<view class="metric-row">
						<view class="dot yellow"></view>
						<text class="metric-label">步数</text>
						<text class="metric-value">{{ stepCurrent }} / {{ stepTarget }}步</text>
					</view>
					<view class="metric-row">
						<view class="dot red"></view>
						<text class="metric-label">消耗</text>
						<text class="metric-value">{{ calorieCurrent }} / {{ calorieTarget }}千卡</text>
					</view>
					<view class="metric-row">
						<view class="dot blue"></view>
						<text class="metric-label">距离</text>
						<text class="metric-value">{{ distanceCurrent }} / {{ distanceTarget }}千米</text>
					</view>
				</view>
			</view>

			<!-- 静态进度条，不可拖动 -->
			<view class="progress-block">
				<text class="progress-label">今日目标进度</text>
				<view class="progress-row">
					<view class="progress-track">
						<view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
					</view>
					<text class="progress-percent">{{ progressPercent }}%</text>
				</view>
			</view>
		</view>

		<!-- 目标设置：整卡 + 分隔线 -->
		<view class="section">
			<text class="section-title">目标设置</text>
			<view class="goal-card">
				<view v-for="(item, index) in goalList" :key="item.key" class="goal-item"
					:class="{ border: index < goalList.length - 1 }" @click="onGoalTap(item)">
					<text class="goal-name">{{ item.name }}</text>
					<view class="goal-right">
						<text class="goal-value">{{ item.value }}{{ item.unit }}</text>
						<!-- 箭头图标，自行补充 src -->
						<image class="goal-arrow" src="/static/minRight.png" mode="aspectFit"></image>
					</view>
				</view>
			</view>
		</view>

		<!-- 统计 -->
		<view class="section">
			<text class="section-title">统计</text>
			<view class="stats-card">
				<view class="stats-col">
					<text class="stats-label">最高值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsMax }}</text>
						<text class="stats-unit">步</text>
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">最低值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsMin }}</text>
						<text class="stats-unit">步</text>
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">平均值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsAvg }}</text>
						<text class="stats-unit">步</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 说明 -->
		<view class="tip-card">
			<view class="tip-header">
				<!-- 灯泡图标，自行补充 src -->
				<image class="tip-icon" mode="aspectFit"></image>
				<text class="tip-title">说明</text>
			</view>
			<text class="tip-text">{{ tipText }}</text>
		</view>

		<!-- 目标设置弹窗 -->
		<view v-if="showGoalPopup" class="goal-mask" @click="closeGoalPopup">
			<view class="goal-popup" @click.stop>
				<!-- 背景图，自行补充 src -->
				<image class="goal-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="goal-popup-body">
					<text class="goal-popup-title">{{ editingGoal.name }}</text>
					<text class="goal-popup-desc">信息仅用于展示，可随时修改</text>
					<text class="goal-popup-label">{{ editingGoal.name }}</text>
					<input class="goal-popup-input" type="number" v-model="goalInput"
						:placeholder="editingGoal.placeholder" placeholder-class="goal-popup-placeholder" />
					<view class="goal-popup-btns">
						<view class="goal-popup-btn cancel" @click="closeGoalPopup">取消</view>
						<view class="goal-popup-btn save" @click="saveGoalPopup">保存</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { toSvgDataUrl } from '@/common/svg-data-url.js'
	import BleService from '@/common/ble-service.js'
	import CoolWearBLE from '@/common/coolwear-ble.js'

	const GOAL_STORAGE_KEY = 'todayStepGoals'

	function ringDash(r, percent) {
		const c = 2 * Math.PI * r
		const p = Math.max(0, Math.min(1, percent))
		return `${c * p} ${c}`
	}

	function ringGroup(ring, gradId) {
		// 起笔方向仍由 .rings-svg 的 CSS rotate(90deg) 控制，与原 SVG 一致
		return (
			`<circle cx="100" cy="100" r="${ring.r + ring.stroke / 2}" fill="none" stroke-width="1.2" stroke="${ring.edge}"/>` +
			`<circle cx="100" cy="100" r="${ring.r}" fill="none" stroke-width="${ring.stroke}" stroke="${ring.track}"/>` +
			`<circle cx="100" cy="100" r="${ring.r - ring.stroke / 2}" fill="none" stroke-width="1.2" stroke="${ring.edge}"/>` +
			`<circle cx="100" cy="100" r="${ring.r}" fill="none" stroke-width="${ring.stroke + 2.5}" stroke="${ring.border}" stroke-dasharray="${ring.dash}" stroke-linecap="round"/>` +
			`<circle cx="100" cy="100" r="${ring.r}" fill="none" stroke-width="${ring.stroke}" stroke="url(#${gradId})" stroke-dasharray="${ring.dash}" stroke-linecap="round"/>`
		)
	}

	function safeRatio(current, target) {
		const t = Number(target)
		if (!t || t <= 0) return 0
		return Math.max(0, Math.min(1, Number(current) / t))
	}

	export default {
		data() {
			return {
				weekList: [],
				ringStroke: 8,
				stepCurrent: 0,
				stepTarget: 8000,
				calorieCurrent: 0,
				calorieTarget: 300,
				distanceCurrent: 0,
				distanceTarget: 6.0,
				showGoalPopup: false,
				goalInput: '',
				editingGoal: {
					key: '',
					name: '',
					placeholder: '',
					unit: ''
				},
				goalList: [{
						key: 'step',
						name: '步数目标',
						value: 8000,
						unit: '步',
						placeholder: '设置步数目标'
					},
					{
						key: 'calorie',
						name: '活动卡路里目标',
						value: 300,
						unit: '千卡',
						placeholder: '设置活动卡路里目标'
					},
					{
						key: 'duration',
						name: '活动时长目标',
						value: 45,
						unit: '分钟',
						placeholder: '设置活动时长目标'
					}
				],
				stepSamples: [],
				statsMax: 0,
				statsMin: 0,
				statsAvg: 0,
				bleConnected: false,
				unsubBle: null,
				tipText: '步数、距离与消耗由智能戒指实时统计。晃动或行走时会持续更新；进入本页会向戒指请求同步。本设备仅用于健康管理参考，不能用于医疗诊断。'
			}
		},
		computed: {
			progressPercent() {
				return Math.round(safeRatio(this.stepCurrent, this.stepTarget) * 100)
			},
			stepRatio() {
				return safeRatio(this.stepCurrent, this.stepTarget)
			},
			calorieRatio() {
				return safeRatio(this.calorieCurrent, this.calorieTarget)
			},
			distanceRatio() {
				return safeRatio(this.distanceCurrent, this.distanceTarget)
			},
			ringOuter() {
				const r = 86
				const stroke = 18
				return {
					r,
					stroke,
					track: '#292524',
					edge: 'rgba(255, 255, 255, 0.22)',
					border: '#E8C84A',
					dash: ringDash(r, this.stepRatio)
				}
			},
			ringMiddle() {
				const r = 58
				const stroke = 12
				return {
					r,
					stroke,
					track: '#221B24',
					edge: 'rgba(255, 255, 255, 0.2)',
					border: '#FF8A80',
					dash: ringDash(r, this.calorieRatio)
				}
			},
			ringInner() {
				const r = 34
				const stroke = 10
				return {
					r,
					stroke,
					track: '#172039',
					edge: 'rgba(255, 255, 255, 0.2)',
					border: '#7EC8FF',
					dash: ringDash(r, this.distanceRatio)
				}
			},
			ringsImageSrc() {
				const inner =
					`<defs>` +
					`<linearGradient id="gradYellow" x1="0%" y1="100%" x2="100%" y2="0%">` +
					`<stop offset="0%" stop-color="#BE8A1A"/><stop offset="50%" stop-color="#795D22"/><stop offset="100%" stop-color="#BE8A1A"/>` +
					`</linearGradient>` +
					`<linearGradient id="gradRed" x1="0%" y1="100%" x2="100%" y2="0%">` +
					`<stop offset="0%" stop-color="#FF6961"/><stop offset="50%" stop-color="#FF453A"/><stop offset="100%" stop-color="#FF2D55"/>` +
					`</linearGradient>` +
					`<linearGradient id="gradBlue" x1="0%" y1="100%" x2="100%" y2="0%">` +
					`<stop offset="0%" stop-color="#64D2FF"/><stop offset="50%" stop-color="#0A84FF"/><stop offset="100%" stop-color="#0055FF"/>` +
					`</linearGradient>` +
					`</defs>` +
					ringGroup(this.ringOuter, 'gradYellow') +
					ringGroup(this.ringMiddle, 'gradRed') +
					ringGroup(this.ringInner, 'gradBlue')
				return toSvgDataUrl(inner, 200, 200)
			},
			isTodaySelected() {
				const active = (this.weekList || []).find((item) => item.active)
				if (!active) return true
				const today = new Date()
				const y = today.getFullYear()
				const m = String(today.getMonth() + 1).padStart(2, '0')
				const d = String(today.getDate()).padStart(2, '0')
				return active.fullDate === `${y}-${m}-${d}`
			}
		},
		created() {
			this.initWeekList()
			this.loadGoals()
			this.unsubBle = BleService.subscribe((s) => {
				this.bleConnected = !!s.connected
				if (s.sport && this.isTodaySelected) {
					this.applySport(s.sport)
				}
			})
			const cached = BleService.getSport && BleService.getSport()
			if (cached) this.applySport(cached)
		},
		onShow() {
			this.refreshSport()
		},
		onUnload() {
			if (this.unsubBle) {
				this.unsubBle()
				this.unsubBle = null
			}
		},
		methods: {
			loadGoals() {
				try {
					const saved = uni.getStorageSync(GOAL_STORAGE_KEY)
					if (!saved) return
					if (saved.step > 0) {
						this.stepTarget = saved.step
						const g = this.goalList.find((i) => i.key === 'step')
						if (g) g.value = saved.step
					}
					if (saved.calorie > 0) {
						this.calorieTarget = saved.calorie
						const g = this.goalList.find((i) => i.key === 'calorie')
						if (g) g.value = saved.calorie
					}
					if (saved.duration > 0) {
						const g = this.goalList.find((i) => i.key === 'duration')
						if (g) g.value = saved.duration
					}
					if (saved.distance > 0) {
						this.distanceTarget = saved.distance
					}
				} catch (e) {
					// ignore
				}
			},
			persistGoals() {
				const durationGoal = this.goalList.find((i) => i.key === 'duration')
				uni.setStorageSync(GOAL_STORAGE_KEY, {
					step: this.stepTarget,
					calorie: this.calorieTarget,
					distance: this.distanceTarget,
					duration: durationGoal ? durationGoal.value : 45
				})
			},
			async refreshSport() {
				const ble = BleService.getState()
				if (!ble.connected) {
					const cached = BleService.getSport && BleService.getSport()
					if (cached) this.applySport(cached)
					return
				}
				try {
					await BleService.syncSport()
				} catch (e) {
					console.log('[todayStep] syncSport failed', e)
					try {
						await CoolWearBLE.sendSyncInfo()
					} catch (err) {
						console.log('[todayStep] sendSyncInfo failed', err)
					}
				}
			},
			applySport(sport) {
				if (!sport) return
				const steps = Number(sport.steps) || 0
				const calories = Number(sport.calories) || 0
				const distanceKm = sport.distanceKm != null
					? Number(sport.distanceKm)
					: (Number(sport.distanceM) || 0) / 1000

				this.stepCurrent = steps
				this.calorieCurrent = calories
				this.distanceCurrent = Math.round(distanceKm * 10) / 10

				if (steps > 0) {
					this.stepSamples = this.stepSamples.concat(steps).slice(-48)
					this.updateStats()
				}
				console.log('[todayStep] applySport', steps, calories, this.distanceCurrent)
			},
			updateStats() {
				const list = this.stepSamples
				if (!list.length) {
					this.statsMax = this.stepCurrent
					this.statsMin = this.stepCurrent
					this.statsAvg = this.stepCurrent
					return
				}
				this.statsMax = Math.max.apply(null, list)
				this.statsMin = Math.min.apply(null, list)
				const sum = list.reduce((a, b) => a + b, 0)
				this.statsAvg = Math.round(sum / list.length)
			},
			initWeekList() {
				const dayNames = ['日', '一', '二', '三', '四', '五', '六']
				const today = new Date()
				const list = []
				const weekStart = new Date(today)
				weekStart.setDate(today.getDate() - today.getDay())
				for (let i = 0; i < 7; i++) {
					const d = new Date(weekStart)
					d.setDate(weekStart.getDate() + i)
					const month = String(d.getMonth() + 1).padStart(2, '0')
					const date = String(d.getDate()).padStart(2, '0')
					const isToday =
						d.getFullYear() === today.getFullYear() &&
						d.getMonth() === today.getMonth() &&
						d.getDate() === today.getDate()
					list.push({
						day: dayNames[d.getDay()],
						dateNum: d.getDate(),
						label: isToday ? `${month}/${date}` : String(d.getDate()),
						active: isToday,
						fullDate: `${d.getFullYear()}-${month}-${date}`
					})
				}
				this.weekList = list
			},
			onSelectDay(index) {
				this.weekList.forEach((item, i) => {
					const monthDay = item.fullDate.slice(5).replace('-', '/')
					item.active = i === index
					item.label = item.active ? monthDay : String(item.dateNum)
				})
				if (this.isTodaySelected) {
					const cached = BleService.getSport && BleService.getSport()
					if (cached) this.applySport(cached)
					else this.refreshSport()
				}
			},
			onGoalTap(item) {
				this.editingGoal = {
					key: item.key,
					name: item.name,
					placeholder: item.placeholder,
					unit: item.unit
				}
				this.goalInput = String(item.value)
				this.showGoalPopup = true
			},
			closeGoalPopup() {
				this.showGoalPopup = false
			},
			saveGoalPopup() {
				const num = Number(this.goalInput)
				if (!this.goalInput || isNaN(num) || num <= 0) {
					uni.showToast({
						title: '请输入有效数值',
						icon: 'none'
					})
					return
				}
				const target = this.goalList.find(item => item.key === this.editingGoal.key)
				if (target) {
					target.value = num
					if (target.key === 'step') this.stepTarget = num
					if (target.key === 'calorie') this.calorieTarget = num
				}
				this.persistGoals()
				this.showGoalPopup = false
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 60rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.week-bar {
		display: flex;
		align-items: stretch;
		justify-content: space-between;
	}

	.week-item {
		position: relative;
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 20rpx 0 24rpx;
		border-radius: 20rpx;
		overflow: hidden;
	}

	.week-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.week-day,
	.week-date {
		position: relative;
		z-index: 1;
	}

	.week-day {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.week-item.active .week-day {
		color: rgba(255, 255, 255, 0.85);
	}

	.week-date {
		margin-top: 10rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #8e8e93;
		line-height: 40rpx;
	}

	.week-item.active .week-date {
		color: #ffffff;
	}

	.summary-card {
		margin-top: 28rpx;
		padding: 32rpx 28rpx 28rpx;
		border-radius: 28rpx;
		background: #1A1B25;
		box-sizing: border-box;
	}

	.summary-top {
		display: flex;
		align-items: center;
	}

	.rings {
		width: 240rpx;
		height: 240rpx;
		flex-shrink: 0;
		margin-right: 24rpx;
	}

	.rings-svg {
		width: 100%;
		height: 100%;
		/* 默认从右侧起笔，旋转 90° 后从底部起、顺时针绘制 */
		transform: rotate(90deg);
		overflow: visible;
	}

	.ring-track,
	.ring-track-edge,
	.ring-progress,
	.ring-progress-border {
		fill: none;
	}

	.summary-metrics {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		justify-content: center;
	}

	.metric-row {
		display: flex;
		align-items: center;
		margin-bottom: 22rpx;
	}

	.metric-row:last-child {
		margin-bottom: 0;
	}

	.dot {
		width: 10rpx;
		height: 28rpx;
		border-radius: 6rpx;
		margin-right: 12rpx;
		flex-shrink: 0;
	}

	.dot.yellow {
		background: #ffd60a;
		box-shadow: 0 0 10rpx rgba(255, 214, 10, 0.6);
	}

	.dot.red {
		background: #ff453a;
		box-shadow: 0 0 10rpx rgba(255, 69, 58, 0.6);
	}

	.dot.blue {
		background: #0a84ff;
		box-shadow: 0 0 10rpx rgba(10, 132, 255, 0.6);
	}

	.metric-label {
		width: 64rpx;
		font-size: 26rpx;
		color: #8e8e93;
		line-height: 36rpx;
		flex-shrink: 0;
	}

	.metric-value {
		flex: 1;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 40rpx;
	}

	.progress-block {
		margin-top: 32rpx;
	}

	.progress-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.progress-row {
		margin-top: 16rpx;
		display: flex;
		align-items: center;
	}

	.progress-track {
		flex: 1;
		height: 12rpx;
		border-radius: 6rpx;
		background: rgba(255, 255, 255, 0.1);
		overflow: hidden;
	}

	.progress-fill {
		height: 100%;
		border-radius: 6rpx;
		background: linear-gradient(90deg, #54b7f7 0%, #2f7bff 50%, #1a53ec 100%);
		box-shadow: 0 0 12rpx rgba(47, 123, 255, 0.8);
	}

	.progress-percent {
		margin-left: 20rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 40rpx;
		flex-shrink: 0;
	}

	.section {
		margin-top: 40rpx;
	}

	.section-title {
		display: block;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
		margin-bottom: 20rpx;
	}

	.goal-card {
		border-radius: 24rpx;
		background: #1A1B25;
		overflow: hidden;
	}

	.goal-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 32rpx 28rpx;
		box-sizing: border-box;
	}

	.goal-item.border {
		border-bottom: 1px solid rgba(255, 255, 255, 0.08);
	}

	.goal-name {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.goal-right {
		display: flex;
		align-items: center;
	}

	.goal-value {
		font-size: 28rpx;
		color: #8e8e93;
		line-height: 40rpx;
	}

	.goal-arrow {
		width: 32rpx;
		height: 32rpx;
		margin-left: 12rpx;
	}

	.stats-card {
		display: flex;
		align-items: stretch;
		padding: 36rpx 0;
		border-radius: 24rpx;
		background: #1A1B25;
	}

	.stats-col {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.stats-divider {
		width: 1px;
		background: rgba(255, 255, 255, 0.1);
		align-self: stretch;
		margin: 8rpx 0;
	}

	.stats-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.stats-value-row {
		margin-top: 12rpx;
		display: flex;
		align-items: baseline;
	}

	.stats-num {
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 48rpx;
	}

	.stats-unit {
		margin-left: 6rpx;
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.tip-card {
		margin-top: 28rpx;
		padding: 28rpx;
		border-radius: 24rpx;
		background: #1A1B25;
		box-sizing: border-box;
		position: relative;
		overflow: hidden;
	}

	.tip-card::before {
		content: '';
		position: absolute;
		left: 0;
		top: 0;
		width: 180rpx;
		height: 120rpx;
		background: radial-gradient(ellipse at top left, rgba(47, 123, 255, 0.35) 0%, transparent 70%);
		pointer-events: none;
	}

	.tip-header {
		position: relative;
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
	}

	.tip-icon {
		width: 32rpx;
		height: 32rpx;
		margin-right: 10rpx;
	}

	.tip-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #4da3ff;
		line-height: 40rpx;
	}

	.tip-text {
		position: relative;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 38rpx;
	}

	.goal-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.goal-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #1A1B25;
		margin: 30rpx;
	}

	.goal-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.goal-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.goal-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.goal-popup-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.goal-popup-label {
		display: block;
		margin-top: 36rpx;
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.goal-popup-input {
		margin-top: 16rpx;
		height: 88rpx;
		padding: 0 28rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		font-size: 28rpx;
		color: #ffffff;
		box-sizing: border-box;
	}

	.goal-popup-placeholder {
		color: #636366;
	}

	.goal-popup-btns {
		margin-top: 40rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.goal-popup-btn {
		flex: 1;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		border-radius: 16rpx;
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		box-sizing: border-box;
	}

	.goal-popup-btn+.goal-popup-btn {
		margin-left: 24rpx;
	}

	.goal-popup-btn.cancel {
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.goal-popup-btn.save {
		background: linear-gradient(90deg, #2563eb 0%, #3b82f6 50%, #60a5fa 100%);
	}
</style>