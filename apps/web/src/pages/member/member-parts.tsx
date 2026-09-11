import type { ReactNode } from "react"
import { Link } from "react-router-dom"
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CircleAlert,
  LoaderCircle,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { cn } from "@/lib/utils"

export function MemberHeading({
  title,
  description,
  action,
  back,
}: {
  title: string
  description?: string
  action?: ReactNode
  back?: string
}) {
  return (
    <header className="mb-7 flex flex-wrap items-start justify-between gap-4">
      <div className="min-w-0">
        {back && (
          <Link
            className="mb-4 inline-flex min-h-9 items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-primary"
            to={back}
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Volver
          </Link>
        )}
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          {title}
        </h1>
        {description && (
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            {description}
          </p>
        )}
      </div>
      {action && (
        <div className="flex flex-wrap items-center gap-2">{action}</div>
      )}
    </header>
  )
}

export function Panel({
  title,
  action,
  children,
  className,
}: {
  title?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <Card className={cn("gap-0 overflow-hidden py-0 shadow-none", className)}>
      {title && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b px-5 py-4">
          <h2 className="font-semibold">{title}</h2>
          {action}
        </div>
      )}
      <CardContent className="p-5">{children}</CardContent>
    </Card>
  )
}

export function Go({
  to,
  children,
  secondary = false,
  className,
}: {
  to: string
  children: ReactNode
  secondary?: boolean
  className?: string
}) {
  return (
    <Button
      className={cn("min-h-10 px-4", className)}
      nativeButton={false}
      render={<Link to={to} />}
      variant={secondary ? "outline" : "default"}
    >
      {children}
    </Button>
  )
}

export function StateBadge({ state }: { state: string }) {
  const warning =
    /Pendiente|vence|Vencid|mantenimiento|conflicto|No disponible/i.test(state)
  const bad = /Cancelad|Rechazad|Suspendid|Inactiv|No disponible/i.test(state)
  const tone = bad
    ? "bg-rose-50 text-rose-700"
    : warning
      ? "bg-amber-50 text-amber-800"
      : "bg-emerald-50 text-emerald-800"
  const dot = bad ? "bg-rose-500" : warning ? "bg-amber-500" : "bg-emerald-500"

  return (
    <Badge
      className={cn("gap-1.5 border-0 px-2 py-1 font-medium", tone)}
      variant="outline"
    >
      <span aria-hidden="true" className={cn("size-1.5 rounded-full", dot)} />
      {state}
    </Badge>
  )
}

export function Notice({
  title,
  children,
  error = false,
}: {
  title: string
  children: ReactNode
  error?: boolean
}) {
  return (
    <Alert
      className={cn(
        error
          ? "border-rose-200 bg-rose-50 text-rose-950"
          : "border-emerald-200 bg-emerald-50/60 text-emerald-950"
      )}
      role={error ? "alert" : "status"}
    >
      <CircleAlert aria-hidden="true" className="size-4" />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription className="text-current opacity-80">
        {children}
      </AlertDescription>
    </Alert>
  )
}

export function InfoRows({ rows }: { rows: [string, ReactNode][] }) {
  return (
    <dl className="space-y-4 text-sm">
      {rows.map(([label, value]) => (
        <div
          className="flex flex-wrap justify-between gap-x-5 gap-y-1"
          key={label}
        >
          <dt className="text-muted-foreground">{label}</dt>
          <dd className="text-right font-medium">{value}</dd>
        </div>
      ))}
    </dl>
  )
}

export function Choice({
  value,
  onChange,
  options,
  label,
}: {
  value: string
  onChange: (value: string) => void
  options: string[]
  label: string
}) {
  return (
    <Select onValueChange={(next) => next && onChange(next)} value={value}>
      <SelectTrigger aria-label={label} className="h-10 min-w-36">
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

export function Result({
  title,
  description,
  children,
  error = false,
}: {
  title: string
  description: string
  children: ReactNode
  error?: boolean
}) {
  return (
    <Panel className="mx-auto max-w-xl">
      <div className="space-y-5 py-8 text-center" role="status">
        <div
          className={cn(
            "mx-auto flex size-16 items-center justify-center rounded-full",
            error
              ? "bg-rose-100 text-rose-700"
              : "bg-emerald-100 text-emerald-800"
          )}
        >
          {error ? (
            <CircleAlert aria-hidden="true" className="size-8" />
          ) : (
            <Check aria-hidden="true" className="size-8" />
          )}
        </div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        <p className="mx-auto max-w-md text-sm leading-6 text-muted-foreground">
          {description}
        </p>
        <div className="flex flex-wrap justify-center gap-3">{children}</div>
      </div>
    </Panel>
  )
}

export function LoadingState({
  label = "Cargando información",
}: {
  label?: string
}) {
  return (
    <Panel>
      <div
        className="flex min-h-48 flex-col items-center justify-center gap-3 text-center"
        role="status"
      >
        <LoaderCircle
          aria-hidden="true"
          className="size-7 animate-spin text-primary"
        />
        <p className="text-sm text-muted-foreground">{label}…</p>
      </div>
    </Panel>
  )
}

export function TextLink({
  to,
  children,
}: {
  to: string
  children: ReactNode
}) {
  return (
    <Link
      className="inline-flex min-h-9 items-center gap-2 text-sm font-medium text-primary hover:underline"
      to={to}
    >
      {children}
      <ArrowRight aria-hidden="true" className="size-3.5" />
    </Link>
  )
}
