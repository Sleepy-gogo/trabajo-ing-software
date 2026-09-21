import { useMutation, useQueryClient } from "@tanstack/react-query"
import { ApiError, usersApi } from "@/lib/users-api"
import { homeFor } from "@/hooks/use-session"
import { useState, type FormEvent, type ReactNode } from "react"
import { Link, useNavigate } from "react-router-dom"
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  QrCode,
  Users,
} from "lucide-react"
import { SeraBrand } from "@/components/layout/app-shell"
import { FeedbackState } from "@/components/shared"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"

function AuthLayout({
  children,
  wide = false,
}: {
  children: ReactNode
  wide?: boolean
}) {
  return (
    <div className="min-h-svh bg-[#eef2f6] p-0 sm:p-4">
      <a href="#main-content" className="skip-link">
        Saltar al contenido
      </a>
      <div className="mx-auto grid min-h-[calc(100svh-2rem)] max-w-[1600px] overflow-hidden bg-white sm:rounded-2xl lg:grid-cols-[0.95fr_1fr]">
        <aside className="relative hidden min-h-[760px] flex-col justify-between overflow-hidden bg-sidebar p-12 text-white lg:flex">
          {/* COLLAGE VERTICAL: Las 3 imágenes una debajo de otra ocupando el 100% de la altura */}
          <div className="absolute inset-0 grid h-full w-full grid-rows-3">
            <img
              src="/poli1.jpg"
              alt="Cancha Polideportivo"
              className="h-full w-full object-cover object-center"
            />
            <img
              src="/poli2.jpg"
              alt="Pileta Polideportivo"
              className="h-full w-full object-cover object-center"
            />
            <img
              src="/poli3.jpg"
              alt="Instalaciones Polideportivo"
              className="h-full w-full object-cover object-center"
            />
          </div>

          {/* GRADIENTE OSCURO: para fundir las fotos y permitir leer el texto */}
          <div className="absolute inset-0 bg-[#10243a]/75 backdrop-contrast-125" />

          {/* Contenido superior */}
          <div className="relative z-10 flex flex-col items-center text-center">
            {/* NUEVO LOGO SERA (Reemplaza al SeraBrand de shadcn) */}
            <div className="flex flex-col items-center justify-center">
              <span className="text-5xl font-black tracking-tighter text-white">
                SERA<span className="text-blue-500">.</span>
              </span>
              <span className="mt-1.5 text-xs font-semibold tracking-widest text-slate-300 uppercase">
                Polideportivo UNSE
              </span>
            </div>

            {/* Títulos también centrados */}
            <div className="mt-16">
              <p className="text-6xl leading-none font-extrabold tracking-[-0.055em] xl:text-7xl">
                Tu lugar
                <br />
                para moverte.
              </p>
              <p className="mx-auto mt-7 max-w-sm text-lg leading-relaxed text-slate-200">
                Reservá espacios, gestioná tu membresía y disfrutá del deporte
                en la UNSE.
              </p>
            </div>
          </div>

          {/* Contenido inferior */}
          <div className="relative z-10">
            <div className="mb-12 flex justify-center gap-10">
              {[
                { icon: Users, label: "Comunidad UNSE" },
                { icon: CalendarDays, label: "Reservas de espacios" },
                { icon: QrCode, label: "Carnet digital" },
              ].map(({ icon: Icon, label }) => (
                <div
                  key={label}
                  className="flex w-28 flex-col items-center text-center"
                >
                  <div className="mb-3 flex size-14 items-center justify-center rounded-xl border border-white/20 bg-white/5">
                    <Icon aria-hidden="true" className="size-6" />
                  </div>

                  <span className="text-sm leading-relaxed text-slate-200">
                    {label}
                  </span>
                </div>
              ))}
            </div>
            <div className="relative z-10">
              <div className="mb-12 grid grid-cols-3 gap-5">
                {/* ... (el código de Comunidad UNSE, Reservas y Carnet queda igual) ... */}
              </div>

              {/* ACÁ ESTÁ EL CAMBIO: Mantenemos la línea divisoria superior y agregamos tu logo */}
              <div className="flex items-center justify-between border-t border-white/15 pt-6">
                {/* Logo UNSE a la izquierda */}
                <img
                  src="/logo-unse_2.png"
                  alt="Universidad Nacional de Santiago del Estero"
                  className="h-[114px] w-auto object-contain"
                />

                {/* Logo Bienestar a la derecha */}
                <img
                  src="/bienestar-transparente.png"
                  alt="Bienestar Estar Bien"
                  className="h-[114px] w-auto object-contain"
                />
              </div>
            </div>
          </div>
        </aside>
        <div className="flex min-w-0 flex-col">
          <header className="flex items-center justify-between gap-3 px-6 py-6 sm:px-10">
            <div className="lg:hidden">
              <SeraBrand compact />
            </div>
            <span className="ml-auto hidden text-right text-xs leading-relaxed text-muted-foreground sm:block">
              Polideportivo UNSE
              <br />
              Santiago del Estero, Argentina
            </span>
          </header>
          <main
            id="main-content"
            className={`mx-auto flex w-full flex-1 flex-col justify-center px-6 py-10 sm:px-10 ${wide ? "max-w-xl" : "max-w-[500px]"}`}
          >
            {children}
          </main>
          <footer className="px-6 py-6 text-center text-[11px] text-muted-foreground">
            SERA · Sistema de Espacios, Reservas y Accesos
          </footer>
        </div>
      </div>
    </div>
  )
}

function PasswordInput({
  id,
  label = "Contraseña",
  autoComplete = "current-password",
}: {
  id: string
  label?: string
  autoComplete?: string
}) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <LockKeyhole
          className="absolute top-3.5 left-3.5 size-4 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id={id}
          name={id}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          required
          minLength={8}
          maxLength={72}
          placeholder="Tu contraseña"
          className="h-11 pr-12 pl-10"
        />
        <Button
          type="button"
          size="icon"
          variant="ghost"
          className="absolute top-1 right-1 text-muted-foreground"
          aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          onClick={() => setVisible(!visible)}
        >
          {visible ? <EyeOff /> : <Eye />}
        </Button>
      </div>
    </div>
  )
}

export function LoginPage() {
  const navigate = useNavigate()
  const [error, setError] = useState("")
  const client = useQueryClient()
  const login = useMutation({
    mutationFn: usersApi.login,
    onSuccess: (user) => {
      client.clear()
      client.setQueryData(["session"], user)
      navigate(homeFor(user.rol), { replace: true })
    },
    onError: (error) =>
      setError(
        error instanceof ApiError && Object.keys(error.fields).length
          ? Object.values(error.fields).join(" ")
          : error.message
      ),
  })
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (login.isPending) return
    setError("")
    const data = new FormData(event.currentTarget)
    login.mutate({
      email: String(data.get("email")).trim(),
      password: String(data.get("password")),
    })
  }
  return (
    <AuthLayout>
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-[-0.04em]">
          Iniciar sesión
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Accedé a tu cuenta del Polideportivo UNSE.
        </p>
      </div>
      <form onSubmit={submit} className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <div className="relative">
            <Mail
              className="absolute top-3.5 left-3.5 size-4 text-muted-foreground"
              aria-hidden="true"
            />
            <Input
              id="email"
              type="email"
              name="email"
              autoComplete="username"
              placeholder="nombre@unse.edu.ar"
              required
              className="h-11 pl-10"
              aria-describedby={error ? "login-error" : undefined}
            />
          </div>
        </div>
        <PasswordInput id="password" />
        {error && (
          <p
            id="login-error"
            role="alert"
            className="rounded-lg bg-rose-50 p-3 text-xs leading-relaxed text-rose-800"
          >
            {error}
          </p>
        )}
        <Button
          type="submit"
          disabled={login.isPending}
          className="h-11 w-full"
        >
          Iniciar sesión
          <ArrowRight aria-hidden="true" />
        </Button>
      </form>
      <div className="my-7 flex items-center gap-4 text-xs text-muted-foreground">
        <span className="h-px flex-1 bg-border" />
        ¿No tenés cuenta?
        <span className="h-px flex-1 bg-border" />
      </div>
      <Button
        variant="outline"
        className="h-11 w-full"
        render={<Link to="/register" />}
      >
        Crear una cuenta
      </Button>
      <p className="mt-8 text-center text-xs leading-relaxed text-muted-foreground">
        Ingresá con el email y la contraseña de tu cuenta.
      </p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState("")
  const registration = useMutation({
    mutationFn: usersApi.register,
    onSuccess: () => setSuccess(true),
    onError: (error) =>
      setError(
        error instanceof ApiError && Object.keys(error.fields).length
          ? Object.values(error.fields).join(" ")
          : error.message
      ),
  })
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    if (new TextEncoder().encode(String(data.get("password"))).length > 72) {
      setError("La contraseña es demasiado larga. Usá menos caracteres.")
      return
    }
    if (data.get("password") !== data.get("confirm-password")) {
      setError("Las contraseñas no coinciden. Volvé a escribirlas.")
      return
    }
    if (!data.get("terms")) {
      setError("Aceptá las condiciones para crear tu cuenta.")
      return
    }
    setError("")
    if (registration.isPending) return
    registration.mutate({
      nombreCompleto: `${String(data.get("first-name")).trim()} ${String(data.get("last-name")).trim()}`,
      email: String(data.get("email")).trim(),
      dni: Number(data.get("dni")),
      password: String(data.get("password")),
    })
  }
  return (
    <AuthLayout wide>
      {success ? (
        <FeedbackState
          title="Tu cuenta está lista"
          description="Tu cuenta se registró. Iniciá sesión para consultar y actualizar tus datos."
          action={
            <Button render={<Link to="/login" />}>
              Iniciar sesión
              <ArrowRight />
            </Button>
          }
        />
      ) : (
        <>
          <Link
            to="/login"
            className="mb-6 flex items-center gap-2 text-xs text-muted-foreground"
          >
            <ArrowLeft className="size-3.5" />
            Volver a iniciar sesión
          </Link>
          <h1 className="text-3xl font-bold tracking-[-0.04em]">
            Crear una cuenta
          </h1>
          <p className="mt-2 mb-7 text-sm text-muted-foreground">
            Sumate al Polideportivo de la UNSE.
          </p>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              {[
                {
                  id: "first-name",
                  label: "Nombre",
                  placeholder: "Gonzalo",
                  autoComplete: "given-name",
                },
                {
                  id: "last-name",
                  label: "Apellido",
                  placeholder: "Pérez",
                  autoComplete: "family-name",
                },
                {
                  id: "dni",
                  label: "DNI",
                  placeholder: "Sin puntos",
                  autoComplete: "off",
                },
              ].map((field) => (
                <div key={field.id} className="space-y-2">
                  <Label htmlFor={field.id}>{field.label}</Label>
                  <Input
                    id={field.id}
                    name={field.id}
                    placeholder={field.placeholder}
                    autoComplete={field.autoComplete}
                    required
                    maxLength={field.id === "dni" ? 8 : 100}
                    pattern={field.id === "dni" ? "[0-9]{1,8}" : undefined}
                    inputMode={
                      field.id === "dni"
                        ? "numeric"
                        : field.id === "phone"
                          ? "tel"
                          : "text"
                    }
                    className="h-11"
                  />
                </div>
              ))}
            </div>
            <div className="space-y-2">
              <Label htmlFor="register-email">Email</Label>
              <Input
                id="register-email"
                name="email"
                type="email"
                autoComplete="email"
                maxLength={100}
                placeholder="nombre@unse.edu.ar"
                required
                className="h-11"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <PasswordInput id="password" autoComplete="new-password" />
              <PasswordInput
                id="confirm-password"
                label="Confirmar contraseña"
                autoComplete="new-password"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Usá entre 8 y 72 caracteres.
            </p>
            <Label className="items-start gap-2 text-xs leading-relaxed font-normal">
              <Checkbox name="terms" value="accepted" className="mt-0.5" />
              Acepto los términos, las condiciones de uso y la política de
              privacidad.
            </Label>
            {error && (
              <p
                role="alert"
                className="rounded-lg bg-rose-50 p-3 text-xs text-rose-800"
              >
                {error}
              </p>
            )}
            <Button
              type="submit"
              disabled={registration.isPending}
              className="h-11 w-full"
            >
              Crear cuenta
              <ArrowRight />
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  )
}

export function ForgotPasswordPage() {
  return (
    <AuthLayout>
      <h1 className="text-3xl font-bold">Recuperar acceso</h1>
      <p className="my-6 text-sm text-muted-foreground">
        Contactá a administración para solicitar un cambio de contraseña.
      </p>
      <Button variant="outline" render={<Link to="/login" />}>
        Volver a iniciar sesión
      </Button>
    </AuthLayout>
  )
}
