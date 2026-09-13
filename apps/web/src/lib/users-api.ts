export type UserRole = "ADMIN" | "STAFF" | "USUARIO"
export type AccountState = "ACTIVO" | "DESHABILITADO" | "INACTIVO"
export type User = {
  id: string
  nombreCompleto: string
  email: string
  dni: number
  rol: UserRole
  estadoCuenta: AccountState
  qrUsuario: string
  creadoEn: string
  actualizadoEn: string
}
export type Profile = Pick<User, "nombreCompleto" | "email" | "dni">
export type UserInput = Profile & {
  rol: UserRole
  estadoCuenta?: AccountState
  password?: string
}

export class ApiError extends Error {
  status: number
  fields: Record<string, string>
  constructor(
    status: number,
    message: string,
    fields: Record<string, string> = {}
  ) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set("Accept", "application/json")
  if (options.body) headers.set("Content-Type", "application/json")
  if (options.method && options.method !== "GET") {
    const csrf = await api<{ token: string; headerName: string }>("/auth/csrf")
    headers.set(csrf.headerName, csrf.token)
  }
  const timeout = AbortSignal.timeout(15_000)
  const response = await fetch(`/api${path}`, {
    ...options,
    headers,
    credentials: "same-origin",
    signal: options.signal
      ? AbortSignal.any([options.signal, timeout])
      : timeout,
  }).catch((cause: unknown) => {
    if (options.signal?.aborted) throw cause
    throw new Error(
      "No se pudo contactar a la API. Revisá la conexión e intentá de nuevo.",
      { cause }
    )
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as {
      mensaje?: string
      campos?: Record<string, string>
    } | null
    throw new ApiError(
      response.status,
      body?.mensaje ?? "No se pudo completar la operación.",
      body?.campos
    )
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const usersApi = {
  me: (signal?: AbortSignal) => api<User>("/usuarios/me", { signal }),
  login: (data: { email: string; password: string }) =>
    api<User>("/auth/login", { method: "POST", body: JSON.stringify(data) }),
  register: (data: Profile & { password: string }) =>
    api<User>("/auth/registro", { method: "POST", body: JSON.stringify(data) }),
  logout: () => api<void>("/auth/logout", { method: "POST" }),
  profile: (data: Profile) =>
    api<User>("/usuarios/me", { method: "PUT", body: JSON.stringify(data) }),
  list: (search: string, signal?: AbortSignal) =>
    api<User[]>(`/usuarios?buscar=${encodeURIComponent(search)}`, { signal }),
  get: (id: string, signal?: AbortSignal) =>
    api<User>(`/usuarios/${encodeURIComponent(id)}`, { signal }),
  save: (data: UserInput, id?: string) =>
    api<User>(id ? `/usuarios/${encodeURIComponent(id)}` : "/usuarios", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(data),
    }),
  deactivate: (id: string) =>
    api<void>(`/usuarios/${encodeURIComponent(id)}`, { method: "DELETE" }),
  password: (id: string, password: string) =>
    api<void>(`/usuarios/${encodeURIComponent(id)}/password`, {
      method: "PUT",
      body: JSON.stringify({ password }),
    }),
}
