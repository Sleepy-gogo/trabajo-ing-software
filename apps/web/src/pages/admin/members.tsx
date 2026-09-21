import { useState, type FormEvent } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import { Plus, Pencil } from "lucide-react"
import {
  membersApi,
  relationships,
  label,
  type Member,
  type Relationship,
  type MemberUpdate,
} from "@/lib/members-api"
import { usersApi } from "@/lib/users-api"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  DetailSheet,
  SectionCard,
  StatusBadge,
} from "@/components/shared"
import {
  Field,
  SelectField,
  ErrorMessage,
  QueryState,
  Note,
} from "@/components/shared/real-data"
import { RecordTable } from "./common"

export function MembersPage() {
  const [search, setSearch] = useState("")
  const [state, setState] = useState("")
  const [relation, setRelation] = useState("")
  const [selected, setSelected] = useState<Member | null>(null)
  const [open, setOpen] = useState(false)
  const [notice, setNotice] = useState("")
  const client = useQueryClient()
  const members = useQuery({
    queryKey: ["members", search, state, relation],
    queryFn: ({ signal }) => membersApi.list(search, state, relation, signal),
  })
  const levels = useQuery({
    queryKey: ["levels", "admin"],
    queryFn: ({ signal }) => membersApi.levels(true, signal),
  })
  const users = useQuery({
    queryKey: ["users", "member-picker"],
    queryFn: ({ signal }) => usersApi.list("", signal),
    enabled: open && !selected,
  })
  const contract = useMutation({
    mutationFn: (nivelId: string) => membersApi.contract(selected!.id, nivelId),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["members"] })
      setOpen(false)
      setNotice("La membresía quedó pendiente de pago.")
    },
  })
  const history = useQuery({
    queryKey: ["member-audit", selected?.id],
    queryFn: ({ signal }) => membersApi.audit(selected!.id, signal),
    enabled: open && !!selected,
  })
  const save = useMutation({
    mutationFn: async (data: FormData) => {
      const base = {
        relacionUnse: String(data.get("relacionUnse")) as Relationship,
        identificadorUnse: String(data.get("identificadorUnse") || "") || null,
        nivelMembresiaId: String(data.get("nivelMembresiaId") || "") || null,
      }
      return selected
        ? membersApi.update(selected.id, {
            ...base,
            estadoVerificacionUnse: String(
              data.get("estadoVerificacionUnse")
            ) as MemberUpdate["estadoVerificacionUnse"],
            estadoMembresia: (String(data.get("estadoMembresia") || "") ||
              null) as MemberUpdate["estadoMembresia"],
            motivo: String(data.get("motivo")),
          })
        : membersApi.register({
            ...base,
            usuarioId: String(data.get("usuarioId")),
          })
    },
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["members"] })
      await client.invalidateQueries({ queryKey: ["my-member"] })
      setOpen(false)
      setNotice("Los datos del socio se guardaron.")
    },
  })
  function edit(member: Member | null) {
    setSelected(member)
    save.reset()
    setOpen(true)
  }
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    save.mutate(new FormData(e.currentTarget))
  }
  return (
    <>
      <PageHeader
        title="Socios y membresías"
        description="Administrá la relación con la UNSE y el estado de cada socio."
        actions={
          <>
            <Link
              className="text-sm font-medium text-primary underline underline-offset-4"
              to="/admin/levels"
            >
              Niveles y precios
            </Link>
            <Button render={<Link to="/admin/users?action=create" />}>
              <Plus />
              Crear usuario y socio
            </Button>
          </>
        }
      />
      <p role="status" className="mb-4 text-sm text-emerald-800">
        {notice}
      </p>
      <div className="mb-5 grid gap-4 sm:grid-cols-3">
        <Field
          label="Buscar socio"
          placeholder="Nombre, DNI o email"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <SelectField
          label="Estado de membresía"
          value={state}
          onChange={(e) => setState(e.target.value)}
        >
          <option value="">Todos los estados</option>
          {[
            "PENDIENTE_PAGO",
            "ACTIVA",
            "VENCIDA",
            "SUSPENDIDA",
            "CANCELADA",
          ].map((s) => (
            <option key={s}>{s}</option>
          ))}
        </SelectField>
        <SelectField
          label="Relación con la UNSE"
          value={relation}
          onChange={(e) => setRelation(e.target.value)}
        >
          <option value="">Todas las relaciones</option>
          {relationships.map((r) => (
            <option key={r} value={r}>
              {label(r)}
            </option>
          ))}
        </SelectField>
      </div>
      <QueryState
        pending={members.isPending}
        error={members.error}
        retry={members.refetch}
      >
        <RecordTable
          footer={false}
          emptyTitle="No hay socios para esta búsqueda"
          columns={[
            "Socio",
            "DNI",
            "Nivel",
            "Estado",
            "Relación UNSE",
            "Acciones",
          ]}
          rows={(members.data ?? []).map((s) => ({
            key: s.id,
            cells: [
              <div>
                <p className="font-semibold">{s.nombreCompleto}</p>
                <p className="text-xs text-muted-foreground">{s.email}</p>
              </div>,
              s.dni,
              s.nivelMembresiaNombre ?? "Sin membresía",
              <StatusBadge
                tone={s.estadoMembresia === "ACTIVA" ? "success" : "warning"}
              >
                {label(s.estadoMembresia)}
              </StatusBadge>,
              <div>
                {label(s.relacionUnse)}
                <p className="text-xs text-muted-foreground">
                  {label(s.estadoVerificacionUnse)}
                </p>
              </div>,
              <Button variant="outline" size="sm" onClick={() => edit(s)}>
                <Pencil />
                Ver y editar
              </Button>,
            ],
          }))}
        />
      </QueryState>
      <DetailSheet
        open={open}
        onOpenChange={(value) => {
          if (!save.isPending) setOpen(value)
        }}
        title={selected ? selected.nombreCompleto : "Registrar socio"}
        description="Los cambios se guardan en el sistema."
      >
        <form
          key={selected?.id ?? "new"}
          onSubmit={submit}
          className="space-y-5"
        >
          {!selected && (
            <>
              <QueryState
                pending={users.isPending}
                error={users.error}
                retry={users.refetch}
              >
                <SelectField
                  label="Usuario registrado"
                  name="usuarioId"
                  required
                  defaultValue=""
                >
                  <option value="" disabled>
                    Seleccioná una persona
                  </option>
                  {users.data
                    ?.filter((u) => u.estadoCuenta === "ACTIVO")
                    .map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.nombreCompleto} · {u.dni}
                      </option>
                    ))}
                </SelectField>
              </QueryState>
              <Link
                to="/admin/users"
                className="text-sm text-primary underline"
              >
                Crear una cuenta de usuario
              </Link>
            </>
          )}
          <SelectField
            label="Relación con la UNSE"
            name="relacionUnse"
            defaultValue={selected?.relacionUnse ?? "EXTERNO"}
          >
            {relationships.map((r) => (
              <option key={r} value={r}>
                {label(r)}
              </option>
            ))}
          </SelectField>
          <Field
            label="Legajo o identificador UNSE"
            name="identificadorUnse"
            maxLength={50}
            defaultValue={selected?.identificadorUnse ?? ""}
          />
          {selected && (
            <SelectField
              label="Verificación manual"
              name="estadoVerificacionUnse"
              defaultValue={selected.estadoVerificacionUnse}
            >
              {["PENDIENTE", "VERIFICADA", "RECHAZADA"].map((s) => (
                <option key={s} value={s}>
                  {label(s)}
                </option>
              ))}
            </SelectField>
          )}
          {(!selected || selected.membresiaId) && (
            <>
              <ErrorMessage error={levels.error} />
              <SelectField
                label="Nivel de membresía"
                name="nivelMembresiaId"
                defaultValue={selected?.nivelMembresiaId ?? ""}
                required={!!selected}
              >
                <option value="">Sin membresía</option>
                {levels.data
                  ?.filter(
                    (n) =>
                      n.disponibleParaContratar ||
                      n.id === selected?.nivelMembresiaId
                  )
                  .map((n) => (
                    <option key={n.id} value={n.id}>
                      {n.nombre}
                      {!n.disponibleParaContratar ? " (inactivo)" : ""}
                    </option>
                  ))}
              </SelectField>
            </>
          )}
          {selected?.membresiaId && (
            <SelectField
              label="Estado de membresía"
              name="estadoMembresia"
              defaultValue={selected.estadoMembresia ?? ""}
            >
              {Array.from(
                new Set([
                  selected.estadoMembresia,
                  "VENCIDA",
                  "SUSPENDIDA",
                  "CANCELADA",
                ])
              )
                .filter(Boolean)
                .map((s) => (
                  <option key={s} value={s!}>
                    {label(s)}
                  </option>
                ))}
            </SelectField>
          )}
          <Note>
            La activación requiere la aprobación del pago. La relación con la
            UNSE se verifica manualmente.
          </Note>
          {selected && (
            <Field
              label="Motivo del cambio"
              name="motivo"
              required
              maxLength={500}
            />
          )}
          <ErrorMessage error={save.error} />
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? "Guardando…" : "Guardar datos"}
          </Button>
        </form>
        {selected && !selected.membresiaId && (
          <SectionCard className="mt-6" title="Contratar membresía">
            <form
              className="space-y-4"
              onSubmit={(e) => {
                e.preventDefault()
                contract.mutate(
                  String(new FormData(e.currentTarget).get("nivel"))
                )
              }}
            >
              <SelectField
                label="Nivel a contratar"
                name="nivel"
                required
                defaultValue=""
              >
                <option value="" disabled>
                  Seleccioná un nivel
                </option>
                {levels.data
                  ?.filter(
                    (n) =>
                      n.disponibleParaContratar &&
                      n.preciosPorRelacion[selected.relacionUnse] != null
                  )
                  .map((n) => (
                    <option key={n.id} value={n.id}>
                      {n.nombre}
                    </option>
                  ))}
              </SelectField>
              <Note>La contratación queda pendiente de pago.</Note>
              <ErrorMessage error={contract.error} />
              <Button disabled={contract.isPending}>Solicitar membresía</Button>
            </form>
          </SectionCard>
        )}
        {selected && (
          <SectionCard className="mt-6" title="Historial de cambios">
            <QueryState
              pending={history.isPending}
              error={history.error}
              retry={history.refetch}
            >
              <ol className="space-y-4">
                {history.data?.map((h) => (
                  <li
                    key={h.id}
                    className="border-l-2 border-primary/20 pl-3 text-sm"
                  >
                    <p className="font-semibold">{h.motivo}</p>
                    <p className="mt-1">{h.detalle}</p>
                    <p className="mt-1 text-xs break-all text-muted-foreground">
                      {new Date(h.fecha).toLocaleString("es-AR")} · Responsable{" "}
                      {h.responsableId}
                    </p>
                  </li>
                ))}
              </ol>
            </QueryState>
          </SectionCard>
        )}
      </DetailSheet>
    </>
  )
}
