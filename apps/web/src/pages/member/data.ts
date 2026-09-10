import {
  currentMember,
  currentUser,
  getMembershipByUserId,
  getPaymentsByUserId,
  getReservationsByUserId,
  getTicketsByUserId,
  membershipPlans,
  memberships,
  payments,
  reservations,
  spaces,
  surveys,
  tickets,
} from "@/mocks"

/**
 * Small presentation helpers kept beside the member pages.
 *
 * The values come from the shared typed fixtures so swapping them for API
 * adapters later does not require changing the page structure.
 */
export const member = currentMember
export {
  currentMember,
  currentUser,
  membershipPlans,
  memberships,
  payments,
  reservations,
  spaces,
  surveys,
  tickets,
}
export {
  getMembershipByUserId,
  getPaymentsByUserId,
  getReservationsByUserId,
  getTicketsByUserId,
}

export const money = (value: number) =>
  new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(value)

export function formatDate(
  value: string,
  options?: Intl.DateTimeFormatOptions
) {
  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "medium",
    timeZone: "America/Argentina/Buenos_Aires",
    ...options,
  }).format(new Date(value))
}

export function getPlanPrice(
  planId: string,
  relationship = currentUser.relacionUnse
) {
  const plan = membershipPlans.find((candidate) => candidate.id === planId)
  return (
    plan?.preciosPorRelacion.find((price) => price.relacion === relationship)
      ?.importeMensual ??
    plan?.importeMensual ??
    0
  )
}
