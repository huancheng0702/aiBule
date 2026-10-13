<template>
	<view class="page">
		<scroll-view class="scroll" scroll-y>
			<rich-text v-if="html" class="rich" :nodes="html"></rich-text>
			<text v-else-if="plain" class="plain">{{ plain }}</text>
			<text v-else class="empty">{{ loading ? '加载中...' : '暂无内容' }}</text>
		</scroll-view>
	</view>
</template>

<script>
	import { getContent } from '@/common/api/content.js'

	export default {
		data() {
			return {
				contentId: '',
				loading: false,
				webUrl: '',
				html: '',
				plain: ''
			}
		},
		onLoad(options) {
			this.contentId = options.contentId
			this.loadContent()
		},
		methods: {
			setNavTitle(title) {
				const t = String(title || '').trim()
				if (!t) return
				uni.setNavigationBarTitle({ title: t })
			},
			async loadContent() {
				if (!this.contentId) {
					this.plain = '内容不存在'
					return
				}
				this.loading = true
				try {
					const data = await getContent(this.contentId)
					const info = data && typeof data === 'object' ? data : {}
					// 用户协议 / 隐私政策标题均以接口为准
					this.setNavTitle(info.title || info.contentTitle || info.name)

					const url = info.url || info.linkUrl || info.h5Url || info.contentUrl || ''
					const html = info.content || info.contentHtml || info.html || ''
					const text = info.contentText || info.text || ''

					if (url && /^https?:\/\//i.test(String(url))) {
						this.webUrl = String(url)
						this.html = ''
						this.plain = ''
						return
					}
					this.webUrl = ''
					if (html) {
						this.html = String(html)
						this.plain = ''
					} else {
						this.html = ''
						this.plain = text ? String(text) : ''
					}
				} catch (e) {
					this.webUrl = ''
					this.html = ''
					this.plain = ''
				} finally {
					this.loading = false
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		background: #000000;
		box-sizing: border-box;
	}

	.scroll {
		height: 100%;
		padding: 24rpx 32rpx 48rpx;
		box-sizing: border-box;
	}

	.rich,
	.plain {
		font-size: 28rpx;
		line-height: 44rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.empty {
		display: block;
		margin-top: 80rpx;
		font-size: 26rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.4);
		text-align: center;
	}
</style>
