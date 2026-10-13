import {
	buildApiUrl
} from '@/common/http.js'
import {
	LOGIN_TOKEN_KEY
} from '@/common/auth.js'

const SUCCESS_CODES = [0, 1, 200]

export function downloadFileToLocal(fileUrl, fileName = '下载文件') {
	return new Promise((resolve, reject) => {
		if (!fileUrl) {
			reject(new Error('文件链接为空'))
			return
		}

		// #ifdef H5
		fetch(fileUrl).then((response) => {
			if (!response.ok) throw new Error('文件下载失败')
			return response.blob()
		}).then((blob) => {
			const objectUrl = URL.createObjectURL(blob)
			const link = document.createElement('a')
			link.href = objectUrl
			link.download = fileName
			document.body.appendChild(link)
			link.click()
			link.remove()
			setTimeout(() => URL.revokeObjectURL(objectUrl), 60000)
			resolve(objectUrl)
		}).catch(() => reject(new Error('下载失败，请检查网络及文件域名跨域配置')))
		return
		// #endif

		// #ifdef APP-PLUS
		// 使用持久目录并保留扩展名，避免临时文件被清理或无法识别文档类型。
		const safeName = String(fileName).replace(/[\\/:*?"<>|\x00-\x1f]/g, '_')
		const task = plus.downloader.createDownload(fileUrl, {
			filename: `_doc/exports/${Date.now()}_${safeName}`,
			timeout: 120
		}, (download, status) => {
			if (status !== 200 || !download.filename) {
				reject(new Error('文件下载失败，请检查网络'))
				return
			}
			resolve(download.filename)
		})
		task.start()
		return
		// #endif

		uni.downloadFile({
			url: fileUrl,
			success: (res) => {
				if (res.statusCode !== 200 || !res.tempFilePath) {
					reject(new Error('文件下载失败'))
					return
				}
				uni.saveFile({
					tempFilePath: res.tempFilePath,
					success: (saved) => {
						if (!saved.savedFilePath) {
							reject(new Error('文件保存失败'))
							return
						}
						resolve(saved.savedFilePath)
					},
					fail: () => reject(new Error('文件保存失败，请检查存储空间'))
				})
			},
			fail: reject
		})
	})
}

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
