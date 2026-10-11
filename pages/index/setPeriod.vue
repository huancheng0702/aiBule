<template>
	<view class="page">
		<view class="header">
			<text class="title">设置经期</text>
			<text class="desc">请填写你的经期数据，用于智能预测经期。每月持续记录可以提高预测准确性。</text>
		</view>

		<view class="form-card">
			<!-- 最近经期开始日 -->
			<picker mode="date" :value="startDate" :end="todayStr" @change="onStartDateChange">
				<view class="form-row border">
					<!-- 粉色爱心图标，自行补充 src -->
					<image class="row-icon" src="/static/xinlv.png" mode=""></image>
					<view class="row-main">
						<text class="row-label">最近经期开始日</text>
						<text class="row-hint">选择最近一次开始日期</text>
					</view>
					<view class="row-right">
						<text class="row-value">{{ startDateText }}</text>
						<image class="row-calendar" src="/static/dateIcon.png" mode="aspectFit"></image>
					</view>
				</view>
			</picker>

			<!-- 经期持续 -->
			<picker mode="selector" :range="durationOptions" :value="durationIndex" @change="onDurationChange">
				<view class="form-row border">
					<!-- 紫色水滴图标，自行补充 src -->
					<image class="row-icon" src="/static/peiShuidi.png" mode=""></image>
					<view class="row-main">
						<text class="row-label">经期持续</text>
						<text class="row-hint">通常为3-7天</text>
					</view>
					<view class="row-right">
						<text class="row-value">{{ duration }} 天</text>
						<image class="row-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
					</view>
				</view>
			</picker>

			<!-- 平均周期 -->
			<picker mode="selector" :range="cycleOptions" :value="cycleIndex" @change="onCycleChange">
				<view class="form-row">
					<!-- 青色日历图标，自行补充 src -->
					<image class="row-icon" src="/static/peiDate.png" mode=""></image>
					<view class="row-main">
						<text class="row-label">平均周期</text>
						<text class="row-hint">通常为21-35天</text>
					</view>
					<view class="row-right">
						<text class="row-value">{{ cycle }} 天</text>
						<image class="row-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
					</view>
				</view>
			</picker>
		</view>

		<view class="footer">
			<view class="start-btn" @click="onStart">
				<!-- 按钮背景图，自行补充 src -->
				<image class="start-btn-bg" src="/static/btnBan.png" mode=""></image>
				<text class="start-btn-text">开始使用</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getPeriodStatistics, initPeriod } from '@/common/api/period.js'

	function pad(n) {
		return String(n).padStart(2, '0')
	}

	function toPickerValue(d) {
		return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
	}

	export default {
		data() {
			const now = new Date()
			const durationOptions = []
			for (let i = 1; i <= 15; i++) durationOptions.push(`${i} 天`)
			const cycleOptions = []
			for (let i = 15; i <= 45; i++) cycleOptions.push(`${i} 天`)
			return {
				startDate: toPickerValue(now),
				duration: 5,
				cycle: 28,
				durationOptions,
				cycleOptions,
				todayStr: toPickerValue(now),
				saving: false
			}
		},
		computed: {
			startDateText() {
				const parts = this.startDate.split('-')
				if (parts.length !== 3) return this.startDate
				return `${parts[0]}/${parts[1]}/${parts[2]}`
			},
			durationIndex() {
				const idx = this.durationOptions.findIndex((item) => parseInt(item, 10) === this.duration)
				return idx < 0 ? 4 : idx
			},
			cycleIndex() {
				const idx = this.cycleOptions.findIndex((item) => parseInt(item, 10) === this.cycle)
				return idx < 0 ? 13 : idx
			}
		},
		onShow() {
			this.todayStr = toPickerValue(new Date())
			this.loadStatistics()
		},
		methods: {
			async loadStatistics() {
				try {
					const stats = await getPeriodStatistics()
					if (!stats) return
					const start = stats.lastStartDate || stats.startDate
					if (start) this.startDate = String(start).slice(0, 10)
					if (Number(stats.duration) > 0) this.duration = Number(stats.duration)
					if (Number(stats.cycleLength || stats.cycle) > 0) {
						this.cycle = Number(stats.cycleLength || stats.cycle)
					}
				} catch (e) {
					console.log('[setPeriod] statistics failed', e && (e.msg || e.message))
				}
			},
			onStartDateChange(e) {
				this.startDate = e.detail.value
			},
			onDurationChange(e) {
				const idx = Number(e.detail.value)
				this.duration = parseInt(this.durationOptions[idx], 10)
			},
			onCycleChange(e) {
				const idx = Number(e.detail.value)
				this.cycle = parseInt(this.cycleOptions[idx], 10)
			},
			async onStart() {
				if (this.saving) return
				if (!this.startDate) {
					uni.showToast({ title: '请选择最近经期开始日', icon: 'none' })
					return
				}
				this.saving = true
				try {
					await initPeriod({
						lastStartDate: this.startDate,
						duration: this.duration,
						cycleLength: this.cycle
					})
					const pages = getCurrentPages()
					const prev = pages.length > 1 ? pages[pages.length - 2] : null
					if (prev && prev.route === 'pages/index/period') {
						uni.navigateBack()
					} else {
						uni.redirectTo({
							url: '/pages/index/period'
						})
					}
				} catch (e) {
					console.log('[setPeriod] init failed', e && (e.msg || e.message))
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
		padding: 24rpx 32rpx 160rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.header {
		padding: 12rpx 4rpx 0;
	}

	.title {
		display: block;
		font-size: 48rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 68rpx;
	}

	.desc {
		display: block;
		margin-top: 16rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.55);
		line-height: 40rpx;
	}

	.form-card {
		margin-top: 40rpx;
		padding: 8rpx 28rpx;
		background: #1A1B25;
		border-radius: 24rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.form-row {
		display: flex;
		align-items: center;
		padding: 32rpx 0;
	}

	.form-row.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.row-icon {
		width: 56rpx;
		height: 56rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
	}

	.row-main {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.row-label {
		font-size: 30rpx;
		font-weight: 500;
		color: #ffffff;
		line-height: 42rpx;
	}

	.row-hint {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.row-right {
		display: flex;
		align-items: center;
		flex-shrink: 0;
		margin-left: 16rpx;
	}

	.row-value {
		font-size: 28rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.row-calendar {
		width: 36rpx;
		height: 36rpx;
		margin-left: 10rpx;
	}

	.row-arrow {
		width: 24rpx;
		height: 24rpx;
		margin-left: 10rpx;
		opacity: 0.55;
	}

	.footer {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 24rpx 48rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
	}

	.start-btn {
		position: relative;
		height: 96rpx;
		border-radius: 20rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.start-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.start-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}
</style>
