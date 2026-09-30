import { useState, type ReactNode } from "react"
import { Link } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  CalendarDays,
  CalendarCheck,
  CalendarX,
  CreditCard,
  Mail,
  Pencil,
  QrCode,
  ShieldCheck,
  UserRound,
} from "lucide-react"
import { useSession } from "@/hooks/use-session"
import { usersApi, type UserInput } from "@/lib/users-api"
import { membersApi, label } from "@/lib/members-api"
import { reservationsApi } from "@/lib/reservations-api"
import { paymentsApi, paymentStateLabel } from "@/lib/payments-api"
import { formatCurrency, formatDate } from "@/lib/format"
import { UserForm } from "@/components/shared/user-form"
import { ReservationStatus } from "@/components/shared/reservation-status"
import {
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
  EmptyState,
} from "@/components/shared"
import { QueryState } from "@/components/shared/real-data"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

function ProfileDetail({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <div className="rounded-xl bg-muted/60 px-4 py-3">
      <dt className="text-xs text-muted-foreground">{title}</dt>
      <dd className="mt-1 text-sm font-medium break-words">{children}</dd>
    </div>
  )
}

export function MemberProfilePage() {
  const session = useSession()
  const client = useQueryClient()
  const [notice, setNotice] = useState("")
  const [editing, setEditing] = useState(false)
  const member = useQuery({
    queryKey: ["my-member", session.data?.id],
    queryFn: ({ signal }) => membersApi.me(signal),
    enabled: !!session.data,
  })
  const reservations = useQuery({
    queryKey: ["reservations", "mine"],
    queryFn: ({ signal }) => reservationsApi.list(false, signal),
    enabled: !!session.data,
    refetchInterval: 30_000,
  })
  const payments = useQuery({
    queryKey: ["payments", session.data?.id, 0, "TODOS"],
    queryFn: ({ signal }) => paymentsApi.list(0, undefined, signal),
    enabled: !!session.data,
  })
  const save = useMutation({
    mutationFn: (data: UserInput) =>
      usersApi.profile({
        nombreCompleto: data.nombreCompleto,
        email: data.email,
        dni: data.dni,
      }),
    onMutate: () => setNotice(""),
    onSuccess: (user) => {
      client.setQueryData(["session"], user)
      setNotice("Tus datos se guardaron.")
      setEditing(false)
      void Promise.all([
        client.invalidateQueries({ queryKey: ["my-member"] }),
        client.invalidateQueries({ queryKey: ["reservations"] }),
        client.invalidateQueries({ queryKey: ["payments"] }),
      ])
    },
  })
  if (!session.data) return null
  const user = session.data
  const initials = user.nombreCompleto
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .filter((_, index, parts) => index === 0 || index === parts.length - 1)
    .map((part) => part[0])
    .join("")
    .toLocaleUpperCase("es-AR")
  const recentReservations = [...(reservations.data ?? [])]
    .sort((a, b) =>
      `${b.fecha}T${b.desde}`.localeCompare(`${a.fecha}T${a.desde}`)
    )
    .slice(0, 4)
  const recentPayments = [...(payments.data?.content ?? [])]
    .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn))
    .slice(0, 4)
  const membership = member.data
  const hasMembership = !!membership?.membresiaId
  const count = (state?: string) =>
    reservations.isSuccess
      ? reservations.data.filter((r) => !state || r.estado === state).length
      : "—"

  return (
    <div>
      <PageHeader
        title="Mi perfil"
        description="Tu cuenta y tu actividad en el polideportivo."
        actions={
          <Button
            nativeButton={false}
            render={<Link to="/app/reservations/new" />}
            className="h-11 rounded-xl px-4"
          >
            <CalendarDays aria-hidden="true" />
            Reservar espacio
          </Button>
        }
      />
      <p
        role="status"
        className={notice ? "mb-4 text-sm text-emerald-800" : "sr-only"}
      >
        {notice}
      </p>
      <div className="grid items-start gap-5 xl:grid-cols-[300px_minmax(0,1fr)]">
        <SectionCard className="min-w-0 rounded-2xl p-6 shadow-xs">
          <div className="flex flex-col items-center text-center">
            <div
              aria-hidden="true"
              className="mb-5 flex size-24 items-center justify-center rounded-full bg-primary/10 text-3xl font-bold tracking-tight text-primary ring-8 ring-primary/5"
            >
              {initials}
            </div>
            <h2 className="max-w-full text-xl font-bold tracking-tight break-words">
              {user.nombreCompleto}
            </h2>
            <div className="mt-3">
              <StatusBadge
                tone={user.estadoCuenta === "ACTIVO" ? "success" : "warning"}
              >
                {user.estadoCuenta === "ACTIVO"
                  ? "Cuenta activa"
                  : user.estadoCuenta === "INACTIVO"
                    ? "Cuenta inactiva"
                    : "Cuenta deshabilitada"}
              </StatusBadge>
            </div>
            <Dialog
              open={editing}
              onOpenChange={(open) => {
                if (save.isPending) return
                if (open) save.reset()
                setEditing(open)
              }}
            >
              <DialogTrigger
                render={
                  <Button
                    variant="outline"
                    className="mt-5 h-11 w-full rounded-xl bg-card"
                  />
                }
              >
                <Pencil aria-hidden="true" />
                Editar datos
              </DialogTrigger>
              <DialogContent
                showCloseButton={false}
                className="max-h-[calc(100dvh-2rem)] overflow-y-auto overscroll-contain rounded-2xl"
              >
                <DialogHeader>
                  <DialogTitle>Editar mis datos</DialogTitle>
                  <DialogDescription>
                    Actualizá el nombre, el email y el DNI de tu cuenta.
                  </DialogDescription>
                </DialogHeader>
                <UserForm
                  user={user}
                  pending={save.isPending}
                  error={save.error}
                  onSave={(data) => save.mutate(data)}
                />
                <DialogClose
                  render={
                    <Button
                      variant="ghost"
                      disabled={save.isPending}
                      className="h-11"
                    />
                  }
                >
                  Cancelar
                </DialogClose>
              </DialogContent>
            </Dialog>
          </div>
          <dl className="mt-6 space-y-3">
            <ProfileDetail title="Email">
              <span className="flex items-start gap-2">
                <Mail
                  aria-hidden="true"
                  className="mt-0.5 size-4 shrink-0 text-muted-foreground"
                />
                <span className="min-w-0 break-all">{user.email}</span>
              </span>
            </ProfileDetail>
            <ProfileDetail title="DNI">
              <span className="flex items-center gap-2">
                <UserRound
                  aria-hidden="true"
                  className="size-4 text-muted-foreground"
                />
                {user.dni.toLocaleString("es-AR")}
              </span>
            </ProfileDetail>
            <ProfileDetail title="Cuenta creada">
              {formatDate(user.creadoEn)}
            </ProfileDetail>
          </dl>
          <Button
            nativeButton={false}
            variant="ghost"
            render={<Link to="/app/card" />}
            className="mt-5 h-11 w-full rounded-xl text-primary"
          >
            <QrCode aria-hidden="true" />
            Ver mi carnet
          </Button>
        </SectionCard>

        <div className="min-w-0 space-y-5">
          <section
            aria-label="Resumen de reservas"
            className="grid gap-3 sm:grid-cols-3"
          >
            <StatCard
              label="Reservas totales"
              value={count()}
              detail="Tu historial de reservas"
              icon={CalendarDays}
              className="rounded-2xl shadow-xs"
            />
            <StatCard
              label="Utilizadas"
              value={count("CONSUMIDA")}
              detail="Ingresos registrados"
              icon={CalendarCheck}
              className="rounded-2xl shadow-xs"
            />
            <StatCard
              label="Canceladas"
              value={count("CANCELADA")}
              detail="Reservas que cancelaste"
              icon={CalendarX}
              className="rounded-2xl shadow-xs"
            />
          </section>
          <SectionCard
            title="Mi membresía"
            className="rounded-2xl shadow-xs"
            action={
              <ShieldCheck aria-hidden="true" className="size-5 text-primary" />
            }
          >
            <QueryState
              pending={member.isPending}
              error={member.error}
              retry={member.refetch}
            >
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-lg font-bold tracking-tight">
                    {membership?.nivelMembresiaNombre ?? "Sin membresía"}
                  </p>
                  <div className="mt-2 flex flex-wrap items-center gap-3">
                    {hasMembership && (
                      <StatusBadge
                        tone={
                          membership?.estadoMembresia === "ACTIVA"
                            ? "success"
                            : "warning"
                        }
                      >
                        {label(membership?.estadoMembresia)}
                      </StatusBadge>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {membership?.proximoVencimiento
                        ? `Vencimiento: ${formatDate(membership.proximoVencimiento)}`
                        : hasMembership
                          ? "Consultá el estado de tus cuotas."
                          : "Elegí un plan para acceder a sus beneficios."}
                    </p>
                  </div>
                </div>
                <Button
                  nativeButton={false}
                  variant="outline"
                  render={
                    <Link
                      to={
                        hasMembership
                          ? "/app/memberships/status"
                          : "/app/memberships"
                      }
                    />
                  }
                  className="h-11 rounded-xl bg-card"
                >
                  {hasMembership ? "Ver membresía" : "Ver planes"}
                </Button>
              </div>
              {membership && (
                <div className="mt-5 flex flex-wrap items-start justify-between gap-3 border-t pt-4">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium">
                      Relación UNSE: {label(membership.relacionUnse)}
                    </p>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                      {membership.estadoVerificacionUnse === "VERIFICADA"
                        ? "Se aplican las tarifas de tu relación con la UNSE."
                        : "Hasta que administración verifique tu relación, se aplica la tarifa de externo."}
                    </p>
                  </div>
                  <StatusBadge
                    tone={
                      membership.estadoVerificacionUnse === "VERIFICADA"
                        ? "success"
                        : "warning"
                    }
                  >
                    {membership.estadoVerificacionUnse === "VERIFICADA"
                      ? "Verificada"
                      : membership.estadoVerificacionUnse === "RECHAZADA"
                        ? "Verificación rechazada"
                        : "Verificación pendiente"}
                  </StatusBadge>
                </div>
              )}
            </QueryState>
          </SectionCard>
          <SectionCard
            title="Mi actividad"
            description="Tus últimas reservas y pagos."
            className="rounded-2xl shadow-xs"
          >
            <Tabs defaultValue="reservations">
              <TabsList
                variant="line"
                aria-label="Actividad de la cuenta"
                className="mb-5 w-full justify-start border-b"
              >
                <TabsTrigger value="reservations" className="min-h-11 gap-2">
                  <CalendarDays aria-hidden="true" className="size-4" />
                  Reservas
                </TabsTrigger>
                <TabsTrigger value="payments" className="min-h-11 gap-2">
                  <CreditCard aria-hidden="true" className="size-4" />
                  Pagos
                </TabsTrigger>
              </TabsList>
              <TabsContent value="reservations">
                <QueryState
                  pending={reservations.isPending}
                  error={reservations.error}
                  retry={reservations.refetch}
                >
                  {recentReservations.length ? (
                    <ul className="divide-y">
                      {recentReservations.map((reservation) => (
                        <li key={reservation.id}>
                          <Link
                            to={`/app/reservations/${reservation.id}`}
                            className="flex flex-wrap items-center gap-3 rounded-xl px-2 py-4 transition-colors hover:bg-muted/60 focus-visible:outline-2 focus-visible:outline-primary"
                          >
                            <div
                              aria-hidden="true"
                              className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/8 text-primary"
                            >
                              <CalendarDays className="size-5" />
                            </div>
                            <div className="min-w-0 flex-1 basis-40">
                              <p className="font-semibold break-words">
                                {reservation.espacioNombre}
                              </p>
                              <p className="mt-1 text-xs text-muted-foreground">
                                {formatDate(reservation.fecha)} ·{" "}
                                {reservation.desde.slice(0, 5)} a{" "}
                                {reservation.hasta.slice(0, 5)}
                              </p>
                            </div>
                            <div className="flex flex-wrap items-center gap-3">
                              <ReservationStatus state={reservation.estado} />
                              <span className="text-sm font-semibold tabular-nums">
                                {formatCurrency(reservation.total)}
                              </span>
                            </div>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <EmptyState
                      icon={CalendarDays}
                      title="Todavía no tenés reservas"
                      description="Elegí un espacio y un horario para tu próxima visita."
                      action={
                        <Button
                          nativeButton={false}
                          render={<Link to="/app/reservations/new" />}
                        >
                          Reservar espacio
                        </Button>
                      }
                    />
                  )}
                  <Link
                    to="/app/reservations"
                    className="mt-4 inline-flex min-h-11 items-center rounded-md text-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-primary"
                  >
                    Ver todas mis reservas
                  </Link>
                </QueryState>
              </TabsContent>
              <TabsContent value="payments">
                <QueryState
                  pending={payments.isPending}
                  error={payments.error}
                  retry={payments.refetch}
                >
                  {recentPayments.length ? (
                    <ul className="divide-y">
                      {recentPayments.map((payment) => (
                        <li
                          key={payment.id}
                          className="flex flex-wrap items-center gap-3 px-2 py-4"
                        >
                          <div
                            aria-hidden="true"
                            className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/8 text-primary"
                          >
                            <CreditCard className="size-5" />
                          </div>
                          <div className="min-w-0 flex-1 basis-40">
                            <p className="font-semibold">
                              {payment.conceptoPago === "CUOTA_MENSUAL"
                                ? "Cuota de membresía"
                                : payment.conceptoPago === "RESERVA"
                                  ? "Reserva de espacio"
                                  : "Diferencia de ticket"}
                            </p>
                            <p className="mt-1 text-xs text-muted-foreground">
                              {formatDate(payment.creadoEn)} ·{" "}
                              {payment.medioPago === "EFECTIVO"
                                ? "Efectivo"
                                : "Mercado Pago"}
                            </p>
                          </div>
                          <div className="flex flex-wrap items-center gap-3">
                            <StatusBadge
                              tone={
                                payment.requiereRevision ||
                                payment.estado === "PENDIENTE"
                                  ? "warning"
                                  : payment.estado === "APROBADO"
                                    ? "success"
                                    : "danger"
                              }
                            >
                              {payment.requiereRevision
                                ? "En revisión"
                                : paymentStateLabel[payment.estado]}
                            </StatusBadge>
                            <span className="text-sm font-semibold tabular-nums">
                              {formatCurrency(payment.monto)}
                            </span>
                          </div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <EmptyState
                      icon={CreditCard}
                      title="Todavía no tenés pagos"
                      description="Los pagos de membresías y reservas aparecerán acá."
                    />
                  )}
                  <Link
                    to="/app/payments"
                    className="mt-4 inline-flex min-h-11 items-center rounded-md text-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-primary"
                  >
                    Ver todos mis pagos
                  </Link>
                </QueryState>
              </TabsContent>
            </Tabs>
          </SectionCard>
        </div>
      </div>
    </div>
  )
}
