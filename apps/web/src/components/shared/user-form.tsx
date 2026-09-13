import { useState, type FormEvent } from "react"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import {
  ApiError,
  type User,
  type UserInput,
  type UserRole,
  type AccountState,
} from "@/lib/users-api"

export function UserForm({
  user,
  admin = false,
  pending,
  error,
  onSave,
}: {
  user?: User
  admin?: boolean
  pending: boolean
  error: Error | null
  onSave: (data: UserInput) => void
}) {
  const [localError, setLocalError] = useState("")
  const fields = error instanceof ApiError ? error.fields : {}
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    const form = new FormData(event.currentTarget)
    const password = String(form.get("password") ?? "")
    if (new TextEncoder().encode(password).length > 72) {
      setLocalError(
        "La contraseña no puede superar los 72 bytes. Usá menos caracteres."
      )
      return
    }
    setLocalError("")
    onSave({
      nombreCompleto: String(form.get("nombreCompleto")).trim(),
      email: String(form.get("email")).trim(),
      dni: Number(form.get("dni")),
      rol: admin ? (form.get("rol") as UserRole) : (user?.rol ?? "USUARIO"),
      ...(admin && user
        ? { estadoCuenta: form.get("estadoCuenta") as AccountState }
        : {}),
      ...(!user ? { password } : {}),
    })
  }
  return (
    <form onSubmit={submit} className="space-y-5">
      {[
        {
          name: "nombreCompleto",
          label: "Nombre completo",
          type: "text",
          maxLength: 200,
          autoComplete: "name",
        },
        {
          name: "email",
          label: "Email",
          type: "email",
          maxLength: 100,
          autoComplete: "email",
        },
        { name: "dni", label: "DNI", type: "number", autoComplete: "off" },
      ].map((field) => (
        <div className="space-y-2" key={field.name}>
          <Label htmlFor={`user-${field.name}`}>{field.label}</Label>
          <Input
            id={`user-${field.name}`}
            name={field.name}
            type={field.type}
            maxLength={field.maxLength}
            autoComplete={field.autoComplete}
            min={field.name === "dni" ? 1 : undefined}
            max={field.name === "dni" ? 99999999 : undefined}
            required
            defaultValue={
              user?.[field.name as "nombreCompleto" | "email" | "dni"]
            }
            aria-invalid={!!fields[field.name]}
            aria-describedby={
              fields[field.name] ? `${field.name}-error` : undefined
            }
          />
          {fields[field.name] && (
            <p id={`${field.name}-error`} className="text-sm text-destructive">
              {fields[field.name]}
            </p>
          )}
        </div>
      ))}
      {admin && (
        <div className="space-y-2">
          <Label htmlFor="user-rol">Rol</Label>
          <select
            id="user-rol"
            name="rol"
            defaultValue={user?.rol ?? "USUARIO"}
            className="h-11 w-full rounded-md border bg-background px-3"
          >
            <option value="USUARIO">Usuario</option>
            <option value="STAFF">Personal de accesos</option>
            <option value="ADMIN">Administrador</option>
          </select>
        </div>
      )}
      {admin && user && (
        <div className="space-y-2">
          <Label htmlFor="user-state">Estado de cuenta</Label>
          <select
            id="user-state"
            name="estadoCuenta"
            defaultValue={user.estadoCuenta}
            className="h-11 w-full rounded-md border bg-background px-3"
          >
            <option value="ACTIVO">Activo</option>
            <option value="DESHABILITADO">Deshabilitado</option>
            <option value="INACTIVO">Inactivo</option>
          </select>
        </div>
      )}
      {!user && (
        <div className="space-y-2">
          <Label htmlFor="user-password">Contraseña inicial</Label>
          <Input
            id="user-password"
            name="password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
        </div>
      )}
      {(error || localError) && (
        <p role="alert" className="text-sm text-destructive">
          {localError || error?.message}
        </p>
      )}
      <Button type="submit" disabled={pending}>
        {pending ? "Guardando…" : user ? "Guardar cambios" : "Crear usuario"}
      </Button>
    </form>
  )
}
