import react from "@vitejs/plugin-react"
import { VitePWA } from "vite-plugin-pwa"
import { defineConfig } from "vitest/config"

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      manifest: {
        name: "VetDose",
        short_name: "VetDose",
        description: "Калькулятор доз для ветеринарного лікаря змішаної практики",
        lang: "uk",
        theme_color: "#0f766e",
        background_color: "#ffffff",
        display: "standalone",
        start_url: "/",
        icons: [
          { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
          { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
          {
            src: "/icons/icon-512-maskable.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable",
          },
        ],
      },
      workbox: {
        // The backend's /api/v1/sync is the only network call the app makes,
        // and it is driven explicitly through runSync() (data/sync.ts), not
        // through the page's own asset fetches — nothing under /api needs to
        // be cached by the service worker.
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  test: {
    environment: "jsdom",
    globals: false,
  },
})
