import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// O front roda em http://localhost:5173 e chama a API por /api/...
// O proxy repassa para o Spring em http://localhost:8080 (tirando o /api), então não precisa de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.API_URL || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
