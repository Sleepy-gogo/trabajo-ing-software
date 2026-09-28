const argentinaDateFormatter = new Intl.DateTimeFormat("es-AR", {
  day: "2-digit",
  month: "short",
  year: "numeric",
})

const argentinaDateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
  dateStyle: "medium",
  timeStyle: "short",
})

const argentinaCurrencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 0,
})

const argentinaNumberFormatter = new Intl.NumberFormat("es-AR")

export function formatCurrency(value: number) {
  return argentinaCurrencyFormatter.format(value)
}

export function formatDate(value: string | Date) {
  const date =
    typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value)
      ? new Date(`${value}T12:00:00`)
      : new Date(value)
  return argentinaDateFormatter.format(date)
}

export function formatDateTime(value: string | Date) {
  return argentinaDateTimeFormatter.format(new Date(value))
}

export function formatNumber(value: number) {
  return argentinaNumberFormatter.format(value)
}
