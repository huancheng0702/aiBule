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
		<scroll-view class="chat-list" scroll-y :scroll-into-view="scrollIntoView"
			:scroll-with-animation="scrollWithAnimation">
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
						<text v-if="msg.content" class="bubble-text">{{ msg.content }}<text v-if="msg.streaming" class="typing-cursor">|</text></text>
						<view v-else-if="msg.streaming" class="thinking"><text class="thinking-text">正在思考</text><text class="thinking-dot d1">.</text><text class="thinking-dot d2">.</text><text class="thinking-dot d3">.</text></view>
						<text v-else class="bubble-text"></text>
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
				<view class="send-btn" :class="{ disabled: sending }" @click="onSend">
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
					<view class="history-filter">
						<view v-for="opt in historyFilters" :key="opt.value" class="history-filter-item"
							:class="{ active: historyTimeType === opt.value }" @click="onHistoryFilter(opt.value)">
							<text class="history-filter-text">{{ opt.label }}</text>
						</view>
					</view>
					<text class="history-section-right">历史记录</text>
				</view>

				<scroll-view class="history-list" scroll-y @scrolltolower="loadMoreHistory">
					<view v-for="item in historyList" :key="item.id" class="history-item"
						:class="{ active: activeHistoryId === item.id }" @click="onSelectHistory(item)">
						<image v-if="activeHistoryId === item.id" src="/static/aiSelectBg.png" class="history-item-bg"
							mode=""></image>
						<view class="history-item-body">
							<text class="history-item-title">{{ item.title }}</text>
							<text class="history-item-time"
								:class="{ active: activeHistoryId === item.id }">{{ item.time }}</text>
						</view>
					</view>
					<view v-if="historyLoading" class="history-tip">
						<text class="history-tip-text">加载中...</text>
					</view>
					<view v-else-if="historyFinished && historyList.length" class="history-tip">
						<text class="history-tip-text">没有更多了</text>
					</view>
					<view v-else-if="!historyList.length" class="history-tip">
						<text class="history-tip-text">暂无历史对话</text>
					</view>
				</scroll-view>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		chatAiStream,
		createAiSession,
		getAiMessages,
		getAiQuickQuestions,
		getAiSessionList
	} from '@/common/api/ai.js'

	const DEFAULT_SUGGESTIONS = [{
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
	]

	function formatSessionTime(value) {
		if (!value) return ''
		const raw = typeof value === 'number' ? value : String(value).replace(/-/g, '/')
		const date = new Date(raw)
		if (isNaN(date.getTime())) return String(value)
		const mm = date.getMonth() + 1
		const dd = date.getDate()
		const hh = date.getHours() < 10 ? '0' + date.getHours() : String(date.getHours())
		const mi = date.getMinutes() < 10 ? '0' + date.getMinutes() : String(date.getMinutes())
		return mm + '/' + dd + ' ' + hh + ':' + mi
	}

	export default {
		name: 'TabAi',
		data() {
			return {
				statusBarHeight: 20,
				inputValue: '',
				scrollIntoView: '',
				scrollWithAnimation: false,
				scrollTimer: null,
				showHistory: false,
				activeHistoryId: null,
				sessionId: '',
				sending: false,
				typeQueue: '',
				typeTimer: null,
				typingIndex: -1,
				historyList: [],
				historyFilters: [{
						label: '全部',
						value: ''
					},
					{
						label: '本月',
						value: 'month'
					},
					{
						label: '本周',
						value: 'week'
					}
				],
				historyTimeType: '',
				historyPageNum: 1,
				historyPageSize: 10,
				historyLoading: false,
				historyFinished: false,
				historyReqId: 0,
				suggestions: DEFAULT_SUGGESTIONS.slice(),
				messages: []
			}
		},
		created() {
			const sys = uni.getSystemInfoSync()
			this.statusBarHeight = sys.statusBarHeight + 10 || 20
			this.loadQuickQuestions()
			this.loadHistory(true)
		},
		beforeDestroy() {
			this.clearTypeTimer()
			this.clearScrollTimer()
		},
		methods: {
			scrollToBottom(animate = false) {
				this.scrollWithAnimation = animate
				this.$nextTick(() => {
					this.scrollIntoView = ''
					this.$nextTick(() => {
						this.scrollIntoView = 'chat-bottom'
					})
				})
			},
			// 流式输出期间节流滚动，避免每 20ms 触发一次带动画的滚动导致抖动
			scheduleScroll() {
				if (this.scrollTimer) return
				this.scrollTimer = setTimeout(() => {
					this.scrollTimer = null
					this.scrollToBottom(false)
				}, 60)
			},
			clearScrollTimer() {
				if (this.scrollTimer) {
					clearTimeout(this.scrollTimer)
					this.scrollTimer = null
				}
			},
			// 回复结束后的收尾滚动：文本/光标布局在渲染完成后还会再变一次，
			// 因此先动画贴底，再补一次无动画贴底，确保真正停在最底部
			async settleScroll() {
				this.clearScrollTimer()
				this.scrollToBottom(true)
				await new Promise((resolve) => setTimeout(resolve, 260))
				this.scrollToBottom(false)
				await new Promise((resolve) => setTimeout(resolve, 120))
				this.scrollToBottom(false)
			},
			clearTypeTimer() {
				if (this.typeTimer) {
					clearInterval(this.typeTimer)
					this.typeTimer = null
				}
			},
			enqueueType(aiIndex, text) {
				if (!text) return
				this.typingIndex = aiIndex
				this.typeQueue += text
				if (this.typeTimer) return
				this.typeTimer = setInterval(() => {
					if (!this.typeQueue) {
						this.clearTypeTimer()
						return
					}
					const step = this.typeQueue.length > 40 ? 2 : 1
					const piece = this.typeQueue.slice(0, step)
					this.typeQueue = this.typeQueue.slice(step)
					const msg = this.messages[this.typingIndex]
					if (msg) {
						msg.content += piece
					}
					this.scheduleScroll()
				}, 20)
			},
			waitTypingDone() {
				return new Promise((resolve) => {
					const check = () => {
						if (!this.typeQueue) {
							this.clearTypeTimer()
							resolve()
							return
						}
						setTimeout(check, 30)
					}
					check()
				})
			},
			async loadQuickQuestions() {
				try {
					const list = await getAiQuickQuestions()
					const items = Array.isArray(list) ? list : (list && list.list) || []
					if (!items.length) return
					this.suggestions = items.slice(0, 4).map((item, index) => {
						const text = typeof item === 'string' ? item : (item.question || item.content || item.msgCon || '')
						return {
							msgCon: text,
							icon: DEFAULT_SUGGESTIONS[index % DEFAULT_SUGGESTIONS.length].icon
						}
					}).filter((item) => item.msgCon)
				} catch (e) {}
			},
			async loadHistory(reset = false) {
				if (!reset && (this.historyLoading || this.historyFinished)) return
				if (reset) {
					this.historyPageNum = 1
					this.historyFinished = false
					this.historyList = []
				}
				const reqId = ++this.historyReqId
				const pageNum = this.historyPageNum
				this.historyLoading = true
				try {
					const params = {
						pageNum,
						pageSize: this.historyPageSize
					}
					if (this.historyTimeType) params.timeType = this.historyTimeType
					const res = await getAiSessionList(params)
					// 期间切换了筛选条件或重新加载，丢弃过期响应
					if (reqId !== this.historyReqId) return
					const records = Array.isArray(res) ? res : (res && (res.records || res.list)) || []
					const items = records.map((item) => ({
						id: item.sessionId,
						title: item.title || '新对话',
						time: formatSessionTime(item.updateTime || item.createTime)
					}))
					this.historyList = pageNum === 1 ? items : this.historyList.concat(items)
					const total = res && res.total !== undefined && res.total !== null ? Number(res.total) : null
					this.historyFinished = items.length < this.historyPageSize ||
						(total !== null && this.historyList.length >= total)
					this.historyPageNum = pageNum + 1
				} catch (e) {} finally {
					if (reqId === this.historyReqId) this.historyLoading = false
				}
			},
			loadMoreHistory() {
				this.loadHistory(false)
			},
			onHistoryFilter(value) {
				if (this.historyTimeType === value) return
				this.historyTimeType = value
				this.loadHistory(true)
			},
			openHistory() {
				this.showHistory = true
				this.loadHistory(true)
			},
			closeHistory() {
				this.showHistory = false
			},
			async onNewChat() {
				if (this.sending) return
				this.messages = []
				this.sessionId = ''
				this.activeHistoryId = null
				try {
					const res = await createAiSession()
					const id = (res && (res.sessionId || res.id)) || res
					if (id) {
						this.sessionId = String(id)
						this.activeHistoryId = this.sessionId
					}
				} catch (e) {}
				this.closeHistory()
			},
			async onSelectHistory(item) {
				if (this.sending) return
				this.activeHistoryId = item.id
				this.sessionId = item.id
				this.closeHistory()
				try {
					const list = await getAiMessages(item.id)
					const items = Array.isArray(list) ? list : (list && list.list) || []
					this.messages = items.map((msg) => ({
						role: msg.role === 'user' ? 'user' : 'ai',
						content: msg.content || '',
						streaming: false
					}))
					this.settleScroll()
				} catch (e) {
					uni.showToast({
						title: '历史消息加载失败',
						icon: 'none'
					})
				}
			},
			onSuggest(item) {
				this.inputValue = item.msgCon
				this.onSend()
			},
			async onSend() {
				const text = (this.inputValue || '').trim()
				if (!text || this.sending) return
				this.sending = true
				this.messages.push({
					role: 'user',
					content: text
				})
				this.inputValue = ''
				const aiIndex = this.messages.length
				this.messages.push({
					role: 'ai',
					content: '',
					streaming: true
				})
				this.scrollToBottom(false)
				try {
					const payload = {
						content: text
					}
					if (this.sessionId) {
						payload.sessionId = this.sessionId
					}
					await chatAiStream(payload, {
						onSession: (id) => {
							this.sessionId = String(id)
							this.activeHistoryId = this.sessionId
						},
						onDelta: (delta) => {
							this.enqueueType(aiIndex, delta)
						}
					})
					await this.waitTypingDone()
					const msg = this.messages[aiIndex]
					if (msg && !msg.content) {
						msg.content = '暂时没有获取到回复'
					}
					if (msg) msg.streaming = false
					this.loadHistory(true)
				} catch (e) {
					await this.waitTypingDone()
					const msg = this.messages[aiIndex]
					if (msg) {
						msg.streaming = false
						msg.content = msg.content || ((e && e.message) || '回复失败，请稍后重试')
					}
				} finally {
					this.sending = false
					await this.settleScroll()
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.tab-page {
		height: 100vh;
		padding: 0 28rpx 160rpx;
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
		justify-content: space-between;
		margin-bottom: 24rpx;
		flex-shrink: 0;
	}

	.suggest-card {
		width: 48%;
		display: flex;
		align-items: center;
		padding: 28rpx 24rpx;
		height: 102rpx;
		background: #1A1B25;
		border-radius: 20rpx 20rpx 20rpx 20rpx;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		box-sizing: border-box;
		margin-bottom: 20rpx;
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
		border-radius: 0rpx 24rpx 24rpx 24rpx;
		background: #1c1c1e;
	}

	.bubble-text {
		position: relative;
		z-index: 1;
		font-size: 28rpx;
		line-height: 42rpx;
		color: #ffffff;
	}

	.typing-cursor {
		display: inline-block;
		margin-left: 4rpx;
		color: #4da3ff;
		animation: cursorBlink 0.8s infinite;
	}

	@keyframes cursorBlink {
		0%, 100% { opacity: 1; }
		50% { opacity: 0; }
	}

	.thinking {
		display: flex;
		align-items: center;
		flex-direction: row;
	}

	.thinking-text {
		font-size: 28rpx;
		line-height: 42rpx;
		color: rgba(255,255,255,0.75);
	}

	.thinking-dot {
		font-size: 32rpx;
		font-weight: 700;
		line-height: 42rpx;
		color: #4da3ff;
		opacity: 0.2;
		animation: thinkingDot 1.2s infinite;
	}

	.thinking-dot.d2 { animation-delay: 0.2s; }
	.thinking-dot.d3 { animation-delay: 0.4s; }

	@keyframes thinkingDot {
		0%, 60%, 100% { opacity: 0.2; transform: translateY(0); }
		30% { opacity: 1; transform: translateY(-6rpx); }
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

	.history-filter {
		display: flex;
		align-items: center;
	}

	.history-filter-item {
		padding: 6rpx 22rpx;
	}

	.history-filter-item.active {
	
	}

	.history-filter-text {
		font-size: 28rpx;
		line-height: 32rpx;
		color: #fff;
	}

	.history-filter-item.active .history-filter-text {
		color: #B46306;
	}

	.history-section-right {
		font-size: 24rpx;
		line-height: 34rpx;
		color: rgba(255, 255, 255);
	}

	.history-tip {
		display: flex;
		justify-content: center;
		padding: 20rpx 0 30rpx;
	}

	.history-tip-text {
		font-size: 22rpx;
		line-height: 30rpx;
		color: rgba(255, 255, 255, 0.4);
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