<script>
	import BleService from '@/common/ble-service.js'
	import { applyLaunchAuth } from '@/common/auth.js'

	export default {
		onLaunch: function() {
			console.log('App Launch')
			applyLaunchAuth()
			// 进入App即申请蓝牙权限（iOS会弹系统框，Android申请定位+蓝牙）
			BleService.requestPermissions().catch((e) => {
				console.log('[App] bluetooth permission', e && e.message)
			})
			BleService.bootstrap()
			// 冷启动（进程被杀后重开）强制断开重连：否则系统残留的“已连接”状态没有重新订阅通知，
			// 会出现已连接却收不到心率/步数等数据的问题
			BleService.tryAutoReconnect({ force: true }).catch((e) => {
				console.log('[App] auto reconnect on launch', e && e.message)
			})
		},
		onShow: function() {
			console.log('App Show')
			// 从后台回前台：有绑定且未连接则自动重连（解绑后无本地设备，不会重连）
			BleService.tryAutoReconnect().catch((e) => {
				console.log('[App] auto reconnect on show', e && e.message)
			})
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	/* 注意要写在第一行，同时给style标签加入lang="scss"属性 */
	@import "@/uni_modules/uview-ui/index.scss";
</style>