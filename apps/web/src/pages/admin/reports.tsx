import { useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
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
  ArrowLeft,
  BarChart3,
  CalendarDays,
  Download,
  FileBarChart,
  FileText,
  History,
  Printer,
  RefreshCw,
  Users,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  ChartContainer,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import {
  DataToolbar,
  EmptyState,
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
} from "@/components/shared"
import { ExportButton, FilterSelect, exportCsv } from "./common"
import { getReportById, recentReports, reports } from "@/mocks"
import type { Report, ReportType } from "@/types"
import { formatDateTime } from "@/lib/format"

const reportTypeMeta: Record<
  ReportType,
  { label: string; description: string; icon: typeof Users }
> = {
  socios: {
    label: "Socios",
    description: "Estado, relación con la UNSE y nivel de membresía.",
    icon: Users,
  },
  reservas: {
    label: "Reservas",
    description: "Reservas por estado, fecha y espacio.",
    icon: CalendarDays,
  },
  pagos: {
    label: "Pagos",
    description: "Ingresos, cuotas y medios de pago.",
    icon: FileText,
  },
  uso_servicios: {
    label: "Uso de servicios",
    description: "Ocupación y cantidad de usos por espacio.",
    icon: BarChart3,
  },
}

const reportChartConfig = {
  valor: { label: "Valor", color: "var(--primary)" },
} satisfies ChartConfig

function reportTone(report: Report) {
  return report.estado === "con_datos"
    ? ("success" as const)
    : report.estado === "sin_resultados"
      ? ("warning" as const)
      : report.estado === "error"
        ? ("danger" as const)
        : ("info" as const)
}

export function ReportsPage() {
  const [params, setParams] = useSearchParams()
  const selectedId = params.get("id")
  const initialType = (params.get("type") as ReportType | null) ?? "socios"
  const [type, setType] = useState<ReportType>(
    initialType && initialType in reportTypeMeta ? initialType : "socios"
  )
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState("Todos")
  const [notice, setNotice] = useState("")
  const selected = selectedId ? getReportById(selectedId) : undefined
  const typeReports = useMemo(
    () =>
      reports.filter(
        (report) =>
          report.tipo === type &&
          report.nombre.toLowerCase().includes(search.toLowerCase()) &&
          (status === "Todos" || report.estadoLabel === status)
      ),
    [search, status, type]
  )

  function selectType(next: ReportType) {
    setType(next)
    setParams({ type: next })
  }

  function openReport(report: Report) {
    setParams({ id: report.id, type: report.tipo })
    setType(report.tipo)
  }

  return selected ? (
    <ReportViewer
      report={selected}
      onBack={() => setParams({ type: selected.tipo })}
    />
  ) : (
    <div>
      <PageHeader
        title="Informes"
        description="Generá vistas operativas y consultá informes guardados."
        actions={
          <Button
            render={<Link to="/admin/reports/recent" />}
            variant="outline"
          >
            <History />
            Informes recientes
          </Button>
        }
      />
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Informes disponibles"
          value={reports.length}
          icon={FileBarChart}
          detail="Por tipo de operación"
        />
        <StatCard
          label="Con datos"
          value={
            reports.filter((report) => report.estado === "con_datos").length
          }
          icon={BarChart3}
          detail="Listos para exportar"
        />
        <StatCard
          label="Último informe"
          value="26 ago"
          icon={History}
          detail="Generado por Alejandro Méndez"
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
      <Tabs
        value={type}
        onValueChange={(value) => {
          if (value) selectType(value as ReportType)
        }}
      >
        <TabsList className="mb-5 grid w-full grid-cols-2 sm:w-auto sm:grid-cols-4">
          <TabsTrigger value="socios">Socios</TabsTrigger>
          <TabsTrigger value="reservas">Reservas</TabsTrigger>
          <TabsTrigger value="pagos">Pagos</TabsTrigger>
          <TabsTrigger value="uso_servicios">Uso de servicios</TabsTrigger>
        </TabsList>
        {(Object.keys(reportTypeMeta) as ReportType[]).map((reportType) => (
          <TabsContent key={reportType} value={reportType}>
            <div className="grid gap-5 lg:grid-cols-[1fr_300px]">
              <SectionCard
                title={`Reporte de ${reportTypeMeta[reportType].label.toLowerCase()}`}
                description={reportTypeMeta[reportType].description}
              >
                <DataToolbar
                  search={search}
                  onSearchChange={setSearch}
                  placeholder="Buscar informe…"
                >
                  <FilterSelect
                    label="Filtrar estado"
                    value={status}
                    onChange={setStatus}
                    options={["Todos", "Con datos", "Sin resultados"]}
                  />
                </DataToolbar>
                {typeReports.length ? (
                  <div className="space-y-3">
                    {typeReports.map((report) => (
                      <button
                        key={report.id}
                        type="button"
                        onClick={() => openReport(report)}
                        className="flex w-full items-start justify-between gap-4 rounded-lg border p-4 text-left transition-colors hover:border-primary/50 hover:bg-muted/30"
                      >
                        <span className="flex min-w-0 items-start gap-3">
                          <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                            {(() => {
                              const Icon = reportTypeMeta[report.tipo].icon
                              return <Icon className="size-4" />
                            })()}
                          </span>
                          <span className="min-w-0">
                            <span className="block truncate text-sm font-semibold">
                              {report.nombre}
                            </span>
                            <span className="mt-1 block text-xs leading-relaxed text-muted-foreground">
                              {report.descripcion}
                            </span>
                            <span className="mt-2 block text-[11px] text-muted-foreground">
                              {formatDateTime(report.creadoEn)} ·{" "}
                              {report.creadoPor}
                            </span>
                          </span>
                        </span>
                        <StatusBadge tone={reportTone(report)}>
                          {report.estadoLabel}
                        </StatusBadge>
                      </button>
                    ))}
                  </div>
                ) : (
                  <EmptyState
                    title="No hay informes con estos filtros"
                    description="Probá con otra búsqueda o generá un nuevo informe."
                  />
                )}
              </SectionCard>
              <ReportGenerator
                type={reportType}
                onGenerate={() =>
                  setNotice(
                    `El ${reportTypeMeta[reportType].label.toLowerCase()} se generó en esta vista previa.`
                  )
                }
              />
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}

function ReportGenerator({
  type,
  onGenerate,
}: {
  type: ReportType
  onGenerate: () => void
}) {
  return (
    <SectionCard
      title="Generar informe"
      description="Elegí los filtros y generá una vista para revisar o exportar."
      className="h-fit"
    >
      <div className="space-y-4">
        <div className="space-y-2">
          <label htmlFor="report-date" className="text-xs font-semibold">
            Período
          </label>
          <Input id="report-date" type="month" defaultValue="2026-08" />
        </div>
        <FilterSelect
          label="Estado"
          value="Todos"
          onChange={() => undefined}
          options={["Todos", "Activos", "Pendientes", "Cancelados"]}
        />
        <FilterSelect
          label="Espacio"
          value="Todos"
          onChange={() => undefined}
          options={[
            "Todos",
            "Cancha Fútbol 5",
            "Cancha Tenis",
            "Pileta",
            "SUM",
          ]}
        />
        <div className="rounded-lg bg-muted/60 p-3 text-xs leading-relaxed text-muted-foreground">
          Los filtros disponibles se adaptan al tipo de informe seleccionado:{" "}
          {reportTypeMeta[type].label}.
        </div>
        <Button className="w-full" onClick={onGenerate}>
          <RefreshCw />
          Generar vista previa
        </Button>
      </div>
    </SectionCard>
  )
}

function ReportViewer({
  report,
  onBack,
}: {
  report: Report
  onBack: () => void
}) {
  const [notice, setNotice] = useState("")
  const chartData = report.filas.map((row) => {
    const firstLabel = String(row[report.columnas[0]?.key ?? ""] ?? "")
    const numeric = report.columnas
      .map((column) => row[column.key])
      .find((value): value is number => typeof value === "number")
    return { label: firstLabel, valor: numeric ?? 0 }
  })
  return (
    <div>
      <PageHeader
        eyebrow="Informe guardado"
        title={report.nombre}
        description={report.descripcion}
        actions={
          <>
            <Button variant="outline" onClick={onBack}>
              <ArrowLeft />
              Volver
            </Button>
            <ExportButton
              onClick={() => {
                exportCsv(
                  report.archivo ?? report.id,
                  report.columnas.map((column) => column.label),
                  report.filas.map((row) =>
                    report.columnas.map((column) =>
                      String(row[column.key] ?? "")
                    )
                  )
                )
                setNotice("La exportación se preparó en formato CSV.")
              }}
            />
            <Button
              variant="outline"
              onClick={() =>
                setNotice("La vista imprimible está lista para abrir.")
              }
            >
              <Printer />
              Imprimir
            </Button>
          </>
        }
      />
      {notice && (
        <p
          role="status"
          className="mb-5 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-xs text-blue-800"
        >
          {notice}
        </p>
      )}
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Estado"
          value={report.estadoLabel}
          icon={FileBarChart}
        />
        <StatCard
          label="Registros"
          value={report.filas.length}
          icon={BarChart3}
          detail={
            report.filas.length ? "Filas disponibles" : "Sin coincidencias"
          }
        />
        <StatCard label="Creado por" value={report.creadoPor} icon={Users} />
      </div>
      <div className="grid gap-5 xl:grid-cols-[1fr_380px]">
        <SectionCard
          title="Resultados"
          description={`Generado el ${formatDateTime(report.creadoEn)}.`}
        >
          {report.filas.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] text-left text-xs">
                <thead>
                  <tr className="border-b text-muted-foreground">
                    {report.columnas.map((column) => (
                      <th key={column.key} className="px-3 py-3 font-semibold">
                        {column.label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {report.filas.map((row, index) => (
                    <tr
                      key={`${report.id}-${index}`}
                      className="border-b last:border-0"
                    >
                      {report.columnas.map((column) => (
                        <td key={column.key} className="px-3 py-3">
                          {String(row[column.key] ?? "—")}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              title="No hay resultados"
              description="Los filtros guardados no encontraron registros para este período."
            />
          )}
        </SectionCard>
        <SectionCard
          title="Resumen visual"
          description="La gráfica acompaña la lectura; el detalle completo está en la tabla."
        >
          {chartData.length ? (
            <ChartContainer config={reportChartConfig} className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={chartData}
                  margin={{ top: 10, right: 4, bottom: 0, left: -18 }}
                >
                  <CartesianGrid
                    vertical={false}
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                  />
                  <XAxis
                    dataKey="label"
                    tickLine={false}
                    axisLine={false}
                    tick={{ fontSize: 10 }}
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tick={{ fontSize: 10 }}
                  />
                  <Tooltip
                    cursor={{ fill: "var(--muted)" }}
                    content={<ChartTooltipContent />}
                  />
                  <Bar dataKey="valor" radius={[5, 5, 0, 0]}>
                    {chartData.map((entry) => (
                      <Cell key={entry.label} fill="var(--primary)" />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </ChartContainer>
          ) : (
            <EmptyState
              title="No hay datos para graficar"
              description="Cuando el informe tenga filas numéricas, vas a ver un resumen aquí."
            />
          )}
        </SectionCard>
      </div>
      <SectionCard title="Filtros aplicados" className="mt-5">
        <div className="flex flex-wrap gap-2">
          {Object.entries(report.filtros).map(([label, value]) => (
            <span
              key={label}
              className="rounded-full bg-muted px-3 py-1.5 text-xs"
            >
              <strong className="mr-1 capitalize">{label}:</strong>
              {value}
            </span>
          ))}
        </div>
      </SectionCard>
    </div>
  )
}

export function RecentReportsPage() {
  const [params, setParams] = useSearchParams()
  const selected = params.get("id")
    ? getReportById(params.get("id") ?? "")
    : undefined
  if (selected) {
    return <ReportViewer report={selected} onBack={() => setParams({})} />
  }
  return (
    <div>
      <PageHeader
        title="Informes recientes"
        description="Cargá un informe generado anteriormente para volver a consultarlo."
        actions={
          <Button render={<Link to="/admin/reports" />} variant="outline">
            <ArrowLeft />
            Todos los informes
          </Button>
        }
      />
      {recentReports.length ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {recentReports.map((report) => (
            <SectionCard
              key={report.id}
              title={report.nombre}
              description={report.descripcion}
              action={
                <StatusBadge tone={reportTone(report)}>
                  {report.estadoLabel}
                </StatusBadge>
              }
            >
              <p className="text-xs text-muted-foreground">
                {formatDateTime(report.creadoEn)} · {report.creadoPor}
              </p>
              <div className="mt-5 flex flex-wrap gap-2">
                <Button onClick={() => setParams({ id: report.id })}>
                  <FileText />
                  Cargar informe
                </Button>
                <Button
                  variant="outline"
                  onClick={() =>
                    exportCsv(
                      report.archivo ?? report.id,
                      report.columnas.map((column) => column.label),
                      report.filas.map((row) =>
                        report.columnas.map((column) =>
                          String(row[column.key] ?? "")
                        )
                      )
                    )
                  }
                >
                  <Download />
                  CSV
                </Button>
              </div>
            </SectionCard>
          ))}
        </div>
      ) : (
        <SectionCard>
          <EmptyState
            title="No hay informes recientes"
            description="Todavía no se generaron informes para este usuario."
          />
        </SectionCard>
      )}
      {params.get("id") && <RecentReportNotice />}
    </div>
  )
}

function RecentReportNotice() {
  return (
    <p
      role="status"
      className="mt-5 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-xs text-blue-800"
    >
      Informe cargado. Volvé a la pantalla de informes para consultar el detalle
      completo.
    </p>
  )
}
