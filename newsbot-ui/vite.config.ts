import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  base: '/NewsAggregationPlatform/',
  
  envDir: path.resolve(__dirname, '../common'), 
  
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          telegram: ['@telegram-apps/sdk', '@telegram-apps/sdk-react']
        }
      }
    }
  },
  server: {
    port: 3000,
    host: true
  }
})