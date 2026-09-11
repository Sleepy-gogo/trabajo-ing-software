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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
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
const roleNames = { admin: "Administración", member: "Socio", staff: "Accesos" }

export function SeraBrand({
  light = false,
  compact = false,
}: {
  light?: boolean
  compact?: boolean
}) {
  return (
    <Link
      to="/admin"
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
  const role: DemoRole =
    roleProp ?? (location.pathname.startsWith("/app") ? "member" : "admin")
  const [menuOpen, setMenuOpen] = useState(false)
  const links =
    role === "member" ? memberNav : role === "staff" ? staffNav : adminNav
  const person =
    role === "member"
      ? "Gonzalo Pérez"
      : role === "staff"
        ? "Lucía Fernández"
        : "Axel Castaño"
  const initials = role === "member" ? "GP" : role === "staff" ? "LF" : "AC"
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
          to={role === "member" ? "/app/profile" : "/admin/settings"}
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
            <div className="flex items-center gap-2">
              <span className="hidden text-[11px] text-muted-foreground xl:block">
                Vista previa
              </span>
              <Select
                value={role}
                onValueChange={(value) => {
                  if (value)
                    navigate(
                      value === "member"
                        ? "/app"
                        : value === "staff"
                          ? "/admin/access"
                          : "/admin"
                    )
                }}
              >
                <SelectTrigger
                  aria-label="Cambiar vista previa"
                  className="h-9 min-w-32 bg-muted/60 text-xs"
                >
                  <SelectValue>{roleNames[role]}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="admin">Administración</SelectItem>
                  <SelectItem value="member">Socio</SelectItem>
                  <SelectItem value="staff">Accesos</SelectItem>
                </SelectContent>
              </Select>
            </div>
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
                <p className="text-xs font-semibold">Mantenimiento de pileta</p>
                <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                  La pileta permanecerá cerrada el lunes por mantenimiento.
                </p>
                <p className="mt-4 text-xs font-semibold">
                  Verificación de datos UNSE
                </p>
                <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                  Acercate a administración para actualizar tu documentación.
                </p>
              </PopoverContent>
            </Popover>
            <Link
              to="/login"
              className="hidden rounded-md p-2 text-muted-foreground hover:text-primary sm:inline-flex"
              aria-label="Ver pantalla de inicio de sesión"
            >
              <LogOut className="size-[18px]" />
            </Link>
          </div>
        </header>
        <main
          id="main-content"
          className={cn(
            "mx-auto max-w-[1680px] px-4 pt-6 pb-10 sm:px-7 sm:pt-7",
            role === "member" && "pb-28 lg:pb-10"
          )}
        >
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
