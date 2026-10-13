<template>
	<view class="page">
		<!-- 顶部引导卡片 -->
		<view class="hero-card">
			<!-- 卡片背景图，自行补充 src -->
			<image class="hero-bg" src="/static/aiBg.png" mode=""></image>
			<view class="hero-body">
				<!-- 顶部图标，自行补充 src -->
				<image class="hero-icon" src="/static/aiTopImg.png" mode=""></image>
				<text class="hero-tag">AI报告解读</text>
				<text class="hero-title">把复杂指标讲明白</text>
				<text class="hero-desc">支持体检单、检验单等演示场景；解析结果需结合专业医生意见</text>

				<image class="upload-btn" @click="onUpload" src="/static/yuntuBg.png" mode=""></image>

				<image class="feature-row" src="/static/aiCardBot.png" mode=""></image>
			</view>
		</view>

		<!-- 隐私承诺 -->
		<view class="privacy-badge">
			<!-- 盾牌图标，自行补充 src -->
			<image class="privacy-icon" src="/static/dunpai.png" mode=""></image>
			<text class="privacy-text">平台承诺保证隐私安全</text>
		</view>
		
		<!-- AI解析报告 -->
		<view v-if="parseResult" class="parse-card">
			<!-- 报告卡片背景图，自行补充 src -->
			<image class="parse-card-bg" mode="aspectFill"></image>
			<view class="parse-body">
				<view class="parse-head">
					<view class="parse-head-left">
						<text class="parse-cate">{{ parseResult.category }}</text>
						<text class="parse-title">{{ parseResult.title }}</text>
					</view>
					<view class="parse-detail-btn" @click="onParseDetail">
						<!-- 按钮背景图，自行补充 src -->
						<image class="parse-detail-btn-bg" src="/static/jiegouhua.png" mode="scaleToFill"></image>
						<text class="parse-detail-btn-text">结构化详情</text>
					</view>
				</view>

				<view
					v-for="(item, index) in parseResult.items"
					:key="index"
					class="parse-item"
					:class="{ long: (item.value || '').length > 18 }"
				>
					<!-- 指标卡片背景图，自行补充 src -->
					<image class="parse-item-bg" mode="aspectFill"></image>
					<view class="parse-item-left">
						<view class="parse-item-name">{{ item.name }}</view>
						<view class="parse-item-ref">{{ item.ref }}</view>
						<view class="parse-item-status">{{ item.status }}</view>
					</view>
					<view class="parse-item-value">{{ item.value }}</view>
				</view>

				<view class="parse-advice">
					<!-- 建议背景图，自行补充 src -->
					<image class="parse-advice-bg" src="/static/heartCardBg.png" mode="aspectFill"></image>
					<!-- 建议图标，自行补充 src -->
					<image class="parse-advice-icon" src="/static/qushijianyi.png" mode="aspectFit"></image>
					<view class="parse-advice-body">
						<view class="parse-advice-title">健康与建议</view>
						<text class="parse-advice-desc">{{ parseResult.advice }}</text>
					</view>
				</view>

				<view class="parse-actions">
					<view class="parse-btn gray" @click="onParseDelete">
						<text class="parse-btn-text gray-text">删除</text>
					</view>
					<view class="parse-btn outline" @click="onParseShare">
						<text class="parse-btn-text outline-text">分享/导出</text>
					</view>
					<view class="parse-btn solid" @click="onParseAsk">
						<!-- 按钮背景图，自行补充 src -->
						<image class="parse-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
						<text class="parse-btn-text">继续提问</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 最近上传 -->
		<view class="section">
			<text class="section-title">最近上传</text>
			<text class="section-sub">历史报告</text>

			<view v-for="(item, index) in reportList" :key="index" class="report-card" @click="onReportTap(item)">
				<view class="report-date">
					<text class="report-day">{{ item.day }}</text>
					<text class="report-month">{{ item.month }}</text>
				</view>
				<view class="report-info">
					<text class="report-title">{{ item.title }}</text>
					<text class="report-desc">{{ item.desc }}</text>
				</view>
				<!-- 箭头图标，自行补充 src -->
				<image class="report-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 底部说明 -->
		<view class="footer-tip">
			<!-- 提示图标，自行补充 src -->
			<image class="footer-tip-icon" src="/static/jinggao.png" mode=""></image>
			<text class="footer-tip-text">档案内容仅供您本人使用，我们将严格保护您的隐私安全</text>
		</view>

		<!-- 选择上传方式弹窗 -->
		<view v-if="showUploadPopup" class="upload-mask" @click="closeUploadPopup">
			<view class="upload-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="upload-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="upload-popup-body">
					<text class="upload-popup-title">选择上传方式</text>
					<text class="upload-popup-sub">智能检测</text>

					<view class="upload-options">
						<view v-for="item in uploadOptions" :key="item.key" class="upload-option"
							@click="onSelectUpload(item.key)">
							<!-- 选项图标，自行补充 src -->
							<image class="upload-option-icon" :src="item.icon" mode=""></image>
							<text class="upload-option-text">{{ item.label }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 结构化报告详情弹窗 -->
		<view v-if="showDetailPopup" class="detail-mask" @click="closeDetailPopup">
			<view class="detail-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="detail-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="detail-popup-body">
					<text class="detail-popup-title">结构化报告详情</text>
					<text class="detail-popup-sub">{{ detailPopup.subText }}</text>

					<view class="detail-alert">
						<!-- 关注条背景图，自行补充 src -->
						<image class="detail-alert-bg" src="/static/heartCardBg.png" mode=""></image>
						<!-- 关注图标，自行补充 src -->
						<image class="detail-alert-icon" src="/static/dengjitu.png" mode="aspectFit"></image>
						<text class="detail-alert-text">{{ detailPopup.alertText }}</text>
					</view>

					<view class="detail-grid">
						<view v-for="(cell, index) in detailPopup.cells" :key="index" class="detail-grid-item">
							<!-- 指标卡片背景图，自行补充 src -->
							<image class="detail-grid-item-bg" mode="aspectFill"></image>
							<view class="" style="display: flex;justify-content: flex-start;align-items: center;">
								<image class="detail-grid-icon" :src="cell.icon" mode=""></image>
								<view class="detail-grid-label">{{ cell.label }}</view>
							</view>
							<view class="detail-grid-value">{{ cell.value }}</view>
						</view>
					</view>

					<view class="detail-actions">
						<view class="detail-btn outline" @click="closeDetailPopup">
							<text class="detail-btn-text outline-text">完成</text>
						</view>
						<view class="detail-btn solid" @click="onDetailAskAi">
							<!-- 按钮背景图，自行补充 src -->
							<image class="detail-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
							<text class="detail-btn-text">继续问AI</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getReportHistory, getReportDetail, analyzeReportStreamByUrl, exportAndDownloadReport } from '@/common/api/ai.js'
	import { uploadFile } from '@/common/api/file.js'

	function pad2(n) {
		return String(n).padStart(2, '0')
	}

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

	function toDayMonth(dateStr) {
		const s = String(dateStr || '').slice(0, 10)
		const parts = s.split('-')
		if (parts.length >= 3) return { day: String(Number(parts[2])), month: `${Number(parts[1])}月` }
		const now = new Date()
		return { day: pad2(now.getDate()), month: `${now.getMonth() + 1}月` }
	}

	function uploadToUrl(filePath) {
		return new Promise((resolve, reject) => {
			uploadFile(filePath).then((d) => {
				if (typeof d === 'string' && d) { resolve(d); return }
				const url = d && (d.url || d.fileUrl || d.path || d.data) || ''
				if (url) resolve(url)
				else reject(new Error('上传失败'))
			}).catch(reject)
		})
	}

	// 后端 item 是 {key, value} 碎片流：名称/正常/异常 三连为一组，
	// 或同名 key 出现两次（第一次=正常范围，第二次=异常含义），拼成一张指标卡
	function groupStreamItems(rawList) {
		const ABNORMAL_RE = /偏高|偏低|异常|阳性|\+|↑|↓|超标|增高|降低|临界/
		const out = []
		let pending = null
		const flush = () => { if (pending && (pending.name || pending.value)) out.push(pending); pending = null }
		;(rawList || []).forEach((r) => {
			const k = String((r && r.k) || '').trim()
			const v = String((r && r.v) || '').trim()
			if (!k && !v) return
			if (k === '名称') { flush(); pending = { name: v, ref: '', status: '正常', value: '', abnormal: '' }; return }
			if (k === '正常' && pending) { pending.ref = v; return }
			if (k === '异常' && pending) { pending.abnormal = v; pending.value = pending.value || v; flush(); return }
			if (pending && pending.name === k) {
				if (!pending.ref) pending.ref = v
				else { pending.abnormal = v; pending.value = pending.value || v; flush() }
				return
			}
			flush()
			pending = { name: k || '指标', ref: '', status: '正常', value: v, abnormal: '' }
		})
		flush()
		return out.map((it) => {
			const abnormalText = String(it.abnormal || '').trim()
			const valueText = String(it.value || it.ref || '').trim()
			const hit = ABNORMAL_RE.test(abnormalText) || ABNORMAL_RE.test(valueText)
			return {
				name: it.name,
				ref: it.ref,
				status: hit ? (abnormalText || '异常') : '正常',
				value: it.abnormal ? (it.ref ? `${it.ref}｜${it.abnormal}` : it.abnormal) : (it.value || it.ref)
			}
		})
	}

	export default {
		data() {
			return {
				showUploadPopup: false,
				uploadOptions: [{
						key: 'camera',
						label: '拍照上传',
						icon: '/static/carmeUp.png'
					},
					{
						key: 'album',
						label: '相册上传',
						icon: '/static/photoUp.png'
					},
					{
						key: 'pdf',
						label: 'PDF上传',
						icon: '/static/pdfUp.png'
					}
				],
				reportList: [],
				reportLoading: false,
				exporting: false,
				streaming: false,
				streamItems: [],
				streamAdvice: '',
				parseResult: null,
				showDetailPopup: false,
				detailPopup: {
					subText: '',
					alertText: '',
					cells: []
				}
			}
		},
		onLoad() {
			this.loadHistory()
		},
		methods: {
			async loadHistory() {
				if (this.reportLoading) return
				this.reportLoading = true
				try {
					const data = await getReportHistory()
					const list = asArray(data)
					this.reportList = list.map((row) => {
						const dm = toDayMonth(pickText(row.createTime, row.create_time, row.date, row.reportDate))
						const items = asArray(row.items || row.itemsJson)
						const title = pickText(row.title, row.reportName, row.name) || '体检报告'
						return {
							reportId: row.reportId != null ? row.reportId : (row.id != null ? row.id : null),
							day: dm.day,
							month: dm.month,
							title,
							desc: items.length ? `${items.length}项指标` : pickText(row.summary, row.analysis, row.desc) || '已解析',
							_raw: row
						}
					})
				} catch (e) {
					this.reportList = []
				} finally {
					this.reportLoading = false
				}
			},
			onUpload() {
				this.showUploadPopup = true
			},
			closeUploadPopup() {
				this.showUploadPopup = false
			},
			onSelectUpload(type) {
				if (type === 'camera') {
					this.pickImage(['camera'])
					return
				}
				if (type === 'album') {
					this.pickImage(['album'])
					return
				}
				if (type === 'pdf') {
					this.pickPdf()
				}
			},
			pickImage(sourceType) {
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType,
					success: (res) => {
						const path = (res.tempFilePaths && res.tempFilePaths[0]) || ''
						if (!path) return
						this.closeUploadPopup()
						this.uploadAndAnalyze(path, sourceType[0] === 'camera' ? '拍照报告' : '相册报告')
					},
					fail: (err) => {
						if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			pickPdf() {
				// #ifdef MP-WEIXIN
				uni.chooseMessageFile({
					count: 1,
					type: 'file',
					extension: ['pdf'],
					success: (res) => {
						const file = (res.tempFiles && res.tempFiles[0]) || null
						if (!file) return
						this.closeUploadPopup()
						this.uploadAndAnalyze(file.path, (file.name || 'PDF报告').replace(/\.pdf$/i, ''))
					},
					fail: (err) => {
						if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
						uni.showToast({ title: '选择PDF失败', icon: 'none' })
					}
				})
				// #endif

				// #ifdef APP-PLUS
				this.closeUploadPopup()
				// App 端原生选 PDF 不稳定，走远端 H5 中转页上传，成功后通过 onPdfUploaded 回跳解析
				const htmlParam = `?html=${encodeURIComponent('https://fx.tailunkaluo.com/html/pdf-upload.html')}`
				uni.navigateTo({ url: `/pages/health/pdfWebview${htmlParam}` })
				return
				// #endif

				// #ifdef H5
				if (typeof uni.chooseFile === 'function') {
					uni.chooseFile({
						count: 1,
						extension: ['.pdf'],
						success: (res) => {
							const file = (res.tempFiles && res.tempFiles[0]) || null
							if (!file || !file.path) return
							this.closeUploadPopup()
							this.uploadAndAnalyze(file.path, (file.name || 'PDF报告').replace(/\.pdf$/i, ''))
						},
						fail: (err) => {
							if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
							uni.showToast({ title: '选择PDF失败', icon: 'none' })
						}
					})
					return
				}
				uni.showToast({ title: '当前端暂不支持PDF上传', icon: 'none' })
				// #endif
			},
			onPdfUploaded(fileUrl, name) {
				if (!fileUrl) return
				this.closeUploadPopup()
				this.analyzeByRemoteUrl(fileUrl, name || 'PDF报告')
			},
			async analyzeByRemoteUrl(fileUrl, title) {
				if (!fileUrl) return
				const reportTitle = title || '体检报告'
				this.streaming = true
				this.streamItems = []
				this.streamAdvice = ''
				this.parseResult = {
					category: '体检报告',
					title: 'AI解析中...',
					sourceTitle: reportTitle,
					path: fileUrl,
					reportId: null,
					items: [],
					advice: '',
					detail: null
				}
				uni.showLoading({ title: 'AI解析中...', mask: true })
				try {
					await this.runReportStream(fileUrl, reportTitle, fileUrl)
					uni.showToast({ title: '解析完成', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: (e && e.message) || '解析失败，请重试', icon: 'none' })
				} finally {
					uni.hideLoading()
					this.streaming = false
				}
			},
			async runReportStream(fileUrl, reportTitle, localPath) {
				let firstChunk = false
				const rawStream = []
				const hideMaskOnFirstChunk = () => {
					if (!firstChunk) { firstChunk = true; uni.hideLoading() }
				}
				await analyzeReportStreamByUrl(fileUrl, (evt) => {
					if (!evt) return
					if (evt.type === 'item') {
						hideMaskOnFirstChunk()
						const d = evt.data || {}
						const k = pickText(d.key, d.name, d.indicator)
						const v = pickText(d.value, d.detectValue, d.val)
						if (!k && !v) return
						rawStream.push({ k, v })
						this.streamItems = groupStreamItems(rawStream)
						this.parseResult.items = this.streamItems.slice()
						this.parseResult.title = `${this.streamItems.length}项指标识别中...`
					} else if (evt.type === 'delta') {
						hideMaskOnFirstChunk()
						this.streamAdvice += (evt.data && evt.data.content) || ''
						this.parseResult.advice = this.streamAdvice
					} else if (evt.type === 'done') {
						this.finishReportStream(evt.data || {}, reportTitle, localPath)
					} else if (evt.type === 'message') {
						const m = evt.data
						if (m && typeof m === 'object' && (m.items || m.analysis || m.reportId != null)) {
							this.finishReportStream(m.data !== undefined && typeof m.data === 'object' ? m.data : m, reportTitle, localPath)
						}
					} else if (evt.type === 'error') {
						throw new Error((evt.data && (evt.data.msg || evt.data.message)) || '解析失败')
					}
				})
			},
			finishReportStream(full, reportTitle, localPath) {
				const finalItems = asArray(full.items)
				if (finalItems.length) {
					this.streamItems = groupStreamItems(finalItems.map((it) => ({
						k: pickText(it.key, it.name, it.indicator),
						v: pickText(it.value, it.detectValue, it.val)
					})))
				}
				const analysis = pickText(full.analysis, full.advice, full.suggestion)
				if (analysis) this.streamAdvice = analysis
				const abnormalCount = this.streamItems.filter((it) => /偏高|偏低|异常|关注|阳性|\+|↑|↓/.test(it.status || '')).length
				this.parseResult = {
					category: pickText(full.category) || '体检报告',
					title: abnormalCount ? `${abnormalCount}项需要关注` : `${this.streamItems.length}项指标`,
					sourceTitle: reportTitle,
					path: localPath,
					reportId: full.reportId != null ? full.reportId : null,
					items: this.streamItems.slice(),
					advice: this.streamAdvice || '请结合专业医生意见解读。',
					detail: {
						subText: `${reportTitle} · ${this.streamItems.length}项指标`,
						alertText: abnormalCount ? `${abnormalCount}项需关注` : '指标总体平稳',
						cells: [
							{ label: '指标总数', value: `${this.streamItems.length}项`, icon: '/static/baogaoDate.png' },
							{ label: '关注项目', value: `${abnormalCount}项`, icon: '/static/yichang.png' },
							{ label: '正常项目', value: `${Math.max(this.streamItems.length - abnormalCount, 0)}项`, icon: '/static/success.png' },
							{ label: '报告来源', value: reportTitle, icon: '/static/tijiancenter.png' }
						]
					}
				}
				this.loadHistory()
			},
			async uploadAndAnalyze(localPath, title) {
				if (!localPath) return
				const reportTitle = title || '体检报告'
				this.streaming = true
				this.streamItems = []
				this.streamAdvice = ''
				this.parseResult = {
					category: '体检报告',
					title: 'AI解析中...',
					sourceTitle: reportTitle,
					path: localPath,
					reportId: null,
					items: [],
					advice: '',
					detail: null
				}
				uni.showLoading({ title: 'AI解析中...', mask: true })
				try {
					const imageUrl = await uploadToUrl(localPath)
					await this.runReportStream(imageUrl, reportTitle, localPath)
					uni.showToast({ title: '解析完成', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: (e && e.message) || '解析失败，请重试', icon: 'none' })
				} finally {
					uni.hideLoading()
					this.streaming = false
				}
			},
			applyAnalyzeResult(data, meta = {}) {
				if (!data || typeof data !== 'object') return
				let rawItems = data.items || data.itemList || data.resultList || data.itemsJson
				if (typeof rawItems === 'string') {
					try { rawItems = JSON.parse(rawItems) } catch (e) { rawItems = [] }
				}
				// reportText 可能是 "key: value\n..." 文本或 JSON 字符串，兜底解析
				if ((!rawItems || !rawItems.length) && typeof data.reportText === 'string' && data.reportText) {
					try {
						const parsed = JSON.parse(data.reportText)
						rawItems = Array.isArray(parsed) ? parsed : (parsed.items || parsed.list || [])
					} catch (e) {
						rawItems = String(data.reportText).split('\n').map((line) => {
							const m = String(line).split(/[:：]/)
							if (m.length < 2) return null
							return { name: m[0].trim(), value: m.slice(1).join(':').trim() }
						}).filter(Boolean)
					}
				}
				const items = asArray(rawItems).map((it) => {
					if (typeof it === 'string') return { name: it, ref: '', status: '', value: '' }
					return {
						name: pickText(it.name, it.key, it.indicator, it.title),
						ref: pickText(it.ref, it.reference, it.range, it.normalRange),
						status: pickText(it.status, it.level, it.flag, it.result),
						value: pickText(it.value, it.detectValue, it.val, it.content)
					}
				}).filter((it) => it.name || it.value)
				const analysis = pickText(data.analysis, data.advice, data.suggestion, data.conclusion, data.summary)
				const abnormalCount = items.filter((it) => /偏高|偏低|异常|关注|阳性/.test(it.status || '')).length
				this.parseResult = {
					category: pickText(data.category, data.type) || '体检报告',
					title: abnormalCount ? `${abnormalCount}项需要关注` : (pickText(data.title) || '解析完成'),
					sourceTitle: meta.title || '体检报告',
					path: meta.path,
					reportId: data.reportId != null ? data.reportId : (data.id != null ? data.id : null),
					items,
					advice: analysis || '请结合专业医生意见解读。',
					detail: {
						subText: `${meta.title || '体检报告'} · ${items.length}项指标`,
						alertText: abnormalCount ? `${abnormalCount}项需关注` : '指标总体平稳',
						cells: [
							{ label: '指标总数', value: `${items.length}项`, icon: '/static/baogaoDate.png' },
							{ label: '关注项目', value: `${abnormalCount}项`, icon: '/static/yichang.png' },
							{ label: '正常项目', value: `${Math.max(items.length - abnormalCount, 0)}项`, icon: '/static/success.png' },
							{ label: '报告来源', value: meta.title || '体检报告', icon: '/static/tijiancenter.png' }
						]
					}
				}
			},
			async onReportTap(item) {
				if (!item || item.reportId == null) return
				uni.showLoading({ title: '加载中...', mask: true })
				try {
					const data = await getReportDetail(item.reportId)
					this.applyAnalyzeResult(data, { title: item.title, path: '' })
				} catch (e) {
					uni.showToast({ title: '加载失败', icon: 'none' })
				} finally {
					uni.hideLoading()
				}
			},
			onParseDetail() {
				if (!this.parseResult || !this.parseResult.detail) return
				this.detailPopup = {
					subText: this.parseResult.detail.subText,
					alertText: this.parseResult.detail.alertText,
					cells: this.parseResult.detail.cells || []
				}
				this.showDetailPopup = true
			},
			closeDetailPopup() {
				this.showDetailPopup = false
			},
			onDetailAskAi() {
				this.closeDetailPopup()
				this.onParseAsk()
			},
			onParseDelete() {
				uni.showModal({
					title: '提示',
					content: '确定删除该解析报告吗？',
					success: (res) => {
						if (!res.confirm) return
						this.parseResult = null
					}
				})
			},
			async onParseShare() {
				if (this.exporting) return
				const reportId = this.parseResult && this.parseResult.reportId
				if (this.streaming || reportId == null || reportId === '') {
					uni.showToast({ title: '报告尚未生成，请稍后重试', icon: 'none' })
					return
				}
				this.exporting = true
				try {
					await exportAndDownloadReport([reportId])
				} finally {
					this.exporting = false
				}
			},
			onParseAsk() {
				uni.showToast({
					title: '继续提问',
					icon: 'none'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.hero-card {
		position: relative;
		overflow: hidden;
		padding: 40rpx 32rpx 36rpx;
		border-radius: 28rpx;
		background: linear-gradient(160deg, #1a3a8a 0%, #0d1a3a 45%, #0a1020 100%);
		border: 1rpx solid rgba(77, 163, 255, 0.18);
		box-sizing: border-box;
	}

	.hero-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.hero-body {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.hero-icon {
		width: 108rpx;
		height: 108rpx;
	}

	.hero-tag {
		margin-top: 20rpx;
		font-weight: 500;
		font-size: 26rpx;
		color: #2C89FF;
	}

	.hero-title {
		margin-top: 12rpx;
		font-size: 44rpx;
		font-weight: 700;
		line-height: 60rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.hero-desc {
		margin-top: 14rpx;
		padding: 0 20rpx;
		text-align: center;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.5);
	}

	.upload-btn {
		position: relative;
		width: 552rpx;
		height: 88rpx;
		margin-top: 36rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.feature-row {
		width: 326px;
		height: 39px;
		margin-top: 36rpx;
	}

	.parse-card {
		position: relative;
		overflow: hidden;
		margin-top: 28rpx;
		padding: 32rpx 28rpx;
		border-radius: 28rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		box-sizing: border-box;
	}

	.parse-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-body {
		position: relative;
		z-index: 1;
	}

	.parse-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
	}

	.parse-head-left {
		flex: 1;
		min-width: 0;
		padding-right: 16rpx;
		display: flex;
		flex-direction: column;
	}

	.parse-cate {
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.parse-title {
		margin-top: 8rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.parse-detail-btn {
		position: relative;
		height: 56rpx;
		padding: 0 24rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.parse-detail-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-detail-btn-text {
		position: relative;
		z-index: 1;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.parse-item {
		position: relative;
		margin-top: 20rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: rgba(255, 255, 255, 0.04);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}

	.parse-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-item-left {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.parse-item-name {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.parse-item-ref {
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.parse-item-status {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #6eb6ff;
	}

	.parse-item-value {
		position: relative;
		z-index: 1;
		margin-left: 16rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #ffffff;
		flex-shrink: 0;
		max-width: 55%;
		text-align: right;
		word-break: break-all;
	}

	.parse-item.long {
		flex-direction: column;
		align-items: flex-start;
	}

	.parse-item.long .parse-item-value {
		margin-left: 0;
		margin-top: 12rpx;
		max-width: 100%;
		text-align: left;
		font-weight: 500;
		font-size: 26rpx;
		line-height: 40rpx;
		white-space: normal;
	}

	.parse-advice {
		position: relative;
		overflow: hidden;
		margin-top: 20rpx;
		padding: 28rpx 24rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: flex-start;
		box-sizing: border-box;
	}

	.parse-advice-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-advice-icon {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.parse-advice-body {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.parse-advice-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.parse-advice-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.parse-actions {
		margin-top: 28rpx;
		display: flex;
		align-items: center;
	}

	.parse-btn {
		position: relative;
		flex: 1;
		height: 80rpx;
		border-radius: 20rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.parse-btn.gray {
		margin-right: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		border: 1rpx solid rgba(255, 255, 255, 0.12);
	}

	.parse-btn.outline {
		margin-right: 16rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.parse-btn.solid {
		background: transparent;
	}

	.parse-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.parse-btn-text {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		font-weight: 600;
		line-height: 36rpx;
		color: #ffffff;
	}

	.parse-btn-text.gray-text {
		color: rgba(255, 255, 255, 0.55);
	}

	.parse-btn-text.outline-text {
		color: #7eb6ff;
	}

	.section {
		margin-top: 40rpx;
	}

	.section-title {
		display: block;
		font-size: 32rpx;
		font-weight: 700;
		line-height: 44rpx;
		color: #ffffff;
	}

	.section-sub {
		display: block;
		margin-top: 6rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.report-card {
		margin-top: 24rpx;
		padding: 28rpx 24rpx;
		display: flex;
		align-items: center;
		border-radius: 24rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		box-sizing: border-box;
	}

	.report-date {
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

	.report-day {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 44rpx;
		color: #6eb6ff;
	}

	.report-month {
		margin-top: 2rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(110, 182, 255, 0.75);
	}

	.report-info {
		flex: 1;
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
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.report-arrow {
		width: 32rpx;
		height: 32rpx;
		margin-left: 12rpx;
		flex-shrink: 0;
		opacity: 0.45;
	}

	.footer-tip {
		margin-top: 48rpx;
		display: flex;
		align-items: flex-start;
		justify-content: center;
		padding: 0 12rpx;
	}

	.footer-tip-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 10rpx;
		margin-top: 4rpx;
		border-radius: 50%;
		background: rgba(77, 163, 255, 0.35);
		flex-shrink: 0;
	}

	.footer-tip-text {
		flex: 1;
		font-size: 22rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.privacy-badge {
		display: flex;
		justify-content: center;
		align-items: center;
		margin-top: 20rpx;
	}

	.privacy-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 8rpx;
	}

	.privacy-text {
		font-weight: 500;
		font-size: 26rpx;
		color: #FFFFFF;
	}

	.upload-mask {
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

	.upload-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
		animation: uploadSlideUp 0.28s ease-out;
	}

	@keyframes uploadSlideUp {
		from {
			transform: translateY(100%);
		}

		to {
			transform: translateY(0);
		}
	}

	.upload-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.upload-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 48rpx;
		box-sizing: border-box;
	}

	.upload-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.upload-popup-sub {
		display: block;
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.upload-options {
		margin-top: 36rpx;
		display: flex;
		align-items: stretch;
		justify-content: space-between;
	}

	.upload-option {
		position: relative;
		width: 30%;
		padding: 28rpx 12rpx 24rpx;
		height: 234rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
		display: flex;
		flex-direction: column;
		align-items: center;
		box-sizing: border-box;
	}

	.upload-option-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.upload-option-icon {
		position: relative;
		z-index: 1;
		width: 141.94rpx;
		height: 141.94rpx;
	}

	.upload-option-text {
		position: relative;
		z-index: 1;
		margin-top: 16rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.detail-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2100;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.detail-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #0b0f1a;
		animation: detailSlideUp 0.28s ease-out;
	}

	@keyframes detailSlideUp {
		from {
			transform: translateY(100%);
		}
		to {
			transform: translateY(0);
		}
	}

	.detail-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.detail-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.detail-popup-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.detail-alert {
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

	.detail-alert-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-alert-icon {
		position: relative;
		z-index: 1;
		width: 40rpx;
		height: 40rpx;
		margin-right: 12rpx;
		flex-shrink: 0;
	}

	.detail-alert-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #4da3ff;
	}

	.detail-grid {
		margin-top: 24rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.detail-grid-item {
		position: relative;
		overflow: hidden;
		width: 48%;
		margin-bottom: 16rpx;
		padding: 24rpx 20rpx;
		border-radius: 20rpx;
		background: rgba(26, 27, 37, 0.95);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
	}

	.detail-grid-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-grid-icon {
		position: relative;
		z-index: 1;
		width: 40rpx;
		height: 40rpx;
		margin-right: 16rpx;
	}

	.detail-grid-label {
		position: relative;
		z-index: 1;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.detail-grid-value {
		position: relative;
		z-index: 1;
		margin-top: 8rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #ffffff;
	}

	.detail-actions {
		margin-top: 32rpx;
		display: flex;
		align-items: center;
	}

	.detail-btn {
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

	.detail-btn.outline {
		margin-right: 20rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.detail-btn.solid {
		background: transparent;
	}

	.detail-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.detail-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.detail-btn-text.outline-text {
		color: #7eb6ff;
	}
</style>