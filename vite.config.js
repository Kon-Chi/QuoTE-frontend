import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  // root: 'vite',
  publicDir: resolve(__dirname, 'public'),
  server: {
    port: 3000,
  },
  build: {
    outDir: resolve(__dirname, 'vite'),
    emptyOutDir: false,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/index.html')
      }
    }
  }
})
console.log(__dirname)
