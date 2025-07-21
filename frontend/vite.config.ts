import { defineConfig } from 'vite'

export default defineConfig({
  root: '.',
  base: './',
  build: {
    outDir: '../coincollector/src/main/resources/frontend',
    emptyOutDir: true,
  },
  server: {
    port: 3000,
  },
})
