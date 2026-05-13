import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/meta':  { target: 'http://localhost:8080', changeOrigin: true },
      '/canvas': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 浏览器页面导航（Accept: text/html）返回 index.html 给 React Router 处理
        // XHR/fetch API 请求正常代理到后端
        bypass(req) {
          if (req.headers.accept?.includes('text/html')) {
            return '/index.html'
          }
        },
      },
    },
  },
})

