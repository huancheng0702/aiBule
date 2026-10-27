<template>
	<view class="tab-page">
		<scroll-view class="tab-scroll" scroll-y :refresher-enabled="true" :refresher-triggered="isRefreshing"
			refresher-default-style="white" refresher-background="#000000" @refresherrefresh="onRefresh">
		<!-- 顶部问候：paddingTop 用系统状态栏高度适配不同机型 -->
		<view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="header-left">
				<text class="greet">{{ greetText }}</text>
				<text class="greet-sub">今天也要关心自己</text>
			</view>
			<view class="battery">
				<image class="battery-icon" src="/static/lvdian.png" mode="aspectFit"></image>
				<text class="battery-text">{{ batteryText }}</text>
			</view>
		</view>

		<!-- 智能戒指横幅 -->
		<view class="hero-banner">
			<swiper v-if="banners.length" class="hero-swiper" circular autoplay interval="4000" indicator-dots>
				<swiper-item v-for="item in banners" :key="item.bannerId" @click="onBanner(item)">
					<image class="hero-img" :src="item.imageUrl" mode=""></image>
				</swiper-item>
			</swiper>
			<image v-else src="/static/swiper.png" class="hero-img" mode="aspectFill"></image>
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
				<view class="status-card" @click="goHeart" style="height: 152rpx;">
					<view class="card-head">
						<image class="card-icon" src="/static/xinlv.png" mode="aspectFit"></image>
						<text class="card-label">心率</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">{{ heartRateValue }}</text>
								<text class="card-unit">{{ heartRateUnit }}</text>
							</view>
						</view>
						<view class="metric-chart">
							<view class="chart-box">
								<qiun-data-charts v-if="chartReady" type="area" canvasId="homeHeartRate"
									:opts="heartRateOpts" :chartData="heartRateData" :tooltipShow="false" :ontap="false"
									:animation="false" />
							</view>
						</view>
					</view>
				</view>

				<!-- 血氧 -->
				<view class="status-card" @click="goBoold" style="height: 152rpx;">
					<view class="card-head">
						<image class="card-icon" src="/static/xueyang.png" mode="aspectFit"></image>
						<text class="card-label">血氧</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">{{ bloodOxygenValue }}</text>
								<text class="card-unit">{{ bloodOxygenUnit }}</text>
							</view>
						</view>
						<view class="metric-chart">
							<view class="chart-box">
								<qiun-data-charts v-if="chartReady" type="column" canvasId="homeBloodOxygen"
									:opts="bloodOxygenOpts" :chartData="bloodOxygenData" :tooltipShow="false"
									:ontap="false" :animation="false" />
							</view>
						</view>
					</view>
				</view>

				<!-- HRV -->
				<view class="status-card" @click="goHrv" style="height: 152rpx;">
					<view class="card-head">
						<image class="card-icon" src="/static/hrv.png" mode="aspectFit"></image>
						<text class="card-label">HRV</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">{{ hrvValue }}</text>
								<text class="card-unit">{{ hrvUnit }}</text>
							</view>
						</view>
						<view class="metric-chart">
							<view class="chart-box">
								<qiun-data-charts v-if="chartReady" type="area" canvasId="homeHrv" :opts="hrvOpts"
									:chartData="hrvData" :tooltipShow="false" :ontap="false" :animation="false" />
							</view>
						</view>
					</view>
				</view>

				<!-- 压力 -->
				<view class="status-card" @click="goStress" style="height: 152rpx;">
					<view class="card-head">
						<image class="card-icon" src="/static/yali.png" mode="aspectFit"></image>
						<text class="card-label">压力</text>
					</view>
					<view class="metric-body">
						<view class="metric-left">
							<view class="card-value-row">
								<text class="card-value">{{ stressValue }}</text>
								<text class="card-unit">{{ stressUnit }}</text>
							</view>
						</view>
						<view class="metric-chart">
							<view class="chart-box">
								<qiun-data-charts v-if="chartReady" type="column" canvasId="homeStress"
									:opts="stressOpts" :chartData="stressData" :tooltipShow="false" :ontap="false"
									:animation="false" />
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
						<text class="record-unit"> {{item.unit}}</text>
					</view>
				</view>
			</view>
		</view>
		<view class="spaceemty">
			
		</view>

		</scroll-view>

		<!-- 管理显示弹窗 -->
		<view v-if="showManagePopup" class="popup-mask" @click="closeManagePopup">
			<view class="popup-sheet" @click.stop>
				<!-- 背景图，自行补充 src -->
				<image class="popup-bg" src="/static/bushuPopBg.png" mode=""></image>
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
							<switch :checked="item.visible" :class="{ ischeck: !item.visible }" color="#2C89FF" @change="onToggleChange(index, $event)" />
							
						</view>
					</view>
					<view class="popup-btn" @click="confirmManagePopup">
						<image class="popup-btn-bg" src="/static/btnBan.png" mode=""></image>
						<text class="popup-btn-text">完成</text>
					</view>

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
						<image class="butler-qr" :src="butlerQrcode" mode="aspectFit"></image>
					</view>
					<text class="butler-tip">微信扫码添加专属健康管家</text>
					<view class="butler-footer">
						<view class="butler-phone">
							<text class="butler-phone-label">管家联系电话</text>
							<text class="butler-phone-num">{{ butlerPhone }}</text>
						</view>
						<image src="/static/copyPhone.png" @click="copyButlerPhone" class="butler-copy-btn" mode="">
						</image>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import qiunDataCharts from '@/uni_modules/qiun-data-charts/components/qiun-data-charts/qiun-data-charts.vue'
	import BleService from '@/common/ble-service.js'
	import {
		getDeviceDashboard
	} from '@/common/api/deviceData.js'
	import {
		getBannerList
	} from '@/common/api/banner.js'
	import {
		getHealthManager
	} from '@/common/api/config.js'
	import {
		getHomeDisplay,
		updateHomeDisplay,
		getManualHealthList
	} from '@/common/api/manualHealth.js'
	import {
		getPeriodStatistics
	} from '@/common/api/period.js'
	import {
		getUserInfo
	} from '@/common/api/user.js'
	import {
		saveLoginSession
	} from '@/common/auth.js'

	const RECORD_META = {
		'1': {
			key: 'weight',
			suffix: ' kg'
		},
		'2': {
			key: 'sugar',
			suffix: ' mmol/L'
		},
		'3': {
			key: 'temp',
			suffix: ' ℃'
		},
		'4': {
			key: 'bp',
			suffix: ' mmHg'
		},
		'5': {
			key: 'period',
			suffix: ''
		}
	}
	const STRESS_COLORS = ['#FF9F0A', '#BF5AF2', '#64D2FF', '#0A84FF', '#5E5CE6', '#FF6B6B']

	function pickDashItem(items, code) {
		const list = Array.isArray(items) ? items : []
		for (let i = 0; i < list.length; i++) {
			if (list[i] && list[i].code === code) return list[i]
		}
		return null
	}

	function timeAxisCategories(len) {
		const n = Math.max(Number(len) || 0, 2)
		const cats = []
		for (let i = 0; i < n; i++) {
			cats.push('')
		}
		cats[0] = '00:00'
		cats[n - 1] = '24:00'
		return cats
	}

	function toChart(series, name, fallbackLen) {
		const list = Array.isArray(series) ? series : []
		if (!list.length) {
			return {
				categories: timeAxisCategories(fallbackLen || 8),
				series: [{
					name: name,
					data: []
				}]
			}
		}
		return {
			categories: timeAxisCategories(list.length),
			series: [{
				name: name,
				data: list.map((p) => Number(p && p.value) || 0)
			}]
		}
	}

	function toStressChart(series) {
		const list = Array.isArray(series) ? series : []
		if (!list.length) {
			return {
				categories: timeAxisCategories(6),
				series: [{
					name: '压力',
					data: []
				}]
			}
		}
		return {
			categories: timeAxisCategories(list.length),
			series: [{
				name: '压力',
				data: list.map((p, i) => ({
					value: Number(p && p.value) || 0,
					color: STRESS_COLORS[i % STRESS_COLORS.length]
				}))
			}]
		}
	}

	function displayValue(item) {
		if (!item) return '--'
		const v = item.value
		if (v === 0 || v === '0') return '0'
		if (v === undefined || v === null || v === '') return '--'
		return String(v)
	}

	function parseSleepValue(item) {
		if (!item) return {
			hours: 0,
			minutes: 0
		}
		const text = String(item.value || '')
		const hourMatch = text.match(/(\d+)\s*小时/)
		const minMatch = text.match(/(\d+)\s*分/)
		if (hourMatch || minMatch) {
			return {
				hours: hourMatch ? Number(hourMatch[1]) : 0,
				minutes: minMatch ? Number(minMatch[1]) : 0
			}
		}
		const num = Number(item.value)
		if (!isNaN(num) && num > 0) {
			if ((item.unit || '') === '小时' || num <= 24) {
				const hours = Math.floor(num)
				const minutes = Math.round((num - hours) * 60)
				return {
					hours,
					minutes
				}
			}
			const hours = Math.floor(num / 60)
			const minutes = Math.round(num % 60)
			return {
				hours,
				minutes
			}
		}
		return {
			hours: 0,
			minutes: 0
		}
	}

	function createChartOpts(extra = {}) {
		return {
			color: extra.color || [],
			padding: extra.padding || [10, 10, 1, 4],
			legend: {
				show: false  
			},
			dataLabel: false,
			dataPointShape: false,
			enableScroll: false,
			xAxis: {
				disabled: false,
				disableGrid: true,
				axisLine: true,
				axisLineColor: 'rgba(142,142,147,0.45)',
				fontSize: 8,
				fontColor: '#8E8E93',
				marginTop: 2,
				calibration: false,
				boundaryGap: 'center',
				labelOffset: 0,
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
				isRefreshing: false,
				batteryLevel: null,
				batteryCharging: false,
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
				butlerQrcode: '',
				banners: [],
				nickName: '',
				heartRateValue: '--',
				heartRateUnit: '次/分',
				bloodOxygenValue: '--',
				bloodOxygenUnit: '%',
				hrvValue: '--',
				hrvUnit: 'ms',
				stressValue: '--',
				stressUnit: '',
				chartReady: false,
				heartRateData: {
					categories: timeAxisCategories(24),
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
					categories: timeAxisCategories(13),
					series: [{
						name: '血氧',
						data: [62, 78, 55, 88, 70, 92, 48, 85, 66, 90, 58, 80, 72, ]
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
					categories: timeAxisCategories(24),
					series: [{
						name: 'HRV',
						data: [35, 48, 72, 88, 70, 52, 40, 55, 78, 92, 80, 58, 42, 50, 68, 85, 75, 55, 38, 48, 62,
							70, 58, 45
						]
					}]
				},
				hrvOpts: createChartOpts({
					color: ['#E1A2FF'],
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
					categories: timeAxisCategories(6),
					series: [{
						name: '压力',
						data: [{
								value: 88,
								color: '#FF9F0A'
							},
							{
								value: 52,
								color: '#BF5AF2'
							},
							{
								value: 78,
								color: '#64D2FF'
							},
							{
								value: 92,
								color: '#0A84FF'
							},
							{
								value: 48,
								color: '#5E5CE6'
							},
							{
								value: 70,
								color: '#FF6B6B'
							}
						]
					}]
				},
				stressOpts: createChartOpts({
					color: ['#FF9F0A'],
					extra: {
						column: {
							type: 'group',
							width:80,
							categoryGap: 4,
							barBorderCircle: true,
							activeBgColor: 'transparent',
							activeBgOpacity: 0
						}
					}
				}),
				recordList: [{
						type: '1',
						key: 'weight',
						manageLabel: '体重',
						homeLabel: '体重',
						value: '',
						unit: '',
						visible: true,
						icon: '/static/tizhong.png'
					},
					{
						type: '2',
						key: 'sugar',
						manageLabel: '血糖',
						homeLabel: '血糖',
						value: '',
						visible: true,
						icon: '/static/xuetang.png'
					},
					{
						type: '3',
						key: 'temp',
						manageLabel: '体温',
						homeLabel: '体温',
						value: '',
						visible: true,
						icon: '/static/tiwen.png'
					},
					{
						type: '4',
						key: 'bp',
						manageLabel: '血压',
						homeLabel: '血压',
						value: '',
						visible: true,
						icon: '/static/xueya.png'
					},
					{
						type: '5',
						key: 'period',
						manageLabel: '经期',
						homeLabel: '生理周期',
						value: '',
						visible: true,
						icon: '/static/jingqi.png'
					}
				]
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight + 10 || 20
			this.nickName = uni.getStorageSync('nickName') || ''
			this.unsubBle = BleService.subscribe((s) => {
				this.batteryLevel = s.batteryLevel
				this.batteryCharging = !!s.batteryCharging
				this.onBleSportUploaded(s && s.sport)
			})
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
					return this.batteryCharging ?
						this.batteryLevel + '% 充电中' :
						this.batteryLevel + '%'
				}
				if (this.batteryCharging) return '充电中'
				return '--'
			},
			greetText() {
				const hour = new Date().getHours()
				let text = '晚上好'
				if (hour < 6) text = '夜深了'
				else if (hour < 12) text = '早上好'
				else if (hour < 14) text = '中午好'
				else if (hour < 18) text = '下午好'
				return text
			},
			stepText() {
				const n = Number(this.stepCurrent) || 0
				return n.toLocaleString()
			},
			stepSubText() {
				const km = Number(this.distanceCurrent) || 0
				const kcal = Number(this.calorieCurrent) || 0
				const kmText = km >= 100 ? Math.round(km).toString() : (Math.round(km * 10) / 10).toString()
				return `${kmText}km · ${Math.round(kcal)}kcal`
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
				return this.sleepScore ? `得分 ${this.sleepScore}` : ''
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
			onBleSportUploaded(s) {
				if (!s) return
				const key = [s.steps, s.distanceM, s.calories, s.updatedAt].join('|')
				if (key === this._lastBleSportKey) return
				this._lastBleSportKey = key
				clearTimeout(this._bleReloadTimer)
				this._bleReloadTimer = setTimeout(() => { this.loadDashboard() }, 1500)
			},
			async onRefresh() {
				this.isRefreshing = true
				try {
					await this.loadHome()
					BleService.syncSport && BleService.syncSport().catch(() => {})
					BleService.syncSleep && BleService.syncSleep().catch(() => {})
				} catch (e) {}
				this.isRefreshing = false
			},
			onTabShow() {
				this.loadHome()
				BleService.syncSport && BleService.syncSport().catch(() => {})
				BleService.syncSleep && BleService.syncSleep().catch(() => {})
			},
			async loadHome() {
				await Promise.all([
					this.loadDashboard(),
					this.loadBanners(),
					this.loadManualRecords(),
					this.loadUser()
				])
			},
			async loadDashboard() {
				try {
					const data = await getDeviceDashboard()
					this.applyDashboard(data)
				} catch (e) {}
			},
			applyDashboard(data) {
				const items = (data && data.items) || []
				const steps = pickDashItem(items, 'steps')
				if (steps) {
					if (steps.value !== undefined && steps.value !== null && steps.value !== '') this.stepCurrent = Number(steps.value) || 0
					if (Number(steps.target) > 0) this.stepTarget = Number(steps.target)
					else this.stepTarget = 10000
					if (steps.calories !== undefined && steps.calories !== null) this.calorieCurrent = Number(steps.calories) || 0
					if (steps.distance !== undefined && steps.distance !== null) this.distanceCurrent = Number(steps.distance) || 0
				}
				const sleep = pickDashItem(items, 'sleep')
				if (sleep) {
					const parsed = parseSleepValue(sleep)
					this.sleepHours = parsed.hours
					this.sleepMinutes = parsed.minutes
					this.sleepScore = Number(sleep.status) || Number((sleep.subText || '').replace(/\D/g, '')) || 0
					const segs = sleep.segments || []
					this.sleepDeepRatio = 0
					this.sleepLightRatio = 0
					this.sleepRemRatio = 0
					segs.forEach((seg) => {
						const name = String(seg.name || '')
						const percent = Number(seg.percent)
						const ratio = isNaN(percent) ? 0 : percent / 100
						if (name.indexOf('深') !== -1) this.sleepDeepRatio = ratio
						else if (name.indexOf('浅') !== -1) this.sleepLightRatio = ratio
						else if (name.indexOf('眼动') !== -1 || name.toLowerCase().indexOf('rem') !== -1) this
							.sleepRemRatio = ratio
					})
				}
				const heart = pickDashItem(items, 'heart_rate')
				if (heart) {
					this.heartRateValue = displayValue(heart)
					this.heartRateUnit = heart.unit || '次/分'
					this.heartRateData = toChart(heart.series, '心率', 24)
				}
				const spo2 = pickDashItem(items, 'blood_oxygen')
				if (spo2) {
					this.bloodOxygenValue = displayValue(spo2)
					this.bloodOxygenUnit = spo2.unit || '%'
					this.bloodOxygenData = toChart(spo2.series, '血氧', 15)
				}
				const hrv = pickDashItem(items, 'hrv')
				if (hrv) {
					this.hrvValue = displayValue(hrv)
					this.hrvUnit = hrv.unit || 'ms'
					this.hrvData = toChart(hrv.series, 'HRV', 24)
				}
				const stress = pickDashItem(items, 'stress')
				if (stress) {
					this.stressValue = displayValue(stress)
					this.stressUnit = stress.unit || ''
					this.stressData = toStressChart(stress.series)
				}
			},
			async loadBanners() {
				try {
					const list = await getBannerList()
					this.banners = Array.isArray(list) ? list : []
				} catch (e) {}
			},
			async loadManualRecords() {
				try {
					const display = await getHomeDisplay()
					const rows = Array.isArray(display) ? display : []
					rows.forEach((row) => {
						const rec = this.recordList.find((item) => item.type === String(row.type))
						if (!rec) return
						this.$set(rec, 'visible', String(row.isShow) !== '0')
						if (row.name) rec.manageLabel = row.name
					})
				} catch (e) {}
				try {
					const list = await getManualHealthList()
					const rows = Array.isArray(list) ? list : []
					const latest = {}
					rows.forEach((row) => {
						const type = String(row.type)
						const prev = latest[type]
						const time = new Date(row.recordTime || row.createTime || 0).getTime()
						if (!prev || time >= prev._t) {
							latest[type] = Object.assign({}, row, {
								_t: time
							})
						}
					})
					this.recordList.forEach((rec) => {
						if (rec.type === '5') return
						const row = latest[rec.type]
						const meta = RECORD_META[rec.type]
						if (!row || !row.value) {
							rec.value = ''
							rec.unit = ''
							return
						}
						const unit = row.unit ? (' ' + row.unit) : (meta && meta.suffix) || ''
						rec.value = String(row.value)
						rec.unit = String(unit).trim()
					})
				} catch (e) {}
				try {
					const stats = await getPeriodStatistics()
					const rec = this.recordList.find((item) => item.type === '5')
					if (!rec || !stats) return
					rec.unit = ''
					if (Number(stats.periodDay) > 0) {
						rec.value = '经期第' + stats.periodDay + '天'
					} else if (stats.nextStartDate) {
						rec.value = '下次 ' + String(stats.nextStartDate).slice(5)
					} else {
						rec.value = ''
					}
				} catch (e) {}
			},
			async loadUser() {
				try {
					const info = await getUserInfo()
					if (info) {
						saveLoginSession(info)
						this.nickName = info.nickName || info.nickname || this.nickName
					}
				} catch (e) {}
			},
			onBanner(item) {
				const url = item && item.linkUrl
				if (!url) return
				// #ifdef H5
				window.open(url)
				// #endif
				// #ifdef APP-PLUS
				plus.runtime.openURL(url)
				// #endif
			},
			goDetail(item) {
				if (item.key == 'weight') {
					uni.navigateTo({
						url: '/pages/index/weight'
					})
				}
				if (item.key == 'sugar') {
					uni.navigateTo({
						url: '/pages/index/sugar'
					})
				}
				if (item.key == 'temp') {
					uni.navigateTo({
						url: '/pages/index/temperature'
					})
				}
				if (item.key == 'bp') {
					uni.navigateTo({
						url: '/pages/index/pressure'
					})
				}
				if (item.key == 'period') {
					uni.navigateTo({
						url: '/pages/index/period'
					})
				}
			},
			goTodayStep() {
				uni.navigateTo({
					url: '/pages/index/todayStep'
				})
			},
			goNight() {
				uni.navigateTo({
					url: '/pages/index/yesterdaynight'
				})
			},
			goHeart() {
				uni.navigateTo({
					url: '/pages/index/heartRate'
				})
			},
			goBoold() {
				uni.navigateTo({
					url: '/pages/index/boold'
				})
			},
			goHrv() {
				uni.navigateTo({
					url: '/pages/index/hrv'
				})
			},
			goStress() {
				uni.navigateTo({
					url: '/pages/index/stress'
				})
			},
			openManagePopup() {
				this.recordList.forEach((item) => {
					this.$set(item, '_origVisible', item.visible)
				})
				this.showManagePopup = true
			},
			closeManagePopup() {
				this.showManagePopup = false
			},
			async confirmManagePopup() {
				this.showManagePopup = false
				for (let i = 0; i < this.recordList.length; i++) {
					const item = this.recordList[i]
					if (item.visible === item._origVisible) continue
					try {
						await updateHomeDisplay({
							type: item.type,
							isShow: item.visible ? '1' : '0'
						})
					} catch (e) {}
				}
			},
			openButlerPopup() {
				this.showButlerPopup = true
				this.loadHealthManager()
			},
			async loadHealthManager() {
				try {
					const info = await getHealthManager()
					if (!info) return
					if (info.phone) this.butlerPhone = info.phone
					if (info.qrcode) this.butlerQrcode = info.qrcode
				} catch (e) {}
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
				console.log(e)
				this.$set(this.recordList[index], 'visible', e.detail.value)
			}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		height: 100%;
		overflow: hidden;
		padding: 0;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.tab-scroll {
		height: 100%;
		padding: 0 28rpx 0;
		box-sizing: border-box;
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
		overflow: hidden;
		border-radius: 24rpx;

		.hero-swiper,
		.hero-img {
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
		padding: 20rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
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
		font-size: 36rpx;
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
		color: rgba(255, 255, 255, 0.65);
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
		align-items: stretch;
		justify-content: space-between;
		margin-top: 8rpx;
	}

	.metric-left {
		flex-shrink: 0;
		z-index: 1;
	}

	.metric-chart {
		width: 152rpx;
		height: 140rpx;
		margin-left: 8rpx;
		position: relative;
		top: -30rpx;
		flex-shrink: 0;
		overflow: visible;
	}

	.chart-box {
		width: 100%;
		height: 100%;
		position: relative;
		top: -30rpx;
	}

	.chart-box ::v-deep .chartsview {
		width: 100% !important;
		height: 100% !important;
	}

	.record-grid {
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
		margin-top: 20rpx;
	}

	.reTop {
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.record-card {
		width: 48%;
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
		align-items: baseline;
		flex-wrap: wrap;
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
		color: #FFFFFF;
	}

	.record-unit {
		margin-left: 10rpx;
		margin-top: 20rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.65);
		line-height: 40rpx;
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
		padding: 40rpx 32rpx 64rpx;
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
		padding: 20rpx;
		margin-bottom: 26rpx;
		box-sizing: border-box;
		height: 118rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1)
	}

	.popup-item:last-child {
		margin-bottom: 0;
	}

	.popup-item-left {
		display: flex;
		flex-direction: column;
	}

	.popup-item-title {
		font-size: 28rpx;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.80);
		line-height: 42rpx;
	}

	.popup-item-sub {
		margin-top: 6rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.80);
		line-height: 30rpx;
	}

	.popup-btn {
		position: relative;
		height: 96rpx;
		line-height: 96rpx;
		text-align: center;
		border-radius:28rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
	}

	.popup-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
	}

	.popup-btn-text {
		position: relative;
		z-index: 1;
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
		border: 2rpx solid rgba(44,137,255,0.1);
		border-radius: 30rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.butler-qr {
		width: 292rpx;
		height: 292rpx;
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
		width: 172rpx;
		height: 64rpx;
	}
	::v-deep uni-switch .uni-switch-input{
		height: 32rpx;
		width: 64rpx;
		background-color: #30313A;
		border: 1px solid #30313A;
	}
	::v-deep uni-switch .uni-switch-input:after{
		height: 28rpx;
		width: 28rpx;
		background-color: #000;
	}
	::v-deep uni-switch .uni-switch-input.uni-switch-input-checked:after{
		transform: translateX(30rpx);
	}
	::v-deep uni-switch .uni-switch-input:before{
		height: 32rpx;
		width: 64rpx;
		background-color: #30313A;
	}

	::v-deep .ischeck .uni-switch-wrapper .uni-switch-input:after{
		background-color: #2C89FF;
	}
	.spaceemty{
		width: 100%;
		height: 100px;
	}
</style>