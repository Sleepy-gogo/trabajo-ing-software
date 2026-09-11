import { useMemo, useState, type FormEvent } from "react"
import { useSearchParams } from "react-router-dom"
import {
  Check,
  CreditCard,
  Eye,
  Plus,
  ReceiptText,
  SearchCheck,
  UserRound,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
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
import { payments as paymentFixtures, users } from "@/mocks"
import type { Payment, PaymentStatus } from "@/types"
import { formatCurrency, formatDateTime } from "@/lib/format"

function paymentTone(status: PaymentStatus) {
  if (status === "aprobado") return "success" as const
  if (status === "pendiente") return "warning" as const
  if (status === "rechazado" || status === "vencido") return "danger" as const
  return "neutral" as const
}

function paymentOwner(userId: string) {
  return (
    users.find((user) => user.id === userId)?.nombreCompleto ??
    "Persona sin identificar"
  )
}

export function PaymentsPage() {
  const [params, setParams] = useSearchParams()
  const [payments, setPayments] = useState<Payment[]>([...paymentFixtures])
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState("Todos los estados")
  const [concept, setConcept] = useState("Todos los conceptos")
  const [notice, setNotice] = useState("")
  const selected = payments.find((payment) => payment.id === params.get("id"))
  const creating = params.get("action") === "create"

  const filtered = useMemo(
    () =>
      payments.filter((payment) => {
        const owner = paymentOwner(payment.userId)
        return (
          `${owner} ${payment.conceptoLabel} ${payment.comprobante ?? ""}`
            .toLowerCase()
            .includes(search.toLowerCase()) &&
          (status === "Todos los estados" || payment.estadoLabel === status) &&
          (concept === "Todos los conceptos" ||
            payment.conceptoLabel.includes(concept))
        )
      }),
    [concept, payments, search, status]
  )

  function open(payment?: Payment) {
    setNotice("")
    setParams(payment ? { id: payment.id } : { action: "create" })
  }

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const value = Number(form.get("amount"))
    const userId = String(form.get("user"))
    const newPayment: Payment = {
      id: `pay-demo-${payments.length + 1}`,
      userId,
      concepto: "cuota_mensual",
      conceptoLabel: String(form.get("concept")),
      importe: Number.isFinite(value) ? value : 0,
      moneda: "ARS",
      fecha: new Date().toISOString(),
      estado: "pendiente",
      estadoLabel: "Pendiente",
      medio: "transferencia_bancaria",
      medioLabel: "Transferencia bancaria",
      motivo: "Pendiente de confirmación administrativa",
    }
    setPayments((current) => [newPayment, ...current])
    setParams({ id: newPayment.id })
    setNotice("El pago quedó registrado como pendiente en esta vista previa.")
  }

  function approve(payment: Payment) {
    setPayments((current) =>
      current.map((item) =>
        item.id === payment.id
          ? {
              ...item,
              estado: "aprobado",
              estadoLabel: "Pagado",
              motivo: undefined,
            }
          : item
      )
    )
    setNotice("El pago fue marcado como aprobado.")
  }

  return (
    <div>
      <PageHeader
        title="Pagos y cuotas"
        description="Registrá, revisá y conciliá los pagos de membresías y reservas."
        actions={
          <>
            <ExportButton
              onClick={() =>
                exportCsv(
                  "pagos",
                  ["Fecha", "Socio", "Concepto", "Importe", "Estado"],
                  filtered.map((payment) => [
                    payment.fecha,
                    paymentOwner(payment.userId),
                    payment.conceptoLabel,
                    payment.importe,
                    payment.estadoLabel,
                  ])
                )
              }
            />
            <Button onClick={() => open()}>
              <Plus />
              Registrar pago
            </Button>
          </>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Ingresos aprobados"
          value={formatCurrency(
            payments
              .filter((item) => item.estado === "aprobado")
              .reduce((sum, item) => sum + item.importe, 0)
          )}
          icon={CreditCard}
          detail="En datos de demostración"
        />
        <StatCard
          label="Pagos pendientes"
          value={payments.filter((item) => item.estado === "pendiente").length}
          icon={SearchCheck}
          detail="Esperando conciliación"
        />
        <StatCard
          label="Rechazados o vencidos"
          value={
            payments.filter(
              (item) => item.estado === "rechazado" || item.estado === "vencido"
            ).length
          }
          icon={ReceiptText}
          detail="Requieren seguimiento"
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
        <Tabs defaultValue="todos">
          <TabsList className="mb-5 w-full justify-start overflow-x-auto sm:w-auto">
            <TabsTrigger value="todos">Todos</TabsTrigger>
            <TabsTrigger value="pendientes">Pendientes</TabsTrigger>
            <TabsTrigger value="aprobados">Pagados</TabsTrigger>
            <TabsTrigger value="observados">Observados</TabsTrigger>
          </TabsList>
          <TabsContent value="todos">
            <PaymentsTable payments={filtered} onOpen={open} />
          </TabsContent>
          <TabsContent value="pendientes">
            <PaymentsTable
              payments={filtered.filter(
                (payment) => payment.estado === "pendiente"
              )}
              onOpen={open}
            />
          </TabsContent>
          <TabsContent value="aprobados">
            <PaymentsTable
              payments={filtered.filter(
                (payment) => payment.estado === "aprobado"
              )}
              onOpen={open}
            />
          </TabsContent>
          <TabsContent value="observados">
            <PaymentsTable
              payments={filtered.filter(
                (payment) =>
                  payment.estado === "rechazado" || payment.estado === "vencido"
              )}
              onOpen={open}
            />
          </TabsContent>
        </Tabs>
        <DataToolbar
          search={search}
          onSearchChange={setSearch}
          placeholder="Buscar socio, comprobante o concepto…"
        >
          <FilterSelect
            label="Filtrar por estado"
            value={status}
            onChange={setStatus}
            options={[
              "Todos los estados",
              "Pagado",
              "Pendiente",
              "Rechazado",
              "Vencido",
              "Abandonado",
            ]}
          />
          <FilterSelect
            label="Filtrar por concepto"
            value={concept}
            onChange={setConcept}
            options={["Todos los conceptos", "Cuota", "Reserva"]}
          />
        </DataToolbar>
      </SectionCard>

      <DetailSheet
        open={creating || !!selected}
        onOpenChange={(openState) => {
          if (!openState) setParams({})
        }}
        title={creating ? "Registrar pago" : "Detalle del pago"}
        description={
          creating
            ? "Los pagos nuevos quedan pendientes hasta la conciliación."
            : "Consultá el comprobante y el historial de la operación."
        }
      >
        {creating ? (
          <form onSubmit={save} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="payment-user">Socio o usuario</Label>
              <select
                id="payment-user"
                name="user"
                className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                defaultValue={users[1]?.id}
                required
              >
                {users
                  .filter((user) => user.id !== "usr-visitante")
                  .map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.nombreCompleto} · {user.dni}
                    </option>
                  ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="payment-concept">Concepto</Label>
              <Input
                id="payment-concept"
                name="concept"
                defaultValue="Cuota septiembre 2026"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="payment-amount">Importe</Label>
              <Input
                id="payment-amount"
                name="amount"
                type="number"
                min="0"
                step="1"
                defaultValue="6000"
                required
              />
            </div>
            <div className="rounded-lg bg-muted/60 p-4 text-xs leading-relaxed text-muted-foreground">
              El pago se registra como pendiente y queda visible para su
              confirmación manual.
            </div>
            <div className="flex justify-end gap-2 border-t pt-5">
              <Button
                type="button"
                variant="outline"
                onClick={() => setParams({})}
              >
                Cancelar
              </Button>
              <Button type="submit">Registrar pago</Button>
            </div>
          </form>
        ) : selected ? (
          <PaymentDetail
            payment={selected}
            onApprove={() => approve(selected)}
          />
        ) : null}
      </DetailSheet>
    </div>
  )
}

export { PaymentsPage as AdminPaymentsPage }

function PaymentsTable({
  payments,
  onOpen,
}: {
  payments: Payment[]
  onOpen: (payment: Payment) => void
}) {
  return (
    <RecordTable
      columns={[
        "Fecha",
        "Socio",
        "Concepto",
        "Medio",
        "Importe",
        "Estado",
        "Acciones",
      ]}
      rows={payments.map((payment) => ({
        key: payment.id,
        cells: [
          <span className="text-muted-foreground">
            {formatDateTime(payment.fecha)}
          </span>,
          <div className="flex items-center gap-2">
            <span className="flex size-8 items-center justify-center rounded-full bg-blue-50 text-[10px] font-bold text-blue-700">
              {paymentOwner(payment.userId)
                .split(" ")
                .map((part) => part[0])
                .slice(0, 2)
                .join("")}
            </span>
            <span className="font-medium">{paymentOwner(payment.userId)}</span>
          </div>,
          <span>{payment.conceptoLabel}</span>,
          <span className="text-muted-foreground">{payment.medioLabel}</span>,
          <span className="font-semibold tabular-nums">
            {formatCurrency(payment.importe)}
          </span>,
          <StatusBadge tone={paymentTone(payment.estado)}>
            {payment.estadoLabel}
          </StatusBadge>,
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={`Ver pago de ${paymentOwner(payment.userId)}`}
            onClick={() => onOpen(payment)}
          >
            <Eye />
          </Button>,
        ],
      }))}
      emptyTitle="No hay pagos con estos filtros"
    />
  )
}

function PaymentDetail({
  payment,
  onApprove,
}: {
  payment: Payment
  onApprove: () => void
}) {
  return (
    <div>
      <div className="mb-6 flex items-center gap-4">
        <span className="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <CreditCard className="size-6" />
        </span>
        <div>
          <p className="text-lg font-bold">{formatCurrency(payment.importe)}</p>
          <StatusBadge tone={paymentTone(payment.estado)}>
            {payment.estadoLabel}
          </StatusBadge>
        </div>
      </div>
      <dl className="grid gap-5 sm:grid-cols-2">
        <FieldValue label="Socio">
          <span className="inline-flex items-center gap-1.5">
            <UserRound className="size-3.5 text-muted-foreground" />
            {paymentOwner(payment.userId)}
          </span>
        </FieldValue>
        <FieldValue label="Concepto">{payment.conceptoLabel}</FieldValue>
        <FieldValue label="Fecha">{formatDateTime(payment.fecha)}</FieldValue>
        <FieldValue label="Medio">{payment.medioLabel}</FieldValue>
        <FieldValue label="Comprobante">
          {payment.comprobante ?? "Pendiente de generar"}
        </FieldValue>
        <FieldValue label="Identificador">{payment.id}</FieldValue>
      </dl>
      {payment.motivo && (
        <p className="mt-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-xs leading-relaxed text-amber-900">
          <strong>Observación:</strong> {payment.motivo}
        </p>
      )}
      {payment.estado === "pendiente" && (
        <Button className="mt-6 w-full" onClick={onApprove}>
          <Check />
          Confirmar recepción
        </Button>
      )}
    </div>
  )
}
