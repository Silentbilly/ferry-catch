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
      '/',        // или '/en' если сделаешь редирект
      '/en',
      '/tr',
      '/ru',
      '/en/ferry/kabatas-buyukada',
      '/en/ferry/kabatas-buyukada',
      '/en/ferry/kadikoy-buyukada',
      '/en/ferry/bostanci-kinaliada',
      '/en/ferry/bostanci-buyukada',
    ],
    script: 'async',
    formatting: 'minify',
  },
})
