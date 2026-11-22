import { useState } from "react"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import {
  TimeSlotPicker,
  type SlotSelection,
} from "@/components/booking/time-slot-picker"
import {
  endTime,
  longestBooking,
  maxHoursFrom,
  selectionFits,
} from "@/lib/booking-times"
import { BookingPage } from "@/pages/member/booking"
import { MemberServiceDetailPage } from "@/pages/spaces-live"
import { reservationsApi, type Reservation } from "@/lib/reservations-api"
import { calendarApi, spacesApi } from "@/lib/spaces-api"

const tarde = [{ desde: "18:00:00", hasta: "21:00:00" }]

function Picker({
  slots = tarde,
  initial = { start: "", hours: 1 },
}: {
  slots?: { desde: string; hasta: string }[]
  initial?: SlotSelection
}) {
  const [value, setValue] = useState(initial)
  return <TimeSlotPicker slots={slots} value={value} onChange={setValue} />
}

const space = {
  id: "e1",
  nombre: "Cancha de prueba",
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

function calendar(fecha: string, franjas: { desde: string; hasta: string }[]) {
  return {
    fecha,
    estado: "HABILITADO" as const,
    tarifaHora: 1000,
    relacionAplicada: "EXTERNO",
    franjas,
  }
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

function mockBookingApis() {
  vi.spyOn(spacesApi, "list").mockResolvedValue([space])
  vi.spyOn(spacesApi, "get").mockResolvedValue(space)
  vi.spyOn(reservationsApi, "list").mockResolvedValue([])
  vi.spyOn(reservationsApi, "quote").mockResolvedValue({
    tarifaHora: 1000,
    relacionAplicada: "EXTERNO",
    total: 2000,
    creditoAplicado: 0,
    aPagar: 2000,
  })
}

describe("Cálculo del horario", () => {
  it("mide las horas libres a continuación de un inicio", () => {
    expect(maxHoursFrom(tarde, "18:00")).toBe(3)
    expect(maxHoursFrom(tarde, "19:30")).toBe(1)
    expect(maxHoursFrom(tarde, "21:00")).toBe(0)
    expect(maxHoursFrom(tarde, "")).toBe(0)
    expect(longestBooking([])).toBe(0)
    expect(longestBooking(tarde)).toBe(3)
    expect(endTime("18:30", 2)).toBe("20:30")
    expect(selectionFits(tarde, "19:00", 2)).toBe(true)
    expect(selectionFits(tarde, "19:00", 3)).toBe(false)
    expect(selectionFits(tarde, "19:15", 1)).toBe(false)
  })
})

describe("Selector de horario", () => {
  it("al aumentar la duración conserva el inicio y extiende el rango", async () => {
    const user = userEvent.setup()
    render(<Picker />)
    await user.click(screen.getByRole("button", { name: "18:00" }))
    await user.click(screen.getByRole("button", { name: "Sumar una hora" }))
    expect(
      screen.getByRole("button", { name: "18:00" }).getAttribute("aria-pressed")
    ).toBe("true")
    expect(screen.getByRole("status").textContent).toContain("18:00 a 20:00")
    expect(screen.getByRole("button", { name: "19:30" }).className).toContain(
      "bg-primary/10"
    )
    expect(
      screen.getByRole("button", { name: "20:00" }).className
    ).not.toContain("bg-primary/10")
  })

  it("deshabilita el + cuando no quedan horas libres y lo explica", async () => {
    const user = userEvent.setup()
    render(<Picker />)
    await user.click(screen.getByRole("button", { name: "19:00" }))
    const plus = screen.getByRole("button", { name: "Sumar una hora" })
    await user.click(plus)
    expect((plus as HTMLButtonElement).disabled).toBe(true)
    expect(
      screen.getByText(/No hay más horas libres a continuación de las 21:00/)
    ).toBeTruthy()
    const minus = screen.getByRole("button", { name: "Restar una hora" })
    await user.click(minus)
    expect((minus as HTMLButtonElement).disabled).toBe(true)
  })

  it("al elegir un inicio donde no entra la duración la ajusta a la máxima y avisa", async () => {
    const user = userEvent.setup()
    render(<Picker initial={{ start: "18:00", hours: 3 }} />)
    await user.click(screen.getByRole("button", { name: "19:30" }))
    expect(screen.getByRole("alert").textContent).toContain(
      "Desde las 19:30 solo queda 1 hora libre"
    )
    expect(screen.getByRole("status").textContent).toContain("19:30 a 20:30")
    await user.click(screen.getByRole("button", { name: "18:00" }))
    expect(screen.queryByRole("alert")).toBeNull()
  })

  it("la duración máxima depende de las horas libres y no de una lista fija", async () => {
    const user = userEvent.setup()
    render(<Picker slots={[{ desde: "08:00:00", hasta: "14:00:00" }]} />)
    await user.click(screen.getByRole("button", { name: "08:00" }))
    const plus = screen.getByRole("button", { name: "Sumar una hora" })
    for (let i = 0; i < 5; i++) await user.click(plus)
    expect(screen.getByRole("status").textContent).toContain("08:00 a 14:00")
    expect((plus as HTMLButtonElement).disabled).toBe(true)
  })

  it("sin horarios libres lo informa", () => {
    render(<Picker slots={[]} />)
    expect(screen.getByText("Sin horarios disponibles")).toBeTruthy()
  })
})

describe("Elegir el horario una sola vez", () => {
  it("la consulta pasa inicio y duración y el formulario llega con el horario elegido", async () => {
    mockBookingApis()
    vi.spyOn(calendarApi, "get").mockResolvedValue(
      calendar("2099-10-01", [{ desde: "10:00:00", hasta: "13:00:00" }])
    )
    const create = vi
      .spyOn(reservationsApi, "create")
      .mockResolvedValue({ id: "r1" } as Reservation)
    const user = mount(
      <Routes>
        <Route path="/app/services/:id" element={<MemberServiceDetailPage />} />
        <Route path="/app/reservations/new" element={<BookingPage />} />
        <Route path="/app/reservations/:id" element={<p>Reserva creada</p>} />
      </Routes>,
      "/app/services/e1"
    )
    const date = await screen.findByLabelText("Fecha de consulta")
    await user.clear(date)
    await user.type(date, "2099-10-01")
    await user.click(await screen.findByRole("button", { name: "10:00" }))
    await user.click(screen.getByRole("button", { name: "Sumar una hora" }))
    const link = screen.getByRole("link", { name: "Reservar este espacio" })
    expect(link.getAttribute("href")).toContain("hours=2")
    await user.click(link)

    expect(await screen.findByText(/10:00 a 12:00 · 2 horas/)).toBeTruthy()
    expect(screen.queryByRole("button", { name: "10:00" })).toBeNull()
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
    expect(await screen.findByText("Reserva creada")).toBeTruthy()
    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        fecha: "2099-10-01",
        desde: "10:00",
        hasta: "12:00",
      })
    )
  })

  it("al cambiar la fecha conserva el horario si sigue libre y si no lo limpia y avisa", async () => {
    mockBookingApis()
    vi.spyOn(calendarApi, "get").mockImplementation(async (_id, fecha) =>
      calendar(
        fecha,
        fecha === "2099-10-03"
          ? [{ desde: "08:00:00", hasta: "10:00:00" }]
          : [{ desde: "08:00:00", hasta: "22:00:00" }]
      )
    )
    const user = mount(
      <Routes>
        <Route path="/app/reservations/new" element={<BookingPage />} />
      </Routes>,
      "/app/reservations/new?space=e1&date=2099-10-01&start=18:00&hours=2"
    )
    await user.click(
      await screen.findByRole("button", { name: "Cambiar horario" })
    )
    const date = screen.getByLabelText("Fecha")
    await user.clear(date)
    await user.type(date, "2099-10-02")
    expect(
      await screen.findByText(/sigue libre el/, {}, { timeout: 5000 })
    ).toBeTruthy()
    expect(
      screen.getByRole("button", { name: "18:00" }).getAttribute("aria-pressed")
    ).toBe("true")

    await user.clear(date)
    await user.type(date, "2099-10-03")
    expect(
      await screen.findByText(/El horario de 18:00 a 20:00 no está libre/)
    ).toBeTruthy()
    expect(screen.getByText("Elegí un horario de inicio.")).toBeTruthy()
  })

  it("si el horario recibido ya no está libre muestra el selector", async () => {
    mockBookingApis()
    vi.spyOn(calendarApi, "get").mockResolvedValue(
      calendar("2099-10-01", [{ desde: "08:00:00", hasta: "10:00:00" }])
    )
    mount(
      <Routes>
        <Route path="/app/reservations/new" element={<BookingPage />} />
      </Routes>,
      "/app/reservations/new?space=e1&date=2099-10-01&start=18:00&hours=9"
    )
    expect(await screen.findByRole("button", { name: "08:00" })).toBeTruthy()
    expect(screen.queryByRole("button", { name: "Cambiar horario" })).toBeNull()
  })
})
