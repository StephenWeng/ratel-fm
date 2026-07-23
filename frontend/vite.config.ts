import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // The backend serves the SPA under the same context path as Spring MVC.
  base: '/ratel/fm/',
  plugins: [vue()],
  resolve: {
    alias: {
      // Keep source imports stable after files move between feature folders.
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // Maven packages the frontend from target/classes/static into the Spring Boot jar.
    outDir: '../target/classes/static',
    // Maven clean owns target/classes; Vite must not empty it or Spring AOT loses compiled Java classes.
    emptyOutDir: false
  },
  server: {
    proxy: {
      // Development proxy for full context-path API requests.
      '/ratel/fm/api': {
        target: 'http://localhost:38000',
        changeOrigin: true
      },
      // Development proxy for actuator health checks used by local scripts and diagnostics.
      '/ratel/fm/actuator': {
        target: 'http://localhost:38000',
        changeOrigin: true
      },
      // Short API prefix used during frontend development, rewritten to the backend context path.
      '/api': {
        target: 'http://localhost:38000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/ratel/fm/api')
      },
      // Short actuator prefix for local development convenience.
      '/actuator': {
        target: 'http://localhost:38000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/actuator/, '/ratel/fm/actuator')
      }
    }
  }
})
