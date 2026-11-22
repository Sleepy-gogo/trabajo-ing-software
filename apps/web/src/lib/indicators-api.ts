import { api } from "./users-api"

export type ActivityType =
  | "INICIO_SESION"
  | "INICIO_SESION_FALLIDO"
  | "INICIO_SESION_BLOQUEADO"
  | "CIERRE_SESION"
  | "RESERVA_CREADA"
  | "RESERVA_CANCELADA"
  | "VERIFICACION_ACTUALIZADA"
  | "INFORME_GENERADO"

export const activityLabels: Record<ActivityType, string> = {
  INICIO_SESION: "Inicio de sesión",
  INICIO_SESION_FALLIDO: "Inicio de sesión fallido",
  INICIO_SESION_BLOQUEADO: "Inicio de sesión bloqueado",
  CIERRE_SESION: "Cierre de sesión",
  RESERVA_CREADA: "Reserva creada",
  RESERVA_CANCELADA: "Reserva cancelada",
  VERIFICACION_ACTUALIZADA: "Verificación actualizada",
  INFORME_GENERADO: "Informe generado",
}

export type Indicator = {
  clave: "ingresos" | "reservas" | "horas" | "cancelacion" | "membresias"
  nombre: string
  unidad: "ARS" | "cantidad" | "horas" | "porcentaje"
  valor: number
  anterior: number
  variacion: number | null
}

export type Indicators = {
  desde: string
  hasta: string
  anteriorDesde: string
  anteriorHasta: string
  indicadores: Indicator[]
  ingresosPorMes: { mes: string; cuotas: number; reservas: number }[]
  espacios: {
    espacio: string
    reservas: number
    horas: number
    ingresos: number
  }[]
  demanda: { dia: number; hora: number; reservas: number }[]
  asistencia: { terminadas: number; conIngreso: number; porcentaje: number }
  sociosPorRelacion: {
    relacion: string
    cantidad: number
    pendientes: number
  }[]
  membresiasPorNivel: { nivel: string; cantidad: number }[]
  conclusiones: string[]
  ultimasAcciones: {
    tipo: ActivityType
    usuario: string | null
    detalle: string | null
    creadoEn: string
  }[]
}

export type PeriodKey = "month" | "30d" | "3m" | "12m"
export const periodLabels: Record<PeriodKey, string> = {
  month: "Este mes",
  "30d": "Últimos 30 días",
  "3m": "Últimos 3 meses",
  "12m": "Últimos 12 meses",
}

function iso(date: Date) {
  return date.toISOString().slice(0, 10)
}

export function periodRange(period: PeriodKey, today: string) {
  const end = new Date(`${today}T12:00:00Z`)
  const start = new Date(end)
  if (period === "month") start.setUTCDate(1)
  else if (period === "30d") start.setUTCDate(start.getUTCDate() - 29)
  else {
    start.setUTCMonth(start.getUTCMonth() - (period === "3m" ? 3 : 12))
    start.setUTCDate(start.getUTCDate() + 1)
  }
  return { desde: iso(start), hasta: today }
}

export const indicatorsApi = {
  get: (desde: string, hasta: string, signal?: AbortSignal) =>
    api<Indicators>(
      `/reportes/indicadores?${new URLSearchParams({ desde, hasta })}`,
      { signal }
    ),
}
