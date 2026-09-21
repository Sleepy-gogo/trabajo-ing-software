import { useId, type ComponentProps, type ReactNode } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
export function Field({
  label,
  ...props
}: ComponentProps<typeof Input> & { label: string }) {
  const id = useId()
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium" htmlFor={id}>
        {label}
      </label>
      <Input id={id} {...props} />
    </div>
  )
}
export function SelectField({
  label,
  children,
  ...props
}: ComponentProps<"select"> & { label: string }) {
  const id = useId()
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium" htmlFor={id}>
        {label}
      </label>
      <select
        id={id}
        className="h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-2 focus-visible:outline-primary"
        {...props}
      >
        {children}
      </select>
    </div>
  )
}
export function ErrorMessage({ error }: { error: Error | null | undefined }) {
  return error ? (
    <p
      role="alert"
      className="my-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
    >
      {error.message}
    </p>
  ) : null
}
export function QueryState({
  pending,
  error,
  retry,
  children,
}: {
  pending: boolean
  error: Error | null
  retry: () => unknown
  children: ReactNode
}) {
  if (pending)
    return (
      <p
        role="status"
        className="rounded-xl border bg-card p-8 text-sm text-muted-foreground"
      >
        Cargando datos…
      </p>
    )
  if (error)
    return (
      <div>
        <ErrorMessage error={error} />
        <Button variant="outline" onClick={() => void retry()}>
          Reintentar
        </Button>
      </div>
    )
  return children
}
export function Note({ children }: { children: ReactNode }) {
  return (
    <p className="my-4 rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm leading-relaxed text-blue-900">
      {children}
    </p>
  )
}
