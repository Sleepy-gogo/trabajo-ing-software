import { useState } from "react"
import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import { ArrowRight, ArrowUpRight, ShieldCheck } from "lucide-react"
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { PageHeader, SectionCard } from "@/components/shared"
import { QueryState } from "@/components/shared/real-data"
import {
  HorizontalBars,
  SERIES_1,
  SERIES_2,
} from "@/components/reports/horizontal-bars"
import { DemandHeatmap } from "@/components/reports/demand-heatmap"
import {
  activityLabels,
  indicatorsApi,
  periodLabels,
  periodRange,
  type Indicator,
  type PeriodKey,
} from "@/lib/indicators-api"
import { label } from "@/lib/members-api"
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format"
import { cn } from "@/lib/utils"

const incomeConfig = {
  cuotas: { label: "Cuotas de membresía", color: SERIES_1 },
  reservas: { label: "Reservas", color: SERIES_2 },
} satisfies ChartConfig

const monthFormatter = new Intl.DateTimeFormat("es-AR", {
  month: "short",
  year: "2-digit",
  timeZone: "UTC",
})

function today() {
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date())
}

function formatValue(indicator: Pick<Indicator, "unidad">, value: number) {
  switch (indicator.unidad) {
    case "ARS":
      return formatCurrency(value)
    case "porcentaje":
      return `${value.toLocaleString("es-AR", { maximumFractionDigits: 1 })} %`
    case "horas":
      return `${value.toLocaleString("es-AR", { maximumFractionDigits: 1 })} h`
    default:
      return value.toLocaleString("es-AR")
  }
}

function comparison(indicator: Indicator) {
  if (indicator.variacion === null) return "Sin base de comparación"
  const v = indicator.variacion
  const amount = Math.abs(v).toLocaleString("es-AR", {
    maximumFractionDigits: 1,
  })
  if (v === 0) return "Sin cambios"
  return indicator.unidad === "porcentaje"
    ? `${v > 0 ? "+" : "−"}${amount} puntos`
    : `${v > 0 ? "+" : "−"}${amount} %`
}

export function AdminDashboardPage() {
  const [period, setPeriod] = useState<PeriodKey>("30d")
  const range = periodRange(period, today())
  const panel = useQuery({
    queryKey: ["indicators", range.desde, range.hasta],
    queryFn: ({ signal }) =>
      indicatorsApi.get(range.desde, range.hasta, signal),
  })
  const data = panel.data
  const pending =
    data?.sociosPorRelacion.reduce((sum, r) => sum + r.pendientes, 0) ?? 0
  return (
    <>
      <PageHeader
        title="Panel de indicadores"
        description="Cómo viene el polideportivo en el período elegido, comparado con el período anterior de igual duración."
      />
      <div
        role="group"
        aria-label="Período"
        className="mb-6 flex flex-wrap gap-2"
      >
        {(Object.keys(periodLabels) as PeriodKey[]).map((key) => (
          <button
            key={key}
            type="button"
            aria-pressed={period === key}
            onClick={() => setPeriod(key)}
            className={cn(
              "min-h-10 rounded-full border px-4 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
              period === key
                ? "border-primary bg-primary text-primary-foreground"
                : "bg-card hover:border-primary/50"
            )}
          >
            {periodLabels[key]}
          </button>
        ))}
      </div>
      <QueryState
        pending={panel.isPending}
        error={panel.error}
        retry={panel.refetch}
      >
        {data && (
          <div className="space-y-6">
            <p className="text-sm text-muted-foreground">
              Del {formatDate(data.desde)} al {formatDate(data.hasta)},
              comparado con el {formatDate(data.anteriorDesde)} al{" "}
              {formatDate(data.anteriorHasta)}.
            </p>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              {data.indicadores.map((indicator) => (
                <div
                  key={indicator.clave}
                  className="rounded-xl border bg-card p-5"
                >
                  <p className="text-xs font-medium text-muted-foreground">
                    {indicator.nombre}
                  </p>
                  <p className="mt-2 text-2xl font-bold tracking-tight tabular-nums">
                    {formatValue(indicator, indicator.valor)}
                  </p>
                  <p className="mt-3 text-xs text-muted-foreground">
                    <span className="font-semibold text-foreground tabular-nums">
                      {comparison(indicator)}
                    </span>{" "}
                    · antes {formatValue(indicator, indicator.anterior)}
                  </p>
                </div>
              ))}
            </div>
            <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
              <SectionCard
                title="Conclusiones"
                description="Se escriben a partir de los mismos datos y solo cuando hay datos suficientes."
              >
                {data.conclusiones.length ? (
                  <ul className="list-disc space-y-2 pl-5 text-sm leading-relaxed">
                    {data.conclusiones.map((c) => (
                      <li key={c}>{c}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Todavía no hay datos suficientes en este período.
                  </p>
                )}
              </SectionCard>
              <Link
                to="/admin/members?verificacion=PENDIENTE"
                className="group flex items-start justify-between gap-3 rounded-xl border bg-card p-5 transition-colors hover:border-primary/50"
              >
                <div>
                  <p className="text-xs font-medium text-muted-foreground">
                    Relaciones por verificar
                  </p>
                  <p className="mt-2 text-[1.75rem] leading-tight font-bold tabular-nums">
                    {pending}
                  </p>
                  <p className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-primary">
                    Ver las personas que esperan
                    <ArrowRight className="size-3.5" aria-hidden="true" />
                  </p>
                </div>
                <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/8 text-primary">
                  <ShieldCheck className="size-5" aria-hidden="true" />
                </span>
              </Link>
            </div>
            <SectionCard
              title="Ingresos aprobados por mes"
              description="Pagos aprobados según su fecha de aprobación, separados en cuotas de membresía y reservas. Muestra al menos los últimos seis meses."
            >
              <ChartContainer
                config={incomeConfig}
                className="aspect-auto h-72 w-full"
              >
                <BarChart
                  data={data.ingresosPorMes}
                  margin={{ top: 8, right: 8, bottom: 0, left: 8 }}
                >
                  <CartesianGrid vertical={false} stroke="var(--border)" />
                  <XAxis
                    dataKey="mes"
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(mes: string) =>
                      monthFormatter.format(new Date(`${mes}-15T12:00:00Z`))
                    }
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    width={84}
                    tickFormatter={(v: number) => formatCurrency(v)}
                  />
                  <ChartTooltip
                    cursor={{ fill: "var(--muted)" }}
                    content={
                      <ChartTooltipContent
                        labelFormatter={(_, payload) =>
                          monthFormatter.format(
                            new Date(
                              `${String(payload?.[0]?.payload?.mes)}-15T12:00:00Z`
                            )
                          )
                        }
                        formatter={(value, name) => (
                          <span className="flex w-full justify-between gap-4">
                            <span>
                              {incomeConfig[name as keyof typeof incomeConfig]
                                ?.label ?? name}
                            </span>
                            <span className="font-medium tabular-nums">
                              {formatCurrency(Number(value))}
                            </span>
                          </span>
                        )}
                      />
                    }
                  />
                  <ChartLegend content={<ChartLegendContent />} />
                  <Bar
                    dataKey="cuotas"
                    stackId="ingresos"
                    isAnimationActive={false}
                    fill={SERIES_1}
                    stroke="var(--card)"
                    strokeWidth={2}
                    maxBarSize={48}
                  />
                  <Bar
                    dataKey="reservas"
                    stackId="ingresos"
                    isAnimationActive={false}
                    fill={SERIES_2}
                    stroke="var(--card)"
                    strokeWidth={2}
                    radius={[4, 4, 0, 0]}
                    maxBarSize={48}
                  />
                </BarChart>
              </ChartContainer>
            </SectionCard>
            <div className="grid gap-6 xl:grid-cols-2">
              <SectionCard
                title="Horas reservadas por espacio"
                description="Reservas confirmadas del período."
              >
                <HorizontalBars
                  label="Horas reservadas"
                  data={data.espacios.map((e) => ({
                    name: e.espacio,
                    value: e.horas,
                  }))}
                  format={(v) =>
                    `${v.toLocaleString("es-AR", { maximumFractionDigits: 1 })} h`
                  }
                />
              </SectionCard>
              <SectionCard
                title="Ingresos por reservas de cada espacio"
                description="Pagos de reservas aprobados en el período."
              >
                <HorizontalBars
                  label="Ingresos"
                  data={data.espacios.map((e) => ({
                    name: e.espacio,
                    value: e.ingresos,
                  }))}
                  format={formatCurrency}
                />
              </SectionCard>
            </div>
            <SectionCard
              title="Demanda por día y hora"
              description="Cantidad de reservas confirmadas que ocupan cada hora. Una reserva de 18 a 20 suma en las 18 y en las 19."
            >
              <DemandHeatmap cells={data.demanda} />
            </SectionCard>
            <div className="grid gap-6 xl:grid-cols-3">
              <SectionCard
                title="Asistencia"
                description="Reservas confirmadas que ya terminaron y tuvieron un ingreso registrado por el personal."
              >
                {data.asistencia.terminadas ? (
                  <>
                    <p className="text-3xl font-bold tabular-nums">
                      {data.asistencia.porcentaje.toLocaleString("es-AR", {
                        maximumFractionDigits: 1,
                      })}{" "}
                      %
                    </p>
                    <div
                      className="mt-4 h-2 rounded-full bg-muted"
                      aria-hidden="true"
                    >
                      <div
                        className="h-2 rounded-full"
                        style={{
                          width: `${data.asistencia.porcentaje}%`,
                          background: SERIES_1,
                        }}
                      />
                    </div>
                    <p className="mt-3 text-sm text-muted-foreground">
                      {data.asistencia.conIngreso} de{" "}
                      {data.asistencia.terminadas} reservas terminadas
                      registraron ingreso.
                    </p>
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Ninguna reserva del período terminó todavía.
                  </p>
                )}
              </SectionCard>
              <SectionCard
                title="Socios por relación con la UNSE"
                description="Cuentas activas de usuarios. Entre paréntesis, las pendientes de verificar."
              >
                <HorizontalBars
                  label="Socios"
                  data={data.sociosPorRelacion.map((r) => ({
                    name: `${label(r.relacion)}${r.pendientes ? ` (${r.pendientes})` : ""}`,
                    value: r.cantidad,
                  }))}
                />
              </SectionCard>
              <SectionCard
                title="Membresías activas por nivel"
                description="Con una cuota aprobada que cubre el último día del período."
              >
                <HorizontalBars
                  label="Membresías activas"
                  data={data.membresiasPorNivel.map((m) => ({
                    name: m.nivel,
                    value: m.cantidad,
                  }))}
                />
              </SectionCard>
            </div>
            <SectionCard
              title="Últimas acciones registradas"
              description="Registro de actividad de los usuarios."
              action={
                <Link
                  to="/admin/reports?type=actividad"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                >
                  Informe de actividad
                  <ArrowUpRight className="size-3.5" aria-hidden="true" />
                </Link>
              }
            >
              {data.ultimasAcciones.length ? (
                <ul className="divide-y text-sm">
                  {data.ultimasAcciones.map((a, i) => (
                    <li
                      key={`${a.creadoEn}-${i}`}
                      className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 py-3"
                    >
                      <span>
                        <span className="font-semibold">
                          {activityLabels[a.tipo] ?? a.tipo}
                        </span>
                        <span className="text-muted-foreground">
                          {" "}
                          · {a.usuario ?? "Sin correo"}
                          {a.detalle ? ` · ${a.detalle}` : ""}
                        </span>
                      </span>
                      <span className="text-xs text-muted-foreground tabular-nums">
                        {formatDateTime(a.creadoEn)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Todavía no hay acciones registradas.
                </p>
              )}
            </SectionCard>
          </div>
        )}
      </QueryState>
    </>
  )
}
