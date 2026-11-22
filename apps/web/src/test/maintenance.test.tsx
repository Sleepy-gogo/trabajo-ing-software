import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import { LoginPage, RegisterPage } from "@/pages/auth"
import { MembersPage } from "@/pages/admin/members"
import { ReportsPage } from "@/pages/admin/reports"
import { AdminDashboardPage } from "@/pages/admin/indicators"
import { membersApi } from "@/lib/members-api"
import { reportsApi, type Report } from "@/lib/reports-api"
import { spacesApi } from "@/lib/spaces-api"
import { usersApi, type User } from "@/lib/users-api"
import {
  indicatorsApi,
  periodRange,
  type Indicators,
} from "@/lib/indicators-api"
import { groupBy, summarize } from "@/lib/report-summary"

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
function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}
const user: User = {
  id: "u1",
  nombreCompleto: "Persona Ficticia",
  email: "ficticia@example.com",
  dni: 12345678,
  rol: "USUARIO",
  estadoCuenta: "ACTIVO",
  qrUsuario: "SERA-U0123456789ab",
  creadoEn: "",
  actualizadoEn: "",
}

async function fillRegistration(actor: ReturnType<typeof userEvent.setup>) {
  await actor.type(screen.getByLabelText("Nombre", { exact: true }), "Ada")
  await actor.type(screen.getByLabelText("Apellido"), "Prueba")
  await actor.type(screen.getByLabelText("DNI"), "12345678")
  await actor.type(screen.getByLabelText("Email"), "ada@example.com")
  await actor.type(
    screen.getByLabelText("Contraseña", { exact: true }),
    "password-seguro"
  )
  await actor.type(
    screen.getByLabelText("Confirmar contraseña"),
    "password-seguro"
  )
  await actor.click(screen.getByRole("checkbox"))
}

describe("SM-02: ingreso con la relación verificada", () => {
  it("el registro avisa según la relación elegida", async () => {
    const register = vi.spyOn(usersApi, "register").mockResolvedValue(user)
    const actor = mount(<RegisterPage />, "/register")
    expect(screen.getByRole("note").textContent).toContain(
      "vas a poder usar tu cuenta apenas la crees"
    )
    await actor.selectOptions(
      screen.getByLabelText("Relación con la UNSE"),
      "ESTUDIANTE"
    )
    expect(screen.getByRole("note").textContent).toContain(
      "Hasta que la confirme no vas a poder iniciar sesión"
    )
    await fillRegistration(actor)
    await actor.click(screen.getByRole("button", { name: "Crear cuenta" }))
    expect(
      await screen.findByText("Tu cuenta espera la verificación")
    ).toBeTruthy()
    expect(screen.getByText(/como estudiante/)).toBeTruthy()
    expect(register.mock.calls[0][0].relacionUnse).toBe("ESTUDIANTE")
  }, 15000)

  it("el inicio de sesión muestra el motivo que informa el servidor", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string) =>
        path.endsWith("csrf")
          ? json({ token: "t", headerName: "X-CSRF-TOKEN" })
          : json(
              {
                codigo: "relacion_no_verificada",
                mensaje:
                  "Tu relación con la UNSE todavía no fue verificada. Vas a poder ingresar cuando administración la confirme.",
                campos: { verificacion: "PENDIENTE" },
              },
              403
            )
      )
    )
    const actor = mount(<LoginPage />, "/login")
    await actor.type(screen.getByLabelText("Email"), "ada@example.com")
    await actor.type(
      screen.getByLabelText("Contraseña", { exact: true }),
      "password-seguro"
    )
    await actor.click(screen.getByRole("button", { name: "Iniciar sesión" }))
    const alert = await screen.findByRole("alert")
    expect(alert.textContent).toBe(
      "Tu relación con la UNSE todavía no fue verificada. Vas a poder ingresar cuando administración la confirme."
    )
    expect(alert.className).toContain("amber")
  })

  it("el listado de socios toma el filtro de verificación de la dirección", async () => {
    const list = vi.spyOn(membersApi, "list").mockResolvedValue([])
    vi.spyOn(membersApi, "levels").mockResolvedValue([])
    mount(
      <Routes>
        <Route path="/admin/members" element={<MembersPage />} />
      </Routes>,
      "/admin/members?verificacion=PENDIENTE"
    )
    await waitFor(() =>
      expect(list).toHaveBeenCalledWith(
        "",
        "",
        "",
        expect.anything(),
        "PENDIENTE"
      )
    )
    expect(
      (
        screen.getByLabelText(
          "Verificación de la relación"
        ) as HTMLSelectElement
      ).value
    ).toBe("PENDIENTE")
  })
})

const indicators: Indicators = {
  desde: "2026-10-19",
  hasta: "2026-11-17",
  anteriorDesde: "2026-09-19",
  anteriorHasta: "2026-10-18",
  indicadores: [
    {
      clave: "ingresos",
      nombre: "Ingresos aprobados",
      unidad: "ARS",
      valor: 118000,
      anterior: 0,
      variacion: null,
    },
    {
      clave: "cancelacion",
      nombre: "Tasa de cancelación",
      unidad: "porcentaje",
      valor: 10,
      anterior: 25,
      variacion: -15,
    },
  ],
  ingresosPorMes: [{ mes: "2026-11", cuotas: 1000, reservas: 2000 }],
  espacios: [
    { espacio: "Cancha ficticia", reservas: 3, horas: 5, ingresos: 9000 },
  ],
  demanda: [
    { dia: 4, hora: 18, reservas: 1 },
    { dia: 4, hora: 19, reservas: 1 },
  ],
  asistencia: { terminadas: 10, conIngreso: 7, porcentaje: 70 },
  sociosPorRelacion: [{ relacion: "ESTUDIANTE", cantidad: 4, pendientes: 2 }],
  membresiasPorNivel: [{ nivel: "General", cantidad: 3 }],
  conclusiones: ["La franja con más demanda es jueves de 18 a 19."],
  ultimasAcciones: [
    {
      tipo: "INICIO_SESION_FALLIDO",
      usuario: "nadie@example.com",
      detalle: "Contraseña incorrecta",
      creadoEn: "2026-11-17T13:00:00Z",
    },
  ],
}

describe("SM-04: panel de indicadores", () => {
  it("calcula los períodos del selector hasta hoy", () => {
    expect(periodRange("month", "2026-11-17")).toEqual({
      desde: "2026-11-01",
      hasta: "2026-11-17",
    })
    expect(periodRange("30d", "2026-11-17").desde).toBe("2026-10-19")
    expect(periodRange("3m", "2026-11-17").desde).toBe("2026-08-18")
    expect(periodRange("12m", "2026-11-17").desde).toBe("2025-11-18")
  })

  it("muestra comparaciones, conclusiones, demanda y últimas acciones", async () => {
    const get = vi.spyOn(indicatorsApi, "get").mockResolvedValue(indicators)
    const actor = mount(<AdminDashboardPage />, "/admin")
    expect(await screen.findByText("Sin base de comparación")).toBeTruthy()
    expect(screen.getByText("−15 puntos")).toBeTruthy()
    expect(
      screen.getByText("La franja con más demanda es jueves de 18 a 19.")
    ).toBeTruthy()
    expect(screen.getByTitle("jueves de 19 a 20: 1 reserva")).toBeTruthy()
    expect(screen.getByText("Inicio de sesión fallido")).toBeTruthy()
    expect(
      screen
        .getByRole("link", { name: /Relaciones por verificar/ })
        .getAttribute("href")
    ).toBe("/admin/members?verificacion=PENDIENTE")
    await actor.click(screen.getByRole("button", { name: "Últimos 12 meses" }))
    await waitFor(() =>
      expect(get).toHaveBeenLastCalledWith(
        expect.any(String),
        expect.any(String),
        expect.anything()
      )
    )
    expect(get).toHaveBeenCalledTimes(2)
  })
})

function report(tipo: Report["tipo"], filas: Report["filas"]): Report {
  return {
    id: "i1",
    tipo,
    filtros: {
      tipo,
      desde: "2026-11-01",
      hasta: "2026-11-17",
      estado: "",
      relacion: "",
      espacioId: null,
    },
    creadoEn: "2026-11-17T12:00:00Z",
    creadoPor: "Admin ficticio",
    espacioNombre: null,
    columnas: [{ key: "estado", label: "Estado" }],
    filas,
    resumen: { registros: filas.length },
  }
}

describe("SM-04 y SM-05: informes con gráficos", () => {
  it("agrupa y ordena las filas de mayor a menor", () => {
    expect(
      groupBy(
        [{ estado: "ACTIVA" }, { estado: "VENCIDA" }, { estado: "ACTIVA" }],
        "estado"
      )
    ).toEqual([
      { name: "Activa", value: 2 },
      { name: "Vencida", value: 1 },
    ])
  })

  it("resume cada tipo de informe con sus filas guardadas", () => {
    const pagos = summarize(
      report("pagos", [
        {
          estado: "APROBADO",
          concepto: "CUOTA_MENSUAL",
          medio: "EFECTIVO",
          monto: 1000,
          revision: false,
        },
        {
          estado: "APROBADO",
          concepto: "RESERVA",
          medio: "MERCADO_PAGO",
          monto: 500,
          revision: true,
        },
        {
          estado: "PENDIENTE",
          concepto: "RESERVA",
          medio: "EFECTIVO",
          monto: 9000,
          revision: false,
        },
      ])
    )
    expect(pagos.stats.map((s) => s.value)).toEqual([
      "3",
      expect.stringContaining("1.500"),
      "1",
    ])
    expect(pagos.charts[0].data).toEqual([
      { name: "Cuota mensual", value: 1000 },
      { name: "Reserva", value: 500 },
    ])
    const reservas = summarize(
      report("reservas", [
        { estado: "CONFIRMADA", espacio: "Pileta", total: 2000 },
        { estado: "CANCELADA", espacio: "Pileta", total: 7000 },
      ])
    )
    expect(reservas.stats[1].value).toContain("2.000")
    expect(reservas.charts[1].data).toEqual([{ name: "Pileta", value: 2 }])
    const uso = summarize(
      report("uso_servicios", [
        {
          espacio: "Pileta",
          reservas: 5,
          terminadas: 4,
          ingresos: 3,
          horas: 6,
        },
        {
          espacio: "Quincho",
          reservas: 0,
          terminadas: 0,
          ingresos: 0,
          horas: 0,
        },
      ])
    )
    expect(uso.stats[1].value).toBe("75 %")
    expect(uso.charts[1].data).toEqual([{ name: "Pileta", value: 75 }])
    const socios = summarize(
      report("socios", [{ estado: "ACTIVA", relacion: "EXTERNO" }])
    )
    expect(socios.charts[1].data).toEqual([{ name: "Externo", value: 1 }])
    const actividad = summarize(
      report("actividad", [
        { tipo: "INICIO_SESION_FALLIDO", accion: "Inicio de sesión fallido" },
        { tipo: "INICIO_SESION", accion: "Inicio de sesión" },
      ])
    )
    expect(actividad.stats[1].value).toBe("1")
  })

  it("un informe guardado muestra el resumen calculado con sus filas originales", async () => {
    vi.spyOn(reportsApi, "get").mockResolvedValue(
      report("pagos", [
        {
          estado: "APROBADO",
          concepto: "RESERVA",
          medio: "EFECTIVO",
          monto: 1250,
          revision: false,
        },
      ])
    )
    mount(
      <Routes>
        <Route path="/admin/reports" element={<ReportsPage />} />
      </Routes>,
      "/admin/reports?id=i1&type=pagos"
    )
    expect(
      await screen.findByText("Importe aprobado por concepto")
    ).toBeTruthy()
    expect(screen.getAllByText(/1\.250/).length).toBeGreaterThan(0)
  })

  it("el informe de actividad filtra por tipo de acción y no por relación ni espacio", async () => {
    vi.spyOn(spacesApi, "list").mockResolvedValue([])
    const generate = vi
      .spyOn(reportsApi, "generate")
      .mockResolvedValue(report("actividad", []))
    const actor = mount(
      <Routes>
        <Route path="/admin/reports" element={<ReportsPage />} />
      </Routes>,
      "/admin/reports?type=actividad"
    )
    expect(screen.queryByLabelText("Relación con UNSE")).toBeNull()
    expect(screen.queryByLabelText("Espacio")).toBeNull()
    fireEvent.change(screen.getByLabelText("Desde"), {
      target: { value: "2026-11-01" },
    })
    await actor.selectOptions(
      screen.getByLabelText("Tipo de acción"),
      "Inicio de sesión fallido"
    )
    await actor.click(screen.getByRole("button", { name: "Generar informe" }))
    await waitFor(() =>
      expect(generate).toHaveBeenCalledWith(
        expect.objectContaining({
          tipo: "actividad",
          estado: "INICIO_SESION_FALLIDO",
          relacion: "",
          espacioId: null,
        }),
        expect.anything()
      )
    )
  })
})
