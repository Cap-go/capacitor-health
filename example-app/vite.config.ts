import { resolve } from 'node:path';
import { defineConfig } from 'vite';

export default defineConfig({
  base: './',
  define: {
    __CAPGO_MAESTRO_E2E__: JSON.stringify(process.env.CAPGO_MAESTRO_E2E === '1'),
  },
  root: './src',
  build: {
    outDir: '../dist',
    minify: false,
    emptyOutDir: true,
    rollupOptions: {
      input: resolve(import.meta.dirname, 'src/index.html'),
    },
  },
});
