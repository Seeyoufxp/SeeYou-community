import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：模板组件自动注册 + ElMessage/ElMessageBox 样式自动注入
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 环境 safe-delete shim 拦截 fs.rmSync 导致 emptyOutDir 报错，构建前手动清 dist 即可
    emptyOutDir: false,
    rollupOptions: {
      output: {
        // 手动分包：按实际引用（tree-shaking 后）把第三方库拆出主 chunk。
        // 注意：不能用对象写法按包名指定 element-plus，会把整包拉进 chunk 抵消按需引入。
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('@element-plus/icons-vue')) return 'icons'
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('markdown-it')) return 'markdown'
          if (id.includes('axios') || id.includes('json-bigint') || id.includes('dayjs')) return 'utils'
          if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 代理到网关，避免浏览器跨域与 ws 跨域问题
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
