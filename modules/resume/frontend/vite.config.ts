import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  base: "/resume/",
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/resume": {
        target: "http://localhost:8082",
        changeOrigin: true,
      },
    },
  },
});
