import { ReservationStatus } from "@/components/shared/reservation-status"
import { useEffect, useRef, useState } from "react"
import { Link, useParams, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import QRCode from "react-qr-code"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  ConfirmationDialog,
} from "@/components/shared"
import {
  ErrorMessage,
  Field,
  Note,
  QueryState,
  SelectField,
} from "@/components/shared/real-data"
import { label } from "@/lib/members-api"
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format"
import {
  reservationsApi,
  reservationState,
  type Reservation,
} from "@/lib/reservations-api"

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
          <Button
            nativeButton={false}
            role="link"
            render={<Link to="/app/reservations/new" />}
          >
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
              action={<ReservationStatus state={r.estado} />}
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
                  nativeButton={false}
                  role="link"
                  render={<Link to={`/app/reservations/${r.id}`} />}
                >
                  Ver detalle
                </Button>
                {r.saldoTicket > 0 && (
                  <Button
                    nativeButton={false}
                    role="link"
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
          <Button
            variant="outline"
            nativeButton={false}
            role="link"
            render={<Link to="/app/reservations" />}
          >
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
  const returnPaymentId = params.get("payment_id") ?? ""
  const [paymentId, setPaymentId] = useState(returnPaymentId)
  const verifiedReturn = useRef("")
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
    mutationFn: (id: number) => reservationsApi.verify(r.id, id),
    onSuccess: async (result) => {
      client.setQueryData(["reservations", r.id], result)
      await refresh()
    },
  })
  const verifyPayment = verify.mutate
  useEffect(() => {
    const id = Number(returnPaymentId)
    const attempt = `${r.id}:${returnPaymentId}`
    if (
      r.medioPago !== "MERCADO_PAGO" ||
      r.estadoPago === "APROBADO" ||
      !/^\d+$/.test(returnPaymentId) ||
      !Number.isSafeInteger(id) ||
      id <= 0 ||
      verifiedReturn.current === attempt
    )
      return
    verifiedReturn.current = attempt
    verifyPayment(id)
  }, [r.id, r.medioPago, r.estadoPago, returnPaymentId, verifyPayment])
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
        action={<ReservationStatus state={r.estado} />}
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
        {verify.isPending && (
          <p role="status" className="mt-4 text-sm text-muted-foreground">
            Verificando el pago con Mercado Pago…
          </p>
        )}
        <div className="mt-6 flex flex-wrap gap-3">
          {pending && r.medioPago === "MERCADO_PAGO" && !admin && (
            <Button
              disabled={checkout.isPending || verify.isPending}
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
              verify.mutate(Number(paymentId))
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
          r.codigo && (r.estado === "CONFIRMADA" || r.estado === "EN_CURSO")
            ? "Código de reserva"
            : "Estado de la reserva"
        }
      >
        {r.codigo && (r.estado === "CONFIRMADA" || r.estado === "EN_CURSO") ? (
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
            {r.consumidaEn
              ? `Ingreso registrado el ${new Intl.DateTimeFormat("es-AR", { dateStyle: "medium", timeStyle: "short", timeZone: "America/Argentina/Buenos_Aires" }).format(new Date(r.consumidaEn))}. El QR ya fue utilizado.`
              : pending
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
                nativeButton={false}
                role="link"
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
