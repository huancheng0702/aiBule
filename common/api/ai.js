import { postSse } from '@/common/sse.js'

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
