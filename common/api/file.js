import {
	buildApiUrl
} from '@/common/http.js'
import {
	LOGIN_TOKEN_KEY
} from '@/common/auth.js'

const SUCCESS_CODES = [0, 1, 200]

export function uploadFile(filePath) {
	return new Promise((resolve, reject) => {
		if (!filePath) {
			reject(new Error('文件路径为空'))
			return
		}
		uni.uploadFile({
			url: buildApiUrl('/appApi/file/upload'),
			filePath,
			name: 'file',
			header: {
				token: uni.getStorageSync(LOGIN_TOKEN_KEY) || ''
			},
			success: (res) => {
				let body = res.data
				try {
					body = typeof body === 'string' ? JSON.parse(body) : body
				} catch (e) {
					reject(new Error('上传响应解析失败'))
					return
				}
				const code = body && body.code
				if (code !== undefined && code !== null && code !== '' && SUCCESS_CODES.indexOf(Number(code)) === -1) {
					uni.showToast({
						title: String((body && body.msg) || '上传失败').slice(0, 40),
						icon: 'none'
					})
					reject(body)
					return
				}
				resolve(body && body.data !== undefined ? body.data : body)
			},
			fail: (err) => {
				uni.showToast({
					title: '上传失败',
					icon: 'none'
				})
				reject(err)
			}
		})
	})
}
