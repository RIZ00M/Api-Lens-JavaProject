import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// API Lens frontend build config. The dev server proxies /api to the
// Spring Boot backend so the browser only ever talks to one origin
// during local development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
