import { createRequestKey } from "@/lib/request-key"
import { useEffect, useRef, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, useSearchParams } from "react-router-dom"
import { useSession } from "@/hooks/use-session"
import { membersApi } from "@/lib/members-api"
import {
  paymentsApi,
  paymentStateLabel,
  type Payment,
  type PaymentState,
} from "@/lib/payments-api"
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { PageHeader, SectionCard, StatusBadge } from "@/components/shared"
import { ErrorMessage, QueryState, Note } from "@/components/shared/real-data"

const states: Array<PaymentState | "TODOS"> = [
  "TODOS",
  "PENDIENTE",
  "APROBADO",
  "RECHAZADO",
  "CANCELADO",
]

function tone(payment: Payment) {
  if (payment.requiereRevision) return "warning" as const
  if (payment.estado === "APROBADO") return "success" as const
  if (payment.estado === "PENDIENTE") return "warning" as const
  return "danger" as const
}

export function MemberPaymentsPage() {
  const session = useSession()
  const client = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [state, setState] = useState<PaymentState | "TODOS">("TODOS")
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)
  const [notice, setNotice] = useState("")
  const requestKey = useRef(createRequestKey())
  const member = useQuery({
    queryKey: ["my-member", session.data?.id],
    queryFn: ({ signal }) => membersApi.me(signal),
    enabled: !!session.data,
  })
  const payments = useQuery({
    queryKey: ["payments", session.data?.id, page, state],
    queryFn: ({ signal }) =>
      paymentsApi.list(page, state === "TODOS" ? undefined : state, signal),
    enabled: !!session.data,
    refetchInterval: (query) =>
      query.state.data?.content.some(
        (payment) => payment.estado === "PENDIENTE"
      )
        ? 15_000
        : false,
  })
  const billing = useQuery({
    queryKey: ["membership-billing", member.data?.membresiaId],
    queryFn: ({ signal }) =>
      paymentsApi.membershipBilling(member.data!.membresiaId!, signal),
    enabled: !!member.data?.membresiaId,
  })
  useEffect(() => {
    if (payments.dataUpdatedAt > 0) {
      // Una confirmación que llega por polling también debe actualizar la membresía.
      void client.invalidateQueries({ queryKey: ["my-member"] })
      void client.invalidateQueries({ queryKey: ["membership-billing"] })
    }
  }, [payments.dataUpdatedAt, client])
  const receipt = useQuery({
    queryKey: ["payment-receipt", session.data?.id, selected],
    queryFn: ({ signal }) => paymentsApi.receipt(selected!, signal),
    enabled:
      !!selected &&
      payments.data?.content.find((p) => p.id === selected)?.estado ===
        "APROBADO",
  })
  const startCash = useMutation({
    mutationFn: () =>
      paymentsApi.start(
        member.data!.membresiaId!,
        "EFECTIVO",
        requestKey.current
      ),
    onSuccess: async (payment) => {
      requestKey.current = createRequestKey()
      setNotice(
        "El pago en efectivo quedó pendiente de confirmación administrativa."
      )
      setSelected(payment.id)
      setState("TODOS")
      setPage(0)
      await Promise.all([
        client.invalidateQueries({ queryKey: ["payments"] }),
        client.invalidateQueries({ queryKey: ["membership-billing"] }),
        client.invalidateQueries({ queryKey: ["my-member"] }),
      ])
    },
  })
  const selectedPayment =
    payments.data?.content.find((payment) => payment.id === selected) ?? null
  const canStart =
    member.data?.membresiaId &&
    member.data.estadoMembresia !== "CANCELADA" &&
    member.data.estadoMembresia !== "SUSPENDIDA" &&
    billing.isSuccess &&
    !billing.data.pagoPendiente &&
    (!billing.data.suscripcion ||
      billing.data.suscripcion.estado === "canceled")

  return (
    <>
      <PageHeader
        title="Mis pagos"
        description="Consultá los pagos de membresías y reservas."
        actions={
          <Button
            variant="outline"
            onClick={() => {
              void payments.refetch()
              void member.refetch()
              if (member.data?.membresiaId) void billing.refetch()
            }}
          >
            Actualizar
          </Button>
        }
      />
      {params.has("view") && (
        <Note>
          Estamos verificando el resultado con Mercado Pago. El regreso al sitio
          no confirma el cobro. Consultá el estado del pago aquí.
          <Button
            className="ml-3"
            size="sm"
            variant="outline"
            onClick={() => setParams({})}
          >
            Entendido
          </Button>
        </Note>
      )}
      {notice && (
        <p role="status" className="mb-4 text-sm text-emerald-800">
          {notice}
        </p>
      )}
      <ErrorMessage error={startCash.error} />
      <QueryState
        pending={member.isPending || payments.isPending}
        error={member.error ?? payments.error}
        retry={() => {
          void member.refetch()
          void payments.refetch()
        }}
      >
        <div className="grid gap-5 lg:grid-cols-[1fr_300px]">
          <SectionCard title="Historial">
            <label className="mb-4 block text-sm">
              Estado
              <select
                className="ml-3 rounded-md border bg-background px-3 py-2"
                value={state}
                onChange={(event) => {
                  setState(event.target.value as typeof state)
                  setPage(0)
                }}
              >
                {states.map((option) => (
                  <option key={option} value={option}>
                    {option === "TODOS" ? "Todos" : paymentStateLabel[option]}
                  </option>
                ))}
              </select>
            </label>
            {payments.data?.content.length ? (
              <ul className="divide-y">
                {payments.data.content.map((payment) => (
                  <li
                    key={payment.id}
                    className="flex flex-wrap items-center justify-between gap-3 py-4"
                  >
                    <div>
                      <p className="font-medium">
                        {payment.conceptoPago === "CUOTA_MENSUAL"
                          ? "Renovación mensual"
                          : payment.conceptoPago === "DIFERENCIA_TICKET"
                            ? "Diferencia de reserva"
                            : "Reserva"}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatDateTime(payment.creadoEn)} ·{" "}
                        {payment.medioPago === "EFECTIVO"
                          ? "Efectivo"
                          : "Mercado Pago"}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="font-semibold tabular-nums">
                        {formatCurrency(payment.monto)}
                      </span>
                      <StatusBadge tone={tone(payment)}>
                        {payment.requiereRevision
                          ? "En revisión"
                          : paymentStateLabel[payment.estado]}
                      </StatusBadge>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setSelected(payment.id)}
                      >
                        Ver
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="py-8 text-sm text-muted-foreground">
                No hay pagos con este estado.
              </p>
            )}
            <div className="mt-4 flex items-center justify-between gap-3 text-sm">
              <Button
                variant="outline"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Anterior
              </Button>
              <span>
                Página {page + 1} de{" "}
                {Math.max(payments.data?.totalPages ?? 1, 1)}
              </span>
              <Button
                variant="outline"
                disabled={page + 1 >= (payments.data?.totalPages ?? 0)}
                onClick={() => setPage(page + 1)}
              >
                Siguiente
              </Button>
            </div>
          </SectionCard>
          <SectionCard title="Membresía">
            <p className="text-sm">
              {member.data?.nivelMembresiaNombre ?? "Sin membresía"}
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              {member.data?.proximoVencimiento
                ? `Vence el ${formatDate(member.data.proximoVencimiento)}`
                : "Aún sin pago aprobado"}
            </p>
            <p className="mt-2 text-sm">
              Estado: {member.data?.estadoMembresia ?? "Sin membresía"}
            </p>
            {canStart && (
              <Button
                className="mt-5 w-full"
                disabled={startCash.isPending}
                onClick={() => startCash.mutate()}
              >
                Solicitar pago en efectivo
              </Button>
            )}
            {billing.data?.pagoPendiente && (
              <p className="mt-4 text-sm">
                Ya tenés un pago de membresía pendiente de confirmación.
              </p>
            )}
            {billing.data?.suscripcion &&
              billing.data.suscripcion.estado !== "canceled" && (
                <p className="mt-4 text-sm">
                  Tu membresía usa Mercado Pago. Verificá el cobro o cancelá la
                  suscripción desde Mi membresía para cambiar el medio de pago.
                </p>
              )}
            <ErrorMessage error={billing.error} />
            {member.data?.membresiaId && (
              <Link
                className="mt-4 block text-sm text-primary underline"
                to="/app/memberships/status"
              >
                Mi membresía
              </Link>
            )}
          </SectionCard>
        </div>
      </QueryState>
      <Dialog
        open={!!selected}
        onOpenChange={(open) => {
          if (!open) setSelected(null)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Detalle del pago</DialogTitle>
            <DialogDescription>
              {selectedPayment?.id ?? selected}
            </DialogDescription>
          </DialogHeader>
          {selectedPayment && (
            <dl className="space-y-3 text-sm">
              <div>
                <dt className="text-muted-foreground">Importe</dt>
                <dd>{formatCurrency(selectedPayment.monto)}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Estado</dt>
                <dd>{paymentStateLabel[selectedPayment.estado]}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Fecha de cobro</dt>
                <dd>
                  {selectedPayment.aprobadoEn
                    ? formatDateTime(selectedPayment.aprobadoEn)
                    : "Pendiente"}
                </dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Comprobante</dt>
                <dd>
                  {receipt.data?.comprobante ??
                    selectedPayment.comprobante ??
                    "Aún no disponible"}
                </dd>
              </div>
              {selectedPayment.vencimientoResultante && (
                <div>
                  <dt className="text-muted-foreground">Vigencia resultante</dt>
                  <dd>{formatDate(selectedPayment.vencimientoResultante)}</dd>
                </div>
              )}
              {selectedPayment.requiereRevision && (
                <p role="status" className="text-amber-800">
                  {selectedPayment.motivoRevision ??
                    "Este cobro necesita revisión administrativa."}
                </p>
              )}
              <ErrorMessage error={receipt.error} />
            </dl>
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}
