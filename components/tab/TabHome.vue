<template>
	<view class="tab-page">
		<!-- 顶部问候 -->
		<view class="header">
			<view class="header-left">
				<text class="greet">{{ greetText }}</text>
				<text class="greet-sub">今天也要关心自己</text>
			</view>
			<view class="battery">
				<image class="battery-icon" mode="aspectFit"></image>
				<text class="battery-text">86%</text>
			</view>
		</view>

		<!-- 智能戒指横幅 -->
		<view class="hero-banner">
			<view class="hero-left">
				<text class="hero-title">健康变化一眼掌握</text>
				<text class="hero-desc">汇总每日关键指标，用清晰趋势帮助你了解身体节奏。</text>
			</view>
			<image class="hero-ring" mode="aspectFit"></image>
		</view>

		<!-- 健康管家 -->
		<view class="butler-card">
			<image class="butler-avatar" mode="aspectFill"></image>
			<view class="butler-info">
				<text class="butler-title">健康管家</text>
				<text class="butler-desc">专属健康咨询 · 报告解读 · 定制建议</text>
			</view>
			<text class="butler-arrow">›</text>
		</view>

		<!-- 身体状态 -->
		<view class="section">
			<text class="section-title">现在了解身体状态</text>
			<view class="status-grid">
				<!-- 今日步数 -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">今日步数</text>
					</view>
					<view class="card-value-row">
						<text class="card-value">68,444</text>
						<text class="card-unit">步</text>
					</view>
					<text class="card-sub">4.8km · 288kcal</text>
					<view class="progress-bar">
						<view class="progress-inner" style="width: 72%;"></view>
					</view>
				</view>

				<!-- 昨夜睡眠 -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">昨夜睡眠</text>
					</view>
					<view class="card-value-row">
						<text class="card-value">1</text>
						<text class="card-unit">小时</text>
						<text class="card-value">32</text>
						<text class="card-unit">分</text>
					</view>
					<text class="card-sub">4.8km · 288kcal</text>
					<view class="sleep-bars">
						<view class="sleep-bar orange"></view>
						<view class="sleep-bar purple"></view>
						<view class="sleep-bar cyan"></view>
					</view>
				</view>

				<!-- 心率 -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">心率</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">98</text>
								<text class="card-unit">ms</text>
							</view>
						</view>
						<view class="metric-chart">
							<image class="chart-img" mode="aspectFit"></image>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 血氧 -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">血氧</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">98</text>
								<text class="card-unit">%</text>
							</view>
						</view>
						<view class="metric-chart">
							<image class="chart-img" mode="aspectFit"></image>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- HRV -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">HRV</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">98</text>
								<text class="card-unit">ms</text>
							</view>
						</view>
						<view class="metric-chart">
							<image class="chart-img" mode="aspectFit"></image>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 压力 -->
				<view class="status-card">
					<view class="card-head">
						<image class="card-icon" mode="aspectFit"></image>
						<text class="card-label">压力</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">98</text>
							</view>
						</view>
						<view class="metric-chart">
							<image class="chart-img" mode="aspectFit"></image>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 手动健康记录 -->
		<view class="section section-last">
			<view class="section-row">
				<text class="section-title">手动健康记录</text>
				<text class="section-action" @click="openManagePopup">管理显示</text>
			</view>
			<view class="record-grid">
				<view
					v-for="item in visibleRecords"
					:key="item.key"
					class="record-card"
				>
					<image class="record-icon" mode="aspectFit"></image>
					<view class="record-info">
						<text class="record-label">{{ item.homeLabel }}</text>
						<text class="record-value" :class="{ empty: !item.value }">{{ item.value || '暂无数据' }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 管理显示弹窗 -->
		<view v-if="showManagePopup" class="popup-mask" @click="closeManagePopup">
			<view class="popup-sheet" @click.stop>
				<!-- 背景图，自行补充 src -->
				<image class="popup-bg" mode="aspectFill"></image>
				<view class="popup-content">
					<view class="popup-header">
						<text class="popup-title">手动健康显示</text>
						<text class="popup-desc">隐藏项目仍可在健康档案中继续记录</text>
					</view>
					<view class="popup-list">
						<view
							v-for="(item, index) in recordList"
							:key="item.key"
							class="popup-item"
						>
							<view class="popup-item-left">
								<text class="popup-item-title">{{ item.manageLabel }}</text>
								<text class="popup-item-sub">显示在首页</text>
							</view>
							<switch
								:checked="item.visible"
								color="#3b82f6"
								@change="onToggleChange(index, $event)"
							/>
						</view>
					</view>
					<view class="popup-btn" @click="confirmManagePopup">完成</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'TabHome',
		data() {
			return {
				showManagePopup: false,
				recordList: [
					{ key: 'weight', manageLabel: '体重', homeLabel: '体重', value: '45 kg', visible: true },
					{ key: 'sugar', manageLabel: '血糖', homeLabel: '血糖', value: '', visible: true },
					{ key: 'temp', manageLabel: '体温', homeLabel: '体温', value: '36.5 ℃', visible: true },
					{ key: 'bp', manageLabel: '血压', homeLabel: '血压', value: '116/74 mmHg', visible: true },
					{ key: 'period', manageLabel: '经期', homeLabel: '生理周期', value: '', visible: false }
				]
			}
		},
		computed: {
			greetText() {
				const hour = new Date().getHours()
				if (hour < 6) return '夜深了'
				if (hour < 12) return '早上好'
				if (hour < 14) return '中午好'
				if (hour < 18) return '下午好'
				return '晚上好'
			},
			visibleRecords() {
				return this.recordList.filter(item => item.visible)
			}
		},
		methods: {
			openManagePopup() {
				this.showManagePopup = true
			},
			closeManagePopup() {
				this.showManagePopup = false
			},
			confirmManagePopup() {
				this.showManagePopup = false
			},
			onToggleChange(index, e) {
				this.$set(this.recordList[index], 'visible', e.detail.value)
			}
		}
	}
</script>

<style scoped>
	.tab-page {
		min-height: 100%;
		padding: calc(var(--status-bar-height) + 24rpx) 28rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-bottom: 28rpx;
	}

	.header-left {
		display: flex;
		flex-direction: column;
	}

	.greet {
		font-size: 26rpx;
		color: #8e8e93;
		line-height: 36rpx;
	}

	.greet-sub {
		margin-top: 8rpx;
		font-size: 40rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 56rpx;
	}

	.battery {
		display: flex;
		align-items: center;
		height: 56rpx;
		padding: 0 20rpx 0 16rpx;
		border-radius: 28rpx;
		background: #1A1B25;
		margin-top: 8rpx;
	}

	.battery-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 8rpx;
	}

	.battery-text {
		font-size: 24rpx;
		color: #ffffff;
		font-weight: 500;
	}

	.hero-banner {
		position: relative;
		display: flex;
		align-items: center;
		min-height: 200rpx;
		padding: 36rpx 28rpx;
		border-radius: 28rpx;
		background: linear-gradient(135deg, #d9e8ff 0%, #c5d9f7 45%, #b8ccee 100%);
		overflow: hidden;
		box-sizing: border-box;
	}

	.hero-left {
		flex: 1;
		padding-right: 180rpx;
		z-index: 1;
	}

	.hero-title {
		display: block;
		font-size: 34rpx;
		font-weight: 700;
		color: #1a1a1a;
		line-height: 48rpx;
	}

	.hero-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #4a5568;
		line-height: 34rpx;
	}

	.hero-ring {
		position: absolute;
		right: 8rpx;
		bottom: -10rpx;
		width: 220rpx;
		height: 220rpx;
	}

	.butler-card {
		display: flex;
		align-items: center;
		margin-top: 20rpx;
		padding: 24rpx 28rpx;
		border-radius: 28rpx;
		background: linear-gradient(90deg, #1a3a6b 0%, #16325c 50%, #122848 100%);
		box-sizing: border-box;
	}

	.butler-avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 50%;
		flex-shrink: 0;
		background: rgba(255, 255, 255, 0.12);
	}

	.butler-info {
		flex: 1;
		margin-left: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.butler-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 42rpx;
	}

	.butler-desc {
		margin-top: 4rpx;
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 32rpx;
	}

	.butler-arrow {
		font-size: 40rpx;
		color: rgba(255, 255, 255, 0.5);
		line-height: 1;
		padding-left: 12rpx;
	}

	.section {
		margin-top: 40rpx;
	}

	.section-last {
		padding-bottom: 20rpx;
	}

	.section-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 44rpx;
	}

	.section-action {
		font-size: 26rpx;
		color: #4da3ff;
		line-height: 44rpx;
	}

	.status-grid {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.status-card {
		width:334rpx;
		margin-bottom: 28rpx;
		padding: 24rpx;
		border-radius: 24rpx;
		background: #1A1B25;
		box-sizing: border-box;
	}

	.card-head {
		display: flex;
		align-items: center;
	}

	.card-icon {
		width: 36rpx;
		height: 36rpx;
		margin-right: 10rpx;
		flex-shrink: 0;
	}

	.card-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.card-value-row {
		display: flex;
		align-items: baseline;
		flex-wrap: wrap;
		margin-top: 16rpx;
	}

	.card-value {
		font-size: 40rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 52rpx;
	}

	.card-unit {
		margin-left: 6rpx;
		margin-right: 6rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.card-sub {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #636366;
		line-height: 30rpx;
	}

	.progress-bar {
		margin-top: 20rpx;
		height: 10rpx;
		border-radius: 5rpx;
		background: rgba(77, 163, 255, 0.15);
		overflow: hidden;
	}

	.progress-inner {
		height: 100%;
		border-radius: 5rpx;
		background: #4da3ff;
	}

	.sleep-bars {
		display: flex;
		align-items: flex-end;
		margin-top: 20rpx;
		height: 28rpx;
	}

	.sleep-bar {
		height: 12rpx;
		border-radius: 4rpx;
		margin-right: 6rpx;
	}

	.sleep-bar.orange {
		width: 28%;
		background: #ff9f0a;
	}

	.sleep-bar.purple {
		width: 40%;
		background: #bf5af2;
	}

	.sleep-bar.cyan {
		width: 24%;
		margin-right: 0;
		background: #64d2ff;
	}

	.metric-body {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		margin-top: 8rpx;
	}

	.metric-left {
		flex-shrink: 0;
	}

	.metric-chart {
		flex: 1;
		margin-left: 12rpx;
		display: flex;
		flex-direction: column;
		align-items: stretch;
	}

	.chart-img {
		width: 100%;
		height: 64rpx;
	}

	.chart-time {
		display: flex;
		justify-content: space-between;
		margin-top: 4rpx;
		font-size: 18rpx;
		color: #636366;
		line-height: 24rpx;
	}

	.record-grid {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.record-card {
		width: calc(50% - 10rpx);
		margin-bottom: 20rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		background: #1A1B25;
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.record-icon {
		width: 56rpx;
		height: 56rpx;
		flex-shrink: 0;
		margin-right: 16rpx;
	}

	.record-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.record-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.record-value {
		margin-top: 6rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 40rpx;
	}

	.record-value.empty {
		font-size: 26rpx;
		font-weight: 400;
		color: #636366;
	}

	.popup-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
	}

	.popup-sheet {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #0b1220;
		margin: 30rpx;
	}

	.popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.popup-content {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx calc(32rpx + constant(safe-area-inset-bottom));
		padding: 40rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
		box-sizing: border-box;
	}

	.popup-header {
		margin-bottom: 28rpx;
	}

	.popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.popup-desc {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.popup-list {
		margin-bottom: 32rpx;
	}

	.popup-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 28rpx 28rpx;
		margin-bottom: 16rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.popup-item:last-child {
		margin-bottom: 0;
	}

	.popup-item-left {
		display: flex;
		flex-direction: column;
	}

	.popup-item-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 42rpx;
	}

	.popup-item-sub {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8e8e93;
		line-height: 30rpx;
	}

	.popup-btn {
		height: 96rpx;
		line-height: 96rpx;
		text-align: center;
		border-radius: 48rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
	}
</style>
