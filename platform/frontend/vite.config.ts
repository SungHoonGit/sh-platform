import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: "/platform/",
  resolve: {
    alias: [
      { find: /^@sh-platform\/ui$/, replacement: fileURLToPath(new URL("../../packages/ui-shared/src/index.ts", import.meta.url)) },
      { find: /^react\/jsx-runtime$/, replacement: fileURLToPath(new URL("node_modules/react/jsx-runtime.js", import.meta.url)) },
      { find: /^react$/, replacement: fileURLToPath(new URL("node_modules/react", import.meta.url)) },
    ],
  },
  server: {
    port: 3001,
    proxy: {
      "/api": "http://localhost:8080",
      "/scraper": "http://localhost:8081",
    },
  },
  build: {
    outDir: "dist",
  },
});
