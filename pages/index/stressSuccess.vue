<template>
	<view class="page">
		<view class="header">
			<image class="success-icon" src="/static/celiangSuccess.png" mode="aspectFit"></image>
			<text class="title">压力测量完成</text>
			<text class="subtitle">数据已保存至健康记录</text>
			<view class="value-row">
				<text class="bpm">{{ stress }}</text>
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
	// 刻度三等分：放松 / 正常 / 偏高，交界约 30、60、90
	const SCALE_MIN = 0
	const SCALE_LOW = 30
	const SCALE_MID = 60
	const SCALE_HIGH = 90
	const SCALE_MAX = 120
	const DEFAULT_REFERENCE = [
		'放松：<30',
		'正常：30~59',
		'中等：60~89',
		'偏高：≥90'
	]
	const DEFAULT_ADVICE = [
		'建议结合作息、运动与情绪变化长期观察压力指数；数值越低通常表示越放松。',
		'如连续偏高并伴随明显不适，请及时咨询专业医生。'
	]

	function clamp01(value) {
		if (value < 0) return 0
		if (value > 1) return 1
		return value
	}

	function stressToPercent(stress) {
		const quarter = 25
		if (stress <= SCALE_LOW) {
			return clamp01((stress - SCALE_MIN) / (SCALE_LOW - SCALE_MIN)) * quarter
		}
		if (stress <= SCALE_MID) {
			return quarter + ((stress - SCALE_LOW) / (SCALE_MID - SCALE_LOW)) * quarter
		}
		if (stress <= SCALE_HIGH) {
			return quarter * 2 + ((stress - SCALE_MID) / (SCALE_HIGH - SCALE_MID)) * quarter
		}
		return quarter * 3 + clamp01((stress - SCALE_HIGH) / (SCALE_MAX - SCALE_HIGH)) * quarter
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
				stress: 72,
				measureId: '',
				apiStatus: '',
				apiResultText: '',
				apiAdvice: '',
				referenceList: DEFAULT_REFERENCE.slice()
			}
		},
		computed: {
			statusKey() {
				if (this.stress < 30) return 'relax'
				if (this.stress < 60) return 'normal'
				if (this.stress < 90) return 'medium'
				return 'high'
			},
			statusText() {
				if (this.apiStatus) return this.apiStatus
				if (this.statusKey === 'relax') return '放松'
				if (this.statusKey === 'normal') return '正常'
				if (this.statusKey === 'medium') return '中等'
				return '偏高'
			},
			resultText() {
				if (this.apiResultText) return this.apiResultText
				if (this.statusKey === 'relax') {
					return '当前压力较低，身心状态较放松。建议继续保持规律作息与适度休息。'
				}
				if (this.statusKey === 'normal') {
					return '当前压力处于常见范围。建议结合工作负荷与睡眠质量持续观察。'
				}
				if (this.statusKey === 'medium') {
					return '当前压力中等。建议适当放慢节奏，通过深呼吸、短暂休息帮助恢复。'
				}
				return '当前压力偏高。建议减少高强度负荷，保证睡眠，必要时寻求专业建议。'
			},
			adviceList() {
				const lines = splitLines(this.apiAdvice)
				return lines.length ? lines : DEFAULT_ADVICE
			},
			pointerLeft() {
				return `${stressToPercent(this.stress).toFixed(2)}%`
			}
		},
		onLoad(options) {
			const stress = Number(options && options.stress)
			if (stress) this.stress = stress
			this.measureId = (options && options.measureId) ? decodeURIComponent(options.measureId) : ''
			this.loadResult()
		},
		methods: {
			async loadResult(retry = 0) {
				if (!this.measureId) return
				try {
					const data = await getMeasureResult(this.measureId, 'stress')
					const ready = data && (Number(data.value) > 0 || data.status || data.resultText)
					if (!ready) {
						if (retry < 2) {
							setTimeout(() => this.loadResult(retry + 1), 600)
						}
						return
					}
					const value = Number(data.value)
					if (value > 0) this.stress = Math.round(value)
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
					url: '/pages/index/stress'
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
