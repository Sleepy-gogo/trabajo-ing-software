import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useSession } from "@/hooks/use-session"
import {
  paymentsApi,
  paymentStateLabel,
  type Payment,
  type PaymentState,
} from "@/lib/payments-api"
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format"
import { Button } from "@/components/ui/button"
import { PageHeader, SectionCard, StatusBadge } from "@/components/shared"
import { ErrorMessage, QueryState } from "@/components/shared/real-data"

const filters: Array<PaymentState | "TODOS"> = [
  "TODOS",
  "PENDIENTE",
  "APROBADO",
  "RECHAZADO",
  "CANCELADO",
]

function paymentTone(payment: Payment) {
  if (payment.requiereRevision) return "warning" as const
  if (payment.estado === "APROBADO") return "success" as const
  if (payment.estado === "PENDIENTE") return "warning" as const
  return "danger" as const
}

export function AdminPaymentsPage() {
  const session = useSession()
  const client = useQueryClient()
  const [state, setState] = useState<PaymentState | "TODOS">("TODOS")
  const [page, setPage] = useState(0)
  const [notice, setNotice] = useState("")
  const payments = useQuery({
    queryKey: ["payments", session.data?.id, "admin", state, page],
    queryFn: ({ signal }) =>
      paymentsApi.list(page, state === "TODOS" ? undefined : state, signal),
    enabled: !!session.data,
  })
  const confirm = useMutation({
    mutationFn: paymentsApi.confirmCash,
    onSuccess: async () => {
      setNotice("El efectivo quedó confirmado y la membresía fue actualizada.")
      await Promise.all([
        client.invalidateQueries({ queryKey: ["payments"] }),
        client.invalidateQueries({ queryKey: ["my-member"] }),
        client.invalidateQueries({ queryKey: ["members"] }),
      ])
    },
  })
  const reconcile = useMutation({
    mutationFn: paymentsApi.reconcile,
    onSuccess: async (result) => {
      setNotice(
        `Se revisaron ${result.facturasRevisadas} facturas de Mercado Pago.`
      )
      await Promise.all([
        client.invalidateQueries({ queryKey: ["payments"] }),
        client.invalidateQueries({ queryKey: ["members"] }),
      ])
    },
  })
  return (
    <>
      <PageHeader
        title="Pagos"
        description="Cobros de membresías registrados en la API."
        actions={
          <Button variant="outline" onClick={() => void payments.refetch()}>
            Actualizar
          </Button>
        }
      />
      {notice && (
        <p role="status" className="mb-4 text-sm text-emerald-800">
          {notice}
        </p>
      )}
      <ErrorMessage error={confirm.error} />
      <ErrorMessage error={reconcile.error} />
      <SectionCard title="Historial de cobros">
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
            {filters.map((option) => (
              <option key={option} value={option}>
                {option === "TODOS" ? "Todos" : paymentStateLabel[option]}
              </option>
            ))}
          </select>
        </label>
        <QueryState
          pending={payments.isPending}
          error={payments.error}
          retry={payments.refetch}
        >
          {payments.data?.content.length ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b text-muted-foreground">
                  <tr>
                    <th className="px-3 py-3 font-medium">Fecha</th>
                    <th className="px-3 py-3 font-medium">Titular</th>
                    <th className="px-3 py-3 font-medium">Medio</th>
                    <th className="px-3 py-3 font-medium">Importe</th>
                    <th className="px-3 py-3 font-medium">Estado</th>
                    <th className="px-3 py-3 font-medium">Comprobante</th>
                    <th className="px-3 py-3 font-medium">Acción</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {payments.data?.content.map((payment) => (
                    <tr key={payment.id}>
                      <td className="px-3 py-4 whitespace-nowrap">
                        {formatDateTime(payment.creadoEn)}
                      </td>
                      <td className="px-3 py-4">{payment.titular}</td>
                      <td className="px-3 py-4">
                        {payment.medioPago === "EFECTIVO"
                          ? "Efectivo"
                          : "Mercado Pago"}
                      </td>
                      <td className="px-3 py-4 tabular-nums">
                        {formatCurrency(payment.monto)}
                      </td>
                      <td className="px-3 py-4">
                        <StatusBadge tone={paymentTone(payment)}>
                          {payment.requiereRevision
                            ? "En revisión"
                            : paymentStateLabel[payment.estado]}
                        </StatusBadge>
                        {payment.motivoRevision && (
                          <p className="mt-1 text-xs text-amber-800">
                            {payment.motivoRevision}
                          </p>
                        )}
                      </td>
                      <td className="px-3 py-4">
                        {payment.comprobante ?? "Pendiente"}
                        {payment.vencimientoResultante && (
                          <p className="text-xs text-muted-foreground">
                            Vence {formatDate(payment.vencimientoResultante)}
                          </p>
                        )}
                      </td>
                      <td className="px-3 py-4">
                        {payment.estado === "PENDIENTE" &&
                          payment.medioPago === "EFECTIVO" && (
                            <Button
                              size="sm"
                              disabled={confirm.isPending}
                              onClick={() => confirm.mutate(payment.id)}
                            >
                              Confirmar efectivo
                            </Button>
                          )}
                        {payment.medioPago === "MERCADO_PAGO" &&
                          payment.idMembresia && (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={reconcile.isPending}
                              onClick={() =>
                                reconcile.mutate(payment.idMembresia!)
                              }
                            >
                              Conciliar cobros
                            </Button>
                          )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="py-8 text-sm text-muted-foreground">
              No hay cobros con este estado.
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
              Página {page + 1} de {Math.max(payments.data?.totalPages ?? 1, 1)}
            </span>
            <Button
              variant="outline"
              disabled={page + 1 >= (payments.data?.totalPages ?? 0)}
              onClick={() => setPage(page + 1)}
            >
              Siguiente
            </Button>
          </div>
        </QueryState>
      </SectionCard>
    </>
  )
}
