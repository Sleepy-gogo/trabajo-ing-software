import { useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import {
  CalendarDays,
  Check,
  Eye,
  Filter,
  Plus,
  QrCode,
  Users,
} from "lucide-react"
import QRCode from "react-qr-code"

import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DataToolbar,
  DetailSheet,
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
} from "@/components/shared"
import {
  ExportButton,
  FieldValue,
  FilterSelect,
  RecordTable,
  exportCsv,
} from "./common"
import { reservations as reservationFixtures, spaces, users } from "@/mocks"
import type { Reservation, ReservationState } from "@/types"
import { formatCurrency, formatDateTime } from "@/lib/format"

function reservationTone(state: ReservationState) {
  if (state === "confirmada" || state === "cumplida") return "success" as const
  if (state === "pendiente_pago") return "warning" as const
  if (state === "conflicto_disponibilidad") return "danger" as const
  return "neutral" as const
}

function ownerName(id: string) {
  return (
    users.find((user) => user.id === id)?.nombreCompleto ??
    "Persona sin identificar"
  )
}

export function ReservationsPage() {
  const [params, setParams] = useSearchParams()
  const [reservations, setReservations] = useState<Reservation[]>([
    ...reservationFixtures,
  ])
  const [search, setSearch] = useState("")
  const [state, setState] = useState("Todos los estados")
  const [space, setSpace] = useState("Todos los espacios")
  const [notice, setNotice] = useState("")
  const selected = reservations.find(
    (reservation) => reservation.id === params.get("id")
  )

  const filtered = useMemo(
    () =>
      reservations.filter(
        (reservation) =>
          `${reservation.codigo} ${reservation.ownerName} ${reservation.spaceName}`
            .toLowerCase()
            .includes(search.toLowerCase()) &&
          (state === "Todos los estados" ||
            reservation.estadoLabel === state) &&
          (space === "Todos los espacios" || reservation.spaceName === space)
      ),
    [reservations, search, space, state]
  )

  function open(reservation: Reservation) {
    setNotice("")
    setParams({ id: reservation.id })
  }

  function markFulfilled(reservation: Reservation) {
    setReservations((current) =>
      current.map((item) =>
        item.id === reservation.id
          ? { ...item, estado: "cumplida", estadoLabel: "Cumplida" }
          : item
      )
    )
    setNotice("La reserva quedó marcada como cumplida.")
  }

  return (
    <div>
      <PageHeader
        title="Reservas"
        description="Supervisá el calendario de espacios, pagos y estados de ingreso."
        actions={
          <>
            <ExportButton
              onClick={() =>
                exportCsv(
                  "reservas",
                  ["Código", "Socio", "Espacio", "Fecha", "Estado"],
                  filtered.map((reservation) => [
                    reservation.codigo,
                    reservation.ownerName,
                    reservation.spaceName,
                    reservation.fecha,
                    reservation.estadoLabel,
                  ])
                )
              }
            />
            <Button render={<Link to="/admin/reservations?action=create" />}>
              <Plus />
              Nueva reserva
            </Button>
          </>
        }
      />
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Reservas del período"
          value={reservations.length}
          icon={CalendarDays}
          detail="Incluye todos los estados"
        />
        <StatCard
          label="Confirmadas"
          value={
            reservations.filter((item) => item.estado === "confirmada").length
          }
          icon={Check}
          trend="Disponibles para ingreso"
        />
        <StatCard
          label="Conflictos"
          value={
            reservations.filter(
              (item) => item.estado === "conflicto_disponibilidad"
            ).length
          }
          icon={Filter}
          detail="Requieren revisión"
        />
      </div>
      {notice && (
        <p
          role="status"
          className="mb-5 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs text-emerald-800"
        >
          {notice}
        </p>
      )}
      <SectionCard>
        <DataToolbar
          search={search}
          onSearchChange={setSearch}
          placeholder="Buscar código, socio o espacio…"
        >
          <FilterSelect
            label="Filtrar por estado"
            value={state}
            onChange={setState}
            options={[
              "Todos los estados",
              "Pendiente de pago",
              "Confirmada",
              "Cancelada",
              "Finalizada",
              "Cumplida",
              "Conflicto de disponibilidad",
            ]}
          />
          <FilterSelect
            label="Filtrar por espacio"
            value={space}
            onChange={setSpace}
            options={[
              "Todos los espacios",
              ...spaces.map((item) => item.nombre),
            ]}
          />
        </DataToolbar>
        <ReservationsTable reservations={filtered} onOpen={open} />
      </SectionCard>
      <DetailSheet
        open={!!selected}
        onOpenChange={(openState) => {
          if (!openState) setParams({})
        }}
        title="Detalle de reserva"
        description="Información operativa, pago y código de ingreso."
      >
        {selected && (
          <ReservationDetail
            reservation={selected}
            onFulfill={() => markFulfilled(selected)}
          />
        )}
      </DetailSheet>
    </div>
  )
}

export { ReservationsPage as AdminReservationsPage }

function ReservationsTable({
  reservations,
  onOpen,
}: {
  reservations: Reservation[]
  onOpen: (reservation: Reservation) => void
}) {
  return (
    <RecordTable
      columns={[
        "Código",
        "Socio",
        "Espacio",
        "Fecha y horario",
        "Pago",
        "Estado",
        "Acciones",
      ]}
      rows={reservations.map((reservation) => ({
        key: reservation.id,
        cells: [
          <span className="font-mono text-[11px] font-semibold">
            {reservation.codigo}
          </span>,
          <div>
            <p className="font-medium">{reservation.ownerName}</p>
            <p className="text-[11px] text-muted-foreground">
              {ownerName(reservation.userId) === reservation.ownerName
                ? "Socio registrado"
                : ownerName(reservation.userId)}
            </p>
          </div>,
          <span>{reservation.spaceName}</span>,
          <div>
            <p>{reservation.fecha}</p>
            <p className="text-[11px] text-muted-foreground">
              {reservation.inicio} a {reservation.fin}
            </p>
          </div>,
          <StatusBadge
            tone={
              reservation.pagoEstado === "aprobado"
                ? "success"
                : reservation.pagoEstado === "pendiente"
                  ? "warning"
                  : "danger"
            }
          >
            {reservation.pagoEstadoLabel}
          </StatusBadge>,
          <StatusBadge tone={reservationTone(reservation.estado)}>
            {reservation.estadoLabel}
          </StatusBadge>,
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={`Ver ${reservation.codigo}`}
            onClick={() => onOpen(reservation)}
          >
            <Eye />
          </Button>,
        ],
      }))}
      emptyTitle="No hay reservas con estos filtros"
    />
  )
}

function ReservationDetail({
  reservation,
  onFulfill,
}: {
  reservation: Reservation
  onFulfill: () => void
}) {
  const canFulfill = reservation.estado === "confirmada"
  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <p className="font-mono text-xs text-muted-foreground">
            {reservation.codigo}
          </p>
          <h2 className="mt-1 text-xl font-bold">{reservation.spaceName}</h2>
          <div className="mt-2">
            <StatusBadge tone={reservationTone(reservation.estado)}>
              {reservation.estadoLabel}
            </StatusBadge>
          </div>
        </div>
        <CalendarDays className="size-6 text-primary" />
      </div>
      <dl className="grid gap-5 sm:grid-cols-2">
        <FieldValue label="Titular">{reservation.ownerName}</FieldValue>
        <FieldValue label="Cantidad de personas">
          <span className="inline-flex items-center gap-1.5">
            <Users className="size-3.5 text-muted-foreground" />
            {reservation.cantidadPersonas}
          </span>
        </FieldValue>
        <FieldValue label="Fecha">{reservation.fecha}</FieldValue>
        <FieldValue label="Horario">
          {reservation.inicio} a {reservation.fin} (
          {reservation.duracionMinutos} min)
        </FieldValue>
        <FieldValue label="Pago">{reservation.pagoEstadoLabel}</FieldValue>
        <FieldValue label="Total">
          {formatCurrency(reservation.precio.total)}
        </FieldValue>
        <FieldValue label="Creada">
          {formatDateTime(reservation.creadaEn)}
        </FieldValue>
        <FieldValue label="Registrada por">
          {reservation.registradoPor ?? "Sistema"}
        </FieldValue>
      </dl>
      {reservation.motivoConflicto && (
        <p className="mt-6 rounded-lg border border-rose-200 bg-rose-50 p-4 text-xs leading-relaxed text-rose-900">
          <strong>Conflicto:</strong> {reservation.motivoConflicto}
        </p>
      )}
      {reservation.estado === "confirmada" ||
      reservation.estado === "cumplida" ? (
        <Tabs defaultValue="code" className="mt-7">
          <TabsList className="w-full">
            <TabsTrigger value="code">Código</TabsTrigger>
            <TabsTrigger value="qr">QR</TabsTrigger>
          </TabsList>
          <TabsContent value="code">
            <div className="mt-4 rounded-lg border bg-muted/30 p-5 text-center">
              <QrCode className="mx-auto mb-3 size-8 text-primary" />
              <p className="font-mono text-lg font-bold tracking-wider">
                {reservation.codigo}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                Presentá este código para validar el ingreso.
              </p>
            </div>
          </TabsContent>
          <TabsContent value="qr">
            <div className="mt-4 flex justify-center rounded-lg bg-white p-5">
              <QRCode
                value={
                  reservation.qrPayload ?? `sera:reserva:${reservation.codigo}`
                }
                size={190}
                title={`QR de ${reservation.codigo}`}
              />
            </div>
          </TabsContent>
        </Tabs>
      ) : null}
      {canFulfill && (
        <Button className="mt-6 w-full" onClick={onFulfill}>
          <Check />
          Marcar ingreso cumplido
        </Button>
      )}
    </div>
  )
}
