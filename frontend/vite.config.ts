import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    // 开发阶段代理到后端
    proxy: {
      // 所有 /api 开头的请求统一转发到 Gateway（9000）
      '/api': {
        target: 'http://127.0.0.1:9000',
        changeOrigin: true,
      },
      // 文件上传/头像直通 user-service（Gateway 是 WebFlux，不支持 multipart）
      '/user': {
        target: 'http://127.0.0.1:9001',
        changeOrigin: true,
      },
      // 静态资源（头像图片）直通 user-service
      '/static': {
        target: 'http://127.0.0.1:9001',
        changeOrigin: true,
      },
    },
  },
})
