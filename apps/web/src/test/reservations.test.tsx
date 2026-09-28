import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import {
  BookingPage,
  MemberReservationsPage,
  ReservationDetails,
} from "@/pages/member/reservations"
import { reservationsApi, type Reservation } from "@/lib/reservations-api"
import { calendarApi, spacesApi } from "@/lib/spaces-api"
import { MemberServiceDetailPage } from "@/pages/spaces-live"

const pending: Reservation = {
  id: "r1",
  usuarioId: "u1",
  titular: "Ana",
  espacioId: "e1",
  espacioNombre: "Cancha real",
  fecha: "2099-10-01",
  desde: "10:00:00",
  hasta: "11:00:00",
  personas: 2,
  tarifaHora: 1000,
  relacionAplicada: "EXTERNO",
  total: 1000,
  creditoAplicado: 0,
  saldoTicket: 0,
  estado: "PENDIENTE_PAGO",
  codigo: null,
  venceEn: "2099-10-01T10:00:00-03:00",
  pagoId: "p1",
  estadoPago: "PENDIENTE",
  medioPago: "MERCADO_PAGO",
  checkoutUrl: null,
  requiereRevision: false,
  cancelable: true,
}
function mount(content: React.ReactNode, url = "/") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[url]}>{content}</MemoryRouter>
    </QueryClientProvider>
  )
  return userEvent.setup()
}
describe("Reservas conectadas", () => {
  it("un retorno approved se verifica en backend y no genera un QR por sí solo", async () => {
    const verify = vi
      .spyOn(reservationsApi, "verify")
      .mockResolvedValue(pending)
    mount(
      <ReservationDetails reservation={pending} />,
      "/?status=approved&payment_id=123"
    )
    expect(screen.queryByTitle("QR de la reserva")).toBeNull()
    expect(screen.getByText("Pendiente de pago")).toBeTruthy()
    expect(
      (
        screen.getByLabelText(
          "Número de pago de Mercado Pago"
        ) as HTMLInputElement
      ).value
    ).toBe("123")
    await waitFor(() => expect(verify).toHaveBeenCalledWith("r1", 123))
    expect(screen.queryByTitle("QR de la reserva")).toBeNull()
  })
  it("no verifica identificadores inválidos del retorno", () => {
    const verify = vi.spyOn(reservationsApi, "verify")
    mount(
      <ReservationDetails reservation={pending} />,
      "/?payment_id=NaN&status=approved"
    )
    expect(verify).not.toHaveBeenCalled()
  })
  it("cancela mediante la API solo después de confirmar", async () => {
    const cancel = vi
      .spyOn(reservationsApi, "cancel")
      .mockResolvedValue({ ...pending, estado: "CANCELADA" })
    const user = mount(<ReservationDetails reservation={pending} />)
    await user.click(screen.getByRole("button", { name: "Cancelar reserva" }))
    expect(cancel).not.toHaveBeenCalled()
    await user.click(
      screen.getByRole("button", { name: "Confirmar cancelación" })
    )
    await waitFor(() => expect(cancel).toHaveBeenCalledWith("r1"))
  })
  it("muestra únicamente los tickets con saldo devueltos por la API", async () => {
    vi.spyOn(reservationsApi, "list").mockResolvedValue([
      { ...pending, estado: "CANCELADA", saldoTicket: 500 },
      { ...pending, id: "r2", espacioNombre: "Sin saldo", estado: "CANCELADA" },
    ])
    mount(<MemberReservationsPage />, "/?tab=tickets")
    expect(await screen.findByText("Cancha real")).toBeTruthy()
    expect(screen.queryByText("Sin saldo")).toBeNull()
    expect(
      screen.getByRole("link", { name: "Usar ticket" }).getAttribute("href")
    ).toContain("ticket=r1")
  })
  it("reserva desde disponibilidad en HTTP y conserva la clave al reintentar", async () => {
    const getRandomValues = crypto.getRandomValues.bind(crypto)
    vi.stubGlobal("crypto", { getRandomValues })
    const space = {
      id: "e1",
      nombre: "Cancha real",
      estado: "HABILITADO" as const,
      capacidad: 10,
      tarifaHora: 1000,
      tarifas: {},
      tipo: "Cancha",
      descripcion: null,
      rutaImagen: null,
      disponibilidades: [],
      creadoEn: "",
      actualizadoEn: "",
    }
    vi.spyOn(spacesApi, "list").mockResolvedValue([space])
    vi.spyOn(spacesApi, "get").mockResolvedValue(space)
    vi.spyOn(calendarApi, "get").mockResolvedValue({
      fecha: pending.fecha,
      estado: "HABILITADO",
      tarifaHora: 1000,
      relacionAplicada: "EXTERNO",
      franjas: [{ desde: "10:00:00", hasta: "12:00:00" }],
    })
    vi.spyOn(reservationsApi, "list").mockResolvedValue([])
    vi.spyOn(reservationsApi, "quote").mockResolvedValue({
      tarifaHora: 1000,
      relacionAplicada: "EXTERNO",
      total: 1000,
      creditoAplicado: 0,
      aPagar: 1000,
    })
    const create = vi
      .spyOn(reservationsApi, "create")
      .mockRejectedValueOnce(
        new Error("No se pudo conectar. Intentá de nuevo.")
      )
      .mockResolvedValue(pending)
    const user = mount(
      <Routes>
        <Route path="/app/services/:id" element={<MemberServiceDetailPage />} />
        <Route path="/app/reservations/new" element={<BookingPage />} />
        <Route
          path="/app/reservations/:id"
          element={<p>Reserva registrada</p>}
        />
      </Routes>,
      "/app/services/e1"
    )
    const date = await screen.findByLabelText("Fecha de consulta")
    await user.clear(date)
    await user.type(date, "2099-10-01")
    await user.click(await screen.findByRole("radio", { name: "10:00" }))
    await user.click(
      screen.getByRole("link", { name: "Reservar este espacio" })
    )
    expect(
      (
        (await screen.findByLabelText(
          "Horario disponible"
        )) as HTMLSelectElement
      ).value
    ).toBe("10:00")
    const review = await screen.findByRole("button", {
      name: "Revisar reserva",
    })
    await waitFor(() =>
      expect((review as HTMLButtonElement).disabled).toBe(false)
    )
    await user.click(review)
    await user.click(
      await screen.findByRole("button", { name: "Crear reserva" })
    )
    expect(await screen.findByRole("alert")).toBeTruthy()
    await user.click(screen.getByRole("button", { name: "Crear reserva" }))
    expect(await screen.findByText("Reserva registrada")).toBeTruthy()
    expect(create).toHaveBeenCalledTimes(2)
    expect(create.mock.calls[0][0].claveSolicitud).toBe(
      create.mock.calls[1][0].claveSolicitud
    )
    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        espacioId: "e1",
        desde: "10:00",
        hasta: "11:00",
        medioPago: "MERCADO_PAGO",
      })
    )
  })
})
