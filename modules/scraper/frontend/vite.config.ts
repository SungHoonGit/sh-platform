import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  base: "/scraper-ui/",
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/scraper": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
