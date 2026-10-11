<template>
	<view class="page">
		<view class="header">
			<image class="success-icon" src="/static/celiangSuccess.png" mode="aspectFit"></image>
			<text class="title">HRV 测量完成</text>
			<text class="subtitle">数据已保存至健康记录</text>
			<view class="value-row">
				<text class="bpm">{{ hrv }}</text>
				<text class="unit">ms</text>
				<view class="status-tag">
					<text class="status-tag-text">{{ statusText }}</text>
				</view>
			</view>
		</view>

		<view class="scale-box">
			<view class="scale-track">
				<image class="scale-img" src="/static/keduHeart.png" mode=""></image>
				<image class="scale-pointer" :style="{ left: pointerLeft }" src="/static/zhizhen.png" mode=""></image>
			</view>
		</view>

		<view class="cards">
			<view class="info-card">
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/jiancejieguo.png" mode=""></image>
				<view class="card-main">
					<image class="card-title" style="width: 148rpx;" src="/static/jianceText.png" mode=""></image>
					<text class="card-text">{{ resultText }}</text>
				</view>
			</view>

			<view class="info-card">
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/cankaofanwei.png" mode=""></image>
				<view class="card-main">
					<image class="card-title" style="width: 208rpx;" src="/static/cankaoText.png" mode=""></image>
					<text v-for="(line, index) in referenceList" :key="index" class="card-text">{{ line }}</text>
				</view>
			</view>

			<view class="info-card">
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/qushijianyi.png" mode=""></image>
				<view class="card-main">
					<image class="card-title" style="width: 148rpx;" src="/static/qushiText.png" mode=""></image>
					<text v-for="(line, index) in adviceList" :key="index" class="card-text">{{ line }}</text>
				</view>
			</view>
		</view>

		<view class="retry-btn" @click="onRetry">
			<image class="retry-btn-bg" src="/static/aginJian.png" mode=""></image>
		</view>
	</view>
</template>

<script>
	import { getMeasureResult } from '@/common/api/deviceData.js'
	// 刻度图三等分：偏低 / 正常 / 偏高，交界对应 20、100
	const SCALE_MIN = 0
	const SCALE_LOW = 20
	const SCALE_HIGH = 100
	const SCALE_MAX = 160
	const DEFAULT_REFERENCE = [
		'HRV 反映连续心跳间期的细微变化，',
		'可用于观察自主神经调节与恢复状态。',
		'建议优先与个人长期基线对比，而非单次绝对值。'
	]
	const DEFAULT_ADVICE = [
		'建议结合日常压力、睡眠与训练负荷观察 HRV 变化；更适合与自己的长期基线比较。',
		'如连续偏低并伴随明显不适，请及时咨询专业医生。'
	]

	function clamp01(value) {
		if (value < 0) return 0
		if (value > 1) return 1
		return value
	}

	function hrvToPercent(hrv) {
		const third = 100 / 3
		if (hrv <= SCALE_LOW) {
			return clamp01((hrv - SCALE_MIN) / (SCALE_LOW - SCALE_MIN)) * third
		}
		if (hrv <= SCALE_HIGH) {
			return third + ((hrv - SCALE_LOW) / (SCALE_HIGH - SCALE_LOW)) * third
		}
		return third * 2 + clamp01((hrv - SCALE_HIGH) / (SCALE_MAX - SCALE_HIGH)) * third
	}

	function splitLines(text) {
		if (!text) return []
		return String(text)
			.split(/\n+|；|;/)
			.map((s) => s.trim())
			.filter(Boolean)
	}

	export default {
		data() {
			return {
				hrv: 72,
				measureId: '',
				apiStatus: '',
				apiResultText: '',
				apiAdvice: '',
				referenceList: DEFAULT_REFERENCE.slice()
			}
		},
		computed: {
			statusKey() {
				if (this.hrv < SCALE_LOW) return 'low'
				if (this.hrv > SCALE_HIGH) return 'high'
				return 'normal'
			},
			statusText() {
				if (this.apiStatus) return this.apiStatus
				if (this.statusKey === 'low') return '偏低'
				if (this.statusKey === 'high') return '偏高'
				return '正常'
			},
			resultText() {
				if (this.apiResultText) return this.apiResultText
				if (this.statusKey === 'low') {
					return '本次 HRV 偏低，可能与压力、疲劳或恢复不足有关。建议保证休息与睡眠，避免过度训练。'
				}
				if (this.statusKey === 'high') {
					return '本次 HRV 偏高。通常提示副交感活性相对较强，建议结合近期休息与训练情况综合观察。'
				}
				return '本次 HRV 处于常见参考范围内。建议继续保持规律作息，并结合长期趋势观察恢复状态。'
			},
			adviceList() {
				const lines = splitLines(this.apiAdvice)
				return lines.length ? lines : DEFAULT_ADVICE
			},
			pointerLeft() {
				return `${hrvToPercent(this.hrv).toFixed(2)}%`
			}
		},
		onLoad(options) {
			const hrv = Number(options && options.hrv)
			if (hrv) this.hrv = hrv
			this.measureId = (options && options.measureId) ? decodeURIComponent(options.measureId) : ''
			this.loadResult()
		},
		methods: {
			async loadResult(retry = 0) {
				if (!this.measureId) return
				try {
					const data = await getMeasureResult(this.measureId, 'hrv')
					const ready = data && (Number(data.value) > 0 || data.status || data.resultText)
					if (!ready) {
						if (retry < 2) {
							setTimeout(() => this.loadResult(retry + 1), 600)
						}
						return
					}
					const value = Number(data.value)
					if (value > 0) this.hrv = Math.round(value)
					if (data.status) this.apiStatus = data.status
					if (data.resultText) this.apiResultText = data.resultText
					if (data.advice) this.apiAdvice = data.advice
					const refs = splitLines(data.referenceRange)
					if (refs.length) this.referenceList = refs
				} catch (e) {
					if (retry < 2) {
						setTimeout(() => this.loadResult(retry + 1), 600)
					}
				}
			},
			onRetry() {
				const pages = getCurrentPages()
				if (pages.length > 1) {
					uni.navigateBack()
					return
				}
				uni.redirectTo({
					url: '/pages/index/hrv'
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		padding: 36rpx 32rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		display: flex;
		flex-direction: column;
	}

	.header {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.success-icon {
		width: 112rpx;
		height: 112rpx;
	}

	.title {
		margin-top: 28rpx;
		font-size: 40rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 56rpx;
	}

	.subtitle {
		margin-top: 12rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.45);
		line-height: 36rpx;
	}

	.value-row {
		margin-top: 36rpx;
		display: flex;
		align-items: flex-end;
		justify-content: center;
	}

	.bpm {
		font-size: 108rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 108rpx;
	}

	.unit {
		margin-left: 8rpx;
		margin-bottom: 14rpx;
		font-size: 28rpx;
		color: rgba(255, 255, 255, 0.88);
		line-height: 40rpx;
	}

	.status-tag {
		margin-left: 16rpx;
		margin-bottom: 16rpx;
		padding: 4rpx 16rpx;
		border-radius: 8rpx;
		background: #2f6bff;
	}

	.status-tag-text {
		font-size: 22rpx;
		color: #ffffff;
		line-height: 32rpx;
	}

	.scale-box {
		margin: 0 auto;
		margin-top: 28rpx;
		margin-bottom: 42rpx;
		width: 442rpx;
	}

	.scale-track {
		position: relative;
		margin-top: 16rpx;
		height: 28rpx;
	}

	.scale-img {
		width: 442rpx;
		height: 90.1rpx;
		border-radius: 14rpx;
	}

	.scale-pointer {
		position: absolute;
		top: 45rpx;
		width: 22rpx;
		height: 22rpx;
		transform: translateX(-50%);
	}

	.cards {
		margin-top: 56rpx;
	}

	.info-card {
		position: relative;
		display: flex;
		align-items: flex-start;
		margin-bottom: 24rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		overflow: hidden;
		box-sizing: border-box;
	}

	.info-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.card-icon {
		position: relative;
		z-index: 1;
		width: 64rpx;
		height: 64rpx;
		flex-shrink: 0;
		margin-right: 20rpx;
	}

	.card-main {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
	}

	.card-title {
		display: block;
		width: 200rpx;
		height: 30rpx;
		margin-bottom: 18rpx;
	}

	.card-text {
		display: block;
		font-size: 26rpx;
		color: #ffffff;
		line-height: 40rpx;
	}

	.retry-btn {
		position: relative;
		height: 96rpx;
		margin-top: 16rpx;
	}

	.retry-btn-bg {
		width: 100%;
		height: 100%;
	}
</style>
