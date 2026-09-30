import { api } from "./users-api"
export type QuestionType = "CALIFICACION" | "OPCION" | "TEXTO"
export type QuestionInput = {
  texto: string
  tipo: QuestionType
  obligatoria: boolean
  opciones: string[]
}
export type SurveyInput = {
  titulo: string
  descripcion: string
  espacioId: string | null
  desde: string
  hasta: string
  preguntas: QuestionInput[]
}
export type Survey = Omit<SurveyInput, "preguntas"> & {
  id: string
  activa: boolean
  creadaEn: string
  preguntas: (QuestionInput & { id: string })[]
}
export type Assignment = {
  encuesta: Survey
  reservaId: string
  espacio: string
  fecha: string
  estado: "DISPONIBLE" | "RESPONDIDA" | "CERRADA"
  respuestas: Record<string, string>
  enviadaEn: string | null
}
export type SurveyResults = {
  encuesta: Survey
  total: number
  estadisticas: {
    preguntaId: string
    texto: string
    respuestas: number
    promedio: number | null
    distribucion: Record<string, number>
  }[]
  envios: {
    envioId: string
    reservaId: string
    titular: string
    espacio: string
    fecha: string
    enviadaEn: string
    respuestas: Record<string, string>
  }[]
}
export const surveysApi = {
  list: (signal?: AbortSignal) => api<Survey[]>("/encuestas", { signal }),
  create: (data: SurveyInput) =>
    api<Survey>("/encuestas", { method: "POST", body: JSON.stringify(data) }),
  active: (id: string, activa: boolean) =>
    api<Survey>(`/encuestas/${id}/estado`, {
      method: "PUT",
      body: JSON.stringify({ activa }),
    }),
  results: (id: string, signal?: AbortSignal) =>
    api<SurveyResults>(`/encuestas/${id}/resultados`, { signal }),
  mine: (signal?: AbortSignal) =>
    api<Assignment[]>("/encuestas/me", { signal }),
  get: (id: string, reservation: string, signal?: AbortSignal) =>
    api<Assignment>(`/encuestas/${id}/reservas/${reservation}`, { signal }),
  respond: (
    id: string,
    reservation: string,
    respuestas: Record<string, string>
  ) =>
    api<Assignment>(`/encuestas/${id}/reservas/${reservation}/respuestas`, {
      method: "POST",
      body: JSON.stringify({ respuestas }),
    }),
}
