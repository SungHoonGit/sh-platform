import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  base: "/scraper/",
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: [
      { find: /^@sh-platform\/ui$/, replacement: fileURLToPath(new URL("../../../packages/ui-shared/src/index.ts", import.meta.url)) },
      { find: /^react\/jsx-runtime$/, replacement: fileURLToPath(new URL("node_modules/react/jsx-runtime.js", import.meta.url)) },
      { find: /^react$/, replacement: fileURLToPath(new URL("node_modules/react", import.meta.url)) },
    ],
  },
  server: {
    proxy: {
      "/scraper": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
