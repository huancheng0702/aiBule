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

		<!-- 提交 -->
		<view class="footer">
			<view class="submit-btn" @click="onSubmit">
				<image class="submit-btn-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="submit-btn-text">提交问题反馈</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				content: '',
				images: [],
				maxImages: 3,
				faqList: [
					{
						id: 1,
						question: '戒指无法连接怎么办',
						answer: '确认蓝牙已开启、戒指电量充足，并在设备管理中重新搜索',
						open: true
					},
					{
						id: 2,
						question: '数据为什么没有同步？',
						answer: '请保持戒指与手机连接，并确保网络正常，稍后下拉刷新数据',
						open: false
					}
				]
			}
		},
		methods: {
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
			onSubmit() {
				const text = (this.content || '').trim()
				if (!text) {
					uni.showToast({ title: '请描述你遇到的问题', icon: 'none' })
					return
				}
				uni.showToast({ title: '反馈已提交', icon: 'none' })
				setTimeout(() => {
					uni.navigateBack()
				}, 500)
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
</style>
