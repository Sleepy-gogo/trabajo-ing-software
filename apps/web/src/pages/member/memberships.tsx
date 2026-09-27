import { useState } from "react"
import { Link } from "react-router-dom"
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
  const session = useSession()
  const client = useQueryClient()
  const [relation, setRelation] = useState<Relationship>("EXTERNO")
  const [identifier, setIdentifier] = useState("")
  const [confirm, setConfirm] = useState<Level | null>(null)
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
      const membership = await membersApi.contract(
        socio.id,
        level.id,
        "MERCADO_PAGO"
      )
      return membersApi.startSubscription(membership.id)
    },
    onSuccess: async (subscription) => {
      await client.invalidateQueries({ queryKey: ["my-member"] })
      window.location.assign(subscription.checkoutUrl)
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
        description="Se registrará la membresía y te llevaremos a Mercado Pago para autorizar el cobro mensual. Los beneficios se habilitan cuando se confirme el primer pago."
        confirmLabel="Continuar a Mercado Pago"
        onConfirm={() => {
          if (confirm) contract.mutate(confirm)
        }}
      />
    </>
  )
}
export function MembershipStatusPage() {
  const client = useQueryClient()
  const [confirm, setConfirm] = useState(false)
  const [reason, setReason] = useState("")
  const [notice, setNotice] = useState("")
  const member = useQuery({
    queryKey: ["my-member"],
    queryFn: ({ signal }) => membersApi.me(signal),
  })
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
                <dt className="text-muted-foreground">Próximo vencimiento</dt>
                <dd className="mt-1 font-medium">
                  {member.data.proximoVencimiento ?? "Sin cuota emitida"}
                </dd>
              </div>
            </dl>
            {member.data.estadoMembresia === "PENDIENTE_PAGO" && (
              <div className="mt-5 space-y-3">
                <Note>
                  Tu solicitud está pendiente. Autorizá el cobro mensual en
                  Mercado Pago; los beneficios se habilitan cuando se confirme
                  el primer pago.
                </Note>
                <Button
                  disabled={subscribe.isPending}
                  onClick={() => subscribe.mutate()}
                >
                  Continuar a Mercado Pago
                </Button>
                <ErrorMessage error={subscribe.error} />
              </div>
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
