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
					<view v-for="(item, index) in partList" :key="item.categoryId || item.name" class="part-item"
						:class="{ active: activePart === index }" @click="onSelectPart(index)">
						<image v-if="activePart === index" class="part-item-bg" src="/static/selectBodyIcon.png"
							mode="aspectFit"></image>
						<text class="part-text">{{ item.name }}</text>
					</view>
				</scroll-view>

				<view class="body-visual">
					<image class="body-base" src="/static/bodyBg.png" mode=""></image>
					<image v-if="bodyImgSrc" class="body-img" :src="bodyImgSrc" mode="aspectFit"></image>

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
							{{ item.text }}
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
							<text class="summary-title">{{ currentPartName }}健康解读</text>
						</view>
						<text class="summary-status">{{ summaryStatus }}</text>
						<text class="summary-desc">{{ summaryDesc }}</text>
					</view>
					<view class="score-wrap">
						<canvas canvas-id="bodyScoreCanvas" id="bodyScoreCanvas" class="score-canvas"></canvas>
						<view class="score-center">
							<text class="score-num" :style="{ backgroundImage: `linear-gradient(to right, #FFFFFF 28%, ${scoreAccentColor} 100%)` }">{{ score }}</text>
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

						<view v-if="weeklyContents.length" class="weekly-list">
							<view
								v-for="(item, index) in weeklyContents"
								:key="index"
								class="weekly-item"
								:class="{ border: index < weeklyContents.length - 1 }"
							>
								<text class="weekly-item-text">{{ item }}</text>
							</view>
						</view>
						<view v-else class="empty-box">
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
					<text class="history-popup-title">{{ currentPartName }} · 近7天</text>
					<text class="history-popup-desc">{{ historyPopupDesc }}</text>

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
							:key="tag.optionId != null ? ('id-' + tag.optionId) : ('name-' + tag.name)"
							class="archive-tag"
							:class="{ active: isArchiveTagSelected(tag) }"
							@click="toggleArchiveTag(tag)"
						>
							<text class="archive-tag-text">{{ tag.name }}</text>
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
import { getHealthCenterIndex, getPortraitList } from '@/common/api/healthCenter.js'
import {
	getHealthArchiveOrgans,
	getHealthArchiveByOrgan,
	getHealthArchiveWeekly,
	getHealthArchiveDetail,
	getHealthArchiveOptions,
	saveHealthArchive
} from '@/common/api/healthArchive.js'

function ringDash(r, percent) {
	const circ = 2 * Math.PI * r
	const p = Math.max(0, Math.min(1, percent))
	return `${circ * p} ${circ}`
}

// 与 healthRecord.vue 同一套三段配色：80-100 蓝，50-79 黄，0-49 红
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

function pad2(n) {
	return String(n).padStart(2, '0')
}

function toYmd(d) {
	return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function startOfWeekMonday(input) {
	const date = new Date(input)
	date.setHours(0, 0, 0, 0)
	const day = date.getDay()
	const diff = day === 0 ? -6 : 1 - day
	date.setDate(date.getDate() + diff)
	return date
}

function asArray(data) {
	if (!data) return []
	if (Array.isArray(data)) return data
	if (Array.isArray(data.list)) return data.list
	if (Array.isArray(data.rows)) return data.rows
	if (Array.isArray(data.records)) return data.records
	if (Array.isArray(data.data)) return data.data
	return []
}

function pickText(...vals) {
	for (let i = 0; i < vals.length; i++) {
		const v = vals[i]
		if (v != null && String(v).trim() !== '') return String(v).trim()
	}
	return ''
}

function formatHistoryDate(dateStr) {
	if (!dateStr) return ''
	const s = String(dateStr).slice(0, 10)
	const today = toYmd(new Date())
	const yest = new Date()
	yest.setDate(yest.getDate() - 1)
	if (s === today) return '今天'
	if (s === toYmd(yest)) return '昨天'
	const parts = s.split('-')
	if (parts.length >= 3) {
		return `${Number(parts[1])}月${Number(parts[2])}日`
	}
	return s
}

const BUBBLE_POS = [
	{ left: '58%', top: '42%', delay: '0s' },
	{ left: '8%', top: '28%', delay: '0.15s' },
	{ left: '62%', top: '22%', delay: '0.3s' },
	{ left: '12%', top: '55%', delay: '0.45s' },
	{ left: '55%', top: '62%', delay: '0.6s' }
]

const ARCHIVE_META = [
	{ archiveType: 1, label: '既往病史', desc: '过往疾病，特别是涉及心脏、脑、肺、肾等重要器官', otherLabel: '其他疾病 +' },
	{ archiveType: 2, label: '家族病史', desc: '直系亲属常见疾病史，便于评估遗传风险', otherLabel: '其他病史 +' },
	{ archiveType: 3, label: '手术史', desc: '既往手术情况，便于评估身体恢复与风险', otherLabel: '其他手术 +' },
	{ archiveType: 4, label: '过敏史', desc: '药物、食物或其他过敏原情况', otherLabel: '其他过敏原 +' },
	{ archiveType: 5, label: '用药史', desc: '长期或近期用药情况', otherLabel: '其他药物 +' },
	{ archiveType: 6, label: '个人史', desc: '生活习惯与个人健康相关情况', otherLabel: '其他情况 +' },
	{ archiveType: 7, label: '婚育史', desc: '婚姻与生育相关情况', otherLabel: '其他情况 +' },
	{ archiveType: 8, label: '月经史', desc: '月经周期与相关异常情况', otherLabel: '其他情况 +' },
	{ archiveType: 9, label: '运动史', desc: '日常运动习惯与强度', otherLabel: '其他运动 +' }
]

const DEFAULT_PARTS = [
	{ name: '全身', categoryId: null, icon: '' },
	{ name: '体征', categoryId: null, icon: '' },
	{ name: '大脑', categoryId: null, icon: '' },
	{ name: '甲状腺', categoryId: null, icon: '' },
	{ name: '呼吸', categoryId: null, icon: '' },
	{ name: '肺部', categoryId: null, icon: '' },
	{ name: '心脏', categoryId: null, icon: '' },
	{ name: '肝脏', categoryId: null, icon: '' },
	{ name: '血管', categoryId: null, icon: '' },
	{ name: '胃部', categoryId: null, icon: '' },
	{ name: '胰腺', categoryId: null, icon: '' },
	{ name: '肾脏', categoryId: null, icon: '' },
	{ name: '其他', categoryId: null, icon: '' }
]

export default {
	data() {
		return {
			statusBarHeight: 20,
			activePart: 0,
			activeTab: 0,
			score: 0,
			summaryStatus: '加载中...',
			summaryDesc: '',
			organSummaryText: '',
			centerSummaryStatus: '',
			centerSummaryDesc: '',
			rangeStart: null,
			partList: DEFAULT_PARTS.map((p) => ({ ...p })),
			archiveList: ARCHIVE_META.map((m) => ({
				archiveType: m.archiveType,
				label: m.label,
				value: '无',
				selected: []
			})),
			showArchivePopup: false,
			showHistoryPopup: false,
			scanning: true,
			showBubbles: false,
			scanTimer: null,
			statusBubbles: [],
			historyList: [],
			historyPopupDesc: '当前数据未发现明显异常变化',
			weeklyContents: [],
			archiveEditingIndex: -1,
			archiveSelected: [],
			archiveOptionList: [],
			archiveSaving: false,
			historyLoading: false
		}
	},
	computed: {
		currentPartName() {
			const part = this.partList[this.activePart]
			return (part && part.name) || '全身'
		},
		bodyImgSrc() {
			const part = this.partList[this.activePart]
			if (part && part.icon) return part.icon
			return ''
		},
		scoreRing() {
			return { r: 40, stroke: 8, dash: '' }
		},
		scoreAccentColor() {
			return scoreAccent(this.score)
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
			const meta = ARCHIVE_META.find((m) => m.archiveType === item.archiveType) || {}
			const baseOptions = this.archiveOptionList.slice()
			const extras = this.archiveSelected.filter((sel) => {
				return !baseOptions.some((opt) => opt.name === sel.name)
			})
			return {
				title: item.label,
				desc: meta.desc || '',
				otherLabel: meta.otherLabel || '其他 +',
				options: baseOptions.concat(extras)
			}
		}
	},
	onLoad() {
		const sys = uni.getSystemInfoSync()
		this.statusBarHeight = sys.statusBarHeight || 20
		this.rangeStart = startOfWeekMonday(new Date())
		this.startBodyScan()
		this.bootstrap()
		this.$nextTick(() => this.drawBodyRing())
	},
	mounted() {
		this.$nextTick(() => this.drawBodyRing())
	},
	watch: {
		score() {
			this.$nextTick(() => this.drawBodyRing())
		}
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
			this.scanTimer = setTimeout(() => {
				this.scanning = false
				this.showBubbles = true
				this.scanTimer = null
			}, 2000)
		},
		drawBodyRing() {
			const canvasId = 'bodyScoreCanvas'
			const score = this.score
			const size = 80
			const ctx = uni.createCanvasContext(canvasId, this)
			if (!ctx) return
			const s = size
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
			strokeArc(0, Math.PI * 2, trackPair[0], lineWidth + 1.5, 'butt')
			strokeArc(0, Math.PI * 2, trackPair[1], lineWidth, 'butt')
			if (percent > 0) {
				const full = percent >= 0.999
				const endAngle = startAngle + Math.PI * 2 * (full ? 1 : percent)
				const stops = scoreStops(score)
				const gradient = ctx.createLinearGradient(cx, cy - r, cx, cy + r)
				gradient.addColorStop(0, stops[0])
				gradient.addColorStop(0.52, stops[1])
				gradient.addColorStop(1, stops[2])
				strokeArc(startAngle, endAngle, gradient, lineWidth, full ? 'butt' : 'round')
			}
			ctx.draw()
		},
		async bootstrap() {
			await Promise.all([
				this.loadCenterIndex(),
				this.loadOrgans(),
				this.loadArchiveDetail(),
				this.loadWeekly()
			])
			this.refreshPartSummary()
		},
		async loadCenterIndex() {
			try {
				const data = await getHealthCenterIndex()
				this.applyCenterIndex(data)
			} catch (e) {
				if (!this.summaryStatus || this.summaryStatus === '加载中...') {
					this.summaryStatus = '暂无健康解读'
					this.summaryDesc = '请稍后再试或完善健康档案'
				}
			}
		},
		applyCenterIndex(data) {
			if (!data || typeof data !== 'object') return
			const score = Number(
				data.score != null
					? data.score
					: data.totalScore != null
						? data.totalScore
						: data.healthScore
			)
			if (!Number.isNaN(score) && score >= 0) {
				this.score = Math.round(score)
			}
			this.centerSummaryStatus = pickText(
				data.status,
				data.statusText,
				data.summaryStatus,
				data.title,
				data.levelText
			) || '今日整体状态良好'
			this.centerSummaryDesc = pickText(
				data.description,
				data.desc,
				data.summary,
				data.summaryDesc,
				data.content,
				data.advice
			) || '活动、睡眠和心率数据均处于个人近期稳定范围'
			this.summaryStatus = this.centerSummaryStatus
			this.summaryDesc = this.centerSummaryDesc

			const abnormalList = asArray(
				data.abnormalList ||
					data.abnormals ||
					data.abnormalRecords ||
					data.todayAbnormals ||
					data.recordList ||
					data.tags
			)
			this.statusBubbles = this.mapBubbles(abnormalList)
		},
		mapBubbles(list) {
			const texts = list
				.map((item) => {
					if (typeof item === 'string') return item
					return pickText(
						item.text,
						item.name,
						item.title,
						item.label,
						item.dataTypeName,
						item.content,
						item.status
					)
				})
				.filter(Boolean)
				.slice(0, BUBBLE_POS.length)
			if (!texts.length) {
				return BUBBLE_POS.slice(0, 3).map((pos, i) => ({
					...pos,
					text: ['心率正常', '睡眠良好', '压力平稳'][i]
				}))
			}
			return texts.map((text, i) => ({
				...BUBBLE_POS[i],
				text
			}))
		},
		async loadOrgans() {
			try {
				const data = await getHealthArchiveOrgans()
				const list = asArray(data)
				if (!list.length) return
				const organs = list
					.map((item) => ({
						name: pickText(item.name, item.categoryName, item.organName) || '未命名',
						categoryId:
							item.categoryId != null
								? item.categoryId
								: item.id != null
									? item.id
									: item.organId != null
										? item.organId
										: null,
						icon: pickText(item.icon, item.iconUrl, item.img, item.imageUrl),
						items: asArray(item.items),
						sort: Number(item.sort != null ? item.sort : 999)
					}))
					.filter((item) => item.name)
					.sort((a, b) => a.sort - b.sort)
				const hasWhole = organs.some((o) => o.name === '全身')
				this.partList = hasWhole ? organs : [{ name: '全身', categoryId: null, icon: '', sort: 0 }].concat(organs)
				if (this.activePart >= this.partList.length) {
					this.activePart = 0
				}
			} catch (e) {}
		},
		async loadArchiveDetail() {
			try {
				const data = await getHealthArchiveDetail(0)
				this.applyArchiveDetail(data)
			} catch (e) {}
		},
		applyArchiveDetail(data) {
			const list = asArray(data)
			const byType = {}
			list.forEach((row) => {
				const type = Number(
					row.archiveType != null
						? row.archiveType
						: row.type != null
							? row.type
							: row.archive_type
				)
				if (!type) return
				const items = asArray(row.items || row.itemList || row.records).map((it) => ({
					optionId: it.optionId != null ? it.optionId : it.id != null ? it.id : null,
					name: pickText(it.name, it.optionName, it.label, it.content)
				})).filter((it) => it.name)
				byType[type] = items
			})
			this.archiveList = ARCHIVE_META.map((meta) => {
				const selected = byType[meta.archiveType] || []
				return {
					archiveType: meta.archiveType,
					label: meta.label,
					selected,
					value: selected.length ? selected.map((i) => i.name).join('、') : '无'
				}
			})
		},
		async loadWeekly() {
			if (!this.rangeStart) return
			try {
				const data = await getHealthArchiveWeekly(toYmd(this.rangeStart))
				this.applyWeekly(data)
			} catch (e) {
				this.weeklyContents = []
			}
		},
		applyWeekly(data) {
			if (!data) {
				this.weeklyContents = []
				return
			}
			const contents = asArray(
				data.contents || data.contentList || data.items || data.list || data.records
			)
			this.weeklyContents = contents
				.map((item) => {
					if (typeof item === 'string') return item
					return pickText(
						item.content,
						item.name,
						item.text,
						item.title,
						item.label,
						item.desc
					)
				})
				.filter(Boolean)
		},
		async refreshPartSummary() {
			const part = this.partList[this.activePart]
			if (!part || part.categoryId == null) {
				this.summaryStatus = this.centerSummaryStatus || '今日整体状态良好'
				this.summaryDesc = this.centerSummaryDesc || '活动、睡眠和心率数据均处于个人近期稳定范围'
				this.organSummaryText = ''
				return
			}
			try {
				const data = await getHealthArchiveByOrgan(part.categoryId)
				const text =
					typeof data === 'string'
						? data
						: pickText(
								data && data.content,
								data && data.text,
								data && data.desc,
								data && data.description,
								data && data.result,
								data && data.data
							)
				this.organSummaryText = text
				if (text) {
					this.summaryStatus = `${part.name}相关档案`
					this.summaryDesc = text
				} else {
					const fromOrganItems = asArray(part.items)
						.map((it) => pickText(it.name, it.optionName))
						.filter(Boolean)
						.join('、')
					this.summaryStatus = fromOrganItems ? `${part.name}有关联档案` : `${part.name}暂无关联档案`
					this.summaryDesc = fromOrganItems || '当前健康档案中暂无该器官相关选项'
				}
			} catch (e) {
				this.summaryStatus = `${part.name}健康解读`
				this.summaryDesc = '暂无法获取该部位解读'
			}
		},
		onSelectPart(index) {
			if (this.activePart === index) return
			this.activePart = index
			this.refreshPartSummary()
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
		async onHistory() {
			this.showHistoryPopup = true
			if (this.historyLoading) return
			this.historyLoading = true
			try {
				const data = await getPortraitList(7)
				const list = asArray(data)
				this.historyList = list.map((item) => {
					const date = formatHistoryDate(
						pickText(item.date, item.day, item.recordDate, item.statDate)
					)
					const status = pickText(
						item.status,
						item.statusText,
						item.summary,
						item.description,
						item.title
					) || '状态平稳'
					const flag = pickText(item.flag, item.flagText, item.completeText, item.tag)
					const score = item.score != null ? item.score : item.totalScore
					return {
						date: date || '--',
						status: score != null && score !== '' ? `${status} · ${score}分` : status,
						flag: flag || (date === '今天' ? '已更新' : '完整')
					}
				})
				if (!this.historyList.length) {
					this.historyPopupDesc = '近7天暂无健康画像数据'
				} else {
					const hasAbnormal = list.some((item) => {
						const n = Number(item.abnormalCount != null ? item.abnormalCount : item.count)
						return (n > 0) || /异常|偏低|偏高|不佳/.test(String(item.status || item.statusText || ''))
					})
					this.historyPopupDesc = hasAbnormal
						? '近7天存在波动，建议结合异常记录查看'
						: '当前数据未发现明显异常变化'
				}
			} catch (e) {
				this.historyPopupDesc = '历史数据加载失败'
			} finally {
				this.historyLoading = false
			}
		},
		closeHistoryPopup() {
			this.showHistoryPopup = false
		},
		shiftRange(days) {
			const next = new Date(this.rangeStart)
			next.setDate(next.getDate() + days)
			this.rangeStart = startOfWeekMonday(next)
			this.loadWeekly()
		},
		onPrevRange() {
			this.shiftRange(-7)
		},
		onNextRange() {
			this.shiftRange(7)
		},
		async onArchiveTap(item) {
			const index = this.archiveList.findIndex((row) => row.label === item.label)
			if (index < 0) return
			this.archiveEditingIndex = index
			this.archiveSelected = (this.archiveList[index].selected || []).map((it) => ({
				optionId: it.optionId != null ? it.optionId : null,
				name: it.name
			}))
			this.archiveOptionList = []
			this.showArchivePopup = true
			try {
				const data = await getHealthArchiveOptions(item.archiveType)
				this.archiveOptionList = this.normalizeOptions(data)
			} catch (e) {
				this.archiveOptionList = []
			}
		},
		normalizeOptions(data) {
			const tags = asArray(
				(data && (data.tags || data.tagList || data.options || data.list)) || data
			)
			const fromTags = tags
				.map((tag) => {
					if (typeof tag === 'string') {
						return { optionId: null, name: tag }
					}
					return {
						optionId:
							tag.optionId != null
								? tag.optionId
								: tag.id != null
									? tag.id
									: null,
						name: pickText(tag.name, tag.optionName, tag.label, tag.content)
					}
				})
				.filter((tag) => tag.name)

			if (fromTags.length) return fromTags

			const groups = asArray(data && (data.groups || data.groupList))
			const fromGroups = []
			groups.forEach((g) => {
				asArray(g.items || g.tags || g.options).forEach((tag) => {
					if (typeof tag === 'string') {
						fromGroups.push({ optionId: null, name: tag })
						return
					}
					const name = pickText(tag.name, tag.optionName, tag.label)
					if (!name) return
					fromGroups.push({
						optionId:
							tag.optionId != null
								? tag.optionId
								: tag.id != null
									? tag.id
									: null,
						name
					})
				})
			})
			return fromGroups
		},
		closeArchivePopup() {
			this.showArchivePopup = false
			this.archiveEditingIndex = -1
			this.archiveSelected = []
			this.archiveOptionList = []
		},
		isArchiveTagSelected(tag) {
			const name = tag && tag.name
			return this.archiveSelected.some((it) => it.name === name)
		},
		toggleArchiveTag(tag) {
			const name = tag && tag.name
			if (!name) return
			const idx = this.archiveSelected.findIndex((it) => it.name === name)
			if (idx > -1) {
				this.archiveSelected.splice(idx, 1)
				return
			}
			this.archiveSelected.push({
				optionId: tag.optionId != null ? tag.optionId : null,
				name
			})
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
					if (!this.archiveSelected.some((it) => it.name === text)) {
						this.archiveSelected.push({
							optionId: null,
							name: text
						})
					}
				}
			})
		},
		async confirmArchivePopup() {
			const index = this.archiveEditingIndex
			if (index < 0 || this.archiveSaving) return
			const row = this.archiveList[index]
			const selected = this.archiveSelected
				.map((it) => ({
					optionId: it.optionId != null ? it.optionId : null,
					name: it.name
				}))
				.filter((it) => it.name)
			this.archiveSaving = true
			try {
				await saveHealthArchive({
					archiveType: row.archiveType,
					items: selected
				})
				const value = selected.length ? selected.map((i) => i.name).join('、') : '无'
				this.$set(this.archiveList, index, {
					...row,
					selected,
					value
				})
				this.closeArchivePopup()
				this.loadOrgans()
				this.refreshPartSummary()
				uni.showToast({
					title: '已保存',
					icon: 'none'
				})
			} catch (e) {
			} finally {
				this.archiveSaving = false
			}
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
			rgba(5, 48, 146, 0) 0%,
			rgba(5, 48, 146, 0.12) 45%,
			rgba(5, 48, 146, 0.35) 100%
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
			rgba(5, 48, 146, 0) 0%,
			rgba(5, 48, 146, 0.55) 18%,
			#053092 50%,
			rgba(5, 48, 146, 0.55) 82%,
			rgba(5, 48, 146, 0) 100%
		);
		box-shadow:
			0 0 6rpx rgba(5, 48, 146, 0.85),
			0 0 14rpx rgba(5, 48, 146, 0.65),
			0 0 28rpx rgba(5, 48, 146, 0.35);
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
		font-size: 22rpx;
		color: #ffffff;
		z-index: 9;
		padding: 12rpx 22rpx;
		border-radius: 28rpx;
		background: linear-gradient(90deg, #1a6dff 0%, #3d9bff 100%);
		box-shadow: 0 6rpx 18rpx rgba(40, 120, 255, 0.35);
		opacity: 0;
		animation: bubbleIn 0.55s ease-out forwards, bubbleFloat 2.8s ease-in-out 0.55s infinite;
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

	.score-canvas {
		width: 160rpx;
		height: 160rpx;
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
		background-image: linear-gradient(to right, #FFFFFF 28%, #3D9BFF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
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
		overflow: hidden;
		display: flex;
		flex-direction: column;
	}

	.archive-label {
		display: block;
		width: 100%;
		font-size: 22rpx;
		line-height: 32rpx;
		color: rgba(255, 255, 255, 0.45);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.archive-value {
		display: block;
		width: 100%;
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