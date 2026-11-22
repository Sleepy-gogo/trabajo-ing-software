import { SectionCard } from "@/components/shared"
import { HorizontalBars } from "./horizontal-bars"
import { formatCurrency } from "@/lib/format"
import { summarize, type SummaryChart } from "@/lib/report-summary"
import type { Report } from "@/lib/reports-api"

const formats: Record<SummaryChart["unit"], (value: number) => string> = {
  count: (v) => v.toLocaleString("es-AR"),
  currency: formatCurrency,
  hours: (v) => `${v.toLocaleString("es-AR")} h`,
  percent: (v) => `${v} %`,
}

/** Resumen y gráficos del informe, calculados con sus filas guardadas. */
export function ReportCharts({ report }: { report: Report }) {
  const summary = summarize(report)
  return (
    <SectionCard
      title="Resumen"
      description="Calculado con los datos guardados al generar el informe."
    >
      <dl className="mb-5 flex flex-wrap gap-x-10 gap-y-3">
        {summary.stats.map((s) => (
          <div key={s.label}>
            <dt className="text-xs text-muted-foreground">{s.label}</dt>
            <dd className="mt-1 text-xl font-bold tabular-nums">{s.value}</dd>
          </div>
        ))}
      </dl>
      {report.filas.length > 0 && (
        <div className="grid gap-6 lg:grid-cols-2">
          {summary.charts.map((chart) => (
            <figure key={chart.title}>
              <figcaption className="mb-3 text-sm font-semibold">
                {chart.title}
              </figcaption>
              <HorizontalBars
                label={chart.title}
                data={chart.data}
                format={formats[chart.unit]}
              />
            </figure>
          ))}
        </div>
      )}
    </SectionCard>
  )
}
