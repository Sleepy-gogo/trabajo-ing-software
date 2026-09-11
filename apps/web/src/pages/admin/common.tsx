import type { ReactNode } from "react"
import { ChevronLeft, ChevronRight, Download } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { EmptyState } from "@/components/shared"

export function FilterSelect({
  label,
  value,
  onChange,
  options,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: string[]
}) {
  return (
    <Select
      value={value}
      onValueChange={(value) => {
        if (value) onChange(value)
      }}
    >
      <SelectTrigger
        aria-label={label}
        className="h-10 min-w-36 bg-card text-xs"
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {options.map((option) => (
          <SelectItem key={option} value={option}>
            {option}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

export function RecordTable({
  columns,
  rows,
  emptyTitle,
  footer = true,
}: {
  columns: string[]
  rows: { key: string; cells: ReactNode[] }[]
  emptyTitle?: string
  footer?: boolean
}) {
  return (
    <div className="overflow-hidden rounded-xl border bg-card">
      {rows.length ? (
        <>
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
                {columns.map((column) => (
                  <TableHead
                    key={column}
                    className="h-11 px-5 text-[11px] font-semibold text-muted-foreground"
                  >
                    {column}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.key} className="h-16">
                  {row.cells.map((cell, index) => (
                    <TableCell key={index} className="px-5 py-3 text-xs">
                      {cell}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {footer && (
            <div className="flex flex-wrap items-center justify-between gap-3 border-t px-5 py-4">
              <p className="text-[11px] text-muted-foreground">
                Mostrando {rows.length} registros
              </p>
              <div className="flex items-center gap-2">
                <Button
                  size="icon-sm"
                  variant="outline"
                  disabled
                  aria-label="Página anterior"
                >
                  <ChevronLeft />
                </Button>
                <Button size="sm" className="size-8">
                  1
                </Button>
                <Button
                  size="icon-sm"
                  variant="outline"
                  disabled
                  aria-label="Página siguiente"
                >
                  <ChevronRight />
                </Button>
              </div>
            </div>
          )}
        </>
      ) : (
        <EmptyState
          title={emptyTitle ?? "No hay registros con estos filtros"}
          description="Cambiá la búsqueda o limpiá los filtros para ver más resultados."
        />
      )}
    </div>
  )
}

export function FieldValue({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div>
      <dt className="mb-1.5 text-[11px] text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium">{children}</dd>
    </div>
  )
}

export function exportCsv(
  name: string,
  columns: string[],
  rows: (string | number)[][]
) {
  const csv =
    "\uFEFF" +
    [columns, ...rows]
      .map((row) =>
        row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(",")
      )
      .join("\r\n")
  const url = URL.createObjectURL(
    new Blob([csv], { type: "text/csv;charset=utf-8;" })
  )
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = `${name}.csv`
  anchor.click()
  URL.revokeObjectURL(url)
}

export function ExportButton({ onClick }: { onClick: () => void }) {
  return (
    <Button variant="outline" onClick={onClick}>
      <Download aria-hidden="true" />
      Exportar
    </Button>
  )
}
