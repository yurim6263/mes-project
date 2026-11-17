import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // 컨테이너 밖(브라우저)에서 접속하려면 0.0.0.0 로 열어야 함
    port: 5173,
  },
})
