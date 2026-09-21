import { Link, useSearchParams } from "react-router-dom"
import QRCode from "react-qr-code"
import {
  ArrowUpRight,
  CalendarDays,
  Clock3,
  CreditCard,
  MapPin,
  Pencil,
  QrCode,
  ShieldCheck,
  Ticket,
} from "lucide-react"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ImagePlaceholder } from "@/components/shared"
import {
  currentUser,
  getMembershipByUserId,
  getPaymentsByUserId,
  getReservationsByUserId,
} from "@/mocks"

import { formatDate, getPlanPrice, member, money } from "./data"
import {
  Go,
  InfoRows,
  LoadingState,
  MemberHeading,
  Notice,
  Panel,
  StateBadge,
  TextLink,
} from "./member-parts"

const membership = getMembershipByUserId(currentUser.id)
const memberReservations = getReservationsByUserId(currentUser.id)
const memberPayments = getPaymentsByUserId(currentUser.id)
const nextReservation = memberReservations.find(
  (reservation) => reservation.estado === "confirmada"
)

export function MemberHomePage() {
  const [params] = useSearchParams()
  const demoState = params.get("state")
  const isOverdue =
    demoState === "overdue" || membership?.estado === "suspendida"

  if (demoState === "loading") {
    return (
      <div>
        <MemberHeading
          description="Un vistazo a tu actividad en el polideportivo."
          title={"¡Hola, " + member.nombre + "!"}
        />
        <LoadingState label="Cargando tu resumen" />
      </div>
    )
  }

  return (
    <div>
      <MemberHeading
        action={
          <Go to="/app/reservations/new">
            <CalendarDays aria-hidden="true" />
            Reservar espacio
          </Go>
        }
        description="Un vistazo a tu actividad en el polideportivo."
        title={"¡Hola, " + member.nombre + "!"}
      />
      {isOverdue && (
        <div className="mb-5">
          <Notice
            error
            title="Tu membresía está suspendida por una cuota vencida"
          >
            Pagá la cuota pendiente para recuperar tus beneficios.{" "}
            <Link
              className="font-semibold underline"
              to="/app/payments?state=overdue"
            >
              Ver cuota pendiente
            </Link>
          </Notice>
        </div>
      )}
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        {[
          {
            label: "Mi membresía",
            value: isOverdue
              ? "Suspendida"
              : (membership?.planNombre ?? "Sin membresía"),
            text: isOverdue
              ? "Regularizá tu cuota para volver a usar tus beneficios"
              : "Activa hasta el " +
                formatDate(membership?.proximoVencimiento ?? "2026-09-01"),
            icon: ShieldCheck,
            to: "/app/memberships/status",
          },
          {
            label: "Próximo vencimiento",
            value: money(getPlanPrice(membership?.planId ?? "")),
            text:
              "Cuota mensual · " +
              formatDate(membership?.proximoVencimiento ?? "2026-09-01"),
            icon: CreditCard,
            to: "/app/payments",
          },
          {
            label: "Tickets disponibles",
            value: "1 ticket",
            text: "Aplicá el saldo a tu próxima reserva",
            icon: Ticket,
            to: "/app/reservations?tab=tickets",
          },
        ].map((item) => (
          <Link
            className="group rounded-xl border bg-card p-5 transition-[border-color,box-shadow] hover:border-primary/40 hover:shadow-sm"
            key={item.label}
            to={item.to}
          >
            <div className="mb-4 flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {item.label}
              </span>
              <item.icon aria-hidden="true" className="size-5 text-primary" />
            </div>
            <p className="text-2xl font-bold tracking-tight tabular-nums">
              {item.value}
            </p>
            <p className="mt-2 text-xs leading-5 text-muted-foreground">
              {item.text}
            </p>
          </Link>
        ))}
      </div>
      <div className="grid items-start gap-6 xl:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          {nextReservation ? (
            <Panel
              action={<StateBadge state={nextReservation.estadoLabel} />}
              title="Tu próxima reserva"
            >
              <div className="flex flex-col gap-5 sm:flex-row">
                <ImagePlaceholder
                  className="h-36 w-full rounded-lg sm:w-48"
                  label={nextReservation.spaceName}
                />
                <div className="flex-1 py-1">
                  <p className="text-xs text-muted-foreground">
                    {nextReservation.codigo}
                  </p>
                  <h3 className="mt-2 text-xl font-semibold">
                    {nextReservation.spaceName}
                  </h3>
                  <div className="mt-3 space-y-2 text-sm text-muted-foreground">
                    <p className="flex items-center gap-2">
                      <CalendarDays aria-hidden="true" className="size-4" />
                      {formatDate(nextReservation.fecha)}
                    </p>
                    <p className="flex items-center gap-2">
                      <Clock3 aria-hidden="true" className="size-4" />
                      {nextReservation.inicio} a {nextReservation.fin}
                    </p>
                  </div>
                  <TextLink to={"/app/reservations/" + nextReservation.id}>
                    Ver reserva y QR
                  </TextLink>
                </div>
              </div>
            </Panel>
          ) : (
            <Panel>
              <div className="py-5">
                <h2 className="font-semibold">Todavía no tenés reservas</h2>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Explorá los espacios y elegí un horario para tu próxima
                  visita.
                </p>
                <div className="mt-4">
                  <Go to="/app/services">Explorar espacios</Go>
                </div>
              </div>
            </Panel>
          )}
          <Panel
            action={<TextLink to="/app/payments">Ver pagos</TextLink>}
            title="Tus últimos movimientos"
          >
            <div className="divide-y">
              {[
                {
                  icon: CalendarDays,
                  title: nextReservation
                    ? "Reserva confirmada"
                    : "Sin reservas recientes",
                  detail:
                    nextReservation?.spaceName ??
                    "Explorá los espacios disponibles",
                  date: nextReservation
                    ? formatDate(nextReservation.creadaEn)
                    : "—",
                },
                {
                  icon: CreditCard,
                  title:
                    memberPayments[0]?.conceptoLabel ?? "Sin pagos registrados",
                  detail: memberPayments[0]
                    ? money(memberPayments[0].importe)
                    : "Tu historial aparecerá aquí",
                  date: memberPayments[0]
                    ? formatDate(memberPayments[0].fecha)
                    : "—",
                },
                {
                  icon: ShieldCheck,
                  title: "Carnet digital disponible",
                  detail: "Presentalo al ingresar al polideportivo",
                  date: "Siempre disponible",
                },
              ].map((item) => (
                <div
                  className="flex items-center gap-3 py-4 first:pt-0 last:pb-0"
                  key={item.title}
                >
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-primary">
                    <item.icon aria-hidden="true" className="size-4" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{item.title}</p>
                    <p className="mt-1 truncate text-xs text-muted-foreground">
                      {item.detail}
                    </p>
                  </div>
                  <span className="shrink-0 text-right text-xs text-muted-foreground">
                    {item.date}
                  </span>
                </div>
              ))}
            </div>
          </Panel>
        </div>
        <div className="space-y-5">
          <div className="rounded-xl bg-primary p-6 text-primary-foreground">
            <div className="flex items-center justify-between">
              <QrCode aria-hidden="true" className="size-7" />
              <span className="text-xs opacity-75">Carnet digital</span>
            </div>
            <h2 className="mt-7 text-xl font-semibold">
              El polideportivo, con vos.
            </h2>
            <p className="mt-3 text-sm leading-6 opacity-80">
              Mostrá tu carnet al ingresar y disfrutá tus beneficios.
            </p>
            <Go
              className="mt-6 w-full border-white/20 bg-white text-primary hover:bg-white/90"
              secondary
              to="/app/card"
            >
              Abrir mi carnet
              <ArrowUpRight aria-hidden="true" />
            </Go>
          </div>
          <Panel title="Para tu próxima visita">
            <p className="text-sm leading-6 text-muted-foreground">
              Consultá los horarios y las condiciones de cada espacio antes de
              reservar.
            </p>
            <TextLink to="/app/services">Explorar espacios</TextLink>
            <div className="mt-4 flex items-start gap-2 border-t pt-4 text-xs leading-5 text-muted-foreground">
              <MapPin aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
              Polideportivo UNSE
              <br />
              Santiago del Estero, Argentina
            </div>
          </Panel>
        </div>
      </div>
    </div>
  )
}

export function MemberProfilePage() {
  const [params, setParams] = useSearchParams()
  const editing = params.get("edit") === "true"
  const saved = params.get("state") === "saved"

  return (
    <div>
      <MemberHeading
        action={
          !editing && (
            <Button
              onClick={() => setParams({ edit: "true" })}
              variant="outline"
            >
              <Pencil aria-hidden="true" />
              Editar datos
            </Button>
          )
        }
        description="Tu información personal y tu relación con la universidad."
        title="Mi perfil"
      />
      {saved && (
        <div className="mb-5">
          <Notice title="Datos actualizados">
            Tus cambios se guardaron en esta vista de demostración.
          </Notice>
        </div>
      )}
      <div className="grid items-start gap-6 lg:grid-cols-[280px_1fr]">
        <Panel>
          <div className="text-center">
            <Avatar className="mx-auto size-24">
              <AvatarFallback className="bg-emerald-100 text-3xl font-semibold text-primary">
                {member.nombre.slice(0, 1)}
                {member.apellido.slice(0, 1)}
              </AvatarFallback>
            </Avatar>
            <h2 className="mt-4 text-xl font-semibold">
              {member.nombreCompleto}
            </h2>
            <div className="mt-2">
              <StateBadge state="Socio activo" />
            </div>
            <p className="mt-3 text-sm text-muted-foreground">
              {member.relacionUnseLabel}
            </p>
          </div>
          <div className="my-6 border-t" />
          <InfoRows
            rows={[
              ["Legajo", member.identificadorUnse ?? "No informado"],
              ["DNI", member.dni],
            ]}
          />
          <Go className="mt-6 w-full" to="/app/card">
            <QrCode aria-hidden="true" />
            Ver carnet digital
          </Go>
        </Panel>
        <Panel>
          {editing ? (
            <form
              className="space-y-6"
              onSubmit={(event) => {
                event.preventDefault()
                setParams({ state: "saved" })
              }}
            >
              <div>
                <h2 className="text-lg font-semibold">
                  Editar información de contacto
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Actualizá los datos que usamos para comunicarnos con vos.
                </p>
              </div>
              <div className="grid gap-5 sm:grid-cols-2">
                {[
                  {
                    id: "nombre",
                    label: "Nombre",
                    value: member.nombre,
                    disabled: true,
                  },
                  {
                    id: "apellido",
                    label: "Apellido",
                    value: member.apellido,
                    disabled: true,
                  },
                  {
                    id: "email",
                    label: "Correo electrónico",
                    value: member.email,
                    type: "email",
                  },
                  {
                    id: "telefono",
                    label: "Teléfono",
                    value: member.telefono ?? "",
                    type: "tel",
                  },
                  {
                    id: "direccion",
                    label: "Dirección",
                    value: member.direccion ?? "",
                  },
                ].map((field) => (
                  <div className="space-y-2" key={field.id}>
                    <Label htmlFor={field.id}>{field.label}</Label>
                    <Input
                      autoComplete={
                        field.id === "email"
                          ? "email"
                          : field.id === "telefono"
                            ? "tel"
                            : "street-address"
                      }
                      className="h-11"
                      defaultValue={field.value}
                      disabled={field.disabled}
                      id={field.id}
                      required
                      type={field.type}
                    />
                  </div>
                ))}
              </div>
              <p className="text-sm text-muted-foreground">
                Para modificar tu identidad o relación con la UNSE, comunicate
                con administración.
              </p>
              <div className="flex flex-wrap gap-3">
                <Button type="submit">Guardar cambios</Button>
                <Button
                  onClick={() => setParams({})}
                  type="button"
                  variant="outline"
                >
                  Cancelar
                </Button>
              </div>
            </form>
          ) : (
            <Tabs defaultValue="information">
              <TabsList
                className="mb-6 w-full justify-start overflow-auto"
                variant="line"
              >
                <TabsTrigger value="information">Información</TabsTrigger>
                <TabsTrigger value="membership">Membresía</TabsTrigger>
                <TabsTrigger value="activity">Actividad</TabsTrigger>
              </TabsList>
              <TabsContent value="information">
                <h2 className="mb-5 font-semibold">Datos personales</h2>
                <InfoRows
                  rows={[
                    ["Nombre completo", member.nombreCompleto],
                    ["DNI", member.dni],
                    ["Correo electrónico", member.email],
                    ["Teléfono", member.telefono ?? "No informado"],
                    ["Dirección", member.direccion ?? "No informada"],
                    ["Relación con UNSE", member.relacionUnseLabel],
                    [
                      "Verificación",
                      <StateBadge state={member.verificacionLabel} />,
                    ],
                  ]}
                />
              </TabsContent>
              <TabsContent value="membership">
                <InfoRows
                  rows={[
                    ["Plan", membership?.planNombre ?? "Sin membresía"],
                    [
                      "Estado",
                      <StateBadge
                        state={membership?.estadoLabel ?? "Sin membresía"}
                      />,
                    ],
                    [
                      "Fecha de alta",
                      membership ? formatDate(membership.fechaAlta) : "—",
                    ],
                    [
                      "Cuota mensual",
                      membership ? money(getPlanPrice(membership.planId)) : "—",
                    ],
                  ]}
                />
                <div className="mt-6">
                  <Go secondary to="/app/memberships/status">
                    Administrar membresía
                  </Go>
                </div>
              </TabsContent>
              <TabsContent value="activity">
                <div className="space-y-4">
                  {memberReservations.slice(0, 3).map((item) => (
                    <div
                      className="flex flex-wrap justify-between gap-3 rounded-lg border p-4"
                      key={item.id}
                    >
                      <div className="text-sm">
                        <p className="font-medium">{item.spaceName}</p>
                        <p className="mt-1 text-muted-foreground">
                          {formatDate(item.fecha)}
                        </p>
                      </div>
                      <StateBadge state={item.estadoLabel} />
                    </div>
                  ))}
                  {!memberReservations.length && (
                    <p className="py-5 text-sm text-muted-foreground">
                      Todavía no hay actividad para mostrar.
                    </p>
                  )}
                </div>
              </TabsContent>
            </Tabs>
          )}
        </Panel>
      </div>
    </div>
  )
}

export function MemberCardPage() {
  const [params] = useSearchParams()
  const suspended = params.get("state") === "suspended"
  const initials = member.nombre.slice(0, 1) + member.apellido.slice(0, 1)

  return (
    <div>
      <MemberHeading
        back="/app"
        description="Presentá este código en el ingreso al polideportivo."
        title="Mi carnet"
      />
      <div className="mx-auto max-w-sm">
        <div className="overflow-hidden rounded-2xl border bg-card shadow-sm">
          <div className="flex items-center justify-between bg-primary px-7 py-6 text-primary-foreground">
            <div>
              <p className="text-2xl font-extrabold tracking-tight">
                SERA<span className="text-emerald-300">.</span>
              </p>
              <p className="mt-1 text-xs opacity-80">Polideportivo UNSE</p>
            </div>
            <ShieldCheck aria-hidden="true" className="size-8" />
          </div>
          <div className="p-7 text-center">
            <Avatar className="mx-auto size-20">
              <AvatarFallback className="bg-emerald-50 text-2xl text-primary">
                {initials}
              </AvatarFallback>
            </Avatar>
            <h2 className="mt-4 text-xl font-bold">{member.nombreCompleto}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {member.relacionUnseLabel}
            </p>
            <div className="mt-3">
              <StateBadge state={suspended ? "Suspendida" : "Socio activo"} />
            </div>
            <div className="mx-auto my-7 w-fit rounded-xl border bg-white p-5">
              <QRCode
                size={192}
                title="Código QR personal de demostración"
                value={"SERA-DEMO:MEMBER:" + member.id}
              />
            </div>
            <p className="text-sm">
              Legajo{" "}
              <span className="font-semibold tabular-nums">
                {member.identificadorUnse ?? "—"}
              </span>
            </p>
            <p className="mt-2 text-xs text-muted-foreground">
              DNI terminado en {member.dni.slice(-4)}
            </p>
            {suspended && (
              <div className="mt-5 rounded-lg bg-rose-50 p-3 text-left text-xs leading-5 text-rose-800">
                La membresía está suspendida. El personal validará tu estado
                antes de autorizar el ingreso.
              </div>
            )}
            <div className="mt-6 border-t pt-5 text-xs text-muted-foreground">
              La autorización depende de tu membresía y del servicio que vayas a
              utilizar.
            </div>
          </div>
          <div className="bg-muted/50 py-4 text-center text-xs font-medium text-muted-foreground">
            Universidad Nacional de Santiago del Estero
          </div>
        </div>
        <p className="mt-5 text-center text-xs text-muted-foreground">
          Carnet de referencia. Este QR no habilita un ingreso real.
        </p>
      </div>
    </div>
  )
}
