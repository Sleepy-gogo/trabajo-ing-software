import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Routes, Route } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  MembershipStatusPage,
  MembershipsPage,
} from "@/pages/member/memberships"
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
  beforeEach(() => {
    vi.spyOn(paymentsApi, "membershipBilling").mockResolvedValue({
      pagoPendiente: pending,
      suscripcion: {
        id: "sub1",
        preapprovalId: "mp1",
        estado: "pending",
        checkoutUrl: "https://mercadopago.example/checkout",
      },
    })
  })
  it("verifica al regresar y permite reintentar sin confiar en la URL", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    const membership = vi.spyOn(membersApi, "me").mockResolvedValue(member)
    vi.spyOn(paymentsApi, "list").mockResolvedValue({
      content: [pending],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    })
    const verify = vi
      .spyOn(membersApi, "verifyPayment")
      .mockResolvedValue({ facturasRevisadas: 0 })
    const actor = mount(
      <MembershipStatusPage />,
      "/app/memberships/status?status=approved"
    )
    await screen.findByText(/Mercado Pago todavía no confirmó/)
    expect(verify).toHaveBeenCalledTimes(1)
    expect(verify).toHaveBeenCalledWith("m1")
    expect(screen.queryByText("Activa")).toBeNull()
    membership.mockResolvedValue({
      ...member,
      estadoMembresia: "ACTIVA",
      proximoVencimiento: "2026-10-28",
    })
    await actor.click(
      screen.getByRole("button", { name: "Ya pagué, verificar pago" })
    )
    await screen.findByText("Pago confirmado. Tu membresía está activa.")
    await waitFor(() => expect(verify).toHaveBeenCalledTimes(2))
    expect(screen.getByText("Activa")).toBeTruthy()
  })

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

  it("verifica Mercado Pago en una membresía activa sin otro pago pendiente", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    vi.spyOn(membersApi, "me").mockResolvedValue({
      ...member,
      estadoMembresia: "ACTIVA",
      proximoVencimiento: "2026-10-28",
    })
    vi.mocked(paymentsApi.membershipBilling).mockResolvedValue({
      pagoPendiente: null,
      suscripcion: {
        id: "sub1",
        preapprovalId: "mp1",
        estado: "authorized",
        checkoutUrl: "https://mercadopago.example/checkout",
      },
    })
    const verify = vi
      .spyOn(membersApi, "verifyPayment")
      .mockResolvedValue({ facturasRevisadas: 1 })
    const actor = mount(<MembershipStatusPage />, "/app/memberships/status")
    await actor.click(
      await screen.findByRole("button", { name: "Ya pagué, verificar pago" })
    )
    expect(
      await screen.findByText(
        "Verificación completada. Tu membresía está activa."
      )
    ).toBeTruthy()
    expect(verify).toHaveBeenCalledWith("m1")
    expect(
      screen.queryByText("Pago confirmado. Tu membresía está activa.")
    ).toBeNull()
  })

  it("no ofrece efectivo cuando el historial filtrado oculta el pendiente de Mercado Pago", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    vi.spyOn(membersApi, "me").mockResolvedValue(member)
    vi.spyOn(paymentsApi, "list").mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
    })
    const actor = mount(<MemberPaymentsPage />, "/app/payments")
    await actor.selectOptions(
      await screen.findByLabelText("Estado"),
      "APROBADO"
    )
    expect(
      await screen.findByText(/Ya tenés un pago de membresía pendiente/)
    ).toBeTruthy()
    expect(
      screen.queryByRole("button", { name: "Solicitar pago en efectivo" })
    ).toBeNull()
    expect(screen.getByRole("link", { name: "Mi membresía" })).toBeTruthy()
  })

  it("permite verificar una membresía vencida con suscripción y no propone otro cobro", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    vi.spyOn(membersApi, "me").mockResolvedValue({
      ...member,
      estadoMembresia: "VENCIDA",
    })
    const verify = vi
      .spyOn(membersApi, "verifyPayment")
      .mockResolvedValue({ facturasRevisadas: 0 })
    const actor = mount(<MembershipStatusPage />, "/app/memberships/status")
    await actor.click(
      await screen.findByRole("button", { name: "Ya pagué, verificar pago" })
    )
    expect(
      await screen.findByText(/Mercado Pago todavía no confirmó/)
    ).toBeTruthy()
    expect(verify).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole("link", { name: "Renovar membresía" })).toBeNull()
  })

  it("una suscripción remota cancelada requiere cerrar la solicitud antes de volver a contratar", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    vi.spyOn(membersApi, "me").mockResolvedValue(member)
    vi.mocked(paymentsApi.membershipBilling).mockResolvedValue({
      pagoPendiente: pending,
      suscripcion: {
        id: "sub1",
        preapprovalId: "mp1",
        estado: "canceled",
        checkoutUrl: "https://mercadopago.example/checkout",
      },
    })
    const verify = vi.spyOn(membersApi, "verifyPayment")
    mount(<MembershipStatusPage />, "/app/memberships/status")
    expect(
      await screen.findByText(/Cancelá esta solicitud para volver a contratar/)
    ).toBeTruthy()
    expect(
      screen.queryByRole("button", { name: "Continuar a Mercado Pago" })
    ).toBeNull()
    expect(
      screen.queryByRole("button", { name: "Ya pagué, verificar pago" })
    ).toBeNull()
    expect(verify).not.toHaveBeenCalled()
  })

  it("cancela y vuelve a contratar sin mostrar el pago ni los avisos de la solicitud anterior", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    const me = vi
      .spyOn(membersApi, "me")
      .mockResolvedValue({ ...member, estadoMembresia: "ACTIVA" })
    vi.mocked(paymentsApi.membershipBilling).mockResolvedValue({
      pagoPendiente: null,
      suscripcion: null,
    })
    vi.spyOn(membersApi, "levels").mockResolvedValue([
      {
        id: "n1",
        nombre: "General",
        descripcion: "Acceso",
        preciosPorRelacion: { EXTERNO: 1000 },
        moneda: "ARS",
        beneficios: [],
        disponibleParaContratar: true,
      },
    ])
    const approved: Payment = {
      ...pending,
      medioPago: "EFECTIVO",
      estado: "APROBADO",
    }
    const list = vi.spyOn(paymentsApi, "list").mockResolvedValue({
      content: [approved],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    })
    const cancel = vi
      .spyOn(membersApi, "cancel")
      .mockImplementation(async () => {
        me.mockResolvedValue({ ...member, estadoMembresia: "CANCELADA" })
      })
    const contract = vi
      .spyOn(membersApi, "contract")
      .mockImplementation(async () => {
        me.mockResolvedValue(member)
        const next: Payment = { ...pending, id: "p-new", medioPago: "EFECTIVO" }
        vi.mocked(paymentsApi.membershipBilling).mockResolvedValue({
          pagoPendiente: next,
          suscripcion: null,
        })
        list.mockResolvedValue({
          content: [next, approved],
          totalElements: 2,
          totalPages: 1,
          number: 0,
        })
        return { id: "m1" }
      })
    const actor = mount(
      <Routes>
        <Route
          path="/app/memberships/status"
          element={<MembershipStatusPage />}
        />
        <Route path="/app/memberships" element={<MembershipsPage />} />
        <Route path="/app/payments" element={<MemberPaymentsPage />} />
      </Routes>,
      "/app/memberships/status"
    )
    await actor.type(
      await screen.findByLabelText("Motivo de cancelación"),
      "Cambiar de medio"
    )
    await actor.click(
      screen.getByRole("button", { name: "Cancelar membresía" })
    )
    await actor.click(
      await screen.findByRole("button", { name: "Confirmar cancelación" })
    )
    await actor.click(
      await screen.findByRole("link", {
        name: "Consultar niveles y volver a contratar",
      })
    )
    await actor.selectOptions(
      await screen.findByLabelText("Cómo querés pagar"),
      "EFECTIVO"
    )
    await actor.click(
      screen.getByRole("button", { name: "Solicitar membresía" })
    )
    await actor.click(
      await screen.findByRole("button", {
        name: "Solicitar pago en efectivo",
      })
    )
    expect(
      await screen.findByText(/Ya tenés un pago de membresía pendiente/)
    ).toBeTruthy()
    expect(cancel).toHaveBeenCalledWith("m1", "Cambiar de medio")
    expect(contract).toHaveBeenCalledWith("s1", "n1", "EFECTIVO")
    expect(screen.queryByText("La membresía fue cancelada.")).toBeNull()
    expect(
      screen.queryByRole("button", { name: "Solicitar pago en efectivo" })
    ).toBeNull()
  })
})
