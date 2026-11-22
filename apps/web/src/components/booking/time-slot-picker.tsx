import { useState } from "react"
import { Clock, Minus, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import {
  availableStarts,
  endTime,
  hoursLabel,
  longestBooking,
  maxHoursFrom,
} from "@/lib/booking-times"

export type SlotSelection = { start: string; hours: number }
type Slot = { desde: string; hasta: string }

/**
 * Selector único de horario: los inicios del día como botones y la duración con "−" y "+".
 * Marca el rango completo y nunca descarta el inicio al cambiar la duración.
 */
export function TimeSlotPicker({
  slots,
  value,
  onChange,
  disabled = false,
}: {
  slots: Slot[]
  value: SlotSelection
  onChange: (value: SlotSelection) => void
  disabled?: boolean
}) {
  const [adjusted, setAdjusted] = useState<{ start: string; text: string }>()
  const starts = availableStarts(slots, 1)
  const { start, hours } = value
  const end = endTime(start, hours)
  const limit = start ? maxHoursFrom(slots, start) : longestBooking(slots)
  const atLimit = hours >= limit

  function choose(next: string) {
    const max = maxHoursFrom(slots, next)
    if (hours > max) {
      setAdjusted({
        start: next,
        text: `Desde las ${next} ${max === 1 ? "solo queda 1 hora libre" : `solo quedan ${max} horas libres`}, así que ajustamos la duración a ${hoursLabel(max)}.`,
      })
      onChange({ start: next, hours: max })
    } else {
      setAdjusted(undefined)
      onChange({ start: next, hours })
    }
  }
  function changeHours(delta: number) {
    setAdjusted(undefined)
    onChange({ start, hours: hours + delta })
  }

  if (starts.length === 0)
    return (
      <div role="status" className="py-8 text-center">
        <Clock
          className="mx-auto mb-3 size-6 text-muted-foreground"
          aria-hidden="true"
        />
        <p className="text-sm font-semibold">Sin horarios disponibles</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Probá con otra fecha.
        </p>
      </div>
    )

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <p
          id="slot-picker-starts"
          className="flex items-center gap-2 text-sm font-semibold"
        >
          <Clock className="size-4" aria-hidden="true" />
          Horario de inicio
        </p>
        <div
          role="group"
          aria-label="Duración"
          className="flex items-center gap-2"
        >
          <span className="text-sm text-muted-foreground">Duración</span>
          <Button
            type="button"
            variant="outline"
            size="icon"
            aria-label="Restar una hora"
            disabled={disabled || hours <= 1}
            onClick={() => changeHours(-1)}
          >
            <Minus aria-hidden="true" />
          </Button>
          <span className="min-w-16 text-center text-sm font-semibold tabular-nums">
            {hoursLabel(hours)}
          </span>
          <Button
            type="button"
            variant="outline"
            size="icon"
            aria-label="Sumar una hora"
            disabled={disabled || atLimit}
            onClick={() => changeHours(1)}
          >
            <Plus aria-hidden="true" />
          </Button>
        </div>
      </div>
      <div
        role="group"
        aria-labelledby="slot-picker-starts"
        className="grid grid-cols-3 gap-2 sm:grid-cols-4 xl:grid-cols-6"
      >
        {starts.map((time) => {
          const selected = time === start
          const inRange = !!start && time > start && time < end
          return (
            <button
              key={time}
              type="button"
              aria-pressed={selected}
              disabled={disabled}
              onClick={() => choose(time)}
              className={cn(
                "flex min-h-11 items-center justify-center rounded-lg border text-sm font-medium tabular-nums transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-60",
                selected
                  ? "border-primary bg-primary text-primary-foreground"
                  : inRange
                    ? "border-primary/40 bg-primary/10 text-primary"
                    : "border-slate-300 bg-card hover:border-primary/50"
              )}
            >
              {time}
            </button>
          )
        })}
      </div>
      {adjusted?.start === start && (
        <p
          role="alert"
          className="rounded-lg bg-amber-50 p-3 text-xs leading-relaxed text-amber-900"
        >
          {adjusted.text}
        </p>
      )}
      {atLimit && hours > 0 && (
        <p className="text-xs text-muted-foreground">
          {start
            ? `No hay más horas libres a continuación de las ${end}, por eso no se puede sumar otra hora.`
            : `En este día no hay franjas libres de más de ${hoursLabel(limit)}.`}
        </p>
      )}
      <p role="status" className="text-sm text-muted-foreground">
        {start ? (
          <>
            Reservás de{" "}
            <span className="font-semibold text-foreground tabular-nums">
              {start} a {end}
            </span>{" "}
            · {hoursLabel(hours)}
          </>
        ) : (
          "Elegí un horario de inicio."
        )}
      </p>
    </div>
  )
}
