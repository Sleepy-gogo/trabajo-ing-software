import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import {
  MembershipStatusPage,
  MembershipsPage,
} from "@/pages/member/memberships"
import { MemberServiceDetailPage } from "@/pages/spaces-live"
import { membersApi, type Member } from "@/lib/members-api"
import { spacesApi, calendarApi } from "@/lib/spaces-api"
import { usersApi } from "@/lib/users-api"
const member: Member = {
  id: "s1",
  usuarioId: "u1",
  nombreCompleto: "Ada",
  email: "ada@example.com",
  dni: 123,
  relacionUnse: "EXTERNO",
  estadoVerificacionUnse: "PENDIENTE",
  identificadorUnse: null,
  membresiaId: null,
  nivelMembresiaId: null,
  nivelMembresiaNombre: null,
  estadoMembresia: null,
  proximoVencimiento: null,
}
function mount(content: React.ReactNode, path = "/app/memberships/status") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>{content}</MemoryRouter>
    </QueryClientProvider>
  )
  return userEvent.setup()
}
describe("Incrementos 2 y 3", () => {
  it("un socio sin membresía puede consultar niveles", async () => {
    vi.spyOn(membersApi, "me").mockResolvedValue(member)
    mount(<MembershipStatusPage />)
    expect(
      await screen.findByRole("link", { name: "Ver niveles disponibles" })
    ).toBeTruthy()
    expect(screen.queryByText("Activa")).toBeNull()
  })
  it("cancelar exige motivo y confirmación y persiste por API", async () => {
    vi.spyOn(membersApi, "me").mockResolvedValue({
      ...member,
      membresiaId: "m1",
      nivelMembresiaNombre: "General",
      estadoMembresia: "PENDIENTE_PAGO",
    })
    const cancel = vi.spyOn(membersApi, "cancel").mockResolvedValue({})
    const actor = mount(<MembershipStatusPage />)
    await actor.type(
      await screen.findByLabelText("Motivo de cancelación"),
      "Ya no lo necesito"
    )
    await actor.click(
      screen.getByRole("button", { name: "Cancelar membresía" })
    )
    expect(cancel).not.toHaveBeenCalled()
    await actor.click(
      screen.getByRole("button", { name: "Confirmar cancelación" })
    )
    expect(await screen.findByText("La membresía fue cancelada.")).toBeTruthy()
    expect(cancel).toHaveBeenCalledWith("m1", "Ya no lo necesito")
  })
  it("no ofrece otra contratación mientras hay una pendiente", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(null as never)
    vi.spyOn(membersApi, "me").mockResolvedValue({
      ...member,
      membresiaId: "m1",
      estadoMembresia: "PENDIENTE_PAGO",
    })
    vi.spyOn(membersApi, "levels").mockResolvedValue([
      {
        id: "n1",
        nombre: "General",
        descripcion: "Acceso",
        preciosPorRelacion: { EXTERNO: 1000 },
        moneda: "ARS",
        beneficios: ["Pileta"],
        disponibleParaContratar: true,
      },
    ])
    mount(<MembershipsPage />, "/app/memberships")
    const button = await screen.findByRole("button", {
      name: "Solicitar membresía",
    })
    expect((button as HTMLButtonElement).disabled).toBe(true)
  })
  it("un espacio en mantenimiento muestra disponibilidad vacía real", async () => {
    vi.spyOn(spacesApi, "get").mockResolvedValue({
      id: "e1",
      nombre: "Cancha",
      descripcion: "Cubierta",
      capacidad: 12,
      tarifaHora: 1000,
      tipo: "Cancha",
      rutaImagen: null,
      estado: "MANTENIMIENTO",
      tarifas: {},
      disponibilidades: [],
      creadoEn: "",
      actualizadoEn: "",
    })
    vi.spyOn(calendarApi, "get").mockResolvedValue({
      fecha: "2026-09-22",
      estado: "MANTENIMIENTO",
      tarifaHora: 1000,
      relacionAplicada: "EXTERNO",
      franjas: [],
    })
    mount(
      <Routes>
        <Route path="/app/services/:id" element={<MemberServiceDetailPage />} />
      </Routes>,
      "/app/services/e1"
    )
    expect(
      await screen.findByText("No hay franjas disponibles para esta fecha.")
    ).toBeTruthy()
    expect(screen.queryByRole("button", { name: "Reservar" })).toBeNull()
  })
})
