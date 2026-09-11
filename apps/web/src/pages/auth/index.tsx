import { useState, type FormEvent, type ReactNode } from "react"
import { Link, useNavigate } from "react-router-dom"
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Eye,
  EyeOff,
  Landmark,
  LockKeyhole,
  Mail,
  QrCode,
  Users,
} from "lucide-react"
import { SeraBrand } from "@/components/layout/app-shell"
import { FeedbackState, ImagePlaceholder } from "@/components/shared"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

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
          <ImagePlaceholder
            asset="polideportivo-exterior"
            label="Polideportivo de la UNSE"
            className="absolute inset-0 aspect-auto! bg-[#193b52] text-white/15"
          />
          <div className="absolute inset-0 bg-linear-to-b from-[#10243a]/30 to-[#10243a]/95" />
          <div className="relative">
            <SeraBrand light />
            <div className="mt-24">
              <p className="text-6xl leading-none font-extrabold tracking-[-0.055em] xl:text-7xl">
                Tu lugar
                <br />
                para moverte.
              </p>
              <p className="mt-7 max-w-sm text-lg leading-relaxed text-slate-200">
                Reservá espacios, gestioná tu membresía y disfrutá del deporte
                en la UNSE.
              </p>
            </div>
          </div>
          <div className="relative">
            <div className="mb-12 grid grid-cols-3 gap-5">
              {[
                { icon: Users, label: "Comunidad UNSE" },
                { icon: CalendarDays, label: "Reservas de espacios" },
                { icon: QrCode, label: "Carnet digital" },
              ].map(({ icon: Icon, label }) => (
                <div key={label}>
                  <div className="mb-3 flex size-11 items-center justify-center rounded-xl border border-white/20 bg-white/5">
                    <Icon aria-hidden="true" className="size-5" />
                  </div>
                  <span className="text-xs leading-relaxed text-slate-200">
                    {label}
                  </span>
                </div>
              ))}
            </div>
            <div className="flex items-center gap-3 border-t border-white/15 pt-6">
              <Landmark className="size-8 text-slate-300" aria-hidden="true" />
              <span className="text-xs leading-relaxed text-slate-300">
                Universidad Nacional
                <br />
                de Santiago del Estero
              </span>
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
            <Link
              to="/admin"
              className="ml-auto inline-flex items-center gap-2 text-xs font-semibold text-primary sm:ml-5"
            >
              Explorar demostración
              <ArrowRight className="size-3.5" aria-hidden="true" />
            </Link>
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
          minLength={6}
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
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    if (String(data.get("email")).includes("error")) {
      setError(
        "El email o la contraseña no coinciden. Revisá los datos e intentá de nuevo."
      )
      return
    }
    navigate("/app")
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
          <Label htmlFor="email">Email o usuario</Label>
          <div className="relative">
            <Mail
              className="absolute top-3.5 left-3.5 size-4 text-muted-foreground"
              aria-hidden="true"
            />
            <Input
              id="email"
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
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Label className="gap-2 text-xs font-normal">
            <Checkbox name="remember" />
            Mantener sesión iniciada
          </Label>
          <Link
            to="/forgot-password"
            className="text-xs font-semibold text-primary"
          >
            ¿Olvidaste tu contraseña?
          </Link>
        </div>
        {error && (
          <p
            id="login-error"
            role="alert"
            className="rounded-lg bg-rose-50 p-3 text-xs leading-relaxed text-rose-800"
          >
            {error}
          </p>
        )}
        <Button type="submit" className="h-11 w-full">
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
        Podés recorrer todas las pantallas sin una cuenta desde la demostración.
      </p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const [success, setSuccess] = useState(false)
  const [relationship, setRelationship] = useState<string | null>("Estudiante")
  const [error, setError] = useState("")
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    if (data.get("password") !== data.get("confirm-password")) {
      setError("Las contraseñas no coinciden. Volvé a escribirlas.")
      return
    }
    if (!data.get("terms")) {
      setError("Aceptá las condiciones para crear tu cuenta.")
      return
    }
    setError("")
    setSuccess(true)
  }
  return (
    <AuthLayout wide>
      {success ? (
        <FeedbackState
          title="Tu cuenta está lista"
          description="Ya podés explorar los espacios y reservar. Administración verificará tu relación con la UNSE antes de aplicar los beneficios correspondientes."
          action={
            <Button render={<Link to="/app" />}>
              Ir a mi inicio
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
                {
                  id: "phone",
                  label: "Teléfono",
                  placeholder: "385 123 4567",
                  autoComplete: "tel",
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
                placeholder="nombre@unse.edu.ar"
                required
                className="h-11"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="relationship">Relación con la UNSE</Label>
                <Select value={relationship} onValueChange={setRelationship}>
                  <SelectTrigger id="relationship" className="h-11 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {["Estudiante", "Docente", "No docente", "Externo"].map(
                      (value) => (
                        <SelectItem key={value} value={value}>
                          {value}
                        </SelectItem>
                      )
                    )}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="legajo">
                  Legajo{" "}
                  <span className="font-normal text-muted-foreground">
                    opcional
                  </span>
                </Label>
                <Input
                  id="legajo"
                  name="legajo"
                  placeholder="Tu número de legajo"
                  className="h-11"
                />
              </div>
            </div>
            <p className="rounded-lg bg-blue-50 p-3 text-xs leading-relaxed text-blue-800">
              Administración verificará tu relación con la UNSE de forma
              presencial.
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              <PasswordInput id="password" autoComplete="new-password" />
              <PasswordInput
                id="confirm-password"
                label="Confirmar contraseña"
                autoComplete="new-password"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Usá al menos 6 caracteres en esta demostración.
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
            <Button type="submit" className="h-11 w-full">
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
  const [sent, setSent] = useState(false)
  return (
    <AuthLayout>
      {sent ? (
        <FeedbackState
          title="Revisá tu email"
          description="Si el email está asociado a una cuenta, recibirás las instrucciones para recuperar el acceso. Esta pantalla muestra el resultado de ejemplo."
          action={
            <Button variant="outline" render={<Link to="/login" />}>
              Volver a iniciar sesión
            </Button>
          }
        />
      ) : (
        <>
          <Link
            to="/login"
            className="mb-7 flex items-center gap-2 text-xs text-muted-foreground"
          >
            <ArrowLeft className="size-4" />
            Volver a iniciar sesión
          </Link>
          <span className="mb-6 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <LockKeyhole className="size-6" />
          </span>
          <h1 className="text-3xl font-bold tracking-[-0.04em]">
            Recuperar contraseña
          </h1>
          <p className="mt-3 mb-8 text-sm leading-relaxed text-muted-foreground">
            Ingresá el email de tu cuenta para recibir las instrucciones de
            recuperación.
          </p>
          <form
            className="space-y-5"
            onSubmit={(event) => {
              event.preventDefault()
              setSent(true)
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="recovery-email">Email</Label>
              <Input
                id="recovery-email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="nombre@unse.edu.ar"
                className="h-11"
                required
              />
            </div>
            <Button type="submit" className="h-11 w-full">
              Enviar instrucciones
              <ArrowRight />
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  )
}
