import { useEffect, useRef, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import {
  membersApi,
  relationships,
  label,
  type Relationship,
  type Level,
} from "@/lib/members-api"
import { useSession } from "@/hooks/use-session"
import { formatCurrency } from "@/lib/format"
import { paymentsApi, type PaymentMethod } from "@/lib/payments-api"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  StatusBadge,
  ConfirmationDialog,
} from "@/components/shared"
import {
  Field,
  SelectField,
  ErrorMessage,
  QueryState,
  Note,
} from "@/components/shared/real-data"
export function MembershipsPage() {
  const navigate = useNavigate()
  const session = useSession()
  const client = useQueryClient()
  const [relation, setRelation] = useState<Relationship>("EXTERNO")
  const [identifier, setIdentifier] = useState("")
  const [confirm, setConfirm] = useState<Level | null>(null)
  const [method, setMethod] = useState<PaymentMethod>("MERCADO_PAGO")
  const member = useQuery({
    queryKey: ["my-member"],
    queryFn: ({ signal }) => membersApi.me(signal),
  })
  const levels = useQuery({
    queryKey: ["levels", "available"],
    queryFn: ({ signal }) => membersApi.levels(false, signal),
  })
  const contract = useMutation({
    mutationFn: async (level: Level) => {
      const socio =
        member.data ??
        (await membersApi.register({
          usuarioId: session.data!.id,
          relacionUnse: relation,
          identificadorUnse: identifier || null,
        }))
      const membership = await membersApi.contract(socio.id, level.id, method)
      return method === "MERCADO_PAGO"
        ? membersApi.startSubscription(membership.id)
        : null
    },
    onSuccess: async (subscription) => {
      await client.invalidateQueries({ queryKey: ["my-member"] })
      await client.invalidateQueries({ queryKey: ["payments"] })
      if (subscription) window.location.assign(subscription.checkoutUrl)
      else navigate("/app/payments")
    },
  })
  const currentRelation: Relationship =
    member.data?.estadoVerificacionUnse === "VERIFICADA"
      ? member.data.relacionUnse
      : "EXTERNO"
  const incompatible =
    !!member.data?.membresiaId && member.data.estadoMembresia !== "CANCELADA"
  return (
    <>
      <PageHeader
        title="Elegí tu membresía"
        description="Conocé los planes y beneficios del polideportivo."
        actions={
          <Link
            to="/app/memberships/status"
            className="text-sm text-primary underline"
          >
            Mi membresía
          </Link>
        }
      />
      <ErrorMessage error={contract.error} />
      {contract.error && (
        <p className="mb-4 text-sm">
          Si la solicitud quedó registrada, continuá desde{" "}
          <Link to="/app/memberships/status" className="underline">
            Mi membresía
          </Link>
          .
        </p>
      )}
      <QueryState
        pending={levels.isPending || member.isPending}
        error={levels.error ?? member.error}
        retry={() => {
          void levels.refetch()
          void member.refetch()
        }}
      >
        {!member.data && (
          <SectionCard title="Tu relación con la UNSE" className="mb-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <SelectField
                label="Relación declarada"
                value={relation}
                onChange={(e) => setRelation(e.target.value as Relationship)}
              >
                {relationships.map((r) => (
                  <option key={r} value={r}>
                    {label(r)}
                  </option>
                ))}
              </SelectField>
              <Field
                label="Legajo o identificador, si corresponde"
                maxLength={50}
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
              />
            </div>
            <p className="mt-3 text-xs text-muted-foreground">
              La administración verificará esta información manualmente.
            </p>
          </SectionCard>
        )}
        <SectionCard title="Medio de pago" className="mb-5">
          <SelectField
            label="Cómo querés pagar"
            value={method}
            onChange={(event) => setMethod(event.target.value as PaymentMethod)}
          >
            <option value="MERCADO_PAGO">
              Mercado Pago, cobro mensual autorizado
            </option>
            <option value="EFECTIVO">
              Efectivo, confirmación administrativa
            </option>
          </SelectField>
        </SectionCard>
        {incompatible && (
          <Note>
            Ya tenés una membresía{" "}
            {label(member.data?.estadoMembresia).toLowerCase()}. Consultá su
            estado antes de iniciar otra contratación.
          </Note>
        )}
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {levels.data?.map((n) => (
            <SectionCard key={n.id} title={n.nombre}>
              <p className="min-h-12 text-sm text-muted-foreground">
                {n.descripcion}
              </p>
              <p className="my-5 text-3xl font-bold tracking-tight">
                {n.preciosPorRelacion[currentRelation] != null
                  ? formatCurrency(n.preciosPorRelacion[currentRelation]!)
                  : "No disponible"}
                <span className="ml-1 text-sm font-normal text-muted-foreground">
                  / mes
                </span>
              </p>
              <p className="mb-4 text-xs text-muted-foreground">
                Precio para {label(currentRelation).toLowerCase()}.
                {currentRelation === "EXTERNO" &&
                  " Se aplica esta tarifa hasta verificar la relación con la UNSE."}
              </p>
              <ul className="mb-6 list-inside list-disc space-y-2 text-sm">
                {n.beneficios.map((b) => (
                  <li key={b}>{b}</li>
                ))}
              </ul>
              <Button
                className="w-full"
                disabled={
                  contract.isPending ||
                  incompatible ||
                  n.preciosPorRelacion[currentRelation] == null
                }
                onClick={() => setConfirm(n)}
              >
                Solicitar membresía
              </Button>
            </SectionCard>
          ))}
        </div>
        {levels.data?.length === 0 && (
          <SectionCard>
            <p>
              No hay niveles disponibles. Consultá con la administración del
              polideportivo.
            </p>
          </SectionCard>
        )}
      </QueryState>
      <ConfirmationDialog
        open={!!confirm}
        onOpenChange={(v) => {
          if (!v) setConfirm(null)
        }}
        title={`Solicitar ${confirm?.nombre ?? "membresía"}`}
        description={
          method === "MERCADO_PAGO"
            ? "Se registrará la membresía y continuarás en Mercado Pago. Los beneficios se habilitan cuando se confirme el primer cobro."
            : "Se registrará un pago pendiente en efectivo. La administración deberá confirmar su recepción para habilitar los beneficios."
        }
        confirmLabel={
          method === "MERCADO_PAGO"
            ? "Continuar a Mercado Pago"
            : "Solicitar pago en efectivo"
        }
        onConfirm={() => {
          if (confirm) contract.mutate(confirm)
        }}
      />
    </>
  )
}
export function MembershipStatusPage() {
  const session = useSession()
  const client = useQueryClient()
  const pollingStarted = useRef<number | null>(null)
  useEffect(() => {
    pollingStarted.current = Date.now()
  }, [])
  const [confirm, setConfirm] = useState(false)
  const [reason, setReason] = useState("")
  const [notice, setNotice] = useState("")
  const member = useQuery({
    queryKey: ["my-member"],
    queryFn: ({ signal }) => membersApi.me(signal),
    refetchInterval: (query) =>
      query.state.data?.estadoMembresia === "PENDIENTE_PAGO" &&
      (pollingStarted.current === null ||
        Date.now() - pollingStarted.current < 120_000)
        ? 10_000
        : false,
  })
  const payments = useQuery({
    queryKey: ["payments", session.data?.id, "membership-status"],
    queryFn: ({ signal }) => paymentsApi.list(0, undefined, signal),
    enabled: !!session.data,
    refetchInterval: () =>
      member.data?.estadoMembresia === "PENDIENTE_PAGO" &&
      (pollingStarted.current === null ||
        Date.now() - pollingStarted.current < 120_000)
        ? 10_000
        : false,
  })
  const pendingPayment = payments.data?.content.find(
    (payment) =>
      payment.idMembresia === member.data?.membresiaId &&
      payment.estado === "PENDIENTE"
  )
  const cancel = useMutation({
    mutationFn: () => membersApi.cancel(member.data!.membresiaId!, reason),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["my-member"] })
      setNotice("La membresía fue cancelada.")
      setReason("")
    },
  })
  const subscribe = useMutation({
    mutationFn: () => membersApi.startSubscription(member.data!.membresiaId!),
    onSuccess: (subscription) => {
      window.location.assign(subscription.checkoutUrl)
    },
  })
  return (
    <>
      <PageHeader
        title="Mi membresía"
        description="Consultá tu nivel, estado y relación con la UNSE."
      />
      <p role="status" className="mb-4 text-sm text-emerald-800">
        {notice}
      </p>
      <QueryState
        pending={member.isPending}
        error={member.error}
        retry={member.refetch}
      >
        {member.data?.membresiaId ? (
          <SectionCard
            title={member.data.nivelMembresiaNombre ?? "Membresía"}
            action={
              <StatusBadge
                tone={
                  member.data.estadoMembresia === "ACTIVA"
                    ? "success"
                    : "warning"
                }
              >
                {label(member.data.estadoMembresia)}
              </StatusBadge>
            }
          >
            <dl className="grid gap-5 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-muted-foreground">Relación UNSE</dt>
                <dd className="mt-1 font-medium">
                  {label(member.data.relacionUnse)}
                </dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Verificación</dt>
                <dd className="mt-1 font-medium">
                  {label(member.data.estadoVerificacionUnse)}
                </dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Vence el</dt>
                <dd className="mt-1 font-medium">
                  {member.data.proximoVencimiento ?? "Aún sin pago aprobado"}
                </dd>
              </div>
            </dl>
            {member.data.estadoMembresia === "PENDIENTE_PAGO" && (
              <div className="mt-5 space-y-3">
                <Note>
                  Tu solicitud está pendiente. Los beneficios se habilitan
                  cuando se confirme el primer pago.
                </Note>
                {payments.isPending ? (
                  <p className="text-sm">Consultando el medio de pago…</p>
                ) : payments.isError ? (
                  <ErrorMessage error={payments.error} />
                ) : pendingPayment?.medioPago === "MERCADO_PAGO" ? (
                  <Button
                    disabled={subscribe.isPending}
                    onClick={() => subscribe.mutate()}
                  >
                    Continuar a Mercado Pago
                  </Button>
                ) : pendingPayment?.medioPago === "EFECTIVO" ? (
                  <p className="text-sm">
                    El pago en efectivo espera confirmación administrativa.
                  </p>
                ) : (
                  <p className="text-sm">
                    No hay un pago pendiente. Consultá el historial o volvé a
                    solicitar la membresía.
                  </p>
                )}
                <ErrorMessage error={subscribe.error} />
              </div>
            )}
            {member.data.estadoMembresia === "VENCIDA" && (
              <Link
                className="mt-5 inline-block text-primary underline"
                to="/app/payments"
              >
                Renovar membresía
              </Link>
            )}
            {member.data.estadoMembresia !== "CANCELADA" ? (
              <form
                className="mt-6 max-w-lg space-y-4 border-t pt-5"
                onSubmit={(e) => {
                  e.preventDefault()
                  setConfirm(true)
                }}
              >
                <Field
                  label="Motivo de cancelación"
                  required
                  maxLength={500}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
                <Button
                  type="submit"
                  variant="outline"
                  disabled={cancel.isPending}
                >
                  Cancelar membresía
                </Button>
              </form>
            ) : (
              <Link
                className="mt-5 inline-block text-primary underline"
                to="/app/memberships"
              >
                Consultar niveles y volver a contratar
              </Link>
            )}
            <ErrorMessage error={cancel.error} />
          </SectionCard>
        ) : (
          <SectionCard title="Todavía no tenés una membresía">
            <p className="mb-5 text-sm text-muted-foreground">
              Podés consultar los niveles y solicitar el que se adapte a tu
              relación con la UNSE.
            </p>
            <Link
              className="font-medium text-primary underline"
              to="/app/memberships"
            >
              Ver niveles disponibles
            </Link>
          </SectionCard>
        )}
      </QueryState>
      <ConfirmationDialog
        open={confirm}
        onOpenChange={setConfirm}
        title="Cancelar membresía"
        description="La cancelación es inmediata. Se conservará tu historial y dejarás de contar con los beneficios de esta membresía."
        confirmLabel="Confirmar cancelación"
        destructive
        onConfirm={() => cancel.mutate()}
      />
    </>
  )
}
