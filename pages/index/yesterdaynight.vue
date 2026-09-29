<template>
	<view class="page">
		<!-- 最近一次摘要 -->
		<view class="hero-card">
			<!-- 整卡装饰背景图 -->
			<image class="hero-bg" src="/static/yearertodayBg.png" mode="aspectFill"></image>
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
					<text v-for="(label, idx) in stageLabels" :key="idx" class="cycle-y-label">{{ label }}</text>
				</view>
				<view class="cycle-main">
					<!-- 圆角阶梯面积图 hypnogram -->
					<view class="cycle-plot">
						<image v-if="chartImageSrc" class="cycle-svg" :src="chartImageSrc" mode="scaleToFill" />
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
					<text class="structure-title">睡眠结构</text>
					<text class="structure-link">各睡眠区间</text>
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
	</view>
</template>

<script>
	import {
		toSvgDataUrl
	} from '@/common/svg-data-url.js'
	import BleService from '@/common/ble-service.js'
	import CoolWearBLE from '@/common/coolwear-ble.js'

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
		const list = segments && segments.length ? segments : DEFAULT_SEGMENTS
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
				color: '#64D2FF'
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
				cycleSegments: DEFAULT_SEGMENTS.slice(),
				structureList: emptyStructure(),
				recentList: [],
				sleepSessions: [],
				unsubBle: null,
				bleConnected: false
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
			qualityMarkerLeft() {
				const p = Number(this.qualityMarkerPercent)
				const safe = Number.isNaN(p) ? 0 : Math.max(0, Math.min(100, p))
				return `${safe}%`
			},
			qualityKeyword() {
				const t = this.qualityText || ''
				if (t.indexOf('很好') >= 0) return '很好'
				if (t.indexOf('较好') >= 0) return '较好'
				if (t.indexOf('偏差') >= 0 || t.indexOf('较差') >= 0) return '较差'
				if (t.indexOf('一般') >= 0) return '一般'
				if (t.indexOf('暂无') >= 0) return ''
				return ''
			},
			gridLineYs() {
				if (!this.chartH) return []
				const usable = this.chartH - this.padTop - this.padBottom
				return [0, 1, 2, 3, 4].map((level) => this.padTop + (usable / 4) * level)
			},
			hypnogramAreas() {
				return this.hypnogramData.areas
			},
			hypnogramConnectors() {
				return this.hypnogramData.connectors
			},
			hypnogramData() {
				if (!this.chartW || !this.chartH) return {
					areas: [],
					connectors: []
				}
				return buildHypnogramAreas(
					this.cycleSegments,
					this.chartW,
					this.chartH,
					this.padTop,
					this.padBottom
				)
			},
			chartImageSrc() {
				if (!this.chartW || !this.chartH) return ''
				const grids = (this.gridLineYs || []).map((y) =>
					`<line x1="0" y1="${y}" x2="${this.chartW}" y2="${y}" stroke="rgba(255,255,255,0.16)" stroke-width="1" stroke-dasharray="3 4"/>`
				).join('')
				const connectors = (this.hypnogramConnectors || []).map((c) =>
					`<line x1="${c.x}" y1="${c.y1}" x2="${c.x}" y2="${c.y2}" stroke="${c.stroke}" stroke-width="6" stroke-linecap="round"/>`
				).join('')
				const areas = (this.hypnogramAreas || []).map((a) =>
					`<path d="${a.d}" fill="${a.fill}"/>`
				).join('')
				return toSvgDataUrl(grids + connectors + areas, this.chartW, this.chartH, 'preserveAspectRatio="none"')
			}
		},
		created() {
			this.unsubBle = BleService.subscribe((s) => {
				this.bleConnected = !!s.connected
				if (s.sleep) this.onSleepUpdate(s.sleep)
			})
			const cached = BleService.getSleep && BleService.getSleep()
			if (cached) this.onSleepUpdate(cached)
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
		onUnload() {
			if (this.unsubBle) {
				this.unsubBle()
				this.unsubBle = null
			}
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
					})
					.exec()
			},
			async refreshSleep() {
				const ble = BleService.getState()
				if (!ble.connected) {
					const cached = BleService.getSleep && BleService.getSleep()
					if (cached) this.onSleepUpdate(cached)
					return
				}
				try {
					await BleService.syncSleep()
				} catch (e) {
					console.log('[yesterdaynight] syncSleep failed', e)
					try {
						await CoolWearBLE.sendSyncInfo()
					} catch (err) {
						console.log('[yesterdaynight] sendSyncInfo failed', err)
					}
				}
			},
			onSleepUpdate(sleep) {
				if (!sleep) return
				this.sleepSessions = Array.isArray(sleep.sessions) && sleep.sessions.length ?
					sleep.sessions :
					[sleep]
				this.applySleepForDate(this.currentDate)
			},
			findSleepForDate(date) {
				const list = this.sleepSessions || []
				if (!list.length) return null
				const y = date.getFullYear()
				const m = date.getMonth()
				const d = date.getDate()
				const matched = list.find((item) => {
					const sec = Number(item.sleepDay || item.endTime || item.startTime) || 0
					if (!sec) return false
					const dt = new Date(sec * 1000)
					return dt.getFullYear() === y && dt.getMonth() === m && dt.getDate() === d
				})
				if (matched) return matched
				// 无精确匹配时：选中“今天/昨天”则用最近一晚
				const today = new Date()
				const isToday =
					y === today.getFullYear() && m === today.getMonth() && d === today.getDate()
				const yest = new Date(today)
				yest.setDate(today.getDate() - 1)
				const isYest =
					y === yest.getFullYear() && m === yest.getMonth() && d === yest.getDate()
				if (isToday || isYest) return list[list.length - 1]
				return null
			},
			applySleepForDate(date) {
				const sleep = this.findSleepForDate(date)
				if (!sleep || !sleep.totalMin) {
					this.sleepHours = 0
					this.sleepMinutes = 0
					this.recoverStatus = '暂无数据'
					this.sleepStart = '--:--'
					this.wakeTime = '--:--'
					this.sleepScore = 0
					this.qualityText = '暂无睡眠数据'
					this.qualityMarkerPercent = 8
					this.structureList = emptyStructure()
					this.cycleSegments = DEFAULT_SEGMENTS.slice()
					this.rebuildRecent()
					return
				}

				this.sleepHours = sleep.hours || Math.floor((sleep.totalMin || 0) / 60)
				this.sleepMinutes = sleep.minutes != null ? sleep.minutes : ((sleep.totalMin || 0) % 60)
				this.recoverStatus = sleep.recoverStatus || '恢复一般'
				this.sleepStart = sleep.sleepStart || '--:--'
				this.wakeTime = sleep.wakeTime || '--:--'
				this.sleepScore = sleep.score || 0
				this.qualityText = sleep.qualityText || '睡眠质量一般'
				this.qualityMarkerPercent = sleep.qualityMarkerPercent != null ? sleep.qualityMarkerPercent : 50

				const total = Math.max(1, sleep.totalMin || 0)
				const deep = sleep.deepMin || 0
				const light = sleep.lightMin || 0
				const rem = sleep.remMin || 0
				const awake = sleep.awakeMin || 0
				this.structureList = [{
						key: 'deep',
						name: '深睡',
						duration: formatDuration(deep),
						percent: Math.round(deep / total * 100),
						color: '#7B61FF'
					},
					{
						key: 'light',
						name: '浅睡',
						duration: formatDuration(light),
						percent: Math.round(light / total * 100),
						color: '#A78BFA'
					},
					{
						key: 'rem',
						name: '快速眼动',
						duration: formatDuration(rem),
						percent: Math.round(rem / total * 100),
						color: '#64D2FF'
					},
					{
						key: 'awake',
						name: '清醒',
						duration: formatDuration(awake),
						percent: Math.round(awake / total * 100),
						color: '#FF9F0A'
					}
				]

				this.cycleSegments = (sleep.segments && sleep.segments.length) ?
					sleep.segments.slice() :
					DEFAULT_SEGMENTS.slice()
				this.rebuildRecent()
				this.$nextTick(() => this.measureChart(0))
			},
			rebuildRecent() {
				const list = (this.sleepSessions || []).slice().reverse().slice(0, 8)
				this.recentList = list.map((item) => ({
					time: formatRecentTime(item.endTime || item.startTime || item.sleepDay),
					source: '戒指采集',
					duration: formatDuration(item.totalMin).replace('小时', ' 小时 ').replace('分', ' 分')
				}))
			},
			shiftDate(delta) {
				const next = new Date(this.currentDate)
				next.setDate(next.getDate() + delta)
				this.currentDate = next
				this.applySleepForDate(next)
			},
			onPrevDay() {
				this.shiftDate(-1)
			},
			onNextDay() {
				this.shiftDate(1)
			},
			openCalendar() {
				// 日历弹层后续接入，先保持与心率页交互结构一致
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
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		padding-top: 4rpx;
		padding-bottom: 4rpx;
		box-sizing: border-box;
	}

	.cycle-y-label {
		font-size: 20rpx;
		color: #8e8e93;
		line-height: 28rpx;
	}

	.cycle-main {
		flex: 1;
		min-width: 0;
	}

	.cycle-plot {
		width: 100%;
		height: 280rpx;
	}

	.cycle-svg {
		width: 100%;
		height: 100%;
		display: block;
		overflow: visible;
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
		margin-top: 8rpx;
		margin-bottom: 24rpx;
		font-size: 24rpx;
		color: #0A84FF;
		line-height: 34rpx;
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