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
				<view class="date-btn" @click="onPrevDay">
					<text class="date-arrow">‹</text>
				</view>
				<text class="date-text">{{ dateText }}</text>
				<view class="date-btn" @click="onNextDay">
					<text class="date-arrow">›</text>
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
					<view class="score-block">
						<text class="score-label">睡眠得分</text>
						<view class="score-row">
							<text class="score-num">{{ sleepScore }}</text>
							<text class="score-unit">分</text>
						</view>
					</view>
					<view class="quality-block">
						<text class="quality-text">{{ qualityText }}</text>
						<view class="quality-bar">
							<view class="quality-seg seg-orange"></view>
							<view class="quality-seg seg-yellow"></view>
							<view class="quality-seg seg-green"></view>
							<view class="quality-seg seg-cyan"></view>
							<view class="quality-marker" :style="{ left: qualityMarkerLeft }"></view>
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
	import { toSvgDataUrl } from '@/common/svg-data-url.js'

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

	// 按设计图从左到右：浅睡 → 深睡凹槽×2 → 浅睡 → REM 平台 → 浅睡 → 清醒尖峰 → …
	const CYCLE_SEGMENTS = [{
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
		// 四个睡眠状态对应四个区间；这里返回每个区间的上边界。
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
		const total = segments.reduce((sum, item) => sum + item.dur, 0)
		const stageOrder = ['awake', 'rem', 'light', 'deep']
		const levels = stageOrder.map((stage) => stageY(stage, chartH, padTop, padBottom))
		const bottomY = chartH - padBottom
		const areas = []
		const connectors = []
		let x = 0
		let previousLevel = -1
		segments.forEach((seg) => {
			const nextX = x + (seg.dur / total) * chartW
			const level = stageOrder.indexOf(seg.stage)
			const top = levels[level]
			const bottom = level === levels.length - 1 ? bottomY : levels[level + 1]
			const fill = level <= 1 ? '#8B7CFF' : '#5B32D6'

			if (previousLevel >= 0 && previousLevel !== level) {
				const previousY = levels[previousLevel]
				connectors.push({
					x,
					y1: previousY,
					y2: top,
					stroke: fill
				})
			}
			areas.push(buildBandArea(x, nextX, top, bottom, fill, 8))
			previousLevel = level
			x = nextX
		})
		return {
			areas,
			connectors
		}
	}

	export default {
		data() {
			return {
				sleepHours: 7,
				sleepMinutes: 32,
				recoverStatus: '恢复良好',
				currentDate: new Date(2026, 7, 28),
				sleepStart: '00:00',
				wakeTime: '06:49',
				sleepScore: 82,
				qualityText: '睡眠质量一般',
				qualityMarkerPercent: 28,
				stageLabels: ['清醒', '快速眼动', '浅睡', '深睡'],
				chartW: 0,
				chartH: 0,
				padTop: 6,
				padBottom: 6,
				structureList: [{
						key: 'deep',
						name: '深睡',
						duration: '1小时48分',
						percent: 24,
						color: '#7B61FF'
					},
					{
						key: 'light',
						name: '浅睡',
						duration: '1小时48分',
						percent: 24,
						color: '#A78BFA'
					},
					{
						key: 'rem',
						name: '快速眼动',
						duration: '1小时48分',
						percent: 24,
						color: '#64D2FF'
					},
					{
						key: 'awake',
						name: '清醒',
						duration: '1小时48分',
						percent: 24,
						color: '#FF9F0A'
					}
				],
				recentList: [{
						time: '今天 09:41',
						source: '戒指采集',
						duration: '7 小时 32 分'
					},
					{
						time: '昨天 21:16',
						source: '戒指采集',
						duration: '7 小时 32 分'
					},
					{
						time: '8月6日 08:32',
						source: '戒指采集',
						duration: '7 小时 32 分'
					}
				]
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
				return this.qualityMarkerPercent + '%'
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
					CYCLE_SEGMENTS,
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
		display: flex;
		align-items: center;
		justify-content: center;
		width: 400rpx;
		margin: 0 auto 28rpx;
		padding: 14rpx 20rpx;
		border-radius: 40rpx;
		background: #1A2740;
		box-sizing: border-box;
	}

	.date-btn {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.date-arrow {
		font-size: 40rpx;
		color: #ffffff;
		line-height: 48rpx;
	}

	.date-text {
		flex: 1;
		text-align: center;
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
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
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
	}

	.score-block {
		flex-shrink: 0;
	}

	.score-label {
		display: block;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.score-row {
		display: flex;
		align-items: baseline;
		margin-top: 8rpx;
	}

	.score-num {
		font-size: 64rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 76rpx;
	}

	.score-unit {
		margin-left: 8rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.85);
		line-height: 36rpx;
	}

	.quality-block {
		flex: 1;
		min-width: 0;
		margin-left: 32rpx;
		padding-bottom: 10rpx;
	}

	.quality-text {
		display: block;
		text-align: right;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.quality-bar {
		position: relative;
		display: flex;
		height: 12rpx;
		margin-top: 20rpx;
		border-radius: 6rpx;
		overflow: visible;
	}

	.quality-seg {
		flex: 1;
		height: 100%;
	}

	.seg-orange {
		background: #FF9F0A;
		border-radius: 6rpx 0 0 6rpx;
	}

	.seg-yellow {
		background: #FFD60A;
	}

	.seg-green {
		background: #30D158;
	}

	.seg-cyan {
		background: #64D2FF;
		border-radius: 0 6rpx 6rpx 0;
	}

	.quality-marker {
		position: absolute;
		top: -14rpx;
		width: 0;
		height: 0;
		margin-left: -8rpx;
		border-left: 8rpx solid transparent;
		border-right: 8rpx solid transparent;
		border-top: 12rpx solid #64D2FF;
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