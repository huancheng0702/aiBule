<template>
	<view class="page">
		<!-- 顶部横幅 -->
		<view class="hero-card">
			<!-- 横幅背景图，自行补充 src -->
			<image class="hero-bg" src="/static/jingqiBan.png" mode=""></image>

		</view>

		<!-- 什么是生理周期 -->
		<view class="card">
			<text class="card-title">什么是生理周期?</text>
			<text class="card-text">{{ introText }}</text>
		</view>

		<!-- 四个阶段 -->
		<view class="section">
			<text class="section-title">周期的四个阶段</text>
			<view class="card phase-card">
				<view v-for="(item, index) in phaseList" :key="item.title" class="phase-item"
					:class="{ border: index < phaseList.length - 1 }">
					<text class="phase-title">{{ item.title }}</text>
					<text class="phase-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<!-- 记录建议 -->
		<view class=" tip-card">
			<image class="tip-bg" src="/static/heartCardBg.png" mode=""></image>
			<view class="tip-body">
				<view class="tip-header">
					<!-- 灯泡图标 -->
					<image class="tip-icon" src="/static/qushijianyi.png" mode="aspectFit"></image>
					<text class="tip-title">记录建议</text>
				</view>
				<text class="tip-text">{{ tipText }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getHealthTypeInfoList
	} from '@/common/api/config.js'

	const DEFAULT_INTRO = '生理周期从一次月经来潮的第一天开始，到下一次月经来潮前一天结束。周期长度存在个体差异，也可能受睡眠、压力、运动和生活方式影响。'
	const DEFAULT_TIP = '准确记录每次月经开始与结束日期；记录经量、痛经及其他身体感受；关注睡眠、压力和运动对周期的影响；周期持续异常或伴明显不适时咨询医生。'
	const DEFAULT_PHASES = [{
			title: '经期',
			desc: '子宫内膜脱落并出现月经，适合关注休息、经量及身体感受。'
		},
		{
			title: '卵泡期',
			desc: '月经结束后进入卵泡发育阶段，身体状态通常逐渐恢复。'
		},
		{
			title: '排卵期',
			desc: '排卵日前后为易孕时段，日历预测仅用于周期管理参考。'
		},
		{
			title: '黄体期',
			desc: '排卵后至下次月经前，部分人可能出现情绪或身体变化。'
		}
	]

	function pickText(item, keys) {
		if (!item) return ''
		for (let i = 0; i < keys.length; i++) {
			const v = item[keys[i]]
			if (v) return String(v)
		}
		return ''
	}

	export default {
		data() {
			return {
				introText: DEFAULT_INTRO,
				tipText: DEFAULT_TIP,
				phaseList: DEFAULT_PHASES.slice()
			}
		},
		onShow() {
			this.loadTypeInfo()
		},
		methods: {
			async loadTypeInfo() {
				try {
					const data = await getHealthTypeInfoList('5')
					const list = Array.isArray(data) ? data : (data && (data.list || data.items || data.records)) || []
					if (!list.length) return
					const sorted = list.slice().sort((a, b) => {
						return Number(a.sort || a.sortNo || a.orderNum || 0) - Number(b.sort || b.sortNo || b
							.orderNum || 0)
					})
					const intro = []
					const phases = []
					const tips = []
					sorted.forEach((item) => {
						const title = pickText(item, ['title', 'name', 'blockTitle', 'label'])
						const desc = pickText(item, ['content', 'desc', 'description', 'text', 'remark'])
						if (!title && !desc) return
						if (/建议|提示|注意/.test(title)) {
							tips.push(desc || title)
							return
						}
						if (/阶段|经期|卵泡|排卵|黄体/.test(title) && desc) {
							phases.push({
								title: title || '阶段',
								desc
							})
							return
						}
						intro.push(desc || title)
					})
					if (intro.length) this.introText = intro.join('\n')
					if (phases.length) this.phaseList = phases
					if (tips.length) this.tipText = tips.join('\n')
				} catch (e) {
					console.log('[lookPeriod] type info failed', e && (e.msg || e.message))
				}
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
		height: 230rpx;
		padding: 36rpx 28rpx;
		border-radius: 24rpx;
		background: linear-gradient(135deg, #0d2a6b 0%, #1a53ec 45%, #4da3ff 100%);
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
		flex: 1;
		min-width: 0;
		padding-right: 16rpx;
	}

	.hero-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.hero-desc {
		display: block;
		margin-top: 16rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.82);
		line-height: 36rpx;
	}

	.hero-deco {
		width: 160rpx;
		height: 160rpx;
		flex-shrink: 0;
	}

	.card {
		margin-top: 24rpx;
		padding: 32rpx 28rpx;
		background: #1A1B25;
		border-radius: 24rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.card-title {
		display: block;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.card-text {
		display: block;
		margin-top: 16rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 42rpx;
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

	.phase-card {
		margin-top: 0;
		padding: 8rpx 28rpx;
	}

	.phase-item {
		padding: 28rpx 0;
	}

	.phase-item.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.phase-title {
		display: block;
		font-size: 28rpx;
		font-weight: 600;
		color: #4da3ff;
		line-height: 40rpx;
	}

	.phase-desc {
		display: block;
		margin-top: 10rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 40rpx;
	}

	.tip-card {
		margin-top: 24rpx;
		position: relative;
		padding: 32rpx 28rpx;
	}

	.tip-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		pointer-events: none;
	}

	.tip-body {
		position: relative;
		z-index: 1;
	}

	.tip-header {
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
	}

	.tip-icon {
		width: 44rpx;
		height: 44rpx;
		margin-right: 12rpx;
	}

	.tip-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.tip-text {
		display: block;
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 42rpx;
		padding-left: 28px;
	}
</style>