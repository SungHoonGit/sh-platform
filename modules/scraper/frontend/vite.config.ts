import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

export default defineConfig({
  base: "/scraper-ui/",
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@common": path.resolve(__dirname, "src/common"),
    },
    preserveSymlinks: true,
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
