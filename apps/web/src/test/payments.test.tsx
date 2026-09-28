import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import { MemberPaymentsPage } from "@/pages/member/membership-payments"
import { AdminPaymentsPage } from "@/pages/admin/payments"
import { membersApi, type Member } from "@/lib/members-api"
import { paymentsApi, type Payment } from "@/lib/payments-api"
import { usersApi, type User } from "@/lib/users-api"

const user: User = {
  id: "u1",
  nombreCompleto: "Ada",
  email: "ada@example.com",
  dni: 12345678,
  rol: "USUARIO",
  estadoCuenta: "ACTIVO",
  qrUsuario: "SERA-U0123456789ab",
  creadoEn: "2026-09-27T12:00:00Z",
  actualizadoEn: "2026-09-27T12:00:00Z",
}
const member: Member = {
  id: "s1",
  usuarioId: "u1",
  nombreCompleto: "Ada",
  email: "ada@example.com",
  dni: 12345678,
  relacionUnse: "EXTERNO",
  estadoVerificacionUnse: "PENDIENTE",
  identificadorUnse: null,
  membresiaId: "m1",
  nivelMembresiaId: "n1",
  nivelMembresiaNombre: "General",
  estadoMembresia: "PENDIENTE_PAGO",
  proximoVencimiento: null,
}
const pending: Payment = {
  id: "p1",
  conceptoPago: "CUOTA_MENSUAL",
  idUsuario: "u1",
  titular: "Ada",
  estado: "PENDIENTE",
  medioPago: "MERCADO_PAGO",
  monto: 1000,
  comprobante: null,
  idMembresia: "m1",
  creadoEn: "2026-09-27T12:00:00Z",
  aprobadoEn: null,
  aplicadoEn: null,
  vencimientoResultante: null,
  requiereRevision: false,
  motivoRevision: null,
}
function mount(content: React.ReactNode, url: string) {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[url]}>{content}</MemoryRouter>
    </QueryClientProvider>
  )
  return userEvent.setup()
}

describe("Pagos conectados", () => {
  it("el retorno del checkout no aprueba un pago pendiente", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    vi.spyOn(membersApi, "me").mockResolvedValue(member)
    vi.spyOn(paymentsApi, "list").mockResolvedValue({
      content: [pending],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    })
    mount(<MemberPaymentsPage />, "/app/payments?view=result&state=approved")

    expect(await screen.findByText("Renovación mensual")).toBeTruthy()
    expect(
      screen.getByText(/El regreso al sitio no confirma el cobro/)
    ).toBeTruthy()
    expect(screen.getAllByText("Pendiente").length).toBeGreaterThan(0)
    expect(screen.queryByText("Pago aprobado")).toBeNull()
  })

  it("administración solo confirma efectivo pendiente", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue({ ...user, rol: "ADMIN" })
    const cash: Payment = { ...pending, id: "p2", medioPago: "EFECTIVO" }
    vi.spyOn(paymentsApi, "list").mockResolvedValue({
      content: [pending, cash],
      totalElements: 2,
      totalPages: 1,
      number: 0,
    })
    const confirm = vi.spyOn(paymentsApi, "confirmCash").mockResolvedValue({
      ...cash,
      estado: "APROBADO",
      comprobante: "SERA-p2",
    })
    const actor = mount(<AdminPaymentsPage />, "/admin/payments")

    await screen.findByRole("button", { name: "Confirmar efectivo" })
    expect(
      screen.getAllByRole("button", { name: "Confirmar efectivo" })
    ).toHaveLength(1)
    await actor.click(
      screen.getByRole("button", { name: "Confirmar efectivo" })
    )
    expect(confirm).toHaveBeenCalledWith("p2", expect.anything())
  })
})
