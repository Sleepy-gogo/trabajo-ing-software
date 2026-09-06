export type HealthResponse = {
  status: "ok"
}

function isHealthResponse(value: unknown): value is HealthResponse {
  return (
    typeof value === "object" &&
    value !== null &&
    "status" in value &&
    value.status === "ok"
  )
}

export async function fetchHealth(
  signal?: AbortSignal
): Promise<HealthResponse> {
  const timeout = AbortSignal.timeout(10_000)
  const requestSignal = signal ? AbortSignal.any([signal, timeout]) : timeout
  const response = await fetch("/api/health", {
    headers: { Accept: "application/json" },
    signal: requestSignal,
  }).catch((cause: unknown) => {
    if (signal?.aborted) throw cause
    throw new Error(
      timeout.aborted
        ? "La API tardó demasiado en responder."
        : "No se pudo contactar a la API.",
      { cause }
    )
  })

  if (!response.ok) {
    throw new Error(`La API respondió con el estado ${response.status}.`)
  }

  const payload: unknown = await response.json().catch((cause: unknown) => {
    if (signal?.aborted) throw cause
    throw new Error(
      timeout.aborted
        ? "La API tardó demasiado en responder."
        : "La API devolvió una respuesta inesperada.",
      { cause }
    )
  })

  if (!isHealthResponse(payload)) {
    throw new Error("La API devolvió una respuesta inesperada.")
  }

  return payload
}
