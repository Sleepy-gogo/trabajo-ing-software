import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import {
  MembershipStatusPage,
  MembershipsPage,
} from "@/pages/member/memberships"
import {
  AdminSpaceDetailPage,
  MemberServiceDetailPage,
} from "@/pages/spaces-live"
import { membersApi, type Member } from "@/lib/members-api"
import { spacesApi, calendarApi, type SpaceResponse } from "@/lib/spaces-api"
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
const space: SpaceResponse = {
  id: "e1",
  nombre: "Cancha",
  descripcion: "Cubierta",
  capacidad: 12,
  tarifaHora: 1000,
  tipo: "Cancha",
  rutaImagen: null,
  estado: "HABILITADO",
  tarifas: {},
  disponibilidades: [],
  creadoEn: "",
  actualizadoEn: "",
}
function mountAdminSpace() {
  vi.spyOn(calendarApi, "get").mockResolvedValue({
    fecha: "2026-09-30",
    estado: "HABILITADO",
    tarifaHora: 1000,
    relacionAplicada: "EXTERNO",
    franjas: [],
  })
  vi.spyOn(calendarApi, "blocks").mockResolvedValue([])
  return mount(
    <Routes>
      <Route path="/admin/spaces/:id" element={<AdminSpaceDetailPage />} />
    </Routes>,
    "/admin/spaces/e1"
  )
}
describe("Incrementos 2 y 3", () => {
  it("agrega y edita horarios por API con confirmación visible", async () => {
    const get = vi.spyOn(spacesApi, "get").mockResolvedValue(space)
    const availability = {
      id: "h1",
      espacioId: "e1",
      diaSemana: "LUNES" as const,
      horaDesde: "08:00",
      horaHasta: "22:00",
    }
    const create = vi
      .spyOn(spacesApi, "createAvailability")
      .mockImplementation(async () => {
        get.mockResolvedValue({ ...space, disponibilidades: [availability] })
        return availability
      })
    const update = vi
      .spyOn(spacesApi, "updateAvailability")
      .mockImplementation(async () => {
        const edited = {
          ...availability,
          horaDesde: "09:00",
          horaHasta: "21:00",
        }
        get.mockResolvedValue({ ...space, disponibilidades: [edited] })
        return edited
      })
    const actor = mountAdminSpace()
    await actor.click(
      await screen.findByRole("button", { name: "Agregar horario" })
    )
    expect(await screen.findByText("Horario agregado.")).toBeTruthy()
    expect(create).toHaveBeenCalledWith("e1", {
      diaSemana: "LUNES",
      horaDesde: "08:00",
      horaHasta: "22:00",
    })
    await actor.click(screen.getByRole("button", { name: /^Editar$/ }))
    fireEvent.change(screen.getByLabelText("Desde"), {
      target: { value: "09:00" },
    })
    fireEvent.change(screen.getByLabelText("Hasta"), {
      target: { value: "21:00" },
    })
    await actor.click(screen.getByRole("button", { name: "Guardar horario" }))
    expect(await screen.findByText("Horario actualizado.")).toBeTruthy()
    expect(update).toHaveBeenCalledWith("e1", "h1", {
      horaDesde: "09:00",
      horaHasta: "21:00",
    })
    expect(screen.getByText("Lunes · 09:00 a 21:00")).toBeTruthy()
  })
  it("conserva el horario y muestra el error cuando la API rechaza el cambio", async () => {
    vi.spyOn(spacesApi, "get").mockResolvedValue(space)
    vi.spyOn(spacesApi, "createAvailability").mockRejectedValue(
      new Error("El horario se superpone con otra franja.")
    )
    const actor = mountAdminSpace()
    fireEvent.change(await screen.findByLabelText("Desde"), {
      target: { value: "09:00" },
    })
    await actor.click(screen.getByRole("button", { name: "Agregar horario" }))
    expect((await screen.findByRole("alert")).textContent).toContain(
      "El horario se superpone"
    )
    expect((screen.getByLabelText("Desde") as HTMLInputElement).value).toBe(
      "09:00"
    )
    expect(screen.queryByText("Horario agregado.")).toBeNull()
  })
  it("envía estado, tarifas y bloqueos desde sus botones de guardado", async () => {
    vi.spyOn(spacesApi, "get").mockResolvedValue(space)
    const state = vi.spyOn(calendarApi, "state").mockResolvedValue(space)
    const rates = vi.spyOn(calendarApi, "rates").mockResolvedValue(space)
    const block = vi.spyOn(calendarApi, "block").mockResolvedValue({
      id: "b1",
      fecha: "2026-09-30",
      desde: "12:00",
      hasta: "13:00",
      motivo: "Mantenimiento",
    })
    const actor = mountAdminSpace()
    await actor.click(
      await screen.findByRole("button", { name: "Guardar estado" })
    )
    await waitFor(() => expect(state).toHaveBeenCalledWith("e1", "HABILITADO"))
    await actor.click(screen.getByRole("button", { name: "Guardar tarifas" }))
    await waitFor(() => expect(rates).toHaveBeenCalledWith("e1", {}))
    fireEvent.change(screen.getByLabelText("Inicio del bloqueo"), {
      target: { value: "12:00" },
    })
    fireEvent.change(screen.getByLabelText("Fin del bloqueo"), {
      target: { value: "13:00" },
    })
    await actor.type(
      screen.getByLabelText("Motivo del bloqueo"),
      "Mantenimiento"
    )
    await actor.click(screen.getByRole("button", { name: "Agregar bloqueo" }))
    await waitFor(() =>
      expect(block).toHaveBeenCalledWith(
        "e1",
        expect.objectContaining({
          desde: "12:00",
          hasta: "13:00",
          motivo: "Mantenimiento",
        })
      )
    )
  })
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
    expect(await screen.findByText("Sin horarios disponibles")).toBeTruthy()
    expect(
      (
        screen.getByRole("button", {
          name: "Reservar este espacio",
        }) as HTMLButtonElement
      ).disabled
    ).toBe(true)
    expect(
      screen.queryByRole("link", { name: "Reservar este espacio" })
    ).toBeNull()
  })
})
