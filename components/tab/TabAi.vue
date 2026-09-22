<template>
	<view class="tab-page">
		<!-- 顶部：菜单 + 问候 -->
		<view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="menu-btn" @click="openHistory">
				<image class="menu-icon" src="/static/aiMsgBtn.png" mode=""></image>
			</view>
			<view class="header-text">
				<text class="nav-title">AI健康助手</text>
				<text class="page-title">你好，小安</text>
			</view>
		</view>

		<!-- AI 光环形象 -->
		<view class="hero">
			<image class="hero-img" src="/static/aiAgentIcon.png" mode=""></image>
		</view>

		<!-- 快捷提问 -->
		<view class="suggest-grid">
			<view v-for="(item, index) in suggestions" :key="index" class="suggest-card" @click="onSuggest(item)">
				<image class="suggest-icon" :src="item.icon" mode=""></image>
				<text class="suggest-text">{{ item.msgCon }}</text>
			</view>
		</view>

		<!-- 对话区域：仅此处内部滚动 -->
		<scroll-view class="chat-list" scroll-y :scroll-into-view="scrollIntoView" :scroll-with-animation="true">
			<view v-for="(msg, index) in messages" :id="'msg-' + index" :key="index" class="chat-row" :class="msg.role">
				<template v-if="msg.role === 'user'">
					<view class="bubble user-bubble">
						<image class="user-bubble-bg" src="/static/myMsgBg.png" mode=""></image>
						<text class="bubble-text">{{ msg.content }}</text>
					</view>
				</template>
				<template v-else>
					<image class="ai-avatar" src="/static/aiHic.png" mode=""></image>
					<view class="bubble ai-bubble">
						<text class="bubble-text">{{ msg.content }}</text>
					</view>
				</template>
			</view>
			<view id="chat-bottom" class="chat-bottom"></view>
		</scroll-view>

		<!-- 底部输入栏 -->
		<view class="input-wrap">
			<view class="input-bar">
				<image class="input-bg" src="/static/sendInputBg.png" mode=""></image>
				<input class="input-field" v-model="inputValue" type="text" confirm-type="send" placeholder="输入您的问题"
					placeholder-class="input-placeholder" @confirm="onSend" />
				<view class="send-btn" @click="onSend">
					<image class="send-icon" src="/static/sendbtn.png" mode=""></image>
				</view>
			</view>
		</view>

		<!-- 历史对话侧边弹窗 -->
		<view v-if="showHistory" class="history-mask" @click="closeHistory">
			<view class="history-drawer" :style="{ paddingTop: statusBarHeight + 'px' }" @click.stop>
				<image class="history-drawer-bg" src="/static/aiRecordBg.png" mode=""></image>
				<view class="history-header">
					<text class="history-title">历史对话</text>
					<view class="history-close" @click="closeHistory">
						<image class="history-close-icon" src="/static/closeBtn.png" mode=""></image>
					</view>
				</view>

				<view class="history-new" @click="onNewChat">
					<text class="history-new-text">+ 新建对话</text>
				</view>

				<view class="history-section">
					<text class="history-section-left">本月</text>
					<text class="history-section-right">历史记录</text>
				</view>

				<scroll-view class="history-list" scroll-y>
					<view v-for="(item, index) in historyList" :key="item.id" class="history-item"
						:class="{ active: activeHistoryId === item.id }" @click="onSelectHistory(item)">
						<image v-if="activeHistoryId === item.id" src="/static/aiSelectBg.png" class="history-item-bg"
							mode=""></image>
						<view class="history-item-body">
							<text class="history-item-title">{{ item.title }}</text>
							<text class="history-item-time"
								:class="{ active: activeHistoryId === item.id }">{{ item.time }}</text>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'TabAi',
		data() {
			return {
				statusBarHeight: 20,
				inputValue: '',
				scrollIntoView: '',
				showHistory: false,
				activeHistoryId: 1,
				historyList: [{
						id: 1,
						title: '今天想了解什么？',
						time: '今天 11:42'
					},
					{
						id: 2,
						title: '昨晚睡眠质量怎么样？',
						time: '今天 11:42'
					},
					{
						id: 3,
						title: '最近心率有异常吗？',
						time: '今天 11:42'
					},
					{
						id: 4,
						title: '如何改善压力状态？',
						time: '今天 11:42'
					}
				],
				suggestions: [{
						msgCon: '昨晚睡得怎么样？',
						icon: '/static/shuimian.png'
					},
					{
						msgCon: '最近心率有异常吗？',
						icon: '/static/xinlv.png'
					},
					{
						msgCon: '帮我解读健康日报',
						icon: '/static/jianribao.png'
					},
					{
						msgCon: '如何改善压力状态',
						icon: '/static/yali.png'
					}
				],
				messages: [{
						role: 'user',
						content: '昨晚睡得怎么样？'
					},
					{
						role: 'ai',
						content: '昨晚共睡眠7小时32分钟，深睡1小时48分，整体恢复良好。建议继续保持固定入睡时间。'
					}
				]
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight || 20
		},
		methods: {
			openHistory() {
				this.showHistory = true
			},
			closeHistory() {
				this.showHistory = false
			},
			onNewChat() {
				this.messages = []
				this.activeHistoryId = null
				this.closeHistory()
			},
			onSelectHistory(item) {
				this.activeHistoryId = item.id
				this.closeHistory()
			},
			onSuggest(item) {
				this.inputValue = item.msgCon
				this.onSend()
			},
			onSend() {
				const text = (this.inputValue || '').trim()
				if (!text) return
				this.messages.push({
					role: 'user',
					content: text
				})
				this.inputValue = ''
				this.$nextTick(() => {
					this.scrollIntoView = ''
					this.$nextTick(() => {
						this.scrollIntoView = 'chat-bottom'
					})
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		height: calc(100vh - 128rpx - constant(safe-area-inset-bottom));
		height: calc(100vh - 128rpx - env(safe-area-inset-bottom));
		padding: 0 28rpx;
		box-sizing: border-box;
		background: #0d0d0f;
		color: #ffffff;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.header {
		display: flex;
		align-items: center;
		padding-bottom: 16rpx;
		flex-shrink: 0;
	}

	.menu-btn {
		width: 76rpx;
		height: 64rpx;
		margin-right: 20rpx;
	}

	.menu-icon {
		width: 100%;
		height: 100%;
	}

	.header-text {
		display: flex;
		flex-direction: column;
	}

	.nav-title {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255, 0.55);
	}

	.page-title {
		margin-top: 4rpx;
		font-size: 40rpx;
		font-weight: 700;
		line-height: 56rpx;
		color: #ffffff;
	}

	.hero {
		display: flex;
		justify-content: center;
		align-items: center;
		margin: 0 40rpx;
		flex-shrink: 0;
	}

	.hero-img {
		width: 436rpx;
		height: 370rpx;
	}

	.suggest-grid {
		display: flex;
		flex-wrap: wrap;
		gap: 20rpx;
		margin-bottom: 24rpx;
		flex-shrink: 0;
	}

	.suggest-card {
		width: calc(50% - 10rpx);
		display: flex;
		align-items: center;
		padding: 28rpx 24rpx;
		height: 102rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
	}

	.suggest-icon {
		width: 40rpx;
		height: 40rpx;
		margin-right: 14rpx;
		flex-shrink: 0;
	}

	.suggest-text {
		flex: 1;
		font-size: 24rpx;
		line-height: 34rpx;
		color: #ffffff;
	}

	.chat-list {
		flex: 1;
		height: 0;
		min-height: 0;
		padding-bottom: 8rpx;
		box-sizing: border-box;
	}

	.chat-row {
		display: flex;
		margin-bottom: 28rpx;
	}

	.chat-row.user {
		justify-content: flex-end;
	}

	.chat-row.ai {
		justify-content: flex-start;
		align-items: flex-start;
	}

	.ai-avatar {
		width: 84rpx;
		height: 84rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.bubble {
		max-width: 78%;
		box-sizing: border-box;
	}

	.user-bubble {
		position: relative;
		padding: 22rpx 32rpx;
		overflow: hidden;
	}

	.user-bubble-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.ai-bubble {
		padding: 24rpx 28rpx;
		border-radius: 24rpx;
		background: #1c1c1e;
	}

	.bubble-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.chat-bottom {
		height: 1rpx;
	}

	.input-wrap {
		flex-shrink: 0;
		padding: 12rpx 0 16rpx;
		box-sizing: border-box;
	}

	.input-bar {
		position: relative;
		display: flex;
		align-items: center;
		height: 96rpx;
		padding: 0 12rpx 0 36rpx;
		border-radius: 999rpx;
		overflow: hidden;
		box-sizing: border-box;
	}

	.input-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.input-field {
		position: relative;
		z-index: 1;
		flex: 1;
		height: 96rpx;
		font-size: 28rpx;
		color: #ffffff;
		background: transparent;
	}

	.input-placeholder {
		color: rgba(255, 255, 255, 0.35);
	}

	.send-btn {
		position: relative;
		z-index: 1;
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.send-icon {
		width: 44rpx;
		height: 44rpx;
	}

	/* 历史对话侧边栏 */
	.history-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 1000;
		background: rgba(0, 0, 0, 0.55);
	}

	.history-drawer {
		position: relative;
		width: 82%;
		height: 100%;
		padding: 0 28rpx 40rpx;
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		background: #1A1B25;
	}

	.history-drawer-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.history-header {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx 0 28rpx;
		flex-shrink: 0;
	}

	.history-title {
		font-size: 36rpx;
		font-weight: 600;
		line-height: 50rpx;
		color: #ffffff;
	}

	.history-close {
		width: 76rpx;
		height: 64rpx;
	}

	.history-close-icon {
		width: 76rpx;
		height: 64rpx;
	}

	.history-new {
		position: relative;
		z-index: 1;
		height: 88rpx;
		background: rgba(255, 255, 255, 0.1);
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		margin-bottom: 28rpx;
	}

	.history-new-text {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.history-section {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
		flex-shrink: 0;
	}

	.history-section-left,
	.history-section-right {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255);
	}

	.history-list {
		position: relative;
		z-index: 1;
		flex: 1;
		height: 0;
		min-height: 0;
	}

	.history-item {
		position: relative;
		height: 126rpx;
		padding: 18rpx 20rpx;
		margin-bottom: 20rpx;
		border-radius: 24rpx;
		background: #25232c;
		overflow: hidden;
		box-sizing: border-box;
	}

	.history-item-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		z-index: 0;
	}

	.history-item-body {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
	}

	.history-item-title {
		font-size: 30rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.history-item-time {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
	}
</style>