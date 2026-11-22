import { useState } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Button } from "@/components/ui/button"
import { PageHeader, SectionCard } from "@/components/shared"
import {
  ErrorMessage,
  Field,
  Note,
  QueryState,
  SelectField,
} from "@/components/shared/real-data"
import {
  TimeSlotPicker,
  type SlotSelection,
} from "@/components/booking/time-slot-picker"
import { useDateChangeCheck } from "@/components/booking/use-date-change-check"
import { endTime, hoursLabel, selectionFits } from "@/lib/booking-times"
import { createRequestKey } from "@/lib/request-key"
import { calendarApi, spacesApi } from "@/lib/spaces-api"
import { label } from "@/lib/members-api"
import { formatCurrency, formatDate } from "@/lib/format"
import { reservationsApi, type BookingInput } from "@/lib/reservations-api"
import type { PaymentMethod } from "@/lib/payments-api"
import { ApiError } from "@/lib/users-api"

/** Duración pedida desde la consulta del espacio; si falta o no es válida, una hora. */
function requestedHours(value: string | null) {
  const hours = Number(value)
  return Number.isInteger(hours) && hours >= 1 && hours <= 24 ? hours : 1
}

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
  const [selection, setSelection] = useState<SlotSelection>(() => ({
    start: params.get("start") ?? "",
    hours: requestedHours(params.get("hours")),
  }))
  const { start, hours } = selection
  // Si la consulta del espacio ya trajo el horario, el formulario arranca con todo elegido.
  const [editingSlot, setEditingSlot] = useState(
    () => !(params.get("space") && params.get("date") && params.get("start"))
  )
  const [dateNotice, setDateNotice] = useState("")
  const checkDate = useDateChangeCheck()
  const [people, setPeople] = useState(1)
  const [ticketId, setTicketId] = useState(params.get("ticket") ?? "")
  const [method, setMethod] = useState<PaymentMethod>("MERCADO_PAGO")
  const [key, setKey] = useState(() => createRequestKey())
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
  const slots = calendar.data?.franjas ?? []
  const fits = selectionFits(slots, start, hours)
  const end = endTime(start, hours)
  // Un horario que llegó elegido pero ya no está libre vuelve a mostrar el selector.
  if (!editingSlot && calendar.data && !calendar.isFetching && !fits)
    setEditingSlot(true)
  const showEditor = editingSlot
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
  const valid = !!space && fits && people > 0 && people <= space.capacidad
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
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409)
        void calendar.refetch()
    },
  })
  function edit() {
    setReview(false)
    setKey(createRequestKey())
    create.reset()
  }
  async function changeDate(next: string) {
    setDate(next)
    setDateNotice("")
    edit()
    const result = await checkDate(spaceId, next, selection)
    if (result === "clear") {
      setSelection((current) => ({ ...current, start: "" }))
      setDateNotice(
        `El horario de ${start} a ${end} no está libre el ${formatDate(next)}. Elegí otro horario.`
      )
    } else if (result === "keep" && start) {
      setDateNotice(
        `El horario de ${start} a ${end} sigue libre el ${formatDate(next)}.`
      )
    }
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
              className="rounded-full border px-4 py-2 text-muted-foreground aria-[current=step]:border-primary aria-[current=step]:bg-primary/5 aria-[current=step]:font-semibold aria-[current=step]:text-primary"
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
                if (
                  !valid ||
                  !quote.data ||
                  quote.isFetching ||
                  quote.isError ||
                  create.isPending
                )
                  return
                if (review) create.mutate()
                else setReview(true)
              }}
            >
              {review ? (
                <dl className="grid gap-5 text-sm sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <dt className="text-muted-foreground">Espacio</dt>
                    <dd className="mt-1 text-lg font-semibold">
                      {space?.nombre}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Fecha</dt>
                    <dd className="mt-1 font-medium">{formatDate(date)}</dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Horario</dt>
                    <dd className="mt-1 font-medium tabular-nums">
                      {start} a {end} · {hoursLabel(hours)}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Personas</dt>
                    <dd className="mt-1 font-medium">{people}</dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Medio de pago</dt>
                    <dd className="mt-1 font-medium">
                      {method === "MERCADO_PAGO"
                        ? "Mercado Pago"
                        : "Efectivo en administración"}
                    </dd>
                  </div>
                </dl>
              ) : (
                <fieldset
                  disabled={review || create.isPending}
                  className="grid gap-5 sm:grid-cols-2 [&>div:first-child]:sm:col-span-2"
                >
                  {showEditor ? (
                    <>
                      <SelectField
                        label="Espacio"
                        value={spaceId}
                        required
                        onChange={(event) => {
                          setSpaceId(event.target.value)
                          setSelection((current) => ({ ...current, start: "" }))
                          setDateNotice("")
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
                      <Field
                        label="Fecha"
                        type="date"
                        min={today()}
                        required
                        value={date}
                        onChange={(event) =>
                          void changeDate(event.target.value)
                        }
                      />
                      {dateNotice && (
                        <p
                          role="status"
                          className="rounded-lg bg-slate-50 p-3 text-xs leading-relaxed text-slate-700 sm:col-span-2"
                        >
                          {dateNotice}
                        </p>
                      )}
                      {!!spaceId && (
                        <div className="sm:col-span-2">
                          <QueryState
                            pending={calendar.isPending}
                            error={calendar.error}
                            retry={calendar.refetch}
                          >
                            <TimeSlotPicker
                              slots={slots}
                              value={selection}
                              onChange={(next) => {
                                setSelection(next)
                                setDateNotice("")
                                edit()
                              }}
                            />
                          </QueryState>
                        </div>
                      )}
                    </>
                  ) : (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-slate-50 p-4 sm:col-span-2">
                      <div>
                        <p className="font-semibold">{space?.nombre}</p>
                        <p className="mt-1 text-sm tabular-nums">
                          {formatDate(date)} · {start} a {end} ·{" "}
                          {hoursLabel(hours)}
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setEditingSlot(true)}
                      >
                        Cambiar horario
                      </Button>
                    </div>
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
                  {(ticketId ||
                    tickets.data?.some((r) => r.saldoTicket > 0)) && (
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
                  )}
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
              )}
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
          <SectionCard title="Resumen" className="lg:sticky lg:top-6">
            <p className="font-semibold">
              {space?.nombre ?? "Elegí un espacio"}
            </p>
            {start && date && (
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
                <dl className="mt-5 space-y-3 text-sm [&_dd]:shrink-0 [&_dd]:text-right [&_dd]:font-medium [&_dd]:tabular-nums [&>div]:flex [&>div]:justify-between [&>div]:gap-4">
                  <div>
                    <dt>
                      Tarifa por hora · {label(quote.data?.relacionAplicada)}
                    </dt>
                    <dd>{formatCurrency(quote.data?.tarifaHora ?? 0)}</dd>
                  </div>
                  <div>
                    <dt>Total por {hoursLabel(hours)}</dt>
                    <dd>{formatCurrency(quote.data?.total ?? 0)}</dd>
                  </div>
                  {quote.data?.creditoAplicado ? (
                    <div>
                      <dt>Ticket aplicado</dt>
                      <dd>
                        − {formatCurrency(quote.data?.creditoAplicado ?? 0)}
                      </dd>
                    </div>
                  ) : null}
                  <div className="border-t pt-3 text-lg font-semibold">
                    <dt>A pagar</dt>
                    <dd>{formatCurrency(quote.data?.aPagar ?? 0)}</dd>
                  </div>
                </dl>
              </QueryState>
            )}
            <Note>
              Las reservas se hacen con al menos 40 minutos de anticipación. El
              horario se retiene al crear la reserva. Tenés hasta una hora para
              pagar, o hasta el inicio si ocurre antes. Podés cancelar antes del
              inicio; lo abonado queda como ticket.
            </Note>
          </SectionCard>
        </div>
      </QueryState>
    </>
  )
}
