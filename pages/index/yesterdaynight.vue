<template>
	<view class="page">
		<!-- 最近一次摘要 -->
		<view class="hero-card" @click="openSleepDetail">
			<!-- 整卡装饰背景图 -->
			<image class="hero-bg" src="/static/yearertodayBg.png" mode=""></image>
			<text class="hero-label">最近一次</text>
			<view class="hero-duration">
				<text class="hero-num">{{ sleepHours }}</text>
				<text class="hero-unit">小时</text>
				<text class="hero-num">{{ sleepMinutes }}</text>
				<text class="hero-unit">分</text>
			</view>
			<text class="hero-status">{{ recoverStatus }}</text>
		</view>

		<!-- 睡眠周期 -->
		<view class="card cycle-card">
			<view class="date-bar">
				<image class="date-bar-bg" src="/static/selectDateBg.png" mode=""></image>
				<view class="date-btn" @click="onPrevDay">
					<image src="/static/arrLeft.png" mode=""></image>
				</view>
				<view class="date-center" @click="openCalendar">
					<text class="date-text">{{ dateText }}</text>
				</view>
				<view class="date-btn" @click="onNextDay">
					<image src="/static/arrRight.png" mode=""></image>
				</view>
			</view>

			<text class="card-title">睡眠周期</text>
			<view class="cycle-meta">
				<text class="cycle-meta-label">夜间睡眠时长</text>
				<view class="cycle-meta-value">
					<text class="cycle-meta-num">{{ sleepHours }}</text>
					<text class="cycle-meta-unit">小时</text>
					<text class="cycle-meta-num">{{ sleepMinutes }}</text>
					<text class="cycle-meta-unit">分</text>
				</view>
			</view>

			<view class="cycle-chart">
				<view class="cycle-y">
					<view v-for="(label, idx) in stageLabels" :key="idx" class="cycle-y-label">{{ label }}</view>
				</view>
				<view class="cycle-main">
					<!-- Canvas圆角阶梯波形图 hypnogram -->
					<view class="cycle-plot" @touchstart="onChartTouch" @touchmove.stop.prevent="onChartTouch"
						@touchend="onChartTouchEnd">
						<canvas class="cycle-canvas" canvas-id="sleepCanvas" id="sleepCanvas"></canvas>
						<view v-if="touchValue.show" class="cycle-tip" :style="{ left: touchValue.tipLeft }">
							<text class="cycle-tip-text">{{ touchValue.label }}</text>
						</view>
					</view>
					<view class="cycle-x">
						<text class="cycle-x-label">{{ sleepStart }}</text>
						<text class="cycle-x-label">醒来 {{ wakeTime }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 睡眠分析概览 -->
		<view class="section">
			<text class="section-title">睡眠分析概览</text>
			<view class="card overview-card">
				<view class="overview-top">
					<image src="/static/sleepBg.png" class="fenBg" mode=""></image>
					<view class="overCon">
						<view class="score-block">
							<text class="score-label">睡眠得分</text>
							<view class="score-row">
								<text class="score-num">{{ sleepScore }}</text>
								<text class="score-unit">分</text>
							</view>
						</view>
						<view class="quality-block">
							<view class="quality-text-row">
								<text class="quality-prefix">睡眠质量</text>
								<text class="quality-keyword">{{ qualityKeyword }}</text>
							</view>
							<view class="quality-scale">
								<image class="scale-img" src="/static/sleepKedu.png" mode="widthFix"></image>
								<image class="scale-pointer" :style="{ left: qualityMarkerLeft }"
									src="/static/zhizhen.png" mode="aspectFit"></image>
							</view>
						</view>
					</view>

				</view>

				<view class="structure-block">
					<view class="" style="display: flex;align-items: center;margin-bottom: 30rpx;">
						<text class="structure-title">睡眠结构</text>
						<text class="structure-link" style="margin-left: 20rpx;">各睡眠区间</text>
					</view>
			
					<view v-for="(item, index) in structureList" :key="item.key" class="structure-row"
						:class="{ 'structure-row-last': index === structureList.length - 1 }">
						<text class="structure-name">{{ item.name }}</text>
						<text class="structure-duration">{{ item.duration }}</text>
						<view class="structure-track">
							<view class="structure-fill" :style="{ width: item.percent + '%', background: item.color }">
							</view>
						</view>
						<text class="structure-percent">{{ item.percent }}%</text>
					</view>
				</view>
			</view>
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

		<!-- 睡眠记录详情弹窗 -->
		<view v-if="showSleepDetail" class="detail-mask" @click="closeSleepDetail">
			<view class="detail-popup" @click.stop>
				<image class="detail-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="detail-body">
					<view class="detail-head">
						<text class="detail-title">睡眠记录详情</text>
						<text class="detail-time">{{ detailTimeText }}</text>
					</view>

					<!-- 最近一次摘要卡片 -->
					<view class="detail-hero">
						<image class="detail-hero-bg" src="/static/yearertodayBg.png" mode=""></image>
						<text class="detail-hero-label">最近一次</text>
						<view class="detail-hero-duration">
							<text class="detail-hero-num">{{ sleepHours }}</text>
							<text class="detail-hero-unit">小时</text>
							<text class="detail-hero-num">{{ sleepMinutes }}</text>
							<text class="detail-hero-unit">分</text>
						</view>
						<text class="detail-hero-status">{{ recoverStatus }}</text>
					</view>

					<!-- 数据来源 / 同步状态 -->
					<view class="detail-info-row">
						<view class="detail-info-item">
							<view class="detail-info-head">
								<!-- 数据来源图标（待补路径） -->
								<image class="detail-info-icon" src="/static/shuiQu.png" mode=""></image>
								<text class="detail-info-label">数据来源</text>
							</view>
							<text class="detail-info-value">近7天</text>
						</view>
						<view class="detail-info-item">
							<view class="detail-info-head">
								<!-- 同步状态图标（待补路径） -->
								<image class="detail-info-icon" src="/static/shuiTong.png" mode=""></image>
								<text class="detail-info-label">同步状态</text>
							</view>
							<text class="detail-info-value">{{ recoverStatus === '等待同步' ? '待同步' : '已同步' }}</text>
						</view>
					</view>

					<!-- 返回趋势按钮 -->
					<view class="detail-btn" @click="closeSleepDetail">
						<text class="detail-btn-text">返回趋势</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		toSvgDataUrl
	} from '@/common/svg-data-url.js'
	import BleService from '@/common/ble-service.js'
	import { getSleepTrend } from '@/common/api/deviceData.js'

	/**
	 * 睡眠分期圆角阶梯面积图（hypnogram）
	 * 上沿按阶段阶梯变化，向下填满；转角圆弧，清醒尖峰圆顶
	 */
	const STAGE_LEVEL = {
		awake: 0,
		rem: 1,
		light: 2,
		deep: 3
	}

	const DEFAULT_SEGMENTS = [{
			stage: 'light',
			dur: 1
		},
		{
			stage: 'deep',
			dur: 4
		},
		{
			stage: 'light',
			dur: 3.5
		},
		{
			stage: 'deep',
			dur: 4.5
		},
		{
			stage: 'light',
			dur: 7
		},
		{
			stage: 'rem',
			dur: 4
		},
		{
			stage: 'light',
			dur: 3.5
		},
		{
			stage: 'awake',
			dur: 0.55
		},
		{
			stage: 'light',
			dur: 3
		},
		{
			stage: 'rem',
			dur: 5.5
		},
		{
			stage: 'awake',
			dur: 0.55
		},
		{
			stage: 'rem',
			dur: 4
		},
		{
			stage: 'light',
			dur: 9
		}
	]

	function stageY(stage, chartH, padTop, padBottom) {
		const usable = chartH - padTop - padBottom
		return padTop + (usable / 4) * STAGE_LEVEL[stage]
	}

	function buildBandArea(x1, x2, top, bottom, fill, radius = 7) {
		const r = Math.min(radius, Math.abs(bottom - top) / 2, (x2 - x1) / 2)
		const left = x1 + r
		const right = x2 - r
		return {
			d: [
				`M ${left.toFixed(2)} ${top.toFixed(2)}`,
				`L ${right.toFixed(2)} ${top.toFixed(2)}`,
				`Q ${x2.toFixed(2)} ${top.toFixed(2)} ${x2.toFixed(2)} ${(top + r).toFixed(2)}`,
				`L ${x2.toFixed(2)} ${(bottom - r).toFixed(2)}`,
				`Q ${x2.toFixed(2)} ${bottom.toFixed(2)} ${right.toFixed(2)} ${bottom.toFixed(2)}`,
				`L ${left.toFixed(2)} ${bottom.toFixed(2)}`,
				`Q ${x1.toFixed(2)} ${bottom.toFixed(2)} ${x1.toFixed(2)} ${(bottom - r).toFixed(2)}`,
				`L ${x1.toFixed(2)} ${(top + r).toFixed(2)}`,
				`Q ${x1.toFixed(2)} ${top.toFixed(2)} ${left.toFixed(2)} ${top.toFixed(2)} Z`
			].join(' '),
			fill
		}
	}

	function buildHypnogramAreas(segments, chartW, chartH, padTop, padBottom) {
		if (!segments || !segments.length) {
			return {
				areas: [],
				connectors: []
			}
		}
		const list = segments
		const total = list.reduce((sum, item) => sum + item.dur, 0) || 1
		const stageOrder = ['awake', 'rem', 'light', 'deep']
		const levels = stageOrder.map((stage) => stageY(stage, chartH, padTop, padBottom))
		const bottomY = chartH - padBottom
		const areas = []
		const connectors = []
		let x = 0
		let previousLevel = -1
		list.forEach((seg) => {
			const nextX = x + (seg.dur / total) * chartW
			const level = stageOrder.indexOf(seg.stage)
			const safeLevel = level >= 0 ? level : 2
			const top = levels[safeLevel]
			const bottom = safeLevel === levels.length - 1 ? bottomY : levels[safeLevel + 1]
			const fill = safeLevel <= 1 ? '#8B7CFF' : '#5B32D6'

			if (previousLevel >= 0 && previousLevel !== safeLevel) {
				const previousY = levels[previousLevel]
				connectors.push({
					x,
					y1: previousY,
					y2: top,
					stroke: fill
				})
			}
			areas.push(buildBandArea(x, nextX, top, bottom, fill, 8))
			previousLevel = safeLevel
			x = nextX
		})
		return {
			areas,
			connectors
		}
	}

	function dateToYmd(d) {
		const y = d.getFullYear()
		const m = d.getMonth() + 1
		const day = d.getDate()
		return y + '-' + (m < 10 ? '0' + m : m) + '-' + (day < 10 ? '0' + day : day)
	}

	function startOfDay(d) {
		return new Date(d.getFullYear(), d.getMonth(), d.getDate())
	}

	function isFutureDate(year, month, day) {
		const target = new Date(year, month, day)
		return startOfDay(target).getTime() > startOfDay(new Date()).getTime()
	}

	function formatClock(value) {
		if (!value) return '--:--'
		const raw = String(value)
		const hm = raw.match(/(\d{1,2}:\d{2})/)
		if (hm && raw.indexOf('T') === -1 && raw.length <= 8) return hm[1].length === 4 ? '0' + hm[1] : hm[1]
		const d = new Date(raw.replace(/-/g, '/').replace('T', ' '))
		if (isNaN(d.getTime())) {
			return hm ? (hm[1].length === 4 ? '0' + hm[1] : hm[1]) : '--:--'
		}
		const h = d.getHours()
		const mi = d.getMinutes()
		return (h < 10 ? '0' + h : h) + ':' + (mi < 10 ? '0' + mi : mi)
	}

	function parseTimeMs(value) {
		if (!value) return 0
		const d = new Date(String(value).replace(/-/g, '/').replace('T', ' '))
		return isNaN(d.getTime()) ? 0 : d.getTime()
	}

	function clockFromMs(ms) {
		if (!ms) return '--:--'
		const d = new Date(ms)
		if (isNaN(d.getTime())) return '--:--'
		const h = d.getHours()
		const mi = d.getMinutes()
		return (h < 10 ? '0' + h : h) + ':' + (mi < 10 ? '0' + mi : mi)
	}

	function formatRecentDate(ymd, minutes) {
		if (!ymd) return '最近'
		const parts = String(ymd).split('-')
		if (parts.length < 3) return ymd
		const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
		const now = new Date()
		const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
		const yest = new Date(today)
		yest.setDate(today.getDate() - 1)
		const cur = new Date(d.getFullYear(), d.getMonth(), d.getDate())
		if (cur.getTime() === today.getTime()) return '今天'
		if (cur.getTime() === yest.getTime()) return '昨天'
		return (d.getMonth() + 1) + '月' + d.getDate() + '日'
	}

	const STAGE_BY_VALUE = {
		1: 'light',
		2: 'deep',
		3: 'light',
		4: 'awake',
		5: 'rem'
	}

	function formatDuration(mins) {
		const m = Math.max(0, Math.round(Number(mins) || 0))
		const h = Math.floor(m / 60)
		const mm = m % 60
		if (h <= 0) return `${mm}分`
		return `${h}小时${mm}分`
	}

	function formatRecentTime(sec) {
		if (!sec) return '最近'
		const d = new Date(sec * 1000)
		const now = new Date()
		const hh = String(d.getHours()).padStart(2, '0')
		const mm = String(d.getMinutes()).padStart(2, '0')
		const sameDay =
			d.getFullYear() === now.getFullYear() &&
			d.getMonth() === now.getMonth() &&
			d.getDate() === now.getDate()
		const yest = new Date(now)
		yest.setDate(now.getDate() - 1)
		const isYest =
			d.getFullYear() === yest.getFullYear() &&
			d.getMonth() === yest.getMonth() &&
			d.getDate() === yest.getDate()
		if (sameDay) return `今天 ${hh}:${mm}`
		if (isYest) return `昨天 ${hh}:${mm}`
		return `${d.getMonth() + 1}月${d.getDate()}日 ${hh}:${mm}`
	}

	function emptyStructure() {
		return [{
				key: 'deep',
				name: '深睡',
				duration: '0分',
				percent: 0,
				color: '#7B61FF'
			},
			{
				key: 'light',
				name: '浅睡',
				duration: '0分',
				percent: 0,
				color: '#A78BFA'
			},
			{
				key: 'rem',
				name: '快速眼动',
				duration: '0分',
				percent: 0,
				color: '#A78BFA'
			},
			{
				key: 'awake',
				name: '清醒',
				duration: '0分',
				percent: 0,
				color: '#FF9F0A'
			}
		]
	}

	export default {
		data() {
			return {
				sleepHours: 0,
				sleepMinutes: 0,
				recoverStatus: '等待同步',
				currentDate: new Date(),
				sleepStart: '--:--',
				wakeTime: '--:--',
				sleepScore: 0,
				qualityText: '暂无睡眠数据',
				qualityMarkerPercent: 8,
				stageLabels: ['清醒', '快速眼动', '浅睡', '深睡'],
				chartW: 0,
				chartH: 0,
				padTop: 6,
				padBottom: 6,
				cycleSegments: [],
				canvasCtx: null,
				canvasW: 0,
				canvasH: 0,
				bandRadius: 5,
				chartRects: [],
				sleepSample: null,
				chartStartMs: 0,
				chartEndMs: 0,
				chartLeft: 0,
				chartTop: 0,
				touchValue: {},
				movaX: 0,
				_lastDrawKey: '',
				structureList: emptyStructure(),
				recentList: [],
				showCalendar: false,
				showSleepDetail: false,
				calYear: new Date().getFullYear(),
				calMonth: new Date().getMonth(),
				calSelectedDay: new Date().getDate(),
				weekLabels: ['一', '二', '三', '四', '五', '六', '日']
			}
		},
		computed: {
			dateText() {
				const d = this.currentDate
				const y = d.getFullYear()
				const m = d.getMonth() + 1
				const day = d.getDate()
				return `${y}年${m}月${day}日`
			},
			calendarTitle() {
				return `${this.calYear}年${this.calMonth + 1}月`
			},
			detailTimeText() {
				const d = this.currentDate
				const now = new Date()
				const sameDay = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
				const hh = String(now.getHours()).padStart(2, '0')
				const mm = String(now.getMinutes()).padStart(2, '0')
				return `${sameDay ? '今天' : this.dateText} ${hh}:${mm}`
			},
			canCalNextMonth() {
				const today = new Date()
				return this.calYear < today.getFullYear() ||
					(this.calYear === today.getFullYear() && this.calMonth < today.getMonth())
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
			},
			qualityMarkerLeft() {
				const p = Number(this.qualityMarkerPercent)
				const safe = Number.isNaN(p) ? 0 : Math.max(0, Math.min(100, p))
				return `${safe}%`
			},
			qualityKeyword() {
				const t = this.qualityText || ''
				if (t.indexOf('优秀') >= 0 || t.indexOf('很好') >= 0) return '很好'
				if (t.indexOf('良好') >= 0 || t.indexOf('较好') >= 0) return '较好'
				if (t.indexOf('偏差') >= 0 || t.indexOf('较差') >= 0) return '较差'
				if (t.indexOf('一般') >= 0) return '一般'
				if (t.indexOf('暂无') >= 0) return ''
				return ''
			},
			gridLineYs() {
				if (!this.canvasH) return []
				const usable = this.canvasH - this.padTop - this.padBottom
				return [0, 1, 2, 3, 4].map((level) => this.padTop + (usable / 4) * level)
			},
			tipColor() {
				const m = { awake: '#A78BFA', rem: '#A78BFA', light: '#7B61FF', deep: '#7B61FF' }
				return m[this.touchValue.stage] || '#7B61FF'
			}
		},
		created() {
			this.loadTrend()
		},
		mounted() {
			this.$nextTick(() => {
				setTimeout(() => {
					this.measureChart(0)
				}, 50)
			})
		},
		onReady() {
			this.measureChart(0)
		},
		onShow() {
			this.refreshSleep()
		},
		methods: {
			measureChart(retry = 0) {
				uni.createSelectorQuery()
					.in(this)
					.select('.cycle-plot')
					.boundingClientRect((rect) => {
						if (!rect || !rect.width) {
							if (retry < 8) setTimeout(() => this.measureChart(retry + 1), 80)
							return
						}
						this.chartW = rect.width
						this.chartH = rect.height
						this.canvasW = rect.width
						this.canvasH = rect.height
						this.chartLeft = rect.left || 0
						this.chartTop = rect.top || 0
						if (!this.canvasCtx) this.canvasCtx = uni.createCanvasContext('sleepCanvas', this)
						this.initSample()
					})
					.exec()
			},
			stageColor(stage) {
				if (stage === 'light' || stage === 'deep') return '#7B61FF'
				return '#A78BFA'
			},
			stageIndex(stage) {
				return { awake: 0, rem: 1, light: 2, deep: 3 }[stage] ?? 2
			},
			stageName(stage) {
				return { awake: '清醒', rem: '快速眼动', light: '浅睡', deep: '深睡' }[stage] || '浅睡'
			},
			// ===== 示例原样照搬：睡眠波图表 =====
			sampleColors() { return ['#A78BFA', '#A78BFA', '#7B61FF', '#7B61FF'] },
			sampleY(H) {
				const padTop = 12
				const padBottom = 24
				const step = (H - padTop - padBottom) / 3
				return [padTop, padTop + step, padTop + step * 2, padTop + step * 3]
			},
			buildSleepData() {
				let ts = Number(this.chartStartMs) || 0
				if (!ts) {
					const t = new Date(this.currentDate); t.setHours(22, 0, 0, 0)
					ts = t.getTime()
				}
				return this.cycleSegments.map((s) => {
					const durMs = Math.max(60000, Number(s.dur || 1) * 60000)
					const o = { startTimestamp: ts, endTimestamp: ts + durMs, state: this.stageIndex(s.stage), stage: s.stage }
					ts += durMs
					return o
				})
			},
			initSample() {
				const W = this.canvasW, H = this.canvasH
				const sleepData = this.buildSleepData()
				if (!W || !H) return
				if (!sleepData.length) {
					if (this.canvasCtx) {
						this.canvasCtx.clearRect(0, 0, W, H)
						this.canvasCtx.draw()
					}
					this.sleepSample = null
					this.touchValue = {}
					return
				}
				const yValue = this.sampleY(H)
				const padX = 4
				const usable = Math.max(1, W - padX * 2)
				const widths = sleepData.map(it => Math.max(1, it.endTimestamp - it.startTimestamp))
				const total = widths.reduce((a, b) => a + b, 0) || 1
				// 先按比例分配像素宽度（保留最小可见宽度），再统一缩放保证总宽不溢出
				let px = widths.map(w => Math.max(2, (w / total) * usable))
				const sumPx = px.reduce((a, b) => a + b, 0)
				if (sumPx > usable) {
					const k = usable / sumPx
					px = px.map(v => v * k)
				}
				let xValue = padX
				const yHeight = H * 0.13
				for (let i = 0; i < sleepData.length; i++) {
					const item = sleepData[i]
					const state = item.state
					const w = Math.max(1, px[i])
					item.rect = { x: xValue, y: yValue[state], w, h: yHeight }
					if (i < sleepData.length - 1) {
						const ns = sleepData[i + 1].state
						item.line = {
							x1: xValue + w, y1: yValue[state] + (state > ns ? yHeight - this.bandRadius : this.bandRadius),
							x2: xValue + w, y2: yValue[ns] + (state > ns ? this.bandRadius : yHeight - this.bandRadius)
						}
					}
					xValue += w
				}
				for (let i = 0; i < sleepData.length; i++) {
					const item = sleepData[i]
					const up = i > 0 ? sleepData[i - 1] : null
					const nx = i < sleepData.length - 1 ? sleepData[i + 1] : null
					item.left = up ? up.state < item.state : false
					item.right = nx ? item.state > nx.state : false
				}
				this.sleepSample = sleepData
				this.draftSample()
			},
			draftSample() {
				const context = this.canvasCtx
				if (!context || !this.sleepSample) return
				context.clearRect(0, 0, this.canvasW, this.canvasH)
				// Y轴横线：4档各一条
				try {
					const yValue = this.sampleY(this.canvasH)
					context.setLineWidth(1)
					context.setStrokeStyle('rgba(255,255,255,0.16)')
					try { if (context.setLineDash) context.setLineDash([4, 4]) } catch (e) {}
					yValue.slice(1).forEach((y) => {
						context.beginPath()
						context.moveTo(0, y)
						context.lineTo(this.canvasW, y)
						context.stroke()
					})
					try { if (context.setLineDash) context.setLineDash([]) } catch (e) {}
				} catch (e) {}
				const _sy = this.sampleY(this.canvasH)
				const gg = context.createLinearGradient(0, _sy[0], 0, _sy[3])
				const c = this.sampleColors()
				gg.addColorStop(0, c[0]); gg.addColorStop(0.33, c[1]); gg.addColorStop(0.66, c[2]); gg.addColorStop(1, c[3])
				context.setLineWidth(1); context.setStrokeStyle(gg); context.setFillStyle(gg)
				// 先画阶段之间的连接线，再用色块覆盖穿过块内部的线
				for (let i = 0; i < this.sleepSample.length - 1; i++) {
					const item = this.sleepSample[i]
					if (item.line) {
						context.beginPath()
						context.moveTo(item.line.x1, item.line.y1)
						context.lineTo(item.line.x2, item.line.y2)
						context.stroke()
					}
				}
				for (let i = 0; i < this.sleepSample.length; i++) {
					const item = this.sleepSample[i]
					context.beginPath()
					this.drawSampleRect(context, item.rect.x - 0.55, item.rect.y, item.rect.w + 1, item.rect.h, item.left, item.right)
					context.fill()
				}
				if (this.touchValue && this.touchValue.show && this.touchValue.touchX) {
					const gline = context.createLinearGradient(0, 0, 0, this.canvasH)
					gline.addColorStop(0, 'rgba(255,255,255,0)'); gline.addColorStop(0.5, 'rgba(255,255,255,0.85)'); gline.addColorStop(1, 'rgba(255,255,255,0)')
					context.setStrokeStyle(gline); context.setFillStyle(gline)
					context.fillRect(this.touchValue.touchX, 0, 1, this.canvasH)
					context.fill()
				}
				context.draw()
			},
			drawSampleRect(context, x, y, width, height, left, right) {
				let rset = this.bandRadius
				if (width < this.bandRadius * 2) rset = width / 2
				const lr = left ? -1 * rset : rset
				const rr = right ? -1 * rset : rset
				const arr = []
				arr.push([x + rset, y + lr, Math.PI, left ? Math.PI / 2 : Math.PI * 3 / 2])
				arr.push([x + width - rset, y])
				arr.push([x + width - rset, y + rr, right ? Math.PI / 2 : Math.PI * 3 / 2, 0])
				arr.push([x + width, y + height + rr])
				arr.push([x + width - rset, y + height + rr, 0, right ? Math.PI / 2 : Math.PI * 3 / 2])
				arr.push([x + rset, y + height])
				arr.push([x + rset, y + height + lr, left ? Math.PI / 2 : Math.PI * 3 / 2, Math.PI])
				arr.push([x, y + rr])
				context.beginPath()
				context.arc(arr[0][0], arr[0][1], rset, arr[0][2], arr[0][3], left)
				context.lineTo(arr[1][0], arr[1][1])
				context.arc(arr[2][0], arr[2][1], rset, arr[2][2], arr[2][3], right)
				context.lineTo(arr[3][0], arr[3][1])
				context.arc(arr[4][0], arr[4][1], rset, arr[4][2], arr[4][3], !right)
				context.lineTo(arr[5][0], arr[5][1])
				context.arc(arr[6][0], arr[6][1], rset, arr[6][2], arr[6][3], !left)
				context.lineTo(arr[7][0], arr[7][1])
				context.closePath()
			},
			formatChartTime(timestamp) {
				const date = new Date(timestamp)
				const hh = String(date.getHours()).padStart(2, '0')
				const mm = String(date.getMinutes()).padStart(2, '0')
				return `${hh}:${mm}`
			},
			onChartTouch(e) {
			if (!this.sleepSample || !this.sleepSample.length) return
			const touch = e.touches && e.touches[0]
			if (!touch) return
			let x
			if (typeof touch.x === 'number') x = touch.x
			else x = (Number(touch.clientX) || 0) - this.chartLeft
			x = Math.max(0, Math.min(this.canvasW, x))
			const item = this.sleepSample.find((it) => x >= it.rect.x && x <= it.rect.x + it.rect.w) ||
				this.sleepSample[this.sleepSample.length - 1]
			if (!item) return
			const ratio = Math.max(0, Math.min(1, (x - item.rect.x) / Math.max(1, item.rect.w)))
			const timestamp = item.startTimestamp + (item.endTimestamp - item.startTimestamp) * ratio
			const stage = ['awake', 'rem', 'light', 'deep'][item.state]
			const label = `${this.stageName(stage)} ${this.formatChartTime(timestamp)}`
			this.touchValue = {
				show: true,
				stage,
				touchX: x,
				label,
				tipLeft: `${Math.max(0, Math.min(this.canvasW, x - 70))}px`
			}
			this.draftSample()
		},
			onChartTouchEnd() {
			setTimeout(() => {
				this.touchValue = {}
				this.draftSample()
			}, 1800)
		},

			async refreshSleep() {
				try {
					if (BleService.getState().connected) {
						await BleService.syncSleep()
					}
				} catch (e) {}
				this.loadTrend()
			},
			async loadTrend() {
				this.recentList = []
				try {
					const data = await getSleepTrend(dateToYmd(this.currentDate), 'day')
					this.applyTrend(data)
				} catch (e) {
					this.applyTrend(null)
				}
			},
			applyTrend(data) {
				const series = data && Array.isArray(data.series) ? data.series : []
				const segs = data && Array.isArray(data.segments) ? data.segments : []
				let totalMin = 0
				series.forEach((p) => {
					totalMin += Number(p.value2) || 0
				})
				if (!totalMin) {
					segs.forEach((s) => {
						totalMin += Number(s.value) || 0
					})
				}
				if (!data || !(totalMin > 0)) {
					this.sleepHours = 0
					this.sleepMinutes = 0
					this.recoverStatus = '暂无数据'
					this.sleepStart = '--:--'
					this.wakeTime = '--:--'
					this.sleepScore = 0
					this.qualityText = '暂无睡眠数据'
					this.qualityMarkerPercent = 8
					this.structureList = emptyStructure()
					this.cycleSegments = []
					this.chartStartMs = 0
					this.chartEndMs = 0
					this.sleepSample = null
					this.touchValue = {}
					this.recentList = []
					if (this.canvasW && this.canvasH) this.initSample()
					this.measureChart(0)
					return
				}

				this.sleepHours = Math.floor(totalMin / 60)
				this.sleepMinutes = totalMin % 60
				this.recoverStatus = data.status || data.scoreText || '恢复一般'

				let firstTime = ''
				let lastTime = ''
				if (series.length) {
					firstTime = series[0].fullTime || series[0].time || ''
					lastTime = (data.latest && data.latest.fullTime) || series[series.length - 1].fullTime || series[series.length - 1].time || ''
				}
				const parsedStart = firstTime ? new Date(String(firstTime).replace(/-/g, '/').replace('T', ' ')).getTime() : 0
				const parsedEnd = lastTime ? new Date(String(lastTime).replace(/-/g, '/').replace('T', ' ')).getTime() : 0
				this.chartStartMs = parsedStart || (parsedEnd && totalMin ? parsedEnd - totalMin * 60000 : 0)
				this.chartEndMs = parsedEnd || (this.chartStartMs && totalMin ? this.chartStartMs + totalMin * 60000 : 0)

				if (series.length) {
					this.sleepStart = firstTime ? formatClock(firstTime) : '--:--'
					this.wakeTime = lastTime ? formatClock(lastTime) : '--:--'
				} else {
					this.sleepStart = '--:--'
					this.wakeTime = '--:--'
				}
				this.sleepScore = Number(data.score) || 0
				this.qualityText = data.scoreText ? ('睡眠质量' + data.scoreText) : (data.status || '睡眠质量一般')
				this.qualityMarkerPercent = this.sleepScore > 0 ? Math.max(4, Math.min(96, this.sleepScore)) : 8

				const mapped = {
					deep: { min: 0, percent: 0 },
					light: { min: 0, percent: 0 },
					rem: { min: 0, percent: 0 },
					awake: { min: 0, percent: 0 }
				}
				segs.forEach((seg) => {
					const name = String(seg.name || '')
					const min = Number(seg.value) || 0
					const percent = Number(seg.percent) || 0
					if (name.indexOf('深') !== -1) mapped.deep = { min, percent }
					else if (name.indexOf('浅') !== -1) mapped.light = { min, percent }
					else if (name.indexOf('眼动') !== -1 || name.toLowerCase().indexOf('rem') !== -1) mapped.rem = { min, percent }
					else if (name.indexOf('醒') !== -1 || name.indexOf('清醒') !== -1) mapped.awake = { min, percent }
				})
				this.structureList = [{
						key: 'deep',
						name: '深睡',
						duration: formatDuration(mapped.deep.min),
						percent: mapped.deep.percent,
						color: '#7B61FF'
					},
					{
						key: 'light',
						name: '浅睡',
						duration: formatDuration(mapped.light.min),
						percent: mapped.light.percent,
						color: '#A78BFA'
					},
					{
						key: 'rem',
						name: '快速眼动',
						duration: formatDuration(mapped.rem.min),
						percent: mapped.rem.percent,
						color: '#64D2FF'
					},
					{
						key: 'awake',
						name: '清醒',
						duration: formatDuration(mapped.awake.min),
						percent: mapped.awake.percent,
						color: '#FF9F0A'
					}
				]

				this.cycleSegments = series.map((p) => ({
					stage: STAGE_BY_VALUE[Number(p.value)] || 'light',
					dur: Number(p.value2) || 1
				})).filter((item) => item.dur > 0)
				this.applyRecent(data.recentList)
				this.touchValue = {}
				this.sleepSample = null
				this._lastDrawKey = ''
				if (this.canvasW && this.canvasH) {
					if (!this.canvasCtx) this.canvasCtx = uni.createCanvasContext('sleepCanvas', this)
					this.initSample()
				}
				this.measureChart(0)
				setTimeout(() => {
					if (!this.canvasCtx) this.canvasCtx = uni.createCanvasContext('sleepCanvas', this)
					if (this.canvasW && this.canvasH) this.initSample()
				}, 80)
			},
			applyRecent(list) {
				const rows = Array.isArray(list) ? list : []
				this.recentList = rows.map((item) => ({
					time: formatRecentDate(item.date),
					source: '戒指采集',
					duration: formatDuration(item.totalMinutes).replace('小时', ' 小时 ').replace('分', ' 分')
				}))
			},
			shiftDate(delta) {
				const next = new Date(this.currentDate)
				next.setDate(next.getDate() + delta)
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
			openSleepDetail() {
				this.showSleepDetail = true
			},
			closeSleepDetail() {
				this.showSleepDetail = false
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
				if (isFutureDate(this.calYear, this.calMonth, this.calSelectedDay)) {
					const today = new Date()
					this.calSelectedDay = today.getDate()
				}
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
			}
		}
	}
</script>
<style>
	page{
		background-color: #000;
	}
</style>
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
		padding: 36rpx 32rpx 40rpx;
		border-radius: 28rpx;
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

	.hero-label {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.75);
		line-height: 36rpx;
	}

	.hero-duration {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: baseline;
		margin-top: 16rpx;
	}

	.hero-num {
		font-size: 72rpx;
		font-weight: 700;
		line-height: 88rpx;
		color: #ffffff;
	}

	.hero-unit {
		margin: 0 12rpx 0 8rpx;
		font-size: 28rpx;
		color: rgba(255, 255, 255, 0.9);
		line-height: 40rpx;
	}

	.hero-status {
		position: relative;
		z-index: 1;
		display: block;
		margin-top: 12rpx;
		font-size: 26rpx;
		color: #64B5F6;
		line-height: 36rpx;
	}

	.card {
		margin-top: 28rpx;
		padding: 32rpx 28rpx 28rpx;
		border-radius: 28rpx;
		background: #1A1B25;
		box-sizing: border-box;
	}

	.cycle-card {
		padding-top: 24rpx;
	}

	.date-bar {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 354rpx;
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

	.detail-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1100;
		display: flex;
		align-items: flex-end;
		justify-content: center;
		padding: 0 30rpx 30rpx;
		box-sizing: border-box;
		background: rgba(0, 0, 0, 0.7);
		animation: detailMaskIn 0.2s ease-out;
	}

	@keyframes detailMaskIn {
		from {
			background: rgba(0, 0, 0, 0);
		}

		to {
			background: rgba(0, 0, 0, 0.7);
		}
	}

	.detail-popup {
		position: relative;
		width: 100%;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		border-radius: 40rpx;
		overflow: hidden;
		background: #191a24;
		box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.45);
		animation: detailPopupUp 0.28s cubic-bezier(0.22, 1, 0.36, 1);
	}

	@keyframes detailPopupUp {
		from {
			transform: translateY(100%);
		}

		to {
			transform: translateY(0);
		}
	}

	.detail-popup-bg,
	.detail-hero-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
	}

	.detail-popup-bg {
		opacity: 0.22;
	}

	.detail-body {
		position: relative;
		z-index: 1;
		padding: 38rpx 36rpx 34rpx;
	}

	.detail-head {
		margin-bottom: 28rpx;
	}

	.detail-title {
		display: block;
		font-size: 38rpx;
		font-weight: 600;
		line-height: 52rpx;
		color: #ffffff;
	}

	.detail-time {
		display: block;
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.65);
	}

	.detail-hero {
		position: relative;
		height: 246rpx;
		padding: 30rpx 28rpx;
		box-sizing: border-box;
		overflow: hidden;
		border-radius: 28rpx;
	}

	.detail-hero-label,
	.detail-hero-duration,
	.detail-hero-status {
		position: relative;
		z-index: 1;
	}

	.detail-hero-label {
		display: block;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.72);
	}

	.detail-hero-duration {
		display: flex;
		align-items: baseline;
		margin-top: 12rpx;
	}

	.detail-hero-num {
		font-size: 70rpx;
		font-weight: 600;
		line-height: 78rpx;
		color: #ffffff;
	}

	.detail-hero-unit {
		margin: 0 10rpx 0 7rpx;
		font-size: 25rpx;
		color: rgba(255, 255, 255, 0.9);
	}

	.detail-hero-status {
		display: block;
		margin-top: 10rpx;
		font-size: 23rpx;
		line-height: 32rpx;
		color: #76bfff;
	}

	.detail-info-row {
		display: flex;
		margin-top: 28rpx;
	}

	.detail-info-item {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		height: 148rpx;
		padding: 22rpx 20rpx;
		box-sizing: border-box;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		border-radius: 20rpx;
		background: rgba(27, 28, 39, 0.72);
	}

	.detail-info-item:first-child {
		margin-left: 0;
	}

	.detail-info-head {
		display: flex;
		align-items: center;
	}

	.detail-info-icon {
		width: 34rpx;
		height: 34rpx;
		margin-right: 8rpx;
	}

	.detail-info-label {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.72);
	}

	.detail-info-value {
		display: block;
		margin-top: 14rpx;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.detail-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 88rpx;
		margin-top: 30rpx;
		border: 2rpx solid #2C89FF;
		border-radius: 22rpx;
		background: rgba(24, 26, 37, 0.72);
	}

	.detail-btn-text {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
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

	.cal-cell.disabled .cal-day {
		color: rgba(255, 255, 255, 0.28);
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

	.card-title {
		display: block;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.cycle-meta {
		display: flex;
		align-items: baseline;
		justify-content: space-between;
		margin-top: 12rpx;
	}

	.cycle-meta-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.cycle-meta-value {
		display: flex;
		align-items: baseline;
	}

	.cycle-meta-num {
		font-size: 40rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 48rpx;
	}

	.cycle-meta-unit {
		margin: 0 8rpx 0 6rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.cycle-chart {
		display: flex;
		margin-top: 28rpx;
	}

	.cycle-y {
		width: 110rpx;
		flex-shrink: 0;
		height: 280rpx;
		padding-top: 4rpx;
		padding-bottom: 4rpx;
		box-sizing: border-box;
	}

	.cycle-y-label {
		font-size: 20rpx;
		color: #8e8e93;
		line-height: 28rpx;
		margin-top: 36rpx;
	}
	.cycle-y-label:first-child{
		margin-top: 20rpx;
	}
	.cycle-y-label:nth-child(2){
		margin-top: 56rpx;
	}
	.cycle-y-label:last-child{
		margin-top: 28rpx;
	}
	
	

	.cycle-main {
		flex: 1;
		min-width: 0;
	}

	.cycle-plot {
		position: relative;
		width: 100%;
		height: 269.46rpx;
	}

	.cycle-canvas {
		width: 100%;
		height: 100%;
		display: block;
	}

	.cycle-tip {
		position: absolute;
		top: 8rpx;
		z-index: 2;
		max-width: 280rpx;
		padding: 8rpx 16rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.18);
		border-radius: 18rpx;
		background: rgba(20, 21, 30, 0.94);
		pointer-events: none;
	}

	.cycle-tip-text {
		font-size: 22rpx;
		color: #fff;
		line-height: 32rpx;
	}

	.cycle-mova {
		width: 100%;
		height: 36rpx;
		margin-top: 12rpx;
		background: rgba(255,255,255,0.08);
		border-radius: 18rpx;
	}

	.cycle-dot {
		width: 36rpx;
		height: 36rpx;
		border-radius: 50%;
		background: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cycle-dot-line {
		width: 4rpx;
		height: 20rpx;
		background: #666;
	}

	.cycle-x {
		display: flex;
		justify-content: space-between;
		margin-top: 12rpx;
		padding: 0 4rpx;
	}

	.cycle-x-label {
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

	.overview-card {
		margin-top: 0;
	}

	.overview-top {
		width: 100%;
		height: 137.45rpx;
		position: relative;
	}

	.fenBg {
		width: 100%;
		height: 100%;
	}

	.overCon {
		position: absolute;
		width: 100%;
		height: 100%;
		padding: 18rpx 28rpx;
		box-sizing: border-box;
		top: 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.score-block {
		flex-shrink: 0;
	}

	.score-label {
		font-size: 28rpx;
		color: #ffffff;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
			-webkit-background-clip: text;
			background-clip: text;
			-webkit-text-fill-color: transparent;
			color: transparent;
	}

	.score-row {
		display: flex;
		align-items: baseline;
		margin-top: 8rpx;
	}

	.score-num {
		font-size: 48rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.score-unit {
		margin-left: 6rpx;
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.quality-block {
		width: 360rpx;
		flex-shrink: 0;
		margin-left: 20rpx;
	}

	.quality-text-row {
		display: flex;
		align-items: baseline;
		justify-content: flex-start;
		min-height: 40rpx;
	}

	.quality-prefix {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.quality-keyword {
		font-size: 28rpx;
		color: #64D2FF;
		line-height: 40rpx;
	}

	.quality-scale {
		position: relative;
		width: 360rpx;
		margin-top: 16rpx;
	}

	.scale-img {
		display: block;
		width: 360rpx;
	}

	.scale-pointer {
		position: absolute;
		left: 0;
		bottom: -10rpx;
		width: 22rpx;
		height: 22rpx;
		transform: translateX(-50%);
	}

	.structure-block {
		margin-top: 36rpx;
		padding-top: 28rpx;
		border-top: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.structure-title {
		display: block;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 40rpx;
	}

	.structure-link {
		display: block;
		font-size: 24rpx;
		color: #0A84FF;
	}

	.structure-row {
		display: flex;
		align-items: center;
		margin-bottom: 28rpx;
	}

	.structure-row-last {
		margin-bottom: 0;
	}

	.structure-name {
		width: 120rpx;
		flex-shrink: 0;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 36rpx;
	}

	.structure-duration {
		width: 150rpx;
		flex-shrink: 0;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.structure-track {
		flex: 1;
		min-width: 0;
		height: 12rpx;
		margin: 0 16rpx;
		border-radius: 6rpx;
		background: rgba(255, 255, 255, 0.08);
		overflow: hidden;
	}

	.structure-fill {
		height: 100%;
		border-radius: 6rpx;
	}

	.structure-percent {
		width: 72rpx;
		flex-shrink: 0;
		text-align: right;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
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
</style>