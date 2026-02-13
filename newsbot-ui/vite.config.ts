import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/NewsAggregationPlatform/', // Must match your GitHub repo name
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    // Ensure proper chunk size for Telegram
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
    host: true // Allow external connections for ngrok testing
  }
})