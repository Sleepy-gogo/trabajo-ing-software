import path from "node:path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig, loadEnv } from "vite"

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, import.meta.dirname, "")
  const proxy = {
    "/api": {
      target: env.API_PROXY_TARGET || "http://localhost:4500",
      changeOrigin: true,
      headers: { "ngrok-skip-browser-warning": "true" },
    },
  }
  const allowedHosts = (env.WEB_ALLOWED_HOSTS || "")
    .split(",")
    .map((host) => host.trim())
    .filter(Boolean)

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      strictPort: true,
      allowedHosts,
      proxy,
    },
    preview: { allowedHosts, proxy },
    resolve: {
      alias: {
        "@": path.resolve(import.meta.dirname, "./src"),
      },
    },
  }
})
