<template>
	<view class="page">
		<!-- 最近一次摘要 -->
		<view class="hero-card">
			<!-- 顶部卡片背景图，自行补充 src -->
			<image class="hero-bg" src="/static/tiwenBg.png" mode=""></image>
			<view class="hero-body">
				<view class="hero-left">
					<text class="hero-label">最近一次 · {{ latestTime }}</text>
					<view class="hero-value-row">
						<text class="hero-num">{{ latestValueText }}</text>
						<text class="hero-unit">°C</text>
					</view>
					<text class="hero-status">{{ latestStatus }}</text>
				</view>
				<view class="measure-btn" @click="onEnterWeight">
					<text class="measure-btn-text">录入体温</text>
				</view>
			</view>
		</view>

		<!-- 体温状态 -->
		<view class="card bmi-card">
			<view class="bmi-header">
				<text class="bmi-title">体温</text>
				<view class="bmi-badge">
					<text class="bmi-badge-text">{{ latestStatus }}</text>
				</view>
			</view>
			<text class="bmi-value">{{ sugarValueText }}</text>
			<view class="bmi-scale">
				<view class="bmi-track">
					<!-- 体温刻度条图片 -->
					<image class="bmi-img" src="/static/luruKedu.png" mode=""></image>
					<image
						class="bmi-pointer"
						:style="{ left: bmiPointerLeft }"
						src="/static/zhizhen.png"
						mode="aspectFit"
					></image>
				</view>
			</view>
			<text class="bmi-desc">{{ tipText }}</text>
			<view class="sugar-footer">
				<text class="sugar-footer-left">建议管理目标</text>
				<text class="sugar-footer-right">{{ targetHint }}</text>
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
				<image v-if="period === item.key" src="/static/confirmBg.png" class="period-item-bg" mode="scaleToFill"></image>
				<text class="period-text">{{ item.label }}</text>
			</view>
		</view>

		<!-- 体温趋势 -->
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

			<view class="trend-title-row">
				<text class="card-title">体温趋势</text>
				<text class="trend-time">{{ trendTime }}</text>
			</view>

			<view class="trend-chart">
				<view class="trend-body">
					<view class="y-axis">
						<text v-for="label in yLabels" :key="label" class="y-label">{{ label }}</text>
					</view>
					<view
						class="trend-plot"
						@touchstart="onChartTouch"
						@touchmove.stop="onChartTouch"
						@touchend="onChartTouchEnd"
						@touchcancel="onChartTouchEnd"
					>
						<image v-if="chartImageSrc" class="trend-svg" :src="chartImageSrc" mode="scaleToFill" />
						<view
							v-if="touchActive && touchPoint"
							class="chart-tip"
							:style="chartTipStyle"
						>
							<text class="chart-tip-value">{{ touchValue }} °C</text>
							<text class="chart-tip-time">{{ touchLabel }}</text>
						</view>
					</view>
				</view>
				<view class="trend-x">
					<text v-for="(label, idx) in xLabels" :key="idx" class="trend-x-label">{{ label }}</text>
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

		<!-- 说明 -->
		<view class="tip-card">
			<image class="tip-bg" src="/static/heartCardBg.png" mode=""></image>
			<view class="tip-header">
				<image class="tip-icon" src="/static/qushijianyi.png" mode=""></image>
				<image src="/static/shuoming.png" style="width: 70rpx;height: 34rpx;" mode=""></image>
			</view>
			<view class="tip-text">{{ tipText }}</view>
		</view>

		<!-- 体温变化记录 -->
		<view class="section">
			<view class="section-head">
				<text class="section-title">体温变化记录</text>
				<text class="section-action" @click="onEnterWeight">新增记录</text>
			</view>
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
					<text class="recent-duration">{{ item.valueText }}</text>
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
						<view class="cal-nav" :class="{ disabled: !canCalNextMonth }" @click="onCalNextMonth">
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
							:class="{ empty: !cell.day, active: cell.active, disabled: cell.disabled }"
							@click="cell.day && !cell.disabled && onSelectCalDay(cell.day)"
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

		<!-- 录入体温 -->
		<view v-if="showEnter" class="cal-mask" @click="closeEnter">
			<view class="enter-popup" @click.stop>
				<image class="cal-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="enter-body">
					<text class="enter-title">手动记录体温</text>
					<text class="enter-desc">手动数据会标注来源，与戒指数据分开展示</text>
					<text class="enter-label">体温 (°C)</text>
					<view class="enter-input-row">
						<input
							class="enter-input"
							type="digit"
							v-model="enterValue"
							placeholder="请输入数值"
							placeholder-class="enter-placeholder"
						/>
					</view>
					<view class="cal-btns">
						<view class="cal-btn cancel" @click="closeEnter">取消</view>
						<view class="cal-btn confirm" @click="confirmEnter">
							<image class="cal-btn-bg" src="/static/confirmBg.png" mode=""></image>
							<text class="cal-btn-text">保存</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { toSvgDataUrl } from '@/common/svg-data-url.js'
	import { getBodyTemperatureTrend, addManualHealthRecord } from '@/common/api/manualHealth.js'
	const CHART_PAD_TOP = 12
	const CHART_PAD_BOTTOM = 8
	const Y_MIN = 30
	const Y_MAX = 45

	const TEMP_LEVELS = [
		{ key: 'low', label: '偏低', min: 30, max: 36.0, color: '#5B9FFF' },
		{ key: 'normal', label: '正常', min: 36.0, max: 37.2, color: '#3EE0C3' },
		{ key: 'watch', label: '偏高', min: 37.2, max: 38.0, color: '#F5C451' },
		{ key: 'fever', label: '发热', min: 38.0, max: 39.0, color: '#FF9F43' },
		{ key: 'high', label: '高热', min: 39.0, max: 41.0, color: '#FF7A45' },
		{ key: 'abnormal', label: '异常', min: 41.0, max: 45, color: '#E74C3C' }
	]

	function valueToY(value, chartH) {
		const usableH = chartH - CHART_PAD_TOP - CHART_PAD_BOTTOM
		const ratio = (Math.max(Y_MIN, Math.min(Y_MAX, value)) - Y_MIN) / (Y_MAX - Y_MIN)
		return CHART_PAD_TOP + usableH * (1 - ratio)
	}

	function toPoints(values, chartW, chartH) {
		const stepX = values.length === 1 ? 0 : chartW / (values.length - 1)
		return values.map((v, i) => ({
			x: i * stepX,
			y: valueToY(v, chartH)
		}))
	}

	function buildSmoothLinePath(points) {
		if (!points.length) return ''
		if (points.length === 1) return `M${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`

		let d = `M${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`
		for (let i = 0; i < points.length - 1; i++) {
			const p0 = points[i - 1] || points[i]
			const p1 = points[i]
			const p2 = points[i + 1]
			const p3 = points[i + 2] || p2
			const cp1x = p1.x + (p2.x - p0.x) / 6
			const cp1y = p1.y + (p2.y - p0.y) / 6
			const cp2x = p2.x - (p3.x - p1.x) / 6
			const cp2y = p2.y - (p3.y - p1.y) / 6
			d += ` C${cp1x.toFixed(2)} ${cp1y.toFixed(2)}, ${cp2x.toFixed(2)} ${cp2y.toFixed(2)}, ${p2.x.toFixed(2)} ${p2.y.toFixed(2)}`
		}
		return d
	}

	function buildAreaPath(lineD, points, chartH) {
		if (!lineD || !points.length) return ''
		const last = points[points.length - 1]
		const first = points[0]
		const bottom = chartH
		return `${lineD} L${last.x.toFixed(2)} ${bottom} L${first.x.toFixed(2)} ${bottom} Z`
	}

	function tempToPercent(value) {
		const count = TEMP_LEVELS.length
		const segW = 100 / count
		if (value == null || value === '') {
			return segW + segW / 2
		}
		for (let i = 0; i < count; i++) {
			const level = TEMP_LEVELS[i]
			if (value < level.max || i === count - 1) {
				const span = level.max - level.min
				const ratio = span <= 0 ? 0 : Math.max(0, Math.min(1, (value - level.min) / span))
				return i * segW + ratio * segW
			}
		}
		return 100
	}

	function tempStatusOf(value) {
		for (let i = 0; i < TEMP_LEVELS.length; i++) {
			if (value < TEMP_LEVELS[i].max || i === TEMP_LEVELS.length - 1) {
				return TEMP_LEVELS[i].label
			}
		}
		return '正常'
	}

	function pad2(n) {
		return n < 10 ? '0' + n : String(n)
	}

	function dateToYmd(d) {
		return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
	}

	function startOfDay(d) {
		return new Date(d.getFullYear(), d.getMonth(), d.getDate())
	}

	function isFutureDate(year, month, day) {
		return startOfDay(new Date(year, month, day)).getTime() > startOfDay(new Date()).getTime()
	}

	function pickXLabels(labels) {
		const times = (labels || []).filter(Boolean)
		if (!times.length) return ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
		if (times.length <= 7) return times
		// day 24 点：按服务端 labels 等距取 00/04/08/12/16/20/最后一个
		const out = []
		for (let i = 0; i < times.length; i += 4) out.push(times[i])
		if (out[out.length - 1] !== times[times.length - 1]) out.push(times[times.length - 1])
		return out.slice(0, 7)
	}

	export default {
		data() {
			return {
				latestTime: '--',
				latestValue: null,
				latestValueText: '--',
				latestStatus: '暂无数据',
				sugarValueText: '--',
				targetHint: '建议保持在 36.0~37.2°C',
				apiAdvice: '',
				period: 'week',
				periodList: [
					{ key: 'day', label: '日' },
					{ key: 'week', label: '周' },
					{ key: 'month', label: '月' }
				],
				currentDate: new Date(),
				trendTime: '',
				chartW: 0,
				chartH: 220,
				chartValues: [],
				chartTimes: [],
				yLabels: [45, 40, 35, 30],
				xLabels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
				statsMax: '--',
				statsMin: '--',
				statsAvg: '--',
				chartLeft: 0,
				touchActive: false,
				touchIndex: -1,
				showCalendar: false,
				showEnter: false,
				enterValue: '',
				saving: false,
				apiPosition: null,
				calYear: new Date().getFullYear(),
				calMonth: new Date().getMonth(),
				calSelectedDay: new Date().getDate(),
				weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
				tipText: '体温需结合测量部位与时间解读。请记录测量时间；如多次结果明显超出个人正常范围，建议咨询专业医生。',
				recentList: []
			}
		},
		computed: {
			bmiPointerLeft() {
				if (this.apiPosition != null && this.apiPosition !== '') {
					return Number(this.apiPosition) + '%'
				}
				return `${tempToPercent(this.latestValue)}%`
			},
			dateText() {
				const d = this.currentDate
				const y = d.getFullYear()
				const m = String(d.getMonth() + 1).padStart(2, '0')
				const day = String(d.getDate()).padStart(2, '0')
				return `${y}年${m}月${day}日`
			},
			chartPoints() {
				return toPoints(this.chartValues, this.chartW, this.chartH)
			},
			gridYs() {
				return [35, 40].map((v) => valueToY(v, this.chartH))
			},
			linePath() {
				return buildSmoothLinePath(this.chartPoints)
			},
			areaPath() {
				return buildAreaPath(this.linePath, this.chartPoints, this.chartH)
			},
			chartImageSrc() {
				if (!this.chartW || !this.chartH || !this.linePath) return ''
				const grids = (this.gridYs || []).map((y) =>
					`<line x1="0" y1="${y}" x2="${this.chartW}" y2="${y}" stroke="rgba(255,255,255,0.12)" stroke-width="1" stroke-dasharray="4 5"/>`
				).join('')
				let touch = ''
				if (this.touchActive && this.touchPoint) {
					touch =
						`<line x1="${this.touchPoint.x}" y1="0" x2="${this.touchPoint.x}" y2="${this.chartH}" stroke="rgba(255,255,255,0.35)" stroke-width="1"/>` +
						`<circle cx="${this.touchPoint.x}" cy="${this.touchPoint.y}" r="5" fill="#3B82F6" stroke="#ffffff" stroke-width="2"/>`
				}
				const inner =
					`<defs><linearGradient id="tempAreaGrad" x1="0" y1="0" x2="0" y2="1">` +
					`<stop offset="0%" stop-color="#3B82F6" stop-opacity="0.35"/>` +
					`<stop offset="55%" stop-color="#3B82F6" stop-opacity="0.12"/>` +
					`<stop offset="100%" stop-color="#3B82F6" stop-opacity="0"/>` +
					`</linearGradient></defs>` +
					grids +
					`<path d="${this.areaPath}" fill="url(#tempAreaGrad)"/>` +
					`<path d="${this.linePath}" fill="none" stroke="#3B82F6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>` +
					touch +
					`<line x1="0" y1="${this.chartH - 1}" x2="${this.chartW}" y2="${this.chartH - 1}" stroke="rgba(255,255,255,0.28)" stroke-width="1"/>`
				return toSvgDataUrl(inner, this.chartW, this.chartH, 'preserveAspectRatio="none"')
			},
			touchPoint() {
				if (this.touchIndex < 0 || this.touchIndex >= this.chartPoints.length) return null
				return this.chartPoints[this.touchIndex]
			},
			touchValue() {
				if (this.touchIndex < 0) return 0
				return this.chartValues[this.touchIndex]
			},
			touchLabel() {
				if (this.touchIndex < 0) return ''
				if (this.chartTimes[this.touchIndex]) return this.chartTimes[this.touchIndex]
				return this.xLabels[this.touchIndex] || ''
			},
			chartTipStyle() {
				if (!this.touchPoint) return {}
				const tipW = 110
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
			canCalNextMonth() {
				const today = new Date()
				if (this.calYear > today.getFullYear()) return false
				if (this.calYear === today.getFullYear() && this.calMonth >= today.getMonth()) return false
				return true
			},
			calendarCells() {
				const year = this.calYear
				const month = this.calMonth
				const daysInMonth = new Date(year, month + 1, 0).getDate()
				const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7
				const cells = []
				for (let i = 0; i < firstWeekday; i++) {
					cells.push({ day: 0, active: false, disabled: false })
				}
				for (let d = 1; d <= daysInMonth; d++) {
					const disabled = isFutureDate(year, month, d)
					cells.push({
						day: d,
						active: !disabled && d === this.calSelectedDay,
						disabled
					})
				}
				while (cells.length % 7 !== 0) {
					cells.push({ day: 0, active: false, disabled: false })
				}
				return cells
			}
		},
		mounted() {
			this.$nextTick(() => {
				this.measureChart(0)
			})
		},
		onShow() {
			this.loadTrend()
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
						this.chartH = rect.height || 220
						this.chartLeft = rect.left || 0
					})
					.exec()
			},
			async loadTrend() {
				try {
					const data = await getBodyTemperatureTrend({
						range: this.period,
						date: dateToYmd(this.currentDate),
						pageNum: 1,
						pageSize: 10
					})
					this.applyTrend(data)
				} catch (e) {
					this.applyTrend(null)
				}
			},
			applyTrend(data) {
				if (!data) {
					this.latestValue = null
					this.latestValueText = '--'
					this.sugarValueText = '--'
					this.latestStatus = '暂无数据'
					this.latestTime = '--'
					this.apiPosition = null
					this.chartValues = []
					this.chartTimes = []
					this.xLabels = pickXLabels([])
					this.statsMax = '--'
					this.statsMin = '--'
					this.statsAvg = '--'
					this.recentList = []
					this.trendTime = ''
					return
				}
				const latest = data.latest || {}
				const info = data.bodyTemperature || {}
				const latestNum = Number(latest.value != null ? latest.value : info.value)
				this.latestValue = latestNum > 0 ? latestNum : null
				this.latestValueText = this.latestValue != null ? String(this.latestValue) : '--'
				this.sugarValueText = info.value != null && Number(info.value) > 0 ? String(info.value) : this.latestValueText
				this.latestStatus = info.status || latest.subText || (this.latestValue != null ? tempStatusOf(this.latestValue) : '暂无数据')
				this.latestTime = latest.timeText || '--'
				this.apiPosition = info.position != null ? info.position : null
				this.targetHint = data.targetHint || '建议保持在 36.0~37.2°C'
				if (data.advice || data.desc) this.tipText = data.advice || data.desc
				this.trendTime = (data.trend && data.trend.title) || ''

				const labels = (data.trend && data.trend.labels) || []
				const values = ((data.trend && data.trend.values) || []).map((v) => Number(v) || 0)
				// 保留服务端完整长度，0 不过滤，X 轴按 labels 等距展开
				this.chartValues = values
				this.chartTimes = labels
				this.xLabels = pickXLabels(labels.length ? labels : this.chartTimes)

				const stats = data.stats || {}
				this.statsMax = Number(stats.max) > 0 ? Number(stats.max) : '--'
				this.statsMin = Number(stats.min) > 0 ? Number(stats.min) : '--'
				this.statsAvg = Number(stats.avg) > 0 ? Number(Number(stats.avg).toFixed(1)) : '--'

				const records = Array.isArray(data.records) ? data.records : []
				this.recentList = records.map((item) => ({
					time: item.timeText || '--',
					source: item.source || '手动录入',
					valueText: (item.value || '--') + ' ' + (item.unit || '°C')
				}))
				this.$nextTick(() => this.measureChart(0))
			},
			onChartTouch(e) {
				const touch = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
				if (!touch || !this.chartW || !this.chartValues.length) return
				const x = touch.clientX - this.chartLeft
				const len = this.chartValues.length
				const step = len === 1 ? 0 : this.chartW / (len - 1)
				let index = step === 0 ? 0 : Math.round(x / step)
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
				if (this.period === key) return
				this.period = key
				this.loadTrend()
			},
			shiftDate(delta) {
				const next = new Date(this.currentDate)
				if (this.period === 'week') next.setDate(next.getDate() + delta * 7)
				else if (this.period === 'month') next.setMonth(next.getMonth() + delta)
				else next.setDate(next.getDate() + delta)
				if (startOfDay(next).getTime() > startOfDay(new Date()).getTime()) {
					uni.showToast({
						title: '不能选择今天之后的日期',
						icon: 'none'
					})
					return
				}
				this.currentDate = next
				this.loadTrend()
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
				if (!this.canCalNextMonth) return
				if (this.calMonth === 11) {
					this.calYear += 1
					this.calMonth = 0
				} else {
					this.calMonth += 1
				}
				const maxDay = new Date(this.calYear, this.calMonth + 1, 0).getDate()
				if (this.calSelectedDay > maxDay) this.calSelectedDay = maxDay
				if (isFutureDate(this.calYear, this.calMonth, this.calSelectedDay)) {
					const today = new Date()
					this.calYear = today.getFullYear()
					this.calMonth = today.getMonth()
					this.calSelectedDay = today.getDate()
				}
			},
			onSelectCalDay(day) {
				if (isFutureDate(this.calYear, this.calMonth, day)) {
					uni.showToast({
						title: '不能选择今天之后的日期',
						icon: 'none'
					})
					return
				}
				this.calSelectedDay = day
			},
			confirmCalendar() {
				if (isFutureDate(this.calYear, this.calMonth, this.calSelectedDay)) {
					uni.showToast({
						title: '不能选择今天之后的日期',
						icon: 'none'
					})
					return
				}
				this.currentDate = new Date(this.calYear, this.calMonth, this.calSelectedDay)
				this.showCalendar = false
				this.loadTrend()
			},
			onEnterWeight() {
				this.enterValue = ''
				this.showEnter = true
			},
			closeEnter() {
				this.showEnter = false
			},
			async confirmEnter() {
				if (this.saving) return
				const value = parseFloat(this.enterValue)
				if (!value || value < 30 || value > 45) {
					uni.showToast({ title: '请输入30~45之间的体温值', icon: 'none' })
					return
				}
				const temp = Math.round(value * 10) / 10
				this.saving = true
				try {
					await addManualHealthRecord({
						type: '3',
						value: String(temp)
					})
					this.showEnter = false
					uni.showToast({ title: '录入成功', icon: 'success' })
					await this.loadTrend()
				} catch (e) {
					console.log('[temperature] add failed', e && (e.msg || e.message))
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
		justify-content: space-between;
		height: 100%;
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
		font-weight: bold;
		font-size: 40rpx;
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

	.measure-btn {
		flex-shrink: 0;
		padding: 16rpx 28rpx;
		border-radius: 40rpx;
		background: linear-gradient(90deg, #1a53ec 0%, #2f7bff 50%, #4da3ff 100%);
		box-shadow: 0 0 20rpx rgba(47, 123, 255, 0.45);
	}

	.measure-btn-text {
		font-size: 26rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 36rpx;
	}

	.bmi-card {
		margin-top: 28rpx;
	}

	.bmi-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.bmi-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.bmi-badge {
		padding: 8rpx 18rpx;
		border-radius: 24rpx;
		background: rgba(47, 123, 255, 0.18);
		border: 1rpx solid rgba(77, 163, 255, 0.35);
		box-sizing: border-box;
	}

	.bmi-badge-text {
		font-size: 22rpx;
		color: #64b5f6;
		line-height: 30rpx;
	}

	.bmi-value {
		display: block;
		margin-top: 20rpx;
		font-size: 64rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 80rpx;
	}

	.bmi-scale {
		margin-top: 16rpx;
	}

	.bmi-labels {
		display: flex;
		align-items: center;
		margin-bottom: 12rpx;
	}

	.bmi-label {
		flex: 1;
		text-align: center;
		font-size: 20rpx;
		line-height: 28rpx;
	}

	.bmi-track {
		position: relative;
		width: 100%;
		height: 53rpx;
	}

	.bmi-img {
		width: 100%;
		height: 53rpx;
		border-radius: 14rpx;
	}

	.bmi-pointer {
		position: absolute;
		top: 42rpx;
		width: 22rpx;
		height: 28rpx;
		margin-left: -11rpx;
		z-index: 2;
		pointer-events: none;
	}

	.bmi-desc {
		display: block;
		margin-top: 28rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 40rpx;
	}

	.sugar-footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 24rpx;
	}

	.sugar-footer-left {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 34rpx;
	}

	.sugar-footer-right {
		font-size: 24rpx;
		color: #4da3ff;
		line-height: 34rpx;
	}

	.period-bar {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-top: 28rpx;
		padding: 6rpx;
		background: #1A1B25;
		border-radius: 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
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
		border-radius: 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
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

	.trend-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.card-title {
		display: block;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
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

	.section {
		margin-top: 36rpx;
	}

	.section-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
	}

	.section-head .section-title {
		margin-bottom: 0;
	}

	.section-title {
		display: block;
		margin-bottom: 20rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.section-action {
		font-size: 26rpx;
		color: #4da3ff;
		line-height: 36rpx;
	}

	.stats-card {
		display: flex;
		align-items: stretch;
		padding: 36rpx 0;
		background: #1A1B25;
		border-radius: 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
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

	.cal-mask {
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
		z-index: 1000;
	}

	.cal-popup,
	.enter-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #1a1b25;
	}

	.cal-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.cal-body,
	.enter-body {
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

	.cal-nav.disabled {
		opacity: 0.35;
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

	.cal-cell.disabled .cal-day {
		opacity: 0.28;
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

	.enter-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.enter-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.enter-label {
		display: block;
		margin-top: 36rpx;
		margin-bottom: 16rpx;
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.enter-input-row {
		display: flex;
		align-items: center;
		padding: 24rpx 28rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.enter-input {
		flex: 1;
		height: 56rpx;
		font-size: 30rpx;
		color: #ffffff;
		line-height: 56rpx;
	}

	.enter-placeholder {
		color: #8e8e93;
		font-weight: 400;
	}
</style>
