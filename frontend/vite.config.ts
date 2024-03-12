import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from "node:path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // lib: {
    //   // Could also be a dictionary or array of multiple entry points
    //   entry: path.resolve(__dirname, 'src/main.tsx'),
    //   name: 'MyLib',
    //   // the proper extensions will be added
    //   fileName: 'my-lib',
    // },
    emptyOutDir: true,
    outDir: path.resolve(__dirname, '../src/main/resources/static/frontend'),
    rollupOptions: {
      output: {
        assetFileNames: 'css/[name][extname]',
        chunkFileNames: 'js/[name].[hash].js',
        entryFileNames: 'js/scripts.js'
      }
    }
  },
  resolve: {
    alias: {
      '@': path.join(__dirname, 'src')
    },
  },
})
