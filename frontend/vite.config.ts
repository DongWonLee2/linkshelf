import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// ADR-0005: during development /api goes to the Spring Boot backend, so the backend needs no CORS setup.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
