import { useMemo, useState, type FormEvent } from "react"
import { Link, useParams, useSearchParams } from "react-router-dom"
import {
  CalendarClock,
  Eye,
  Landmark,
  Pencil,
  Plus,
  Settings2,
  Users,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DetailSheet,
  ImagePlaceholder,
  PageHeader,
  SectionCard,
  StatusBadge,
} from "@/components/shared"
import { FieldValue } from "./common"
import { getAvailabilityForSpace, spaces as spaceFixtures } from "@/mocks"
import type { Space, SpaceCategory, SpaceState } from "@/types"
import { formatCurrency, formatDate } from "@/lib/format"

function spaceTone(state: SpaceState) {
  if (state === "habilitado") return "success" as const
  if (state === "en_uso") return "info" as const
  if (state === "en_mantenimiento") return "warning" as const
  return "danger" as const
}

function categoryLabel(category: SpaceCategory) {
  return category === "deporte"
    ? "Cancha"
    : category === "recreacion"
      ? "Recreación"
      : "Servicio"
}

export function AdminSpacesPage() {
  const [params, setParams] = useSearchParams()
  const [spaces, setSpaces] = useState<Space[]>([...spaceFixtures])
  const [category, setCategory] = useState("Todos")
  const [search, setSearch] = useState("")
  const [notice, setNotice] = useState("")
  const selected = spaces.find((space) => space.id === params.get("id"))
  const creating = params.get("action") === "create"
  const filtered = useMemo(
    () =>
      spaces.filter(
        (space) =>
          `${space.nombre} ${space.descripcion}`
            .toLowerCase()
            .includes(search.toLowerCase()) &&
          (category === "Todos" || space.tipoLabel === category)
      ),
    [category, search, spaces]
  )

  function open(space?: Space) {
    setNotice("")
    setParams(space ? { id: space.id } : { action: "create" })
  }

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const id = `space-demo-${spaces.length + 1}`
    const newSpace: Space = {
      id,
      nombre: String(form.get("name")),
      tipo: "servicio",
      tipoLabel: "Servicio",
      descripcion: String(form.get("description")),
      capacidad: Number(form.get("capacity")) || 1,
      estado: "habilitado",
      estadoLabel: "Habilitado",
      fechaRegistro: new Date().toISOString().slice(0, 10),
      ultimaMantenimiento: new Date().toISOString().slice(0, 10),
      cantidadUsos: 0,
      imagenes: [
        {
          id: `${id}-image`,
          alt: String(form.get("name")),
          placeholder: `Foto de ${String(form.get("name"))}`,
        },
      ],
      precios: [
        {
          relacion: "no_socio",
          relacionLabel: "No socio",
          importe: Number(form.get("price")) || 0,
        },
      ],
      requiereMembresia: false,
      requiereReserva: true,
      servicios: [String(form.get("name"))],
    }
    setSpaces((current) => [newSpace, ...current])
    setParams({ id })
    setNotice("El espacio se agregó a la vista previa.")
  }

  return (
    <div>
      <PageHeader
        title="Espacios del polideportivo"
        description="Administrá canchas, quinchos, pileta y servicios disponibles."
        actions={
          <Button onClick={() => open()}>
            <Plus />
            Nuevo espacio
          </Button>
        }
      />
      {notice && (
        <p
          role="status"
          className="mb-5 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs text-emerald-800"
        >
          {notice}
        </p>
      )}
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <div className="flex flex-wrap gap-2">
          {["Todos", "Cancha", "Quincho", "Pileta", "Salón"].map((item) => (
            <Button
              key={item}
              variant={category === item ? "default" : "outline"}
              size="sm"
              onClick={() => setCategory(item)}
            >
              {item}
            </Button>
          ))}
        </div>
        <Input
          className="h-9 w-full sm:ml-auto sm:max-w-xs"
          placeholder="Buscar espacio…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Buscar espacio"
        />
      </div>
      <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
        {filtered.map((space) => (
          <SectionCard key={space.id} className="overflow-hidden p-0">
            <ImagePlaceholder
              label={space.imagenes[0]?.placeholder ?? space.nombre}
              className="h-40 w-full"
            />
            <div className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">
                    {space.tipoLabel}
                  </p>
                  <h2 className="mt-1 text-lg font-bold">{space.nombre}</h2>
                </div>
                <StatusBadge tone={spaceTone(space.estado)}>
                  {space.estadoLabel}
                </StatusBadge>
              </div>
              <p className="mt-3 min-h-10 text-sm leading-relaxed text-muted-foreground">
                {space.descripcion}
              </p>
              <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                  <Users className="size-3.5" />
                  {space.capacidad} personas
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <CalendarClock className="size-3.5" />
                  {space.cantidadUsos} usos
                </span>
              </div>
              <div className="mt-5 flex items-center justify-between border-t pt-4">
                <span className="text-sm font-semibold">
                  Desde{" "}
                  {formatCurrency(
                    Math.min(...space.precios.map((price) => price.importe))
                  )}
                </span>
                <Button variant="outline" size="sm" onClick={() => open(space)}>
                  <Eye />
                  Ver ficha
                </Button>
              </div>
            </div>
          </SectionCard>
        ))}
        {!filtered.length && (
          <SectionCard className="sm:col-span-2 xl:col-span-3">
            <p className="py-10 text-center text-sm text-muted-foreground">
              No encontramos espacios con esos filtros.
            </p>
          </SectionCard>
        )}
      </div>
      <DetailSheet
        open={creating || !!selected}
        onOpenChange={(openState) => {
          if (!openState) setParams({})
        }}
        title={
          creating
            ? "Registrar espacio"
            : (selected?.nombre ?? "Detalle de espacio")
        }
        description={
          creating
            ? "Definí los datos básicos. La disponibilidad y las tarifas se configuran después."
            : "Consultá estado, disponibilidad y tarifas del servicio."
        }
      >
        {creating ? (
          <CreateSpaceForm onSubmit={save} onCancel={() => setParams({})} />
        ) : selected ? (
          <SpaceDetail space={selected} onNotice={setNotice} />
        ) : null}
      </DetailSheet>
    </div>
  )
}

function CreateSpaceForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}) {
  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="space-name">Nombre del espacio</Label>
        <Input
          id="space-name"
          name="name"
          placeholder="Ej. Cancha de vóley"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="space-description">Descripción</Label>
        <Input
          id="space-description"
          name="description"
          placeholder="Describí el espacio y su uso"
          required
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="space-capacity">Capacidad</Label>
          <Input
            id="space-capacity"
            name="capacity"
            type="number"
            min="1"
            defaultValue="10"
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="space-price">Tarifa base por hora</Label>
          <Input
            id="space-price"
            name="price"
            type="number"
            min="0"
            defaultValue="10000"
            required
          />
        </div>
      </div>
      <p className="rounded-lg bg-muted/60 p-4 text-xs leading-relaxed text-muted-foreground">
        Podrás agregar tarifas por relación con la UNSE y franjas de
        disponibilidad desde la ficha del espacio.
      </p>
      <div className="flex justify-end gap-2 border-t pt-5">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit">Crear espacio</Button>
      </div>
    </form>
  )
}

function SpaceDetail({
  space,
  onNotice,
}: {
  space: Space
  onNotice: (message: string) => void
}) {
  const availability = getAvailabilityForSpace(space.id)
  return (
    <Tabs defaultValue="general">
      <TabsList className="mb-6 w-full">
        <TabsTrigger value="general">Información</TabsTrigger>
        <TabsTrigger value="availability">Disponibilidad</TabsTrigger>
        <TabsTrigger value="tariffs">Tarifas</TabsTrigger>
      </TabsList>
      <TabsContent value="general">
        <ImagePlaceholder
          label={space.imagenes[0]?.placeholder ?? space.nombre}
          className="mb-6 h-44 w-full rounded-lg"
        />
        <div className="mb-6 flex items-start gap-3">
          <span className="flex size-11 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Landmark className="size-5" />
          </span>
          <div>
            <h2 className="font-bold">{space.nombre}</h2>
            <div className="mt-1">
              <StatusBadge tone={spaceTone(space.estado)}>
                {space.estadoLabel}
              </StatusBadge>
            </div>
          </div>
        </div>
        <p className="text-sm leading-6 text-muted-foreground">
          {space.descripcion}
        </p>
        <dl className="mt-6 grid gap-5 sm:grid-cols-2">
          <FieldValue label="Tipo">{categoryLabel(space.tipo)}</FieldValue>
          <FieldValue label="Capacidad">{space.capacidad} personas</FieldValue>
          <FieldValue label="Registrado">
            {formatDate(space.fechaRegistro)}
          </FieldValue>
          <FieldValue label="Último mantenimiento">
            {formatDate(space.ultimaMantenimiento)}
          </FieldValue>
          <FieldValue label="Reserva requerida">
            {space.requiereReserva ? "Sí" : "No"}
          </FieldValue>
          <FieldValue label="Membresía requerida">
            {space.requiereMembresia ? "Sí" : "No"}
          </FieldValue>
        </dl>
        <div className="mt-6 flex flex-wrap gap-2">
          {space.servicios.map((service) => (
            <span
              key={service}
              className="rounded-full bg-muted px-3 py-1 text-xs"
            >
              {service}
            </span>
          ))}
        </div>
      </TabsContent>
      <TabsContent value="availability">
        <div className="mb-5 flex items-center justify-between gap-3">
          <div>
            <h3 className="font-semibold">Franjas del día</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Vista de ejemplo para el 26 de agosto de 2026.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              onNotice("La disponibilidad se guardó en esta vista previa.")
            }
          >
            <Settings2 />
            Editar
          </Button>
        </div>
        {availability.length ? (
          availability
            .flatMap((day) => day.slots)
            .map((slot) => (
              <div
                key={slot.id}
                className="flex items-center justify-between gap-3 border-b py-3 text-sm"
              >
                <span className="font-medium">
                  {slot.inicio} a {slot.fin}
                </span>
                <StatusBadge
                  tone={
                    slot.estado === "disponible"
                      ? "success"
                      : slot.estado === "ocupado"
                        ? "warning"
                        : "danger"
                  }
                >
                  {slot.estadoLabel}
                </StatusBadge>
              </div>
            ))
        ) : (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Todavía no hay franjas configuradas.
          </p>
        )}
        <div className="mt-6 rounded-lg bg-muted/60 p-4 text-xs leading-relaxed text-muted-foreground">
          Las franjas ocupadas se bloquean automáticamente cuando una reserva
          queda confirmada.
        </div>
      </TabsContent>
      <TabsContent value="tariffs">
        <div className="mb-5 flex items-center justify-between gap-3">
          <div>
            <h3 className="font-semibold">Tarifas por categoría</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Importes de referencia para cada tipo de usuario.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              onNotice(
                "Las tarifas quedaron actualizadas en esta vista previa."
              )
            }
          >
            <Pencil />
            Editar
          </Button>
        </div>
        {space.precios.map((price) => (
          <div
            key={price.relacionLabel}
            className="flex items-center justify-between gap-3 border-b py-4"
          >
            <div>
              <p className="text-sm font-medium">{price.relacionLabel}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                Por hora de uso
              </p>
            </div>
            <p className="font-semibold tabular-nums">
              {formatCurrency(price.importe)}
            </p>
          </div>
        ))}
        <p className="mt-5 text-xs leading-relaxed text-muted-foreground">
          Antes de aplicar un cambio de tarifa se debe mostrar una confirmación
          y registrar el responsable.
        </p>
      </TabsContent>
    </Tabs>
  )
}

export function AdminSpaceDetailPage() {
  const [params] = useSearchParams()
  const routeParams = useParams()
  const id = params.get("id") ?? routeParams.id ?? "space-futbol-5"
  const space = spaceFixtures.find((item) => item.id === id) ?? spaceFixtures[0]
  return (
    <div>
      <PageHeader
        title={space.nombre}
        description="Detalle operativo del espacio y sus condiciones de uso."
        actions={
          <Button render={<Link to="/admin/spaces" />} variant="outline">
            Volver a espacios
          </Button>
        }
      />
      <SectionCard>
        <SpaceDetail space={space} onNotice={() => undefined} />
      </SectionCard>
    </div>
  )
}

export { AdminSpacesPage as SpacesPage }
