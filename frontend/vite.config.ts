import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser only talks to this dev server. It forwards API calls to the right service,
// so there is no CORS to configure. The first matching entry wins, so the specific ones come first.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    allowedHosts: true, // dev only: lets a Codespaces forwarded address through
    proxy: {
      '^/api/(auth|me|hospitals|staff)': { target: 'http://localhost:8081', changeOrigin: true },
      '^/api/analytics': { target: 'http://localhost:8084', changeOrigin: true },
      '^/api/alerts': { target: 'http://localhost:8083', changeOrigin: true },
      '/api': { target: 'http://localhost:8082', changeOrigin: true },
    },
  },
});
