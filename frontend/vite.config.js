import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 3000,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/resumes': 'http://localhost:8080',
      '/sections': 'http://localhost:8080',
      '/ai': 'http://localhost:8080',
      '/templates': 'http://localhost:8080',
      '/exports': 'http://localhost:8080',
      '/job-matches': 'http://localhost:8080',
      '/notifications': 'http://localhost:8080',
    }
  }
})