import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 第一阶段：将前端对 /api 的请求全部转发到本地启动的 Java Spring Boot 后端
      '/api': {
        target: 'http://localhost:3002',
        changeOrigin: true,
        // 这里不需要 rewrite，因为我们 Java 的 RequestMapping 也是 /api 开头
      }
    }
  }
})
