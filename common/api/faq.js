export function getFaqList(category) {
	const params = {}
	if (category) params.category = category
	return uni.$get('/appApi/faq/list', params)
}
