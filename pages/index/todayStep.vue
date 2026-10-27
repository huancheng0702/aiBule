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
				<!-- 三环进度图：App 端用 canvas 绘制 -->
				<view class="rings">
					<canvas
						class="rings-canvas"
						canvas-id="stepRingsCanvas"
						id="stepRingsCanvas"
						:style="ringsSize ? { width: ringsSize + 'px', height: ringsSize + 'px' } : {}"
					></canvas>
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

		<view class="tip-card">
			<!-- 说明卡片背景图，自行补充 src -->
			<image class="tip-bg" src="/static/heartCardBg.png" mode=""></image>
			<view class="tip-header">
				<!-- 灯泡图标，自行补充 src -->
				<image class="tip-icon" src="/static/qushijianyi.png" mode=""></image>
				<image src="/static/shuoming.png" style="width: 70rpx;height: 34rpx;" mode=""></image>
			</view>
			<view class="tip-text">{{ tipText }}</view>
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
						<view class="goal-popup-btn cancel" @click="closeGoalPopup">
							<image class="goal-popup-btn-bg" src="" mode="aspectFill"></image>
							取消
						</view>
						<view class="goal-popup-btn save" @click="saveGoalPopup">
							<image class="goal-popup-btn-bg" src="/static/confirmBg.png" mode=""></image>
							保存
						</view>
						
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getSportTrend } from '@/common/api/deviceData.js'
	import { saveDeviceConfig } from '@/common/api/device.js'
	import BleService from '@/common/ble-service.js'

	const GOAL_STORAGE_KEY = 'todayStepGoals'

	function safeRatio(current, target) {
		const t = Number(target)
		if (!t || t <= 0) return 0
		return Math.max(0, Math.min(1, Number(current) / t))
	}

	export default {
		data() {
			return {
				weekList: [],
				ringsSize: 0,
				ringsCtx: null,
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
				statsMax: 0,
				statsMin: 0,
				statsAvg: 0,
				tipText: '步数、距离与消耗由智能戒指统计后同步到云端。本设备仅用于健康管理参考，不能用于医疗诊断。'
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
			ringsKey() {
				return [this.stepRatio, this.calorieRatio, this.distanceRatio].join('|')
			}
		},
		watch: {
			ringsKey() {
				this.$nextTick(() => this.drawRings())
			}
		},
		created() {
			this.initWeekList()
			this.loadGoals()
			this.refreshByDate()
			this._unsubBle = BleService.subscribe((st) => {
				this.onBleSportUploaded(st && st.sport)
			})
		},
		mounted() {
			this.$nextTick(() => {
				setTimeout(() => this.measureRings(0), 50)
			})
		},
		onReady() {
			this.measureRings(0)
		},
		onShow() {
			this.refreshByDate()
			this.$nextTick(() => this.drawRings())
		},
		onHide() {
			if (this._unsubBle) { this._unsubBle(); this._unsubBle = null }
		},
		onUnload() {
			if (this._unsubBle) { this._unsubBle(); this._unsubBle = null }
		},
		methods: {
			measureRings(retry = 0) {
				uni.createSelectorQuery()
					.in(this)
					.select('.rings')
					.boundingClientRect((rect) => {
						if (!rect || !rect.width) {
							if (retry < 8) setTimeout(() => this.measureRings(retry + 1), 80)
							return
						}
						this.ringsSize = Math.round(rect.width)
						if (!this.ringsCtx) {
							this.ringsCtx = uni.createCanvasContext('stepRingsCanvas', this)
						}
						this.$nextTick(() => this.drawRings())
					})
					.exec()
			},
			drawRings() {
				const ctx = this.ringsCtx
				const size = this.ringsSize
				if (!ctx || !size) return

				const scale = size / 100
				const cx = size / 2
				const cy = size / 2
				// 设计稿从 6 点钟方向起笔，沿顺时针方向推进。
				const startAngle = Math.PI / 2
				const rings = [{
					radius: 40.5,
					width: 9,
					track: '#292725',
					gradient: ['#8D6A1F', '#604A20', '#BD8919'],
					ratio: this.stepRatio
				}, {
					radius: 27.5,
					width: 7,
					track: '#291E24',
					gradient: ['#A74436', '#ED503F', '#FF454A'],
					ratio: this.calorieRatio
				}, {
					radius: 16,
					width: 5.5,
					track: '#14213B',
					gradient: ['#173665', '#2375D6', '#318FFF'],
					ratio: this.distanceRatio
				}]

				const strokeArc = (radius, width, color, from, to, cap = 'butt') => {
					ctx.beginPath()
					ctx.setLineWidth(width * scale)
					ctx.setStrokeStyle(color)
					ctx.setLineCap(cap)
					ctx.arc(cx, cy, radius * scale, from, to, false)
					ctx.stroke()
				}
				ctx.clearRect(0, 0, size, size)

				// 先画三条完整暗色轨道。
				for (let i = 0; i < rings.length; i++) {
					const ring = rings[i]
					strokeArc(ring.radius, ring.width, ring.track, 0, Math.PI * 2)
				}

				// 再画进度。不同粗细、统一起点与由暗到亮的纵向渐变均按设计稿处理。
				for (let i = 0; i < rings.length; i++) {
					const ring = rings[i]
					const percent = Math.max(0, Math.min(1, Number(ring.ratio) || 0))
					if (percent <= 0) continue

					const full = percent >= 0.999
					const endAngle = startAngle + Math.PI * 2 * (full ? 1 : percent)
					const gradient = ctx.createLinearGradient(cx, cy - ring.radius * scale, cx, cy + ring.radius * scale)
					gradient.addColorStop(0, ring.gradient[0])
					gradient.addColorStop(0.52, ring.gradient[1])
					gradient.addColorStop(1, ring.gradient[2])

					strokeArc(ring.radius, ring.width, gradient, startAngle, endAngle, full ? 'butt' : 'round')
				}

				ctx.beginPath()
				ctx.setFillStyle('#17213A')
				ctx.arc(cx, cy, 8.2 * scale, 0, Math.PI * 2, false)
				ctx.fill()
				ctx.draw()
			},
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
			getActiveDate() {
				const active = (this.weekList || []).find((item) => item.active)
				return (active && active.fullDate) || ''
			},
			isTodayDate(dateStr) {
				const d = new Date()
				const p = (n) => String(n).padStart(2, '0')
				return dateStr === `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
			},
			isActiveToday() {
				return this.isTodayDate(this.getActiveDate())
			},
			refreshByDate() {
				if (this.isActiveToday()) {
					this.loadTrend()
					BleService.syncSport && BleService.syncSport().catch(() => {})
				} else {
					this.loadTrend()
				}
			},
			onBleSportUploaded(s) {
				console.log('[todayStep] onBleSportUploaded:', JSON.stringify(s))
				if (!s || !this.isActiveToday()) return
				const key = [s.steps, s.distanceM, s.calories, s.updatedAt].join('|')
				if (key === this._lastBleKey) return
				this._lastBleKey = key
				clearTimeout(this._bleReloadTimer)
				this._bleReloadTimer = setTimeout(() => {
					this.loadTrend()
				}, 1500)
			},
			async loadTrend() {
				const date = this.getActiveDate()
				try {
					const data = await getSportTrend(date)
					this.applyTrend(data)
				} catch (e) {}
			},
			applyTrend(data) {
				if (!data) return
				
				this.stepCurrent = Number(data.totalSteps) || 0
				this.calorieCurrent = Math.round(Number(data.totalCalories) || 0)
				const meters = Number(data.totalDistance) || 0
				this.distanceCurrent = Math.round((meters / 1000) * 10) / 10
				if (Number(data.target) > 0) {
					this.stepTarget = Number(data.target)
					const g = this.goalList.find((i) => i.key === 'step')
					if (g) g.value = this.stepTarget
				}
				if (Number(data.calorieTarget) > 0) {
					this.calorieTarget = Number(data.calorieTarget)
					const g = this.goalList.find((i) => i.key === 'calorie')
					if (g) g.value = this.calorieTarget
				}
				if (Number(data.distanceTarget) > 0) {
					this.distanceTarget = Math.round((Number(data.distanceTarget) / 1000) * 10) / 10
				}
				if (Number(data.sportTimeTarget) > 0) {
					const g = this.goalList.find((i) => i.key === 'duration')
					if (g) g.value = Number(data.sportTimeTarget)
				}
				this.statsMax = this.stepCurrent
				this.statsMin = this.stepCurrent
				this.statsAvg = this.stepCurrent
				const desc = data.description || data.desc || data.status
				if (desc) this.tipText = desc
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
				this.refreshByDate()
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
			async saveGoalPopup() {
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
				const durationGoal = this.goalList.find((i) => i.key === 'duration')
				try {
					await saveDeviceConfig({
						step: this.stepTarget,
						calorie: this.calorieTarget,
						sprotTime: durationGoal ? durationGoal.value : undefined
					})
				} catch (e) {}
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

	.rings-canvas {
		width: 100%;
		height: 100%;
		display: block;
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
		background: #FFC53D;
		box-shadow: 0 0 10rpx rgba(255, 197, 61, 0.6);
	}

	.dot.red {
		background: #FF2C30;
		box-shadow: 0 0 10rpx rgba(255, 44, 48, 0.6);
	}

	.dot.blue {
		background: #3D9BFF;
		box-shadow: 0 0 10rpx rgba(61, 155, 255, 0.6);
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
		position: relative;
		overflow: hidden;
		margin-top: 28rpx;
		padding: 28rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		box-sizing: border-box;
	}
	
	.tip-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		pointer-events: none;
		z-index: 0;
	}
	
	.tip-header {
		position: relative;
		z-index: 1;
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
		z-index: 1;
		font-size: 24rpx;
		color: #fff;
		line-height: 38rpx;
		padding-left: 40rpx;
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
		position: relative;
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

	.goal-popup-btn-bg {
		position: absolute;
		inset: 0;
		width: 100%;
		height: 100%;
		z-index: -1;
	}

	.goal-popup-btn+.goal-popup-btn {
		margin-left: 24rpx;
	}

	.goal-popup-btn.cancel {
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.goal-popup-btn.save {
	}
</style>