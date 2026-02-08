import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  ssgOptions: {
    includedRoutes: [
      '/',
      '/en',
      '/tr',
      '/ru',
      '/en/ferry/kabatas-buyukada',
      '/en/ferry/besiktas-buyukada',
      '/en/ferry/kadikoy-buyukada',
      '/en/ferry/bostanci-kinaliada',
      '/en/ferry/bostanci-buyukada',
    ],
    script: 'async',
    formatting: 'minify',
  },
})
