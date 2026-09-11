import js from "@eslint/js"
import globals from "globals"
import reactHooks from "eslint-plugin-react-hooks"
import reactRefresh from "eslint-plugin-react-refresh"
import tseslint from "typescript-eslint"
import { defineConfig, globalIgnores } from "eslint/config"

export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ["src/components/ui/button.tsx"],
    rules: {
      "react-refresh/only-export-components": [
        "error",
        { allowExportNames: ["buttonVariants"] },
      ],
    },
  },
  {
    files: ["src/components/ui/badge.tsx"],
    rules: {
      "react-refresh/only-export-components": [
        "error",
        { allowExportNames: ["badgeVariants"] },
      ],
    },
  },
  {
    files: ["src/components/ui/sidebar.tsx"],
    rules: {
      "react-refresh/only-export-components": [
        "error",
        { allowExportNames: ["useSidebar"] },
      ],
    },
  },
  {
    files: ["src/components/ui/tabs.tsx"],
    rules: {
      "react-refresh/only-export-components": [
        "error",
        { allowExportNames: ["tabsListVariants"] },
      ],
    },
  },
  {
    files: ["src/pages/admin/common.tsx"],
    rules: {
      "react-refresh/only-export-components": [
        "error",
        { allowExportNames: ["exportCsv"] },
      ],
    },
  },
])
