import { api } from "./users-api"
import type { PaymentMethod, PaymentState } from "./payments-api"

export type Reservation = {
  id: string
  usuarioId: string
  titular: string
  espacioId: string
  espacioNombre: string
  fecha: string
  desde: string
  hasta: string
  personas: number
  tarifaHora: number
  relacionAplicada: string
  total: number
  creditoAplicado: number
  saldoTicket: number
  estado:
    "PENDIENTE_PAGO" | "CONFIRMADA" | "CANCELADA" | "VENCIDA" | "FINALIZADA"
  codigo: string | null
  venceEn: string
  pagoId: string | null
  estadoPago: PaymentState | null
  medioPago: PaymentMethod | null
  checkoutUrl: string | null
  cancelable: boolean
  requiereRevision: boolean
}
export type BookingInput = {
  espacioId: string
  fecha: string
  desde: string
  hasta: string
  personas: number
  ticketId: string | null
  medioPago: PaymentMethod
  claveSolicitud: string
}
export type Quote = {
  tarifaHora: number
  relacionAplicada: string
  total: number
  creditoAplicado: number
  aPagar: number
}
export const reservationState: Record<Reservation["estado"], string> = {
  PENDIENTE_PAGO: "Pendiente de pago",
  CONFIRMADA: "Confirmada",
  CANCELADA: "Cancelada",
  VENCIDA: "Vencida",
  FINALIZADA: "Finalizada",
}
export const reservationsApi = {
  list: (all = false, signal?: AbortSignal) =>
    api<Reservation[]>(`/reservas?todas=${all}`, { signal }),
  get: (id: string, signal?: AbortSignal) =>
    api<Reservation>(`/reservas/${id}`, { signal }),
  quote: (data: BookingInput, signal?: AbortSignal) =>
    api<Quote>("/reservas/cotizacion", {
      method: "POST",
      body: JSON.stringify(data),
      signal,
    }),
  create: (data: BookingInput) =>
    api<Reservation>("/reservas", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  cancel: (id: string) =>
    api<Reservation>(`/reservas/${id}/cancelacion`, { method: "POST" }),
  checkout: (id: string) =>
    api<Reservation>(`/reservas/${id}/checkout`, { method: "POST" }),
  verify: (id: string, paymentId: number) =>
    api<Reservation>(`/reservas/${id}/verificar-pago`, {
      method: "POST",
      body: JSON.stringify({ pagoId: paymentId }),
    }),
}
