<template>
	<view class="page">
		<!-- 最近一次摘要 -->
		<view class="hero-card">
			<!-- 顶部卡片背景图，自行补充 src -->
			<image class="hero-bg" src="/static/yaliceliang.png" mode="aspectFill"></image>
			<view class="hero-body">
				<view class="hero-left">
					<text class="hero-label">最近一次 · {{ latestTime }}</text>
					<view class="hero-value-row">
						<text class="hero-num">{{ latestValue }}</text>
					</view>
					<text class="hero-status">{{ latestStatus }}</text>
				</view>
				<!-- 压力装饰图，自行补充 src -->
				<image class="hero-deco" mode="aspectFit"></image>
				<view class="measure-btn" @click="onStartMeasure">
					<!-- 开始测量按钮背景图，自行补充 src -->
					<image class="measure-btn-bg" mode="scaleToFill"></image>
					<text class="measure-btn-text">{{ measuring ? '测量中' : '开始测量' }}</text>
				</view>
			</view>
		</view>

		<!-- 日 / 周 / 月 -->
		<view class="period-bar">
			<view
				v-for="item in periodList"
				:key="item.key"
				class="period-item"
				:class="{ active: period === item.key }"
				@click="onPeriodChange(item.key)"
			>
				<!-- 选中背景图，自行补充 src -->
				<image v-if="period === item.key" src="/static/confirmBg.png" class="period-item-bg" mode="scaleToFill"></image>
				<text class="period-text">{{ item.label }}</text>
			</view>
		</view>

		<!-- 压力趋势 -->
		<view class="card trend-card">
			<view class="date-bar">
				<image class="date-bar-bg" src="/static/selectDateBg.png" mode=""></image>
				<view class="date-btn" @click="onPrevDay">
					<image src="/static/arrLeft.png" mode=""></image>
				</view>
				<view class="date-center" @click="openCalendar">
					<text class="date-text">{{ dateText }}</text>
					<image class="date-calendar" src="/static/dateIcon.png" mode="aspectFit"></image>
				</view>
				<view class="date-btn" @click="onNextDay">
					<image src="/static/arrRight.png" mode=""></image>
				</view>
			</view>

			<text class="card-title">压力趋势</text>

			<view class="trend-chart">
				<view class="trend-body">
					<view class="y-axis">
						<text v-for="label in yLabels" :key="label" class="y-label">{{ label }}</text>
					</view>
					<view
						class="trend-plot"
						@touchstart="onChartTouch"
						@touchmove.stop.prevent="onChartTouch"
						@touchend="onChartTouchEnd"
						@touchcancel="onChartTouchEnd"
					>
						<image v-if="chartImageSrc" class="trend-svg" :src="chartImageSrc" mode="scaleToFill" />
						<view
							v-if="touchActive && touchPoint"
							class="chart-tip"
							:style="chartTipStyle"
						>
							<text class="chart-tip-value">{{ touchValue }}</text>
							<text class="chart-tip-time">{{ touchLabel }}</text>
						</view>
					</view>
				</view>
				<view class="trend-x">
					<text v-for="(label, idx) in xLabels" :key="idx" class="trend-x-label">{{ label }}</text>
				</view>
				<view class="legend">
					<view v-for="item in legendList" :key="item.label" class="legend-item">
						<view class="legend-dot" :style="{ background: item.color }"></view>
						<text class="legend-text">{{ item.label }}</text>
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
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">最低值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsMin }}</text>
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">平均值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsAvg }}</text>
					</view>
				</view>
			</view>
		</view>
	<!-- 说明（背景图） -->
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

		<!-- 最近数据 -->
		<view class="section">
			<text class="section-title">最近数据</text>
			<view class="card recent-card">
				<view
					v-for="(item, index) in recentList"
					:key="index"
					class="recent-row"
					:class="{ border: index < recentList.length - 1 }"
				>
					<view class="recent-left">
						<text class="recent-time">{{ item.time }}</text>
						<text class="recent-source">{{ item.source }}</text>
					</view>
					<text class="recent-duration">{{ item.duration }}</text>
				</view>
			</view>
		</view>

		<!-- 日历弹窗 -->
		<view v-if="showCalendar" class="cal-mask" @click="closeCalendar">
			<view class="cal-popup" @click.stop>
				<image class="cal-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="cal-body">
					<view class="cal-header">
						<view class="cal-nav" @click="onCalPrevMonth">
							<text class="cal-nav-arrow">‹</text>
						</view>
						<text class="cal-title">{{ calendarTitle }}</text>
						<view class="cal-nav" @click="onCalNextMonth">
							<text class="cal-nav-arrow">›</text>
						</view>
					</view>

					<view class="cal-week">
						<text v-for="(w, i) in weekLabels" :key="i" class="cal-week-item">{{ w }}</text>
					</view>

					<view class="cal-grid">
						<view
							v-for="(cell, index) in calendarCells"
							:key="index"
							class="cal-cell"
							:class="{ empty: !cell.day, active: cell.active }"
							@click="cell.day && onSelectCalDay(cell.day)"
						>
							<image v-if="cell.active" src="/static/dateSelect.png" class="cal-day-bg" mode="aspectFill"></image>
							<text v-if="cell.day" class="cal-day">{{ cell.day }}</text>
						</view>
					</view>

					<view class="cal-btns">
						<view class="cal-btn cancel" @click="closeCalendar">取消</view>
						<view class="cal-btn confirm" @click="confirmCalendar">
							<image class="cal-btn-bg" src="/static/confirmBg.png" mode=""></image>
							<text class="cal-btn-text">确定</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 检测注意事项 -->
		<view v-if="showMeasureTip" class="tip-mask" @click="closeMeasureTip">
			<view class="tip-popup" @click.stop>
				<image class="tip-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="tip-popup-body">
					<text class="tip-popup-title">检测注意事项</text>
					<image class="tip-popup-illust" src="/static/celiangTipsIcon.png" mode=""></image>
					<view class="tip-popup-card">
						<view
							v-for="(item, index) in measureTips"
							:key="item.no"
							class="tip-popup-row"
							:class="{ border: index < measureTips.length - 1 }"
						>
							<view class="tip-popup-no">
								<text class="tip-popup-no-text">{{ item.no }}</text>
							</view>
							<text class="tip-popup-desc">{{ item.text }}</text>
						</view>
					</view>
					<view class="cal-btn confirm" style="margin-top: 46rpx;" @click="onMeasureNext">
						<image class="cal-btn-bg" src="/static/xiayibu.png" mode=""></image>
					</view>
				</view>
			</view>
		</view>

		<!-- 检测中 -->
		<view v-if="showMeasuring" class="measuring-mask" @click="onTryCloseMeasuring">
			<view class="measuring-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="measuring-bg" mode="aspectFill"></image>
				<view class="measuring-body">
					<view class="ripple">
						<view class="ripple-disc d3"></view>
						<view class="ripple-disc d2"></view>
						<view class="ripple-disc d1"></view>
						<view class="ripple-core"></view>
					</view>
					<text class="measuring-text">{{ liveStress ? ('检测中 ' + liveStress) : '检测中...' }}</text>
					<text class="measuring-sub">请保持手指静止，等待戒指返回数据</text>
				</view>
			</view>
		</view>

		<!-- 取消检测确认 -->
		<view v-if="showCancelConfirm" class="cancel-mask">
			<view class="cancel-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="cancel-bg" mode="aspectFill"></image>
				<view class="cancel-body">
					<!-- 警告图标，自行补充 src -->
					<image class="cancel-icon" mode="aspectFit"></image>
					<text class="cancel-title">取消检测?</text>
					<text class="cancel-desc">取消后将会终止正在进行的测量任务。确认取消?</text>
					<view class="cancel-btns">
						<view class="cancel-btn ghost" @click="confirmCancelMeasure">取消</view>
						<view class="cancel-btn solid" @click="keepMeasuring">继续</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { toSvgDataUrl } from '@/common/svg-data-url.js'
	import CoolWearBLE from '@/common/coolwear-ble.js'
	import BleService from '@/common/ble-service.js'

	const CHART_H = 220
	const Y_MIN = 0
	const Y_MAX = 120
	const MEASURE_TIMEOUT_MS = 45000
	const MEASURE_MIN_SAMPLES = 2
	const MEASURE_STABLE_MS = 8000

	function clamp(value, min, max) {
		return Math.max(min, Math.min(max, value))
	}

	function valueToY(value, chartH) {
		const ratio = (clamp(value, Y_MIN, Y_MAX) - Y_MIN) / (Y_MAX - Y_MIN)
		return chartH * (1 - ratio)
	}

	function barColor(value) {
		if (value < 30) return '#3B82F6'
		if (value < 60) return '#8B5CF6'
		if (value < 90) return '#F59E0B'
		return '#F472B6'
	}

	/** SDK：压力由 HRV 计算。HRV 越高通常压力越低。 */
	function hrvToStress(hrv) {
		const h = clamp(Number(hrv) || 0, 5, 120)
		const stress = Math.round(100 - (h - 5) * (90 / 115))
		return clamp(stress, 1, 99)
	}

	function extractStressValues(data) {
		const list = Array.isArray(data) ? data : (data != null ? [data] : [])
		const values = []
		list.forEach((item) => {
			if (typeof item === 'number') {
				if (item > 0) values.push(hrvToStress(item))
				return
			}
			if (!item || typeof item !== 'object') return
			const stressRaw = item.stress != null
				? item.stress
				: (item.pressure != null ? item.pressure : item.stressValue)
			if (stressRaw != null && stressRaw !== '') {
				const s = Number(stressRaw)
				if (!Number.isNaN(s) && s > 0) {
					values.push(clamp(Math.round(s), 1, 120))
					return
				}
			}
			const hrvRaw = item.value != null
				? item.value
				: (item.hrvNums != null ? item.hrvNums : item.hrv)
			const hrv = Number(hrvRaw)
			if (!Number.isNaN(hrv) && hrv > 0) values.push(hrvToStress(hrv))
		})
		return values
	}

	function stressStatusText(stress) {
		if (stress < 30) return '当前放松'
		if (stress < 60) return '当前正常'
		if (stress < 90) return '中等压力'
		return '压力偏高'
	}

	function formatLatestTime(date = new Date()) {
		const h = String(date.getHours()).padStart(2, '0')
		const m = String(date.getMinutes()).padStart(2, '0')
		return `今天 ${h}:${m}`
	}

	export default {
		data() {
			return {
				latestTime: '今天 09:41',
				latestValue: 72,
				latestStatus: '当前放松',
				period: 'day',
				periodList: [
					{ key: 'day', label: '日' },
					{ key: 'week', label: '周' },
					{ key: 'month', label: '月' }
				],
				currentDate: new Date(2026, 8, 4),
				chartW: 0,
				chartH: CHART_H,
				chartValues: [28, 45, 62, 38, 55, 78, 92, 48, 35, 68, 85, 42],
				yLabels: [120, 90, 60, 30],
				xLabels: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'],
				legendList: [
					{ color: '#3B82F6', label: '放松' },
					{ color: '#8B5CF6', label: '正常' },
					{ color: '#F59E0B', label: '中等' },
					{ color: '#F472B6', label: '偏高' }
				],
				statsMax: 88,
				statsMin: 88,
				statsAvg: 73,
				chartLeft: 0,
				touchActive: false,
				touchIndex: -1,
				showCalendar: false,
				showMeasureTip: false,
				showMeasuring: false,
				showCancelConfirm: false,
				measuring: false,
				liveStress: 0,
				measureSamples: [],
				measureTimer: null,
				measureStableTimer: null,
				hrvUnsub: null,
				measureTips: [
					{ no: '1', text: '请正确佩戴设备，使设备光路对准指腹或手指两侧' },
					{ no: '2', text: '测量过程中身体尽可能保持静止，请勿走动或晃动' }
				],
				calYear: 2026,
				calMonth: 8,
				calSelectedDay: 4,
				weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
				tipText: '压力指数综合反映当前身心紧张程度。数值越低表示越放松，建议结合作息、运动与情绪变化长期观察。本设备仅用于健康管理参考，不能用于医疗诊断。',
				recentList: [
					{ time: '今天 09:41', source: '戒指采集', duration: '7 小时 32 分' },
					{ time: '昨天 21:16', source: '戒指采集', duration: '7 小时 32 分' },
					{ time: '8月6日 08:32', source: '戒指采集', duration: '7 小时 32 分' }
				]
			}
		},
		computed: {
			dateText() {
				const d = this.currentDate
				const y = d.getFullYear()
				const m = String(d.getMonth() + 1).padStart(2, '0')
				const day = String(d.getDate()).padStart(2, '0')
				return `${y}年${m}月${day}日`
			},
			gridYs() {
				return [30, 60, 90].map((v) => valueToY(v, this.chartH))
			},
			chartBars() {
				const values = this.chartValues
				const count = values.length
				if (!count || !this.chartW) return []
				const slot = this.chartW / count
				const barW = Math.max(8, slot * 0.42)
				return values.map((value, index) => {
					const y = valueToY(value, this.chartH)
					const h = Math.max(2, this.chartH - y)
					return {
						x: index * slot + (slot - barW) / 2,
						y: this.chartH - h,
						w: barW,
						h,
						color: barColor(value)
					}
				})
			},
			chartImageSrc() {
				if (!this.chartW || !this.chartH || !this.chartBars.length) return ''
				const grids = (this.gridYs || []).map((y) =>
					`<line x1="0" y1="${y}" x2="${this.chartW}" y2="${y}" stroke="rgba(255,255,255,0.12)" stroke-width="1" stroke-dasharray="4 5"/>`
				).join('')
				const bars = this.chartBars.map((bar) =>
					`<rect x="${bar.x}" y="${bar.y}" width="${bar.w}" height="${bar.h}" fill="${bar.color}" rx="2"/>`
				).join('')
				let touch = ''
				if (this.touchActive && this.touchPoint) {
					touch = `<line x1="${this.touchPoint.x}" y1="0" x2="${this.touchPoint.x}" y2="${this.chartH}" stroke="rgba(255,255,255,0.35)" stroke-width="1"/>`
				}
				const inner =
					grids +
					bars +
					touch +
					`<line x1="0" y1="${this.chartH - 1}" x2="${this.chartW}" y2="${this.chartH - 1}" stroke="rgba(255,255,255,0.28)" stroke-width="1"/>`
				return toSvgDataUrl(inner, this.chartW, this.chartH, 'preserveAspectRatio="none"')
			},
			touchPoint() {
				if (this.touchIndex < 0 || !this.chartBars[this.touchIndex]) return null
				const bar = this.chartBars[this.touchIndex]
				return {
					x: bar.x + bar.w / 2,
					y: bar.y
				}
			},
			touchValue() {
				if (this.touchIndex < 0) return 0
				return this.chartValues[this.touchIndex]
			},
			touchLabel() {
				if (this.touchIndex < 0) return ''
				return this.xLabels[this.touchIndex] || String(this.touchIndex + 1)
			},
			chartTipStyle() {
				if (!this.touchPoint) return {}
				const tipW = 90
				const x = this.touchPoint.x
				const y = this.touchPoint.y
				let left = x - tipW / 2
				if (left < 0) left = 0
				if (left + tipW > this.chartW) left = this.chartW - tipW
				let top = y - 56
				if (top < 0) top = y + 12
				return {
					left: `${left}px`,
					top: `${top}px`,
					width: `${tipW}px`
				}
			},
			calendarTitle() {
				return `${this.calYear}年${this.calMonth + 1}月`
			},
			calendarCells() {
				const year = this.calYear
				const month = this.calMonth
				const daysInMonth = new Date(year, month + 1, 0).getDate()
				const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7
				const cells = []
				for (let i = 0; i < firstWeekday; i++) {
					cells.push({ day: 0, active: false })
				}
				for (let d = 1; d <= daysInMonth; d++) {
					cells.push({
						day: d,
						active: d === this.calSelectedDay
					})
				}
				while (cells.length % 7 !== 0) {
					cells.push({ day: 0, active: false })
				}
				return cells
			}
		},
		mounted() {
			this.$nextTick(() => {
				this.measureChart(0)
			})
		},
		onReady() {
			this.measureChart(0)
		},
		onBackPress() {
			if (!this.showMeasuring && !this.showCancelConfirm) return false
			this.showCancelConfirm = true
			return true
		},
		onUnload() {
			this.stopRealMeasure(false)
		},
		methods: {
			measureChart(retry = 0) {
				uni.createSelectorQuery()
					.in(this)
					.select('.trend-plot')
					.boundingClientRect((rect) => {
						if (!rect || !rect.width) {
							if (retry < 8) setTimeout(() => this.measureChart(retry + 1), 80)
							return
						}
						this.chartW = rect.width
						this.chartH = rect.height || CHART_H
						this.chartLeft = rect.left || 0
					})
					.exec()
			},
			onChartTouch(e) {
				const touch = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
				if (!touch || !this.chartW || !this.chartValues.length) return
				const x = touch.clientX - this.chartLeft
				const len = this.chartValues.length
				const slot = this.chartW / len
				let index = Math.floor(x / slot)
				if (index < 0) index = 0
				if (index > len - 1) index = len - 1
				this.touchActive = true
				this.touchIndex = index
			},
			onChartTouchEnd() {
				this.touchActive = false
				this.touchIndex = -1
			},
			onPeriodChange(key) {
				this.period = key
			},
			shiftDate(delta) {
				const next = new Date(this.currentDate)
				next.setDate(next.getDate() + delta)
				this.currentDate = next
			},
			onPrevDay() {
				this.shiftDate(-1)
			},
			onNextDay() {
				this.shiftDate(1)
			},
			openCalendar() {
				const d = this.currentDate
				this.calYear = d.getFullYear()
				this.calMonth = d.getMonth()
				this.calSelectedDay = d.getDate()
				this.showCalendar = true
			},
			closeCalendar() {
				this.showCalendar = false
			},
			onCalPrevMonth() {
				if (this.calMonth === 0) {
					this.calYear -= 1
					this.calMonth = 11
				} else {
					this.calMonth -= 1
				}
				const maxDay = new Date(this.calYear, this.calMonth + 1, 0).getDate()
				if (this.calSelectedDay > maxDay) this.calSelectedDay = maxDay
			},
			onCalNextMonth() {
				if (this.calMonth === 11) {
					this.calYear += 1
					this.calMonth = 0
				} else {
					this.calMonth += 1
				}
				const maxDay = new Date(this.calYear, this.calMonth + 1, 0).getDate()
				if (this.calSelectedDay > maxDay) this.calSelectedDay = maxDay
			},
			onSelectCalDay(day) {
				this.calSelectedDay = day
			},
			confirmCalendar() {
				this.currentDate = new Date(this.calYear, this.calMonth, this.calSelectedDay)
				this.showCalendar = false
			},
			onStartMeasure() {
				if (this.measuring) return
				const ble = BleService.getState()
				if (!ble.connected) {
					uni.showModal({
						title: '未连接戒指',
						content: '请先连接智能戒指后再开始测量',
						confirmText: '去连接',
						success: (res) => {
							if (res.confirm) {
								uni.navigateTo({
									url: '/pages/mine/deviceManage'
								})
							}
						}
					})
					return
				}
				this.showMeasureTip = true
			},
			closeMeasureTip() {
				this.showMeasureTip = false
			},
			async onMeasureNext() {
				this.showMeasureTip = false
				await this.startRealMeasure()
			},
			clearMeasureTimer() {
				if (this.measureTimer) {
					clearTimeout(this.measureTimer)
					this.measureTimer = null
				}
				if (this.measureStableTimer) {
					clearTimeout(this.measureStableTimer)
					this.measureStableTimer = null
				}
			},
			unbindHrv() {
				if (this.hrvUnsub) {
					this.hrvUnsub()
					this.hrvUnsub = null
				}
			},
			async startRealMeasure() {
				if (this.measuring) return
				this.measuring = true
				this.showMeasuring = true
				this.showCancelConfirm = false
				this.liveStress = 0
				this.measureSamples = []
				this.clearMeasureTimer()
				this.unbindHrv()

				// 官方说明：压力由 HRV 数据通过算法计算
				this.hrvUnsub = CoolWearBLE.on('onHRV', (data) => {
					this.onHrvData(data)
				})

				try {
					await CoolWearBLE.setHRVSwitch(true)
					console.log('[stress] 已开启 HRV/压力检测')
				} catch (e) {
					console.error('[stress] 开启检测失败', e)
					this.stopRealMeasure(false)
					uni.showToast({
						title: (e && e.message) || '开启压力检测失败',
						icon: 'none'
					})
					return
				}

				this.measureTimer = setTimeout(() => {
					if (!this.measuring) return
					if (this.liveStress > 0) {
						this.finishMeasure(this.liveStress)
						return
					}
					this.stopRealMeasure(true)
					uni.showToast({
						title: '未收到压力数据，请确认佩戴后重试',
						icon: 'none',
						duration: 2500
					})
				}, MEASURE_TIMEOUT_MS)
			},
			onHrvData(data) {
				if (!this.measuring) return
				const values = extractStressValues(data)
				if (!values.length) {
					console.log('[stress] onHRV 原始数据无法解析', data)
					return
				}
				values.forEach((v) => this.measureSamples.push(v))
				const latest = values[values.length - 1]
				this.liveStress = latest
				console.log('[stress] 实时压力', latest, 'samples=', this.measureSamples.length)

				if (this.measureSamples.length >= MEASURE_MIN_SAMPLES && !this.measureStableTimer) {
					this.measureStableTimer = setTimeout(() => {
						if (!this.measuring) return
						const stress = this.liveStress || this.measureSamples[this.measureSamples.length - 1]
						this.finishMeasure(stress)
					}, MEASURE_STABLE_MS)
				}
			},
			async stopRealMeasure(turnOffSwitch = true) {
				this.clearMeasureTimer()
				this.unbindHrv()
				this.measuring = false
				this.showMeasuring = false
				this.showCancelConfirm = false
				this.liveStress = 0
				this.measureSamples = []
				if (turnOffSwitch) {
					try {
						await CoolWearBLE.setHRVSwitch(false)
					} catch (e) {
						console.log('[stress] 关闭检测失败', e)
					}
				}
			},
			async finishMeasure(stress) {
				const value = Number(stress) || this.liveStress
				if (!value) {
					await this.stopRealMeasure(true)
					uni.showToast({
						title: '测量失败，请重试',
						icon: 'none'
					})
					return
				}
				this.latestValue = value
				this.latestStatus = stressStatusText(value)
				this.latestTime = formatLatestTime()
				this.recentList = [{
					time: this.latestTime,
					source: '戒指采集',
					duration: '即时测量'
				}].concat(this.recentList || []).slice(0, 10)

				await this.stopRealMeasure(true)
				uni.navigateTo({
					url: '/pages/index/stressSuccess?stress=' + value
				})
			},
			onTryCloseMeasuring() {
				this.showCancelConfirm = true
			},
			async confirmCancelMeasure() {
				await this.stopRealMeasure(true)
			},
			keepMeasuring() {
				this.showCancelConfirm = false
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

	.hero-card {
		position: relative;
		overflow: hidden;
		padding: 36rpx 28rpx;
		height: 232rpx;
		border-radius: 28rpx;
		background: #0d1f3d;
		box-sizing: border-box;
	}

	.hero-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		pointer-events: none;
	}

	.hero-body {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
	}

	.hero-left {
		flex-shrink: 0;
		min-width: 0;
	}

	.hero-label {
		display: block;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.75);
		line-height: 34rpx;
	}

	.hero-value-row {
		display: flex;
		align-items: baseline;
		margin-top: 12rpx;
	}

	.hero-num {
		font-size: 72rpx;
		font-weight: 700;
		line-height: 88rpx;
		color: #ffffff;
	}

	.hero-unit {
		margin-left: 10rpx;
		font-size: 28rpx;
		color: rgba(255, 255, 255, 0.9);
		line-height: 40rpx;
	}

	.hero-status {
		display: block;
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #64b5f6;
		line-height: 34rpx;
	}

	.hero-deco {
		flex: 1;
		width: 140rpx;
		height: 140rpx;
		margin: 0 12rpx;
	}

	.measure-btn {
		position: relative;
		flex-shrink: 0;
		min-width: 160rpx;
		height: 64rpx;
		padding: 0 28rpx;
		border-radius: 40rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #1a53ec;
	}

	.measure-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.measure-btn-text {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 36rpx;
	}

	.period-bar {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-top: 28rpx;
		padding: 6rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
		box-sizing: border-box;
	}

	.period-item {
		position: relative;
		flex: 1;
		height: 78rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.period-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.period-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		color: #8e8e93;
		line-height: 40rpx;
	}

	.period-item.active .period-text {
		color: #ffffff;
		font-weight: 600;
	}

	.card {
		margin-top: 28rpx;
		padding: 32rpx 28rpx 28rpx;
	background: #1A1B25;
	border-radius: 20rpx 20rpx 20rpx 20rpx;
	border: 1rpx solid rgba(255,255,255,0.1);
		box-sizing: border-box;
	}

	.trend-card {
		padding-top: 24rpx;
	}

	.date-bar {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 510rpx;
		margin: 0 auto 28rpx;
		padding: 14rpx 20rpx;
		box-sizing: border-box;
	}

	.date-bar-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
	}

	.date-btn {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.date-btn image {
		width: 34rpx;
		height: 34rpx;
	}

	.date-center {
		position: relative;
		z-index: 1;
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.date-text {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.date-calendar {
		width: 40rpx;
		height: 40rpx;
		margin-left: 10rpx;
	}

	.card-title {
		display: block;
		margin-top: 8rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.trend-meta {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 16rpx;
	}

	.trend-meta-left {
		display: flex;
		align-items: baseline;
		min-width: 0;
	}

	.trend-prefix {
		margin-right: 10rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.trend-bpm {
		font-size: 44rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 56rpx;
	}

	.trend-unit {
		margin-left: 10rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.trend-tag {
		margin-left: 16rpx;
		width: 80rpx;
		height: 44rpx;
		border-radius: 30rpx;
		text-align: center;
		background: #212f34;
		line-height: 44rpx;
	}

	.trend-tag-text {
		font-size: 22rpx;
		color: #64ebba;
	}

	.trend-time {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
		flex-shrink: 0;
	}

	.trend-chart {
		margin-top: 24rpx;
	}

	.trend-body {
		display: flex;
		align-items: stretch;
	}

	.y-axis {
		width: 48rpx;
		height: 220rpx;
		margin-right: 8rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		flex-shrink: 0;
	}

	.y-label {
		font-size: 20rpx;
		color: #8e8e93;
		line-height: 24rpx;
		text-align: right;
	}

	.trend-plot {
		position: relative;
		flex: 1;
		height: 220rpx;
		min-width: 0;
	}

	.trend-svg {
		width: 100%;
		height: 100%;
		display: block;
		overflow: visible;
	}

	.threshold-label {
		position: absolute;
		right: 0;
		font-size: 20rpx;
		color: #b388ff;
		line-height: 28rpx;
		z-index: 1;
		transform: translateY(-100%);
		pointer-events: none;
	}

	.chart-tip {
		position: absolute;
		z-index: 2;
		padding: 10rpx 14rpx;
		border-radius: 12rpx;
		background: rgba(20, 22, 32, 0.92);
		border: 1rpx solid rgba(59, 130, 246, 0.45);
		box-sizing: border-box;
		pointer-events: none;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.chart-tip-value {
		font-size: 24rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 34rpx;
	}

	.chart-tip-time {
		margin-top: 2rpx;
		font-size: 20rpx;
		color: #8e8e93;
		line-height: 28rpx;
	}

	.trend-x {
		display: flex;
		justify-content: space-between;
		margin-top: 12rpx;
		margin-left: 56rpx;
	}

	.trend-x-label {
		font-size: 20rpx;
		color: #8e8e93;
		line-height: 28rpx;
	}

	.legend {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 24rpx;
	}

	.legend-item {
		display: flex;
		align-items: center;
	}

	.legend-dot {
		width: 16rpx;
		height: 16rpx;
		margin-right: 8rpx;
		border-radius: 50%;
	}

	.legend-text {
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.75);
		line-height: 32rpx;
	}

	.section {
		margin-top: 36rpx;
	}

	.section-title {
		display: block;
		margin-bottom: 20rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.stats-card {
		display: flex;
		align-items: stretch;
		padding: 36rpx 0;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
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
		width: 44rpx;
		height: 44rpx;
		margin-right: 10rpx;
	}

	.tip-title {
		font-size: 28rpx;
		font-weight: 600;
		font-style: normal;
	}

	.tip-text {
		position: relative;
		z-index: 1;
		font-size: 24rpx;
		color: #fff;
		line-height: 50rpx;
		padding-left: 44rpx;
	}

	.recent-card {
		margin-top: 0;
		padding: 8rpx 28rpx;
	}

	.recent-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 28rpx 0;
	}

	.recent-row.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.recent-left {
		display: flex;
		flex-direction: column;
	}

	.recent-time {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.recent-source {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.recent-duration {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.cal-mask,
	.tip-mask,
	.measuring-mask,
	.cancel-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
		padding: 0 30rpx 30rpx;
	}

	.cal-mask,
	.tip-mask {
		z-index: 1000;
	}

	.measuring-mask {
		z-index: 1001;
	}

	.cancel-mask {
		z-index: 1002;
	}

	.cal-popup,
	.tip-popup,
	.measuring-popup,
	.cancel-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #1a1b25;
	}

	.cal-popup-bg,
	.tip-popup-bg,
	.measuring-bg,
	.cancel-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.cal-body {
		position: relative;
		z-index: 1;
		padding: 36rpx 28rpx 32rpx;
		box-sizing: border-box;
	}

	.cal-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 28rpx;
	}

	.cal-nav {
		width: 56rpx;
		height: 56rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cal-nav-arrow {
		font-size: 44rpx;
		color: #ffffff;
		line-height: 56rpx;
	}

	.cal-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.cal-week {
		display: flex;
		align-items: center;
		margin-bottom: 12rpx;
	}

	.cal-week-item {
		flex: 1;
		text-align: center;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 36rpx;
	}

	.cal-grid {
		display: flex;
		flex-wrap: wrap;
	}

	.cal-cell {
		position: relative;
		width: 14.28%;
		height: 104rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 8rpx;
		box-sizing: border-box;
		overflow: hidden;
	}

	.cal-day-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.cal-day {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.cal-btns {
		display: flex;
		align-items: center;
		margin-top: 28rpx;
	}

	.cal-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 16rpx;
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		box-sizing: border-box;
		overflow: hidden;
	}

	.cal-btn + .cal-btn {
		margin-left: 24rpx;
	}

	.cal-btn.cancel {
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.cal-btn.confirm {
		background: transparent;
	}

	.cal-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.cal-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 88rpx;
	}

	.tip-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.tip-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.tip-popup-illust {
		display: block;
		width: 100%;
		height: 194.74rpx;
		margin-top: 24rpx;
	}

	.tip-popup-card {
		margin-top: 48rpx;
		padding: 8rpx 24rpx;
		border-radius: 20rpx;
		background: rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.tip-popup-row {
		display: flex;
		align-items: flex-start;
		padding: 24rpx 0;
	}

	.tip-popup-row.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.tip-popup-no {
		width: 36rpx;
		height: 36rpx;
		margin-top: 4rpx;
		margin-right: 16rpx;
		border-radius: 50%;
		background: #2f7bff;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.tip-popup-no-text {
		font-size: 22rpx;
		color: #ffffff;
		line-height: 30rpx;
	}

	.tip-popup-desc {
		flex: 1;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.measuring-body {
		position: relative;
		z-index: 1;
		padding: 72rpx 40rpx 64rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-sizing: border-box;
	}

	.ripple {
		position: relative;
		width: 280rpx;
		height: 280rpx;
	}

	.ripple-disc,
	.ripple-core {
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		margin: auto;
		border-radius: 50%;
		animation: disc-breathe 2.6s ease-in-out infinite;
	}

	.ripple-disc.d3 {
		width: 280rpx;
		height: 280rpx;
		background: rgba(22, 42, 78, 0.55);
	}

	.ripple-disc.d2 {
		width: 196rpx;
		height: 196rpx;
		background: rgba(32, 64, 118, 0.62);
		animation-delay: 0.2s;
	}

	.ripple-disc.d1 {
		width: 128rpx;
		height: 128rpx;
		background: rgba(40, 82, 148, 0.82);
		animation-delay: 0.4s;
	}

	.ripple-core {
		width: 72rpx;
		height: 72rpx;
		background: #2b589a;
		animation-delay: 0.1s;
	}

	.measuring-text {
		margin-top: 36rpx;
		font-size: 30rpx;
		color: #4da3ff;
		line-height: 42rpx;
	}

	.measuring-sub {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
		text-align: center;
	}

	@keyframes disc-breathe {
		0%,
		100% {
			transform: scale(0.9);
		}

		50% {
			transform: scale(1);
		}
	}

	.cancel-body {
		position: relative;
		z-index: 1;
		padding: 48rpx 36rpx 40rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-sizing: border-box;
	}

	.cancel-icon {
		width: 88rpx;
		height: 88rpx;
	}

	.cancel-title {
		margin-top: 24rpx;
		font-size: 34rpx;
		font-weight: 600;
		color: #4da3ff;
		line-height: 48rpx;
	}

	.cancel-desc {
		margin-top: 16rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.72);
		line-height: 40rpx;
		text-align: center;
	}

	.cancel-btns {
		display: flex;
		align-items: center;
		width: 100%;
		margin-top: 40rpx;
	}

	.cancel-btn {
		flex: 1;
		height: 80rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30rpx;
		color: #ffffff;
		box-sizing: border-box;
	}

	.cancel-btn + .cancel-btn {
		margin-left: 24rpx;
	}

	.cancel-btn.ghost {
		background: transparent;
		border: 2rpx solid #3b82f6;
	}

	.cancel-btn.solid {
		background: linear-gradient(90deg, #1a53ec 0%, #3b82f6 50%, #60a5fa 100%);
	}
</style>
