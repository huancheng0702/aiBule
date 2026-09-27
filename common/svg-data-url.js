/**
 * 将 SVG 标记转为 App 可用的 image data URL。
 * uni-app App 端模板不支持 <svg>，H5 可用，故统一走 image 渲染。
 */
export function toSvgDataUrl(innerMarkup, width, height, attrs = '') {
	const w = Math.max(1, Math.round(Number(width) || 1))
	const h = Math.max(1, Math.round(Number(height) || 1))
	const svg =
		`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" ${attrs}>` +
		`${innerMarkup || ''}` +
		`</svg>`
	return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}

export function escapeAttr(value) {
	return String(value == null ? '' : value)
		.replace(/&/g, '&amp;')
		.replace(/"/g, '&quot;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
}
