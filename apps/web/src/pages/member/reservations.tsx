import { useState } from "react"
import { format } from "date-fns"
import { es } from "date-fns/locale"
import { useParams, useSearchParams } from "react-router-dom"
import QRCode from "react-qr-code"
import {
  ArrowRight,
  CalendarDays,
  Check,
  CircleAlert,
  Clock3,
  MapPin,
  Ticket,
  Users,
} from "lucide-react"

import { ImagePlaceholder, EmptyState } from "@/components/shared"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  currentUser,
  getAvailabilityForSpace,
  getReservationById,
  getReservationsByUserId,
  getSpaceById,
  getTicketsByUserId,
  reservations as allReservations,
  spaces,
} from "@/mocks"

import { formatDate, member, money } from "./data"
import {
  Choice,
  Go,
  InfoRows,
  LoadingState,
  MemberHeading,
  Notice,
  Panel,
  Result,
  StateBadge,
  TextLink,
} from "./member-parts"

const categoryOptions = ["Todos", "deporte", "recreacion", "servicio"]
const categoryLabels: Record<string, string> = {
  Todos: "Todos",
  deporte: "Canchas",
  recreacion: "Quinchos",
  servicio: "Servicios",
}
const times = [
  "08:00",
  "09:00",
  "10:00",
  "11:00",
  "12:00",
  "13:00",
  "14:00",
  "15:00",
  "16:00",
  "17:00",
  "18:00",
  "19:00",
  "20:00",
  "21:00",
]

export function MemberServicesPage() {
  const [params] = useSearchParams()
  const [category, setCategory] = useState(params.get("category") ?? "Todos")
  const [search, setSearch] = useState("")
  const loading = params.get("state") === "loading"
  const empty = params.get("state") === "empty"
  const filtered = spaces.filter(
    (space) =>
      (category === "Todos" || space.tipo === category) &&
      space.nombre.toLowerCase().includes(search.toLowerCase())
  )

  if (loading) {
    return (
      <div>
        <MemberHeading
          description="Elegí dónde disfrutar tu próxima visita."
          title="Espacios del polideportivo"
        />
        <LoadingState label="Cargando espacios" />
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
        description="Elegí dónde disfrutar tu próxima visita."
        title="Espacios del polideportivo"
      />
      <div className="mb-6 flex flex-wrap justify-between gap-4">
        <div className="flex flex-wrap gap-2">
          {categoryOptions.map((item) => (
            <Button
              aria-pressed={category === item}
              key={item}
              onClick={() => setCategory(item)}
              variant={category === item ? "default" : "outline"}
            >
              {categoryLabels[item]}
            </Button>
          ))}
        </div>
        <Input
          aria-label="Buscar espacio"
          className="h-10 w-full sm:max-w-xs"
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar un espacio…"
          value={search}
        />
      </div>
      {empty || !filtered.length ? (
        <Panel>
          <EmptyState
            action={
              <Button
                onClick={() => {
                  setSearch("")
                  setCategory("Todos")
                }}
                variant="outline"
              >
                Limpiar filtros
              </Button>
            }
            description="Probá con otra búsqueda o revisá las categorías disponibles."
            title="No encontramos espacios"
          />
        </Panel>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {filtered.map((space) => (
            <Panel className="[&>[data-slot=card-content]]:p-0" key={space.id}>
              <ImagePlaceholder
                asset={space.imagenes[0]?.id}
                className="h-44 w-full"
                label={space.imagenes[0]?.placeholder ?? space.nombre}
              />
              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="font-semibold">{space.nombre}</h2>
                  <StateBadge state={space.estadoLabel} />
                </div>
                <p className="mt-2 text-sm text-muted-foreground">
                  {space.descripcion}
                </p>
                <p className="mt-4 flex items-center gap-2 text-xs text-muted-foreground">
                  <Users aria-hidden="true" className="size-4" />
                  Capacidad: {space.capacidad} personas
                </p>
                <div className="mt-5 flex items-center justify-between gap-3 border-t pt-4">
                  <p className="font-semibold tabular-nums">
                    {money(
                      space.precios.find(
                        (price) => price.relacion === "socio_activo"
                      )?.importe ??
                        space.precios[0]?.importe ??
                        0
                    )}
                    <span className="ml-1 text-xs font-normal text-muted-foreground">
                      / reserva
                    </span>
                  </p>
                  <TextLink to={"/app/services/" + space.id}>
                    Ver espacio
                  </TextLink>
                </div>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  )
}

export function MemberServiceDetailPage() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const [accessChecked, setAccessChecked] = useState(false)
  const space = getSpaceById(id ?? "")
  const denied = params.get("state") === "denied"

  if (!space) {
    return (
      <Result
        description="Volvé al listado para elegir un espacio disponible."
        error
        title="Espacio no encontrado"
      >
        <Go to="/app/services">Ver espacios</Go>
      </Result>
    )
  }

  const memberPrice =
    space.precios.find((price) => price.relacion === "socio_activo")?.importe ??
    space.precios.find((price) => price.relacion === currentUser.relacionUnse)
      ?.importe ??
    space.precios[0]?.importe ??
    0
  const unavailable =
    space.estado === "en_mantenimiento" || space.estado === "inutilizable"

  return (
    <div>
      <MemberHeading
        action={<StateBadge state={space.estadoLabel} />}
        back="/app/services"
        description={space.descripcion}
        title={space.nombre}
      />
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_340px]">
        <div>
          <ImagePlaceholder
            asset={space.imagenes[0]?.id}
            className="mb-6 h-64 rounded-xl sm:h-80"
            label={space.imagenes[0]?.placeholder ?? space.nombre}
          />
          <Panel title="Acerca de este espacio">
            <p className="text-sm leading-7 text-muted-foreground">
              Un espacio del Polideportivo UNSE para compartir tu próxima
              actividad. Consultá las condiciones, elegí un horario disponible y
              completá tu reserva.
            </p>
            <div className="mt-6 grid gap-5 sm:grid-cols-3">
              {[
                {
                  icon: Users,
                  label: "Capacidad",
                  value: space.capacidad + " personas",
                },
                { icon: Clock3, label: "Horario", value: "08:00 a 22:00" },
                {
                  icon: MapPin,
                  label: "Ubicación",
                  value: "Polideportivo UNSE",
                },
              ].map((item) => (
                <div key={item.label}>
                  <item.icon
                    aria-hidden="true"
                    className="mb-3 size-5 text-primary"
                  />
                  <p className="text-xs text-muted-foreground">{item.label}</p>
                  <p className="mt-1 text-sm font-medium">{item.value}</p>
                </div>
              ))}
            </div>
            <h3 className="mt-7 mb-3 font-semibold">Condiciones de uso</h3>
            <ul className="list-disc space-y-2 pl-4 text-sm leading-6 text-muted-foreground">
              <li>Presentá el QR o código de la reserva al ingresar.</li>
              <li>Respetá el horario y la capacidad del espacio.</li>
              <li>La reserva se confirma al completar el pago total.</li>
              <li>
                Una cancelación puede generar un ticket para otra reserva.
              </li>
            </ul>
          </Panel>
        </div>
        <div className="space-y-5">
          <Panel title="Tarifa de reserva">
            <InfoRows
              rows={[
                ["Tarifa para tu membresía", money(memberPrice)],
                ["Capacidad máxima", space.capacidad + " personas"],
              ]}
            />
            <p className="my-5 text-xs leading-5 text-muted-foreground">
              El importe final depende de la duración y de la disponibilidad
              seleccionada.
            </p>
            {unavailable ? (
              <Notice error title="Espacio no disponible">
                Las reservas estarán disponibles cuando el espacio vuelva a
                habilitarse.
              </Notice>
            ) : (
              <Go
                className="w-full"
                to={"/app/reservations/new?space=" + space.id}
              >
                Consultar disponibilidad
              </Go>
            )}
          </Panel>
          <Panel title="Mi acceso">
            <p className="mb-4 text-sm text-muted-foreground">
              Consultá si tu membresía cumple las condiciones del servicio.
            </p>
            <Button
              className="w-full"
              onClick={() => setAccessChecked(true)}
              variant="outline"
            >
              Consultar mi acceso
            </Button>
            {accessChecked && (
              <div className="mt-4">
                <Notice
                  error={denied}
                  title={
                    denied
                      ? "Necesitás una membresía activa"
                      : "Podés reservar este espacio"
                  }
                >
                  {denied
                    ? "Consultá los niveles disponibles para acceder a este servicio."
                    : "Tu membresía está activa. Este espacio requiere una reserva confirmada."}
                </Notice>
                {denied && (
                  <TextLink to="/app/memberships">Ver membresías</TextLink>
                )}
              </div>
            )}
          </Panel>
        </div>
      </div>
    </div>
  )
}

export function BookingPage() {
  const [params, setParams] = useSearchParams()
  const [step, setStep] = useState(Number(params.get("step") ?? 1))
  const [spaceId, setSpaceId] = useState(params.get("space") ?? spaces[0]?.id)
  const [date, setDate] = useState<Date>(new Date(2026, 7, 26))
  const [slot, setSlot] = useState("")
  const [duration, setDuration] = useState("1 hora")
  const [ticketApplied, setTicketApplied] = useState(false)
  const [accepted, setAccepted] = useState(false)
  const [method, setMethod] = useState("Mercado Pago")
  const [error, setError] = useState("")
  const space = getSpaceById(spaceId) ?? spaces[0]
  const dateIso = format(date, "yyyy-MM-dd")
  const availability = space
    ? (getAvailabilityForSpace(space.id, dateIso)[0]?.slots ?? [])
    : []
  const slotByTime = new Map(availability.map((item) => [item.inicio, item]))
  const availableTimes = availability.length
    ? availability
    : times.map((time, index) => ({
        id: "demo-" + time,
        inicio: time,
        fin:
          time === "21:00"
            ? "22:00"
            : (Number(time.slice(0, 2)) + 1).toString().padStart(2, "0") +
              ":00",
        estado: [1, 4, 6, 9].includes(index) ? "ocupado" : "disponible",
        estadoLabel: [1, 4, 6, 9].includes(index) ? "Reservado" : "Disponible",
      }))
  const hours = Number(duration[0])
  const basePrice =
    space?.precios.find((price) => price.relacion === "socio_activo")
      ?.importe ??
    space?.precios[0]?.importe ??
    0
  const gross = basePrice * hours
  const ticketValue = ticketApplied ? 6000 : 0
  const total = Math.max(0, gross - ticketValue)
  const dateLabel = format(date, "EEEE d 'de' MMMM", { locale: es })
  const state = params.get("state")
  const isResult = ["conflict", "rejected", "expired", "pending"].includes(
    state ?? ""
  )

  if (isResult) {
    return (
      <Result
        description={
          state === "conflict"
            ? "La disponibilidad cambió antes de confirmar tu reserva. Elegí otro horario para continuar."
            : state === "pending"
              ? "La reserva todavía no está confirmada. El código y el QR estarán disponibles cuando se registre el pago completo."
              : "La reserva no se confirmó. Podés revisar la disponibilidad y volver a intentar el pago."
        }
        error={state !== "pending"}
        title={
          state === "conflict"
            ? "Ese horario ya no está disponible"
            : state === "expired"
              ? "La orden de pago venció"
              : state === "pending"
                ? "Pago pendiente de confirmación"
                : "El pago fue rechazado"
        }
      >
        <Button
          onClick={() => {
            setParams({})
            setStep(2)
          }}
        >
          Volver a disponibilidad
        </Button>
        <Go secondary to="/app/reservations">
          Mis reservas
        </Go>
      </Result>
    )
  }

  if (state === "success") {
    return (
      <div>
        <Result
          description={
            (space?.nombre ?? "Espacio") +
            ". " +
            dateLabel +
            ", " +
            (slot || "18:00") +
            ". Presentá este código al llegar al polideportivo."
          }
          title="¡Tu reserva está confirmada!"
        >
          <Go to="/app/reservations">Ver mis reservas</Go>
        </Result>
        <div className="mx-auto mt-6 max-w-xl rounded-xl border bg-white p-6 text-center">
          <QRCode
            className="mx-auto"
            size={160}
            title="QR de reserva de demostración"
            value="SERA-DEMO:RESERVATION:NEW"
          />
          <p className="mt-4 text-lg font-bold tracking-wider">SERA-DEMO-001</p>
          <p className="mt-2 text-xs text-muted-foreground">
            Pago aprobado · {money(total)}
          </p>
        </div>
      </div>
    )
  }

  const chooseSlot = (time: string, available: boolean) => {
    if (available) {
      setSlot(time)
      setError("")
    }
  }

  return (
    <div>
      <MemberHeading
        action={
          <Go secondary to="/app/reservations">
            Mis reservas
          </Go>
        }
        description="Elegí el espacio, la fecha y el horario de tu próxima visita."
        title="Reservar espacio"
      />
      <ol
        className="mb-7 grid grid-cols-4 gap-2"
        aria-label="Pasos de la reserva"
      >
        {["Espacio", "Fecha y horario", "Confirmación", "Pago"].map(
          (label, index) => (
            <li
              className={
                "flex items-center gap-2 border-b-2 pb-4 text-xs sm:gap-3 sm:text-sm " +
                (index + 1 <= step
                  ? "border-primary text-primary"
                  : "border-border text-muted-foreground")
              }
              key={label}
            >
              <span
                className={
                  "flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold " +
                  (index + 1 <= step
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted")
                }
              >
                {index + 1 < step ? (
                  <Check aria-hidden="true" className="size-4" />
                ) : (
                  index + 1
                )}
              </span>
              <span className="hidden sm:inline">{label}</span>
            </li>
          )
        )}
      </ol>
      <div className="grid items-start gap-6 xl:grid-cols-[1fr_300px]">
        <div className="space-y-5">
          {step === 1 && (
            <Panel title="Elegí un espacio">
              <div className="grid gap-4 sm:grid-cols-2">
                {spaces.map((item) => {
                  const unavailable =
                    item.estado !== "habilitado" && item.estado !== "en_uso"
                  return (
                    <Button
                      aria-pressed={spaceId === item.id}
                      className={
                        "h-auto flex-col items-stretch overflow-hidden p-0 text-left whitespace-normal " +
                        (spaceId === item.id
                          ? "border-primary ring-1 ring-primary"
                          : "")
                      }
                      disabled={unavailable}
                      key={item.id}
                      onClick={() => setSpaceId(item.id)}
                      variant="outline"
                    >
                      <ImagePlaceholder
                        asset={item.imagenes[0]?.id}
                        className="h-28 w-full"
                        label={item.imagenes[0]?.placeholder ?? item.nombre}
                      />
                      <div className="w-full p-4">
                        <div className="flex justify-between gap-2">
                          <span className="font-semibold">{item.nombre}</span>
                          {spaceId === item.id && (
                            <Check
                              aria-hidden="true"
                              className="size-4 text-primary"
                            />
                          )}
                        </div>
                        <p className="mt-2 text-xs font-normal text-muted-foreground">
                          {item.descripcion}
                        </p>
                        <p className="mt-3 text-sm">
                          {unavailable
                            ? item.estadoLabel
                            : money(
                                item.precios.find(
                                  (price) => price.relacion === "socio_activo"
                                )?.importe ??
                                  item.precios[0]?.importe ??
                                  0
                              ) + " / hora"}
                        </p>
                      </div>
                    </Button>
                  )
                })}
              </div>
            </Panel>
          )}
          {step === 2 && (
            <Panel title="Elegí fecha y horario">
              <div className="grid gap-7 lg:grid-cols-[auto_1fr]">
                <Calendar
                  className="mx-auto rounded-lg border [--cell-size:2.4rem]"
                  defaultMonth={new Date(2026, 7, 1)}
                  locale={es}
                  mode="single"
                  onSelect={(next) => next && setDate(next)}
                  selected={date}
                />
                <div>
                  <h3 className="mb-4 text-sm font-semibold capitalize">
                    {dateLabel}
                  </h3>
                  <div className="grid grid-cols-3 gap-2">
                    {availableTimes.map((item) => {
                      const state =
                        slotByTime.get(item.inicio)?.estado ?? item.estado
                      const available = state === "disponible"
                      const selected = slot === item.inicio
                      return (
                        <Button
                          aria-label={
                            item.inicio +
                            ", " +
                            (available ? "disponible" : "ocupado")
                          }
                          aria-pressed={selected}
                          className={
                            "h-12 flex-col gap-0.5 " +
                            (!available
                              ? "bg-rose-50 text-rose-700 disabled:opacity-70"
                              : "")
                          }
                          disabled={!available}
                          key={item.id}
                          onClick={() => chooseSlot(item.inicio, available)}
                          variant={selected ? "default" : "outline"}
                        >
                          <span>{item.inicio}</span>
                          <span className="text-[10px] font-normal">
                            {!available
                              ? "Ocupado"
                              : selected
                                ? "Seleccionado"
                                : "Libre"}
                          </span>
                        </Button>
                      )
                    })}
                  </div>
                  <div className="mt-5 space-y-2">
                    <Label>Duración</Label>
                    <Choice
                      label="Duración de reserva"
                      onChange={setDuration}
                      options={["1 hora", "2 horas", "3 horas"]}
                      value={duration}
                    />
                  </div>
                </div>
              </div>
            </Panel>
          )}
          {step === 3 && (
            <>
              <Panel title="Revisá tu reserva">
                <InfoRows
                  rows={[
                    ["Espacio", space?.nombre ?? "—"],
                    ["Fecha", dateLabel],
                    [
                      "Horario",
                      (slot || "Elegí un horario") + " · " + duration,
                    ],
                    ["Titular", member.nombreCompleto],
                    ["Tarifa aplicada", "Socio activo"],
                  ]}
                />
              </Panel>
              <Panel
                title="Usar un ticket de reserva"
                action={
                  <Ticket aria-hidden="true" className="size-5 text-primary" />
                }
              >
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-dashed border-primary/40 bg-emerald-50/30 p-4">
                  <div>
                    <p className="text-sm font-semibold">
                      Ticket disponible · {money(6000)}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      Saldo parcial · Vence el 30/09/2026
                    </p>
                  </div>
                  <Button
                    onClick={() => setTicketApplied((current) => !current)}
                    variant={ticketApplied ? "secondary" : "outline"}
                  >
                    {ticketApplied ? "Quitar ticket" : "Aplicar ticket"}
                  </Button>
                </div>
                {ticketApplied && (
                  <p className="mt-3 text-sm text-primary">
                    Ticket aplicado. Quedan {money(total)} por abonar.
                  </p>
                )}
              </Panel>
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={accepted}
                  id="booking-terms"
                  onCheckedChange={(checked) => setAccepted(checked === true)}
                />
                <Label
                  className="text-sm leading-6 font-normal"
                  htmlFor="booking-terms"
                >
                  Acepto las condiciones de uso y cancelación. La reserva se
                  confirma cuando se registra el pago total.
                </Label>
              </div>
            </>
          )}
          {step === 4 && (
            <Panel title="Pago de la reserva">
              <StateBadge state="Pendiente de pago" />
              <p className="my-5 text-sm leading-6 text-muted-foreground">
                Elegí el medio de pago. El QR y el código de ingreso se emitirán
                después de la aprobación.
              </p>
              <Choice
                label="Medio de pago"
                onChange={setMethod}
                options={["Mercado Pago", "Transferencia bancaria"]}
                value={method}
              />
              <div className="mt-6">
                <Notice title="Confirmación de demostración">
                  Este paso muestra el resultado del pago sin realizar un cobro
                  real.
                </Notice>
              </div>
              <div className="mt-5 flex flex-wrap gap-3 text-xs">
                <Button
                  onClick={() => setParams({ state: "rejected" })}
                  variant="link"
                >
                  Ver pago rechazado
                </Button>
                <Button
                  onClick={() => setParams({ state: "conflict" })}
                  variant="link"
                >
                  Ver horario ocupado
                </Button>
              </div>
            </Panel>
          )}
          {error && (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
          <div className="flex justify-between gap-3">
            <Button
              disabled={step === 1}
              onClick={() => {
                setStep((current) => Math.max(1, current - 1))
                setError("")
              }}
              variant="outline"
            >
              Volver
            </Button>
            <Button
              className="h-11 px-5"
              onClick={() => {
                if (step === 2 && !slot) {
                  setError("Elegí un horario para continuar.")
                  return
                }
                if (step === 3 && !accepted) {
                  setError("Aceptá las condiciones de uso y cancelación.")
                  return
                }
                setError("")
                if (step === 4) {
                  setParams({
                    state: method === "Mercado Pago" ? "success" : "pending",
                  })
                } else {
                  setStep((current) => current + 1)
                }
              }}
            >
              {step === 4 ? "Confirmar pago de " + money(total) : "Continuar"}
              <ArrowRight aria-hidden="true" />
            </Button>
          </div>
        </div>
        <Panel title="Resumen de reserva">
          <ImagePlaceholder
            asset={space?.imagenes[0]?.id}
            className="mb-4 h-28 rounded-lg"
            label={space?.nombre ?? "Espacio"}
          />
          <h3 className="mb-5 font-semibold">{space?.nombre ?? "Espacio"}</h3>
          <InfoRows
            rows={[
              ["Fecha", format(date, "dd/MM/yyyy")],
              ["Hora", slot || "Sin seleccionar"],
              ["Duración", duration],
            ]}
          />
          <div className="my-5 border-t" />
          <InfoRows
            rows={[
              [money(basePrice) + " × " + hours + " h", money(gross)],
              ...(ticketApplied
                ? [
                    ["Ticket de reserva", "− " + money(ticketValue)] as [
                      string,
                      string,
                    ],
                  ]
                : []),
            ]}
          />
          <div className="mt-5 flex items-center justify-between border-t pt-5">
            <span className="text-sm font-semibold">Total a pagar</span>
            <strong className="text-2xl tabular-nums">{money(total)}</strong>
          </div>
          <p className="mt-4 text-xs leading-5 text-muted-foreground">
            La disponibilidad se verifica nuevamente antes de confirmar.
          </p>
        </Panel>
      </div>
    </div>
  )
}

export function MemberReservationsPage() {
  const [params] = useSearchParams()
  const [filter, setFilter] = useState("Todas")
  const [tab, setTab] = useState(
    params.get("tab") === "tickets" ? "tickets" : "reservations"
  )
  const reservations = getReservationsByUserId(currentUser.id)
  const tickets = getTicketsByUserId(currentUser.id)
  const filtered = reservations.filter(
    (item) => filter === "Todas" || item.estadoLabel === filter
  )
  const filters = [
    "Todas",
    "Confirmada",
    "Pendiente de pago",
    "Cancelada",
    "Cumplida",
    "Finalizada",
  ]

  return (
    <div>
      <MemberHeading
        action={
          <Go to="/app/reservations/new">
            <CalendarDays aria-hidden="true" />
            Nueva reserva
          </Go>
        }
        description="Tus próximas visitas, reservas anteriores y tickets disponibles."
        title="Mis reservas"
      />
      <div className="mb-6 flex flex-wrap justify-between gap-4">
        <div className="flex gap-2">
          <Button
            aria-pressed={tab === "reservations"}
            onClick={() => setTab("reservations")}
            variant={tab === "reservations" ? "default" : "outline"}
          >
            Reservas
          </Button>
          <Button
            aria-pressed={tab === "tickets"}
            onClick={() => setTab("tickets")}
            variant={tab === "tickets" ? "default" : "outline"}
          >
            <Ticket aria-hidden="true" />
            Mis tickets
          </Button>
        </div>
        {tab === "reservations" && (
          <Choice
            label="Estado de reserva"
            onChange={setFilter}
            options={filters}
            value={filter}
          />
        )}
      </div>
      {params.get("state") === "loading" ? (
        <LoadingState label="Cargando tus reservas" />
      ) : tab === "tickets" ? (
        tickets.length ? (
          <div className="grid gap-5 md:grid-cols-2">
            {tickets.map((item) => (
              <Panel key={item.id}>
                <div className="flex items-center justify-between">
                  <Ticket aria-hidden="true" className="size-6 text-primary" />
                  <StateBadge state={item.estadoLabel} />
                </div>
                <h2 className="mt-5 text-lg font-semibold">
                  Ticket {item.codigo}
                </h2>
                <p className="my-3 text-2xl font-bold tabular-nums">
                  {money(item.valorDisponible)}
                </p>
                <p className="text-sm text-muted-foreground">
                  {item.coberturaDescripcion}
                </p>
                <p className="mt-2 text-xs text-muted-foreground">
                  Vence el {formatDate(item.venceEn)}
                </p>
                {item.estado === "valido" && (
                  <div className="mt-5">
                    <Go to="/app/reservations/new">Usar en una reserva</Go>
                  </div>
                )}
              </Panel>
            ))}
          </div>
        ) : (
          <Panel>
            <EmptyState
              description="Los tickets generados por cancelaciones aparecerán aquí."
              title="No tenés tickets disponibles"
            />
          </Panel>
        )
      ) : params.get("state") === "empty" || !filtered.length ? (
        <Panel>
          <EmptyState
            action={<Go to="/app/services">Explorar espacios</Go>}
            description="Cuando reserves un espacio, vas a poder consultar aquí sus horarios y códigos."
            title="Todavía no tenés reservas"
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          {filtered.map((item) => (
            <Panel key={item.id}>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
                <div className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-primary">
                  <CalendarDays aria-hidden="true" className="size-6" />
                </div>
                <div className="flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <h2 className="font-semibold">{item.spaceName}</h2>
                    <StateBadge state={item.estadoLabel} />
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {formatDate(item.fecha)} · {item.inicio} a {item.fin}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {item.codigo}
                  </p>
                </div>
                <div className="flex items-center justify-between gap-5 sm:flex-col sm:items-end">
                  <p className="font-semibold tabular-nums">
                    {money(item.precio.total)}
                  </p>
                  <TextLink to={"/app/reservations/" + item.id}>
                    Ver detalle
                  </TextLink>
                </div>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  )
}

export function MemberReservationDetailPage() {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const [cancelOpen, setCancelOpen] = useState(false)
  const reservation =
    getReservationById(id ?? "") ??
    allReservations.find((item) => item.codigo === id)
  const cancelled =
    params.get("state") === "cancelled" || reservation?.estado === "cancelada"

  if (!reservation) {
    return (
      <Result
        description="La reserva puede haber sido eliminada o no pertenece a tu cuenta."
        error
        title="Reserva no encontrada"
      >
        <Go to="/app/reservations">Ver mis reservas</Go>
      </Result>
    )
  }

  const pending = reservation.estado === "pendiente_pago"
  const confirmed =
    !cancelled &&
    (reservation.estado === "confirmada" || reservation.estado === "cumplida")

  return (
    <div>
      <MemberHeading
        action={
          <StateBadge
            state={cancelled ? "Cancelada" : reservation.estadoLabel}
          />
        }
        back="/app/reservations"
        description={reservation.codigo}
        title={cancelled ? "Reserva cancelada" : "Detalle de reserva"}
      />
      {cancelled && (
        <div className="mb-5">
          <Notice title="La cancelación quedó registrada">
            El horario fue liberado. Si corresponde, se generó un ticket para
            una próxima reserva. No se realiza una devolución automática.
          </Notice>
        </div>
      )}
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_320px]">
        <Panel title={reservation.spaceName}>
          <ImagePlaceholder
            className="mb-6 h-52 rounded-lg"
            label={reservation.spaceName}
          />
          <InfoRows
            rows={[
              ["Fecha", formatDate(reservation.fecha)],
              ["Horario", reservation.inicio + " a " + reservation.fin],
              ["Titular", reservation.ownerName],
              ["Importe total", money(reservation.precio.total)],
              ["Pago", <StateBadge state={reservation.pagoEstadoLabel} />],
              ["Registrada el", formatDate(reservation.creadaEn)],
            ]}
          />
          <div className="mt-6 flex flex-wrap gap-3">
            {pending && (
              <Go to="/app/reservations/new?step=4">Continuar al pago</Go>
            )}
            {(confirmed || pending) && (
              <Button onClick={() => setCancelOpen(true)} variant="destructive">
                Cancelar reserva
              </Button>
            )}
            {reservation.estado === "cumplida" && (
              <Go secondary to="/app/surveys">
                Evaluar mi visita
              </Go>
            )}
          </div>
        </Panel>
        {confirmed ? (
          <Panel title="Tu código de ingreso">
            <div className="bg-white p-4">
              <QRCode
                className="mx-auto"
                size={200}
                title="QR de la reserva"
                value={
                  reservation.qrPayload ??
                  "SERA-DEMO:RESERVATION:" + reservation.codigo
                }
              />
            </div>
            <p className="mt-4 text-center text-xl font-bold tracking-wider">
              {reservation.codigo}
            </p>
            <p className="mt-4 text-center text-sm leading-6 text-muted-foreground">
              Presentá el QR o indicá este código al personal de acceso.
            </p>
          </Panel>
        ) : cancelled ? (
          <Panel title="Ticket de reserva">
            <Ticket aria-hidden="true" className="mb-4 size-8 text-primary" />
            <p className="font-semibold">Ticket de cancelación</p>
            <p className="my-3 text-2xl font-bold">
              {money(reservation.precio.total)}
            </p>
            <p className="text-sm text-muted-foreground">
              Consultá tus tickets disponibles para usar este saldo en otra
              reserva.
            </p>
            <div className="mt-5">
              <Go secondary to="/app/reservations?tab=tickets">
                Ver mis tickets
              </Go>
            </div>
          </Panel>
        ) : (
          <Panel title="Código de ingreso">
            <CircleAlert
              aria-hidden="true"
              className="mb-4 size-7 text-muted-foreground"
            />
            <p className="text-sm leading-6 text-muted-foreground">
              {pending
                ? "El QR estará disponible cuando se apruebe el pago completo de tu reserva."
                : "Esta reserva ya terminó. Su código de ingreso no está vigente."}
            </p>
          </Panel>
        )}
      </div>
      <AlertDialog onOpenChange={setCancelOpen} open={cancelOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Cancelar esta reserva?</AlertDialogTitle>
            <AlertDialogDescription>
              Se liberará {reservation.spaceName} el{" "}
              {formatDate(reservation.fecha)}, de {reservation.inicio} a{" "}
              {reservation.fin}. La cancelación no genera un reintegro
              automático.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Conservar reserva</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                setCancelOpen(false)
                setParams({ state: "cancelled" })
              }}
              variant="destructive"
            >
              Confirmar cancelación
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
