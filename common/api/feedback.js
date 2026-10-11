export function submitFeedback(data) {
	return uni.$post('/appApi/feedback/submit', data)
}
