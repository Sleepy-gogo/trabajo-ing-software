import { api } from "./users-api"

export type PaymentState = "PENDIENTE" | "APROBADO" | "RECHAZADO" | "CANCELADO"
export type PaymentMethod = "EFECTIVO" | "MERCADO_PAGO"
export type Payment = {
  id: string
  conceptoPago: "CUOTA_MENSUAL"
  idUsuario: string
  titular: string
  estado: PaymentState
  medioPago: PaymentMethod
  monto: number
  comprobante: string | null
  idMembresia: string | null
  creadoEn: string
  aprobadoEn: string | null
  aplicadoEn: string | null
  vencimientoResultante: string | null
  requiereRevision: boolean
  motivoRevision: string | null
}
export type PaymentPage = {
  content: Payment[]
  totalElements: number
  totalPages: number
  number: number
}

export const paymentsApi = {
  list: (page = 0, state?: PaymentState, signal?: AbortSignal) => {
    const params = new URLSearchParams({ pagina: String(page) })
    if (state) params.set("estado", state)
    return api<PaymentPage>(`/pagos?${params}`, { signal })
  },
  get: (id: string, signal?: AbortSignal) =>
    api<Payment>(`/pagos/${encodeURIComponent(id)}`, { signal }),
  receipt: (id: string, signal?: AbortSignal) =>
    api<Payment>(`/pagos/${encodeURIComponent(id)}/comprobante`, { signal }),
  start: (membershipId: string, method: PaymentMethod, key: string) =>
    api<Payment>(`/pagos/membresias/${encodeURIComponent(membershipId)}`, {
      method: "POST",
      body: JSON.stringify({ medioPago: method, claveSolicitud: key }),
    }),
  confirmCash: (id: string) =>
    api<Payment>(`/pagos/${encodeURIComponent(id)}/confirmacion-efectivo`, {
      method: "POST",
    }),
  reconcile: (membershipId: string) =>
    api<{ facturasRevisadas: number }>(
      `/pagos/membresias/${encodeURIComponent(membershipId)}/conciliacion`,
      { method: "POST" }
    ),
  cancel: (id: string) =>
    api<Payment>(`/pagos/${encodeURIComponent(id)}/cancelacion`, {
      method: "POST",
    }),
}

export const paymentStateLabel: Record<PaymentState, string> = {
  PENDIENTE: "Pendiente",
  APROBADO: "Aprobado",
  RECHAZADO: "Rechazado",
  CANCELADO: "Cancelado",
}
