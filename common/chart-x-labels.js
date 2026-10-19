const WEEK_TEXT = ['\u5468\u4e00', '\u5468\u4e8c', '\u5468\u4e09', '\u5468\u56db', '\u5468\u4e94', '\u5468\u516d', '\u5468\u65e5']
const DAY_TEXT = ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
const MONTH_TEXT = ['1\u65e5', '6\u65e5', '11\u65e5', '16\u65e5', '21\u65e5', '26\u65e5']
const DAY_HOURS = [0, 4, 8, 12, 16, 20]

function pad2(n) {
	return n < 10 ? '0' + n : String(n)
}

/** parse labels like 2026-10-30 / 2026-10-30 08:00 / 10-30 08:00 */
function matchDate(raw) {
	const s = String(raw)
	let m = s.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})(?:[T ](\d{1,2}):(\d{2}))?/)
	if (m) {
		return {
			month: Number(m[2]),
			day: Number(m[3]),
			hour: m[4] == null ? null : Number(m[4]),
			minute: m[5] || '00'
		}
	}
	m = s.match(/^(\d{1,2})[-/](\d{1,2})(?:[T ](\d{1,2}):(\d{2}))?$/)
	if (m) {
		return {
			month: Number(m[1]),
			day: Number(m[2]),
			hour: m[3] == null ? null : Number(m[3]),
			minute: m[4] || '00'
		}
	}
	return null
}

function matchHour(raw) {
	const d = matchDate(raw)
	if (d && d.hour != null) return d.hour
	const m = String(raw).match(/(\d{1,2}):(\d{2})/)
	return m ? Number(m[1]) : null
}

function formatLabel(raw, period) {
	const s = String(raw == null ? '' : raw).trim()
	if (!s) return ''
	const d = matchDate(s)
	if (d) {
		if (period === 'day') return d.hour == null ? DAY_TEXT[0] : pad2(d.hour) + ':' + d.minute
		if (period === 'week') return d.month + '/' + d.day
		return d.day + '\u65e5'
	}
	const t = s.match(/^(\d{1,2}):(\d{2})$/)
	if (t) return pad2(Number(t[1])) + ':' + t[2]
	if (/^\d{1,2}$/.test(s)) {
		const n = Number(s)
		if (period === 'day') return pad2(n) + ':00'
		if (period === 'week' && n >= 1 && n <= 7) return WEEK_TEXT[n - 1]
		return s + '\u65e5'
	}
	return s
}

function evenIndexes(len, count) {
	if (len <= count) {
		const out = []
		for (let i = 0; i < len; i++) out.push(i)
		return out
	}
	const out = []
	for (let i = 0; i < count; i++) out.push(Math.round(i * (len - 1) / (count - 1)))
	return out.filter((v, i, arr) => i === 0 || v !== arr[i - 1])
}

/**
 * Format and thin out the server time labels for the trend chart X axis.
 * day   -> 00:00 / 04:00 / 08:00 / 12:00 / 16:00 / 20:00 / last point
 * week  -> M/D, one label per day of the week
 * month -> day-of-month, 6 evenly spaced labels including first and last
 */
export function pickXLabels(times, period) {
	const list = (times || []).filter((t) => t != null && String(t).trim() !== '')
	if (!list.length) {
		if (period === 'week') return WEEK_TEXT.slice()
		if (period === 'month') return MONTH_TEXT.slice()
		return DAY_TEXT.slice()
	}

	if (period === 'day' && list.length > 7) {
		const picked = []
		DAY_HOURS.forEach((h) => {
			for (let i = 0; i < list.length; i++) {
				if (matchHour(list[i]) === h) {
					picked.push(i)
					break
				}
			}
		})
		const lastIdx = list.length - 1
		if (picked[picked.length - 1] !== lastIdx) picked.push(lastIdx)
		if (picked.length > 1) return picked.map((i) => formatLabel(list[i], 'day'))
	}

	const size = period === 'month' ? 6 : 7
	return evenIndexes(list.length, size).map((i) => formatLabel(list[i], period))
}
