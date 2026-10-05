<template>
	<view class="tab-page">
		<!-- 顶部问候：paddingTop 用系统状态栏高度适配不同机型 -->
		<view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="header-left">
				<text class="greet">{{ greetText }}</text>
				<text class="greet-sub">今天也要关心自己</text>
			</view>
			<view class="battery">
				<image class="battery-icon" mode="aspectFit"></image>
				<text class="battery-text">{{ batteryText }}</text>
			</view>
		</view>

		<!-- 智能戒指横幅 -->
		<view class="hero-banner">
			<image src="/static/swiper.png" mode=""></image>
		</view>

		<!-- 健康管家 -->
		<image src="/static/indexKefu.png" class="indexkefu" mode="" @click="openButlerPopup"></image>

		<!-- 身体状态 -->
		<view class="section">
			<text class="section-title">现在了解身体状态</text>
			<view class="status-grid">
				<!-- 今日步数 -->
				<view class="status-card" @click="goTodayStep">
					<view class="card-head">
						<image class="card-icon" src="/static/bushu.png" mode="aspectFit"></image>
						<text class="card-label">今日步数</text>
					</view>
					<view class="card-value-row">
						<text class="card-value">{{ stepText }}</text>
						<text class="card-unit">步</text>
					</view>
					<text class="card-sub">{{ stepSubText }}</text>
					<view class="progress-bar">
						<view class="progress-inner" :style="{ width: stepProgress + '%' }"></view>
					</view>
				</view>

				<!-- 昨夜睡眠 -->
				<view class="status-card" @click="goNight">
					<view class="card-head">
						<image class="card-icon" src="/static/shuimian.png" mode="aspectFit"></image>
						<text class="card-label">昨夜睡眠</text>
					</view>
					<view class="card-value-row">
						<text class="card-value">{{ sleepHours }}</text>
						<text class="card-unit">小时</text>
						<text class="card-value">{{ sleepMinutes }}</text>
						<text class="card-unit">分</text>
					</view>
					<text class="card-sub">{{ sleepSubText }}</text>
					<view v-if="hasSleepData" class="sleep-bars">
						<view class="sleep-bar orange">
							<view class="sleep-bar-solid" :style="{ width: sleepBarDeep + '%' }"></view>
							<view class="sleep-bar-glow"></view>
						</view>
						<view class="sleep-bar purple">
							<view class="sleep-bar-solid" :style="{ width: sleepBarLight + '%' }"></view>
							<view class="sleep-bar-glow"></view>
						</view>
						<view class="sleep-bar cyan">
							<view class="sleep-bar-solid" :style="{ width: sleepBarRem + '%' }"></view>
							<view class="sleep-bar-glow"></view>
						</view>
					</view>
					<text v-else class="sleep-empty">暂无数据</text>
				</view>

				<!-- 心率 -->
				<view class="status-card" @click="goHeart">
					<view class="card-head">
						<image class="card-icon" src="/static/xinlv.png" mode="aspectFit"></image>
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
							<view class="chart-box">
								<qiun-data-charts
									v-if="chartReady"
									type="area"
									canvasId="homeHeartRate"
									:opts="heartRateOpts"
									:chartData="heartRateData"
									:tooltipShow="false"
									:ontap="false"
									:animation="false"
								/>
							</view>
							<view class="chart-axis-line"></view>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 血氧 -->
				<view class="status-card" @click="goBoold">
					<view class="card-head">
						<image class="card-icon" src="/static/xueyang.png" mode="aspectFit"></image>
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
							<view class="chart-box">
								<qiun-data-charts
									v-if="chartReady"
									type="column"
									canvasId="homeBloodOxygen"
									:opts="bloodOxygenOpts"
									:chartData="bloodOxygenData"
									:tooltipShow="false"
									:ontap="false"
									:animation="false"
								/>
							</view>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- HRV -->
				<view class="status-card" @click="goHrv">
					<view class="card-head">
						<image class="card-icon" src="/static/hrv.png" mode="aspectFit"></image>
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
							<view class="chart-box">
								<qiun-data-charts
									v-if="chartReady"
									type="area"
									canvasId="homeHrv"
									:opts="hrvOpts"
									:chartData="hrvData"
									:tooltipShow="false"
									:ontap="false"
									:animation="false"
								/>
							</view>
							<view class="chart-time">
								<text>00:00</text>
								<text>24:00</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 压力 -->
				<view class="status-card" @click="goStress">
					<view class="card-head">
						<image class="card-icon" src="/static/yali.png" mode="aspectFit"></image>
						<text class="card-label">压力</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">98</text>
							</view>
						</view>
						<view class="metric-chart">
							<view class="chart-box">
								<qiun-data-charts
									v-if="chartReady"
									type="column"
									canvasId="homeStress"
									:opts="stressOpts"
									:chartData="stressData"
									:tooltipShow="false"
									:ontap="false"
									:animation="false"
								/>
							</view>
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
				<view v-for="(item,index) in visibleRecords" :key="index" class="record-card" @click="goDetail(item)">
					<view class="reTop">
						<image class="record-icon" :src="item.icon" mode="aspectFit"></image>
						<view class="record-label">{{ item.homeLabel }}</view>
					</view>
					<view class="record-info">
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
						<view v-for="(item, index) in recordList" :key="item.key" class="popup-item">
							<view class="popup-item-left">
								<text class="popup-item-title">{{ item.manageLabel }}</text>
								<text class="popup-item-sub">显示在首页</text>
							</view>
							<switch :checked="item.visible" color="#3b82f6" @change="onToggleChange(index, $event)" />
						</view>
					</view>
					<view class="popup-btn" @click="confirmManagePopup">完成</view>
				</view>
			</view>
		</view>

		<!-- 健康管家弹窗 -->
		<view v-if="showButlerPopup" class="popup-mask butler-mask" @click="closeButlerPopup">
			<view class="butler-sheet" @click.stop>
				<!-- 背景图，自行补充 src -->
				<image class="popup-bg" src="/static/kefuBg.png" mode=""></image>
				<view class="butler-content">
					<text class="butler-title">健康管家</text>
					<view class="butler-qr-wrap">
						<!-- 二维码图，自行补充 src -->
						<image class="butler-qr" mode="aspectFit"></image>
					</view>
					<text class="butler-tip">微信扫码添加专属健康管家</text>
					<view class="butler-footer">
						<view class="butler-phone">
							<text class="butler-phone-label">管家联系电话</text>
							<text class="butler-phone-num">{{ butlerPhone }}</text>
						</view>
						<image src="/static/copyPhone.png" @click="copyButlerPhone" class="butler-copy-btn" mode=""></image>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import qiunDataCharts from '@/uni_modules/qiun-data-charts/components/qiun-data-charts/qiun-data-charts.vue'
	import BleService from '@/common/ble-service.js'

	function createChartOpts(extra = {}) {
		return {
			color: [],
			padding: [2, 2, 2, 2],
			legend: { show: false },
			dataLabel: false,
			dataPointShape: false,
			enableScroll: false,
			xAxis: {
				disabled: true,
				disableGrid: true,
				axisLine: false,
				boundaryGap: 'center'
			},
			yAxis: {
				disabled: true,
				disableGrid: true
			},
			...extra,
			xAxis: {
				disabled: true,
				disableGrid: true,
				axisLine: false,
				boundaryGap: 'center',
				...(extra.xAxis || {})
			},
			yAxis: {
				disabled: true,
				disableGrid: true,
				...(extra.yAxis || {})
			},
			extra: extra.extra || {}
		}
	}

	export default {
		name: 'TabHome',
		components: {
			qiunDataCharts
		},
		data() {
			return {
				statusBarHeight: 20,
				batteryLevel: null,
				batteryCharging: false,
				bleConnected: false,
				unsubBle: null,
				stepCurrent: 0,
				stepTarget: 8000,
				calorieCurrent: 0,
				distanceCurrent: 0,
				sleepHours: 0,
				sleepMinutes: 0,
				sleepScore: 0,
				sleepDeepRatio: 0,
				sleepLightRatio: 0,
				sleepRemRatio: 0,
				showManagePopup: false,
				showButlerPopup: false,
				butlerPhone: '400-820-1624',
				chartReady: false,
				heartRateData: {
					categories: Array.from({ length: 30 }, () => ''),
					series: [{
						name: '心率',
						data: [
							72, 95, 68, 110, 78, 125, 88, 102, 70, 118, 85, 98,
							76, 132, 90, 108, 74, 115, 82, 128, 94, 105, 80, 120
						]
					}]
				},
				heartRateOpts: createChartOpts({
					color: ['#FF5E7B'],
					xAxis: { boundaryGap: 'justify' },
					yAxis: {
						disabled: true,
						disableGrid: true,
						data: [{
							min: 0,
							max: 200
						}]
					},
					extra: {
						area: {
							type: 'straight',
							opacity: 0.45,
							addLine: true,
							width: 1.5,
							gradient: true,
							activeType: 'hollow'
						}
					}
				}),
				bloodOxygenData: {
					categories: Array.from({ length: 15 }, () => ''),
					series: [{
						name: '血氧',
						data: [62, 78, 55, 88, 70, 92, 48, 85, 66, 90, 58, 80, 72,]
					}]
				},
				bloodOxygenOpts: createChartOpts({
					color: ['#83DABF'],
					extra: {
						column: {
							type: 'group',
							width: 2.5,
							categoryGap: 1,
							activeBgColor: 'transparent',
							activeBgOpacity: 0
						}
					}
				}),
				hrvData: {
					categories: Array.from({ length: 30 }, () => ''),
					series: [{
						name: 'HRV',
						data: [35, 48, 72, 88, 70, 52, 40, 55, 78, 92, 80, 58, 42, 50, 68, 85, 75, 55, 38, 48, 62, 70, 58, 45]
					}]
				},
				hrvOpts: createChartOpts({
					color: ['#E1A2FF'],
					xAxis: { boundaryGap: 'justify' },
					yAxis: {
						disabled: true,
						disableGrid: true,
						data: [{
							min: 0,
							max: 200
						}]
					},
					extra: {
						area: {
							type: 'curve',
							opacity: 0.35,
							addLine: true,
							width: 1.5,
							gradient: true,
							activeType: 'none'
						}
					}
				}),
				stressData: {
					categories: ['1', '2', '3', '4', '5', '6'],
					series: [{
						name: '压力',
						data: [
							{ value: 88, color: '#FF9F0A' },
							{ value: 52, color: '#BF5AF2' },
							{ value: 78, color: '#64D2FF' },
							{ value: 92, color: '#0A84FF' },
							{ value: 48, color: '#5E5CE6' },
							{ value: 70, color: '#FF6B6B' }
						]
					}]
				},
				stressOpts: createChartOpts({
					color: ['#FF9F0A'],
					extra: {
						column: {
							type: 'group',
							width: 10,
							categoryGap: 6,
							barBorderCircle: true,
							activeBgColor: 'transparent',
							activeBgOpacity: 0
						}
					}
				}),
				recordList: [{
						key: 'weight',
						manageLabel: '体重',
						homeLabel: '体重',
						value: '45 kg',
						visible: true,
						icon:'/static/tizhong.png'
					},
					{
						key: 'sugar',
						manageLabel: '血糖',
						homeLabel: '血糖',
						value: '',
						visible: true,
						icon:'/static/xuetang.png'
					},
					{
						key: 'temp',
						manageLabel: '体温',
						homeLabel: '体温',
						value: '36.5 ℃',
						visible: true,
						icon:'/static/tiwen.png'
					},
					{
						key: 'bp',
						manageLabel: '血压',
						homeLabel: '血压',
						value: '116/74 mmHg',
						visible: true,
						icon:'/static/xueya.png'
					},
					{
						key: 'period',
						manageLabel: '经期',
						homeLabel: '生理周期',
						value: '',
						visible: true,
						icon:'/static/jingqi.png'
					}
				]
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
			this.unsubBle = BleService.subscribe((s) => {
				this.bleConnected = !!s.connected
				this.batteryLevel = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				if (s.sport) {
					this.stepCurrent = Number(s.sport.steps) || 0
					this.calorieCurrent = Number(s.sport.calories) || 0
					this.distanceCurrent = s.sport.distanceKm != null
						? Number(s.sport.distanceKm)
						: Math.round(((Number(s.sport.distanceM) || 0) / 1000) * 10) / 10
				}
				if (s.sleep) this.applyHomeSleep(s.sleep)
			})
			try {
				const goals = uni.getStorageSync('todayStepGoals')
				if (goals && goals.step > 0) this.stepTarget = goals.step
			} catch (e) {}
			const sport = BleService.getSport && BleService.getSport()
			if (sport) {
				this.stepCurrent = Number(sport.steps) || 0
				this.calorieCurrent = Number(sport.calories) || 0
				this.distanceCurrent = sport.distanceKm != null
					? Number(sport.distanceKm)
					: Math.round(((Number(sport.distanceM) || 0) / 1000) * 10) / 10
			}
			const sleep = BleService.getSleep && BleService.getSleep()
			if (sleep) this.applyHomeSleep(sleep)
		},
		beforeDestroy() {
			if (this.unsubBle) {
				this.unsubBle()
				this.unsubBle = null
			}
		},
		mounted() {
			this.$nextTick(() => {
				setTimeout(() => {
					this.chartReady = true
				}, 80)
			})
		},
		computed: {
			batteryText() {
				if (this.batteryLevel != null) {
					return this.batteryCharging
						? `${this.batteryLevel}% 充电中`
						: `${this.batteryLevel}%`
				}
				if (this.batteryCharging) return '充电中'
				return this.bleConnected ? '--%' : '--'
			},
			greetText() {
				const hour = new Date().getHours()
				if (hour < 6) return '夜深了'
				if (hour < 12) return '早上好'
				if (hour < 14) return '中午好'
				if (hour < 18) return '下午好'
				return '晚上好'
			},
			stepText() {
				const n = Number(this.stepCurrent) || 0
				return n.toLocaleString()
			},
			stepSubText() {
				const km = Number(this.distanceCurrent) || 0
				const kcal = Number(this.calorieCurrent) || 0
				return `${km}km · ${kcal}kcal`
			},
			stepProgress() {
				const t = Number(this.stepTarget) || 8000
				const p = Math.round((Number(this.stepCurrent) || 0) / t * 100)
				if (p < 0) return 0
				if (p > 100) return 100
				return p
			},
			sleepSubText() {
				if (!this.hasSleepData) return ''
				return this.sleepScore ? `得分 ${this.sleepScore}` : '戒指采集'
			},
			hasSleepData() {
				return !!(Number(this.sleepHours) || Number(this.sleepMinutes))
			},
			sleepBarDeep() {
				return Math.max(8, Math.min(100, Math.round((this.sleepDeepRatio || 0) * 100)))
			},
			sleepBarLight() {
				return Math.max(8, Math.min(100, Math.round((this.sleepLightRatio || 0) * 100)))
			},
			sleepBarRem() {
				return Math.max(8, Math.min(100, Math.round((this.sleepRemRatio || 0) * 100)))
			},
			visibleRecords() {
				return this.recordList.filter(item => item.visible)
			}
		},
		methods: {
			applyHomeSleep(sleep) {
				if (!sleep) return
				this.sleepHours = Number(sleep.hours) || 0
				this.sleepMinutes = Number(sleep.minutes) || 0
				this.sleepScore = Number(sleep.score) || 0
				const total = Math.max(1, Number(sleep.totalMin) || 0)
				this.sleepDeepRatio = (Number(sleep.deepMin) || 0) / total
				this.sleepLightRatio = (Number(sleep.lightMin) || 0) / total
				this.sleepRemRatio = (Number(sleep.remMin) || 0) / total
			},
			goDetail(item){
				if(item.key=='weight'){
					uni.navigateTo({
						url:'/pages/index/weight'
					})
				}
				if(item.key=='sugar'){
					uni.navigateTo({
						url:'/pages/index/sugar'
					})
				}
				if(item.key=='temp'){
					uni.navigateTo({
						url:'/pages/index/temperature'
					})
				}
				if(item.key=='bp'){
					uni.navigateTo({
						url:'/pages/index/pressure'
					})
				}
				if(item.key=='period'){
					uni.navigateTo({
						url:'/pages/index/period'
					})
				}
			},
			goTodayStep() {
				uni.navigateTo({
					url: '/pages/index/todayStep'
				})
			},
			goNight(){
				uni.navigateTo({
					url: '/pages/index/yesterdaynight'
				})
			},
			goHeart(){
				uni.navigateTo({
					url:'/pages/index/heartRate'
				})
			},
			goBoold(){
				uni.navigateTo({
					url:'/pages/index/boold'
				})
			},
			goHrv(){
				uni.navigateTo({
					url:'/pages/index/hrv'
				})
			},
			goStress(){
				uni.navigateTo({
					url:'/pages/index/stress'
				})
			},
			openManagePopup() {
				this.showManagePopup = true
			},
			closeManagePopup() {
				this.showManagePopup = false
			},
			confirmManagePopup() {
				this.showManagePopup = false
			},
			openButlerPopup() {
				this.showButlerPopup = true
			},
			closeButlerPopup() {
				this.showButlerPopup = false
			},
			copyButlerPhone() {
				uni.setClipboardData({
					data: this.butlerPhone,
					success: () => {
						uni.showToast({
							title: '已复制',
							icon: 'none'
						})
					}
				})
			},
			onToggleChange(index, e) {
				this.$set(this.recordList[index], 'visible', e.detail.value)
			}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		min-height: 100%;
		padding: 0 28rpx 40rpx;
		box-sizing: border-box;
		padding-bottom: 140rpx;
		background: #000000;
		color: #ffffff;
	}

	.header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		padding-bottom: 28rpx;
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
		width: 100%;
		height: 300rpx;

		image {
			width: 100%;
			height: 100%;
		}
	}

	.indexkefu {
		width: 100%;
		height: 140rpx;
		margin-top: 36rpx;
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
		width: 334rpx;
		margin-bottom: 28rpx;
		padding: 24rpx;
		border-radius: 24rpx;
		background: #1A1B25;
		box-sizing: border-box;
		overflow: hidden;
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
		background: rgba(44, 137, 255, 0.1);
		overflow: hidden;
	}

	.progress-inner {
		height: 100%;
		border-radius: 5rpx;
		background: linear-gradient(180deg, #54B7F7 0%, #1A53EC 100%);
	}

	.sleep-bars {
		display: flex;
		align-items: flex-start;
		margin-top: 20rpx;
		height: 44rpx;
	}

	.sleep-empty {
		margin-top: 20rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.sleep-bar {
		height: 100%;
		margin-right: 6rpx;
		display: flex;
		flex-direction: column;
	}

	.sleep-bar-solid {
		width: 100%;
		height: 12rpx;
		flex-shrink: 0;
	}

	.sleep-bar-glow {
		width: 100%;
		flex: 1;
	}

	.sleep-bar.orange {
		width: 28%;
	}

	.sleep-bar.orange .sleep-bar-solid {
		background: #ff9f0a;
		border-top-left-radius: 30rpx;
		border-bottom-left-radius: 30rpx;
	}

	.sleep-bar.orange .sleep-bar-glow {
		background: linear-gradient(180deg, rgba(255, 159, 10, 0.55) 0%, rgba(255, 159, 10, 0) 100%);
	}

	.sleep-bar.purple {
		width: 40%;
	}

	.sleep-bar.purple .sleep-bar-solid {
		background: #bf5af2;
	}

	.sleep-bar.purple .sleep-bar-glow {
		background: linear-gradient(180deg, rgba(191, 90, 242, 0.55) 0%, rgba(191, 90, 242, 0) 100%);
	}

	.sleep-bar.cyan {
		width: 24%;
		margin-right: 0;
	}

	.sleep-bar.cyan .sleep-bar-solid {
		background: #64d2ff;
		border-top-right-radius: 30rpx;
		border-bottom-right-radius: 30rpx;
	}

	.sleep-bar.cyan .sleep-bar-glow {
		background: linear-gradient(180deg, rgba(100, 210, 255, 0.55) 0%, rgba(100, 210, 255, 0) 100%);
	}

	.metric-body {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		margin-top: 8rpx;
		overflow: hidden;
	}

	.metric-left {
		flex-shrink: 0;
		z-index: 1;
	}

	.metric-chart {
		width: 168rpx;
		flex-shrink: 0;
		margin-left: 8rpx;
		display: flex;
		flex-direction: column;
		align-items: stretch;
		overflow: hidden;
	}

	.chart-box {
		width: 168rpx;
		height: 88rpx;
		overflow: hidden;
		position: relative;
	}

	.chart-box ::v-deep .chartsview {
		width: 100% !important;
		height: 100% !important;
		flex: none !important;
	}

	.chart-axis-line {
		width: 100%;
		height: 0;
		margin-top: 2rpx;
		border-bottom: 1px dashed rgba(142, 142, 147, 0.5);
	}

	.chart-time {
		display: flex;
		justify-content: space-between;
		margin-top: 6rpx;
		font-size: 18rpx;
		color: #8e8e93;
		line-height: 24rpx;
	}

	.record-grid {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		margin-top: 20rpx;
	}
	.reTop{
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.record-card {
		width: calc(50% - 10rpx);
		margin-bottom: 20rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		background: #1A1B25;
		box-sizing: border-box;
	}

	.record-icon {
		width: 43rpx;
		height: 43rpx;
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
		font-size: 36rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 40rpx;
		margin-top: 20rpx;
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

	.butler-mask {
		align-items: flex-end;
	}

	.butler-sheet {
		position: relative;
		width: 100%;
		border-radius: 40rpx;
		overflow: hidden;
		background: #1A1B25;
		margin: 30rpx;
	}

	.butler-content {
		position: relative;
		z-index: 1;
		padding: 48rpx 40rpx 44rpx;
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.butler-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 50rpx;
	}

	.butler-qr-wrap {
		margin-top: 40rpx;
		width: 356rpx;
		height: 353.32rpx;
		border: 2rpx dashed rgba(255, 255, 255, 0.20);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.butler-qr {
		width: 282rpx;
		height: 282rpx;
	}

	.butler-tip {
		margin-top: 28rpx;
		font-size: 26rpx;
		color: #4da3ff;
		line-height: 36rpx;
	}

	.butler-footer {
		margin-top: 48rpx;
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.butler-phone {
		display: flex;
		flex-direction: column;
	}

	.butler-phone-label {
		font-size: 24rpx;
		color: #8e8e93;
		line-height: 34rpx;
	}

	.butler-phone-num {
		margin-top: 8rpx;
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 50rpx;
	}

	.butler-copy-btn {
		width:172rpx;
		height: 64rpx;
	}
</style>