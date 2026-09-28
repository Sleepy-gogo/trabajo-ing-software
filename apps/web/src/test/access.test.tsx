import { act, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { AccessPage } from "@/pages/staff/access"
import { accessApi } from "@/lib/access-api"

const scanner = vi.hoisted(() => ({
  start: vi.fn(),
  stop: vi.fn(),
  destroy: vi.fn(),
  scanImage: vi.fn(),
  decode: null as null | ((result: { data: string }) => void),
}))
vi.mock("qr-scanner", () => ({
  default: class {
    static scanImage = scanner.scanImage
    start = scanner.start
    stop = scanner.stop
    destroy = scanner.destroy
    constructor(_video: unknown, decode: typeof scanner.decode) {
      scanner.decode = decode
    }
  },
}))
const denied = {
  autorizado: false,
  motivo: "La reserva fue cancelada.",
  tipo: "RESERVA" as const,
  titular: "Ana",
  espacio: "Cancha",
  fecha: "2026-09-29",
  desde: "10:00",
  hasta: "11:00",
}
function mount() {
  const view = render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { mutations: { retry: false } } })
      }
    >
      <AccessPage />
    </QueryClientProvider>
  )
  return { ...view, user: userEvent.setup() }
}
beforeEach(() => {
  scanner.start.mockReset().mockResolvedValue(undefined)
  scanner.stop.mockReset()
  scanner.destroy.mockReset()
  scanner.scanImage.mockReset()
  vi.stubGlobal("isSecureContext", true)
  vi.stubGlobal("navigator", {
    ...navigator,
    mediaDevices: { getUserMedia: vi.fn() },
  })
})
describe("Validación de acceso", () => {
  it("usa la respuesta del servidor y borra el resultado al cambiar el código", async () => {
    const validate = vi.spyOn(accessApi, "validate").mockResolvedValue(denied)
    const { user } = mount()
    await user.type(
      screen.getByLabelText("Código de reserva o carnet"),
      "SERA-cancelada"
    )
    await user.click(screen.getByRole("button", { name: "Validar código" }))
    expect(await screen.findByText("Acceso no habilitado")).toBeTruthy()
    expect(validate).toHaveBeenCalledWith("SERA-cancelada")
    await user.type(screen.getByLabelText("Código de reserva o carnet"), "x")
    expect(screen.queryByText("Acceso no habilitado")).toBeNull()
  })
  it("lee una vez y libera la cámara al detectar el QR", async () => {
    const validate = vi.spyOn(accessApi, "validate").mockResolvedValue({
      ...denied,
      autorizado: true,
      motivo: "Reserva vigente.",
    })
    const { user } = mount()
    await user.click(screen.getByRole("button", { name: "Activar cámara" }))
    await act(async () => {
      scanner.decode?.({ data: "SERA-real" })
      scanner.decode?.({ data: "SERA-real" })
    })
    expect(await screen.findByText("Acceso habilitado")).toBeTruthy()
    expect(validate).toHaveBeenCalledTimes(1)
    expect(scanner.stop).toHaveBeenCalled()
    expect(scanner.destroy).toHaveBeenCalled()
  })
  it("libera cámara al salir y muestra errores de permiso", async () => {
    scanner.start.mockRejectedValue(new Error("denied"))
    const { user, unmount } = mount()
    await user.click(screen.getByRole("button", { name: "Activar cámara" }))
    expect(await screen.findByRole("alert")).toBeTruthy()
    unmount()
    expect(scanner.destroy).toHaveBeenCalled()
  })
  it("decodifica imagen y verifica su contenido en la API", async () => {
    scanner.scanImage.mockResolvedValue({ data: "SERA-imagen" })
    const validate = vi.spyOn(accessApi, "validate").mockResolvedValue(denied)
    const { user } = mount()
    await user.upload(
      screen.getByLabelText("Imagen del QR"),
      new File(["qr"], "qr.png", { type: "image/png" })
    )
    await waitFor(() => expect(validate).toHaveBeenCalledWith("SERA-imagen"))
  })
  it("en HTTP ofrece alternativas y no solicita cámara", () => {
    vi.stubGlobal("isSecureContext", false)
    mount()
    expect(
      (
        screen.getByRole("button", {
          name: "Activar cámara",
        }) as HTMLButtonElement
      ).disabled
    ).toBe(true)
    expect(screen.getByLabelText("Imagen del QR")).toBeTruthy()
    expect(scanner.start).not.toHaveBeenCalled()
  })
  it("solo consume al confirmar el ingreso y no vuelve a ofrecer confirmación", async () => {
    vi.spyOn(accessApi, "validate").mockResolvedValue({
      ...denied,
      autorizado: true,
      motivo: "Reserva vigente.",
    })
    const confirm = vi.spyOn(accessApi, "confirm").mockResolvedValue({
      ...denied,
      consumidaEn: "2026-09-29T10:01:00-03:00",
      motivo: "El ingreso ya está registrado.",
    })
    const { user } = mount()
    await user.type(
      screen.getByLabelText("Código de reserva o carnet"),
      "SERA-reserva"
    )
    await user.click(screen.getByRole("button", { name: "Validar código" }))
    const action = await screen.findByRole("button", {
      name: "Confirmar ingreso y consumir reserva",
    })
    expect(confirm).not.toHaveBeenCalled()
    await user.click(action)
    expect(await screen.findByText("Reserva consumida")).toBeTruthy()
    expect(confirm).toHaveBeenCalledWith("SERA-reserva")
    expect(
      screen.queryByRole("button", {
        name: "Confirmar ingreso y consumir reserva",
      })
    ).toBeNull()
  })
})
