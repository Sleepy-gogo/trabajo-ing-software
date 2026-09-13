import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query"
import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { BrowserRouter } from "react-router-dom"

import { TooltipProvider } from "@/components/ui/tooltip"
import App from "./App.tsx"
import "./index.css"
import { ApiError } from "@/lib/users-api"

function refreshSessionOnAuthError(error: Error) {
  if (
    error instanceof ApiError &&
    (error.status === 401 || error.status === 403)
  ) {
    void queryClient.invalidateQueries({ queryKey: ["session"] })
  }
}

const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: refreshSessionOnAuthError }),
  mutationCache: new MutationCache({ onError: refreshSessionOnAuthError }),
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
})

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <TooltipProvider>
          <App />
        </TooltipProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>
)
