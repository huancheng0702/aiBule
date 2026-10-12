import { LOGIN_TOKEN_KEY } from '@/common/auth.js'
import { buildApiUrl } from '@/common/http.js'

function parseJsonSafe(text) {
	try {
		return JSON.parse(text)
	} catch (e) {
		return null
	}
}

function pickText(obj) {
	if (obj == null) return ''
	if (typeof obj === 'string' || typeof obj === 'number') return String(obj)
	const keys = ['delta', 'content', 'text', 'message', 'chunk', 'answer', 'output', 'v']
	for (let i = 0; i < keys.length; i++) {
		const val = obj[keys[i]]
		if (typeof val === 'string' && val) return val
	}
	if (obj.data != null && obj.data !== obj) {
		return pickText(obj.data)
	}
	return ''
}

function pickSessionId(obj) {
	if (!obj || typeof obj !== 'object') return ''
	const id = obj.sessionId || obj.session_id || (obj.data && (obj.data.sessionId || obj.data.session_id))
	return id == null ? '' : String(id)
}

export function interpretSseEvent(eventName, data) {
	const event = (eventName || 'message').toLowerCase()
	const raw = data == null ? '' : String(data).trim()
	if (event === 'error') {
		const parsed = parseJsonSafe(raw)
		return { error: (parsed && (parsed.msg || parsed.message || parsed.error)) || raw || '流式请求失败' }
	}
	if (event === 'done' || event === 'end' || event === 'complete' || event === 'finish' || raw === '[DONE]') {
		return { done: true }
	}
	if (!raw) {
		return {}
	}
	if (event === 'session' || event === 'sessionid') {
		const parsed = parseJsonSafe(raw)
		return { sessionId: pickSessionId(parsed) || raw }
	}
	const parsed = parseJsonSafe(raw)
	if (parsed && typeof parsed === 'object') {
		const code = parsed.code
		if (code != null && [0, 1, 200].indexOf(Number(code)) === -1) {
			return { error: parsed.msg || parsed.message || '流式请求失败', sessionId: pickSessionId(parsed) }
		}
		return {
			delta: pickText(parsed),
			sessionId: pickSessionId(parsed),
			done: parsed.done === true || parsed.finished === true
		}
	}
	return { delta: raw }
}

export function createSseParser(onEvent) {
	let buffer = ''
	let eventName = 'message'
	function handleLine(line) {
		if (line === '') {
			eventName = 'message'
			return
		}
		if (line.indexOf('event:') === 0) {
			eventName = line.slice(6).trim()
			return
		}
		if (line.indexOf('id:') === 0 || line.indexOf('retry:') === 0 || line.indexOf(':') === 0) {
			return
		}
		if (line.indexOf('data:') === 0) {
			onEvent(interpretSseEvent(eventName, line.slice(5).replace(/^\s/, '')))
			return
		}
		onEvent(interpretSseEvent(eventName, line))
	}
	return {
		push(chunk) {
			if (!chunk) return
			buffer += String(chunk).replace(/\r\n/g, '\n').replace(/\r/g, '\n')
			let idx = buffer.indexOf('\n')
			while (idx !== -1) {
				const line = buffer.slice(0, idx)
				buffer = buffer.slice(idx + 1)
				handleLine(line)
				idx = buffer.indexOf('\n')
			}
		},
		flush() {
			if (buffer.trim()) {
				handleLine(buffer.trim())
				buffer = ''
			}
			eventName = 'message'
		}
	}
}

function buildHeaders() {
	const headers = {
		'Content-Type': 'application/json',
		Accept: 'text/event-stream'
	}
	const token = uni.getStorageSync(LOGIN_TOKEN_KEY)
	if (token) {
		headers.token = token
	}
	return headers
}

function streamWithFetch(url, { body, headers, onChunk, signal }) {
	return fetch(url, {
		method: 'POST',
		headers,
		body,
		cache: 'no-store',
		signal
	}).then((res) => {
		if (!res.ok) {
			return res.text().then((text) => {
				const parsed = parseJsonSafe(text)
				const msg = (parsed && (parsed.msg || parsed.message)) || text || ('HTTP ' + res.status)
				return Promise.reject(new Error(msg))
			})
		}
		if (!res.body || !res.body.getReader) {
			return res.text().then((text) => {
				onChunk(text)
			})
		}
		const reader = res.body.getReader()
		const decoder = new TextDecoder('utf-8')
		function readNext() {
			return reader.read().then(({ done, value }) => {
				if (done) {
					const tail = decoder.decode()
					if (tail) onChunk(tail)
					return
				}
				onChunk(decoder.decode(value, { stream: true }))
				return readNext()
			})
		}
		return readNext()
	})
}

function streamWithPlusXhr(url, { body, headers, onChunk }) {
	return new Promise((resolve, reject) => {
		function start() {
			const xhr = new plus.net.XMLHttpRequest()
			let last = 0
			function consume() {
				const text = xhr.responseText || ''
				if (text.length > last) {
					onChunk(text.slice(last))
					last = text.length
				}
			}
			xhr.timeout = 0
			xhr.onprogress = consume
			xhr.onreadystatechange = function() {
				consume()
				if (xhr.readyState === 4) {
					if (xhr.status >= 200 && xhr.status < 300) {
						resolve()
					} else {
						const parsed = parseJsonSafe(xhr.responseText || '')
						reject(new Error((parsed && (parsed.msg || parsed.message)) || ('HTTP ' + xhr.status)))
					}
				}
			}
			xhr.onerror = function() {
				reject(new Error('网络异常'))
			}
			xhr.ontimeout = function() {
				reject(new Error('连接超时'))
			}
			xhr.open('POST', url)
			Object.keys(headers).forEach((key) => {
				xhr.setRequestHeader(key, headers[key])
			})
			xhr.send(body)
		}
		if (typeof plus !== 'undefined' && plus.net && plus.net.XMLHttpRequest) {
			start()
			return
		}
		document.addEventListener('plusready', start)
	})
}

function canUsePlusXhr() {
	// #ifdef APP-PLUS
	return true
	// #endif
	return false
}

export function postSseEvents(path, payload, onEvent = () => {}) {
	const url = buildApiUrl(path)
	const headers = buildHeaders()
	const body = JSON.stringify(payload || {})
	let buffer = ''
	function dispatchFrame(frame) {
		const lines = String(frame || '').split('\n')
		let name = 'message'
		const dataLines = []
		for (let i = 0; i < lines.length; i++) {
			const line = lines[i]
			if (!line || line.charAt(0) === ':') continue
			if (line.indexOf('event:') === 0) { name = line.slice(6).trim().toLowerCase() || 'message'; continue }
			if (line.indexOf('id:') === 0 || line.indexOf('retry:') === 0) continue
			if (line.indexOf('data:') === 0) { dataLines.push(line.slice(5).replace(/^\s/, '')); continue }
		}
		if (!dataLines.length) return
		const text = dataLines.join('\n')
		let parsed = null
		try { parsed = text ? JSON.parse(text) : null } catch (e) { parsed = null }
		if (name === 'error') {
			onEvent({ type: 'error', data: parsed || { msg: text || '解析失败' }, raw: text })
		} else if (name === 'item') {
			onEvent({ type: 'item', data: parsed || { key: text, value: '' }, raw: text })
		} else if (name === 'delta') {
			const content = parsed != null && typeof parsed === 'object'
				? (parsed.content != null ? String(parsed.content) : text)
				: text
			onEvent({ type: 'delta', data: { content }, raw: text })
		} else if (name === 'done' || name === 'end' || name === 'complete' || name === 'finish') {
			onEvent({ type: 'done', data: parsed || {}, raw: text })
		} else if (text === '[DONE]') {
			onEvent({ type: 'done', data: {}, raw: text })
		} else if (text) {
			const parsedBody = parsed
			if (parsedBody && typeof parsedBody === 'object') {
				const code = parsedBody.code
				if (code !== undefined && code !== null && code !== '' && [0, 1, 200].indexOf(Number(code)) === -1) {
					onEvent({ type: 'error', data: parsedBody, raw: text })
					return
				}
				if (parsedBody.done === true || parsedBody.finished === true ||
					(parsedBody.items && parsedBody.items.length) ||
					parsedBody.analysis || parsedBody.advice || parsedBody.reportId != null) {
					onEvent({ type: 'done', data: parsedBody.data !== undefined && typeof parsedBody.data === 'object' ? parsedBody.data : parsedBody, raw: text })
					return
				}
				if (parsedBody.key != null || parsedBody.name != null || parsedBody.indicator != null) {
					onEvent({ type: 'item', data: parsedBody, raw: text })
					return
				}
				if (parsedBody.content != null || parsedBody.delta != null || parsedBody.text != null) {
					const content = parsedBody.content != null ? String(parsedBody.content)
						: parsedBody.delta != null ? String(parsedBody.delta) : String(parsedBody.text)
					onEvent({ type: 'delta', data: { content }, raw: text })
					return
				}
			}
			onEvent({ type: 'message', data: parsedBody != null ? parsedBody : text, raw: text })
		}
	}
	const onChunk = (chunk) => {
		buffer += String(chunk || '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
		let idx = buffer.indexOf('\n\n')
		while (idx !== -1) {
			dispatchFrame(buffer.slice(0, idx))
			buffer = buffer.slice(idx + 2)
			idx = buffer.indexOf('\n\n')
		}
	}
	let task
	if (canUsePlusXhr()) {
		task = streamWithPlusXhr(url, { body, headers, onChunk })
	} else if (typeof fetch === 'function') {
		task = streamWithFetch(url, { body, headers, onChunk })
	} else {
		return Promise.reject(new Error('当前环境不支持流式'))
	}
	return task.then(() => {
		if (buffer.trim()) dispatchFrame(buffer)
		buffer = ''
	})
}

export function postSse(path, payload, handlers = {}) {
	const url = buildApiUrl(path)
	const headers = buildHeaders()
	const body = JSON.stringify(payload || {})
	const onDelta = handlers.onDelta || function() {}
	const onSession = handlers.onSession || function() {}
	let sessionSent = false
	let streamError = null
	const parser = createSseParser((evt) => {
		if (!evt) return
		if (evt.error) {
			streamError = new Error(evt.error)
			return
		}
		if (evt.sessionId && !sessionSent) {
			sessionSent = true
			onSession(evt.sessionId)
		}
		if (evt.delta) {
			onDelta(evt.delta)
		}
	})
	const onChunk = (chunk) => parser.push(chunk)
	let task
	if (canUsePlusXhr()) {
		task = streamWithPlusXhr(url, { body, headers, onChunk })
	} else if (typeof fetch === 'function') {
		const controller = typeof AbortController === 'function' ? new AbortController() : null
		task = streamWithFetch(url, {
			body,
			headers,
			onChunk,
			signal: controller ? controller.signal : undefined
		})
	} else {
		return Promise.reject(new Error('当前环境不支持流式'))
	}
	return task.then(() => {
		parser.flush()
		if (streamError) {
			return Promise.reject(streamError)
		}
	})
}
