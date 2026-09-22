<template>
	<view class="tab-page">
		<!-- 顶部标题：paddingTop 用系统状态栏高度适配不同机型 -->
		<view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<text class="nav-title">健康中心</text>
			<text class="page-title">你的健康，一目了然</text>
		</view>

		<!-- 健康画像 -->
		<view class="portrait-card">
			<image src="/static/helthTopImg.png" class="cardImg" mode=""></image>
		</view>

		<!-- 最近数据 -->
		<view class="section">
			<text class="section-title">最近数据</text>

			<!-- 健康报告中心 -->
			<view class="report-card">
				<image src="/static/jiankangbaogao.png" class="reprotImg" mode=""></image>
			</view>

			<!-- 异常记录 / AI报告解读 -->
			<view class="mini-grid">
				<view class="mini-card">
					<image class="mini-bg" src="/static/yichangBg.png" mode="aspectFill"></image>
					<image class="mini-icon" mode="aspectFit"></image>
					<view class="mini-info">
						<text class="mini-title">异常记录</text>
						<text class="mini-desc">1 条已处理</text>
					</view>
				</view>
				<view class="mini-card">
					<image class="mini-bg" src="/static/aibaogaojie.png" mode="aspectFill"></image>

				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'TabHealth',
		data() {
			return {
				statusBarHeight: 20
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
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
	.cardImg{
		width: 100%;
		height: 497rpx;
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
</style>
