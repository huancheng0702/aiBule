<template>
	<view class="page">
		<!-- 顶部引导卡片 -->
		<view class="hero-card">
			<!-- 卡片背景图，自行补充 src -->
			<image class="hero-bg" src="/static/aiBg.png" mode=""></image>
			<view class="hero-body">
				<!-- 顶部图标，自行补充 src -->
				<image class="hero-icon" src="/static/aiTopImg.png" mode=""></image>
				<text class="hero-tag">AI报告解读</text>
				<text class="hero-title">把复杂指标讲明白</text>
				<text class="hero-desc">支持体检单、检验单等演示场景；解析结果需结合专业医生意见</text>

				<image class="upload-btn" @click="onUpload" src="/static/yuntuBg.png" mode=""></image>

				<image class="feature-row" src="/static/aiCardBot.png" mode=""></image>
			</view>
		</view>

		<!-- 隐私承诺 -->
		<view class="privacy-badge">
			<!-- 盾牌图标，自行补充 src -->
			<image class="privacy-icon" src="/static/dunpai.png" mode=""></image>
			<text class="privacy-text">平台承诺保证隐私安全</text>
		</view>
		
		<!-- AI解析报告 -->
		<view v-if="parseResult" class="parse-card">
			<!-- 报告卡片背景图，自行补充 src -->
			<image class="parse-card-bg" mode="aspectFill"></image>
			<view class="parse-body">
				<view class="parse-head">
					<view class="parse-head-left">
						<text class="parse-cate">{{ parseResult.category }}</text>
						<text class="parse-title">{{ parseResult.title }}</text>
					</view>
					<view class="parse-detail-btn" @click="onParseDetail">
						<!-- 按钮背景图，自行补充 src -->
						<image class="parse-detail-btn-bg" src="/static/jiegouhua.png" mode="scaleToFill"></image>
						<text class="parse-detail-btn-text">结构化详情</text>
					</view>
				</view>

				<view
					v-for="(item, index) in parseResult.items"
					:key="index"
					class="parse-item"
				>
					<!-- 指标卡片背景图，自行补充 src -->
					<image class="parse-item-bg" mode="aspectFill"></image>
					<view class="parse-item-left">
						<text class="parse-item-name">{{ item.name }}</text>
						<text class="parse-item-ref">{{ item.ref }}</text>
						<text class="parse-item-status">{{ item.status }}</text>
					</view>
					<text class="parse-item-value">{{ item.value }}</text>
				</view>

				<view class="parse-advice">
					<!-- 建议背景图，自行补充 src -->
					<image class="parse-advice-bg" src="/static/heartCardBg.png" mode="aspectFill"></image>
					<!-- 建议图标，自行补充 src -->
					<image class="parse-advice-icon" src="/static/qushijianyi.png" mode="aspectFit"></image>
					<view class="parse-advice-body">
						<view class="parse-advice-title">健康与建议</view>
						<text class="parse-advice-desc">{{ parseResult.advice }}</text>
					</view>
				</view>

				<view class="parse-actions">
					<view class="parse-btn gray" @click="onParseDelete">
						<text class="parse-btn-text gray-text">删除</text>
					</view>
					<view class="parse-btn outline" @click="onParseShare">
						<text class="parse-btn-text outline-text">分享/导出</text>
					</view>
					<view class="parse-btn solid" @click="onParseAsk">
						<!-- 按钮背景图，自行补充 src -->
						<image class="parse-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
						<text class="parse-btn-text">继续提问</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 最近上传 -->
		<view class="section">
			<text class="section-title">最近上传</text>
			<text class="section-sub">历史报告</text>

			<view v-for="(item, index) in reportList" :key="index" class="report-card" @click="onReportTap(item)">
				<view class="report-date">
					<text class="report-day">{{ item.day }}</text>
					<text class="report-month">{{ item.month }}</text>
				</view>
				<view class="report-info">
					<text class="report-title">{{ item.title }}</text>
					<text class="report-desc">{{ item.desc }}</text>
				</view>
				<!-- 箭头图标，自行补充 src -->
				<image class="report-arrow" src="/static/rightLitterIcon.png" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 底部说明 -->
		<view class="footer-tip">
			<!-- 提示图标，自行补充 src -->
			<image class="footer-tip-icon" src="/static/jinggao.png" mode=""></image>
			<text class="footer-tip-text">档案内容仅供您本人使用，我们将严格保护您的隐私安全</text>
		</view>

		<!-- 选择上传方式弹窗 -->
		<view v-if="showUploadPopup" class="upload-mask" @click="closeUploadPopup">
			<view class="upload-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="upload-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="upload-popup-body">
					<text class="upload-popup-title">选择上传方式</text>
					<text class="upload-popup-sub">智能检测</text>

					<view class="upload-options">
						<view v-for="item in uploadOptions" :key="item.key" class="upload-option"
							@click="onSelectUpload(item.key)">
							<!-- 选项图标，自行补充 src -->
							<image class="upload-option-icon" :src="item.icon" mode=""></image>
							<text class="upload-option-text">{{ item.label }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 结构化报告详情弹窗 -->
		<view v-if="showDetailPopup" class="detail-mask" @click="closeDetailPopup">
			<view class="detail-popup" @click.stop>
				<!-- 弹窗背景图，自行补充 src -->
				<image class="detail-popup-bg" src="/static/bushuPopBg.png" mode=""></image>
				<view class="detail-popup-body">
					<text class="detail-popup-title">结构化报告详情</text>
					<text class="detail-popup-sub">{{ detailPopup.subText }}</text>

					<view class="detail-alert">
						<!-- 关注条背景图，自行补充 src -->
						<image class="detail-alert-bg" src="/static/heartCardBg.png" mode=""></image>
						<!-- 关注图标，自行补充 src -->
						<image class="detail-alert-icon" src="/static/dengjitu.png" mode="aspectFit"></image>
						<text class="detail-alert-text">{{ detailPopup.alertText }}</text>
					</view>

					<view class="detail-grid">
						<view v-for="(cell, index) in detailPopup.cells" :key="index" class="detail-grid-item">
							<!-- 指标卡片背景图，自行补充 src -->
							<image class="detail-grid-item-bg" mode="aspectFill"></image>
							<view class="" style="display: flex;justify-content: flex-start;align-items: center;">
								<image class="detail-grid-icon" :src="cell.icon" mode=""></image>
								<view class="detail-grid-label">{{ cell.label }}</view>
							</view>
							<view class="detail-grid-value">{{ cell.value }}</view>
						</view>
					</view>

					<view class="detail-actions">
						<view class="detail-btn outline" @click="closeDetailPopup">
							<text class="detail-btn-text outline-text">完成</text>
						</view>
						<view class="detail-btn solid" @click="onDetailAskAi">
							<!-- 按钮背景图，自行补充 src -->
							<image class="detail-btn-bg" src="/static/confirmBg.png" mode="scaleToFill"></image>
							<text class="detail-btn-text">继续问AI</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	function pad2(n) {
		return String(n).padStart(2, '0')
	}

	export default {
		data() {
			return {
				showUploadPopup: false,
				uploadOptions: [{
						key: 'camera',
						label: '拍照上传',
						icon: '/static/carmeUp.png'
					},
					{
						key: 'album',
						label: '相册上传',
						icon: '/static/photoUp.png'
					},
					{
						key: 'pdf',
						label: 'PDF上传',
						icon: '/static/pdfUp.png'
					}
				],
				reportList: [{
					day: '26',
					month: '8月',
					title: '年度体检报告',
					desc: '12项指标 · 2项关注'
				}],
				parseResult: null,
				showDetailPopup: false,
				detailPopup: {
					subText: '',
					alertText: '',
					cells: []
				}
			}
		},
		methods: {
			onUpload() {
				this.showUploadPopup = true
			},
			closeUploadPopup() {
				this.showUploadPopup = false
			},
			onSelectUpload(type) {
				if (type === 'camera') {
					this.pickImage(['camera'])
					return
				}
				if (type === 'album') {
					this.pickImage(['album'])
					return
				}
				if (type === 'pdf') {
					this.pickPdf()
				}
			},
			pickImage(sourceType) {
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType,
					success: (res) => {
						const path = (res.tempFilePaths && res.tempFilePaths[0]) || ''
						if (!path) return
						this.closeUploadPopup()
						this.addReport({
							title: sourceType[0] === 'camera' ? '拍照报告' : '相册报告',
							path,
							kind: 'image'
						})
					},
					fail: (err) => {
						if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			pickPdf() {
				// #ifdef MP-WEIXIN
				uni.chooseMessageFile({
					count: 1,
					type: 'file',
					extension: ['pdf'],
					success: (res) => {
						const file = (res.tempFiles && res.tempFiles[0]) || null
						if (!file) return
						this.closeUploadPopup()
						this.addReport({
							title: (file.name || 'PDF报告').replace(/\.pdf$/i, ''),
							path: file.path,
							kind: 'pdf'
						})
					},
					fail: (err) => {
						if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
						uni.showToast({
							title: '选择PDF失败',
							icon: 'none'
						})
					}
				})
				// #endif

				// #ifndef MP-WEIXIN
				if (typeof uni.chooseFile === 'function') {
					uni.chooseFile({
						count: 1,
						type: 'all',
						extension: ['.pdf'],
						success: (res) => {
							const file = (res.tempFiles && res.tempFiles[0]) || null
							if (!file) return
							this.closeUploadPopup()
							this.addReport({
								title: (file.name || 'PDF报告').replace(/\.pdf$/i, ''),
								path: file.path || '',
								kind: 'pdf'
							})
						},
						fail: (err) => {
							if (err && err.errMsg && err.errMsg.indexOf('cancel') > -1) return
							uni.showToast({
								title: '选择PDF失败',
								icon: 'none'
							})
						}
					})
					return
				}
				uni.showToast({
					title: '当前端暂不支持PDF上传',
					icon: 'none'
				})
				// #endif
			},
			addReport({
				title,
				path,
				kind
			}) {
				const now = new Date()
				const item = {
					day: pad2(now.getDate()),
					month: `${now.getMonth() + 1}月`,
					title,
					desc: kind === 'pdf' ? 'PDF报告 · 待解读' : '图片报告 · 待解读',
					path,
					kind
				}
				this.reportList.unshift(item)
				this.runParse(item)
			},
			runParse(item) {
				uni.showLoading({
					title: 'AI解析中...',
					mask: true
				})
				setTimeout(() => {
					uni.hideLoading()
					this.parseResult = {
						category: '检测报告 · 内分泌',
						title: '2项需要关注',
						sourceTitle: item.title || '年度体检报告',
						path: item.path,
						kind: item.kind,
						items: [{
								name: '总胆固醇',
								ref: '参考2.50-5.20',
								status: '轻度偏高',
								value: '5.8 mmol/l'
							},
							{
								name: '维生素 D',
								ref: '参考2.50-5.20',
								status: '低于参考',
								value: '5.8 mmol/l'
							}
						],
						advice: '结合7月报告，总胆固醇连续两次轻度偏高；建议携带原报告咨询专业医生。',
						detail: {
							subText: `${item.title || '年度体检报告'} · 12项指标`,
							alertText: '2项需关注',
							cells: [{
									label: '报告日期',
									value: '2026-07-26',
									icon: '/static/baogaoDate.png'
								},
								{
									label: '报告机构',
									value: '体检中心',
									icon: '/static/tijiancenter.png'
								},
								{
									label: '正常项目',
									value: '10项',
									icon: '/static/success.png'
								},
								{
									label: '异常项目',
									value: '2项',
									icon: '/static/yichang.png'
								}
							]
						}
					}
					if (item.desc && item.desc.indexOf('待解读') > -1) {
						item.desc = '12项指标 · 2项关注'
					}
					uni.showToast({
						title: '解析完成',
						icon: 'success'
					})
				}, 800)
			},
			onReportTap(item) {
				if (this.parseResult && this.parseResult.path === item.path) return
				this.runParse(item)
			},
			onParseDetail() {
				if (!this.parseResult || !this.parseResult.detail) return
				this.detailPopup = {
					subText: this.parseResult.detail.subText,
					alertText: this.parseResult.detail.alertText,
					cells: this.parseResult.detail.cells || []
				}
				this.showDetailPopup = true
			},
			closeDetailPopup() {
				this.showDetailPopup = false
			},
			onDetailAskAi() {
				this.closeDetailPopup()
				this.onParseAsk()
			},
			onParseDelete() {
				uni.showModal({
					title: '提示',
					content: '确定删除该解析报告吗？',
					success: (res) => {
						if (!res.confirm || !this.parseResult) return
						const path = this.parseResult.path
						this.parseResult = null
						const idx = this.reportList.findIndex((row) => row.path === path)
						if (idx > -1) {
							this.reportList.splice(idx, 1)
						}
					}
				})
			},
			onParseShare() {
				uni.showToast({
					title: '分享/导出',
					icon: 'none'
				})
			},
			onParseAsk() {
				uni.showToast({
					title: '继续提问',
					icon: 'none'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 48rpx;
		box-sizing: border-box;
		background: #000000;
		color: #ffffff;
	}

	.hero-card {
		position: relative;
		overflow: hidden;
		padding: 40rpx 32rpx 36rpx;
		border-radius: 28rpx;
		background: linear-gradient(160deg, #1a3a8a 0%, #0d1a3a 45%, #0a1020 100%);
		border: 1rpx solid rgba(77, 163, 255, 0.18);
		box-sizing: border-box;
	}

	.hero-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.hero-body {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.hero-icon {
		width: 108rpx;
		height: 108rpx;
	}

	.hero-tag {
		margin-top: 20rpx;
		font-weight: 500;
		font-size: 26rpx;
		color: #2C89FF;
	}

	.hero-title {
		margin-top: 12rpx;
		font-size: 44rpx;
		font-weight: 700;
		line-height: 60rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.hero-desc {
		margin-top: 14rpx;
		padding: 0 20rpx;
		text-align: center;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.5);
	}

	.upload-btn {
		position: relative;
		width: 552rpx;
		height: 88rpx;
		margin-top: 36rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.feature-row {
		width: 326px;
		height: 39px;
		margin-top: 36rpx;
	}

	.parse-card {
		position: relative;
		overflow: hidden;
		margin-top: 28rpx;
		padding: 32rpx 28rpx;
		border-radius: 28rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		box-sizing: border-box;
	}

	.parse-card-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-body {
		position: relative;
		z-index: 1;
	}

	.parse-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
	}

	.parse-head-left {
		flex: 1;
		min-width: 0;
		padding-right: 16rpx;
		display: flex;
		flex-direction: column;
	}

	.parse-cate {
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.parse-title {
		margin-top: 8rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.parse-detail-btn {
		position: relative;
		height: 56rpx;
		padding: 0 24rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.parse-detail-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-detail-btn-text {
		position: relative;
		z-index: 1;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.parse-item {
		position: relative;
		overflow: hidden;
		margin-top: 20rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: rgba(255, 255, 255, 0.04);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.parse-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-item-left {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.parse-item-name {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		color: #ffffff;
	}

	.parse-item-ref {
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.parse-item-status {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #6eb6ff;
	}

	.parse-item-value {
		position: relative;
		z-index: 1;
		margin-left: 16rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #ffffff;
		flex-shrink: 0;
	}

	.parse-advice {
		position: relative;
		overflow: hidden;
		margin-top: 20rpx;
		padding: 28rpx 24rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: flex-start;
		box-sizing: border-box;
	}

	.parse-advice-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.parse-advice-icon {
		position: relative;
		z-index: 1;
		width: 48rpx;
		height: 48rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.parse-advice-body {
		position: relative;
		z-index: 1;
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.parse-advice-title {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 40rpx;
		background-image: linear-gradient(to right, #FFFFFF 18%, #4EA1FF 100%);
		-webkit-background-clip: text;
		background-clip: text;
		-webkit-text-fill-color: transparent;
		color: transparent;
	}

	.parse-advice-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 36rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.parse-actions {
		margin-top: 28rpx;
		display: flex;
		align-items: center;
	}

	.parse-btn {
		position: relative;
		flex: 1;
		height: 80rpx;
		border-radius: 20rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.parse-btn.gray {
		margin-right: 16rpx;
		background: rgba(255, 255, 255, 0.06);
		border: 1rpx solid rgba(255, 255, 255, 0.12);
	}

	.parse-btn.outline {
		margin-right: 16rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.parse-btn.solid {
		background: transparent;
	}

	.parse-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.parse-btn-text {
		position: relative;
		z-index: 1;
		font-size: 26rpx;
		font-weight: 600;
		line-height: 36rpx;
		color: #ffffff;
	}

	.parse-btn-text.gray-text {
		color: rgba(255, 255, 255, 0.55);
	}

	.parse-btn-text.outline-text {
		color: #7eb6ff;
	}

	.section {
		margin-top: 40rpx;
	}

	.section-title {
		display: block;
		font-size: 32rpx;
		font-weight: 700;
		line-height: 44rpx;
		color: #ffffff;
	}

	.section-sub {
		display: block;
		margin-top: 6rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.report-card {
		margin-top: 24rpx;
		padding: 28rpx 24rpx;
		display: flex;
		align-items: center;
		border-radius: 24rpx;
		background: #1a1b25;
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		box-sizing: border-box;
	}

	.report-date {
		width: 96rpx;
		height: 96rpx;
		margin-right: 24rpx;
		border-radius: 20rpx;
		background: #243044;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.report-day {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 44rpx;
		color: #6eb6ff;
	}

	.report-month {
		margin-top: 2rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(110, 182, 255, 0.75);
	}

	.report-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.report-title {
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.report-desc {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.report-arrow {
		width: 32rpx;
		height: 32rpx;
		margin-left: 12rpx;
		flex-shrink: 0;
		opacity: 0.45;
	}

	.footer-tip {
		margin-top: 48rpx;
		display: flex;
		align-items: flex-start;
		justify-content: center;
		padding: 0 12rpx;
	}

	.footer-tip-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 10rpx;
		margin-top: 4rpx;
		border-radius: 50%;
		background: rgba(77, 163, 255, 0.35);
		flex-shrink: 0;
	}

	.footer-tip-text {
		flex: 1;
		font-size: 22rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.privacy-badge {
		display: flex;
		justify-content: center;
		align-items: center;
		margin-top: 20rpx;
	}

	.privacy-icon {
		width: 28rpx;
		height: 28rpx;
		margin-right: 8rpx;
	}

	.privacy-text {
		font-weight: 500;
		font-size: 26rpx;
		color: #FFFFFF;
	}

	.upload-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2000;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.upload-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #12131a;
		animation: uploadSlideUp 0.28s ease-out;
	}

	@keyframes uploadSlideUp {
		from {
			transform: translateY(100%);
		}

		to {
			transform: translateY(0);
		}
	}

	.upload-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.upload-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 48rpx;
		box-sizing: border-box;
	}

	.upload-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.upload-popup-sub {
		display: block;
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.upload-options {
		margin-top: 36rpx;
		display: flex;
		align-items: stretch;
		justify-content: space-between;
	}

	.upload-option {
		position: relative;
		width: 30%;
		padding: 28rpx 12rpx 24rpx;
		height: 234rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255,255,255,0.1);
		display: flex;
		flex-direction: column;
		align-items: center;
		box-sizing: border-box;
	}

	.upload-option-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.upload-option-icon {
		position: relative;
		z-index: 1;
		width: 141.94rpx;
		height: 141.94rpx;
	}

	.upload-option-text {
		position: relative;
		z-index: 1;
		margin-top: 16rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.detail-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 2100;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		box-sizing: border-box;
	}

	.detail-popup {
		position: relative;
		width: 100%;
		margin: 0 30rpx 60rpx;
		border-radius: 32rpx;
		overflow: hidden;
		background: #0b0f1a;
		animation: detailSlideUp 0.28s ease-out;
	}

	@keyframes detailSlideUp {
		from {
			transform: translateY(100%);
		}
		to {
			transform: translateY(0);
		}
	}

	.detail-popup-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-popup-body {
		position: relative;
		z-index: 1;
		padding: 40rpx 32rpx 36rpx;
		box-sizing: border-box;
	}

	.detail-popup-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 50rpx;
		color: #ffffff;
	}

	.detail-popup-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.45);
	}

	.detail-alert {
		position: relative;
		margin-top: 28rpx;
		height: 72rpx;
		padding: 0 24rpx;
		border-radius: 16rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		background: rgba(30, 48, 90, 0.65);
		box-sizing: border-box;
	}

	.detail-alert-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-alert-icon {
		position: relative;
		z-index: 1;
		width: 40rpx;
		height: 40rpx;
		margin-right: 12rpx;
		flex-shrink: 0;
	}

	.detail-alert-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		line-height: 40rpx;
		color: #4da3ff;
	}

	.detail-grid {
		margin-top: 24rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;
	}

	.detail-grid-item {
		position: relative;
		overflow: hidden;
		width: 48%;
		margin-bottom: 16rpx;
		padding: 24rpx 20rpx;
		border-radius: 20rpx;
		background: rgba(26, 27, 37, 0.95);
		border: 1rpx solid rgba(255, 255, 255, 0.06);
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
	}

	.detail-grid-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.detail-grid-icon {
		position: relative;
		z-index: 1;
		width: 40rpx;
		height: 40rpx;
		margin-right: 16rpx;
	}

	.detail-grid-label {
		position: relative;
		z-index: 1;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}

	.detail-grid-value {
		position: relative;
		z-index: 1;
		margin-top: 8rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #ffffff;
	}

	.detail-actions {
		margin-top: 32rpx;
		display: flex;
		align-items: center;
	}

	.detail-btn {
		position: relative;
		flex: 1;
		height: 88rpx;
		border-radius: 28rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.detail-btn.outline {
		margin-right: 20rpx;
		background: rgba(18, 24, 40, 0.9);
		border: 2rpx solid rgba(77, 163, 255, 0.55);
	}

	.detail-btn.solid {
		background: transparent;
	}

	.detail-btn-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
		pointer-events: none;
	}

	.detail-btn-text {
		position: relative;
		z-index: 1;
		font-size: 30rpx;
		font-weight: 600;
		line-height: 42rpx;
		color: #ffffff;
	}

	.detail-btn-text.outline-text {
		color: #7eb6ff;
	}
</style>