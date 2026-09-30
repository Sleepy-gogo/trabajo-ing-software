import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import { ReportsPage, RecentReportsPage } from "@/pages/admin/reports"
import { AdminSurveysPage } from "@/pages/admin/surveys"
import { MemberSurveysPage } from "@/pages/member/surveys"
import { reportsApi, type Report } from "@/lib/reports-api"
import { surveysApi, type Assignment } from "@/lib/surveys-api"
import { spacesApi } from "@/lib/spaces-api"

const report: Report = {
  id: "i1",
  tipo: "pagos",
  filtros: {
    tipo: "pagos",
    desde: "2026-09-01",
    hasta: "2026-09-29",
    estado: "APROBADO",
    relacion: "",
    espacioId: null,
  },
  creadoEn: "2026-09-29T12:00:00Z",
  creadoPor: "Admin real",
  espacioNombre: null,
  columnas: [
    { key: "titular", label: "Titular" },
    { key: "monto", label: "Monto ARS" },
  ],
  filas: [{ titular: "Socio real", monto: 1250 }],
  resumen: { registros: 1, importe_aprobado: 1250 },
}
const assignment: Assignment = {
  encuesta: {
    id: "e1",
    titulo: "Experiencia real",
    descripcion: "Contanos cómo fue",
    espacioId: "s1",
    desde: "2026-09-01",
    hasta: "2026-10-31",
    activa: true,
    creadaEn: "2026-09-29T12:00:00Z",
    preguntas: [
      {
        id: "q1",
        texto: "Calificación",
        tipo: "CALIFICACION",
        obligatoria: true,
        opciones: [],
      },
      {
        id: "q2",
        texto: "Comentario",
        tipo: "TEXTO",
        obligatoria: false,
        opciones: [],
      },
    ],
  },
  reservaId: "r1",
  espacio: "Cancha real",
  fecha: "2026-09-29",
  estado: "DISPONIBLE",
  respuestas: {},
  enviadaEn: null,
}
function mount(content: React.ReactNode, route: string, path: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={path} element={content} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
  return userEvent.setup()
}

describe("Reportes y encuestas conectados", () => {
  it("envía filtros al backend y exporta la instantánea generada", async () => {
    vi.spyOn(spacesApi, "list").mockResolvedValue([])
    const generate = vi.spyOn(reportsApi, "generate").mockResolvedValue(report)
    const actor = mount(
      <ReportsPage />,
      "/admin/reports?type=pagos",
      "/admin/reports"
    )
    fireEvent.change(screen.getByLabelText("Desde"), {
      target: { value: "2026-09-01" },
    })
    await actor.selectOptions(screen.getByLabelText("Estado"), "APROBADO")
    await actor.click(screen.getByRole("button", { name: "Generar informe" }))
    await screen.findByText("Socio real")
    expect(generate).toHaveBeenCalledWith(
      expect.objectContaining({
        tipo: "pagos",
        estado: "APROBADO",
        espacioId: null,
      }),
      expect.anything()
    )
    expect(
      screen.getByRole("link", { name: "Exportar CSV" }).getAttribute("href")
    ).toBe("/api/reportes/i1/csv")
    expect(screen.getByText(/Admin real/)).toBeTruthy()
  })
  it("muestra un error de consulta y permite reintentar", async () => {
    vi.spyOn(reportsApi, "list")
      .mockRejectedValueOnce(new Error("API no disponible"))
      .mockResolvedValueOnce([report])
    const actor = mount(
      <RecentReportsPage />,
      "/admin/reports/recent",
      "/admin/reports/recent"
    )
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      "API no disponible"
    )
    await actor.click(screen.getByRole("button", { name: "Reintentar" }))
    expect(await screen.findByText("Pagos")).toBeTruthy()
  })
  it("persiste respuestas por reserva y muestra la confirmación del servidor", async () => {
    vi.spyOn(surveysApi, "get").mockResolvedValue(assignment)
    const respond = vi.spyOn(surveysApi, "respond").mockResolvedValue({
      ...assignment,
      estado: "RESPONDIDA",
      respuestas: { q1: "4", q2: "Me gustó" },
      enviadaEn: "2026-09-29T12:30:00Z",
    })
    const actor = mount(
      <MemberSurveysPage />,
      "/app/surveys/e1?reserva=r1",
      "/app/surveys/:id"
    )
    await actor.click(await screen.findByRole("radio", { name: "4 de 5" }))
    await actor.type(screen.getByLabelText("Comentario"), "Me gustó")
    await actor.click(screen.getByRole("button", { name: "Enviar encuesta" }))
    await screen.findByText(/Tu respuesta quedó registrada/)
    expect(respond).toHaveBeenCalledWith("e1", "r1", {
      q1: "4",
      q2: "Me gustó",
    })
    expect(screen.queryByRole("button", { name: "Enviar encuesta" })).toBeNull()
    expect(
      (screen.getByRole("radio", { name: "4 de 5" }) as HTMLInputElement)
        .disabled ||
        screen.getByRole("radio", { name: "4 de 5" }).closest("fieldset")
          ?.disabled
    ).toBe(true)
  })
  it("un estado de éxito en la URL no simula el envío", async () => {
    vi.spyOn(surveysApi, "get").mockResolvedValue(assignment)
    mount(
      <MemberSurveysPage />,
      "/app/surveys/e1?reserva=r1&state=submitted",
      "/app/surveys/:id"
    )
    expect(
      await screen.findByRole("button", { name: "Enviar encuesta" })
    ).toBeTruthy()
    expect(screen.queryByText(/Tu respuesta quedó registrada/)).toBeNull()
  })
  it("consulta respuestas previas y muestra el estado vacío", async () => {
    vi.spyOn(surveysApi, "mine").mockResolvedValue([])
    mount(<MemberSurveysPage />, "/app/surveys", "/app/surveys")
    expect(await screen.findByText("No tenés encuestas")).toBeTruthy()
  })
  it("publica una definición de encuesta desde administración", async () => {
    vi.spyOn(spacesApi, "list").mockResolvedValue([])
    vi.spyOn(surveysApi, "list").mockResolvedValue([])
    const create = vi
      .spyOn(surveysApi, "create")
      .mockResolvedValue(assignment.encuesta)
    const actor = mount(
      <AdminSurveysPage />,
      "/admin/surveys",
      "/admin/surveys"
    )
    await actor.click(screen.getByRole("button", { name: "Nueva encuesta" }))
    await actor.type(screen.getByLabelText("Título"), "Prueba publicada")
    await actor.type(screen.getByLabelText("Texto de pregunta 1"), "¿Cómo fue?")
    await actor.click(screen.getByRole("button", { name: "Publicar encuesta" }))
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith(
        expect.objectContaining({
          titulo: "Prueba publicada",
          preguntas: [
            {
              texto: "¿Cómo fue?",
              tipo: "CALIFICACION",
              obligatoria: true,
              opciones: [],
            },
          ],
        }),
        expect.anything()
      )
    )
    expect(await screen.findByText(/Encuesta creada/)).toBeTruthy()
  })
})
