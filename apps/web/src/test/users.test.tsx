import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, expect, it, vi } from "vitest"
import { LoginPage, RegisterPage } from "@/pages/auth"
import { MemberProfilePage } from "@/pages/member/profile"
import { UsersPage } from "@/pages/admin/users"
import { RequireSession } from "@/components/layout/require-session"
import { usersApi, type User } from "@/lib/users-api"

const user: User = {
  id: "test-id",
  nombreCompleto: "Ada Lovelace",
  email: "ada@example.com",
  dni: 12345678,
  rol: "USUARIO",
  estadoCuenta: "ACTIVO",
  qrUsuario: "SERA-U0123456789ab",
  creadoEn: "2026-09-13T00:00:00Z",
  actualizadoEn: "2026-09-13T00:00:00Z",
}
function mount(content: React.ReactNode, path = "/login") {
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
function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

describe("Incremento 1", () => {
  it("envía login JSON con CSRF y navega al perfil real", async () => {
    const request = vi.fn(async (path: string, options?: RequestInit) => {
      if (path === "/api/auth/csrf")
        return json({ token: "csrf-test", headerName: "X-CSRF-TOKEN" })
      expect(path).toBe("/api/auth/login")
      expect(new Headers(options?.headers).get("X-CSRF-TOKEN")).toBe(
        "csrf-test"
      )
      expect(JSON.parse(String(options?.body))).toEqual({
        email: "ada@example.com",
        password: "password-seguro",
      })
      return json(user)
    })
    vi.stubGlobal("fetch", request)
    const actor = mount(
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/app/profile" element={<p>Perfil conectado</p>} />
      </Routes>
    )
    await actor.type(screen.getByLabelText("Email"), "ada@example.com")
    await actor.type(
      screen.getByLabelText("Contraseña", { exact: true }),
      "password-seguro"
    )
    await actor.click(screen.getByRole("button", { name: "Iniciar sesión" }))
    expect(await screen.findByText("Perfil conectado")).toBeTruthy()
    expect(request).toHaveBeenCalledTimes(2)
  })

  it("mantiene el formulario y muestra el error de credenciales", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string) =>
        path.endsWith("csrf")
          ? json({ token: "test", headerName: "X-CSRF-TOKEN" })
          : json({ mensaje: "Credenciales incorrectas." }, 401)
      )
    )
    const actor = mount(<LoginPage />)
    await actor.type(screen.getByLabelText("Email"), "ada@example.com")
    await actor.type(
      screen.getByLabelText("Contraseña", { exact: true }),
      "incorrecta"
    )
    await actor.click(screen.getByRole("button", { name: "Iniciar sesión" }))
    expect((await screen.findByRole("alert")).textContent).toContain(
      "Credenciales incorrectas."
    )
    expect((screen.getByLabelText("Email") as HTMLInputElement).value).toBe(
      "ada@example.com"
    )
  })

  it("registra solo los campos del contrato y no permite elegir rol", async () => {
    const register = vi.spyOn(usersApi, "register").mockResolvedValue(user)
    const actor = mount(<RegisterPage />, "/register")
    await actor.type(screen.getByLabelText("Nombre", { exact: true }), "Ada")
    await actor.type(screen.getByLabelText("Apellido"), "Lovelace")
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
    await actor.click(screen.getByRole("button", { name: "Crear cuenta" }))
    expect(await screen.findByText("Tu cuenta está lista")).toBeTruthy()
    expect(register.mock.calls[0][0]).toEqual({
      relacionUnse: "EXTERNO",
      identificadorUnse: "",
      nombreCompleto: "Ada Lovelace",
      email: "ada@example.com",
      dni: 12345678,
      password: "password-seguro",
    })
  }, 15000)

  it("guarda el perfil sin enviar rol ni estado", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    const profile = vi
      .spyOn(usersApi, "profile")
      .mockResolvedValue({ ...user, nombreCompleto: "Ada editada" })
    const actor = mount(<MemberProfilePage />, "/app/profile")
    const name = await screen.findByLabelText("Nombre completo")
    await actor.clear(name)
    await actor.type(name, "Ada editada")
    await actor.click(screen.getByRole("button", { name: "Guardar cambios" }))
    expect(await screen.findByText("Tus datos se guardaron.")).toBeTruthy()
    expect(profile.mock.calls[0][0]).toEqual({
      nombreCompleto: "Ada editada",
      email: user.email,
      dni: user.dni,
    })
  })

  it("redirige a usuarios sin rol administrativo", async () => {
    vi.spyOn(usersApi, "me").mockResolvedValue(user)
    mount(
      <Routes>
        <Route element={<RequireSession roles={["ADMIN"]} />}>
          <Route path="/admin/users" element={<p>Administración privada</p>} />
        </Route>
        <Route path="/app/profile" element={<p>Perfil permitido</p>} />
      </Routes>,
      "/admin/users"
    )
    expect(await screen.findByText("Perfil permitido")).toBeTruthy()
    expect(screen.queryByText("Administración privada")).toBeNull()
  })

  it("consulta usuarios reales y aplica búsqueda al endpoint", async () => {
    const list = vi.spyOn(usersApi, "list").mockResolvedValue([user])
    const actor = mount(<UsersPage />, "/admin/users")
    expect(await screen.findByText(user.email)).toBeTruthy()
    await actor.type(
      screen.getByRole("textbox", { name: "Buscar por nombre, email o DNI" }),
      "Ada"
    )
    await waitFor(() =>
      expect(list.mock.calls.some((call) => call[0] === "Ada")).toBe(true)
    )
  })
})
