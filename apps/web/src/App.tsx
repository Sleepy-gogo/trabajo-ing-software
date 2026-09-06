import { useQuery } from "@tanstack/react-query"
import { AlertCircle, Check, LoaderCircle, RefreshCw } from "lucide-react"
import { BrowserRouter, Route, Routes } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { fetchHealth } from "@/lib/api"

function StatusIcon({ state }: { state: "pending" | "error" | "success" }) {
  if (state === "pending") {
    return (
      <LoaderCircle
        aria-hidden="true"
        className="status-spinner size-5 text-primary"
        strokeWidth={1.8}
      />
    )
  }

  if (state === "error") {
    return (
      <AlertCircle
        aria-hidden="true"
        className="size-5 text-destructive"
        strokeWidth={1.8}
      />
    )
  }

  return (
    <span className="flex size-8 items-center justify-center rounded-full bg-primary/10 text-primary">
      <Check aria-hidden="true" className="size-4" strokeWidth={2.25} />
    </span>
  )
}

function HomePage() {
  const healthQuery = useQuery({
    queryKey: ["health"],
    queryFn: ({ signal }) => fetchHealth(signal),
    networkMode: "always",
    retry: false,
    staleTime: 30_000,
  })

  const state = healthQuery.isPending
    ? "pending"
    : healthQuery.isError
      ? "error"
      : "success"

  const errorMessage =
    healthQuery.error instanceof Error
      ? healthQuery.error.message
      : "No se pudo contactar a la API."

  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="mx-auto flex w-full max-w-5xl items-center justify-between px-6 py-6 sm:px-10 lg:px-12">
        <a
          aria-label="SERA, volver al inicio"
          className="inline-flex items-center gap-3 rounded-md text-sm font-semibold tracking-[0.12em] text-foreground uppercase focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
          href="/"
        >
          <span className="flex size-8 items-center justify-center rounded-md bg-primary text-sm font-bold text-primary-foreground">
            S
          </span>
          <span>SERA</span>
        </a>
        <span className="text-sm text-muted-foreground">Frontend</span>
      </header>

      <main
        id="main-content"
        className="mx-auto flex w-full max-w-5xl flex-col px-6 pt-16 pb-16 sm:px-10 sm:pt-24 lg:px-12 lg:pt-32"
      >
        <div className="max-w-2xl">
          <p className="mb-5 text-sm font-medium tracking-[0.12em] text-primary uppercase">
            Base lista
          </p>
          <h1 className="max-w-xl text-4xl leading-[1.08] font-semibold tracking-[-0.03em] text-balance sm:text-5xl">
            El frontend ya está en marcha.
          </h1>
          <p className="mt-6 max-w-xl text-base leading-7 text-pretty text-muted-foreground sm:text-lg">
            Comprobá la conexión con la API antes de empezar a desarrollar.
          </p>
        </div>

        <section
          aria-atomic="true"
          aria-busy={healthQuery.isPending || healthQuery.isFetching}
          aria-live="polite"
          className="mt-14 max-w-2xl rounded-xl border border-border bg-card p-6 shadow-sm sm:mt-16 sm:p-8"
        >
          <div className="flex flex-col gap-8 sm:flex-row sm:items-start sm:justify-between sm:gap-10">
            <div className="flex min-w-0 items-start gap-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-border bg-background">
                <StatusIcon state={state} />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-muted-foreground">
                  Conexión con la API
                </p>
                <h2 className="mt-1 text-xl font-semibold tracking-[-0.02em] text-balance">
                  {state === "pending" && "Comprobando el estado…"}
                  {state === "error" && "La API no responde"}
                  {state === "success" && "Frontend conectado"}
                </h2>
              </div>
            </div>

            <Button
              className="min-h-11 w-full shrink-0 sm:w-auto"
              disabled={healthQuery.isFetching}
              onClick={() => void healthQuery.refetch()}
              size="lg"
              variant={state === "error" ? "default" : "outline"}
            >
              <RefreshCw
                aria-hidden="true"
                className={healthQuery.isFetching ? "animate-spin" : undefined}
              />
              {healthQuery.isFetching ? "Actualizando…" : "Actualizar estado"}
            </Button>
          </div>

          <div className="mt-8 border-t border-border pt-6">
            {state === "pending" && (
              <p className="text-sm leading-6 text-muted-foreground">
                Consultando <code className="endpoint">GET /api/health</code>…
              </p>
            )}

            {state === "error" && (
              <div className="space-y-4">
                <p className="max-w-prose text-sm leading-6 text-muted-foreground">
                  {errorMessage} Revisá que el backend esté ejecutándose y volvé
                  a intentar.
                </p>
                <p className="endpoint">GET /api/health</p>
              </div>
            )}

            {healthQuery.isSuccess && (
              <div className="space-y-4">
                <p className="text-sm leading-6 text-muted-foreground">
                  La interfaz y la API se comunican correctamente.
                </p>
                <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
                  <code className="endpoint">GET /api/health</code>
                  <span className="inline-flex items-center gap-2 font-medium text-primary">
                    <span
                      aria-hidden="true"
                      className="size-1.5 rounded-full bg-primary"
                    />
                    {healthQuery.data.status}
                  </span>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<HomePage />} path="/" />
        <Route element={<HomePage />} path="*" />
      </Routes>
    </BrowserRouter>
  )
}

export default App
