<template>
	<view class="page">
		<view class="card">
			<text class="card-title">{{ isChange ? '换绑手机号' : '绑定手机号' }}</text>
			<text v-if="currentPhone" class="card-sub">当前：{{ maskPhone(currentPhone) }}</text>
			<text v-else class="card-sub">绑定后可用于登录与账号找回</text>
			<view class="field">
				<input v-model="phone" type="number" maxlength="11" placeholder="请输入新手机号" placeholder-class="ph" />
			</view>
			<view class="field code-row">
				<input v-model="code" type="number" maxlength="6" placeholder="请输入验证码" placeholder-class="ph" />
				<view class="send" :class="{ disabled: countdown > 0 || sending }" @click="onSendCode">
					{{ countdown > 0 ? countdown + 's' : '获取验证码' }}
				</view>
			</view>
			<view class="submit" @click="onSubmit">
				<image class="submit-bg" src="/static/btnBan.png" mode="scaleToFill"></image>
				<text class="submit-text">{{ submitting ? '提交中...' : '确认' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import { sendSmsCode, bindPhone, changePhone } from '@/common/api/auth.js'
import { getUserInfo } from '@/common/api/user.js'
import { saveLoginSession, USER_INFO_KEY } from '@/common/auth.js'

const PHONE_REG = /^1\d{10}$/

export default {
	data() {
		return {
			phone: '',
			code: '',
			currentPhone: '',
			userId: null,
			countdown: 0,
			timer: null,
			sending: false,
			submitting: false,
			from: '',
			needCompleteInfo: false
		}
	},
	computed: {
		isChange() {
			return !!this.currentPhone
		}
	},
	onLoad(query) {
		const q = query || {}
		this.from = q.from || ''
		const qUid = q.userId || q.user_id || q.uid
		const cached = uni.getStorageSync(USER_INFO_KEY) || {}
		this.needCompleteInfo = q.needCompleteInfo === '1' || q.needCompleteInfo === 1
		const cachedUid = uni.getStorageSync('userId') || cached.userId || cached.id || null
		this.userId = qUid || cachedUid
		this.currentPhone = uni.getStorageSync('phone') || cached.phone || ''
		this.loadLatest()
	},
	onUnload() {
		if (this.timer) clearInterval(this.timer)
	},
	methods: {
		maskPhone(p) {
			const s = String(p || '')
			if (s.length < 7) return s || '--'
			return s.slice(0, 3) + '****' + s.slice(-4)
		},
		async loadLatest() {
			try {
				const info = await getUserInfo()
				const data = (info && info.data) || info || {}
				if (data.phone) {
					this.currentPhone = data.phone
					uni.setStorageSync('phone', data.phone)
				}
				const uid = data.userId || data.id
				if (uid != null) {
					this.userId = uid
					uni.setStorageSync('userId', uid)
				}
				saveLoginSession(data)
			} catch (e) {}
		},
		startCountdown() {
			if (this.timer) clearInterval(this.timer)
			this.countdown = 60
			this.timer = setInterval(() => {
				if (this.countdown <= 1) {
					clearInterval(this.timer)
					this.timer = null
					this.countdown = 0
					return
				}
				this.countdown -= 1
			}, 1000)
		},
		async onSendCode() {
			if (this.countdown > 0 || this.sending) return
			if (!PHONE_REG.test(this.phone)) {
				uni.showToast({ title: '请输入正确手机号', icon: 'none' })
				return
			}
			if (this.isChange && this.phone === this.currentPhone) {
				uni.showToast({ title: '新手机号与当前相同', icon: 'none' })
				return
			}
			this.sending = true
			try {
				await sendSmsCode({ phone: this.phone, scene: 'bind' })
				this.startCountdown()
				uni.showToast({ title: '验证码已发送', icon: 'none' })
			} catch (e) {
				console.log(e)
			} finally {
				this.sending = false
			}
		},
		async onSubmit() {
			if (this.submitting) return
			if (!PHONE_REG.test(this.phone)) {
				uni.showToast({ title: '请输入正确手机号', icon: 'none' })
				return
			}
			if (!this.code) {
				uni.showToast({ title: '请输入验证码', icon: 'none' })
				return
			}
			if (this.isChange && !this.userId) {
				uni.showToast({ title: '缺少用户ID，请重进', icon: 'none' })
				return
			}
			this.submitting = true
			try {
				const payload = this.isChange
					? { userId: this.userId, phone: this.phone, code: this.code }
					: (this.userId ? { userId: this.userId, phone: this.phone, code: this.code } : { phone: this.phone, code: this.code })
				if (this.isChange) {
					await changePhone(payload)
				} else {
					await bindPhone(payload)
				}
				uni.setStorageSync('phone', this.phone)
				uni.setStorageSync('phoneBound', true)
				let need = this.needCompleteInfo
				try {
					const info = await getUserInfo()
					const infoData = (info && info.data) || info || {}
					if (infoData.needCompleteInfo !== undefined) need = infoData.needCompleteInfo === true
					saveLoginSession(infoData || {})
				} catch (e) {}
				uni.showToast({ title: this.isChange ? '换绑成功' : '绑定成功', icon: 'success' })
				setTimeout(() => {
					if (this.from === 'login') {
						if (need === true) {
							uni.reLaunch({ url: '/pages/mine/myInfo' })
						} else {
							uni.reLaunch({ url: '/pages/index/index' })
						}
						return
					}
					uni.navigateBack()
				}, 600)
			} catch (e) {
			} finally {
				this.submitting = false
			}
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
.card {
	background: #1a1b25;
	border-radius: 24rpx;
	padding: 40rpx 32rpx 36rpx;
	box-sizing: border-box;
}
.card-title {
	display: block;
	font-size: 36rpx;
	font-weight: 700;
	line-height: 50rpx;
}
.card-sub {
	display: block;
	margin-top: 10rpx;
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.45);
}
.field {
	margin-top: 28rpx;
	height: 96rpx;
	background: rgba(255, 255, 255, 0.06);
	border-radius: 24rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	box-sizing: border-box;
	input {
		flex: 1;
		min-width: 0;
		height: 96rpx;
		font-size: 30rpx;
		color: #fff;
	}
}
.code-row .send {
	flex-shrink: 0;
	padding-left: 20rpx;
	font-size: 26rpx;
	color: #3b82f6;
	&.disabled { color: rgba(255, 255, 255, 0.35); }
}
.ph { color: rgba(255, 255, 255, 0.35); }
.submit {
	margin-top: 40rpx;
	position: relative;
	height: 96rpx;
	border-radius: 28rpx;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
}
.submit-bg {
	position: absolute;
	left: 0;
	top: 0;
	width: 100%;
	height: 100%;
}
.submit-text {
	position: relative;
	z-index: 1;
	font-size: 30rpx;
	font-weight: 600;
	color: #fff;
}
</style>
