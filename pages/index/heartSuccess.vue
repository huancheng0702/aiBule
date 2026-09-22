<template>
	<view class="page">
		<view class="header">
			<!-- 完成图标，自行补充 src -->
			<image class="success-icon" src="/static/celiangSuccess.png" mode="aspectFit"></image>
			<text class="title">心率测量完成</text>
			<text class="subtitle">数据已保存至健康记录</text>
			<view class="value-row">
				<text class="bpm">{{ bpm }}</text>
				<text class="unit">bpm</text>
				<view class="status-tag">
					<text class="status-tag-text">{{ statusText }}</text>
				</view>
			</view>
		</view>

		<view class="scale-box">
			<view class="scale-track">
				<!-- 刻度条图片，自行补充 src。数字刻度不绘制，只叠指示针 -->
				<image class="scale-img" src="/static/keduHeart.png" mode=""></image>
				<image class="scale-pointer" :style="{ left: pointerLeft }" src="/static/zhizhen.png" mode=""></image>
			</view>
		</view>

		<view class="cards">
			<view class="info-card">
				<!-- 卡片背景图，自行补充 src -->
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/jiancejieguo.png" mode=""></image>
				<view class="card-main">
					<!-- 标题图，自行补充 src -->
					<image class="card-title" style="width: 148rpx;" src="/static/jianceText.png" mode=""></image>
					<text class="card-text">{{ resultText }}</text>
				</view>
			</view>

			<view class="info-card">
				<!-- 卡片背景图，自行补充 src -->
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/cankaofanwei.png" mode=""></image>
				<view class="card-main">
					<!-- 标题图，自行补充 src -->
					<image class="card-title" style="width: 208rpx;" src="/static/cankaoText.png" mode=""></image>
					<text v-for="(line, index) in referenceList" :key="index" class="card-text">{{ line }}</text>
				</view>
			</view>

			<view class="info-card">
				<!-- 卡片背景图，自行补充 src -->
				<image class="info-card-bg" src="/static/heartCardBg.png" mode=""></image>
				<image class="card-icon" src="/static/qushijianyi.png" mode=""></image>
				<view class="card-main">
					<!-- 标题图，自行补充 src -->
					<image class="card-title" style="width: 148rpx;" src="/static/qushiText.png" mode=""></image>
					<text class="card-text">建议结合日常活动量和长期趋势进行观察。保持规律作息与适量运动；</text>
					<text class="card-text">如连续出现明显异常或伴随不适，请及时咨询专业医生。</text>
				</view>
			</view>
		</view>

		<view class="retry-btn" @click="onRetry">
			<!-- 重新检测按钮背景图，自行补充 src -->
			<image class="retry-btn-bg" src="/static/aginJian.png" mode=""></image>
		</view>
	</view>
</template>

<script>
	// 刻度图三等分，色块交界对应 60、100。两端不展示，只用来算指针。
	const SCALE_MIN = 40
	const SCALE_LOW = 60
	const SCALE_HIGH = 100
	const SCALE_MAX = 160

	function clamp01(value) {
		if (value < 0) return 0
		if (value > 1) return 1
		return value
	}

	function bpmToPercent(bpm) {
		const third = 100 / 3
		if (bpm <= SCALE_LOW) {
			return clamp01((bpm - SCALE_MIN) / (SCALE_LOW - SCALE_MIN)) * third
		}
		if (bpm <= SCALE_HIGH) {
			return third + ((bpm - SCALE_LOW) / (SCALE_HIGH - SCALE_LOW)) * third
		}
		return third * 2 + clamp01((bpm - SCALE_HIGH) / (SCALE_MAX - SCALE_HIGH)) * third
	}

	export default {
		data() {
			return {
				bpm: 72,
				referenceList: [
					'成人正常心率：60~100次/分',
					'3岁以下儿童心率：>100次/分',
					'老年人心率：55~75次/分'
				]
			}
		},
		computed: {
			statusKey() {
				if (this.bpm < SCALE_LOW) return 'low'
				if (this.bpm > SCALE_HIGH) return 'high'
				return 'normal'
			},
			statusText() {
				if (this.statusKey === 'low') return '过缓'
				if (this.statusKey === 'high') return '过速'
				return '正常'
			},
			resultText() {
				if (this.statusKey === 'low') {
					return '您的心率偏低，属于心动过缓。建议注意休息，避免突然起身；如持续偏低或伴随不适，请及时咨询专业医生。'
				}
				if (this.statusKey === 'high') {
					return '您的心率偏高，属于心动过速。建议保持静息，避免剧烈活动；如伴随不适，请及时咨询专业医生。'
				}
				return '您的心率处于正常范围，建议您继续保持良好的生活习惯避免过度劳累和紧张。'
			},
			pointerLeft() {
				return `${bpmToPercent(this.bpm).toFixed(2)}%`
			}
		},
		onLoad(options) {
			const bpm = Number(options && options.bpm)
			if (bpm) this.bpm = bpm
		},
		methods: {
			onRetry() {
				const pages = getCurrentPages()
				if (pages.length > 1) {
					uni.navigateBack()
					return
				}
				uni.redirectTo({
					url: '/pages/index/heartRate'
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

	.scale-labels {
		display: flex;
	}

	.scale-label {
		width: 33.33%;
		text-align: center;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.82);
		line-height: 34rpx;
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