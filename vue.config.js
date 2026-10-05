module.exports = {
	devServer: {
		compress: false,
		proxy: {
			'/appApi': {
				target: 'https://fx.tailunkaluo.com',
				changeOrigin: true,
				secure: false,
				ws: true,
				timeout: 0,
				proxyTimeout: 0,
				onProxyRes(proxyRes, req) {
					const url = req.url || ''
					if (url.indexOf('/stream') !== -1) {
						proxyRes.headers['cache-control'] = 'no-cache, no-transform'
						proxyRes.headers['x-accel-buffering'] = 'no'
						delete proxyRes.headers['content-length']
						delete proxyRes.headers['content-encoding']
					}
				}
			}
		}
	}
}
