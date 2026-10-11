export function getContent(contentId) {
	return uni.$get('/appApi/content/' + contentId)
}
