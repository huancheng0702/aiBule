import { postSse, postSseEvents } from '@/common/sse.js'
import { downloadFileToLocal } from '@/common/api/file.js'

export function createAiSession() {
	return uni.$post('/appApi/ai/session')
}

export function getAiSessionList() {
	return uni.$get('/appApi/ai/session/list')
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
	let filePath
	let fileName
	try {
		const response = await exportReportHistory(reportIds)
		// 实际导出响应为 { fileName, fileUrl }，兼容尚未解包的 data。
		const data = response && (response.data || response)
		if (!data || typeof data.fileUrl !== 'string' || !/^https?:\/\//i.test(data.fileUrl)) {
			throw new Error('导出失败，未返回有效文件链接')
		}
		fileName = data.fileName || '体检报告解析记录.docx'
		uni.showLoading({ title: '下载中...', mask: true })
		filePath = await downloadFileToLocal(data.fileUrl, fileName)
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: String((error && (error.message || error.msg)) || '导出失败，请重试').slice(0, 40), icon: 'none' })
		return
	}
	uni.hideLoading()
	// #ifdef H5
	uni.showToast({ title: '已开始下载', icon: 'none' })
	// #endif
	// #ifndef H5
	// #ifndef APP-PLUS
	uni.showToast({ title: '已下载到本地', icon: 'success' })
	// #endif
	// #endif
	// #ifdef APP-PLUS
	const openFile = () => plus.runtime.openFile(filePath, {}, () => {
		uni.showToast({ title: '文件已保存，请安装支持该格式的应用', icon: 'none' })
	})
	try {
		plus.share.sendWithSystem({ type: 'file', files: [filePath] }, () => {}, (error) => {
			if (!/cancel|取消/i.test((error && error.message) || '')) openFile()
		})
	} catch (error) {
		openFile()
	}
	// #endif
	return filePath
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
