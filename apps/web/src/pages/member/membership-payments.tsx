import { useState } from "react"
import {
  Check,
  CreditCard,
  FileText,
  RefreshCw,
  ShieldCheck,
} from "lucide-react"
import { useLocation, useParams, useSearchParams } from "react-router-dom"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  currentUser,
  getMembershipByUserId,
  getPaymentsByUserId,
  membershipPlans,
} from "@/mocks"

import { formatDate, getPlanPrice, money } from "./data"
import {
  Choice,
  Go,
  InfoRows,
  LoadingState,
  MemberHeading,
  Notice,
  Panel,
  Result,
  StateBadge,
  TextLink,
} from "./member-parts"

const membership = getMembershipByUserId(currentUser.id)
const memberPayments = getPaymentsByUserId(currentUser.id)

export function MembershipsPage() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const [accepted, setAccepted] = useState(false)
  const [showValidation, setShowValidation] = useState(false)
  const plan = membershipPlans.find((item) => item.id === id)
  const activePlan = membershipPlans.find(
    (item) => item.id === membership?.planId
  )

  if (id && !plan) {
    return (
      <Result
        description="No encontramos el nivel de membresía solicitado."
        error
        title="Membresía no disponible"
      >
        <Go to="/app/memberships">Ver membresías</Go>
      </Result>
    )
  }

  if (plan) {
    const price = getPlanPrice(plan.id)
    const alreadyActive = Boolean(activePlan && activePlan.id === plan.id)
    return (
      <div>
        <MemberHeading
          back="/app/memberships"
          description={plan.descripcion}
          title={plan.nombre}
        />
        <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
          <Panel title="Qué incluye">
            <div className="space-y-5">
              {plan.beneficios.map((benefit) => (
                <p className="flex items-center gap-3 text-sm" key={benefit}>
                  <span className="flex size-8 items-center justify-center rounded-full bg-emerald-50 text-primary">
                    <Check aria-hidden="true" className="size-4" />
                  </span>
                  {benefit}
                </p>
              ))}
            </div>
            <h3 className="mt-8 mb-3 font-semibold">
              Condiciones de contratación
            </h3>
            <ul className="space-y-2 text-sm leading-6 text-muted-foreground">
              {plan.condiciones.map((condition) => (
                <li className="flex gap-2" key={condition}>
                  <span aria-hidden="true">•</span>
                  {condition}
                </li>
              ))}
            </ul>
            <div className="mt-5">
              <Notice title="Validación manual">
                La administración revisa la documentación antes de activar los
                beneficios.
              </Notice>
            </div>
          </Panel>
          <Panel title="Tu membresía">
            <p className="text-3xl font-bold tabular-nums">
              {money(price)}
              <span className="ml-1 text-sm font-normal text-muted-foreground">
                / mes
              </span>
            </p>
            <p className="mt-3 text-sm text-muted-foreground">
              Tarifa de referencia para{" "}
              {currentUser.relacionUnseLabel.toLowerCase()}.
            </p>
            {params.get("state") === "incompatible" || alreadyActive ? (
              <div className="mt-6">
                <Notice error title="Ya tenés una membresía activa">
                  Administrá tu plan actual antes de contratar otro.
                </Notice>
                <TextLink to="/app/memberships/status">
                  Ver mi membresía
                </TextLink>
              </div>
            ) : (
              <>
                <div className="my-6 flex items-start gap-3">
                  <Checkbox
                    checked={accepted}
                    id="accept-plan"
                    onCheckedChange={(checked) => setAccepted(checked === true)}
                  />
                  <Label
                    className="text-sm leading-5 font-normal"
                    htmlFor="accept-plan"
                  >
                    Leí las condiciones y quiero contratar esta membresía.
                  </Label>
                </div>
                <Button
                  className="h-11 w-full"
                  onClick={() => setShowValidation(true)}
                >
                  Continuar al pago
                </Button>
                {showValidation && !accepted && (
                  <p className="mt-3 text-sm text-destructive" role="alert">
                    Aceptá las condiciones para continuar.
                  </p>
                )}
                {showValidation && accepted && (
                  <div className="mt-4">
                    <Go
                      className="w-full"
                      to={
                        "/app/payments?view=checkout&concept=membership&plan=" +
                        plan.id
                      }
                    >
                      Confirmar contratación
                    </Go>
                  </div>
                )}
              </>
            )}
          </Panel>
        </div>
      </div>
    )
  }

  return (
    <div>
      <MemberHeading
        action={
          <Go secondary to="/app/memberships/status">
            Mi membresía
          </Go>
        }
        description="Elegí el nivel que corresponde a tu relación con la UNSE."
        title="Encontrá tu membresía"
      />
      <Notice title="Tu relación con UNSE está verificada">
        {currentUser.relacionUnseLabel}. Los precios que ves corresponden a los
        planes de referencia disponibles.
      </Notice>
      <div className="mt-6 grid gap-5 lg:grid-cols-3">
        {membershipPlans.map((item) => {
          const isCurrent = item.id === membership?.planId
          const price = getPlanPrice(item.id)
          return (
            <Panel
              className={
                isCurrent ? "border-primary/50 ring-1 ring-primary/20" : ""
              }
              key={item.id}
            >
              <div className="flex items-center justify-between">
                <ShieldCheck
                  aria-hidden="true"
                  className="size-8 text-primary"
                />
                {isCurrent && <StateBadge state="Tu plan actual" />}
              </div>
              <h2 className="mt-6 text-xl font-bold">{item.nombre}</h2>
              <p className="mt-2 min-h-10 text-sm text-muted-foreground">
                {item.descripcion}
              </p>
              <p className="my-6 text-3xl font-bold tabular-nums">
                {money(price)}
                <span className="ml-1 text-sm font-normal text-muted-foreground">
                  / mes
                </span>
              </p>
              <div className="mb-7 space-y-4">
                {item.beneficios.map((benefit) => (
                  <p className="flex items-center gap-2 text-sm" key={benefit}>
                    <Check aria-hidden="true" className="size-4 text-primary" />
                    {benefit}
                  </p>
                ))}
              </div>
              <Go
                className="w-full"
                secondary={!isCurrent}
                to={"/app/memberships/" + item.id}
              >
                Ver plan
              </Go>
            </Panel>
          )
        })}
      </div>
      <p className="mt-6 text-xs text-muted-foreground">
        La administración valida manualmente la documentación y la relación con
        la universidad.
      </p>
    </div>
  )
}

export function MembershipStatusPage() {
  const [params, setParams] = useSearchParams()
  const [cancelOpen, setCancelOpen] = useState(false)
  const state = params.get("state")
  const suspended = state === "suspended" || membership?.estado === "suspendida"
  const cancelled = state === "cancelled"

  if (state === "none" || cancelled) {
    return (
      <Result
        description={
          cancelled
            ? "La baja quedó registrada. Ya no se generarán nuevas cuotas de esta membresía."
            : "Consultá los niveles disponibles y elegí el que corresponde a tu relación con la UNSE."
        }
        title={
          cancelled
            ? "Membresía dada de baja"
            : "Todavía no tenés una membresía"
        }
      >
        <Go to="/app/memberships">Ver membresías</Go>
        <Go secondary to="/app">
          Volver al inicio
        </Go>
      </Result>
    )
  }

  if (state === "loading") {
    return <LoadingState label="Consultando el estado de tu membresía" />
  }

  return (
    <div>
      <MemberHeading
        description="Consultá tu estado, los beneficios y el próximo vencimiento."
        title="Mi membresía"
      />
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-5">
          {suspended && (
            <Notice error title="Beneficios suspendidos">
              Tu cuota está vencida. Regularizá el pago para recuperar el
              acceso.
            </Notice>
          )}
          <Panel
            action={
              <StateBadge
                state={
                  suspended
                    ? "Suspendida"
                    : (membership?.estadoLabel ?? "Activa")
                }
              />
            }
            title={membership?.planNombre ?? "Plan Estudiante"}
          >
            <InfoRows
              rows={[
                ["Relación con UNSE", currentUser.relacionUnseLabel],
                [
                  "Fecha de alta",
                  membership ? formatDate(membership.fechaAlta) : "—",
                ],
                [
                  "Cuota mensual",
                  membership ? money(getPlanPrice(membership.planId)) : "—",
                ],
                [
                  "Próximo vencimiento",
                  membership ? formatDate(membership.proximoVencimiento) : "—",
                ],
                [
                  "Estado de cuotas",
                  <StateBadge
                    state={
                      suspended
                        ? "Cuota vencida"
                        : (membership?.estadoCargoLabel ?? "Al día")
                    }
                  />,
                ],
              ]}
            />
            <div className="mt-6 flex flex-wrap gap-3">
              <Go secondary to="/app/card">
                Ver carnet
              </Go>
              <Go secondary to="/app/memberships">
                Consultar otros niveles
              </Go>
            </div>
          </Panel>
          <Panel title="Servicios y beneficios">
            <div className="space-y-4">
              {(membership?.serviciosDisponibles ?? []).map((service) => (
                <p className="flex items-center gap-3 text-sm" key={service}>
                  <Check aria-hidden="true" className="size-4 text-primary" />
                  {service}
                </p>
              ))}
              {!membership?.serviciosDisponibles.length && (
                <p className="text-sm text-muted-foreground">
                  No hay beneficios disponibles mientras la membresía esté
                  suspendida.
                </p>
              )}
            </div>
            <TextLink to="/app/services">
              Consultar condiciones de los espacios
            </TextLink>
          </Panel>
          <Panel title="Dar de baja la membresía">
            <p className="text-sm leading-6 text-muted-foreground">
              Al confirmar la baja, dejás de acceder a los beneficios del plan.
              Podés volver a consultar las membresías disponibles cuando
              quieras.
            </p>
            <Button
              className="mt-4"
              onClick={() => setCancelOpen(true)}
              variant="destructive"
            >
              Dar de baja membresía
            </Button>
          </Panel>
        </div>
        <div className="space-y-5">
          <Panel title={suspended ? "Cuota vencida" : "Próxima cuota"}>
            <p className="text-sm text-muted-foreground">
              {suspended ? "Cuota vencida" : "Próximo período"}
            </p>
            <p className="my-4 text-3xl font-bold tabular-nums">
              {money(getPlanPrice(membership?.planId ?? ""))}
            </p>
            <Go className="w-full" to="/app/payments?view=checkout">
              Pagar cuota
            </Go>
          </Panel>
          <Panel title="Pago recurrente">
            <RefreshCw
              aria-hidden="true"
              className="mb-3 size-6 text-primary"
            />
            <p className="text-sm leading-6 text-muted-foreground">
              Elegí un medio de pago y autorizá el cobro mensual de tu cuota.
            </p>
            <TextLink to="/app/payments?view=recurring">
              Configurar pago recurrente
            </TextLink>
          </Panel>
        </div>
      </div>
      <AlertDialog onOpenChange={setCancelOpen} open={cancelOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Dar de baja tu membresía?</AlertDialogTitle>
            <AlertDialogDescription>
              Perderás los beneficios del plan. La baja no cancela tus reservas
              existentes ni genera una devolución automática.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Conservar membresía</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                setCancelOpen(false)
                setParams({ state: "cancelled" })
              }}
              variant="destructive"
            >
              Confirmar baja
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

export function MemberPaymentsPage() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  const [filter, setFilter] = useState("Todos")
  const [method, setMethod] = useState("Mercado Pago")
  const [authorized, setAuthorized] = useState(false)
  const [selected, setSelected] = useState<
    (typeof memberPayments)[number] | null
  >(null)
  const view =
    params.get("view") ??
    (location.pathname.endsWith("/recurring") ? "recurring" : "")
  const state = params.get("state")
  const isOverdue = state === "overdue"

  if (view === "loading") {
    return <LoadingState label="Cargando historial de pagos" />
  }

  if (view === "result") {
    const rejected =
      state === "rejected" || state === "cancelled" || state === "expired"
    const pending = state === "pending"
    return (
      <Result
        description={
          pending
            ? "Tu pago todavía no fue confirmado. Podés consultar su estado desde Mis pagos."
            : rejected
              ? "La cuota sigue pendiente. Revisá el medio de pago o generá una nueva orden para volver a intentarlo."
              : "Se registró el pago de tu cuota y tu membresía está al día."
        }
        error={rejected}
        title={
          pending
            ? "Pago pendiente de confirmación"
            : state === "rejected"
              ? "El pago fue rechazado"
              : state === "cancelled"
                ? "El pago fue cancelado"
                : state === "expired"
                  ? "La orden de pago venció"
                  : "Pago aprobado"
        }
      >
        <Go to={rejected ? "/app/payments?view=checkout" : "/app/payments"}>
          {rejected ? "Volver a intentar" : "Ver mis pagos"}
        </Go>
        <Go secondary to="/app/memberships/status">
          Ver membresía
        </Go>
      </Result>
    )
  }

  if (view === "recurring-success") {
    return (
      <Result
        description="La autorización mensual quedó registrada en esta demostración. Tu próximo cobro corresponde al siguiente período."
        title="Pago recurrente configurado"
      >
        <Go to="/app/payments">Volver a mis pagos</Go>
      </Result>
    )
  }

  if (view === "checkout" || view === "recurring") {
    const recurring = view === "recurring"
    return (
      <div>
        <MemberHeading
          back="/app/payments"
          description={
            recurring
              ? "Autorizá el cobro mensual de tu cuota."
              : "Revisá el concepto y el importe antes de confirmar."
          }
          title={recurring ? "Configurar pago recurrente" : "Pagar membresía"}
        />
        <div className="mx-auto grid max-w-4xl items-start gap-6 lg:grid-cols-[1fr_320px]">
          <Panel title="Medio de pago">
            <Choice
              label="Medio de pago"
              onChange={setMethod}
              options={["Mercado Pago", "Transferencia bancaria"]}
              value={method}
            />
            <div className="my-6 rounded-lg border bg-muted/30 p-5">
              <CreditCard
                aria-hidden="true"
                className="mb-3 size-6 text-primary"
              />
              <h3 className="font-medium">{method}</h3>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {method === "Mercado Pago"
                  ? "Vas a continuar al paso de confirmación del pago."
                  : "La transferencia quedará pendiente hasta que administración confirme la recepción."}
              </p>
            </div>
            {recurring && (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={authorized}
                  id="recurring-consent"
                  onCheckedChange={(checked) => setAuthorized(checked === true)}
                />
                <Label
                  className="text-sm leading-6 font-normal"
                  htmlFor="recurring-consent"
                >
                  Autorizo el cobro mensual de{" "}
                  {money(getPlanPrice(membership?.planId ?? ""))} para mi
                  membresía.
                </Label>
              </div>
            )}
            {recurring &&
              !authorized &&
              params.get("validation") === "error" && (
                <p className="mt-3 text-sm text-destructive" role="alert">
                  Aceptá la autorización del cobro mensual para continuar.
                </p>
              )}
            <div className="mt-6">
              <Notice title="Vista de demostración">
                Este recorrido muestra la confirmación visual. No se realiza
                ningún cobro.
              </Notice>
            </div>
          </Panel>
          <Panel title="Resumen">
            <InfoRows
              rows={[
                [
                  "Concepto",
                  recurring ? "Cuota mensual" : "Cuota de membresía",
                ],
                ["Membresía", membership?.planNombre ?? "Plan Estudiante"],
                [
                  "Vencimiento",
                  membership ? formatDate(membership.proximoVencimiento) : "—",
                ],
              ]}
            />
            <div className="my-6 flex justify-between border-t pt-5 font-semibold">
              <span>Total</span>
              <span className="text-xl tabular-nums">
                {money(getPlanPrice(membership?.planId ?? ""))}
              </span>
            </div>
            <Button
              className="h-11 w-full"
              onClick={() => {
                if (recurring && !authorized) {
                  setParams({ view: "recurring", validation: "error" })
                  return
                }
                setParams({
                  state:
                    method === "Transferencia bancaria"
                      ? "pending"
                      : "approved",
                  view: recurring ? "recurring-success" : "result",
                })
              }}
            >
              {recurring
                ? "Autorizar pago recurrente"
                : "Confirmar pago de " +
                  money(getPlanPrice(membership?.planId ?? ""))}
            </Button>
            <div className="mt-3">
              <Go className="w-full" secondary to="/app/payments">
                Cancelar
              </Go>
            </div>
          </Panel>
        </div>
      </div>
    )
  }

  const filtered = memberPayments.filter(
    (payment) =>
      filter === "Todos" ||
      (filter === "Cuotas" && payment.concepto === "cuota_mensual") ||
      (filter === "Reservas" && payment.concepto === "reserva")
  )

  return (
    <div>
      <MemberHeading
        action={
          <Go secondary to="/app/payments?view=recurring">
            <RefreshCw aria-hidden="true" />
            Pago recurrente
          </Go>
        }
        description="Tus cuotas, reservas y comprobantes en un solo lugar."
        title="Mis pagos"
      />
      {isOverdue && (
        <div className="mb-5">
          <Notice error title="Tenés una cuota vencida">
            Regularizá el pago para recuperar los beneficios de tu membresía.
          </Notice>
        </div>
      )}
      <div className="mb-6 grid gap-5 lg:grid-cols-2">
        <Panel>
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Estado de cuotas</p>
              <h2 className="mt-3 text-2xl font-bold">
                {isOverdue ? "Cuota vencida" : "Estás al día"}
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {memberPayments[0]
                  ? "Último pago: " + formatDate(memberPayments[0].fecha)
                  : "Todavía no registramos pagos"}
              </p>
            </div>
            <span className="rounded-xl bg-emerald-50 p-3 text-primary">
              <ShieldCheck aria-hidden="true" className="size-6" />
            </span>
          </div>
        </Panel>
        <Panel>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Próxima cuota</p>
              <p className="mt-2 text-2xl font-bold tabular-nums">
                {money(getPlanPrice(membership?.planId ?? ""))}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                Vence el{" "}
                {membership ? formatDate(membership.proximoVencimiento) : "—"}
              </p>
            </div>
            <Go to="/app/payments?view=checkout">Pagar cuota</Go>
          </div>
        </Panel>
      </div>
      <Panel
        action={
          <Choice
            label="Filtrar por concepto"
            onChange={setFilter}
            options={["Todos", "Cuotas", "Reservas"]}
            value={filter}
          />
        }
        title="Historial de pagos"
      >
        {params.get("empty") === "true" || !filtered.length ? (
          <div className="py-10 text-center">
            <p className="font-medium">Todavía no hay pagos para mostrar</p>
            <p className="mt-2 text-sm text-muted-foreground">
              Cuando registres una operación, aparecerá en este historial.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Fecha</TableHead>
                  <TableHead>Concepto</TableHead>
                  <TableHead>Medio de pago</TableHead>
                  <TableHead>Importe</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead>
                    <span className="sr-only">Detalle</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((payment) => (
                  <TableRow key={payment.id}>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {formatDate(payment.fecha)}
                    </TableCell>
                    <TableCell className="font-medium">
                      {payment.conceptoLabel}
                    </TableCell>
                    <TableCell>{payment.medioLabel}</TableCell>
                    <TableCell className="tabular-nums">
                      {money(payment.importe)}
                    </TableCell>
                    <TableCell>
                      <StateBadge state={payment.estadoLabel} />
                    </TableCell>
                    <TableCell>
                      <Button
                        aria-label={"Ver pago " + payment.id}
                        onClick={() => setSelected(payment)}
                        size="icon"
                        variant="ghost"
                      >
                        <FileText aria-hidden="true" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>
      <Dialog
        onOpenChange={(open) => !open && setSelected(null)}
        open={Boolean(selected)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Detalle de pago</DialogTitle>
            <DialogDescription>{selected?.id}</DialogDescription>
          </DialogHeader>
          {selected && (
            <div className="space-y-5">
              <InfoRows
                rows={[
                  ["Concepto", selected.conceptoLabel],
                  ["Fecha", formatDate(selected.fecha)],
                  ["Importe", money(selected.importe)],
                  ["Medio de pago", selected.medioLabel],
                  ["Estado", <StateBadge state={selected.estadoLabel} />],
                ]}
              />
              <Notice title="Comprobante de demostración">
                El comprobante se adjuntará al integrar los pagos del backend.
              </Notice>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
