import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useSearchParams } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { PageHeader, SectionCard, DetailSheet } from "@/components/shared"
import { Field, QueryState, SelectField } from "@/components/shared/real-data"
import { reservationsApi, reservationState } from "@/lib/reservations-api"
import { formatDate } from "@/lib/format"
import { ReservationDetails } from "@/pages/member/reservations"

export function AdminReservationsPage() {
  const [params, setParams] = useSearchParams()
  const [search, setSearch] = useState("")
  const [state, setState] = useState("")
  const reservations = useQuery({
    queryKey: ["reservations", "admin"],
    queryFn: ({ signal }) => reservationsApi.list(true, signal),
    refetchInterval: 15000,
  })
  const selected = reservations.data?.find((r) => r.id === params.get("id"))
  const filtered =
    reservations.data?.filter(
      (r) =>
        (!state || r.estado === state) &&
        `${r.titular} ${r.espacioNombre} ${r.codigo ?? ""}`
          .toLowerCase()
          .includes(search.toLowerCase())
    ) ?? []
  return (
    <>
      <PageHeader
        title="Reservas"
        description="Consultá reservas, pagos y cancelaciones del polideportivo."
      />
      <SectionCard>
        <div className="mb-6 grid gap-4 sm:grid-cols-2">
          <Field
            label="Buscar titular, espacio o código"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <SelectField
            label="Estado"
            value={state}
            onChange={(e) => setState(e.target.value)}
          >
            <option value="">Todos</option>
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
          <ul className="divide-y">
            {filtered.map((r) => (
              <li
                key={r.id}
                className="flex flex-wrap items-center justify-between gap-4 py-4"
              >
                <div>
                  <p className="font-semibold">
                    {r.espacioNombre} · {r.titular}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {formatDate(r.fecha)} · {r.desde.slice(0, 5)} a{" "}
                    {r.hasta.slice(0, 5)} · {reservationState[r.estado]}
                  </p>
                </div>
                <Button
                  variant="outline"
                  onClick={() => setParams({ id: r.id })}
                >
                  Ver reserva
                </Button>
              </li>
            ))}
          </ul>
          {filtered.length === 0 && (
            <p className="py-6 text-sm">No hay reservas con estos filtros.</p>
          )}
        </QueryState>
      </SectionCard>
      <DetailSheet
        open={!!selected}
        onOpenChange={(open) => {
          if (!open) setParams({})
        }}
        title="Detalle de reserva"
        description="Información, pago y cancelación."
      >
        {selected && (
          <ReservationDetails key={selected.id} reservation={selected} admin />
        )}
      </DetailSheet>
    </>
  )
}
