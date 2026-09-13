import { useEffect, useState, type FormEvent } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate, useParams, useSearchParams } from "react-router-dom"
import { usersApi, type UserInput } from "@/lib/users-api"
import { PageHeader, SectionCard, DetailSheet } from "@/components/shared"
import { UserForm } from "@/components/shared/user-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

const roleLabels = {
  ADMIN: "Administrador",
  STAFF: "Personal de accesos",
  USUARIO: "Usuario",
}
const stateLabels = {
  ACTIVO: "Activo",
  DESHABILITADO: "Deshabilitado",
  INACTIVO: "Inactivo",
}

export function UsersPage() {
  const [params, setParams] = useSearchParams()
  const route = useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [search, setSearch] = useState("")
  const [criterion, setCriterion] = useState("")
  const [notice, setNotice] = useState("")
  const [confirm, setConfirm] = useState(false)
  const id = route.id ?? params.get("id") ?? undefined
  const creating = params.get("action") === "create"
  const list = useQuery({
    queryKey: ["users", criterion],
    queryFn: ({ signal }) => usersApi.list(criterion, signal),
  })
  const detail = useQuery({
    queryKey: ["user", id],
    queryFn: ({ signal }) => usersApi.get(id!, signal),
    enabled: !!id,
  })
  useEffect(() => {
    const timer = setTimeout(() => setCriterion(search), 300)
    return () => clearTimeout(timer)
  }, [search])
  async function refresh() {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: ["user"] }),
      client.invalidateQueries({ queryKey: ["session"] }),
    ])
  }
  const save = useMutation({
    mutationFn: (data: UserInput) =>
      usersApi.save(data, creating ? undefined : id),
    onSuccess: async (user) => {
      await refresh()
      setParams({ id: user.id })
      setNotice("Los datos del usuario se guardaron.")
    },
  })
  const deactivate = useMutation({
    mutationFn: () => usersApi.deactivate(id!),
    onSuccess: async () => {
      await refresh()
      setConfirm(false)
      setNotice("La cuenta quedó inactiva.")
    },
  })
  const password = useMutation({
    mutationFn: (value: string) => usersApi.password(id!, value),
    onSuccess: () => setNotice("La contraseña se actualizó."),
  })
  function close() {
    navigate("/admin/users")
    setConfirm(false)
    save.reset()
    password.reset()
    deactivate.reset()
  }
  function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password.isPending) return
    const form = event.currentTarget
    password.mutate(String(new FormData(form).get("password")), {
      onSuccess: () => form.reset(),
    })
  }
  return (
    <div>
      <PageHeader
        title="Usuarios"
        description="Administrá cuentas, roles y acceso al sistema."
        actions={
          <Button
            onClick={() => {
              navigate("/admin/users?action=create")
              save.reset()
              setNotice("")
            }}
          >
            Nuevo usuario
          </Button>
        }
      />
      <p role="status" className="mb-4 text-sm">
        {notice}
      </p>
      <Input
        aria-label="Buscar por nombre, email o DNI"
        placeholder="Buscar por nombre, email o DNI"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        className="mb-5 max-w-md"
      />
      {list.isPending ? (
        <p role="status">Cargando usuarios…</p>
      ) : list.isError ? (
        <div>
          <p role="alert">{list.error.message}</p>
          <Button onClick={() => void list.refetch()}>Reintentar</Button>
        </div>
      ) : (
        <SectionCard>
          {list.data.length === 0 ? (
            <p>No hay usuarios que coincidan con la búsqueda.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <caption className="sr-only">Cuentas registradas</caption>
                <thead>
                  <tr>
                    {[
                      "Nombre",
                      "Email",
                      "DNI",
                      "Rol",
                      "Estado",
                      "Acciones",
                    ].map((label) => (
                      <th key={label} scope="col" className="p-3">
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {list.data.map((user) => (
                    <tr key={user.id} className="border-t">
                      <td className="p-3">{user.nombreCompleto}</td>
                      <td className="p-3">{user.email}</td>
                      <td className="p-3">{user.dni}</td>
                      <td className="p-3">{roleLabels[user.rol]}</td>
                      <td className="p-3">{stateLabels[user.estadoCuenta]}</td>
                      <td className="p-3">
                        <Button
                          variant="outline"
                          onClick={() => {
                            navigate(`/admin/users/${user.id}`)
                            save.reset()
                            password.reset()
                            deactivate.reset()
                            setConfirm(false)
                            setNotice("")
                          }}
                        >
                          Ver y editar
                          <span className="sr-only">
                            {" "}
                            {user.nombreCompleto}
                          </span>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}
      <DetailSheet
        open={creating || !!id}
        onOpenChange={(open) => {
          if (!open) close()
        }}
        title={creating ? "Crear usuario" : "Datos del usuario"}
        description="Los cambios se guardan en la cuenta seleccionada."
      >
        {creating ? (
          <UserForm
            key="new"
            admin
            pending={save.isPending}
            error={save.error}
            onSave={(data) => save.mutate(data)}
          />
        ) : detail.isPending ? (
          <p role="status">Cargando usuario…</p>
        ) : detail.isError ? (
          <div>
            <p role="alert">{detail.error.message}</p>
            <Button onClick={() => void detail.refetch()}>Reintentar</Button>
          </div>
        ) : (
          detail.data && (
            <div className="space-y-6">
              <UserForm
                key={`${detail.data.id}:${detail.data.actualizadoEn}`}
                user={detail.data}
                admin
                pending={save.isPending}
                error={save.error}
                onSave={(data) => save.mutate(data)}
              />
              <dl className="space-y-2 text-sm">
                <dt>Identificador QR</dt>
                <dd>{detail.data.qrUsuario}</dd>
                <dt>Fecha de registro</dt>
                <dd>
                  {new Date(detail.data.creadoEn).toLocaleString("es-AR")}
                </dd>
                <dt>Última modificación</dt>
                <dd>
                  {new Date(detail.data.actualizadoEn).toLocaleString("es-AR")}
                </dd>
              </dl>
              <form
                onSubmit={changePassword}
                className="space-y-3 border-t pt-5"
              >
                <Label htmlFor="new-password">Nueva contraseña</Label>
                <Input
                  id="new-password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  minLength={8}
                  maxLength={72}
                  required
                />
                <Button
                  type="submit"
                  variant="outline"
                  disabled={password.isPending}
                >
                  Cambiar contraseña
                </Button>
                {password.error && <p role="alert">{password.error.message}</p>}
              </form>
              {detail.data.estadoCuenta !== "INACTIVO" && (
                <div className="space-y-3 border-t pt-5">
                  {confirm ? (
                    <>
                      <p>
                        La cuenta de {detail.data.nombreCompleto} no podrá
                        iniciar sesión. Se conservará su historial.
                      </p>
                      <Button
                        variant="destructive"
                        disabled={deactivate.isPending}
                        onClick={() => deactivate.mutate()}
                      >
                        Confirmar baja
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => setConfirm(false)}
                      >
                        Cancelar
                      </Button>
                    </>
                  ) : (
                    <Button
                      variant="destructive"
                      onClick={() => setConfirm(true)}
                    >
                      Dar de baja
                    </Button>
                  )}
                  {deactivate.error && (
                    <p role="alert">{deactivate.error.message}</p>
                  )}
                </div>
              )}
              <p role="status" className="text-sm">
                {notice}
              </p>
            </div>
          )
        )}
      </DetailSheet>
    </div>
  )
}
