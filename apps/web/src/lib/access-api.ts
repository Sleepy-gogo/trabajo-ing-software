import { api } from "./users-api"
export type AccessResult = {
  consumidaEn?: string | null
  autorizado: boolean
  motivo: string
  tipo: "RESERVA" | "PERSONAL" | "DESCONOCIDO"
  titular: string | null
  espacio: string | null
  fecha: string | null
  desde: string | null
  hasta: string | null
}
export const accessApi = {
  confirm: (codigo: string) =>
    api<AccessResult>("/accesos/ingresos", {
      method: "POST",
      body: JSON.stringify({ codigo }),
    }),
  validate: (codigo: string) =>
    api<AccessResult>("/accesos/validacion", {
      method: "POST",
      body: JSON.stringify({ codigo }),
    }),
}
