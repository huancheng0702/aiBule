import { postSse, postSseEvents } from '@/common/sse.js'
import { downloadFileToLocal } from '@/common/api/file.js'

export function createAiSession() {
	return uni.$post('/appApi/ai/session')
}

export function getAiSessionList(params) {
	return uni.$get('/appApi/ai/session/list', params)
}

export function getAiMessages(sessionId) {
	return uni.$get('/appApi/ai/session/' + sessionId + '/messages')
}

export function deleteAiSession(sessionId) {
	return uni.$delete('/appApi/ai/session/' + sessionId)
}

export function getAiQuickQuestions() {
	return uni.$get('/appApi/ai/quickQuestions')
}

export function getReportHistory() {
	return uni.$get('/appApi/ai/report/history')
}

export function getReportDetail(reportId) {
	return uni.$get('/appApi/ai/report/history/' + reportId)
}

export function analyzeReportByUrl(imageUrl) {
	return uni.$post('/appApi/ai/report/analyzeByUrl?imageUrl=' + encodeURIComponent(imageUrl || ''))
}

export function analyzeReportText(reportText) {
	return uni.$post('/appApi/ai/report/analyzeText?reportText=' + encodeURIComponent(reportText || ''))
}

export function exportReportHistory(reportIds) {
	const q = reportIds && reportIds.length ? '?reportIds=' + encodeURIComponent([].concat(reportIds).join(',')) : ''
	return uni.$post('/appApi/ai/report/export' + q)
}

export async function exportAndDownloadReport(reportIds) {
	uni.showLoading({ title: '导出中...', mask: true })
	let fileUrl = ''
	let fileName = ''
	try {
		const response = await exportReportHistory(reportIds)
		// 接口返回 { code:200, msg, data:{ fileName, fileUrl } }；http 层已解包到 data，
		// 这里再兜底兼容一次未解包的情形。
		const data = response && (response.data || response)
		if (!data || typeof data.fileUrl !== 'string' || !/^https?:\/\//i.test(data.fileUrl)) {
			throw new Error('导出失败，未返回有效文件链接')
		}
		fileName = data.fileName || '体检报告解析记录.docx'
		fileUrl = data.fileUrl
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: String((error && (error.message || error.msg)) || '导出失败，请重试').slice(0, 40), icon: 'none' })
		return ''
	}

	// #ifdef APP-PLUS
	// 文件名带中文，iOS downloader / 浏览器打开前先编码
	const safeUrl = (() => {
		try { return encodeURI(fileUrl) } catch (e) { return fileUrl }
	})()
	// Android：下载 → 保存到本地 → 打开查看
	if (plus.os.name === 'Android') {
		uni.hideLoading()
		uni.showLoading({ title: '下载中...', mask: true })
		return new Promise((resolve) => {
			uni.downloadFile({
				url: safeUrl,
				success: (res) => {
					if (res.statusCode !== 200 || !res.tempFilePath) {
						uni.hideLoading()
						uni.showToast({ title: '下载失败，请重试', icon: 'none' })
						resolve('')
						return
					}
					uni.saveFile({
						tempFilePath: res.tempFilePath,
						success: (saveRes) => {
							uni.hideLoading()
							setTimeout(() => {
								uni.openDocument({
									filePath: saveRes.savedFilePath,
									fileType: 'docx',
									fail: () => uni.showToast({ title: '打开失败，请安装WPS/Office', icon: 'none' })
								})
							}, 500)
							resolve(saveRes.savedFilePath)
						},
						fail: () => {
							uni.hideLoading()
							uni.showToast({ title: '保存失败，请重试', icon: 'none' })
							resolve('')
						}
					})
				},
				fail: () => {
					uni.hideLoading()
					uni.showToast({ title: '下载失败，请重试', icon: 'none' })
					resolve('')
				}
			})
		})
	}
	// iOS：交给系统浏览器打开下载
	uni.hideLoading()
	uni.showLoading({ title: '下载中...', mask: true })
	try {
		plus.runtime.openURL(safeUrl)
	} catch (e) {}
	setTimeout(() => {
		uni.hideLoading()
		uni.showToast({ title: '正在浏览器中下载', icon: 'none' })
	}, 1000)
	return fileUrl
	// #endif

	// #ifndef APP-PLUS
	uni.hideLoading()
	let filePath = ''
	try {
		filePath = await downloadFileToLocal(fileUrl, fileName)
	} catch (error) {
		uni.showToast({ title: '下载失败，请重试', icon: 'none' })
		return ''
	}
	// #ifdef H5
	uni.showToast({ title: '已开始下载', icon: 'none' })
	// #endif
	// #ifndef H5
	uni.showToast({ title: '已下载到本地', icon: 'success' })
	// #endif
	return filePath
	// #endif
}

export function analyzeReportStreamByUrl(imageUrl, onEvent) {
	return postSseEvents('/appApi/ai/report/analyzeByAi/stream?imageUrl=' + encodeURIComponent(imageUrl || ''), {}, onEvent)
}

export function chatAi(data) {
	return uni.$post('/appApi/ai/chat', data)
}

export function chatAiStream(data, handlers) {
	return postSse('/appApi/ai/chat/stream', data, handlers).catch((err) => {
		if (err && err.message === '当前环境不支持流式') {
			return chatAi(data).then((res) => {
				const text = (res && (res.content || res.answer || res.message || res.delta)) || ''
				const sessionId = res && (res.sessionId || res.session_id)
				if (sessionId && handlers && handlers.onSession) handlers.onSession(sessionId)
				if (text && handlers && handlers.onDelta) handlers.onDelta(text)
				return res
			})
		}
		return Promise.reject(err)
	})
}
