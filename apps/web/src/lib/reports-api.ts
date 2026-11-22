import { api } from "./users-api"

import { activityLabels } from "./indicators-api"

export type ReportType =
  "socios" | "reservas" | "pagos" | "uso_servicios" | "actividad"
export type ReportFilters = {
  tipo: ReportType
  desde: string
  hasta: string
  estado: string
  relacion: string
  espacioId: string | null
}
export type Report = {
  id: string
  tipo: ReportType
  filtros: ReportFilters
  creadoEn: string
  creadoPor: string
  espacioNombre: string | null
  columnas: { key: string; label: string }[]
  filas: Record<string, string | number | boolean | null>[]
  resumen: { registros: number; importe_aprobado?: number }
}
export const reportLabels: Record<ReportType, string> = {
  socios: "Socios",
  reservas: "Reservas",
  pagos: "Pagos",
  uso_servicios: "Uso de servicios",
  actividad: "Actividad",
}
export const reportStates: Record<ReportType, string[]> = {
  socios: [
    "ACTIVA",
    "VENCIDA",
    "PENDIENTE_PAGO",
    "SUSPENDIDA",
    "CANCELADA",
    "SIN_MEMBRESIA",
    "DESHABILITADO",
    "INACTIVO",
  ],
  reservas: [
    "PENDIENTE_PAGO",
    "CONFIRMADA",
    "CANCELADA",
    "VENCIDA",
    "FINALIZADA",
    "EN_CURSO",
    "CONSUMIDA",
  ],
  pagos: ["PENDIENTE", "APROBADO", "RECHAZADO", "CANCELADO"],
  uso_servicios: [],
  actividad: Object.keys(activityLabels),
}
/** Etiqueta legible de un estado o tipo de acción de los filtros. */
export function stateLabel(value: string) {
  return (
    activityLabels[value as keyof typeof activityLabels] ??
    value.replaceAll("_", " ").toLowerCase()
  )
}
export const reportsApi = {
  generate: (data: ReportFilters) =>
    api<Report>("/reportes", { method: "POST", body: JSON.stringify(data) }),
  list: (page = 0, signal?: AbortSignal) =>
    api<Report[]>(`/reportes?pagina=${page}`, { signal }),
  get: (id: string, signal?: AbortSignal) =>
    api<Report>(`/reportes/${id}`, { signal }),
}
