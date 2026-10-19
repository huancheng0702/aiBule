<template>
	<view class="page">
		<!-- 常见问题 -->
		<view class="section">
			<text class="section-label">常见问题</text>
			<view class="menu-card">
				<view
					v-for="(item, index) in faqList"
					:key="item.id"
					class="faq-item"
					:class="{ 'no-border': index === faqList.length - 1 }"
				>
					<view class="faq-head" @click="toggleFaq(index)">
						<text class="faq-title">{{ item.question }}</text>
						<image
							class="faq-arrow"
							:class="{ open: item.open }"
							src="/static/rightLitterIcon.png"
							mode="aspectFit"
						></image>
					</view>
					<text v-if="item.open" class="faq-answer">{{ item.answer }}</text>
				</view>
				<view v-if="!faqList.length && !faqLoading" class="faq-empty">
					<text class="faq-empty-text">暂无常见问题</text>
				</view>
				<view v-else-if="faqLoading" class="faq-empty">
					<text class="faq-empty-text">加载中...</text>
				</view>
			</view>
		</view>

		<!-- 问题描述 -->
		<view class="section">
			<text class="section-label">问题描述</text>
			<view class="desc-card">
				<textarea
					class="desc-input"
					v-model="content"
					maxlength="500"
					placeholder="请描述你遇到的问题"
					placeholder-class="desc-placeholder"
				/>
			</view>
		</view>

		<!-- 上传图片 -->
		<view class="section">
			<text class="section-label">上传图片</text>
			<view class="upload-row">
				<view v-for="(src, index) in images" :key="src + index" class="upload-item">
					<image class="upload-preview" :src="src" mode="aspectFill" @click="previewImage(index)"></image>
					<view class="upload-remove" @click.stop="removeImage(index)">×</view>
				</view>
				<view v-if="images.length < maxImages" class="upload-add" @click="chooseImage">
					<text class="upload-plus">+</text>
				</view>
			</view>
		</view>

		<!-- 联系方式 -->
		<view class="section">
			<text class="section-label">联系方式（选填）</text>
			<view class="desc-card contact-card">
				<input
					class="contact-input"
					v-model="contact"
					maxlength="50"
					placeholder="手机号或微信号，方便我们联系你"
					placeholder-class="desc-placeholder"
				/>
			</view>
		</view>

		<!-- 提交 -->
		<view class="footer">
			<view class="submit-btn" :class="{ disabled: submitting }" @click="onSubmit">
				<image class="submit-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="submit-btn-text">{{ submitting ? '提交中...' : '提交问题反馈' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getFaqList } from '@/common/api/faq.js'
	import { submitFeedback } from '@/common/api/feedback.js'
	import { uploadFile } from '@/common/api/file.js'

	export default {
		data() {
			return {
				content: '',
				contact: '',
				images: [],
				maxImages: 3,
				submitting: false,
				faqList: [],
				faqLoading: false
			}
		},
		onShow() {
			this.loadFaqList()
		},
		methods: {
			async loadFaqList() {
				if (this.faqLoading) return
				this.faqLoading = true
				try {
					const data = await getFaqList()
					const list = Array.isArray(data)
						? data
						: data && Array.isArray(data.list)
							? data.list
							: []
					this.faqList = list
						.map((item, i) => ({
							id: item.faqId != null ? item.faqId : item.id != null ? item.id : i,
							question: item.question || '',
							answer: item.answer || '',
							category: item.category || '',
							sort: Number(item.sort != null ? item.sort : i + 1),
							open: false
						}))
						.filter((item) => item.question)
						.sort((a, b) => a.sort - b.sort)
					// 默认展开第一条
					if (this.faqList.length) {
						this.$set(this.faqList[0], 'open', true)
					}
				} catch (e) {
					console.log('[helpFeedback] faq list failed', e && (e.msg || e.message))
					this.faqList = []
				} finally {
					this.faqLoading = false
				}
			},
			toggleFaq(index) {
				const item = this.faqList[index]
				if (!item) return
				this.$set(item, 'open', !item.open)
			},
			chooseImage() {
				const remain = this.maxImages - this.images.length
				if (remain <= 0) return
				uni.chooseImage({
					count: remain,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => {
						const paths = (res.tempFilePaths || []).slice(0, remain)
						this.images = this.images.concat(paths)
					}
				})
			},
			removeImage(index) {
				this.images.splice(index, 1)
			},
			previewImage(index) {
				uni.previewImage({
					current: this.images[index],
					urls: this.images
				})
			},
			async onSubmit() {
				const text = (this.content || '').trim()
				if (!text) {
					uni.showToast({ title: '请描述你遇到的问题', icon: 'none' })
					return
				}
				if (this.submitting) return
				this.submitting = true
				uni.showLoading({ title: '提交中...', mask: true })
				try {
					// 先把本地图片上传，拿到 URL 再逗号拼接提交
					let imageUrls = ''
					if (this.images.length) {
						const uploaded = []
						for (let i = 0; i < this.images.length; i++) {
							try {
								const res = await uploadFile(this.images[i])
								const url = typeof res === 'string' ? res : (res && (res.url || res.fileUrl || res.path))
								if (url) uploaded.push(url)
							} catch (e) {
								console.log('[helpFeedback] upload image failed', i, e && (e.msg || e.message))
							}
						}
						imageUrls = uploaded.join(',')
					}
					await submitFeedback({
						content: text,
						images: imageUrls || undefined,
						contact: (this.contact || '').trim() || undefined
					})
					uni.hideLoading()
					uni.showToast({ title: '反馈已提交', icon: 'none' })
					this.content = ''
					this.images = []
					setTimeout(() => {
						uni.navigateBack()
					}, 800)
				} catch (e) {
					uni.hideLoading()
					uni.showToast({
						title: String((e && (e.msg || e.message)) || '提交失败，请重试').slice(0, 30),
						icon: 'none'
					})
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		height: 100vh;
		padding: 24rpx 28rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.section {
		margin-bottom: 36rpx;
	}

	.section-label {
		display: block;
		margin-bottom: 16rpx;
		padding-left: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.menu-card {
		background: #1a1b25;
		border-radius: 24rpx;
		overflow: hidden;
		padding: 0 28rpx;
	}

	.faq-item {
		padding: 28rpx 0;
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
	}

	.faq-item.no-border {
		border-bottom: none;
	}

	.faq-head {
		display: flex;
		align-items: center;
	}

	.faq-title {
		flex: 1;
		min-width: 0;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.faq-arrow {
		width: 24rpx;
		height: 24rpx;
		margin-left: 16rpx;
		flex-shrink: 0;
		opacity: 0.45;
		transform: rotate(90deg);
	}

	.faq-arrow.open {
		transform: rotate(-90deg);
	}

	.faq-answer {
		display: block;
		margin-top: 16rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.faq-empty {
		padding: 48rpx 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.faq-empty-text {
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.35);
	}

	.desc-card {
		background: #1a1b25;
		border-radius: 24rpx;
		padding: 28rpx;
		box-sizing: border-box;
	}

	.desc-input {
		width: 100%;
		height: 220rpx;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
		box-sizing: border-box;
	}

	.desc-placeholder {
		color: #636366;
	}

	.contact-card {
		padding: 0 28rpx;
		height: 96rpx;
		display: flex;
		align-items: center;
	}

	.contact-input {
		flex: 1;
		width: 100%;
		height: 96rpx;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #ffffff;
	}

	.upload-row {
		display: flex;
		flex-wrap: wrap;
	}

	.upload-item,
	.upload-add {
		position: relative;
		width: 160rpx;
		height: 160rpx;
		margin-right: 20rpx;
		margin-bottom: 20rpx;
		border-radius: 20rpx;
		background: #1a1b25;
		overflow: hidden;
		box-sizing: border-box;
	}

	.upload-preview {
		width: 100%;
		height: 100%;
	}

	.upload-remove {
		position: absolute;
		top: 0;
		right: 0;
		width: 40rpx;
		height: 40rpx;
		background: rgba(0, 0, 0, 0.55);
		color: #ffffff;
		font-size: 28rpx;
		line-height: 40rpx;
		text-align: center;
	}

	.upload-add {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.upload-plus {
		font-size: 64rpx;
		line-height: 64rpx;
		color: rgba(255, 255, 255, 0.35);
		font-weight: 300;
	}

	.footer {
		margin-top: 20rpx;
		padding-bottom: 20rpx;
	}

	.submit-btn {
		position: relative;
		width: 100%;
		height: 96rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.submit-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.submit-btn-text {
		position: relative;
		z-index: 1;
		font-size: 32rpx;
		font-weight: 600;
		line-height: 44rpx;
		color: #ffffff;
	}

	.submit-btn.disabled {
		opacity: 0.6;
	}
</style>
