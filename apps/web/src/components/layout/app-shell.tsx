import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useSession } from "@/hooks/use-session"
import { usersApi, ApiError } from "@/lib/users-api"
import { useState, type ReactNode } from "react"
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom"
import {
  Activity,
  ArrowUpRight,
  Bell,
  CalendarDays,
  ChartNoAxesCombined,
  ChevronDown,
  ClipboardList,
  CreditCard,
  DoorOpen,
  House,
  Landmark,
  LogOut,
  Menu,
  QrCode,
  Settings2,
  ShieldCheck,
  Ticket,
  Users,
  X,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

export type DemoRole = "admin" | "member" | "staff"
const adminNav = [
  { label: "Inicio", path: "/admin", icon: House },
  { label: "Socios", path: "/admin/members", icon: Users },
  { label: "Niveles y precios", path: "/admin/levels", icon: ShieldCheck },
  { label: "Reservas", path: "/admin/reservations", icon: CalendarDays },
  { label: "Espacios", path: "/admin/spaces", icon: Landmark },
  { label: "Pagos", path: "/admin/payments", icon: CreditCard },
  { label: "Accesos", path: "/admin/access", icon: DoorOpen },
  { label: "Informes", path: "/admin/reports", icon: ChartNoAxesCombined },
]
const memberNav = [
  { label: "Inicio", path: "/app", icon: House },
  { label: "Mis reservas", path: "/app/reservations", icon: CalendarDays },
  { label: "Espacios", path: "/app/services", icon: Landmark },
  { label: "Mi membresía", path: "/app/memberships/status", icon: ShieldCheck },
  { label: "Mis pagos", path: "/app/payments", icon: CreditCard },
  { label: "Mi carnet", path: "/app/card", icon: QrCode },
  { label: "Encuestas", path: "/app/surveys", icon: ClipboardList },
]
const staffNav = [
  { label: "Validar ingreso", path: "/admin/access", icon: QrCode },
  {
    label: "Historial de accesos",
    path: "/admin/access?tab=history",
    icon: Activity,
  },
]
const roleNames = {
  admin: "Administración",
  member: "Usuario",
  staff: "Accesos",
}

export function SeraBrand({
  light = false,
  compact = false,
}: {
  light?: boolean
  compact?: boolean
}) {
  return (
    <Link
      to="/app/profile"
      aria-label="SERA, inicio"
      className={cn(
        "inline-flex items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4",
        light ? "text-white" : "text-foreground"
      )}
    >
      <span
        className={cn(
          "flex size-10 items-center justify-center rounded-xl",
          light ? "bg-white/10 text-blue-200" : "bg-primary/10 text-primary"
        )}
      >
        <Landmark aria-hidden="true" className="size-6" strokeWidth={1.7} />
      </span>
      <span>
        <span className="block text-xl leading-none font-extrabold tracking-tight">
          SERA<span className="text-blue-400">.</span>
        </span>
        {!compact && (
          <span
            className={cn(
              "mt-1.5 block text-[10px] font-medium",
              light ? "text-slate-300" : "text-muted-foreground"
            )}
          >
            Polideportivo UNSE
          </span>
        )}
      </span>
    </Link>
  )
}

export function AppShell({
  role: roleProp,
  children,
}: {
  role?: DemoRole
  children?: ReactNode
}) {
  const location = useLocation()
  const navigate = useNavigate()
  const session = useSession()
  const role: DemoRole =
    location.pathname === "/app/profile" && session.data
      ? session.data.rol === "ADMIN"
        ? "admin"
        : session.data.rol === "STAFF"
          ? "staff"
          : "member"
      : (roleProp ??
        (location.pathname.startsWith("/app") ? "member" : "admin"))
  const [menuOpen, setMenuOpen] = useState(false)
  const links =
    role === "member" ? memberNav : role === "staff" ? staffNav : adminNav
  const client = useQueryClient()
  const logout = useMutation({
    mutationFn: async () => {
      try {
        await usersApi.logout()
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 401)) throw error
      }
    },
    onSuccess: () => {
      client.clear()
      navigate("/login", { replace: true })
    },
  })
  const person = session.data?.nombreCompleto ?? ""
  const initials = person
    .split(" ")
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
  const navContent = (
    <>
      <div className="px-6 pt-7 pb-8">
        <SeraBrand light />
      </div>
      <nav aria-label="Navegación principal" className="space-y-1 px-3">
        {links.map(({ label, path, icon: Icon }) => (
          <NavLink
            key={path}
            end={path === "/app" || path === "/admin"}
            to={path}
            onClick={() => setMenuOpen(false)}
            className={({ isActive }) =>
              cn(
                "flex min-h-11 items-center gap-3 rounded-lg px-3 text-[13px] font-medium transition-colors",
                isActive
                  ? "bg-blue-500/20 text-white"
                  : "text-slate-300 hover:bg-white/5 hover:text-white"
              )
            }
          >
            <Icon
              aria-hidden="true"
              className="size-[18px]"
              strokeWidth={1.7}
            />
            {label}
          </NavLink>
        ))}
      </nav>
      {role === "admin" && (
        <div className="mt-8 px-3">
          <p className="mb-3 px-3 text-[11px] text-slate-400">Administración</p>
          {[
            { label: "Usuarios", path: "/admin/users", icon: Users },
            {
              label: "Configuración",
              path: "/admin/settings",
              icon: Settings2,
            },
          ].map(({ label, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                cn(
                  "flex min-h-11 items-center gap-3 rounded-lg px-3 text-[13px]",
                  isActive
                    ? "bg-blue-500/20 text-white"
                    : "text-slate-300 hover:bg-white/5"
                )
              }
            >
              <Icon className="size-[18px]" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </div>
      )}
      <div className="mt-auto px-5 pt-8 pb-5">
        <div className="mb-5 rounded-lg border border-white/10 bg-white/5 p-3">
          <p className="text-xs font-semibold text-white">
            Un espacio para encontrarnos
          </p>
          <p className="mt-1.5 text-[11px] leading-relaxed text-slate-400">
            Deporte y bienestar en la comunidad UNSE.
          </p>
        </div>
        <Link
          to="/app/profile"
          className="flex items-center gap-3 rounded-lg py-2 text-white"
        >
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-slate-600 text-xs font-bold">
            {initials}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-xs font-semibold">
              {person}
            </span>
            <span className="mt-0.5 block text-[10px] text-slate-400">
              {roleNames[role]}
            </span>
          </span>
          <ChevronDown
            className="ml-auto size-4 text-slate-400"
            aria-hidden="true"
          />
        </Link>
      </div>
    </>
  )

  return (
    <div className="min-h-svh bg-background">
      <a href="#main-content" className="skip-link">
        Saltar al contenido
      </a>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[228px] flex-col overflow-y-auto bg-sidebar lg:flex">
        {navContent}
      </aside>
      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent
          side="left"
          showCloseButton={false}
          className="w-[280px]! gap-0 overflow-y-auto border-0 bg-sidebar p-0 text-white"
        >
          <SheetHeader className="sr-only">
            <SheetTitle>Navegación de SERA</SheetTitle>
          </SheetHeader>
          <SheetClose
            render={
              <Button
                variant="ghost"
                size="icon"
                className="absolute top-3 right-3 text-white hover:bg-white/10 hover:text-white"
                aria-label="Cerrar menú"
              />
            }
          >
            <X />
          </SheetClose>
          {navContent}
        </SheetContent>
      </Sheet>
      <div className="lg:pl-[228px]">
        <header className="sticky top-0 z-20 flex min-h-16 items-center justify-between gap-3 border-b bg-white/95 px-4 backdrop-blur-sm sm:px-7">
          <div className="flex min-w-0 items-center gap-3">
            <Button
              size="icon"
              variant="ghost"
              className="lg:hidden"
              aria-label="Abrir menú"
              onClick={() => setMenuOpen(true)}
            >
              <Menu />
            </Button>
            <span className="hidden items-center gap-2 text-xs text-muted-foreground sm:flex">
              <Landmark className="size-4" aria-hidden="true" />
              <span>Polideportivo UNSE</span>
              <span className="mx-1 text-border">/</span>
              <span className="text-foreground">{roleNames[role]}</span>
            </span>
            <span className="font-bold sm:hidden">SERA.</span>
          </div>
          <div className="flex items-center gap-2 sm:gap-4">
            <Popover>
              <PopoverTrigger
                render={
                  <Button size="icon" variant="ghost" aria-label="Ver avisos" />
                }
              >
                <Bell className="size-[18px]" />
              </PopoverTrigger>
              <PopoverContent align="end" className="w-80">
                <h2 className="mb-4 text-sm font-bold">
                  Avisos del polideportivo
                </h2>
                <p className="mt-4 text-xs font-semibold">
                  Verificación de datos UNSE
                </p>
                <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                  Acercate a administración para actualizar tu documentación.
                </p>
              </PopoverContent>
            </Popover>
            <Button
              variant="ghost"
              disabled={logout.isPending}
              onClick={() => logout.mutate()}
              aria-label="Cerrar sesión"
            >
              <LogOut className="size-[18px]" />
            </Button>
            {logout.error && (
              <p role="alert" className="text-xs text-destructive">
                {logout.error.message}
              </p>
            )}
          </div>
        </header>
        <main
          id="main-content"
          className={cn(
            "mx-auto max-w-[1680px] px-4 pt-6 pb-10 sm:px-7 sm:pt-7",
            role === "member" && "pb-28 lg:pb-10"
          )}
        >
          {/\/(payments|reservations|access|reports|surveys|settings)(\/|$)/.test(
            location.pathname
          ) && (
            <div
              role="note"
              className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900"
            >
              <strong>Vista previa de próximos incrementos.</strong> Esta
              sección usa datos de ejemplo y sus cambios no se guardan. La
              entrega actual incluye usuarios, socios, membresías, espacios y
              disponibilidad.
            </div>
          )}
          {children ?? <Outlet />}
        </main>
        <footer
          className={cn(
            "mx-4 flex flex-wrap items-center justify-between gap-2 border-t py-4 text-[10px] text-muted-foreground sm:mx-7",
            role === "member" && "mb-20 lg:mb-0"
          )}
        >
          <span>SERA · Universidad Nacional de Santiago del Estero</span>
          <Link
            to="/login"
            className="inline-flex items-center gap-1 hover:text-primary"
          >
            Explorar acceso y registro
            <ArrowUpRight className="size-3" aria-hidden="true" />
          </Link>
        </footer>
      </div>
      {role === "member" && (
        <nav
          aria-label="Navegación móvil"
          className="fixed inset-x-0 bottom-0 z-30 flex items-center justify-around border-t bg-white px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] lg:hidden"
        >
          {[
            { label: "Inicio", path: "/app", icon: House },
            {
              label: "Reservas",
              path: "/app/reservations",
              icon: CalendarDays,
            },
            { label: "Mi carnet", path: "/app/card", icon: QrCode },
            { label: "Pagos", path: "/app/payments", icon: Ticket },
          ].map(({ label, path, icon: Icon }) => (
            <NavLink
              end
              key={path}
              to={path}
              className={({ isActive }) =>
                cn(
                  "flex min-h-12 min-w-14 flex-col items-center gap-1 rounded-lg px-2 py-1 text-[10px]",
                  isActive ? "font-bold text-primary" : "text-muted-foreground"
                )
              }
            >
              <Icon className="size-5" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
          <Button
            variant="ghost"
            className="h-12 flex-col gap-1 px-2 text-[10px] text-muted-foreground"
            onClick={() => setMenuOpen(true)}
          >
            <Menu className="size-5" />
            Más
          </Button>
        </nav>
      )}
    </div>
  )
}
