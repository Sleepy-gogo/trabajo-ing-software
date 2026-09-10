import type { ReactNode } from "react"
import {
  ImageIcon,
  Search,
  Inbox,
  CheckCircle2,
  AlertCircle,
  X,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetClose,
} from "@/components/ui/sheet"
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
} from "@/components/ui/alert-dialog"

export function PageHeader({
  title,
  description,
  actions,
  eyebrow,
}: {
  title: string
  description?: string
  actions?: ReactNode
  eyebrow?: string
}) {
  return (
    <header className="mb-7 flex flex-wrap items-start justify-between gap-4">
      {" "}
      <div className="min-w-0">
        {eyebrow && (
          <p className="mb-2 text-xs text-muted-foreground">{eyebrow}</p>
        )}
        <h1 className="text-2xl font-bold tracking-[-0.035em] text-balance sm:text-[1.75rem]">
          {title}
        </h1>
        {description && (
          <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-muted-foreground">
            {description}
          </p>
        )}
      </div>
      {actions && (
        <div className="flex flex-wrap items-center gap-2">{actions}</div>
      )}
    </header>
  )
}

export function SectionCard({
  title,
  description,
  action,
  children,
  className,
}: {
  title?: string
  description?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={cn("rounded-xl border bg-card p-5", className)}>
      {(title || action) && (
        <div className="mb-5 flex items-start justify-between gap-3">
          <div>
            {title && (
              <h2 className="text-base font-bold tracking-tight">{title}</h2>
            )}
            {description && (
              <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                {description}
              </p>
            )}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function StatCard({
  label,
  value,
  icon: Icon,
  detail,
  trend,
  className,
}: {
  label: string
  value: ReactNode
  icon?: LucideIcon
  detail?: string
  trend?: string
  className?: string
}) {
  return (
    <div className={cn("rounded-xl border bg-card p-5", className)}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium text-muted-foreground">{label}</p>
          <p className="mt-2 text-[1.75rem] leading-tight font-bold tracking-tight tabular-nums">
            {value}
          </p>
        </div>
        {Icon && (
          <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/8 text-primary">
            <Icon className="size-5" aria-hidden="true" />
          </span>
        )}
      </div>
      {(trend || detail) && (
        <p className="mt-3 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          {trend && (
            <span className="font-semibold text-emerald-700">{trend}</span>
          )}
          {detail}
        </p>
      )}
    </div>
  )
}

export type StatusTone = "success" | "warning" | "danger" | "info" | "neutral"
const statusStyles: Record<StatusTone, string> = {
  success: "bg-emerald-50 text-emerald-800",
  warning: "bg-amber-50 text-amber-800",
  danger: "bg-rose-50 text-rose-800",
  info: "bg-blue-50 text-blue-800",
  neutral: "bg-slate-100 text-slate-600",
}
export function StatusBadge({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode
  tone?: StatusTone
  className?: string
}) {
  return (
    <Badge
      className={cn(
        "gap-1.5 border-0 px-2 py-1 text-[11px] font-semibold whitespace-nowrap",
        statusStyles[tone],
        className
      )}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {children}
    </Badge>
  )
}

export function ImagePlaceholder({
  label,
  className,
  asset,
  icon: Icon = ImageIcon,
}: {
  label: string
  className?: string
  asset?: string
  icon?: LucideIcon
}) {
  return (
    <div
      data-image-placeholder={asset ?? label}
      role="img"
      aria-label={`Imagen pendiente: ${label}`}
      className={cn(
        "relative flex aspect-[16/10] items-center justify-center overflow-hidden bg-slate-100 text-slate-400",
        className
      )}
    >
      <div
        aria-hidden="true"
        className="absolute inset-5 rounded-md border border-current/15"
      />
      <div className="relative flex flex-col items-center gap-2 px-5 text-center">
        <Icon aria-hidden="true" className="size-8 stroke-1" />
        <span className="text-xs font-medium">{label}</span>
      </div>
    </div>
  )
}

export function EmptyState({
  title = "No hay resultados",
  description = "Probá con otros filtros para encontrar lo que buscás.",
  action,
  icon: Icon = Inbox,
}: {
  title?: string
  description?: string
  action?: ReactNode
  icon?: LucideIcon
}) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center px-5 py-10 text-center">
      <span className="mb-4 rounded-xl bg-muted p-3">
        <Icon className="size-6 text-muted-foreground" aria-hidden="true" />
      </span>
      <h3 className="text-base font-semibold">{title}</h3>
      <p className="mt-2 mb-5 max-w-sm text-sm leading-relaxed text-muted-foreground">
        {description}
      </p>
      {action}
    </div>
  )
}

export function FeedbackState({
  title,
  description,
  success = true,
  action,
}: {
  title: string
  description: string
  success?: boolean
  action?: ReactNode
}) {
  const Icon = success ? CheckCircle2 : AlertCircle
  return (
    <div role="status" className="mx-auto max-w-md py-10 text-center">
      <Icon
        aria-hidden="true"
        className={cn(
          "mx-auto mb-5 size-14",
          success ? "text-emerald-600" : "text-destructive"
        )}
      />
      <h2 className="text-xl font-bold">{title}</h2>
      <p className="mt-3 mb-6 text-sm leading-relaxed text-muted-foreground">
        {description}
      </p>
      {action}
    </div>
  )
}

export function DataToolbar({
  search,
  onSearchChange,
  placeholder = "Buscar por nombre, DNI o email…",
  children,
}: {
  search?: string
  onSearchChange?: (value: string) => void
  placeholder?: string
  children?: ReactNode
}) {
  return (
    <div className="mb-5 flex flex-wrap items-center gap-3">
      <div className="relative min-w-48 flex-1 sm:max-w-sm">
        <Search
          aria-hidden="true"
          className="absolute top-3 left-3 size-4 text-muted-foreground"
        />
        <Input
          aria-label={placeholder}
          className="h-10 bg-card pl-9"
          placeholder={placeholder}
          value={search}
          onChange={(event) => onSearchChange?.(event.target.value)}
        />
      </div>
      {children}
    </div>
  )
}

export function DetailSheet({
  open,
  onOpenChange,
  title,
  description,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        showCloseButton={false}
        className="w-full! overflow-y-auto sm:max-w-xl!"
      >
        <SheetHeader className="border-b px-6 py-6 pr-14">
          <SheetTitle>{title}</SheetTitle>
          <SheetDescription>
            {description ?? "Consultá los datos y las acciones disponibles."}
          </SheetDescription>
        </SheetHeader>
        <SheetClose
          render={
            <Button
              variant="ghost"
              size="icon"
              className="absolute top-4 right-4"
              aria-label="Cerrar panel"
            />
          }
        >
          <X aria-hidden="true" />
        </SheetClose>
        <div className="px-6 pb-6">{children}</div>
      </SheetContent>
    </Sheet>
  )
}

export function ConfirmationDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Confirmar",
  onConfirm,
  destructive = false,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  confirmLabel?: string
  onConfirm: () => void
  destructive?: boolean
}) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Volver</AlertDialogCancel>
          <Button
            variant={destructive ? "destructive" : "default"}
            onClick={() => {
              onConfirm()
              onOpenChange(false)
            }}
          >
            {confirmLabel}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
