export function availableStarts(
  slots: { desde: string; hasta: string }[],
  hours: number
): string[] {
  const starts: string[] = []
  for (const slot of slots) {
    const [h, m, seconds = 0] = slot.desde.split(":").map(Number)
    const [endH, endM] = slot.hasta.split(":").map(Number)
    for (
      let minute = Math.ceil((h * 60 + m + seconds / 60) / 30) * 30;
      minute + hours * 60 <= endH * 60 + endM;
      minute += 30
    ) {
      starts.push(
        `${String(Math.floor(minute / 60)).padStart(2, "0")}:${String(minute % 60).padStart(2, "0")}`
      )
    }
  }
  return starts
}

type Slot = { desde: string; hasta: string }

function minutes(time: string) {
  const [h, m, seconds = 0] = time.split(":").map(Number)
  return h * 60 + m + seconds / 60
}

function clock(minute: number) {
  return `${String(Math.floor(minute / 60)).padStart(2, "0")}:${String(minute % 60).padStart(2, "0")}`
}

/** Hora de fin de una reserva que empieza en `start` y dura `hours` horas. */
export function endTime(start: string, hours: number) {
  return start ? clock(minutes(start) + hours * 60) : ""
}

/** Horas completas libres a continuación de `start`, sin cruzar reservas ni bloqueos. */
export function maxHoursFrom(slots: Slot[], start: string) {
  if (!start) return 0
  const from = minutes(start)
  const slot = slots.find(
    (s) => from >= minutes(s.desde) && from < minutes(s.hasta)
  )
  return slot ? Math.floor((minutes(slot.hasta) - from) / 60) : 0
}

/** Mayor duración que se puede reservar en el día, para limitar el control sin inicio elegido. */
export function longestBooking(slots: Slot[]) {
  return Math.max(
    0,
    ...availableStarts(slots, 1).map((start) => maxHoursFrom(slots, start))
  )
}

/** Indica si el rango completo elegido sigue libre en las franjas del día. */
export function selectionFits(slots: Slot[], start: string, hours: number) {
  return (
    !!start &&
    availableStarts(slots, 1).includes(start) &&
    maxHoursFrom(slots, start) >= hours
  )
}

export function hoursLabel(hours: number) {
  return `${hours} ${hours === 1 ? "hora" : "horas"}`
}
