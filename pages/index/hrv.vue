<template>
	<view class="page">
		<!-- 最近一次摘要 -->
		<view class="hero-card">
			<!-- 顶部卡片背景图，自行补充 src -->
			<image class="hero-bg" src="/static/hrvCardBg.png" mode=""></image>
			<view class="hero-body">
				<view class="hero-left">
					<text class="hero-label">最近一次 · {{ latestTime }}</text>
					<view class="hero-value-row">
						<text class="hero-num">{{ latestValue }}</text>
						<text class="hero-unit">ms</text>
					</view>
					<text class="hero-status">{{ latestStatus }}</text>
				</view>
				<!-- 波形装饰图，自行补充 src -->
				<image class="hero-wave" mode="aspectFit"></image>
				<view class="measure-btn" @click="onStartMeasure">
					<image src="/static/kaishiceliang.png" style="width:201rpx;height: 104rpx;" mode=""></image>
				</view>
			</view>
		</view>

		<!-- 日 / 周 / 月 -->
		<view class="period-bar">
			<view v-for="item in periodList" :key="item.key" class="period-item"
				:class="{ active: period === item.key }" @click="onPeriodChange(item.key)">
				<!-- 选中背景图，自行补充 src -->
				<image v-if="period === item.key" src="/static/confirmBg.png" class="period-item-bg" mode="scaleToFill">
				</image>
				<text class="period-text">{{ item.label }}</text>
			</view>
		</view>

		<!-- HRV趋势 -->
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

			<text class="card-title">HRV趋势</text>

			<view class="trend-chart">
				<view class="trend-body">
					<view class="y-axis">
						<text v-for="label in yLabels" :key="label" class="y-label">{{ label }}</text>
					</view>
					<view class="trend-plot" @touchstart="onChartTouch" @touchmove.stop.prevent="onChartTouch"
						@touchend="onChartTouchEnd" @touchcancel="onChartTouchEnd">
						<image v-if="chartImageSrc" class="trend-svg" :src="chartImageSrc" mode="scaleToFill" />
						<view v-if="touchActive && touchPoint" class="chart-tip" :style="chartTipStyle">
							<text class="chart-tip-value">{{ touchValue }} ms</text>
							<text class="chart-tip-time">{{ touchTimeText }}</text>
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
						<text class="stats-unit">ms</text>
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">最低值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsMin }}</text>
						<text class="stats-unit">ms</text>
					</view>
				</view>
				<view class="stats-divider"></view>
				<view class="stats-col">
					<text class="stats-label">平均值</text>
					<view class="stats-value-row">
						<text class="stats-num">{{ statsAvg }}</text>
						<text class="stats-unit">ms</text>
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
				<view v-for="(item, index) in recentList" :key="index" class="recent-row"
					:class="{ border: index < recentList.length - 1 }">
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
				<!-- 背景图，自行补充 src -->
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
						<view v-for="(cell, index) in calendarCells" :key="index" class="cal-cell"
							:class="{ empty: !cell.day, active: cell.active, disabled: cell.disabled }"
							@click="cell.day && !cell.disabled && onSelectCalDay(cell.day)">
							<!-- 选中日期背景图，自行补充 src -->
							<image v-if="cell.active" src="/static/dateSelect.png" class="cal-day-bg" mode="aspectFill">
							</image>
							<text v-if="cell.day" class="cal-day">{{ cell.day }}</text>
						</view>
					</view>

					<view class="cal-btns">
						<view class="cal-btn cancel" @click="closeCalendar">取消</view>
						<view class="cal-btn confirm" @click="confirmCalendar">
							<!-- 确定按钮背景图，自行补充 src -->
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
				<!-- 弹窗背景图，自行补充 src -->
				<image class="tip-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="tip-popup-body">
					<text class="tip-popup-title">检测注意事项</text>
					<!-- 佩戴示意，自行补充 src -->
					<image class="tip-popup-illust" src="/static/celiangTipsIcon.png" mode=""></image>
					<view class="tip-popup-card">
						<view v-for="(item, index) in measureTips" :key="item.no" class="tip-popup-row"
							:class="{ border: index < measureTips.length - 1 }">
							<view class="tip-popup-no">
								<text class="tip-popup-no-text">{{ item.no }}</text>
							</view>
							<text class="tip-popup-desc">{{ item.text }}</text>
						</view>
					</view>

					<view class="cal-btn confirm" style="margin-top: 46rpx;" @click="onMeasureNext">
						<!-- 确定按钮背景图，自行补充 src -->
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
					<text class="measuring-text">{{ liveHrv ? ('检测中 ' + liveHrv + ' ms') : '检测中...' }}</text>
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
	import { hasLogin } from '@/common/auth.js'
	import {
		getDeviceLatest,
		getHrvTrend,
		getDeviceDataList,
		uploadDeviceData
	} from '@/common/api/deviceData.js'

	const CHART_PAD_TOP = 12
	const CHART_PAD_BOTTOM = 8
	const Y_MIN = 0
	const Y_MAX = 120
	const MEASURE_TIMEOUT_MS = 45000
	const MEASURE_MIN_SAMPLES = 2
	const MEASURE_STABLE_MS = 8000

	function valueToY(value, chartH) {
		const usableH = chartH - CHART_PAD_TOP - CHART_PAD_BOTTOM
		const ratio = (Math.max(Y_MIN, Math.min(Y_MAX, value)) - Y_MIN) / (Y_MAX - Y_MIN)
		return CHART_PAD_TOP + usableH * (1 - ratio)
	}

	function extractHrvValues(data) {
		const list = Array.isArray(data) ? data : (data != null ? [data] : [])
		const values = []
		list.forEach((item) => {
			if (typeof item === 'number') {
				if (item > 0) values.push(item)
				return
			}
			if (!item || typeof item !== 'object') return
			const raw = item.value != null
				? item.value
				: (item.hrvNums != null
					? item.hrvNums
					: (item.hrv != null ? item.hrv : item.HRV))
			const n = Number(raw)
			if (!Number.isNaN(n) && n > 0) values.push(n)
		})
		return values
	}

	function hrvStatusText(hrv) {
		if (hrv < 20) return '偏低'
		if (hrv > 100) return '偏高'
		return '正常范围'
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

	function parseDateTime(value) {
		if (!value) return null
		if (value instanceof Date) return isNaN(value.getTime()) ? null : value
		if (typeof value === 'number') {
			const d = new Date(value < 1e12 ? value * 1000 : value)
			return isNaN(d.getTime()) ? null : d
		}
		const d = new Date(String(value).replace(/-/g, '/').replace('T', ' '))
		return isNaN(d.getTime()) ? null : d
	}

	function formatLatestTime(date = new Date()) {
		return `今天 ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
	}

	function formatRecordTime(value) {
		const d = parseDateTime(value)
		if (!d) return value ? String(value) : '--'
		const hm = pad2(d.getHours()) + ':' + pad2(d.getMinutes())
		const today = startOfDay(new Date())
		const that = startOfDay(d)
		const yest = new Date(today)
		yest.setDate(today.getDate() - 1)
		if (that.getTime() === today.getTime()) return '今天 ' + hm
		if (that.getTime() === yest.getTime()) return '昨天 ' + hm
		return (d.getMonth() + 1) + '月' + d.getDate() + '日 ' + hm
	}

	function formatMeasureTime(value) {
		const d = parseDateTime(value) || new Date()
		return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) +
			' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds())
	}

	function createMeasureId() {
		return 'hrv_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10)
	}

	function sourceText(source) {
		return String(source) === '2' ? '手动测量' : '戒指采集'
	}

	function unwrapList(data) {
		if (Array.isArray(data)) return data
		if (!data || typeof data !== 'object') return []
		if (Array.isArray(data.records)) return data.records
		if (Array.isArray(data.list)) return data.list
		if (Array.isArray(data.rows)) return data.rows
		return []
	}

	function pickHrvValue(item) {
		if (!item) return 0
		const n = Number(
			item.hrvNum != null ? item.hrvNum
				: (item.hrvNums != null ? item.hrvNums
					: (item.hrv != null ? item.hrv
						: (item.heartNum != null ? item.heartNum : item.value)))
		)
		return n > 0 ? n : 0
	}

	function pickXLabels(times, period) {
		if (!times.length) {
			if (period === 'week') return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
			if (period === 'month') return ['1日', '6日', '11日', '16日', '21日', '26日']
			return ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
		}
		const count = period === 'month' ? 6 : 7
		if (times.length <= count) return times
		const out = []
		for (let i = 0; i < count; i++) {
			out.push(times[Math.round(i * (times.length - 1) / (count - 1))])
		}
		return out
	}

	function toPoints(values, chartW, chartH) {
		const stepX = values.length === 1 ? 0 : chartW / (values.length - 1)
		return values.map((v, i) => ({
			x: i * stepX,
			y: valueToY(v, chartH)
		}))
	}

	/** Catmull-Rom → 三次贝塞尔，折线带圆角过渡 */
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

	export default {
		data() {
			return {
				latestTime: '--',
				latestValue: '--',
				latestStatus: '暂无数据',
				period: 'day',
				periodList: [{
						key: 'day',
						label: '日'
					},
					{
						key: 'week',
						label: '周'
					},
					{
						key: 'month',
						label: '月'
					}
				],
				currentDate: new Date(),
				chartW: 0,
				chartH: 220,
				chartValues: [],
				chartTimes: [],
				yLabels: [120, 90, 60, 30],
				xLabels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
				statsMax: '--',
				statsMin: '--',
				statsAvg: '--',
				chartLeft: 0,
				touchActive: false,
				touchIndex: -1,
				showCalendar: false,
				showMeasureTip: false,
				showMeasuring: false,
				showCancelConfirm: false,
				measuring: false,
				liveHrv: 0,
				measureSamples: [],
				measureTimer: null,
				measureStableTimer: null,
				hrvUnsub: null,
				measureTips: [{
						no: '1',
						text: '请正确佩戴设备，使设备光路对准指腹或手指两侧'
					},
					{
						no: '2',
						text: '测量过程中身体尽可能保持静止，请勿走动或晃动'
					}
				],
				calYear: new Date().getFullYear(),
				calMonth: new Date().getMonth(),
				calSelectedDay: new Date().getDate(),
				weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
				tipText: 'HRV表示连续心跳间期的细微变化，可用于观察自主神经调节、压力和恢复状态，更适合与自己的长期基线比较。',
				recentList: []
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
			chartPoints() {
				return toPoints(this.chartValues, this.chartW, this.chartH)
			},
			gridYs() {
				return [30, 60, 90].map((v) => valueToY(v, this.chartH))
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
					`<defs><linearGradient id="hrvAreaGrad" x1="0" y1="0" x2="0" y2="1">` +
					`<stop offset="0%" stop-color="#3B82F6" stop-opacity="0.35"/>` +
					`<stop offset="55%" stop-color="#3B82F6" stop-opacity="0.12"/>` +
					`<stop offset="100%" stop-color="#3B82F6" stop-opacity="0"/>` +
					`</linearGradient></defs>` +
					grids +
					`<path d="${this.areaPath}" fill="url(#hrvAreaGrad)"/>` +
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
			touchTimeText() {
				if (this.touchIndex < 0 || !this.chartValues.length) return ''
				if (this.chartTimes[this.touchIndex]) return this.chartTimes[this.touchIndex]
				const totalMin = 24 * 60
				const ratio = this.chartValues.length === 1 ? 0 : this.touchIndex / (this.chartValues.length - 1)
				const minutes = Math.round(totalMin * ratio)
				const h = String(Math.floor(minutes / 60) % 24).padStart(2, '0')
				const m = String(minutes % 60).padStart(2, '0')
				return `${h}:${m}`
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
					cells.push({
						day: 0,
						active: false,
						disabled: false
					})
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
					cells.push({
						day: 0,
						active: false,
						disabled: false
					})
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
		onShow() {
			this.loadPageData()
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
						this.chartH = rect.height || 220
						this.chartLeft = rect.left || 0
					})
					.exec()
			},
			async loadPageData() {
				await Promise.all([
					this.loadLatest(),
					this.loadTrend(),
					this.loadRecent()
				])
			},
			async loadLatest() {
				try {
					const data = await getDeviceLatest('hrv')
					this.applyLatest(data)
				} catch (e) {}
			},
			applyLatest(data) {
				const value = pickHrvValue(data)
				if (!data || !value) {
					this.latestValue = '--'
					this.latestStatus = '暂无数据'
					this.latestTime = '--'
					return
				}
				this.latestValue = value
				this.latestStatus = hrvStatusText(value)
				this.latestTime = formatRecordTime(data.measureTime)
			},
			async loadTrend() {
				try {
					const data = await getHrvTrend(dateToYmd(this.currentDate), this.period)
					this.applyTrend(data)
				} catch (e) {
					this.applyTrend(null)
				}
			},
			applyTrend(data) {
				if (!data) {
					this.chartValues = []
					this.chartTimes = []
					this.xLabels = pickXLabels([], this.period)
					this.statsMax = '--'
					this.statsMin = '--'
					this.statsAvg = '--'
					return
				}
				const series = Array.isArray(data.series) ? data.series : []
				const points = series.filter((p) => Number(p.value) > 0)
				this.chartValues = points.map((p) => Number(p.value))
				this.chartTimes = points.map((p) => p.time || '')
				this.xLabels = pickXLabels(this.chartTimes.length ? this.chartTimes : series.map((p) => p.time || '').filter(Boolean), this.period)
				const stats = data.stats || {}
				this.statsMax = Number(stats.maxValue) > 0 ? Math.round(Number(stats.maxValue)) : '--'
				this.statsMin = Number(stats.minValue) > 0 ? Math.round(Number(stats.minValue)) : '--'
				this.statsAvg = Number(stats.avgValue) > 0 ? Math.round(Number(stats.avgValue)) : '--'
				if (data.description || data.desc) this.tipText = data.description || data.desc
				this.$nextTick(() => this.measureChart(0))
			},
			async loadRecent() {
				try {
					const end = new Date()
					const start = new Date()
					start.setDate(end.getDate() - 30)
					const list = await getDeviceDataList({
						dataType: 'hrv',
						startDate: dateToYmd(start),
						endDate: dateToYmd(end)
					})
					this.applyRecent(unwrapList(list))
				} catch (e) {}
			},
			applyRecent(list) {
				const rows = unwrapList(list)
				const sorted = rows.slice().sort((a, b) => {
					const ta = parseDateTime(a.measureTime || a.fullTime)
					const tb = parseDateTime(b.measureTime || b.fullTime)
					return (tb ? tb.getTime() : 0) - (ta ? ta.getTime() : 0)
				})
				this.recentList = sorted.slice(0, 10).map((item) => {
					const value = pickHrvValue(item)
					return {
						time: formatRecordTime(item.measureTime || item.fullTime),
						source: sourceText(item.source),
						duration: value > 0 ? (value + ' ms') : '--'
					}
				})
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
				this.liveHrv = 0
				this.measureSamples = []
				this.clearMeasureTimer()
				this.unbindHrv()

				this.hrvUnsub = CoolWearBLE.on('onHRV', (data) => {
					this.onHrvData(data)
				})

				try {
					await CoolWearBLE.setHRVSwitch(true)
					console.log('[hrv] 已开启 HRV 检测')
				} catch (e) {
					console.error('[hrv] 开启 HRV 失败', e)
					this.stopRealMeasure(false)
					uni.showToast({
						title: (e && e.message) || '开启 HRV 失败',
						icon: 'none'
					})
					return
				}

				this.measureTimer = setTimeout(() => {
					if (!this.measuring) return
					if (this.liveHrv > 0) {
						this.finishMeasure(this.liveHrv)
						return
					}
					this.stopRealMeasure(true)
					uni.showToast({
						title: '未收到 HRV 数据，请确认佩戴后重试',
						icon: 'none',
						duration: 2500
					})
				}, MEASURE_TIMEOUT_MS)
			},
			onHrvData(data) {
				if (!this.measuring) return
				const values = extractHrvValues(data)
				if (!values.length) {
					console.log('[hrv] onHRV 原始数据无法解析', data)
					return
				}
				values.forEach((v) => this.measureSamples.push(v))
				const latest = values[values.length - 1]
				this.liveHrv = latest
				console.log('[hrv] 实时 HRV', latest, 'samples=', this.measureSamples.length)

				if (this.measureSamples.length >= MEASURE_MIN_SAMPLES && !this.measureStableTimer) {
					this.measureStableTimer = setTimeout(() => {
						if (!this.measuring) return
						const hrv = this.liveHrv || this.measureSamples[this.measureSamples.length - 1]
						this.finishMeasure(hrv)
					}, MEASURE_STABLE_MS)
				}
			},
			async stopRealMeasure(turnOffSwitch = true) {
				this.clearMeasureTimer()
				this.unbindHrv()
				this.measuring = false
				this.showMeasuring = false
				this.showCancelConfirm = false
				this.liveHrv = 0
				this.measureSamples = []
				if (turnOffSwitch) {
					try {
						await CoolWearBLE.setHRVSwitch(false)
					} catch (e) {
						console.log('[hrv] 关闭 HRV 失败', e)
					}
				}
			},
			async finishMeasure(hrv) {
				const value = Number(hrv) || this.liveHrv
				if (!value) {
					await this.stopRealMeasure(true)
					uni.showToast({
						title: '测量失败，请重试',
						icon: 'none'
					})
					return
				}
				this.latestValue = value
				this.latestStatus = hrvStatusText(value)
				this.latestTime = formatLatestTime()
				await this.stopRealMeasure(true)

				let measureId = createMeasureId()
				uni.showLoading({ title: '保存中', mask: true })
				try {
					await this.uploadHrvOnce(value, measureId)
				} catch (e) {
					measureId = ''
					console.log('[hrv] upload failed', e && (e.msg || e.message))
				} finally {
					uni.hideLoading()
				}

				let url = '/pages/index/hrvSuccess?hrv=' + value
				if (measureId) url += '&measureId=' + encodeURIComponent(measureId)
				uni.navigateTo({ url })
			},
			async uploadHrvOnce(hrv, measureId) {
				if (!hasLogin()) {
					return Promise.reject(new Error('未登录'))
				}
				const ble = BleService.getState()
				const device = (ble && ble.device) || {}
				await uploadDeviceData({
					deviceMac: device.mac || device.id || '',
					deviceVersion: (ble && ble.firmware) || '',
					batteryLevel: ble && ble.batteryLevel != null ? ble.batteryLevel : undefined,
					dataList: [{
						dataType: 'hrv',
						heartNum: Number(hrv),
						measureTime: formatMeasureTime(new Date()),
						source: '2',
						measureId
					}]
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
		border-radius: 28rpx;
		background: linear-gradient(135deg, #1a3a6e 0%, #0d1f3d 55%, #0a1628 100%);
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

	.hero-wave {
		flex: 1;
		height: 80rpx;
		margin: 0 16rpx;
		min-width: 80rpx;
	}

	.measure-btn {}

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
		border-radius: 20rpx 20rpx 20rpx 20rpx;
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

	.date-arrow {
		font-size: 40rpx;
		color: #ffffff;
		line-height: 48rpx;
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
		background: #212F34;
		line-height: 44rpx;
	}

	.trend-tag-text {
		font-size: 22rpx;
		color: #64EBBA;
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
		color: #8e8e93;
		line-height: 38rpx;
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
		z-index: 1000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
		padding: 0 30rpx 30rpx;
	}

	.cal-popup {
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

	.cal-cell.active {
		border-radius: 12rpx;
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

	.cal-btn+.cal-btn {
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

	.tip-mask {
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
		padding: 0 30rpx 30rpx;
	}

	.tip-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #1a1b25;
	}

	.tip-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
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

	.tip-popup-next {
		margin-top: 36rpx;
		height: 88rpx;
		border-radius: 44rpx;
		background: linear-gradient(90deg, #1a53ec 0%, #3b82f6 50%, #4da3ff 100%);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.tip-popup-next-text {
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 42rpx;
	}

	.measuring-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1001;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
		padding: 0 30rpx 30rpx;
	}

	.measuring-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #12131a;
	}

	.measuring-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
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

	.cancel-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1002;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
		padding: 0 30rpx 30rpx;
	}

	.cancel-popup {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #12131a;
	}

	.cancel-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
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

	.cancel-btn+.cancel-btn {
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