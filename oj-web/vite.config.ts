import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 前端构建配置（Vue3 / Vite，保留既有分包策略）
export default defineConfig({
  plugins: [vue()],
  // sockjs-client 引用 Node 的 global 全局量，浏览器端需映射到 globalThis
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 开发代理：/api /ws /uploads 转发到 oj-server（论文 4.2：Nginx 同样承担此职责）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        // 头像等静态资源（API.md v1.8）：生产环境由 Nginx 反代同一前缀
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    // 拆包：monaco / echarts / element-plus 体量大且低频，独立成 chunk 便于缓存
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('monaco-editor')) return 'vendor-monaco'
            if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
            if (id.includes('vue-router') || id.includes('pinia') || /node_modules[\\/]vue[\\/]/.test(id)) {
              return 'vendor-vue'
            }
            return 'vendor'
          }
        },
      },
    },
  },
})
