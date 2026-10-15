<template>
	<view class="tab-page">
		<!-- 顶部标题：paddingTop 用系统状态栏高度适配不同机型 -->
		<view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<text class="nav-title">健康中心</text>
			<text class="page-title">你的健康，一目了然</text>
		</view>

		<!-- 健康画像 -->
		<view class="portrait-card" @click="navHealth">
			<image src="/static/helthTopImg.png" class="cardImg" mode=""></image>
			<view class="porText">
				<text class="porStatus">{{ portraitStatus }}</text>
			</view>
		</view>

		<!-- 最近数据 -->
		<view class="section">
			<text class="section-title">最近数据</text>

			<!-- 健康报告中心 -->
			<view class="report-card" @click="navAiReport">
				<image src="/static/jiankangbaogao.png" class="reprotImg" mode=""></image>
			</view>

			<!-- 异常记录 / AI报告解读 -->
			<view class="mini-grid">
				<view class="mini-card" @click="navRecord(1)">
					<image class="mini-bg" src="/static/yichangBg.png" mode="aspectFill"></image>
					<image class="mini-icon" mode="aspectFit"></image>
					<view class="mini-info">
						<text class="mini-title">异常记录</text>
						<text class="mini-desc">{{ abnormalDesc }}</text>
					</view>
				</view>
				<view class="mini-card" @click="navRecord(0)">
					<image class="mini-bg" src="/static/aibaogaojie.png" mode="aspectFill"></image>

				</view>
			</view>
		</view> 
	</view>
</template>

<script>
	import { getHealthCenterIndex } from '@/common/api/healthCenter.js'
	import { getAbnormalList } from '@/common/api/healthAbnormal.js'
	import { getWeeklyList } from '@/common/api/healthReport.js'

	function asArray(data) {
		if (!data) return []
		if (Array.isArray(data)) return data
		if (Array.isArray(data.list)) return data.list
		if (Array.isArray(data.rows)) return data.rows
		if (Array.isArray(data.records)) return data.records
		return []
	}

	function pickText(...vals) {
		for (let i = 0; i < vals.length; i++) {
			const v = vals[i]
			if (v != null && String(v).trim() !== '') return String(v).trim()
		}
		return ''
	}

	export default {
		name: 'TabHealth',
		data() {
			return {
				statusBarHeight: 20,
				portraitStatus: '加载中...',
				portraitDesc: '',
				portraitScore: '',
				abnormalDesc: '暂无异常记录',
				reportDesc: ''
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
			this.loadData()
		},
		methods: {
			onTabShow() {
				this.loadData()
			},
			async loadData() {
				this.loadPortrait()
				this.loadAbnormal()
				this.loadReport()
			},
			async loadPortrait() {
				try {
					const data = await getHealthCenterIndex()
					if (!data || typeof data !== 'object') return
					const score = data.score != null ? data.score : (data.totalScore != null ? data.totalScore : data.healthScore)
					if (score != null && score !== '') this.portraitScore = Math.round(Number(score))
					this.portraitStatus = pickText(data.status, data.statusText, data.summaryStatus, data.title) || '整体状态良好'
					this.portraitDesc = pickText(data.description, data.desc, data.summary, data.summaryDesc, data.content, data.advice)
				} catch (e) {
					this.portraitStatus = '暂无健康解读'
					this.portraitDesc = ''
				}
			},
			async loadAbnormal() {
				try {
					const data = await getAbnormalList({ pageNum: 1, pageSize: 1 })
					const total = Number(data && (data.total != null ? data.total : data.count))
					const list = asArray(data)
					if (!Number.isNaN(total) && total >= 0) {
						this.abnormalDesc = total > 0 ? `${total} 条异常` : '暂无异常记录'
						return
					}
					this.abnormalDesc = list.length ? `${list.length} 条异常` : '暂无异常记录'
				} catch (e) {
					this.abnormalDesc = '暂无异常记录'
				}
			},
			async loadReport() {
				try {
					const data = await getWeeklyList({ pageNum: 1, pageSize: 1 })
					const list = asArray(data)
					const first = list[0]
					if (first) {
						this.reportDesc = pickText(first.title, first.date, first.weekRange, first.summary)
					}
				} catch (e) {}
			},
			navHealth() {
				uni.navigateTo({
					url: '/pages/health/healthBody'
				})
			},
			navAiReport() {
				uni.navigateTo({
					url: '/pages/health/aiReport'
				})
			},
			navRecord(i) {
				uni.navigateTo({
					url: '/pages/health/healthRecord?type=' + i
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		min-height: 100%;
		padding: 0 28rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.header {
		display: flex;
		flex-direction: column;
		padding-bottom: 32rpx;
	}

	.nav-title {
		font-size: 26rpx;
		line-height: 36rpx;
		color: #ffffff;
	}

	.page-title {
		margin-top: 12rpx;
		font-size: 44rpx;
		font-weight: 700;
		line-height: 60rpx;
		color: #ffffff;
	}
	
	.portrait-left {
		position: relative;
		z-index: 2;
		flex: 1;
		display: flex;
		flex-direction: column;
		padding-right: 16rpx;
	}

	.portrait-tag {
		display: flex;
		align-items: center;
	}

	.portrait-tag-icon {
		width: 36rpx;
		height: 36rpx;
		margin-right: 10rpx;
		border-radius: 8rpx;
		background: rgba(255, 255, 255, 0.15);
	}

	.portrait-tag-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.92);
	}

	.portrait-status {
		margin-top: 36rpx;
		font-size: 44rpx;
		font-weight: 700;
		line-height: 60rpx;
		color: #ffffff;
	}

	.portrait-desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.72);
	}

	.portrait-btn {
		margin-top: 36rpx;
		align-self: flex-start;
		padding: 14rpx 32rpx;
		border-radius: 999rpx;
		background: linear-gradient(90deg, #2f7dff 0%, #4aa3ff 100%);
	}

	.portrait-btn-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: #ffffff;
		font-weight: 500;
	}

	.portrait-figure {
		position: relative;
		z-index: 1;
		width: 240rpx;
		height: 300rpx;
		flex-shrink: 0;
		align-self: center;
		background: rgba(255, 255, 255, 0.06);
		border-radius: 20rpx;
	}

	.section {
		margin-top: 48rpx;
	}

	.section-title {
		display: block;
		font-size: 36rpx;
		font-weight: 600;
		line-height: 50rpx;
		color: #ffffff;
		margin-bottom: 24rpx;
	}
	.reprotImg{
		width: 100%;
		height: 264rpx;
	}

	.report-left {
		position: relative;
		z-index: 2;
		display: flex;
		align-items: center;
		flex: 1;
		min-width: 0;
		padding-right: 16rpx;
	}

	.report-icon {
		width: 72rpx;
		height: 72rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.12);
	}

	.report-info {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.report-title {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.report-desc {
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.65);
	}

	.report-btn {
		position: relative;
		z-index: 2;
		flex-shrink: 0;
		padding: 12rpx 28rpx;
		border-radius: 999rpx;
		background: linear-gradient(90deg, #2f7dff 0%, #4aa3ff 100%);
	}

	.report-btn-text {
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.report-deco {
		position: absolute;
		right: 120rpx;
		top: 50%;
		transform: translateY(-50%);
		width: 160rpx;
		height: 120rpx;
		opacity: 0.35;
		z-index: 1;
	}

	.mini-grid {
		display: flex;
		margin-top: 20rpx;
		gap: 20rpx;
	}

	.mini-card {
		position: relative;
		flex: 1;
		display: flex;
		align-items: center;
		height: 188rpx;
		padding: 24rpx 20rpx;
		border-radius: 24rpx;
		overflow: hidden;
		box-sizing: border-box;
	}

	.mini-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.mini-icon {
		position: relative;
		z-index: 1;
		width: 56rpx;
		height: 56rpx;
		margin-right: 14rpx;
		flex-shrink: 0;
		border-radius: 12rpx;
	}

	.mini-info {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		padding-left: 24rpx;
	}

	.mini-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.mini-desc {
		margin-top:12rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.mini-arrow {
		position: relative;
		z-index: 1;
		margin-left: 8rpx;
		font-size: 36rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
		flex-shrink: 0;
	}
	.portrait-card{
		position: relative;
		width: 100%;
		height: 497rpx;
	}
	.porText{
		width: 50%;
		height: 100%;
		color: #FFFFFF;
		font-size: 48rpx;
		position: absolute;
		top: 30%;
		left: 30rpx;
		display: flex;
		flex-direction: column;
	}
	.porStatus {
		font-size: 48rpx;
		font-weight: 700;
		line-height: 60rpx;
		color: #FFFFFF;
	}
	.porDesc {
		margin-top: 12rpx;
		max-width: 380rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255,255,255,0.72);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.porScore {
		margin-top: 8rpx;
		font-size: 28rpx;
		line-height: 38rpx;
		color: rgba(255,255,255,0.85);
	}
	.cardImg{
		width: 100%;
		height: 100%;
	}
</style>
