import { SERIES_1 } from "./horizontal-bars"

const days = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]
const dayNames = [
  "lunes",
  "martes",
  "miércoles",
  "jueves",
  "viernes",
  "sábado",
  "domingo",
]

export function DemandHeatmap({
  cells,
}: {
  cells: { dia: number; hora: number; reservas: number }[]
}) {
  if (cells.length === 0)
    return (
      <p className="text-sm text-muted-foreground">
        No hay reservas confirmadas en este período.
      </p>
    )
  const max = Math.max(...cells.map((c) => c.reservas))
  const first = Math.min(8, ...cells.map((c) => c.hora))
  const last = Math.max(21, ...cells.map((c) => c.hora))
  const hours = Array.from({ length: last - first + 1 }, (_, i) => first + i)
  const value = (dia: number, hora: number) =>
    cells.find((c) => c.dia === dia && c.hora === hora)?.reservas ?? 0
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-separate border-spacing-0.5 text-xs">
        <caption className="sr-only">
          Reservas por día de la semana y hora de inicio de cada franja
        </caption>
        <thead>
          <tr>
            <th scope="col" className="w-10" />
            {hours.map((h) => (
              <th
                key={h}
                scope="col"
                className="pb-1 font-normal text-muted-foreground tabular-nums"
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {days.map((day, i) => (
            <tr key={day}>
              <th
                scope="row"
                className="pr-2 text-left font-medium text-muted-foreground"
              >
                {day}
              </th>
              {hours.map((h) => {
                const n = value(i + 1, h)
                const share = n === 0 ? 0 : 15 + Math.round((n / max) * 85)
                return (
                  <td
                    key={h}
                    title={`${dayNames[i]} de ${h} a ${h + 1}: ${n} ${n === 1 ? "reserva" : "reservas"}`}
                    className="h-8 min-w-8 rounded text-center tabular-nums"
                    style={{
                      background:
                        n === 0
                          ? "var(--muted)"
                          : `color-mix(in oklch, ${SERIES_1} ${share}%, white)`,
                      color: share > 55 ? "white" : "var(--foreground)",
                    }}
                  >
                    {n || ""}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="mt-3 text-xs text-muted-foreground">
        Cada celda indica la cantidad de reservas. Más oscuro, más demanda.
      </p>
    </div>
  )
}
