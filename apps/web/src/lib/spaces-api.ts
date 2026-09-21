import { api } from "@/lib/users-api"

export type Weekday =
  "LUNES" | "MARTES" | "MIERCOLES" | "JUEVES" | "VIERNES" | "SABADO" | "DOMINGO"

export type Availability = {
  id: string
  espacioId: string
  diaSemana: Weekday
  horaDesde: string
  horaHasta: string
}

export type SpaceResponse = {
  estado: "HABILITADO" | "MANTENIMIENTO" | "INUTILIZABLE" | "EN_USO"
  tarifas: Partial<Record<import("./members-api").Relationship, number>>
  id: string
  nombre: string
  descripcion: string | null
  capacidad: number
  tarifaHora: number
  tipo: string
  rutaImagen: string | null
  disponibilidades: Availability[]
  creadoEn: string
  actualizadoEn: string
}

export type SpaceInput = {
  nombre: string
  descripcion: string | null
  capacidad: number
  tarifaHora: number
  tipo: string
  rutaImagen: string | null
}

export type CreateAvailabilityInput = Pick<
  Availability,
  "diaSemana" | "horaDesde" | "horaHasta"
>

export type UpdateAvailabilityInput = Pick<
  Availability,
  "horaDesde" | "horaHasta"
>

/** Contrato HTTP de espacios y horarios. */
export const spacesApi = {
  list: (search = "", signal?: AbortSignal) => {
    const query = search.trim()
      ? `?buscar=${encodeURIComponent(search.trim())}`
      : ""
    return api<SpaceResponse[]>(`/espacios${query}`, { signal })
  },
  get: (id: string, signal?: AbortSignal) =>
    api<SpaceResponse>(`/espacios/${encodeURIComponent(id)}`, { signal }),
  save: (data: SpaceInput, id?: string) =>
    api<SpaceResponse>(
      id ? `/espacios/${encodeURIComponent(id)}` : "/espacios",
      {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(data),
      }
    ),
  remove: (id: string) =>
    api<void>(`/espacios/${encodeURIComponent(id)}`, { method: "DELETE" }),
  listAvailability: (spaceId: string, signal?: AbortSignal) =>
    api<Availability[]>(
      `/espacios/${encodeURIComponent(spaceId)}/disponibilidades`,
      { signal }
    ),
  createAvailability: (spaceId: string, data: CreateAvailabilityInput) =>
    api<Availability>(
      `/espacios/${encodeURIComponent(spaceId)}/disponibilidades`,
      { method: "POST", body: JSON.stringify(data) }
    ),
  updateAvailability: (
    spaceId: string,
    availabilityId: string,
    data: UpdateAvailabilityInput
  ) =>
    api<Availability>(
      `/espacios/${encodeURIComponent(spaceId)}/disponibilidades/${encodeURIComponent(availabilityId)}`,
      { method: "PUT", body: JSON.stringify(data) }
    ),
  removeAvailability: (spaceId: string, availabilityId: string) =>
    api<void>(
      `/espacios/${encodeURIComponent(spaceId)}/disponibilidades/${encodeURIComponent(availabilityId)}`,
      { method: "DELETE" }
    ),
}

export type CalendarResponse = {
  fecha: string
  estado: SpaceResponse["estado"]
  tarifaHora: number
  relacionAplicada: string
  franjas: { desde: string; hasta: string }[]
}
export type Block = {
  id: string
  fecha: string
  desde: string
  hasta: string
  motivo: string
}
export const calendarApi = {
  get: (id: string, date: string, signal?: AbortSignal) =>
    api<CalendarResponse>(`/espacios/${id}/calendario?fecha=${date}`, {
      signal,
    }),
  blocks: (id: string, date: string, signal?: AbortSignal) =>
    api<Block[]>(`/espacios/${id}/bloqueos?fecha=${date}`, { signal }),
  block: (id: string, data: Omit<Block, "id">) =>
    api<Block>(`/espacios/${id}/bloqueos`, {
      method: "POST",
      body: JSON.stringify(data),
    }),
  unblock: (id: string, blockId: string) =>
    api<void>(`/espacios/${id}/bloqueos/${blockId}`, { method: "DELETE" }),
  state: (id: string, estado: SpaceResponse["estado"]) =>
    api<SpaceResponse>(`/espacios/${id}/estado`, {
      method: "PUT",
      body: JSON.stringify({ estado }),
    }),
  rates: (id: string, tarifas: SpaceResponse["tarifas"]) =>
    api<SpaceResponse>(`/espacios/${id}/tarifas`, {
      method: "PUT",
      body: JSON.stringify({ tarifas }),
    }),
}
