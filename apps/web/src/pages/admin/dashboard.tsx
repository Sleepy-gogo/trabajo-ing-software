import { Link } from "react-router-dom"
import {
  ArrowRight,
  CalendarDays,
  ChartNoAxesCombined,
  CreditCard,
  DoorOpen,
  Landmark,
  Plus,
  Users,
} from "lucide-react"
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"

import {
  ChartContainer,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
} from "@/components/shared"
import {
  accessRecords,
  dashboardData,
  payments,
  reservations,
  spaces,
} from "@/mocks"
import { formatCurrency } from "@/lib/format"

const chartConfig = {
  reservas: { label: "Reservas", color: "var(--primary)" },
} satisfies ChartConfig

const usageData = [
  { espacio: "Fútbol 5", reservas: 120 },
  { espacio: "Tenis", reservas: 98 },
  { espacio: "Básquet", reservas: 76 },
  { espacio: "Quincho", reservas: 45 },
  { espacio: "SUM", reservas: 23 },
]

const activityIcons = [CalendarDays, CreditCard, DoorOpen]

export function AdminDashboardPage() {
  const approvedIncome = payments
    .filter((payment) => payment.estado === "aprobado")
    .reduce((total, payment) => total + payment.importe, 0)
  const activeSpaces = spaces.filter(
    (space) => space.estado === "en_uso"
  ).length
  const pendingPayments = payments.filter(
    (payment) => payment.estado === "pendiente"
  ).length

  return (
    <div>
      <PageHeader
        eyebrow="Panel de control"
        title={dashboardData.saludo}
        description="Este es el resumen operativo del Polideportivo UNSE."
        actions={
          <Button render={<Link to="/admin/reservations?action=create" />}>
            <Plus aria-hidden="true" />
            Nueva reserva
          </Button>
        }
      />

      <div className="mb-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {dashboardData.metricas.map((metric) => {
          const icon =
            metric.id === "socios-activos"
              ? Users
              : metric.id === "reservas-hoy"
                ? CalendarDays
                : metric.id === "ingresos-mes"
                  ? CreditCard
                  : Landmark
          return (
            <StatCard
              key={metric.id}
              label={metric.label}
              value={metric.value}
              icon={icon}
              trend={metric.trend}
              detail={metric.helper}
            />
          )
        })}
      </div>

      <div className="mb-7 grid gap-5 xl:grid-cols-[1.35fr_1fr]">
        <SectionCard
          title="Actividad de reservas"
          description="Cantidad de reservas registradas por espacio este mes."
          action={
            <Link
              to="/admin/reports?type=uso_servicios"
              className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
            >
              Ver informe <ArrowRight className="size-3.5" />
            </Link>
          }
        >
          <ChartContainer config={chartConfig} className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={usageData}
                margin={{ top: 10, right: 4, bottom: 0, left: -18 }}
              >
                <CartesianGrid
                  vertical={false}
                  strokeDasharray="3 3"
                  stroke="var(--border)"
                />
                <XAxis
                  dataKey="espacio"
                  tickLine={false}
                  axisLine={false}
                  tick={{ fontSize: 11 }}
                />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  tick={{ fontSize: 11 }}
                />
                <Tooltip
                  cursor={{ fill: "var(--muted)" }}
                  content={<ChartTooltipContent />}
                />
                <Bar dataKey="reservas" radius={[5, 5, 0, 0]}>
                  {usageData.map((entry, index) => (
                    <Cell
                      key={entry.espacio}
                      fill={
                        index === 0
                          ? "var(--primary)"
                          : "color-mix(in oklch, var(--primary) 65%, white)"
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartContainer>
        </SectionCard>

        <SectionCard
          title="Actividad reciente"
          description="Últimos movimientos registrados por el sistema."
        >
          <div className="space-y-1">
            {dashboardData.actividadReciente.map((activity, index) => {
              const Icon = activityIcons[index % activityIcons.length]
              return (
                <div
                  key={activity}
                  className="flex items-start gap-3 rounded-lg px-2 py-3 hover:bg-muted/50"
                >
                  <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icon className="size-4" aria-hidden="true" />
                  </span>
                  <div className="min-w-0">
                    <p className="text-sm font-medium">
                      {activity.split(" · ")[0]}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {activity.split(" · ")[1]}
                    </p>
                    <p className="mt-1 text-[11px] text-muted-foreground">
                      Hace {index + 1} hora{index ? "s" : ""}
                    </p>
                  </div>
                </div>
              )
            })}
          </div>
          <Link
            to="/admin/access?tab=history"
            className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
          >
            Ver historial completo <ArrowRight className="size-3.5" />
          </Link>
        </SectionCard>
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <SectionCard
          title="Estado operativo"
          description="Indicadores que requieren seguimiento."
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-medium">Espacios en uso</p>
                <p className="text-xs text-muted-foreground">Ahora mismo</p>
              </div>
              <StatusBadge tone="info">{activeSpaces} espacios</StatusBadge>
            </div>
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-medium">Pagos pendientes</p>
                <p className="text-xs text-muted-foreground">
                  Requieren revisión
                </p>
              </div>
              <StatusBadge tone={pendingPayments ? "warning" : "success"}>
                {pendingPayments || "Sin pendientes"}
              </StatusBadge>
            </div>
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-medium">Accesos registrados</p>
                <p className="text-xs text-muted-foreground">
                  En el período actual
                </p>
              </div>
              <StatusBadge tone="success">
                {accessRecords.length} hoy
              </StatusBadge>
            </div>
          </div>
        </SectionCard>

        <SectionCard
          title="Ingresos registrados"
          description="Suma de pagos aprobados en los datos de demostración."
        >
          <p className="text-3xl font-bold tracking-tight tabular-nums">
            {formatCurrency(approvedIncome)}
          </p>
          <p className="mt-2 text-xs text-muted-foreground">
            Incluye cuotas y reservas confirmadas.
          </p>
          <Button
            render={<Link to="/admin/payments" />}
            variant="outline"
            className="mt-5 w-full"
          >
            Revisar pagos <ArrowRight />
          </Button>
        </SectionCard>

        <SectionCard
          title="Acciones rápidas"
          description="Atajos para las tareas más frecuentes."
        >
          <div className="grid gap-2">
            <Button
              render={<Link to="/admin/members?action=create" />}
              variant="outline"
              className="justify-start"
            >
              <Users />
              Registrar socio
            </Button>
            <Button
              render={<Link to="/admin/spaces?action=create" />}
              variant="outline"
              className="justify-start"
            >
              <Landmark />
              Agregar espacio
            </Button>
            <Button
              render={<Link to="/admin/reports" />}
              variant="outline"
              className="justify-start"
            >
              <ChartNoAxesCombined />
              Generar informe
            </Button>
          </div>
        </SectionCard>
      </div>

      <SectionCard
        title="Próximas reservas"
        description="Reservas confirmadas del próximo turno operativo."
        className="mt-5"
      >
        <div className="overflow-x-auto">
          <div className="min-w-[640px] divide-y">
            {reservations.slice(0, 5).map((reservation) => (
              <div
                key={reservation.id}
                className="grid grid-cols-[1fr_1fr_1fr_auto] items-center gap-4 py-3 text-sm"
              >
                <div>
                  <p className="font-medium">{reservation.spaceName}</p>
                  <p className="text-xs text-muted-foreground">
                    {reservation.ownerName}
                  </p>
                </div>
                <p className="text-muted-foreground">{reservation.fecha}</p>
                <p className="text-muted-foreground">
                  {reservation.inicio} a {reservation.fin}
                </p>
                <StatusBadge
                  tone={
                    reservation.estado === "confirmada"
                      ? "success"
                      : reservation.estado === "pendiente_pago"
                        ? "warning"
                        : "neutral"
                  }
                >
                  {reservation.estadoLabel}
                </StatusBadge>
              </div>
            ))}
          </div>
        </div>
      </SectionCard>
    </div>
  )
}
