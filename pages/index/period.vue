<template>
	<view class="page">
		<!-- 日历 -->
		<view class="card cal-card">
			<view class="cal-nav">
				<view class="cal-nav-btn" @click="onPrevMonth">
					<image class="cal-nav-icon" src="/static/arrLeft.png" mode="aspectFit"></image>
				</view>
				<text class="cal-title">{{ monthTitle }}</text>
				<view class="cal-nav-btn" @click="onNextMonth">
					<image class="cal-nav-icon" src="/static/arrRight.png" mode="aspectFit"></image>
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
					:class="{
						empty: !cell.day,
						today: cell.isToday,
						period: cell.isPeriod,
						predict: cell.isPredict,
						fertile: cell.isFertile,
						ovulation: cell.isOvulation
					}"
					@click="cell.day && onSelectDay(cell)"
				>
					<text v-if="cell.day" class="cal-day">{{ cell.isToday ? '今' : cell.day }}</text>
				</view>
			</view>

			<view class="legend">
				<view class="legend-item">
					<view class="legend-dot period"></view>
					<text class="legend-text">经期</text>
				</view>
				<view class="legend-item">
					<view class="legend-dot predict"></view>
					<text class="legend-text">预测经期</text>
				</view>
				<view class="legend-item">
					<view class="legend-dot fertile"></view>
					<text class="legend-text">易孕期</text>
				</view>
				<view class="legend-item">
					<view class="legend-dot ovulation"></view>
					<text class="legend-text">排卵日</text>
				</view>
			</view>
		</view>

		<!-- 统计 -->
		<view class="card stats-card">
			<view class="stats-col">
				<text class="stats-label">经期</text>
				<view class="stats-value-row">
					<text class="stats-prefix">第</text>
					<text class="stats-num">{{ periodDay }}</text>
					<text class="stats-suffix">天</text>
				</view>
			</view>
			<view class="stats-divider"></view>
			<view class="stats-col">
				<text class="stats-label">周期统计</text>
				<view class="stats-value-row">
					<text class="stats-prefix">持续</text>
					<text class="stats-num">{{ duration }}</text>
					<text class="stats-suffix">天</text>
					<text class="stats-cycle">周期{{ cycle }}天</text>
				</view>
			</view>
		</view>

		<!-- 今天是经期日 -->
		<view class="card action-row">
			<!-- 粉色爱心图标，自行补充 src -->
			<image class="action-icon" src="/static/jingqiStart.png" mode="aspectFit"></image>
			<text class="action-text">今天是经期日</text>
			<switch
				class="action-switch"
				:checked="isPeriodToday"
				color="#2F7BFF"
				@change="onTogglePeriodToday"
			/>
		</view>

		<!-- 了解生理周期 -->
		<view class="card action-row link" @click="onLearnCycle">
			<!-- 粉色爱心图标，自行补充 src -->
			<image class="action-icon" src="/static/jingqiStart.png" mode="aspectFit"></image>
			<text class="action-text">了解生理周期</text>
			<image class="action-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
		</view>
	</view>
</template>

<script>
	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function parseDate(str) {
		if (!str) return null
		const parts = String(str).split(/[-/]/)
		if (parts.length < 3) return null
		return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
	}

	function sameDay(a, b) {
		return (
			a.getFullYear() === b.getFullYear() &&
			a.getMonth() === b.getMonth() &&
			a.getDate() === b.getDate()
		)
	}

	function daysBetween(a, b) {
		const ms = 24 * 60 * 60 * 1000
		const ua = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())
		const ub = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate())
		return Math.floor((ub - ua) / ms)
	}

	export default {
		data() {
			return {
				viewYear: 2026,
				viewMonth: 1,
				weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
				startDate: '2026-02-06',
				duration: 5,
				cycle: 28,
				isPeriodToday: true,
				today: new Date(2026, 1, 8)
			}
		},
		computed: {
			monthTitle() {
				return `${this.viewYear}年${this.viewMonth + 1}月`
			},
			periodDay() {
				const start = parseDate(this.startDate)
				if (!start) return 1
				const diff = daysBetween(start, this.today) + 1
				if (diff < 1 || diff > this.duration) return 1
				return diff
			},
			calendarCells() {
				const year = this.viewYear
				const month = this.viewMonth
				const daysInMonth = new Date(year, month + 1, 0).getDate()
				const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7
				const start = parseDate(this.startDate)
				const cells = []

				for (let i = 0; i < firstWeekday; i++) {
					cells.push({ day: 0 })
				}

				for (let d = 1; d <= daysInMonth; d++) {
					const date = new Date(year, month, d)
					const flags = this.getDayFlags(date, start)
					cells.push({
						day: d,
						date,
						isToday: sameDay(date, this.today),
						...flags
					})
				}

				while (cells.length % 7 !== 0) {
					cells.push({ day: 0 })
				}
				return cells
			}
		},
		onShow() {
			this.loadSetting()
		},
		methods: {
			loadSetting() {
				try {
					const setting = uni.getStorageSync('periodSetting')
					if (!setting) return
					if (setting.startDate) this.startDate = setting.startDate
					if (setting.duration) this.duration = setting.duration
					if (setting.cycle) this.cycle = setting.cycle
					const start = parseDate(this.startDate)
					if (start) {
						this.viewYear = start.getFullYear()
						this.viewMonth = start.getMonth()
					}
				} catch (e) {}
			},
			getDayFlags(date, start) {
				const flags = {
					isPeriod: false,
					isPredict: false,
					isFertile: false,
					isOvulation: false
				}
				if (!start) return flags

				const diff = daysBetween(start, date)
				if (diff < 0) return flags

				const cycleLen = this.cycle || 28
				const duration = this.duration || 5
				const offset = diff % cycleLen
				const cycleIndex = Math.floor(diff / cycleLen)

				// 当前周期：实际经期；后续周期：预测经期
				if (offset < duration) {
					if (cycleIndex === 0) flags.isPeriod = true
					else flags.isPredict = true
				}

				// 排卵日约在下次月经前 14 天；易孕期为其前后共约 7 天
				const ovulationOffset = cycleLen - 14
				const fertileStart = ovulationOffset - 5
				const fertileEnd = ovulationOffset + 1
				if (offset === ovulationOffset) {
					flags.isOvulation = true
					flags.isFertile = true
				} else if (offset >= fertileStart && offset <= fertileEnd) {
					flags.isFertile = true
				}

				return flags
			},
			onPrevMonth() {
				if (this.viewMonth === 0) {
					this.viewYear -= 1
					this.viewMonth = 11
				} else {
					this.viewMonth -= 1
				}
			},
			onNextMonth() {
				if (this.viewMonth === 11) {
					this.viewYear += 1
					this.viewMonth = 0
				} else {
					this.viewMonth += 1
				}
			},
			onSelectDay(cell) {
				if (!cell || !cell.day) return
			},
			onTogglePeriodToday(e) {
				this.isPeriodToday = !!(e.detail && e.detail.value)
			},
			onLearnCycle() {
				uni.navigateTo({
					url: '/pages/index/lookPeriod'
				})
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

	.card {
		background: #1A1B25;
		border-radius: 24rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.cal-card {
		padding: 28rpx 20rpx 32rpx;
	}

	.cal-nav {
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 24rpx;
	}

	.cal-nav-btn {
		width: 56rpx;
		height: 56rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cal-nav-icon {
		width: 28rpx;
		height: 28rpx;
		opacity: 0.55;
	}

	.cal-title {
		min-width: 220rpx;
		text-align: center;
		font-size: 30rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 42rpx;
	}

	.cal-week {
		display: flex;
		align-items: center;
		margin-bottom: 8rpx;
	}

	.cal-week-item {
		flex: 1;
		text-align: center;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.cal-grid {
		display: flex;
		flex-wrap: wrap;
	}

	.cal-cell {
		width: 14.28%;
		height: 80rpx;
		margin-bottom: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		border-radius: 16rpx;
	}

	.cal-cell.empty {
		opacity: 0;
	}

	.cal-day {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.cal-cell.period {
		background: rgba(255, 77, 141, 0.28);
	}

	.cal-cell.predict {
		background: rgba(255, 77, 141, 0.14);
	}

	.cal-cell.fertile {
		background: rgba(47, 123, 255, 0.28);
	}

	.cal-cell.ovulation .cal-day {
		color: #c084fc;
		font-weight: 700;
	}

	.cal-cell.today {
		background: #ff4d8d;
		border-radius: 16rpx;
	}

	.cal-cell.today .cal-day {
		color: #ffffff;
		font-weight: 700;
	}

	.legend {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		padding: 0 8rpx;
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
		box-sizing: border-box;
	}

	.legend-dot.period {
		background: #ff4d8d;
	}

	.legend-dot.predict {
		background: transparent;
		border: 2rpx solid #ff4d8d;
	}

	.legend-dot.fertile {
		background: #2f7bff;
	}

	.legend-dot.ovulation {
		background: #c084fc;
	}

	.legend-text {
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.stats-card {
		display: flex;
		align-items: stretch;
		margin-top: 24rpx;
		padding: 32rpx 0;
	}

	.stats-col {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 0 16rpx;
		box-sizing: border-box;
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
		flex-wrap: wrap;
		justify-content: center;
	}

	.stats-prefix,
	.stats-suffix {
		font-size: 26rpx;
		color: #ffffff;
		line-height: 36rpx;
	}

	.stats-num {
		margin: 0 4rpx;
		font-size: 40rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 52rpx;
	}

	.stats-cycle {
		margin-left: 12rpx;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 36rpx;
	}

	.action-row {
		display: flex;
		align-items: center;
		margin-top: 24rpx;
		padding: 28rpx 28rpx;
	}

	.action-icon {
		width: 44rpx;
		height: 44rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.action-text {
		flex: 1;
		font-size: 30rpx;
		color: #ffffff;
		line-height: 42rpx;
	}

	.action-switch {
		transform: scale(0.9);
		transform-origin: right center;
	}

	.action-arrow {
		width: 24rpx;
		height: 24rpx;
		opacity: 0.55;
		flex-shrink: 0;
	}
</style>
