import { Bar, BarChart, CartesianGrid, LabelList, XAxis, YAxis } from "recharts"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"

export const SERIES_1 = "#2a78d6"
export const SERIES_2 = "#eb6834"

/**
 * Barras horizontales de una sola serie, para comparar cantidades entre categorías. El título
 * de la tarjeta nombra la serie, así que no lleva leyenda.
 */
export function HorizontalBars({
  data,
  label,
  format = (value) => String(value),
  color = SERIES_1,
}: {
  data: { name: string; value: number }[]
  label: string
  format?: (value: number) => string
  color?: string
}) {
  const config = { value: { label, color } } satisfies ChartConfig
  if (data.length === 0)
    return (
      <p className="text-sm text-muted-foreground">Sin datos para mostrar.</p>
    )
  return (
    <ChartContainer
      config={config}
      className="aspect-auto w-full"
      style={{ height: data.length * 38 + 16 }}
      role="img"
      aria-label={`${label}: ${data.map((d) => `${d.name} ${format(d.value)}`).join(", ")}`}
    >
      <BarChart
        data={data}
        layout="vertical"
        margin={{ top: 0, right: 72, bottom: 0, left: 0 }}
        barCategoryGap={8}
      >
        <CartesianGrid horizontal={false} stroke="var(--border)" />
        <XAxis type="number" hide />
        <YAxis
          type="category"
          dataKey="name"
          width={150}
          tickLine={false}
          axisLine={false}
          tick={{ fontSize: 12 }}
        />
        <ChartTooltip
          cursor={{ fill: "var(--muted)" }}
          content={
            <ChartTooltipContent
              formatter={(value) => (
                <span className="font-medium tabular-nums">
                  {label}: {format(Number(value))}
                </span>
              )}
            />
          }
        />
        <Bar
          isAnimationActive={false}
          dataKey="value"
          fill={color}
          radius={[0, 4, 4, 0]}
          maxBarSize={22}
        >
          <LabelList
            dataKey="value"
            position="right"
            className="fill-foreground"
            fontSize={12}
            formatter={(value) => format(Number(value))}
          />
        </Bar>
      </BarChart>
    </ChartContainer>
  )
}
