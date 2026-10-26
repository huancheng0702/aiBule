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
			// 冷启动（进程被杀后重开）：仅安卓强制断开重连，全新连接才能让设备重新 dump 历史数据；
			// iOS 的 sendSyncInfo 会主动 requestAllInfo，保持原逻辑（force 在 iOS 上被 !isIos() 置 false）。
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
			// 重开 sensor 上传开关、补报离线缓存、回放原生缓存的健康事件
			BleService.onAppShow().catch((e) => {
				console.log('[App] ble onAppShow', e && e.message)
			})
		},
		onHide: function() {
			console.log('App Hide')
			// 官方建议：进后台关闭 sensor 数据上传开关（设备侧会缓存，回前台/重连后补发）
			BleService.onAppHide().catch((e) => {
				console.log('[App] ble onAppHide', e && e.message)
			})
		}
	}
</script>

<style lang="scss">
	/* 注意要写在第一行，同时给style标签加入lang="scss"属性 */
	@import "@/uni_modules/uview-ui/index.scss";
</style>