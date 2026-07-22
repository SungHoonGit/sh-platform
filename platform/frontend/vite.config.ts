import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: "/platform/",
  resolve: {
    alias: {
      "@common": path.resolve(__dirname, "src/common"),
    },
    preserveSymlinks: true,
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
