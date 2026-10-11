export function getBannerList() {
	return uni.$get('/appApi/bannerInfo/list')
}
