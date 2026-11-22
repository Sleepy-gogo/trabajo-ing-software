import { useRef } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { calendarApi } from "@/lib/spaces-api"
import { selectionFits } from "@/lib/booking-times"
import type { SlotSelection } from "./time-slot-picker"

/**
 * Al cambiar la fecha, consulta las franjas del nuevo día y dice si el horario elegido sigue
 * libre. Ignora respuestas de una fecha que ya no es la última elegida.
 */
export function useDateChangeCheck() {
  const client = useQueryClient()
  const latest = useRef("")
  return async function check(
    spaceId: string,
    date: string,
    selection: SlotSelection
  ): Promise<"keep" | "clear" | "stale"> {
    latest.current = date
    // Mientras la fecha está incompleta no hay un día contra el cual comparar.
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return "stale"
    if (!spaceId || !selection.start) return "keep"
    try {
      const calendar = await client.fetchQuery({
        queryKey: ["calendar", spaceId, date],
        queryFn: ({ signal }) => calendarApi.get(spaceId, date, signal),
      })
      if (latest.current !== date) return "stale"
      return selectionFits(calendar.franjas, selection.start, selection.hours)
        ? "keep"
        : "clear"
    } catch {
      return latest.current === date ? "clear" : "stale"
    }
  }
}
