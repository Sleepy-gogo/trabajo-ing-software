import { useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Download, History, Printer } from "lucide-react"
import { Button } from "@/components/ui/button"
import { EmptyState, PageHeader, SectionCard } from "@/components/shared"
import {
  ErrorMessage,
  Field,
  QueryState,
  SelectField,
} from "@/components/shared/real-data"
import {
  reportsApi,
  reportLabels,
  reportStates,
  type Report,
  type ReportFilters,
  type ReportType,
} from "@/lib/reports-api"
import { spacesApi } from "@/lib/spaces-api"
import { formatDateTime, formatCurrency } from "@/lib/format"

const relationships = [
  "ESTUDIANTE",
  "DOCENTE",
  "NO_DOCENTE",
  "GRADUADO",
  "EXTERNO",
  "VISITANTE",
]
const label = (value: string) => value.replaceAll("_", " ").toLowerCase()
export function ReportsPage() {
  const [params, setParams] = useSearchParams()
  const id = params.get("id")
  const requested = params.get("type") ?? "socios"
  const initial =
    requested in reportLabels ? (requested as ReportType) : "socios"
  const today = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date())
  const [filters, setFilters] = useState<ReportFilters>({
    tipo: initial,
    desde: today.slice(0, 8) + "01",
    hasta: today,
    estado: "",
    relacion: "",
    espacioId: null,
  })
  const client = useQueryClient()
  const spaces = useQuery({
    queryKey: ["spaces"],
    queryFn: ({ signal }) => spacesApi.list("", signal),
  })
  const report = useQuery({
    queryKey: ["report", id],
    queryFn: ({ signal }) => reportsApi.get(id!, signal),
    enabled: !!id,
  })
  const generate = useMutation({
    mutationFn: reportsApi.generate,
    onSuccess: (data) => {
      client.setQueryData(["report", data.id], data)
      void client.invalidateQueries({ queryKey: ["reports"] })
      setParams({ id: data.id, type: data.tipo })
    },
  })
  function update<K extends keyof ReportFilters>(
    key: K,
    value: ReportFilters[K]
  ) {
    setFilters((current) => ({ ...current, [key]: value }))
    generate.reset()
  }
  if (id)
    return (
      <QueryState
        pending={report.isPending}
        error={report.error}
        retry={report.refetch}
      >
        {report.data && (
          <ReportViewer
            report={report.data}
            onBack={() => setParams({ type: filters.tipo })}
          />
        )}
      </QueryState>
    )
  return (
    <div>
      <PageHeader
        title="Informes"
        description="Consultá datos reales, guardá los resultados y exportalos en CSV."
        actions={
          <Button
            variant="outline"
            nativeButton={false}
            role="link"
            render={<Link to="/admin/reports/recent" />}
          >
            <History />
            Informes recientes
          </Button>
        }
      />
      <SectionCard
        title="Generar informe"
        description="El resultado conserva los filtros y los datos al momento de generarlo."
      >
        <form
          onSubmit={(event) => {
            event.preventDefault()
            generate.mutate(filters)
          }}
          className="space-y-5"
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <SelectField
              label="Tipo de informe"
              value={filters.tipo}
              onChange={(event) => {
                setFilters((current) => ({
                  ...current,
                  tipo: event.target.value as ReportType,
                  estado: "",
                  espacioId: null,
                }))
                generate.reset()
              }}
            >
              {Object.entries(reportLabels).map(([key, name]) => (
                <option key={key} value={key}>
                  {name}
                </option>
              ))}
            </SelectField>
            <Field
              label="Desde"
              type="date"
              required
              value={filters.desde}
              max={filters.hasta}
              onChange={(event) => update("desde", event.target.value)}
            />
            <Field
              label="Hasta"
              type="date"
              required
              value={filters.hasta}
              min={filters.desde}
              onChange={(event) => update("hasta", event.target.value)}
            />
            {reportStates[filters.tipo].length > 0 && (
              <SelectField
                label="Estado"
                value={filters.estado}
                onChange={(event) => update("estado", event.target.value)}
              >
                <option value="">Todos</option>
                {reportStates[filters.tipo].map((state) => (
                  <option key={state} value={state}>
                    {label(state)}
                  </option>
                ))}
              </SelectField>
            )}
            <SelectField
              label="Relación con UNSE"
              value={filters.relacion}
              onChange={(event) => update("relacion", event.target.value)}
            >
              <option value="">Todas</option>
              {relationships.map((r) => (
                <option key={r} value={r}>
                  {label(r)}
                </option>
              ))}
            </SelectField>
            {filters.tipo !== "socios" && (
              <SelectField
                label="Espacio"
                value={filters.espacioId ?? ""}
                onChange={(event) =>
                  update("espacioId", event.target.value || null)
                }
              >
                <option value="">Todos</option>
                {spaces.data?.map((space) => (
                  <option key={space.id} value={space.id}>
                    {space.nombre}
                  </option>
                ))}
              </SelectField>
            )}
          </div>
          <p className="text-sm text-muted-foreground">
            {filters.tipo === "socios"
              ? "El período filtra la fecha de alta del socio. El estado y el nivel reflejan la situación actual."
              : filters.tipo === "pagos"
                ? "El período filtra la fecha de creación de la orden. El importe aprobado incluye solo pagos aprobados."
                : filters.tipo === "uso_servicios"
                  ? "Las horas corresponden a reservas confirmadas. Los ingresos cuentan reservas con ingreso registrado; las personas son las declaradas en esas reservas."
                  : "El período filtra la fecha de la reserva. El estado refleja su situación actual."}{" "}
            Fechas de Argentina. Máximo 5000 filas por informe.
          </p>
          <ErrorMessage error={generate.error ?? spaces.error} />
          <Button type="submit" disabled={generate.isPending}>
            {generate.isPending ? "Generando…" : "Generar informe"}
          </Button>
        </form>
      </SectionCard>
    </div>
  )
}

function ReportViewer({
  report,
  onBack,
}: {
  report: Report
  onBack: () => void
}) {
  return (
    <div>
      <PageHeader
        title={`Informe de ${reportLabels[report.tipo].toLowerCase()}`}
        description={`Generado el ${formatDateTime(report.creadoEn)} por ${report.creadoPor}.`}
        actions={
          <>
            <Button variant="outline" onClick={onBack}>
              Volver
            </Button>
            <Button
              nativeButton={false}
              role="link"
              render={<a href={`/api/reportes/${report.id}/csv`} download />}
            >
              <Download />
              Exportar CSV
            </Button>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer />
              Imprimir
            </Button>
          </>
        }
      />
      <SectionCard
        title="Filtros guardados"
        description={`${report.filtros.desde} a ${report.filtros.hasta}`}
      >
        <p className="text-sm text-muted-foreground">
          Estado:{" "}
          {report.filtros.estado ? label(report.filtros.estado) : "todos"}.
          Relación:{" "}
          {report.filtros.relacion ? label(report.filtros.relacion) : "todas"}.
          Espacio: {report.espacioNombre ?? "todos"}.
        </p>
        <p role="status" className="mt-3 text-sm font-semibold">
          {report.resumen.registros} registros
          {report.resumen.importe_aprobado !== undefined
            ? ` · Importe aprobado: ${formatCurrency(report.resumen.importe_aprobado)}`
            : ""}
        </p>
      </SectionCard>
      <div className="mt-5">
        <SectionCard
          title="Resultados"
          description="La exportación contiene estas mismas filas y columnas."
        >
          {report.filas.length === 0 ? (
            <EmptyState
              title="Sin resultados"
              description="No hay registros que coincidan con los filtros guardados."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b">
                    {report.columnas.map((c) => (
                      <th
                        scope="col"
                        className="p-3 whitespace-nowrap"
                        key={c.key}
                      >
                        {c.label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {report.filas.map((row, i) => (
                    <tr
                      key={String(row.id ?? i)}
                      className="border-b last:border-0"
                    >
                      {report.columnas.map((c) => (
                        <td key={c.key} className="p-3">
                          {typeof row[c.key] === "boolean"
                            ? row[c.key]
                              ? "Sí"
                              : "No"
                            : String(row[c.key] ?? "")}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  )
}

export function RecentReportsPage() {
  const [page, setPage] = useState(0)
  const reports = useQuery({
    queryKey: ["reports", page],
    queryFn: ({ signal }) => reportsApi.list(page, signal),
  })
  return (
    <div>
      <PageHeader
        title="Informes recientes"
        description="Resultados guardados al generar cada informe."
        actions={
          <Button
            nativeButton={false}
            role="link"
            render={<Link to="/admin/reports" />}
          >
            Generar informe
          </Button>
        }
      />
      <QueryState
        pending={reports.isPending}
        error={reports.error}
        retry={reports.refetch}
      >
        <SectionCard title="Historial">
          {reports.data?.length ? (
            <div className="divide-y">
              {reports.data.map((report) => (
                <Link
                  className="flex flex-wrap items-center justify-between gap-3 py-4 text-sm hover:text-primary"
                  to={`/admin/reports?id=${report.id}&type=${report.tipo}`}
                  key={report.id}
                >
                  <span>
                    <span className="font-semibold">
                      {reportLabels[report.tipo]}
                    </span>
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {report.filtros.desde} a {report.filtros.hasta} ·{" "}
                      {report.creadoPor}
                    </span>
                  </span>
                  <span>
                    {formatDateTime(report.creadoEn)} · {report.filas.length}{" "}
                    filas
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No hay informes en esta página"
              description="Generá un informe para guardar sus resultados."
            />
          )}
          <div className="mt-5 flex items-center gap-3">
            <Button
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Anterior
            </Button>
            <span className="text-sm">Página {page + 1}</span>
            <Button
              variant="outline"
              disabled={reports.data?.length !== 20}
              onClick={() => setPage((p) => p + 1)}
            >
              Siguiente
            </Button>
          </div>
        </SectionCard>
      </QueryState>
    </div>
  )
}
