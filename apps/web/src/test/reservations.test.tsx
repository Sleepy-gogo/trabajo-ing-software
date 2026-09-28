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
  it("un retorno approved no genera un QR ni confirma desde el navegador", () => {
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
  it("crea con horario y precio cotizado por el backend", async () => {
    vi.spyOn(spacesApi, "list").mockResolvedValue([
      {
        id: "e1",
        nombre: "Cancha real",
        estado: "HABILITADO",
        capacidad: 10,
        tarifaHora: 1000,
        tarifas: {},
        tipo: "Cancha",
        descripcion: null,
        rutaImagen: null,
        disponibilidades: [],
        creadoEn: "",
        actualizadoEn: "",
      },
    ])
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
      .mockResolvedValue(pending)
    const user = mount(
      <Routes>
        <Route path="/new" element={<BookingPage />} />
        <Route
          path="/app/reservations/:id"
          element={<p>Reserva registrada</p>}
        />
      </Routes>,
      "/new?space=e1&date=2099-10-01"
    )
    await user.selectOptions(
      await screen.findByLabelText("Horario disponible"),
      "10:00"
    )
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
    expect(await screen.findByText("Reserva registrada")).toBeTruthy()
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
