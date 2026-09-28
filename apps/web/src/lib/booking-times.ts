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
