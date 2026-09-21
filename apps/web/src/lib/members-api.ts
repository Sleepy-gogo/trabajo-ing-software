import { api } from "./users-api"
export const relationships = [
  "ESTUDIANTE",
  "DOCENTE",
  "NO_DOCENTE",
  "GRADUADO",
  "EXTERNO",
  "VISITANTE",
] as const
export type Relationship = (typeof relationships)[number]
export type MemberState =
  "PENDIENTE_PAGO" | "ACTIVA" | "VENCIDA" | "SUSPENDIDA" | "CANCELADA"
export type Level = {
  id: string
  nombre: string
  descripcion: string
  preciosPorRelacion: Partial<Record<Relationship, number>>
  moneda: string
  beneficios: string[]
  disponibleParaContratar: boolean
}
export type LevelInput = Omit<Level, "id" | "moneda">
export type Member = {
  id: string
  usuarioId: string
  nombreCompleto: string
  email: string
  dni: number
  relacionUnse: Relationship
  estadoVerificacionUnse: "PENDIENTE" | "VERIFICADA" | "RECHAZADA"
  identificadorUnse: string | null
  membresiaId: string | null
  nivelMembresiaId: string | null
  nivelMembresiaNombre: string | null
  estadoMembresia: MemberState | null
  proximoVencimiento: string | null
}
export type MemberInput = Pick<
  Member,
  "usuarioId" | "relacionUnse" | "identificadorUnse" | "nivelMembresiaId"
>
export type MemberUpdate = Pick<
  Member,
  | "relacionUnse"
  | "estadoVerificacionUnse"
  | "identificadorUnse"
  | "nivelMembresiaId"
  | "estadoMembresia"
> & { motivo: string }
export type Audit = {
  id: string
  responsableId: string
  motivo: string
  detalle: string
  fecha: string
}
export const membersApi = {
  levels: (all = false, signal?: AbortSignal) =>
    api<Level[]>(`/niveles-membresia?soloDisponibles=${!all}`, { signal }),
  saveLevel: (data: LevelInput, id?: string) =>
    api<Level>(`/niveles-membresia${id ? `/${id}` : ""}`, {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(data),
    }),
  disableLevel: (id: string) =>
    api<void>(`/niveles-membresia/${id}`, { method: "DELETE" }),
  me: async (signal?: AbortSignal) =>
    (await api<Member | undefined>("/socios/me", { signal })) ?? null,
  list: (
    search: string,
    state: string,
    relation: string,
    signal?: AbortSignal
  ) =>
    api<Member[]>(
      `/socios?${new URLSearchParams({ buscar: search, ...(state ? { estadoMembresia: state } : {}), ...(relation ? { relacionUnse: relation } : {}) })}`,
      { signal }
    ),
  register: (data: MemberInput) =>
    api<Member>("/socios", { method: "POST", body: JSON.stringify(data) }),
  update: (id: string, data: MemberUpdate) =>
    api<Member>(`/socios/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  contract: (socioId: string, nivelMembresiaId: string) =>
    api("/membresias", {
      method: "POST",
      body: JSON.stringify({ socioId, nivelMembresiaId }),
    }),
  cancel: (id: string, motivo: string) =>
    api(`/membresias/${id}/cancelacion`, {
      method: "POST",
      body: JSON.stringify({ motivo }),
    }),
  audit: (id: string, signal?: AbortSignal) =>
    api<Audit[]>(`/socios/${id}/historial`, { signal }),
}
export function label(value: string | null | undefined) {
  return value
    ? value
        .toLowerCase()
        .replaceAll("_", " ")
        .replace(/^./, (c) => c.toUpperCase())
    : "Sin membresía"
}
