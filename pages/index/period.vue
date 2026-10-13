<template>
	<view class="page">
		<view class="navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="navbar-inner">
				<view class="nav-side" @click="goBack">
					<image class="nav-back-icon" src="/static/leftBack.png" mode="aspectFit"></image>
				</view>
				<text class="nav-title">经期</text>
				<view class="nav-side nav-side-right" @click="goSetPeriod">
					<image src="/static/moreBtn.png" style="width: 36rpx;height: 36rpx;" mode=""></image>
				</view>
			</view>
		</view>

		<view class="main" :style="{ paddingTop: navPadTop }">
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
					<view v-for="(week, wi) in calendarWeeks" :key="wi" class="cal-week-row">
						<view class="cal-run-layer">
							<view v-for="(bar, bi) in week.bars" :key="'b' + bi" class="cal-run" :class="bar.type"
								:style="{ left: bar.left, width: bar.width }"></view>
						</view>
						<view class="cal-week-days">
							<view v-for="(cell, index) in week.days" :key="index" class="cal-cell"
								:class="{
									empty: !cell.day,
									today: cell.isToday,
									selected: cell.isSelected && !cell.isToday,
									disabled: cell.isFuture
								}"
								@click="cell.day && onSelectDay(cell)">
								<view v-if="cell.isToday" class="today-badge">
									<text class="cal-day">今</text>
								</view>
								<view v-else-if="cell.isSelected" class="selected-badge">
									<text class="cal-day">{{ cell.day }}</text>
								</view>
								<text v-else-if="cell.day" class="cal-day"
									:class="{ ovulation: cell.isOvulation }">{{ cell.day }}</text>
							</view>
						</view>
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
						<view class="legend-dot fertile" style="background-color: #2987F9;"></view>
						<text class="legend-text">易孕期</text>
					</view>
					<view class="legend-item">
						<view class="legend-dot ovulation" style="background-color: #A14CE1;"></view>
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
			<view class="action-row">
				<image class="action-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="action-icon" src="/static/xinPer.png" mode="aspectFit"></image>
				<text class="action-text">{{ selectedIsToday ? '今天是经期日' : '这天是经期日' }}</text>
				<u-switch class="action-switch" :value="isPeriodToday" :space="2" :asyncChange="true" :loading="savingToday"
					activeColor="#2F7BFF" inactiveColor="#E5E5EA" :size="22" @change="onTogglePeriodToday"></u-switch>

			</view>

			<!-- 了解生理周期 -->
			<view class="action-row" @click="onLearnCycle">
				<image class="action-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="action-icon" src="/static/xinPer.png" mode="aspectFit"></image>
				<text class="action-text">了解生理周期</text>
				<image class="action-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getPeriodStatistics,
		getPeriodCalendar,
		togglePeriodToday
	} from '@/common/api/period.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function dateToYmd(d) {
		return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
	}

	function toYearMonth(year, month) {
		return year + '-' + pad(month + 1)
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

	function startOfDay(d) {
		return new Date(d.getFullYear(), d.getMonth(), d.getDate())
	}

	function isFutureDate(date, today) {
		return startOfDay(date).getTime() > startOfDay(today).getTime()
	}

	function daysBetween(a, b) {
		const ms = 24 * 60 * 60 * 1000
		const ua = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())
		const ub = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate())
		return Math.floor((ub - ua) / ms)
	}

	function flagFromType(type) {
		const t = String(type == null ? '' : type).toLowerCase()
		return {
			isPeriod: t === 'period' || t === 'menstrual' || t === '1',
			isPredict: t === 'predict' || t === 'predicted' || t === '2',
			isFertile: t === 'fertile' || t === 'ovulation' || t === '3' || t === '4',
			isOvulation: t === 'ovulationday' || t === 'ovulation_day'
		}
	}

	function flagsFromItem(item) {
		const fromType = flagFromType(item.type || item.dayType || item.status || item.mark)
		const tags = item.tags || item.marks || []
		const has = (name) => tags.indexOf(name) >= 0 || tags.indexOf(name.toUpperCase()) >= 0
		return {
			isPeriod: !!(item.isPeriod || item.period || fromType.isPeriod || has('period')),
			isPredict: !!(item.isPredict || item.predicted || fromType.isPredict || has('predict')),
			isFertile: !!(item.isFertile || item.fertile || item.ovulation || fromType.isFertile || has('fertile') || has(
				'ovulation')),
			isOvulation: !!(item.isOvulationDay || fromType.isOvulation || has('ovulationDay'))
		}
	}

	function addFlagDates(map, list, flag) {
		;
		(list || []).forEach((d) => {
			const raw = typeof d === 'string' ? d : (d && (d.date || d.day || d.dayDate))
			const parsed = parseDate(raw)
			if (!parsed) return
			const key = dateToYmd(parsed)
			if (!map[key]) {
				map[key] = {
					isPeriod: false,
					isPredict: false,
					isFertile: false,
					isOvulation: false
				}
			}
			map[key][flag] = true
		})
	}

	function visibleBg(cell) {
		if (!cell || !cell.day) return ''
		if (cell.isPeriod) return 'period'
		if (cell.isPredict) return 'predict'
		if (cell.isFertile) return 'fertile'
		if (cell.isOvulation) return 'ovulation'
		return ''
	}

	function barGroup(type) {
		if (type === 'period' || type === 'predict') return 'menstrual'
		return type
	}

	function buildWeek(row) {
		const bars = []
		let i = 0
		while (i < 7) {
			const type = visibleBg(row[i])
			if (!type) {
				i += 1
				continue
			}
			const group = barGroup(type)
			let j = i
			while (j + 1 < 7 && barGroup(visibleBg(row[j + 1])) === group) {
				j += 1
			}
			const count = j - i + 1
			bars.push({
				type: group,
				left: ((i / 7) * 100).toFixed(4) + '%',
				width: ((count / 7) * 100).toFixed(4) + '%'
			})
			i = j + 1
		}
		return {
			days: row,
			bars
		}
	}

	function buildDayFlagMap(data) {
		const map = {}
		if (!data) return map
		addFlagDates(map, data.periodDays || data.periodDates, 'isPeriod')
		addFlagDates(map, data.predictDays || data.predictedDays || data.predictDates, 'isPredict')
		addFlagDates(map, data.fertileDays || data.fertileDates || data.ovulationDays || data.ovulationDates, 'isFertile')
		const days = data.days || data.list || data.items || []
		days.forEach((item) => {
			if (!item) return
			const raw = item.date || item.dayDate || item.day
			const parsed = parseDate(raw)
			if (!parsed) return
			const key = dateToYmd(parsed)
			const prev = map[key] || {
				isPeriod: false,
				isPredict: false,
				isFertile: false,
				isOvulation: false
			}
			const next = flagsFromItem(item)
			map[key] = {
				isPeriod: prev.isPeriod || next.isPeriod,
				isPredict: prev.isPredict || next.isPredict,
				isFertile: prev.isFertile || next.isFertile,
				isOvulation: prev.isOvulation || next.isOvulation
			}
		})
		return map
	}

	export default {
		data() {
			const now = new Date()
			return {
				viewYear: now.getFullYear(),
				viewMonth: now.getMonth(),
				weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
				startDate: '',
				duration: 0,
				cycle: 0,
				apiPeriodDay: null,
				isPeriodToday: false,
				today: now,
				selectedDate: dateToYmd(now),
				dayFlagMap: {},
				savingToday: false,
				inited: true,
				statusBarHeight: 20
			}
		},
		computed: {
			navPadTop() {
				return (this.statusBarHeight + 44 + 12) + 'px'
			},
			monthTitle() {
				return `${this.viewYear}年${this.viewMonth + 1}月`
			},
			periodDay() {
				if (this.apiPeriodDay != null && this.apiPeriodDay !== '') {
					return this.apiPeriodDay
				}
				const start = parseDate(this.startDate)
				if (!start || !this.duration) return '--'
				const diff = daysBetween(start, this.today) + 1
				if (diff < 1 || diff > this.duration) return '--'
				return diff
			},
			calendarCells() {
				const year = this.viewYear
				const month = this.viewMonth
				const daysInMonth = new Date(year, month + 1, 0).getDate()
				const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7
				const start = parseDate(this.startDate)
				const hasApiFlags = Object.keys(this.dayFlagMap).length > 0
				const cells = []

				for (let i = 0; i < firstWeekday; i++) {
					cells.push({
						day: 0
					})
				}

				for (let d = 1; d <= daysInMonth; d++) {
					const date = new Date(year, month, d)
					const key = dateToYmd(date)
					const flags = hasApiFlags ?
						(this.dayFlagMap[key] || {
							isPeriod: false,
							isPredict: false,
							isFertile: false,
							isOvulation: false
						}) :
						this.getDayFlags(date, start)
					cells.push({
						day: d,
						date,
						ymd: key,
						isToday: sameDay(date, this.today),
						isFuture: isFutureDate(date, this.today),
						isSelected: this.selectedDate === key,
						...flags
					})
				}

				while (cells.length % 7 !== 0) {
					cells.push({
						day: 0
					})
				}
				return cells
			},
			calendarWeeks() {
				const cells = this.calendarCells
				const weeks = []
				for (let i = 0; i < cells.length; i += 7) {
					weeks.push(buildWeek(cells.slice(i, i + 7)))
				}
				return weeks
			},
			selectedIsToday() {
				return this.selectedDate === dateToYmd(this.today)
			}
		},
		onLoad() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
		},
		onShow() {
			this.today = new Date()
			if (!this.selectedDate || isFutureDate(parseDate(this.selectedDate) || this.today, this.today)) {
				this.selectedDate = dateToYmd(this.today)
			}
			this.loadPage()
		},
		methods: {
			async loadPage() {
				await this.loadStatistics()
				if (!this.inited) {
					uni.redirectTo({
						url: '/pages/index/setPeriod'
					})
					return
				}
				await this.loadCalendar()
			},
			async loadStatistics() {
				try {
					const stats = await getPeriodStatistics()
					this.applyStatistics(stats)
				} catch (e) {
					console.log('[period] statistics failed', e && (e.msg || e.message))
				}
			},
			applyStatistics(stats) {
				if (!stats) {
					this.inited = false
					return
				}
				const start = stats.lastStartDate || stats.startDate || ''
				const duration = Number(stats.duration || stats.periodDuration || 0)
				const cycle = Number(stats.cycleLength || stats.cycle || 0)
				this.startDate = start
				if (duration > 0) this.duration = duration
				if (cycle > 0) this.cycle = cycle
				this.apiPeriodDay = stats.periodDay != null && stats.periodDay !== '' ? stats.periodDay : null
				if (this.selectedDate === dateToYmd(this.today) && stats.isPeriodToday != null) {
					this.isPeriodToday = !!stats.isPeriodToday
				}
				this.inited = !!(start || duration > 0 || cycle > 0)
			},
			async loadCalendar() {
				try {
					const data = await getPeriodCalendar({
						yearMonth: toYearMonth(this.viewYear, this.viewMonth)
					})
					this.dayFlagMap = buildDayFlagMap(data || {})
					if (data) {
						if (data.periodDay != null) this.apiPeriodDay = data.periodDay
						if (Number(data.duration) > 0) this.duration = Number(data.duration)
						if (Number(data.cycleLength || data.cycle) > 0) this.cycle = Number(data.cycleLength || data
							.cycle)
					}
					this.syncSelectedPeriodFlag()
				} catch (e) {
					this.dayFlagMap = {}
					console.log('[period] calendar failed', e && (e.msg || e.message))
				}
			},
			syncSelectedPeriodFlag() {
				const flags = this.dayFlagMap[this.selectedDate]
				if (flags) {
					this.isPeriodToday = !!flags.isPeriod
					return
				}
				const date = parseDate(this.selectedDate)
				const start = parseDate(this.startDate)
				if (date && start) {
					this.isPeriodToday = !!this.getDayFlags(date, start).isPeriod
				}
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

				if (offset < duration) {
					if (cycleIndex === 0) flags.isPeriod = true
					else flags.isPredict = true
				}

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
				this.loadCalendar()
			},
			onNextMonth() {
				if (this.viewMonth === 11) {
					this.viewYear += 1
					this.viewMonth = 0
				} else {
					this.viewMonth += 1
				}
				this.loadCalendar()
			},
			onSelectDay(cell) {
				if (!cell || !cell.day || !cell.date) return
				if (cell.isFuture) {
					uni.showToast({
						title: '不能选择今天之后的日期',
						icon: 'none'
					})
					return
				}
				this.selectedDate = cell.ymd || dateToYmd(cell.date)
				this.isPeriodToday = !!cell.isPeriod
			},
			async onTogglePeriodToday(next) {
				if (this.savingToday) return
				const prev = this.isPeriodToday
				this.isPeriodToday = !!next
				this.savingToday = true
				try {
					await togglePeriodToday({
						isPeriod: !!next,
						date: this.selectedDate || dateToYmd(this.today)
					})
					await this.loadPage()
				} catch (e2) {
					this.isPeriodToday = prev
					console.log('[period] toggle today failed', e2 && (e2.msg || e2.message))
				} finally {
					this.savingToday = false
				}
			},
			onLearnCycle() {
				uni.navigateTo({
					url: '/pages/index/lookPeriod'
				})
			},
			goBack() {
				uni.navigateBack({
					fail: () => {
						uni.reLaunch({
							url: '/pages/index/index'
						})
					}
				})
			},
			goSetPeriod() {
				uni.navigateTo({
					url: '/pages/index/setPeriod'
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		padding: 0 28rpx 60rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.navbar {
		position: fixed;
		left: 0;
		top: 0;
		width: 100%;
		z-index: 100;
		background: #000000;
	}

	.navbar-inner {
		height: 88rpx;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.nav-side {
		width: 120rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.nav-side-right {
		justify-content: flex-end;
	}

	.nav-back-icon {
		width: 40rpx;
		height: 40rpx;
	}

	.nav-title {
		flex: 1;
		text-align: center;
		font-size: 34rpx;
		font-weight: 600;
		line-height: 48rpx;
		color: #ffffff;
	}

	.nav-set-text {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.main {
		position: relative;
		z-index: 1;
		box-sizing: border-box;
	}

	.card {
		background: #262732;
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
		flex-direction: column;
	}

	.cal-week-row {
		position: relative;
		height: 84rpx;
		margin-bottom: 8rpx;
	}

	.cal-run-layer {
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
	}

	.cal-run {
		position: absolute;
		top: 0;
		bottom: 0;
		border-radius: 16rpx;
	}

	.cal-run.period,
	.cal-run.predict,
	.cal-run.menstrual {
		background: rgba(240, 68, 136, 0.18);
	}

	.cal-run.fertile {
		background: rgba(38, 49, 70, 1);
	}

	.cal-run.ovulation {
		background: rgba(192, 132, 252, 0.28);
	}

	.cal-week-days {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: stretch;
		height: 84rpx;
	}

	.cal-cell {
		position: relative;
		z-index: 1;
		flex: 1;
		height: 84rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.cal-cell.empty {
		opacity: 0;
	}

	.cal-day {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.cal-day.ovulation {
		color: #c084fc;
		font-weight: 700;
	}

	.cal-cell.disabled {
		opacity: 0.35;
	}

	.today-badge,
	.selected-badge {
		width: 64rpx;
		height:84rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		
	}

	.today-badge {
		background: #F04488;
	}

	.selected-badge {
		background: #F04488;
		box-sizing: border-box;
	}

	.today-badge .cal-day,
	.selected-badge .cal-day {
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
		background: #263146;
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
		position: relative;
		overflow: hidden;
		display: flex;
		align-items: center;
		margin-top: 24rpx;
		padding: 28rpx 28rpx;
		height: 144rpx;
		border-radius: 24rpx;
		box-sizing: border-box;
	}

	.action-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		pointer-events: none;
		z-index: 0;
	}

	.action-icon {
		position: relative;
		z-index: 1;
		width: 44rpx;
		height: 44rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.action-text {
		position: relative;
		z-index: 1;
		flex: 1;
		font-size: 30rpx;
		color: #ffffff;
		line-height: 42rpx;
	}

	.action-switch {
		position: relative;
		z-index: 1;
		flex-shrink: 0;
	}
	
	::v-deep .u-switch__node{
		background: #000!important;
	}

	.action-arrow {
		position: relative;
		z-index: 1;
		width: 24rpx;
		height: 24rpx;
		opacity: 0.55;
		flex-shrink: 0;
	}
</style>