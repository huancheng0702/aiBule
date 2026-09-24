<template>
	<view class="page">
		<!-- 顶部背景图，自行补充 src -->
		<image class="page-bg" src="/static/bodyTopBg.png" mode=""></image>

		<!-- 自定义导航栏 -->
		<view class="navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="navbar-inner">
				<view class="nav-back" @click="goBack">
					<!-- 返回图标，自行补充 src -->
					<image class="nav-back-icon" src="/static/leftBack.png" mode=""></image>
				</view>
				<text class="nav-title">健康画像</text>
				<view class="nav-right"></view>
			</view>
		</view>

		<view class="main" :style="{ paddingTop: statusBarHeight + 44 + 'px' }">
			<!-- 身体可视化区域 -->
			<view class="body-section">
				<scroll-view class="part-list" scroll-y :show-scrollbar="false">
					<view v-for="(item, index) in partList" :key="item" class="part-item"
						:class="{ active: activePart === index }" @click="activePart = index">
						<image v-if="activePart === index" class="part-item-bg" src="/static/selectBodyIcon.png"
							mode="aspectFit"></image>
						<text class="part-text">{{ item }}</text>
					</view>
				</scroll-view>

				<view class="body-visual">
					<image class="body-img" :src="bodyImgSrc" mode=""></image>
					<image class="body-base" src="/static/bodyBg.png" mode=""></image>

					<!-- 从上到下检测线 -->
					<view v-if="scanning" class="scan-band">
						<view class="scan-line"></view>
					</view>

					<!-- 检测完成后弹幕状态标签 -->
					<view v-if="showBubbles" class="status-bubble-layer">
						<view
							v-for="(item, index) in statusBubbles"
							:key="index"
							class="status-bubble"
							:style="{
								left: item.left,
								top: item.top,
								animationDelay: item.delay
							}"
						>
							<text class="status-bubble-text">{{ item.text }}</text>
						</view>
					</view>
				</view>
			</view> 

			<!-- 底部：Tab + 内容 -->
			<view class="bottom-panel">
				<view class="tab-bar">
					<view class="tab-item" :class="{ active: activeTab === 0 }" @click="activeTab = 0">
						<image v-if="activeTab === 0" class="tab-item-bg" src="/static/confirmBg.png"
							mode="scaleToFill"></image>
						<text class="tab-text">健康解读</text>
					</view>
					<view class="tab-item" :class="{ active: activeTab === 1 }" @click="activeTab = 1">
						<image v-if="activeTab === 1" class="tab-item-bg" src="/static/confirmBg.png"
							mode="scaleToFill"></image>
						<text class="tab-text">异常指标记录</text>
					</view>
				</view>

				<view v-if="activeTab === 0" class="history-row" @click="onHistory">
					<text class="history-text">历史/对比 </text>
					<image src="/static/rightLitterIcon.png" style="width: 34rpx;height: 34rpx;" mode=""></image>
				</view>

				<!-- 健康解读 -->
				<view v-if="activeTab === 0" class="summary-card">
					<view class="summary-left">
						<view class="summary-title-row">
							<view class="title-bar"></view>
							<text class="summary-title">{{ partList[activePart] }}健康解读</text>
						</view>
						<text class="summary-status">今日整体状态良好</text>
						<text class="summary-desc">活动、睡眠和心率数据均处于个人近期稳定范围</text>
					</view>
					<view class="score-wrap">
						<svg class="score-svg" viewBox="0 0 100 100">
							<defs>
								<linearGradient id="scoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
									<stop offset="0%" stop-color="#1A5CFF" />
									<stop offset="45%" stop-color="#3D9BFF" />
									<stop offset="100%" stop-color="#7AD0FF" />
								</linearGradient>
							</defs>
							<circle class="score-track" cx="50" cy="50" :r="scoreRing.r"
								:stroke-width="scoreRing.stroke" stroke="rgba(255,255,255,0.12)" />
							<circle class="score-progress" cx="50" cy="50" :r="scoreRing.r"
								:stroke-width="scoreRing.stroke" stroke="url(#scoreGrad)"
								:stroke-dasharray="scoreRing.dash" stroke-linecap="round" />
						</svg>
						<view class="score-center">
							<text class="score-num">{{ score }}</text>
							<text class="score-label">综合分</text>
						</view>
					</view>
				</view>

				<!-- 异常指标记录 -->
				<template v-else>
					<view class="panel-card">
						<view class="summary-title-row">
							<view class="title-bar"></view>
							<text class="summary-title">异常指标记录</text>
						</view>

						<!-- 日期选择，背景图参考 boold.vue -->
						<view class="date-bar">
							<image class="date-bar-bg" src="/static/selectDateBg.png" mode="scaleToFill"></image>
							<view class="date-btn" @click="onPrevRange">
								<image src="/static/arrLeft.png" mode="aspectFit"></image>
							</view>
							<view class="date-center">
								<text class="date-text">{{ dateRangeText }}</text>
							</view>
							<view class="date-btn" @click="onNextRange">
								<image src="/static/arrRight.png" mode="aspectFit"></image>
							</view>
						</view>

						<view class="empty-box">
							<!-- 空状态图，自行补充 src -->
							<image class="empty-img" src="/static/empty.png" mode=""></image>
						</view>
					</view>

					<view class="panel-card archive-card">
						<view class="summary-title-row">
							<view class="title-bar"></view>
							<text class="summary-title">健康档案</text>
						</view>
						<view class="archive-grid">
							<view v-for="item in archiveList" :key="item.label" class="archive-item"
								@click="onArchiveTap(item)">
								<view class="archive-info">
									<text class="archive-label">{{ item.label }}</text>
									<text class="archive-value">{{ item.value }}</text>
								</view>
								<image class="archive-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
							</view>
						</view>
					</view>
				</template>
			</view>
		</view>

		<!-- 历史/对比弹窗 -->
		<view v-if="showHistoryPopup" class="history-mask" @click="closeHistoryPopup">
			<view class="history-popup" @click.stop>
				<image class="history-popup-bg" src="/static/bushuPopBg.png" mode="aspectFill"></image>
				<view class="history-popup-body">
					<text class="history-popup-title">{{ partList[activePart] }} · 近7天</text>
					<text class="history-popup-desc">当前数据未发现明显异常变化</text>

					<view class="history-list">
						<view
							v-for="(item, index) in historyList"
							:key="index"
							class="history-item"
							:class="{ border: index < historyList.length - 1 }"
						>
							<text class="history-date">{{ item.date }}</text>
							<text class="history-status">{{ item.status }}</text>
							<text class="history-flag">{{ item.flag }}</text>
						</view>
					</view>

					<view class="history-back-btn" @click="closeHistoryPopup">
						<text class="history-back-text">返回画像</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 健康档案底部弹窗 -->
		<view v-if="showArchivePopup" class="archive-mask">
			<view class="archive-popup" :style="{ paddingTop: statusBarHeight + 'px' }" @click.stop>
				<view class="archive-popup-nav">
					<view class="archive-popup-back" @click="closeArchivePopup">
						<image class="archive-popup-back-icon" src="/static/leftBack.png" mode="aspectFit"></image>
					</view>
				</view>

				<view class="archive-popup-head">
					<text class="archive-popup-title">{{ archivePopup.title }}</text>
					<text class="archive-popup-desc">{{ archivePopup.desc }}</text>
					<view class="archive-other-btn" @click="onAddOtherTag">
						<text class="archive-other-text">{{ archivePopup.otherLabel }}</text>
					</view>
				</view>

				<scroll-view class="archive-tag-scroll" scroll-y :show-scrollbar="false">
					<view class="archive-tag-list">
						<view
							v-for="tag in archivePopup.options"
							:key="tag"
							class="archive-tag"
							:class="{ active: archiveSelected.indexOf(tag) > -1 }"
							@click="toggleArchiveTag(tag)"
						>
							<text class="archive-tag-text">{{ tag }}</text>
						</view>
					</view>
				</scroll-view>

				<view class="archive-confirm-wrap">
					<view class="archive-confirm-btn" @click="confirmArchivePopup">
						<text class="archive-confirm-text">确定</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	function ringDash(r, percent) {
		const c = 2 * Math.PI * r
		const p = Math.max(0, Math.min(1, percent))
		return `${c * p} ${c}`
	}

	function pad2(n) {
		return String(n).padStart(2, '0')
	}

	export default {
		data() {
			return {
				statusBarHeight: 20,
				activePart: 0,
				activeTab: 0,
				score: 83,
				rangeStart: null,
				partList: [
					'全身',
					'体征',
					'大脑',
					'甲状腺',
					'呼吸',
					'肺部',
					'心脏',
					'肝脏',
					'血管',
					'胃部',
					'胰腺',
					'肾脏',
					'其他'
				],
				// 全身 / 体征 / 其他用默认全身图，其余按拼音对应 bodyImg 目录
				partImgMap: {
					大脑: '/static/bodyImg/nanDanao.png',
					甲状腺: '/static/bodyImg/nanJiazhuang.png',
					呼吸: '/static/bodyImg/nanHuxi.png',
					肺部: '/static/bodyImg/nanfei.png',
					心脏: '/static/bodyImg/nanXin.png',
					肝脏: '/static/bodyImg/nanGan.png',
					血管: '/static/bodyImg/nanXue.png',
					胃部: '/static/bodyImg/nanWei.png',
					胰腺: '/static/bodyImg/nanYixian.png',
					肾脏: '/static/bodyImg/nanShen.png'
				},
				archiveList: [{
						label: '既往病史',
						value: '无',
						selected: []
					},
					{
						label: '家族病史',
						value: '无',
						selected: []
					},
					{
						label: '手术史',
						value: '无',
						selected: []
					},
					{
						label: '过敏史',
						value: '无',
						selected: []
					},
					{
						label: '用药史',
						value: '无',
						selected: []
					},
					{
						label: '个人史',
						value: '无',
						selected: []
					},
					{
						label: '婚育史',
						value: '无',
						selected: []
					},
					{
						label: '月经史',
						value: '无',
						selected: []
					},
					{
						label: '运动史',
						value: '无',
						selected: []
					}
				],
				showArchivePopup: false,
				showHistoryPopup: false,
				scanning: true,
				showBubbles: false,
				scanTimer: null,
				statusBubbles: [
					{ text: '体脂偏低', left: '58%', top: '42%', delay: '0s' },
					{ text: '心率正常', left: '8%', top: '28%', delay: '0.15s' },
					{ text: '睡眠良好', left: '62%', top: '22%', delay: '0.3s' },
					{ text: '压力平稳', left: '12%', top: '55%', delay: '0.45s' },
					{ text: '血氧正常', left: '55%', top: '62%', delay: '0.6s' }
				],
				historyList: [
					{ date: '今天', status: '整体良好', flag: '已更新' },
					{ date: '昨天', status: '状态平稳', flag: '完整' },
					{ date: '8月5日', status: '状态平稳', flag: '完整' }
				],
				archiveEditingIndex: -1,
				archiveSelected: [],
				archivePopupMap: {
					既往病史: {
						title: '既往病史',
						desc: '过往疾病，特别是涉及心脏、脑、肺、肾等重要器官',
						otherLabel: '其他疾病 +',
						options: [
							'脑血管疾病',
							'高血脂',
							'高血压',
							'高血糖',
							'糖尿病',
							'冠心病',
							'夜间鼾症',
							'心律失常',
							'房颤',
							'室性早搏',
							'房性早搏',
							'心慌气短',
							'心力衰竭',
							'甲状腺亢进',
							'肺动脉高血压',
							'肥胖（BMI大于28）',
							'心梗',
							'咽炎',
							'鼻炎',
							'脑卒中',
							'贫血',
							'慢性肾损伤',
							'肝功能不全'
						]
					},
					家族病史: {
						title: '家族病史',
						desc: '直系亲属常见疾病史，便于评估遗传风险',
						otherLabel: '其他病史 +',
						options: [
							'高血压',
							'糖尿病',
							'冠心病',
							'脑卒中',
							'恶性肿瘤',
							'高血脂',
							'哮喘',
							'肾病'
						]
					},
					手术史: {
						title: '手术史',
						desc: '既往手术情况，便于评估身体恢复与风险',
						otherLabel: '其他手术 +',
						options: [
							'心脏手术',
							'脑部手术',
							'骨科手术',
							'腹部手术',
							'眼科手术',
							'甲状腺手术',
							'妇科手术',
							'泌尿手术'
						]
					},
					过敏史: {
						title: '过敏史',
						desc: '药物、食物或其他过敏原情况',
						otherLabel: '其他过敏原 +',
						options: [
							'青霉素',
							'头孢类',
							'磺胺类',
							'阿司匹林',
							'海鲜',
							'花粉',
							'尘螨',
							'牛奶'
						]
					},
					用药史: {
						title: '用药史',
						desc: '长期或近期用药情况',
						otherLabel: '其他药物 +',
						options: [
							'降压药',
							'降糖药',
							'降脂药',
							'抗凝药',
							'激素类',
							'抗生素',
							'中成药',
							'保健品'
						]
					},
					个人史: {
						title: '个人史',
						desc: '生活习惯与个人健康相关情况',
						otherLabel: '其他情况 +',
						options: [
							'吸烟',
							'饮酒',
							'熬夜',
							'久坐',
							'精神压力大',
							'饮食不规律'
						]
					},
					婚育史: {
						title: '婚育史',
						desc: '婚姻与生育相关情况',
						otherLabel: '其他情况 +',
						options: [
							'未婚',
							'已婚',
							'已育',
							'未育',
							'备孕中',
							'妊娠期'
						]
					},
					月经史: {
						title: '月经史',
						desc: '月经周期与相关异常情况',
						otherLabel: '其他情况 +',
						options: [
							'周期规律',
							'周期不规律',
							'痛经',
							'月经量少',
							'月经量多',
							'已绝经'
						]
					},
					运动史: {
						title: '运动史',
						desc: '日常运动习惯与强度',
						otherLabel: '其他运动 +',
						options: [
							'几乎不运动',
							'偶尔运动',
							'规律有氧',
							'力量训练',
							'高强度训练',
							'康复训练'
						]
					}
				}
			}
		},
		computed: {
			bodyImgSrc() {
				const name = this.partList[this.activePart]
				return this.partImgMap[name] || '/static/bodyImg.png'
			},
			scoreRing() {
				const r = 40
				const stroke = 8
				return {
					r,
					stroke,
					dash: ringDash(r, this.score / 100)
				}
			},
			dateRangeText() {
				if (!this.rangeStart) return ''
				const start = this.rangeStart
				const end = new Date(start)
				end.setDate(start.getDate() + 6)
				const y = start.getFullYear()
				const sm = start.getMonth() + 1
				const sd = pad2(start.getDate())
				const em = pad2(end.getMonth() + 1)
				const ed = pad2(end.getDate())
				return `${y}年${sm}月${sd}日-${em}月${ed}日`
			},
			archivePopup() {
				const item = this.archiveList[this.archiveEditingIndex]
				if (!item) {
					return {
						title: '',
						desc: '',
						otherLabel: '其他 +',
						options: []
					}
				}
				const cfg = this.archivePopupMap[item.label] || {
					title: item.label,
					desc: '',
					otherLabel: '其他 +',
					options: []
				}
				const extras = this.archiveSelected.filter((t) => cfg.options.indexOf(t) === -1)
				return {
					...cfg,
					options: cfg.options.concat(extras)
				}
			}
		},
		onLoad() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
			// 默认展示包含今天的一周区间（往前 6 天）
			const end = new Date()
			const start = new Date(end)
			start.setDate(end.getDate() - 6)
			start.setHours(0, 0, 0, 0)
			this.rangeStart = start
			this.startBodyScan()
		},
		onUnload() {
			this.clearScanTimer()
		},
		methods: {
			clearScanTimer() {
				if (this.scanTimer) {
					clearTimeout(this.scanTimer)
					this.scanTimer = null
				}
			},
			startBodyScan() {
				this.clearScanTimer()
				this.scanning = true
				this.showBubbles = false
				// 与 CSS 检测线动画时长对齐
				this.scanTimer = setTimeout(() => {
					this.scanning = false
					this.showBubbles = true
					this.scanTimer = null
				}, 2000)
			},
			goBack() {
				const pages = getCurrentPages()
				if (pages.length > 1) {
					uni.navigateBack()
					return
				}
				uni.reLaunch({
					url: '/pages/index/index'
				})
			},
			onHistory() {
				this.showHistoryPopup = true
			},
			closeHistoryPopup() {
				this.showHistoryPopup = false
			},
			shiftRange(days) {
				const next = new Date(this.rangeStart)
				next.setDate(next.getDate() + days)
				this.rangeStart = next
			},
			onPrevRange() {
				this.shiftRange(-7)
			},
			onNextRange() {
				this.shiftRange(7)
			},
			onArchiveTap(item) {
				const index = this.archiveList.findIndex((row) => row.label === item.label)
				if (index < 0) return
				this.archiveEditingIndex = index
				this.archiveSelected = (this.archiveList[index].selected || []).slice()
				this.showArchivePopup = true
			},
			closeArchivePopup() {
				this.showArchivePopup = false
				this.archiveEditingIndex = -1
				this.archiveSelected = []
			},
			toggleArchiveTag(tag) {
				const idx = this.archiveSelected.indexOf(tag)
				if (idx > -1) {
					this.archiveSelected.splice(idx, 1)
				} else {
					this.archiveSelected.push(tag)
				}
			},
			onAddOtherTag() {
				uni.showModal({
					title: this.archivePopup.otherLabel.replace(/\s*\+\s*$/, ''),
					editable: true,
					placeholderText: '请输入内容',
					success: (res) => {
						if (!res.confirm) return
						const text = (res.content || '').trim()
						if (!text) return
						if (this.archiveSelected.indexOf(text) === -1) {
							this.archiveSelected.push(text)
						}
					}
				})
			},
			confirmArchivePopup() {
				const index = this.archiveEditingIndex
				if (index < 0) return
				const selected = this.archiveSelected.slice()
				const value = selected.length ? selected.join('、') : '无'
				this.$set(this.archiveList, index, {
					...this.archiveList[index],
					selected,
					value
				})
				this.closeArchivePopup()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		position: relative;
		min-height: 100vh;
		background: #050914;
		overflow-x: hidden;
	}

	.page-bg {
		position: absolute;
		right: 0;
		top: 0;
		width: 206.48px;
		height: 99.37px;
		z-index: 999;
	}

	.body-base {
		position: absolute;
		right: 0px;
		bottom: -257px;
		width: 312.48px;
		height: 736.7px;

	}

	.navbar {
		position: fixed;
		left: 0;
		top: 0;
		width: 100%;
		z-index: 100;
		background: transparent;
	}

	.navbar-inner {
		height: 88rpx;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.nav-back {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.nav-back-icon {
		width: 40rpx;
		height: 40rpx;
	}

	.nav-title {
		flex: 1;
		text-align: center;
		font-size: 34rpx;
		font-weight: 600;
		line-height: 48rpx;
		color: #ffffff;
	}

	.nav-right {
		width: 64rpx;
		height: 64rpx;
	}

	.main {
		position: relative;
		z-index: 1;
		padding: 0 28rpx 48rpx;
		box-sizing: border-box;
	}

	.body-section {
		display: flex;
		align-items: stretch;
		min-height: 720rpx;
		margin-top: 12rpx;
	}

	.part-list {
		width: 162rpx;
		height: 906rpx;
		padding: 16rpx 0;
		background: rgba(38, 39, 50, 0.85);
		border-radius: 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
		z-index: 999;
	}

	.part-item {
		position: relative;
		height: 72rpx;
		width: 100%;
		margin-bottom: 12rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.part-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.part-text {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.part-item.active .part-text {
		color: #4ea1ff;
		font-weight: 600;
	}

	.body-visual {
		position: relative;
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 720rpx;
	}

	.body-img {
		width: 516.46rpx;
		height: 458.83px;
		position: relative;
		z-index: 2;
	}

	.scan-band {
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		height: 96rpx;
		z-index: 8;
		pointer-events: none;
		background: linear-gradient(
			180deg,
			rgba(90, 170, 255, 0) 0%,
			rgba(90, 170, 255, 0.06) 45%,
			rgba(160, 210, 255, 0.22) 100%
		);
		animation: scanDown 2s ease-in-out forwards;
	}

	.scan-line {
		position: absolute;
		left: 10%;
		right: 10%;
		bottom: 0;
		height: 3rpx;
		border-radius: 3rpx;
		background: linear-gradient(
			90deg,
			rgba(255, 255, 255, 0) 0%,
			rgba(200, 230, 255, 0.55) 18%,
			#ffffff 50%,
			rgba(200, 230, 255, 0.55) 82%,
			rgba(255, 255, 255, 0) 100%
		);
		box-shadow:
			0 0 6rpx rgba(255, 255, 255, 0.85),
			0 0 14rpx rgba(120, 190, 255, 0.65),
			0 0 28rpx rgba(70, 150, 255, 0.35);
	}

	@keyframes scanDown {
		0% {
			top: 0;
			opacity: 0;
		}
		8% {
			opacity: 1;
		}
		92% {
			opacity: 1;
		}
		100% {
			top: 100%;
			opacity: 0;
		}
	}

	.status-bubble-layer {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 9;
		pointer-events: none;
	}

	.status-bubble {
		position: absolute;
		z-index: 9;
		padding: 10rpx 22rpx;
		border-radius: 28rpx;
		background: linear-gradient(90deg, #1a6dff 0%, #3d9bff 100%);
		box-shadow: 0 6rpx 18rpx rgba(40, 120, 255, 0.35);
		opacity: 0;
		animation: bubbleIn 0.55s ease-out forwards, bubbleFloat 2.8s ease-in-out 0.55s infinite;
	}

	.status-bubble-text {
		font-size: 22rpx;
		line-height: 32rpx;
		color: #ffffff;
		white-space: nowrap;
	}

	@keyframes bubbleIn {
		0% {
			opacity: 0;
			transform: translateX(24rpx) scale(0.9);
		}
		100% {
			opacity: 1;
			transform: translateX(0) scale(1);
		}
	}

	@keyframes bubbleFloat {
		0%,
		100% {
			transform: translateY(0);
		}
		50% {
			transform: translateY(-10rpx);
		}
	}

	.bottom-panel {
		margin-top: 76rpx;
		position: relative;
	}

	.tab-bar {
		display: flex;
		align-items: center;
		height: 88rpx;
		padding: 6rpx;
		border-radius: 20rpx;
		background: #2a2a2e;
		box-sizing: border-box;
	}

	.tab-item {
		position: relative;
		flex: 1;
		height: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 20rpx;
		overflow: hidden;
	}

	.tab-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.tab-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.tab-item.active .tab-text {
		color: #ffffff;
	}

	.history-row {
		margin-top: 20rpx;
		display: flex;
		justify-content: flex-end;
	}

	.history-text {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.summary-card {
		margin-top: 20rpx;
		padding: 32rpx 28rpx;
		border-radius: 24rpx;
		background: #1c1c1e;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.summary-left {
		flex: 1;
		padding-right: 20rpx;
		min-width: 0;
	}

	.summary-title-row {
		display: flex;
		align-items: center;
	}

	.title-bar {
		width: 6rpx;
		height: 28rpx;
		margin-right: 12rpx;
		border-radius: 4rpx;
		background: #3d9bff;
	}

	.summary-title {
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.summary-status {
		display: block;
		margin-top: 20rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #6eb6ff;
	}

	.summary-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.score-wrap {
		position: relative;
		width: 160rpx;
		height: 160rpx;
		flex-shrink: 0;
	}

	.score-svg {
		width: 160rpx;
		height: 160rpx;
		transform: rotate(-90deg);
	}

	.score-track,
	.score-progress {
		fill: none;
	}

	.score-center {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.score-num {
		font-size: 44rpx;
		font-weight: 700;
		line-height: 52rpx;
		color: #ffffff;
	}

	.score-label {
		margin-top: 2rpx;
		font-size: 20rpx;
		line-height: 28rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.panel-card {
		margin-top: 20rpx;
		padding: 32rpx 28rpx;
		border-radius: 24rpx;
		background: #1c1c1e;
		box-sizing: border-box;
	}

	.date-bar {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		height: 72rpx;
		margin-top: 24rpx;
		padding: 0 20rpx;
		box-sizing: border-box;
	}

	.date-bar-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.date-btn {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.date-btn image {
		width: 28rpx;
		height: 28rpx;
	}

	.date-center {
		position: relative;
		z-index: 1;
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.date-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: #ffffff;
	}

	.empty-box {
		margin-top: 40rpx;
		padding-bottom: 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.empty-img {
		width: 636rpx;
		height: 309.56rpx;
	}

	.empty-text {
		margin-top: 16rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.archive-card {
		margin-bottom: 20rpx;
	}

	.archive-grid {
		margin-top: 24rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.archive-item {
		width: 305rpx;
		height: 152rpx;
		background: rgba(255,255,255,0.05);
		margin-bottom: 28rpx;
		padding: 20rpx 24rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
	}

	.archive-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.archive-label {
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.archive-value {
		margin-top: 10rpx;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.archive-arrow {
		width: 24rpx;
		height: 24rpx;
		flex-shrink: 0;
		margin-left: 12rpx;
	}

	.history-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1900;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.history-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #1a1b25;
		animation: historySlideUp 0.28s ease-out;
	}

	@keyframes historySlideUp {
		from {
			transform: translateY(100%);
		}
		to {
			transform: translateY(0);
		}
	}

	.history-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.history-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.history-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.history-popup-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.history-list {
		margin-top: 28rpx;
		padding: 8rpx 28rpx;
		border-radius: 20rpx;
		background: rgba(42, 42, 46, 0.85);
		box-sizing: border-box;
	}

	.history-item {
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.history-item.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.history-date {
		width: 140rpx;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.history-status {
		flex: 1;
		text-align: center;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #5ec8e8;
	}

	.history-flag {
		width: 120rpx;
		text-align: right;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #3d9b6a;
	}

	.history-back-btn {
		margin-top: 36rpx;
		height: 96rpx;
		border-radius: 48rpx;
		background: linear-gradient(90deg, #1a6dff 0%, #3d9bff 50%, #1a6dff 100%);
		box-shadow: 0 8rpx 28rpx rgba(40, 120, 255, 0.35);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.history-back-text {
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.archive-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
	}

	.archive-popup {
		width: 100%;
		height: 100%;
		background: #0a0a0c;
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
		animation: archiveSlideUp 0.28s ease-out;
	}

	@keyframes archiveSlideUp {
		from {
			transform: translateY(100%);
		}
		to {
			transform: translateY(0);
		}
	}

	.archive-popup-nav {
		height: 88rpx;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.archive-popup-back {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}

	.archive-popup-back-icon {
		width: 40rpx;
		height: 40rpx;
	}

	.archive-popup-head {
		padding: 8rpx 32rpx 0;
		box-sizing: border-box;
	}

	.archive-popup-title {
		display: block;
		font-size: 48rpx;
		font-weight: 700;
		line-height: 66rpx;
		color: #ffffff;
	}

	.archive-popup-desc {
		display: block;
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.archive-other-btn {
		margin-top: 28rpx;
		width: 220rpx;
		height: 64rpx;
		border-radius: 32rpx;
		border: 2rpx solid #2f6fe0;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.archive-other-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: #4ea1ff;
	}

	.archive-tag-scroll {
		flex: 1;
		height: 0;
		margin-top: 32rpx;
		padding: 0 32rpx;
		box-sizing: border-box;
	}

	.archive-tag-list {
		display: flex;
		flex-wrap: wrap;
		padding-bottom: 24rpx;
	}

	.archive-tag {
		margin-right: 16rpx;
		margin-bottom: 20rpx;
		padding: 16rpx 28rpx;
		border-radius: 12rpx;
		background: #2a2a2e;
		box-sizing: border-box;
	}

	.archive-tag.active {
		background: #2f7bff;
	}

	.archive-tag-text {
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.archive-tag.active .archive-tag-text {
		color: #ffffff;
		font-weight: 600;
	}

	.archive-confirm-wrap {
		padding: 20rpx 32rpx 48rpx;
		box-sizing: border-box;
	}

	.archive-confirm-btn {
		height: 96rpx;
		border-radius: 48rpx;
		background: linear-gradient(90deg, #1a6dff 0%, #3d9bff 50%, #1a6dff 100%);
		box-shadow: 0 8rpx 28rpx rgba(40, 120, 255, 0.35);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.archive-confirm-text {
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}
</style>