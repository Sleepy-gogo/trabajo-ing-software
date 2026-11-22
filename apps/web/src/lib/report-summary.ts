import type { Report } from "./reports-api"
import { formatCurrency } from "./format"

type Row = Report["filas"][number]
export type SummaryChart = {
  title: string
  data: { name: string; value: number }[]
  unit: "count" | "currency" | "hours" | "percent"
}
export type ReportSummary = {
  stats: { label: string; value: string }[]
  charts: SummaryChart[]
}

const text = (value: unknown) =>
  String(value ?? "")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (c) => c.toUpperCase()) || "Sin dato"
const number = (value: unknown) => Number(value ?? 0) || 0

/** Agrupa filas por una columna y suma un valor (o cuenta filas), de mayor a menor. */
export function groupBy(
  rows: Row[],
  key: string,
  value: (row: Row) => number = () => 1,
  name: (raw: unknown) => string = text
) {
  const totals = new Map<string, number>()
  for (const row of rows) {
    const group = name(row[key])
    totals.set(group, (totals.get(group) ?? 0) + value(row))
  }
  return [...totals]
    .map(([group, total]) => ({ name: group, value: total }))
    .sort((a, b) => b.value - a.value || a.name.localeCompare(b.name))
}

const inactive = new Set(["CANCELADA", "VENCIDA"])

/**
 * Resumen y datos de los gráficos de un informe. Se calculan con las filas guardadas, así que
 * un informe abierto más tarde muestra los mismos valores que su tabla.
 */
export function summarize(report: Report): ReportSummary {
  const rows = report.filas
  const count = { label: "Registros", value: String(rows.length) }
  switch (report.tipo) {
    case "socios":
      return {
        stats: [count],
        charts: [
          {
            title: "Socios por estado de membresía",
            data: groupBy(rows, "estado"),
            unit: "count",
          },
          {
            title: "Socios por relación con la UNSE",
            data: groupBy(rows, "relacion"),
            unit: "count",
          },
        ],
      }
    case "reservas": {
      const total = rows
        .filter((r) => !inactive.has(String(r.estado)))
        .reduce((sum, r) => sum + number(r.total), 0)
      return {
        stats: [
          count,
          {
            label: "Importe total sin canceladas ni vencidas",
            value: formatCurrency(total),
          },
        ],
        charts: [
          {
            title: "Reservas por estado",
            data: groupBy(rows, "estado"),
            unit: "count",
          },
          {
            title: "Reservas por espacio",
            data: groupBy(rows, "espacio", () => 1, String),
            unit: "count",
          },
        ],
      }
    }
    case "pagos": {
      const approved = rows.filter((r) => r.estado === "APROBADO")
      const amount = (r: Row) => number(r.monto)
      return {
        stats: [
          count,
          {
            label: "Importe aprobado",
            value: formatCurrency(approved.reduce((s, r) => s + amount(r), 0)),
          },
          {
            label: "Pagos que requieren revisión",
            value: String(rows.filter((r) => r.revision === true).length),
          },
        ],
        charts: [
          {
            title: "Importe aprobado por concepto",
            data: groupBy(approved, "concepto", amount),
            unit: "currency",
          },
          {
            title: "Importe aprobado por medio de pago",
            data: groupBy(approved, "medio", amount),
            unit: "currency",
          },
        ],
      }
    }
    case "uso_servicios": {
      const hours = rows.reduce((s, r) => s + number(r.horas), 0)
      const ended = rows.reduce(
        (s, r) => s + number(r.terminadas ?? r.reservas),
        0
      )
      const entries = rows.reduce((s, r) => s + number(r.ingresos), 0)
      return {
        stats: [
          {
            label: "Horas reservadas",
            value: `${hours.toLocaleString("es-AR")} h`,
          },
          {
            label: "Asistencia",
            value: ended ? `${Math.round((entries / ended) * 100)} %` : "—",
          },
        ],
        charts: [
          {
            title: "Horas reservadas por espacio",
            data: groupBy(rows, "espacio", (r) => number(r.horas), String),
            unit: "hours",
          },
          {
            title: "Asistencia por espacio",
            data: rows
              .filter((r) => number(r.terminadas ?? r.reservas) > 0)
              .map((r) => ({
                name: String(r.espacio),
                value: Math.round(
                  (number(r.ingresos) / number(r.terminadas ?? r.reservas)) *
                    100
                ),
              }))
              .sort((a, b) => b.value - a.value),
            unit: "percent",
          },
        ],
      }
    }
    case "actividad":
      return {
        stats: [
          count,
          {
            label: "Intentos fallidos o bloqueados",
            value: String(
              rows.filter((r) =>
                ["INICIO_SESION_FALLIDO", "INICIO_SESION_BLOQUEADO"].includes(
                  String(r.tipo)
                )
              ).length
            ),
          },
        ],
        charts: [
          {
            title: "Acciones por tipo",
            data: groupBy(rows, "accion", () => 1, String),
            unit: "count",
          },
        ],
      }
  }
}
