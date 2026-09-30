import { useState, type FormEvent } from "react"
import { Link, useParams } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import {
  Plus,
  Landmark,
  Users,
  ArrowLeft,
  ArrowRight,
  Clock,
} from "lucide-react"
import { availableStarts } from "@/lib/booking-times"
import {
  spacesApi,
  calendarApi,
  type SpaceResponse,
  type SpaceInput,
  type Weekday,
  type Availability,
} from "@/lib/spaces-api"
import { relationships, label } from "@/lib/members-api"
import { formatCurrency } from "@/lib/format"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  DetailSheet,
  StatusBadge,
  ConfirmationDialog,
} from "@/components/shared"
import {
  Field,
  SelectField,
  ErrorMessage,
  QueryState,
} from "@/components/shared/real-data"
const days: Weekday[] = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
  "DOMINGO",
]
function today() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`
}
function SpaceImage({ space }: { space: SpaceResponse }) {
  return space.rutaImagen ? (
    <img
      src={space.rutaImagen}
      alt={space.nombre}
      loading="lazy"
      className="aspect-[16/9] w-full rounded-lg bg-slate-100 object-cover"
    />
  ) : (
    <div className="flex aspect-[16/9] items-center justify-center rounded-lg bg-blue-50 text-blue-300">
      <Landmark className="size-16" strokeWidth={1} />
    </div>
  )
}
function SpaceForm({
  space,
  onSave,
  pending,
  error,
}: {
  space?: SpaceResponse
  onSave: (data: SpaceInput) => void
  pending: boolean
  error: Error | null
}) {
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    onSave({
      nombre: String(f.get("nombre")),
      descripcion: String(f.get("descripcion")) || null,
      capacidad: Number(f.get("capacidad")),
      tarifaHora: Number(f.get("tarifaHora")),
      tipo: String(f.get("tipo")),
      rutaImagen: String(f.get("rutaImagen")) || null,
    })
  }
  return (
    <form onSubmit={submit} className="space-y-5">
      <Field
        label="Nombre"
        name="nombre"
        required
        maxLength={100}
        defaultValue={space?.nombre}
      />
      <Field
        label="Descripción"
        name="descripcion"
        maxLength={500}
        defaultValue={space?.descripcion ?? ""}
      />
      <Field
        label="Tipo de espacio"
        name="tipo"
        required
        maxLength={100}
        placeholder="Cancha, quincho, pileta…"
        defaultValue={space?.tipo}
      />
      <div className="grid grid-cols-2 gap-4">
        <Field
          label="Capacidad"
          name="capacidad"
          type="number"
          min="1"
          max="100000"
          required
          defaultValue={space?.capacidad ?? 1}
        />
        <Field
          label="Tarifa general por hora"
          name="tarifaHora"
          type="number"
          min="0"
          step="0.01"
          max="9999999999.99"
          required
          defaultValue={space?.tarifaHora ?? 0}
        />
      </div>
      <Field
        label="URL o ruta de imagen"
        name="rutaImagen"
        maxLength={255}
        placeholder="https://… o /poli1.jpg"
        defaultValue={space?.rutaImagen ?? ""}
      />
      <ErrorMessage error={error} />
      <Button type="submit" disabled={pending}>
        {pending ? "Guardando…" : "Guardar espacio"}
      </Button>
    </form>
  )
}
export function SpacesList({ admin = false }: { admin?: boolean }) {
  const [search, setSearch] = useState("")
  const [state, setState] = useState("")
  const [open, setOpen] = useState(false)
  const client = useQueryClient()
  const spaces = useQuery({
    queryKey: ["spaces", search],
    queryFn: ({ signal }) => spacesApi.list(search, signal),
  })
  const save = useMutation({
    mutationFn: (data: SpaceInput) => spacesApi.save(data),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["spaces"] })
      setOpen(false)
    },
  })
  const filtered =
    spaces.data?.filter((s) => !state || s.estado === state) ?? []
  return (
    <>
      <PageHeader
        title={admin ? "Espacios y servicios" : "Encontrá tu próximo espacio"}
        description={
          admin
            ? "Administrá espacios, tarifas y horarios del polideportivo."
            : "Explorá los espacios y consultá los horarios disponibles."
        }
        actions={
          admin ? (
            <Button
              onClick={() => {
                save.reset()
                setOpen(true)
              }}
            >
              <Plus />
              Registrar espacio
            </Button>
          ) : undefined
        }
      />
      <div className="mb-6 grid max-w-2xl gap-4 sm:grid-cols-2">
        <Field
          label="Buscar espacio"
          placeholder="Nombre del espacio"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <SelectField
          label="Estado"
          value={state}
          onChange={(e) => setState(e.target.value)}
        >
          <option value="">Todos los estados</option>
          {["HABILITADO", "MANTENIMIENTO", "INUTILIZABLE", "EN_USO"].map(
            (s) => (
              <option key={s} value={s}>
                {label(s)}
              </option>
            )
          )}
        </SelectField>
      </div>
      <QueryState
        pending={spaces.isPending}
        error={spaces.error}
        retry={spaces.refetch}
      >
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((s) => (
            <SectionCard key={s.id}>
              <SpaceImage space={s} />
              <div className="my-4 flex items-start justify-between gap-3">
                <div>
                  <p className="mb-1 text-xs text-muted-foreground">{s.tipo}</p>
                  <h2 className="text-lg font-bold">{s.nombre}</h2>
                </div>
                <StatusBadge
                  tone={s.estado === "HABILITADO" ? "success" : "warning"}
                >
                  {label(s.estado)}
                </StatusBadge>
              </div>
              <p className="mb-4 line-clamp-2 min-h-10 text-sm text-muted-foreground">
                {s.descripcion ??
                  "Consultá horarios y tarifas de este espacio."}
              </p>
              <p className="mb-4 flex items-center gap-2 text-sm">
                <Users className="size-4" />
                {s.capacidad} personas
              </p>
              <Link
                to={`${admin ? "/admin/spaces" : "/app/services"}/${s.id}`}
                className="inline-flex min-h-10 items-center rounded-lg bg-primary px-4 text-sm font-semibold text-primary-foreground"
              >
                {admin ? "Administrar espacio" : "Consultar disponibilidad"}
              </Link>
            </SectionCard>
          ))}
        </div>
        {filtered.length === 0 && (
          <SectionCard>
            <p>No hay espacios para esta búsqueda.</p>
          </SectionCard>
        )}
      </QueryState>
      <DetailSheet
        open={open}
        onOpenChange={(v) => {
          if (!save.isPending) setOpen(v)
        }}
        title="Registrar espacio"
        description="Después de registrarlo podés configurar horarios, tarifas y bloqueos."
      >
        <SpaceForm
          onSave={(data) => save.mutate(data)}
          pending={save.isPending}
          error={save.error}
        />
      </DetailSheet>
    </>
  )
}
export function SpaceDetail({ admin = false }: { admin?: boolean }) {
  const { id = "" } = useParams()
  const client = useQueryClient()
  const [date, setDate] = useState(today)
  const [selectedStart, setSelectedStart] = useState("")
  const [editing, setEditing] = useState(false)
  const [notice, setNotice] = useState("")
  const [hoursNotice, setHoursNotice] = useState("")
  const [deleting, setDeleting] = useState<{
    kind: "schedule" | "block"
    id: string
  } | null>(null)
  const [schedule, setSchedule] = useState<Availability | null>(null)
  const space = useQuery({
    queryKey: ["space", id],
    queryFn: ({ signal }) => spacesApi.get(id, signal),
  })
  const calendar = useQuery({
    queryKey: ["calendar", id, date],
    queryFn: ({ signal }) => calendarApi.get(id, date, signal),
    enabled: !!date,
  })
  const blocks = useQuery({
    queryKey: ["blocks", id, date],
    queryFn: ({ signal }) => calendarApi.blocks(id, date, signal),
    enabled: admin && !!date,
  })
  async function refresh() {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["space", id] }),
      client.invalidateQueries({ queryKey: ["spaces"] }),
      client.invalidateQueries({ queryKey: ["calendar", id] }),
      client.invalidateQueries({ queryKey: ["blocks", id] }),
    ])
    setNotice("Los cambios se guardaron.")
  }
  const update = useMutation({
    mutationFn: (d: SpaceInput) => spacesApi.save(d, id),
    onSuccess: async () => {
      await refresh()
      setEditing(false)
    },
  })
  const state = useMutation({
    mutationFn: (value: SpaceResponse["estado"]) =>
      calendarApi.state(id, value),
    onSuccess: refresh,
  })
  const rates = useMutation({
    mutationFn: (data: SpaceResponse["tarifas"]) => calendarApi.rates(id, data),
    onSuccess: refresh,
  })
  const hours = useMutation({
    onMutate: () => {
      setNotice("")
      setHoursNotice("")
    },
    mutationFn: (f: FormData) =>
      schedule
        ? spacesApi.updateAvailability(id, schedule.id, {
            horaDesde: String(f.get("desde")),
            horaHasta: String(f.get("hasta")),
          })
        : spacesApi.createAvailability(id, {
            diaSemana: String(f.get("dia")) as Weekday,
            horaDesde: String(f.get("desde")),
            horaHasta: String(f.get("hasta")),
          }),
    onSuccess: async () => {
      await refresh()
      setHoursNotice(schedule ? "Horario actualizado." : "Horario agregado.")
      setSchedule(null)
    },
  })
  const block = useMutation({
    mutationFn: (f: FormData) =>
      calendarApi.block(id, {
        fecha: date,
        desde: String(f.get("desde")),
        hasta: String(f.get("hasta")),
        motivo: String(f.get("motivo")),
      }),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (v: { kind: "schedule" | "block"; id: string }) =>
      v.kind === "schedule"
        ? spacesApi.removeAvailability(id, v.id)
        : calendarApi.unblock(id, v.id),
    onSuccess: refresh,
  })
  const s = space.data
  const starts = availableStarts(calendar.data?.franjas ?? [], 1)
  const canReserve =
    !calendar.isFetching && !calendar.isError && starts.includes(selectedStart)
  return (
    <>
      <Link
        to={admin ? "/admin/spaces" : "/app/services"}
        className="mb-5 inline-flex items-center gap-2 text-sm text-muted-foreground"
      >
        <ArrowLeft className="size-4" />
        Volver a espacios
      </Link>
      <QueryState
        pending={space.isPending}
        error={space.error}
        retry={space.refetch}
      >
        {s && (
          <>
            <PageHeader
              title={s.nombre}
              description={s.descripcion ?? s.tipo}
              actions={
                admin ? (
                  <Button
                    variant="outline"
                    onClick={() => {
                      update.reset()
                      setEditing(true)
                    }}
                  >
                    Editar espacio
                  </Button>
                ) : undefined
              }
            />
            {notice && (
              <p role="status" className="mb-4 text-sm text-emerald-800">
                {notice}
              </p>
            )}
            <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)]">
              <SectionCard className={admin ? undefined : "order-2 lg:order-1"}>
                <SpaceImage space={s} />
                <div className="mt-5 flex justify-between gap-3">
                  <span className="text-sm">
                    {s.tipo} · {s.capacidad} personas
                  </span>
                  <StatusBadge
                    tone={s.estado === "HABILITADO" ? "success" : "warning"}
                  >
                    {label(s.estado)}
                  </StatusBadge>
                </div>
                {admin && (
                  <form
                    className="mt-5 space-y-3"
                    onSubmit={(e) => {
                      e.preventDefault()
                      state.mutate(
                        String(
                          new FormData(e.currentTarget).get("estado")
                        ) as SpaceResponse["estado"]
                      )
                    }}
                  >
                    <SelectField
                      label="Estado del espacio"
                      name="estado"
                      defaultValue={s.estado}
                    >
                      {[
                        "HABILITADO",
                        "MANTENIMIENTO",
                        "INUTILIZABLE",
                        "EN_USO",
                      ].map((v) => (
                        <option key={v} value={v}>
                          {label(v)}
                        </option>
                      ))}
                    </SelectField>
                    <ErrorMessage error={state.error} />
                    <Button
                      type="submit"
                      variant="outline"
                      disabled={state.isPending}
                    >
                      Guardar estado
                    </Button>
                  </form>
                )}
              </SectionCard>
              <SectionCard
                title="Disponibilidad"
                description={
                  admin
                    ? "Horarios libres, descontando reservas y bloqueos."
                    : "Elegí una fecha y un horario de inicio."
                }
                className={admin ? undefined : "order-1 lg:order-2"}
              >
                <div className="grid items-end gap-4 border-b pb-5 sm:grid-cols-[1fr_auto]">
                  <Field
                    label="Fecha de consulta"
                    type="date"
                    min={today()}
                    required
                    value={date}
                    onChange={(e) => {
                      setDate(e.target.value)
                      setSelectedStart("")
                    }}
                  />
                  {calendar.data && !calendar.isError && (
                    <div className="sm:text-right">
                      <p className="text-xl font-bold tabular-nums">
                        {formatCurrency(calendar.data.tarifaHora)}
                        <span className="ml-1 text-sm font-normal text-muted-foreground">
                          / hora
                        </span>
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        Tarifa{" "}
                        {label(calendar.data.relacionAplicada).toLowerCase()}
                      </p>
                    </div>
                  )}
                </div>
                <QueryState
                  pending={calendar.isPending}
                  error={calendar.error}
                  retry={calendar.refetch}
                >
                  {admin ? (
                    <div className="mt-5 divide-y">
                      {calendar.data?.franjas.map((f) => (
                        <div
                          key={f.desde}
                          className="flex items-center justify-between gap-3 py-3"
                        >
                          <p className="font-medium tabular-nums">
                            {f.desde.slice(0, 5)} a {f.hasta.slice(0, 5)}
                          </p>
                          <p className="text-xs text-emerald-800">Disponible</p>
                        </div>
                      ))}
                    </div>
                  ) : (
                    starts.length > 0 && (
                      <fieldset className="mt-5">
                        <legend className="mb-3 flex items-center gap-2 text-sm font-semibold">
                          <Clock className="size-4" aria-hidden="true" />
                          Horarios de inicio
                        </legend>
                        <p className="mb-4 text-xs text-muted-foreground">
                          Para una reserva de 1 hora. Podés cambiar la duración
                          en el siguiente paso.
                        </p>
                        <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 xl:grid-cols-5">
                          {starts.map((time) => (
                            <label
                              key={time}
                              className="relative cursor-pointer"
                            >
                              <input
                                type="radio"
                                name="start"
                                value={time}
                                checked={selectedStart === time}
                                onChange={() => setSelectedStart(time)}
                                className="peer sr-only"
                              />
                              <span className="flex min-h-11 items-center justify-center rounded-lg border border-slate-300 bg-card text-sm font-medium tabular-nums transition-colors peer-checked:border-primary peer-checked:bg-primary peer-checked:text-primary-foreground peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary hover:border-primary/50">
                                {time}
                              </span>
                            </label>
                          ))}
                        </div>
                      </fieldset>
                    )
                  )}
                  {(admin
                    ? calendar.data?.franjas.length === 0
                    : starts.length === 0) && (
                    <div role="status" className="py-8 text-center">
                      <Clock
                        className="mx-auto mb-3 size-6 text-muted-foreground"
                        aria-hidden="true"
                      />
                      <p className="text-sm font-semibold">
                        Sin horarios disponibles
                      </p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        Probá con otra fecha.
                      </p>
                    </div>
                  )}
                </QueryState>
                {!admin && (
                  <div className="mt-5 flex flex-wrap items-center justify-between gap-4 border-t pt-5">
                    <p role="status" className="text-sm text-muted-foreground">
                      {canReserve ? (
                        <>
                          Inicio{" "}
                          <span className="font-semibold text-foreground tabular-nums">
                            {selectedStart}
                          </span>{" "}
                          · 1 hora
                        </>
                      ) : (
                        "Seleccioná un horario para continuar."
                      )}
                    </p>
                    {canReserve ? (
                      <Button
                        size="lg"
                        nativeButton={false}
                        role="link"
                        render={
                          <Link
                            to={`/app/reservations/new?space=${s.id}&date=${date}&start=${selectedStart}`}
                          />
                        }
                      >
                        Reservar este espacio
                        <ArrowRight aria-hidden="true" />
                      </Button>
                    ) : (
                      <Button size="lg" disabled>
                        Reservar este espacio
                        <ArrowRight aria-hidden="true" />
                      </Button>
                    )}
                  </div>
                )}
              </SectionCard>
            </div>
            {admin && (
              <div className="mt-6 grid items-start gap-6 xl:grid-cols-2">
                <SectionCard
                  title="Horarios semanales"
                  description="Los rangos del mismo día no pueden superponerse."
                >
                  <ul className="mb-5 divide-y">
                    {s.disponibilidades.map((h) => (
                      <li
                        key={h.id}
                        className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm"
                      >
                        <span>
                          {label(h.diaSemana)} · {h.horaDesde.slice(0, 5)} a{" "}
                          {h.horaHasta.slice(0, 5)}
                        </span>
                        <div className="flex gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={hours.isPending}
                            onClick={() => {
                              hours.reset()
                              setHoursNotice("")
                              setSchedule(h)
                            }}
                          >
                            Editar
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              setDeleting({ kind: "schedule", id: h.id })
                            }
                          >
                            Eliminar
                          </Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                  <p
                    role="status"
                    className={
                      hoursNotice
                        ? "mb-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800"
                        : "sr-only"
                    }
                  >
                    {hoursNotice}
                  </p>
                  <form
                    key={schedule?.id ?? "new"}
                    className="space-y-4 border-t pt-4"
                    onSubmit={(e) => {
                      e.preventDefault()
                      if (hours.isPending) return
                      hours.mutate(new FormData(e.currentTarget))
                    }}
                  >
                    <SelectField
                      label="Día de la semana"
                      name="dia"
                      defaultValue={schedule?.diaSemana ?? "LUNES"}
                      disabled={!!schedule}
                    >
                      {days.map((d) => (
                        <option key={d} value={d}>
                          {label(d)}
                        </option>
                      ))}
                    </SelectField>
                    <div className="grid grid-cols-2 gap-3">
                      <Field
                        label="Desde"
                        name="desde"
                        type="time"
                        autoFocus={!!schedule}
                        required
                        defaultValue={
                          schedule?.horaDesde.slice(0, 5) ?? "08:00"
                        }
                      />
                      <Field
                        label="Hasta"
                        name="hasta"
                        type="time"
                        required
                        defaultValue={
                          schedule?.horaHasta.slice(0, 5) ?? "22:00"
                        }
                      />
                    </div>
                    <ErrorMessage error={hours.error} />
                    <Button type="submit" disabled={hours.isPending}>
                      {hours.isPending
                        ? "Guardando horario…"
                        : schedule
                          ? "Guardar horario"
                          : "Agregar horario"}
                    </Button>
                    {schedule && (
                      <Button
                        type="button"
                        variant="ghost"
                        disabled={hours.isPending}
                        onClick={() => {
                          hours.reset()
                          setHoursNotice("")
                          setSchedule(null)
                        }}
                      >
                        Cancelar edición
                      </Button>
                    )}
                  </form>
                </SectionCard>
                <SectionCard
                  title="Bloqueos por fecha"
                  description={`Excepciones para ${date}.`}
                >
                  <QueryState
                    pending={blocks.isPending}
                    error={blocks.error}
                    retry={blocks.refetch}
                  >
                    <ul className="mb-4 divide-y">
                      {blocks.data?.map((b) => (
                        <li
                          key={b.id}
                          className="flex items-start justify-between gap-2 py-3 text-sm"
                        >
                          <div>
                            <p className="font-semibold">
                              {b.desde.slice(0, 5)} a {b.hasta.slice(0, 5)}
                            </p>
                            <p>{b.motivo}</p>
                          </div>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              setDeleting({ kind: "block", id: b.id })
                            }
                          >
                            Eliminar
                          </Button>
                        </li>
                      ))}
                    </ul>
                    {blocks.data?.length === 0 && (
                      <p className="mb-5 text-sm text-muted-foreground">
                        Sin bloqueos para esta fecha.
                      </p>
                    )}
                  </QueryState>
                  <form
                    className="space-y-4 border-t pt-4"
                    onSubmit={(e) => {
                      e.preventDefault()
                      block.mutate(new FormData(e.currentTarget))
                    }}
                  >
                    <div className="grid grid-cols-2 gap-3">
                      <Field
                        label="Inicio del bloqueo"
                        name="desde"
                        type="time"
                        required
                      />
                      <Field
                        label="Fin del bloqueo"
                        name="hasta"
                        type="time"
                        required
                      />
                    </div>
                    <Field
                      label="Motivo del bloqueo"
                      name="motivo"
                      required
                      maxLength={500}
                    />
                    <ErrorMessage error={block.error} />
                    <Button type="submit" disabled={block.isPending || !date}>
                      Agregar bloqueo
                    </Button>
                  </form>
                  <ErrorMessage error={remove.error} />
                </SectionCard>
                <SectionCard
                  title="Tarifas por relación UNSE"
                  description="Los campos vacíos usan la tarifa general del espacio."
                >
                  <form
                    key={JSON.stringify(s.tarifas)}
                    className="space-y-4"
                    onSubmit={(e) => {
                      e.preventDefault()
                      const f = new FormData(e.currentTarget)
                      rates.mutate(
                        Object.fromEntries(
                          relationships
                            .filter((r) => f.get(r) !== "")
                            .map((r) => [r, Number(f.get(r))])
                        )
                      )
                    }}
                  >
                    <p className="text-sm">
                      Tarifa general: {formatCurrency(s.tarifaHora)} / hora
                    </p>
                    <div className="grid grid-cols-2 gap-4">
                      {relationships.map((r) => (
                        <Field
                          key={r}
                          label={label(r)}
                          name={r}
                          type="number"
                          min="0"
                          max="9999999999.99"
                          step="0.01"
                          defaultValue={s.tarifas[r] ?? ""}
                        />
                      ))}
                    </div>
                    <ErrorMessage error={rates.error} />
                    <Button type="submit" disabled={rates.isPending}>
                      Guardar tarifas
                    </Button>
                  </form>
                </SectionCard>
              </div>
            )}
            <DetailSheet
              open={editing}
              onOpenChange={(v) => {
                if (!update.isPending) setEditing(v)
              }}
              title="Editar espacio"
              description="Actualizá los datos del espacio."
            >
              <SpaceForm
                space={s}
                onSave={(d) => update.mutate(d)}
                pending={update.isPending}
                error={update.error}
              />
            </DetailSheet>
          </>
        )}
      </QueryState>
      <ConfirmationDialog
        open={!!deleting}
        onOpenChange={(v) => {
          if (!v) setDeleting(null)
        }}
        title="Eliminar configuración"
        description="La disponibilidad se recalculará al guardar este cambio."
        confirmLabel="Eliminar"
        destructive
        onConfirm={() => {
          if (deleting) remove.mutate(deleting)
        }}
      />
    </>
  )
}
export function AdminSpacesPage() {
  return <SpacesList admin />
}
export function AdminSpaceDetailPage() {
  return <SpaceDetail admin />
}
export function MemberServicesPage() {
  return <SpacesList />
}
export function MemberServiceDetailPage() {
  return <SpaceDetail />
}
