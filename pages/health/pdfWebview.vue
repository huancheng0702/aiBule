<template>
	<view class="page">
		<web-view :src="src" @message="onMessage"></web-view>
	</view>
</template>

<script>
	import { LOGIN_TOKEN_KEY } from '@/common/auth.js'
	import { buildApiUrl } from '@/common/http.js'

	export default {
		data() {
			return { src: '', finished: false }
		},
		onLoad(query) {
			const base = query && query.html
				? decodeURIComponent(query.html)
				: 'https://fx.tailunkaluo.com/html/pdf-upload.html'
			const uploadUrl = encodeURIComponent(buildApiUrl('/appApi/file/upload'))
			const token = encodeURIComponent(uni.getStorageSync(LOGIN_TOKEN_KEY) || '')
			const sep = base.indexOf('?') > -1 ? '&' : '?'
			this.src = `${base}${sep}uploadUrl=${uploadUrl}&token=${token}&auto=1&t=${Date.now()}`
		},
		methods: {
			onMessage(e) {
				const d = ((e && e.detail && e.detail.data) || [])[0] || (e && e.detail) || {}
				const url = d.fileUrl || d.url || ''
				if (!url || this.finished) return
				this.finished = true
				const pages = getCurrentPages()
				const prev = pages.length > 1 ? pages[pages.length - 2] : null
				if (prev && prev.$vm && typeof prev.$vm.onPdfUploaded === 'function') {
					prev.$vm.onPdfUploaded(url, d.name || 'PDF报告')
				}
				uni.navigateBack()
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #0b0f1a; }
</style>
