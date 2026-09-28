import { useState } from "react"
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import QRCode from "react-qr-code"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  StatusBadge,
  ConfirmationDialog,
} from "@/components/shared"
import {
  ErrorMessage,
  Field,
  Note,
  QueryState,
  SelectField,
} from "@/components/shared/real-data"
import { calendarApi, spacesApi } from "@/lib/spaces-api"
import { label } from "@/lib/members-api"
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format"
import {
  reservationsApi,
  reservationState,
  type BookingInput,
  type Reservation,
} from "@/lib/reservations-api"
import type { PaymentMethod } from "@/lib/payments-api"

function today() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date())
}

export function BookingPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [spaceId, setSpaceId] = useState(params.get("space") ?? "")
  const [date, setDate] = useState(params.get("date") ?? today())
  const [start, setStart] = useState("")
  const [hours, setHours] = useState(1)
  const [people, setPeople] = useState(1)
  const [ticketId, setTicketId] = useState(params.get("ticket") ?? "")
  const [method, setMethod] = useState<PaymentMethod>("MERCADO_PAGO")
  const [key, setKey] = useState(() => crypto.randomUUID())
  const [review, setReview] = useState(false)
  const spaces = useQuery({
    queryKey: ["spaces"],
    queryFn: ({ signal }) => spacesApi.list("", signal),
  })
  const tickets = useQuery({
    queryKey: ["reservations", "mine"],
    queryFn: ({ signal }) => reservationsApi.list(false, signal),
  })
  const calendar = useQuery({
    queryKey: ["calendar", spaceId, date],
    queryFn: ({ signal }) => calendarApi.get(spaceId, date, signal),
    enabled: !!spaceId && !!date,
  })
  const space = spaces.data?.find((s) => s.id === spaceId)
  const starts: string[] = []
  for (const slot of calendar.data?.franjas ?? []) {
    const [h, m, seconds = 0] = slot.desde.split(":").map(Number)
    const [endH, endM] = slot.hasta.split(":").map(Number)
    for (
      let minute = Math.ceil((h * 60 + m + seconds / 60) / 30) * 30;
      minute + hours * 60 <= endH * 60 + endM;
      minute += 30
    ) {
      starts.push(
        `${String(Math.floor(minute / 60)).padStart(2, "0")}:${String(minute % 60).padStart(2, "0")}`
      )
    }
  }
  const end = start
    ? `${String(Number(start.slice(0, 2)) + hours).padStart(2, "0")}:${start.slice(3)}`
    : ""
  const input: BookingInput = {
    espacioId: spaceId,
    fecha: date,
    desde: start,
    hasta: end,
    personas: people,
    ticketId: ticketId || null,
    medioPago: method,
    claveSolicitud: key,
  }
  const valid =
    !!space && starts.includes(start) && people > 0 && people <= space.capacidad
  const quote = useQuery({
    queryKey: ["reservation-quote", input],
    queryFn: ({ signal }) => reservationsApi.quote(input, signal),
    enabled: valid,
    retry: false,
  })
  const create = useMutation({
    mutationFn: () => reservationsApi.create(input),
    onSuccess: async (reservation) => {
      await Promise.all(
        ["reservations", "calendar", "payments"].map((name) =>
          client.invalidateQueries({ queryKey: [name] })
        )
      )
      navigate(`/app/reservations/${reservation.id}`)
    },
    onError: () => {
      void calendar.refetch()
    },
  })
  function edit() {
    setReview(false)
    setKey(crypto.randomUUID())
    create.reset()
  }
  return (
    <>
      <PageHeader
        title="Reservar un espacio"
        description="Elegí el horario, revisá el importe y completá el pago."
      />
      <ol
        className="mb-6 flex flex-wrap gap-3 text-sm"
        aria-label="Pasos de la reserva"
      >
        {["Espacio", "Fecha y horario", "Confirmación", "Pago"].map(
          (step, i) => (
            <li
              key={step}
              className="rounded-full border px-4 py-2"
              aria-current={
                i === (review ? 2 : spaceId ? 1 : 0) ? "step" : undefined
              }
            >
              {i + 1}. {step}
            </li>
          )
        )}
      </ol>
      <QueryState
        pending={spaces.isPending || tickets.isPending}
        error={spaces.error ?? tickets.error}
        retry={() => {
          void spaces.refetch()
          void tickets.refetch()
        }}
      >
        <div className="grid items-start gap-6 lg:grid-cols-[1fr_320px]">
          <SectionCard
            title={review ? "Revisá tu reserva" : "Espacio y horario"}
          >
            <form
              className="space-y-5"
              onSubmit={(event) => {
                event.preventDefault()
                if (review) create.mutate()
                else setReview(true)
              }}
            >
              <fieldset
                disabled={review || create.isPending}
                className="space-y-5"
              >
                <SelectField
                  label="Espacio"
                  value={spaceId}
                  required
                  onChange={(event) => {
                    setSpaceId(event.target.value)
                    setStart("")
                    edit()
                  }}
                >
                  <option value="">Elegí un espacio</option>
                  {spaces.data
                    ?.filter((s) => s.estado === "HABILITADO")
                    .map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.nombre}
                      </option>
                    ))}
                </SelectField>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field
                    label="Fecha"
                    type="date"
                    min={today()}
                    required
                    value={date}
                    onChange={(event) => {
                      setDate(event.target.value)
                      setStart("")
                      edit()
                    }}
                  />
                  <SelectField
                    label="Duración"
                    value={hours}
                    onChange={(event) => {
                      setHours(Number(event.target.value))
                      setStart("")
                      edit()
                    }}
                  >
                    {[1, 2, 3, 4].map((n) => (
                      <option key={n} value={n}>
                        {n} {n === 1 ? "hora" : "horas"}
                      </option>
                    ))}
                  </SelectField>
                </div>
                {!!spaceId && (
                  <QueryState
                    pending={calendar.isPending}
                    error={calendar.error}
                    retry={calendar.refetch}
                  >
                    <SelectField
                      label="Horario disponible"
                      required
                      value={start}
                      onChange={(event) => {
                        setStart(event.target.value)
                        edit()
                      }}
                    >
                      <option value="">Elegí un horario</option>
                      {starts.map((time) => (
                        <option key={time} value={time}>
                          {time}
                        </option>
                      ))}
                    </SelectField>
                    {starts.length === 0 && (
                      <p className="mt-3 text-sm">
                        No quedan horarios con esa duración. Probá otra fecha o
                        duración.
                      </p>
                    )}
                  </QueryState>
                )}
                <Field
                  label="Cantidad de personas"
                  type="number"
                  min={1}
                  max={space?.capacidad ?? 1}
                  required
                  value={people}
                  onChange={(event) => {
                    setPeople(Number(event.target.value))
                    edit()
                  }}
                />
                <SelectField
                  label="Ticket de una cancelación"
                  value={ticketId}
                  onChange={(event) => {
                    setTicketId(event.target.value)
                    edit()
                  }}
                >
                  <option value="">Sin ticket</option>
                  {tickets.data
                    ?.filter((r) => r.saldoTicket > 0)
                    .map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.espacioNombre} · {formatCurrency(r.saldoTicket)}
                      </option>
                    ))}
                </SelectField>
                <SelectField
                  label="Medio de pago"
                  value={method}
                  onChange={(event) => {
                    setMethod(event.target.value as PaymentMethod)
                    edit()
                  }}
                >
                  <option value="MERCADO_PAGO">Mercado Pago</option>
                  <option value="EFECTIVO">Efectivo en administración</option>
                </SelectField>
              </fieldset>
              <ErrorMessage error={create.error} />
              <div className="flex flex-wrap gap-3">
                {review && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={create.isPending}
                    onClick={edit}
                  >
                    Modificar
                  </Button>
                )}
                <Button
                  type="submit"
                  disabled={
                    !valid ||
                    !quote.data ||
                    quote.isFetching ||
                    quote.isError ||
                    create.isPending
                  }
                >
                  {create.isPending
                    ? "Registrando…"
                    : review
                      ? "Crear reserva"
                      : "Revisar reserva"}
                </Button>
              </div>
            </form>
          </SectionCard>
          <SectionCard title="Resumen">
            <p className="font-semibold">
              {space?.nombre ?? "Elegí un espacio"}
            </p>
            {start && (
              <p className="mt-2 text-sm">
                {formatDate(date)} · {start} a {end}
              </p>
            )}
            {valid && (
              <QueryState
                pending={quote.isPending}
                error={quote.error}
                retry={quote.refetch}
              >
                <dl className="mt-5 space-y-3 text-sm">
                  <div>
                    <dt>
                      Tarifa por hora · {label(quote.data?.relacionAplicada)}
                    </dt>
                    <dd>{formatCurrency(quote.data?.tarifaHora ?? 0)}</dd>
                  </div>
                  <div>
                    <dt>
                      Total por {hours} {hours === 1 ? "hora" : "horas"}
                    </dt>
                    <dd>{formatCurrency(quote.data?.total ?? 0)}</dd>
                  </div>
                  <div>
                    <dt>Ticket aplicado</dt>
                    <dd>
                      − {formatCurrency(quote.data?.creditoAplicado ?? 0)}
                    </dd>
                  </div>
                  <div className="border-t pt-3 text-lg font-semibold">
                    <dt>A pagar</dt>
                    <dd>{formatCurrency(quote.data?.aPagar ?? 0)}</dd>
                  </div>
                </dl>
              </QueryState>
            )}
            <Note>
              El horario se retiene al crear la reserva. Tenés hasta una hora
              para pagar, o hasta el inicio si ocurre antes. Podés cancelar
              antes del inicio; lo abonado queda como ticket.
            </Note>
          </SectionCard>
        </div>
      </QueryState>
    </>
  )
}

export function MemberReservationsPage() {
  const reservations = useQuery({
    queryKey: ["reservations", "mine"],
    queryFn: ({ signal }) => reservationsApi.list(false, signal),
    refetchInterval: 30000,
  })
  const [params] = useSearchParams()
  const [filter, setFilter] = useState(
    params.get("tab") === "tickets" ? "TICKETS" : "TODAS"
  )
  const filtered =
    reservations.data?.filter(
      (r) =>
        filter === "TODAS" ||
        (filter === "TICKETS" ? r.saldoTicket > 0 : r.estado === filter)
    ) ?? []
  return (
    <>
      <PageHeader
        title="Mis reservas"
        description="Horarios, pagos y tickets disponibles."
        actions={
          <Button render={<Link to="/app/reservations/new" />}>
            Nueva reserva
          </Button>
        }
      />
      <div className="mb-5 max-w-xs">
        <SelectField
          label="Mostrar"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option value="TODAS">Todas las reservas</option>
          <option value="TICKETS">Tickets con saldo</option>
          {Object.entries(reservationState).map(([value, text]) => (
            <option key={value} value={value}>
              {text}
            </option>
          ))}
        </SelectField>
      </div>
      <QueryState
        pending={reservations.isPending}
        error={reservations.error}
        retry={reservations.refetch}
      >
        <div className="space-y-4">
          {filtered.map((r) => (
            <SectionCard
              key={r.id}
              title={r.espacioNombre}
              action={<StatusBadge>{reservationState[r.estado]}</StatusBadge>}
            >
              <p className="text-sm">
                {formatDate(r.fecha)} · {r.desde.slice(0, 5)} a{" "}
                {r.hasta.slice(0, 5)}
              </p>
              <p className="mt-2 font-semibold">{formatCurrency(r.total)}</p>
              {r.saldoTicket > 0 && (
                <p className="mt-2 text-sm">
                  Ticket disponible: {formatCurrency(r.saldoTicket)}
                </p>
              )}
              <div className="mt-4 flex flex-wrap gap-3">
                <Button
                  variant="outline"
                  render={<Link to={`/app/reservations/${r.id}`} />}
                >
                  Ver detalle
                </Button>
                {r.saldoTicket > 0 && (
                  <Button
                    render={
                      <Link to={`/app/reservations/new?ticket=${r.id}`} />
                    }
                  >
                    Usar ticket
                  </Button>
                )}
              </div>
            </SectionCard>
          ))}
        </div>
        {filtered.length === 0 && (
          <SectionCard title="No hay reservas para mostrar">
            <p className="text-sm">
              Podés elegir un espacio y hacer tu primera reserva.
            </p>
          </SectionCard>
        )}
      </QueryState>
    </>
  )
}

export function MemberReservationDetailPage() {
  const { id = "" } = useParams()
  const reservation = useQuery({
    queryKey: ["reservations", id],
    queryFn: ({ signal }) => reservationsApi.get(id, signal),
    refetchInterval: 10000,
  })
  return (
    <>
      <PageHeader
        title="Detalle de reserva"
        actions={
          <Button variant="outline" render={<Link to="/app/reservations" />}>
            Mis reservas
          </Button>
        }
      />
      <QueryState
        pending={reservation.isPending}
        error={reservation.error}
        retry={reservation.refetch}
      >
        {reservation.data && (
          <ReservationDetails reservation={reservation.data} />
        )}
      </QueryState>
    </>
  )
}

export function ReservationDetails({
  reservation: r,
  admin = false,
}: {
  reservation: Reservation
  admin?: boolean
}) {
  const client = useQueryClient()
  const [params] = useSearchParams()
  const [cancelOpen, setCancelOpen] = useState(false)
  const [paymentId, setPaymentId] = useState(params.get("payment_id") ?? "")
  async function refresh() {
    await Promise.all(
      ["reservations", "calendar", "payments"].map((name) =>
        client.invalidateQueries({ queryKey: [name] })
      )
    )
  }
  const cancel = useMutation({
    mutationFn: () => reservationsApi.cancel(r.id),
    onSuccess: async () => {
      setCancelOpen(false)
      await refresh()
    },
  })
  const checkout = useMutation({
    mutationFn: () => reservationsApi.checkout(r.id),
    onSuccess: (result) => {
      if (result.checkoutUrl) window.location.assign(result.checkoutUrl)
    },
  })
  const verify = useMutation({
    mutationFn: () => reservationsApi.verify(r.id, Number(paymentId)),
    onSuccess: refresh,
  })
  const future = r.cancelable
  const pending = r.estado === "PENDIENTE_PAGO"
  return (
    <div
      className={
        admin ? "space-y-6" : "grid items-start gap-6 lg:grid-cols-[1fr_300px]"
      }
    >
      <SectionCard
        title={r.espacioNombre}
        action={<StatusBadge>{reservationState[r.estado]}</StatusBadge>}
      >
        <dl className="grid gap-5 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-muted-foreground">Titular</dt>
            <dd>{r.titular}</dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Fecha y horario</dt>
            <dd>
              {formatDate(r.fecha)} · {r.desde.slice(0, 5)} a{" "}
              {r.hasta.slice(0, 5)}
            </dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Personas</dt>
            <dd>{r.personas}</dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Total</dt>
            <dd>{formatCurrency(r.total)}</dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Ticket aplicado</dt>
            <dd>{formatCurrency(r.creditoAplicado)}</dd>
          </div>
          <div>
            <dt className="text-muted-foreground">Pago</dt>
            <dd>
              {r.estadoPago ? label(r.estadoPago) : "Sin importe pendiente"}{" "}
              {r.medioPago
                ? `· ${r.medioPago === "EFECTIVO" ? "Efectivo" : "Mercado Pago"}`
                : ""}
            </dd>
          </div>
        </dl>
        {pending && (
          <Note>
            Completá {formatCurrency(r.total - r.creditoAplicado)} antes del{" "}
            {formatDateTime(r.venceEn)}.{" "}
            {r.medioPago === "EFECTIVO"
              ? "Administración debe registrar el pago para confirmar la reserva."
              : "La reserva se confirma cuando SERA verifica el pago con Mercado Pago."}
          </Note>
        )}
        {r.requiereRevision && (
          <Note>
            El cobro requiere revisión de administración. No se recupera el
            horario de una reserva cancelada o vencida.
          </Note>
        )}
        <ErrorMessage error={checkout.error ?? cancel.error ?? verify.error} />
        <div className="mt-6 flex flex-wrap gap-3">
          {pending && r.medioPago === "MERCADO_PAGO" && !admin && (
            <Button
              disabled={checkout.isPending}
              onClick={() => checkout.mutate()}
            >
              {checkout.isPending ? "Abriendo pago…" : "Pagar con Mercado Pago"}
            </Button>
          )}
          {future && (pending || r.estado === "CONFIRMADA") && (
            <Button variant="destructive" onClick={() => setCancelOpen(true)}>
              Cancelar reserva
            </Button>
          )}
        </div>
        {r.medioPago === "MERCADO_PAGO" && r.estadoPago !== "APROBADO" && (
          <form
            className="mt-6 space-y-3 border-t pt-5"
            onSubmit={(e) => {
              e.preventDefault()
              verify.mutate()
            }}
          >
            <Field
              label="Número de pago de Mercado Pago"
              type="number"
              min={1}
              required
              value={paymentId}
              onChange={(e) => setPaymentId(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Si ya pagaste y el estado no cambió, verificá el número de tu
              comprobante.
            </p>
            <Button
              type="submit"
              variant="outline"
              disabled={verify.isPending || !paymentId}
            >
              Verificar pago
            </Button>
          </form>
        )}
      </SectionCard>
      <SectionCard
        title={
          r.codigo && r.estado === "CONFIRMADA"
            ? "Código de reserva"
            : "Estado de la reserva"
        }
      >
        {r.codigo && r.estado === "CONFIRMADA" ? (
          <>
            <div className="rounded-lg bg-white p-4">
              <QRCode
                title="QR de la reserva"
                value={r.codigo}
                className="h-auto w-full"
              />
            </div>
            <p className="mt-4 font-mono text-sm break-all">{r.codigo}</p>
          </>
        ) : (
          <p className="text-sm">
            {pending
              ? "El código y el QR aparecen al confirmar el pago completo."
              : "Esta reserva no tiene un código de ingreso vigente."}
          </p>
        )}
        {r.saldoTicket > 0 && (
          <div className="mt-5 border-t pt-5">
            <p className="text-sm">Saldo de ticket</p>
            <p className="my-3 text-xl font-semibold">
              {formatCurrency(r.saldoTicket)}
            </p>
            {!admin && (
              <Button
                render={<Link to={`/app/reservations/new?ticket=${r.id}`} />}
              >
                Usar ticket
              </Button>
            )}
          </div>
        )}
      </SectionCard>
      <ConfirmationDialog
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        title="Cancelar reserva"
        description="Se libera el horario. Si ya estaba confirmada, el importe queda como ticket para otra reserva. No hay devolución automática de dinero."
        confirmLabel={
          cancel.isPending ? "Cancelando…" : "Confirmar cancelación"
        }
        destructive
        onConfirm={() => {
          if (!cancel.isPending) cancel.mutate()
        }}
      />
    </div>
  )
}
