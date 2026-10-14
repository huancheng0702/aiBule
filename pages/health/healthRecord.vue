<template>
	<view class="page">
		<!-- 顶部 Tab -->
		<view class="tab-bar">
			<view v-for="(item, index) in tabList" :key="item" class="tab-item" :class="{ active: activeTab == index }"
				@click="activeTab = index">
				<!-- 选中背景图，自行补充 src -->
				<image v-if="activeTab == index" class="tab-item-bg" src="/static/confirmBg.png" mode="scaleToFill">
				</image>
				<text class="tab-text">{{ item }}</text>
			</view>
		</view>

		<!-- 健康报告 -->
		<view v-if="activeTab == 0" class="panel">
			<!-- 今日健康日报 -->
			<view class="daily-card" @click="onDailyDetail">
				<!-- 卡片背景图，自行补充 src -->
				<image class="daily-card-bg" src="/static/recordBg.png" mode=""></image>
				<view class="daily-body">
					<image src="/static/yuanhuanBg.png" class="defsBg" mode=""></image>
					<view class="daily-left">
						<view class="daily-tag">
							<!-- 日报图标，自行补充 src -->
							<image class="daily-tag-icon" src="/static/ribaoImg.png" mode=""></image>
							<text class="daily-tag-text">今日健康日报</text>

						</view>
						<text class="daily-status">{{ dailyStatus }}</text>
						<text class="daily-desc">{{ dailyDesc }}</text>
						<view class="daily-link">
							<text class="daily-link-text">查看完整日报</text>
							<!-- 箭头图标，自行补充 src -->
							<image class="daily-link-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
						</view>
					</view>
					<view class="score-wrap">
						<canvas canvas-id="scoreCanvas" id="scoreCanvas" class="score-canvas"></canvas>
						<view class="score-center">
							<text class="score-num" :style="{ backgroundImage: `linear-gradient(to right, #FFFFFF 28%, ${scoreAccentColor} 100%)` }">{{ score }}</text>

							<text class="score-label">综合分</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 周报列表 -->
			<view v-for="(item, index) in weekList" :key="index" class="week-card" @click="onWeekDetail(item)">
				<view class="week-date">
					<text class="week-day">{{ item.day }}</text>
					<text class="week-month">{{ item.month }}</text>
				</view>
				<view class="week-info">
					<text class="week-title">{{ item.title }}</text>
					<text class="week-desc">{{ item.desc }}</text>
				</view>
				<!-- 箭头图标，自行补充 src -->
				<image class="week-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 异常记录 -->
		<view v-else-if="activeTab == 1" class="panel">
			<!-- 日期筛选：全部日期 / 单独日期 -->
			<view class="ab-filter">
				<view class="ab-filter-item" :class="{ active: abDateMode == 'all' }" @click="onAbDateMode('all')">
					<!-- 选中背景图，自行补充 src -->
					<image v-if="abDateMode == 'all'" class="ab-filter-item-bg" src="/static/dateSelectShiguang.png"
						mode="scaleToFill"></image>
					<text class="ab-filter-item-text">全部</text>
				</view>
				<view class="ab-filter-item" :class="{ active: abDateMode == 'day' }" @click="onAbDateMode('day')">
					<!-- 选中背景图，自行补充 src -->
					<image v-if="abDateMode == 'day'" class="ab-filter-item-bg" src="/static/dateSelectShiguang.png"
						mode="scaleToFill"></image>
					<text class="ab-filter-item-text">{{ abDateLabel }}</text>
				</view>
				<text class="ab-filter-range">{{ abRangeLabel }}</text>
			</view>

			<!-- 异常卡片列表 -->
			<view v-for="(item, index) in abList" :key="index" class="ab-card" @click="onAbDetail(item)">
				<!-- 卡片背景图，自行补充 src -->
				<image class="ab-card-bg" mode="aspectFill"></image>
				<view class="ab-card-body">
					<text class="ab-time">{{ item.time }}</text>
					<text class="ab-title">{{ item.title }}</text>
					<text class="ab-desc">{{ item.desc }}</text>

					<view class="ab-timeline">
						<view class="ab-timeline-left">
							<!-- 时间轴发光点，自行补充 src -->
							<image class="ab-dot" src="/static/faguangdian.png" mode=""></image>
							<view class="ab-line"></view>
						</view>
						<view class="ab-timeline-right">
							<view class="ab-chip">
								<!-- chip 背景图，自行补充 src -->
								<image class="ab-chip-bg" src="/static/shiguangzhouBg.png" mode=""></image>
								<text class="ab-chip-text">{{ abChipText }}</text>
							</view>
							<view class="ab-status">
								<!-- 状态块背景图，自行补充 src -->
								<image class="ab-status-bg" src="/static/shiguangzhouBgBig.png" mode=""></image>
								<view class="ab-status-body">
									<text class="ab-status-title">{{ item.statusTitle }}</text>
									<text class="ab-status-desc">{{ item.statusDesc }}</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 健康档案 -->
		<view v-else class="panel">
			<!-- 基本信息 -->
			<view class="archive-base" @click="onArchiveBaseTap">
				<view class="archive-base-info">
					<text class="archive-base-label">基本信息</text>
					<text class="archive-base-value">{{ archiveBaseText }}</text>
				</view>
				<!-- 箭头图标，自行补充 src -->
				<image class="archive-arrow" src="/static/arrRight.png" mode=""></image>
			</view>

			<!-- 病史网格 -->
			<view class="archive-grid">
				<view
					v-for="item in archiveList"
					:key="item.label"
					class="archive-item"
					@click="onArchiveTap(item)"
				>
					<view class="archive-info">
						<text class="archive-label">{{ item.label }}</text>
						<text class="archive-value">{{ item.value }}</text>

					</view>
					<!-- 箭头图标，自行补充 src -->
					<image class="archive-arrow" src="/static/arrRight.png" mode=""></image>
				</view>
			</view>
		</view>

		<!-- 本周健康周报弹窗 -->
		<view v-if="showWeekPopup" class="week-mask" @click="closeWeekPopup">
			<view class="week-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="week-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<scroll-view class="week-popup-scroll" scroll-y :show-scrollbar="false">
					<view class="week-popup-body">
						<view class="week-popup-head">
							<text class="week-popup-title">本周健康周报</text>
							<text class="week-popup-date">{{ weekPopup.dateText }}</text>
						</view>

						<view class="week-score-wrap">
							<canvas canvas-id="weekScoreCanvas" id="weekScoreCanvas" class="week-score-canvas"></canvas>
							<view class="week-score-center">
								<text class="week-score-num" :style="{ backgroundImage: `linear-gradient(to right, #FFFFFF 28%, ${weekAccentColor} 100%)` }">{{ weekPopup.score }}</text>
								<text class="week-score-label">综合分</text>
							</view>
						</view>

						<text class="week-popup-summary">{{ weekPopup.summary }}</text>

						<view class="metric-card">
							<view v-for="(item, index) in weekPopup.metrics" :key="item.name" class="metric-row"
								:class="{ border: index < weekPopup.metrics.length - 1 }">
								<!-- 指标图标，自行补充 src -->
								<image class="metric-icon" :src="item.icon" mode="aspectFit"></image>
								<view class="metric-info">
									<text class="metric-name">{{ item.name }}</text>
									<text class="metric-trend">{{ item.trend }}</text>
								</view>
								<text class="metric-value">{{ item.value }}</text>
							</view>
						</view>

						<view class="advice-card">
							<!-- 建议卡片背景图，自行补充 src -->
							<image class="advice-bg" src="/static/heartCardBg.png" mode="aspectFill"></image>
							<!-- 建议图标，自行补充 src -->
							<image class="advice-icon" src="/static/qushijianyi.png" mode="aspectFit"></image>
							<view class="advice-body">
								<text class="advice-title">健康与建议</text>
								<text class="advice-desc">{{ weekPopup.advice }}</text>
							</view>
						</view>

						<view class="week-popup-actions">
							<view class="week-btn outline" @click="onWeekShare">
								<text class="week-btn-text outline-text">分享/导出</text>
							</view>
							<view class="week-btn solid" @click="onWeekAskAi">
								<!-- 按钮背景图，自行补充 src -->
								<image class="week-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
								<text class="week-btn-text">继续问AI</text>
							</view>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- 异常记录详情弹窗 -->
		<view v-if="showAbPopup" class="ab-mask" @click="closeAbPopup">
			<view class="ab-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="ab-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="ab-popup-body">
					<text class="ab-popup-title">异常记录详情</text>
					<text class="ab-popup-sub">{{ abPopup.subText }}</text>

					<view class="ab-level">
						<!-- 关注等级背景图，自行补充 src -->
						<image class="ab-level-bg" src="/static/heartCardBg.png" mode=""></image>
						<!-- 关注等级图标，自行补充 src -->
						<image class="ab-level-icon" src="/static/dengjitu.png" mode="aspectFit"></image>
						<text class="ab-level-text">{{ abPopup.level }}</text>
					</view>

					<text class="ab-popup-headline">{{ abPopup.headline }}</text>

					<view class="ab-grid">
						<view v-for="(cell, index) in abPopup.cells" :key="index" class="ab-grid-item">
							<!-- 指标图标，自行补充 src -->
							<view class="" style="display: flex;justify-content: flex-start;align-items: center;">
								<image class="ab-grid-icon" :src="cell.icon" mode=""></image>
								<view class="ab-grid-label">{{ cell.label }}</view>
							</view>
					
							<view class="ab-grid-value">{{ cell.value }}</view>
						</view>
					</view>

					<view class="ab-popup-actions">
						<view class="ab-popup-btn outline" @click="closeAbPopup">
							<text class="ab-popup-btn-text outline-text">返回</text>
						</view>
						<view class="ab-popup-btn solid" @click="onAbAskAi">
							<!-- 按钮背景图，自行补充 src -->
							<image class="ab-popup-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
							<text class="ab-popup-btn-text">咨询AI助手</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 健康档案选择弹窗 -->
		<view v-if="showArchivePopup" class="archive-mask">
			<view class="archive-popup" :style="{ paddingTop: statusBarHeight + 'px' }" @click.stop>
				<view class="archive-popup-nav">
					<view class="archive-popup-back" @click="closeArchivePopup">
						<!-- 返回图标，自行补充 src -->
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
						<!-- 确定按钮背景图，自行补充 src -->
						<image class="archive-confirm-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
						<text class="archive-confirm-text">确定</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getDailyDetail, getWeeklyList, getWeeklyDetail } from '@/common/api/healthReport.js'
	import { getAbnormalList, getAbnormalDetail } from '@/common/api/healthAbnormal.js'
	import { getHealthArchiveDetail, getHealthArchiveOptions, saveHealthArchive } from '@/common/api/healthArchive.js'
	import { getUserInfo } from '@/common/api/user.js'
	import { exportAndDownloadReport } from '@/common/api/ai.js'

	const ARCHIVE_TYPE_MAP = ['既往病史', '家族病史', '手术史', '过敏史', '用药史', '个人史', '婚育史', '月经史', '运动史']
	function scoreStops(score) {
		const s = Number(score) || 0
		if (s >= 80) return ['#2C89FF', '#1D4FA0', '#12255E']
		if (s >= 50) return ['#FF9D2C', '#825434', '#39293A']
		return ['#E92A31', '#B52634', '#3B173A']
	}
	function scoreAccent(score) {
		const s = Number(score) || 0
		if (s >= 80) return '#3D9BFF'
		if (s >= 50) return '#FFDA20'
		return '#FF2C30'
	}
	function progressOpacity(t) {
		const p = Math.max(0, Math.min(1, t))
		if (p <= 0.58) {
			return 1 - (1 - 0.22) * (p / 0.58)
		}
		return 0.22 - (0.22 - 0.05) * ((p - 0.58) / 0.42)
	}

	function buildRingSegs(score, color) {
		const r = 38
		const stroke = 10
		const c = 2 * Math.PI * r
		const percent = Math.max(0, Math.min(1, score / 100))
		const progressLen = c * percent
		const count = Math.max(24, Math.round(120 * percent))
		const segLen = progressLen / count || 0
		const segs = []
		for (let i = 0; i < count; i++) {
			const t = (i + 0.5) / count
			const isEnd = i == 0 || i == count - 1
			segs.push({
				dash: `${segLen + 0.35} ${c}`,
				offset: -i * segLen,
				opacity: progressOpacity(t),
				cap: 'butt',
				color
			})
		}
		return {
			r,
			stroke,
			segs
		}
	}

	export default {
		data() {
			return {
				activeTab: 0,
				tabList: ['健康报告', '异常记录', '健康档案'],
				score: 21,
				dailyStatus: '恢复状态良好',
				dailyDesc: '睡眠与静息心率稳定，建议今天完成25分钟轻量活动。',
				weekList: [{
						day: '07',
						month: '8月',
						title: '本周健康周报',
						desc: '7项指标 · 已生成',
						dateText: '2025年8月1日-8月7日',
						score: 83,
						summary: '本周恢复表现稳定，运动时长较上周增加。',
						advice: '建议保持当前运动节奏，并尽量固定睡眠时间，进一步提升恢复质量。',
						metrics: [{
								name: '心率',
								trend: '较上周下降 2 bpm',
								value: '平均 71 Bpm',
								icon: '/static/xinlv.png'
							},
							{
								name: '睡眠',
								trend: '较上周提升 16 分钟',
								value: '平均 7小时18分',
								icon: '/static/shuimian.png'
							},
							{
								name: '血氧',
								trend: '与上周持平',
								value: '平均 98%',
								icon: '/static/xueyang.png'
							},
							{
								name: '压力',
								trend: '较上周下降 4',
								value: '平均 31',
								icon: '/static/yali.png'
							},
							{
								name: '运动',
								trend: '较上周提升 22 分钟',
								value: '186 分钟',
								icon: '/static/bushu.png'
							}
						]
					},
					{
						day: '31',
						month: '7月',
						title: '本周健康周报',
						desc: '7项指标 · 已生成',
						dateText: '2025年7月25日-7月31日',
						score: 79,
						summary: '本周整体状态平稳，睡眠略有波动。',
						advice: '建议保持当前运动节奏，并尽量固定睡眠时间，进一步提升恢复质量。',
						metrics: [{
								name: '心率',
								trend: '较上周下降 1 bpm',
								value: '平均 72 Bpm',
								icon: '/static/xinlv.png'
							},
							{
								name: '睡眠',
								trend: '较上周下降 10 分钟',
								value: '平均 7小时05分',
								icon: '/static/shuimian.png'
							},
							{
								name: '血氧',
								trend: '与上周持平',
								value: '平均 98%',
								icon: '/static/xueyang.png'
							},
							{
								name: '压力',
								trend: '较上周上升 2',
								value: '平均 33',
								icon: '/static/yali.png'
							},
							{
								name: '运动',
								trend: '较上周提升 12 分钟',
								value: '164 分钟',
								icon: '/static/bushu.png'
							}
						]
					}
				],
				showWeekPopup: false,
				exporting: false,
				weekPopup: {
					dateText: '',
					score: 83,
					summary: '',
					advice: '',
					metrics: []
				},
				abDateMode: 'all',
				abDateLabel: '08/09',
				abRangeLabel: '近30天',
				abList: [{
					time: '8月6日 23:18',
					title: '静息心率短时',
					desc: '持续 8 分钟，随后恢复个人正常区间。点击查看详情',
					statusTitle: '其余时段未发现异常',
					statusDesc: '继续佩戴可提高趋势判断完整度。',
					subText: '8月6日 23:18 · 静息心率',
					level: '轻度关注',
					headline: '静息心率短时偏高',
					cells: [{
							label: '检测值',
							value: '96 bpm',
							icon: '/static/jianceImg.png'
						},
						{
							label: '个人参考区间',
							value: '58-88 bpm',
							icon: '/static/cankaoqujian.png'
						},
						{
							label: '持续时间',
							value: '8 分钟',
							icon: '/static/chixushijian.png'
						},
						{
							label: '当前状态',
							value: '已恢复',
							icon: '/static/dangqianzhuangtai.png'
						}
					]
				}],
				showAbPopup: false,
				abPopup: {
					subText: '',
					level: '',
					headline: '',
					cells: []
				},
				statusBarHeight: 20,
				archiveBase: {
					gender: '女',
					age: 30,
					height: '165cm',
					weight: '52kg'
				},
				archiveList: [
					{ label: '既往病史', value: '无', selected: [] },
					{ label: '家族病史', value: '无', selected: [] },
					{ label: '手术史', value: '无', selected: [] },
					{ label: '过敏史', value: '无', selected: [] },
					{ label: '用药史', value: '无', selected: [] },
					{ label: '个人史', value: '无', selected: [] },
					{ label: '婚育史', value: '无', selected: [] },
					{ label: '月经史', value: '无', selected: [] },
					{ label: '运动史', value: '无', selected: [] }
				],
				showArchivePopup: false,
				archiveEditingIndex: -1,
				archiveSelected: [],
				archivePopupMap: {
					既往病史: {
						title: '既往病史',
						desc: '过往疾病，特别是涉及心脏、脑、肺、肾等重要器官',
						otherLabel: '其他疾病 +',
						options: [
							'脑血管疾病', '高血脂', '高血压', '高血糖', '糖尿病', '冠心病',
							'夜间鼾症', '心律失常', '房颤', '室性早搏', '房性早搏', '心慌气短',
							'心力衰竭', '甲状腺亢进', '肺动脉高血压', '肥胖（BMI大于28）',
							'心梗', '咽炎', '鼻炎', '脑卒中', '贫血', '慢性肾损伤', '肝功能不全'
						]
					},
					家族病史: {
						title: '家族病史',
						desc: '直系亲属常见疾病史，便于评估遗传风险',
						otherLabel: '其他病史 +',
						options: ['高血压', '糖尿病', '冠心病', '脑卒中', '恶性肿瘤', '高血脂', '哮喘', '肾病']
					},
					手术史: {
						title: '手术史',
						desc: '既往手术情况，便于评估身体恢复与风险',
						otherLabel: '其他手术 +',
						options: ['心脏手术', '脑部手术', '骨科手术', '腹部手术', '眼科手术', '甲状腺手术', '妇科手术', '泌尿手术']
					},
					过敏史: {
						title: '过敏史',
						desc: '药物、食物或其他过敏原情况',
						otherLabel: '其他过敏原 +',
						options: ['青霉素', '头孢类', '磺胺类', '阿司匹林', '海鲜', '花粉', '尘螨', '牛奶']
					},
					用药史: {
						title: '用药史',
						desc: '长期或近期用药情况',
						otherLabel: '其他药物 +',
						options: ['降压药', '降糖药', '降脂药', '抗凝药', '激素类', '抗生素', '中成药', '保健品']
					},
					个人史: {
						title: '个人史',
						desc: '生活习惯与个人健康相关情况',
						otherLabel: '其他情况 +',
						options: ['吸烟', '饮酒', '熬夜', '久坐', '精神压力大', '饮食不规律']
					},
					婚育史: {
						title: '婚育史',
						desc: '婚姻与生育相关情况',
						otherLabel: '其他情况 +',
						options: ['未婚', '已婚', '已育', '未育', '备孕中', '妊娠期']
					},
					月经史: {
						title: '月经史',
						desc: '月经周期与相关异常情况',
						otherLabel: '其他情况 +',
						options: ['周期规律', '周期不规律', '痛经', '月经量少', '月经量多', '已绝经']
					},
					运动史: {
						title: '运动史',
						desc: '日常运动习惯与强度',
						otherLabel: '其他运动 +',
						options: ['几乎不运动', '偶尔运动', '规律有氧', '力量训练', '高强度训练', '康复训练']
					}
				}
			}
		},
		computed: {
			scoreAccentColor() {
				return scoreAccent(this.score)
			},
			weekAccentColor() {
				return scoreAccent(this.weekPopup.score)
			},
			scoreRing() {
				return buildRingSegs(this.score, '#FF2C30')
			},
			weekScoreRing() {
				return buildRingSegs(this.weekPopup.score, '#3D9BFF')
			},
			abChipText() {
				if (this.abDateMode == 'day') {
					return `${this.abDateLabel} · ${this.abRangeLabel}`
				}
				return `全部日期 · ${this.abRangeLabel}`
			},
			archiveBaseText() {
				const b = this.archiveBase
				return `${b.gender} · ${b.age} · ${b.height} · ${b.weight}`
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
				const extras = this.archiveSelected.filter((t) => cfg.options.indexOf(t) == -1)
				return {
					...cfg,
					options: cfg.options.concat(extras)
				}
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
		},
		mounted() {
			this.$nextTick(() => {
				this.drawScoreRing()
			})
		},
		onLoad(options) {
			const t = Number(options.type)
			this.activeTab = Number.isNaN(t) ? 0 : t
			this.loadTabData(this.activeTab)
		},
		onShow() {
			this.loadTabData(this.activeTab)
		},
		watch: {
			activeTab(v) { this.loadTabData(v) },
			score() {
				this.$nextTick(() => this.drawScoreRing())
			},
			showWeekPopup(v) {
				if (v) this.$nextTick(() => this.drawWeekScoreRing())
			},
			'weekPopup.score'() {
				if (this.showWeekPopup) this.$nextTick(() => this.drawWeekScoreRing())
			}
		},
		methods: {
			drawRing(canvasId, score, color, size) {
				const ctx = uni.createCanvasContext(canvasId, this)
				const s = 132
				const cx = s / 2
				const cy = s / 2
				const lineWidth = Math.round(s * 0.1)
				const r = (s - lineWidth) / 2 - 2
				const percent = Math.max(0, Math.min(1, score / 100))
				const startAngle = -Math.PI / 2
				const strokeArc = (from, to, style, width, cap) => {
					ctx.beginPath()
					ctx.setLineWidth(width)
					ctx.setStrokeStyle(style)
					ctx.setLineCap(cap)
					ctx.arc(cx, cy, r, from, to, false)
					ctx.stroke()
				}
				ctx.clearRect(0, 0, s, s)
				const sNum = Number(score) || 0
				const trackPair = sNum >= 80 ? ['#1B2E50', '#14213B'] : sNum >= 50 ? ['#3A341C', '#2A2515'] : ['#3A1E2A', '#2B1620']
				// 轨道：参考 todayStep，先画稍粗的外缘再画暗色底环，颜色跟随分数段取同色系深色
				strokeArc(0, Math.PI * 2, trackPair[0], lineWidth + 1.5, 'butt')
				strokeArc(0, Math.PI * 2, trackPair[1], lineWidth, 'butt')
				if (percent <= 0) {
					ctx.draw()
					return
				}
				const full = percent >= 0.999
				const endAngle = startAngle + Math.PI * 2 * (full ? 1 : percent)
				const stops = scoreStops(score)
				const gradient = ctx.createLinearGradient(cx, cy - r, cx, cy + r)
				gradient.addColorStop(0, stops[0])
				gradient.addColorStop(0.52, stops[1])
				gradient.addColorStop(1, stops[2])
				// 整段一次画出：起点圆弧由 round 帽自然形成，终点非满环时也是同一个圆头，不会出现分段的首尾两个孤立圆点；满环用 butt 首尾闭合
				strokeArc(startAngle, endAngle, gradient, lineWidth, full ? 'butt' : 'round')
				ctx.draw()
			},
			drawScoreRing() {
				this.drawRing('scoreCanvas', this.score, '#FF2C30', 120)
			},
			drawWeekScoreRing() {
				this.drawRing('weekScoreCanvas', this.weekPopup.score || 0, '#3D9BFF', 110)
			},
			unwrapList(res) {
				if (Array.isArray(res)) return res
				if (res && Array.isArray(res.list)) return res.list
				if (res && Array.isArray(res.records)) return res.records
				if (res && Array.isArray(res.rows)) return res.rows
				return []
			},
			async loadTabData(tab) {
				try {
					if (tab == 0) await this.loadReport()
					else if (tab == 1) await this.loadAbnormal()
					else await this.loadArchive()
				} catch (e) { console.warn('healthRecord load fail', e) }
			},
			// ---- 健康报告：今日日报 + 周报列表 ----
			async loadReport() {
				const [daily, weekly] = await Promise.all([
					getDailyDetail().catch(() => null),
					getWeeklyList({ pageNum: 1, pageSize: 10 }).catch(() => [])
				])
				if (daily) {
					this.score = daily.healthScore ?? daily.score ?? daily.totalScore ?? this.score
					this.dailyStatus = daily.scoreLevel || daily.status || daily.title || this.dailyStatus
					this.dailyDesc = daily.summary || daily.desc || daily.advice || this.dailyDesc
				}
				const list = this.unwrapList(weekly)
				if (list.length) {					this.weekList = list.map((w, i) => {
						const range = w.dateRangeText || w.dateRange || w.weekRange || w.dateText || ''
						const d = w.date || w.endDate || w.endTime || ''
						return {
							raw: w,
							day: w.day || (d ? String(d).slice(8, 10) : `0${7 - i}`.slice(-2)),
							month: w.month || (d ? `${Number(String(d).slice(5, 7))}月` : ''),
							title: w.title || '本周健康周报',
							desc: w.desc || `${(w.indicators || []).length || w.indicatorCount || 7}项指标 · 已生成`,
							dateText: range, date: d, score: w.healthScore ?? w.score ?? 0,
							summary: w.summary || '', advice: w.advice || '',
							metrics: this.normalizeWeekMetrics(w)
						}
					})
				}
				this.$nextTick(() => this.drawScoreRing())
			},
			onDailyDetail() {
				uni.navigateTo({ url: '/pages/health/aiReport?type=daily' })
			},
			normalizeWeekMetrics(src) {
				const arr = src.indicators || src.metrics || src.items || []
				const iconMap = { '心率': '/static/xinlv.png', '睡眠': '/static/shuimian.png', '血氧': '/static/xueyang.png', '压力': '/static/yali.png', '运动': '/static/bushu.png', '步数': '/static/bushu.png' }
				return (Array.isArray(arr) ? arr : []).map((m) => ({
					name: m.name || m.label || m.title || '',
					trend: m.trend || m.desc || m.compare || m.subTitle || '',
					value: m.value || m.display || m.text || (m.avg != null ? `平均 ${m.avg}` : ''),
					icon: m.icon || iconMap[m.name] || '/static/xinlv.png'
				}))
			},
			async onWeekDetail(item) {
				const toPopup = (src) => ({
					dateText: src.dateRangeText || src.dateRange || src.weekRange || src.dateText || item.dateText || '',
					title: src.title || item.title || '本周健康周报',
					score: src.healthScore ?? src.score ?? src.totalScore ?? item.score ?? 0,
					summary: src.summary || item.summary || '',
					advice: src.advice || item.advice || '',
					metrics: this.normalizeWeekMetrics(src).length ? this.normalizeWeekMetrics(src) : (item.metrics || [])
				})
				try {
					const d = await getWeeklyDetail(item.date || undefined)
					this.weekPopup = toPopup((d && typeof d === 'object') ? { ...item.raw, ...item, ...d } : item)
				} catch (e) { this.weekPopup = toPopup(item) }
				this.showWeekPopup = true
				this.$nextTick(() => this.drawWeekScoreRing())
			},
			closeWeekPopup() {
				this.showWeekPopup = false
			},
			async onWeekShare() {
				if (this.exporting) return
				this.exporting = true
				try {
					await exportAndDownloadReport()
				} finally {
					this.exporting = false
				}
			},
			onWeekAskAi() {
				this.showWeekPopup = false
				uni.reLaunch({ url: '/pages/index/index?tab=2' })
			},
			onAbDateMode(mode) {
				this.abDateMode = mode
				this.loadAbnormal()
			},
			formatAbCell(a) {
				const unit = a.unit || ''
				const val = a.detectedValue ?? a.value ?? a.detectValue
				const ref = (a.referenceMin != null || a.referenceMax != null)
					? `${a.referenceMin ?? ''}-${a.referenceMax ?? ''} ${unit}`.trim()
					: (a.referenceRange || a.normalRange || '--')
				return [
					{ label: '检测值', value: val != null && val !== '' ? `${val}${unit ? ` ${unit}` : ''}` : '--', icon: '/static/jianceImg.png' },
					{ label: '个人参考区间', value: ref || '--', icon: '/static/cankaoqujian.png' },
					{ label: '持续时间', value: a.duration != null && a.duration !== '' ? `${a.duration} 分钟` : '--', icon: '/static/chixushijian.png' },
					{ label: '当前状态', value: a.statusText || (String(a.status) === '0' ? '已恢复' : a.status || '已恢复'), icon: '/static/dangqianzhuangtai.png' }
				]
			},
			toAbCard(a) {
				const title = a.abnormalName || a.title || a.typeName || a.dataType || '异常记录'
				const time = a.measureTime || a.time || a.createTime || ''
				return {
					raw: a,
					id: a.abnormalId ?? a.id,
					time,
					title,
					desc: a.advice || a.remark || a.desc || '点击查看详情',
					statusTitle: a.statusTitle || '其余时段未发现异常',
					statusDesc: a.statusDesc || a.advice || '',
					subText: a.subText || (time ? `${time} · ${title}` : title),
					level: a.severity || a.level || a.attentionLevel || '轻度关注',
					headline: a.headline || a.abnormalName || a.title || '',
					cells: a.cells || this.formatAbCell(a)
				}
			},
			async loadAbnormal() {
				const params = { pageNum: 1, pageSize: 30 }
				// 单日筛选：abDateLabel 为 MM/DD，拼当年日期做 startTime/endTime
				if (this.abDateMode === 'day' && /^\d{2}\/\d{2}$/.test(this.abDateLabel)) {
					const y = new Date().getFullYear()
					const [m, d] = this.abDateLabel.split('/')
					params.startTime = `${y}-${m}-${d} 00:00:00`
					params.endTime = `${y}-${m}-${d} 23:59:59`
				}
				const res = await getAbnormalList(params).catch(() => [])
				const list = this.unwrapList(res)
				if (!list.length) return
				this.abList = list.map((a) => this.toAbCard(a))
			},
			async onAbDetail(item) {
				try {
					if (item.id != null) {
						const d = await getAbnormalDetail(item.id)
						if (d && typeof d === 'object') {
							const card = this.toAbCard({ ...item.raw, ...d })
							this.abPopup = {
								subText: card.subText, level: card.level,
								headline: card.headline, cells: card.cells
							}
							this.showAbPopup = true
							return
						}
					}
				} catch (e) {}
				this.abPopup = { subText: item.subText, level: item.level, headline: item.headline, cells: item.cells || [] }
				this.showAbPopup = true
			},
			closeAbPopup() {
				this.showAbPopup = false
			},
			onAbAskAi() {
				this.showAbPopup = false
				uni.reLaunch({ url: '/pages/index/index?tab=2' })
			},
			onArchiveBaseTap() {
				uni.navigateTo({ url: '/pages/mine/myInfo?from=mine' })
			},
			// ---- 健康档案：detail(0=全部) + options + save ----
			async loadArchive() {
				const [detail, user] = await Promise.all([
					getHealthArchiveDetail(0).catch(() => null),
					getUserInfo().catch(() => null)
				])
				if (user) {
					const age = user.age ?? this.archiveBase.age
					this.archiveBase = {
						gender: user.gender === '1' || user.gender === 1 ? '男' : user.gender === '2' || user.gender === 2 ? '女' : (user.genderText || this.archiveBase.gender),
						age, height: user.height ? `${user.height}cm` : this.archiveBase.height,
						weight: user.weight ? `${user.weight}kg` : this.archiveBase.weight
					}
				}
				// detail 可能是数组(9项)或对象；统一按 archiveType 1-9 映射
				const arr = Array.isArray(detail) ? detail : (detail && Array.isArray(detail.list) ? detail.list : null)
				const pickItems = (node) => node?.items || node?.options || node?.contents || []
				if (arr) {
					arr.forEach((node, i) => {
						const names = pickItems(node).map((x) => x.name || x).filter(Boolean)
						const idx = (node.archiveType ? node.archiveType - 1 : i)
						if (this.archiveList[idx]) this.$set(this.archiveList, idx, { ...this.archiveList[idx], selected: names, value: names.length ? names.join('、') : '无' })
					})
				} else if (detail && typeof detail === 'object') {
					ARCHIVE_TYPE_MAP.forEach((label, i) => {
						const names = pickItems(detail[label] || detail[i + 1] || detail[String(i + 1)])
						if (names.length) this.$set(this.archiveList, i, { ...this.archiveList[i], selected: names, value: names.join('、') })
					})
				}
			},
			async onArchiveTap(item) {
				const index = this.archiveList.findIndex((row) => row.label == item.label)
				if (index < 0) return
				this.archiveEditingIndex = index
				this.archiveSelected = (this.archiveList[index].selected || []).slice()
				this.showArchivePopup = true
				// 拉取该类型的选项配置，替换本地写死的 options（文档暂无 desc/otherLabel，保持本地文案）
				try {
					const res = await getHealthArchiveOptions(index + 1)
					const tags = res?.tags || res?.options || (Array.isArray(res) ? res : null) || []
					const names = tags.map((t) => t.name || t.optionName || t).filter(Boolean)
					if (names.length) {
						const label = this.archiveList[index].label
						this.$set(this.archivePopupMap, label, { ...this.archivePopupMap[label], options: names })
					}
				} catch (e) {}
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
						if (this.archiveSelected.indexOf(text) == -1) {
							this.archiveSelected.push(text)
						}
					}
				})
			},
			async confirmArchivePopup() {
				const index = this.archiveEditingIndex
				if (index < 0) return
				const selected = this.archiveSelected.slice()
				// optionId 回填：已选项尽量带上服务端 optionId，手动输入项传 null
				const cfgOptions = this.archivePopupMap[this.archiveList[index].label]?.options || []
				const items = selected.map((name) => ({ optionId: null, name }))
				void cfgOptions
				try {
					await saveHealthArchive({ archiveType: index + 1, items })
				} catch (e) { return }
				const value = selected.length ? selected.join('、') : '无'
				this.$set(this.archiveList, index, { ...this.archiveList[index], selected, value })
				this.closeArchivePopup()
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 40rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.tab-bar {
		display: flex;
		align-items: center;
		padding: 6rpx;
		background: #1a1b25;
		border-radius: 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.tab-item {
		position: relative;
		flex: 1;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
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
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.tab-item.active .tab-text {
		color: #ffffff;
		font-weight: 600;
	}

	.panel {
		margin-top: 28rpx;
	}

	.daily-card {
		position: relative;
		overflow: hidden;
		padding: 36rpx 32rpx;
		border-radius: 24rpx;
		background: linear-gradient(135deg, #152038 0%, #0c1018 55%, #0a0c12 100%);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.daily-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.daily-body {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.daily-left {
		flex: 1;
		padding-right: 20rpx;
		display: flex;
		flex-direction: column;
	}

	.daily-tag {
		display: flex;
		align-items: center;
	}

	.daily-tag-icon {
		width: 43.01rpx;
		height: 43.01rpx;
		margin-right: 10rpx;
	}

	.daily-tag-text {
		font-size: 24rpx;
		line-height: 34rpx;

	}

	.daily-status {
		margin-top: 24rpx;
		font-size: 40rpx;
		font-weight: 700;
		line-height: 56rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.daily-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.daily-link {
		margin-top: 28rpx;
		display: flex;
		align-items: center;
	}

	.daily-link-text {
		font-size: 24rpx;
		line-height: 34rpx;
		color: #4da3ff;
	}

	.daily-link-arrow {
		width: 28rpx;
		height: 28rpx;
		margin-left: 4rpx;
	}

	.score-wrap {
		position: relative;
		width: 238.52rpx;
		height: 238.52rpx;
		flex-shrink: 0;
	}

	.score-svg {
		width: 238.52rpx;
		height: 238.52rpx;
		transform: rotate(-90deg);
	}

	.score-canvas {
		width: 238.52rpx;
		height: 238.52rpx;
	}

	.score-edge,
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
		font-size: 52rpx;
		font-weight: 700;
		line-height: 60rpx;
		background-image: linear-gradient(to right, #FFFFFF 28%, #FF2C30 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.score-label {
		margin-top: 2rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.week-card {
		margin-top: 20rpx;
		padding: 28rpx 24rpx;
		display: flex;
		align-items: center;
		border-radius: 24rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		box-sizing: border-box;
	}

	.week-date {
		width: 96rpx;
		height: 96rpx;
		margin-right: 24rpx;
		border-radius: 20rpx;
		background: #243044;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.week-day {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 44rpx;
		color: #6eb6ff;
	}

	.week-month {
		margin-top: 2rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(110, 182, 255, 0.75);
	}

	.week-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.week-title {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.week-desc {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.week-arrow {
		width: 32rpx;
		height: 32rpx;
		margin-left: 12rpx;
		flex-shrink: 0;
		opacity: 0.45;
	}

	.empty-card {
		margin-top: 80rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.empty-img {
		width: 280rpx;
		height: 220rpx;
	}

	.empty-text {
		margin-top: 20rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.ab-filter {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.ab-filter-item {
		position: relative;
		height: 56rpx;
		padding: 0 28rpx;
		margin-right: 16rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.ab-filter-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-filter-item-text {
		position: relative;
		z-index: 1;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.ab-filter-item.active .ab-filter-item-text {
		color: #ffffff;
		font-weight: 600;
	}

	.ab-filter-range {
		margin-left: auto;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.ab-card {
		position: relative;
		overflow: hidden;
		margin-bottom: 20rpx;
		padding: 32rpx 28rpx;
		border-radius: 24rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.ab-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-card-body {
		position: relative;
		z-index: 1;
	}

	.ab-time {
		display: block;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.ab-title {
		display: block;
		margin-top: 12rpx;
		font-size: 34rpx;
		font-weight: 700;
		line-height: 48rpx;
		background-image: linear-gradient(to right, #FFFFFF 28%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.ab-desc {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.ab-timeline {
		margin-top: 28rpx;
		display: flex;
		align-items: stretch;
	}

	.ab-timeline-left {
		width: 40rpx;
		margin-right: 16rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		flex-shrink: 0;
	}

	.ab-dot {
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.ab-line {
		flex: 1;
		width: 4rpx;
		margin-top: 0rpx;
		min-height: 120rpx;
		border-radius: 2rpx;
		background: linear-gradient(180deg, #4da3ff 0%, rgba(77, 163, 255, 0.08) 100%);
	}

	.ab-timeline-right {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.ab-chip {
		position: relative;
		align-self: flex-start;
		height: 52rpx;
		padding: 0 24rpx;
		border-radius: 16rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		background: rgba(36, 48, 72, 0.9);
		box-sizing: border-box;
	}

	.ab-chip-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-chip-text {
		position: relative;
		z-index: 1;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.ab-status {
		position: relative;
		overflow: hidden;
		margin-top: 16rpx;
		padding: 28rpx 24rpx;
		border-radius: 20rpx;
		background: rgba(28, 36, 56, 0.95);
		box-sizing: border-box;
	}

	.ab-status-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-status-body {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
	}

	.ab-status-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.ab-status-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.defsBg {
		    position: absolute;
		    top: -6rpx;
		    right: -8rpx;
		    width: 252rpx;
		    height: 252rpx;
	}

	.week-mask {
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

	.week-popup {
		position: relative;
		width: 100%;
		max-height: 92vh;
		margin: 30rpx;
		border-radius: 32rpx 32rpx 0 0;
		overflow: hidden;
		background: #0b0f1a;
		animation: weekSlideUp 0.28s ease-out;
	}

	@keyframes weekSlideUp {
		from {
			transform: translateY(100%);
		}

		to {
			transform: translateY(0);
		}
	}

	.week-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.week-popup-scroll {
		position: relative;
		z-index: 1;
		max-height: 92vh;
	}

	.week-popup-body {
		padding: 40rpx 32rpx 48rpx;
		box-sizing: border-box;
	}

	.week-popup-head {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.week-popup-title {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.week-popup-date {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.week-score-wrap {
		position: relative;
		width: 250rpx;
		height: 250rpx;
		margin: 36rpx auto 0;
	}

	.week-score-svg {
		width: 250rpx;
		height: 250rpx;
		transform: rotate(-90deg);
	}

	.week-score-canvas {
		width: 250rpx;
		height: 250rpx;
	}

	.week-score-center {
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

	.week-score-num {
		font-size: 64rpx;
		font-weight: 700;
		line-height: 72rpx;
		background-image: linear-gradient(to right, #FFFFFF 28%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.week-score-label {
		margin-top: 2rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.5);
	}

	.week-popup-summary {
		display: block;
		margin-top: 28rpx;
		padding: 0 20rpx;
		text-align: center;
		font-size: 28rpx;
		line-height: 40rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.metric-card {
		margin-top: 32rpx;
		padding: 8rpx 28rpx;
		border-radius: 24rpx;
		background: rgba(26, 27, 37, 0.92);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.metric-row {
		min-height: 139rpx;
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.metric-row.border {
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.metric-icon {
		width: 48rpx;
		height: 48rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
	}

	.metric-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.metric-name {
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.metric-trend {
		margin-top: 4rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.metric-value {
		margin-left: 16rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		font-weight: 500;
		font-size: 24rpx;
		color: #FFFFFF;
	}

	.advice-card {
		position: relative;
		overflow: hidden;
		margin-top: 24rpx;
		padding: 28rpx 24rpx;
		border-radius: 24rpx;
		display: flex;
		align-items: flex-start;
		box-sizing: border-box;
	}

	.advice-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.advice-icon {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.advice-body {
		position: relative;
		z-index: 1;
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.advice-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.advice-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.week-popup-actions {
		margin-top: 36rpx;
		display: flex;
		align-items: center;
	}

	.week-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.week-btn.outline {
		margin-right: 20rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.week-btn.solid {
		background: transparent;
	}

	.week-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.week-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.week-btn-text.outline-text {
		color: #7eb6ff;
	}

	.ab-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.ab-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #0b0f1a;
		animation: abSlideUp 0.28s ease-out;
	}

	@keyframes abSlideUp {
		from {
			transform: translateY(100%);
		}

		to {
			transform: translateY(0);
		}
	}

	.ab-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.ab-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.ab-popup-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.ab-level {
		position: relative;
		margin-top: 28rpx;
		height: 72rpx;
		padding: 0 24rpx;
		border-radius: 16rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		background: rgba(30, 48, 90, 0.65);
		box-sizing: border-box;
	}

	.ab-level-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-level-icon {
		position: relative;
		z-index: 1;
		width: 40rpx;
		height: 40rpx;
		margin-right: 12rpx;
		flex-shrink: 0;
	}

	.ab-level-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #4da3ff;
	}

	.ab-popup-headline {
		display: block;
		margin-top: 28rpx;
		font-size: 34rpx;
		font-weight: 700;
		line-height: 48rpx;
		color: #ffffff;
	}

	.ab-grid {
		margin-top: 24rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.ab-grid-item {
		position: relative;
		width: 304rpx;
		height: 152rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		margin-bottom: 16rpx;
		padding: 24rpx 20rpx;
		border-radius: 20rpx;
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
	}

	.ab-grid-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ab-grid-icon {
		position: relative;
		z-index: 1;
		width: 36rpx;
		height: 36rpx;
		margin-right: 10rpx;
	}

	.ab-grid-label {
		position: relative;
		z-index: 1;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.ab-grid-value {
		position: relative;
		z-index: 1;
		margin-top: 8rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #ffffff;
	}

	.ab-popup-actions {
		margin-top: 32rpx;
		display: flex;
		align-items: center;
	}

	.ab-popup-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.ab-popup-btn.outline {
		margin-right: 20rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.ab-popup-btn.solid {
		background: transparent;
	}

	.ab-popup-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.ab-popup-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.ab-popup-btn-text.outline-text {
		color: #7eb6ff;
	}

	.archive-base {
		padding: 28rpx 24rpx;
		width: 694rpx;
		height: 152rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.archive-base-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.archive-base-label {
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.archive-base-value {
		margin-top: 10rpx;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.archive-grid {
		margin-top: 20rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.archive-item {
		width: 334rpx;
		height: 152rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
		margin-bottom: 20rpx;
		padding: 24rpx 20rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
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
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.archive-arrow {
		width: 34rpx;
		height: 34rpx;
		margin-left: 12rpx;
	}

	.archive-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2100;
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
		position: relative;
		height: 96rpx;
		border-radius: 48rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.archive-confirm-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.archive-confirm-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}
</style>