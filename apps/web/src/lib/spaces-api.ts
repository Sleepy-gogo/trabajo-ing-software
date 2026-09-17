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

/**
 * HTTP contract ready for the spaces screens.
 *
 * TODO(web-wiring): replace the fixtures in the admin and member space pages
 * with TanStack Query hooks that call these functions. Keep the mapping from
 * SpaceResponse to the richer visual Space type next to those hooks.
 */
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
