import { useState, type FormEvent } from "react"
import { useSearchParams } from "react-router-dom"
import { Check, Eye, Pencil, Plus, ShieldCheck, Users } from "lucide-react"
import {
  PageHeader,
  StatCard,
  StatusBadge,
  DataToolbar,
  DetailSheet,
  SectionCard,
} from "@/components/shared"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import {
  FieldValue,
  FilterSelect,
  RecordTable,
  ExportButton,
  exportCsv,
} from "./common"
import { peopleFixtures, type PersonRecord } from "./data"

function PeoplePage({ members = false }: { members?: boolean }) {
  const [params, setParams] = useSearchParams()
  const [people, setPeople] = useState(peopleFixtures)
  const [search, setSearch] = useState("")
  const [role, setRole] = useState("Todos los roles")
  const [status, setStatus] = useState("Todos los estados")
  const [relation, setRelation] = useState("Todas las relaciones")
  const [notice, setNotice] = useState("")
  const [error, setError] = useState("")
  const [formRole, setFormRole] = useState("Solicitante")
  const [formRelation, setFormRelation] = useState("Estudiante")
  const [formStatus, setFormStatus] = useState("Activo")
  const [formPlan, setFormPlan] = useState("Estudiante")
  const selected = people.find((person) => person.id === params.get("id"))
  const creating = params.get("action") === "create"
  const editing = params.get("action") === "edit"
  const filtered = people.filter(
    (person) =>
      `${person.name} ${person.dni} ${person.email}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (role === "Todos los roles" || person.role === role) &&
      (status === "Todos los estados" || person.status === status) &&
      (relation === "Todas las relaciones" || person.relationship === relation)
  )
  function open(person?: PersonRecord, edit = false) {
    setError("")
    setFormRole(person?.role ?? "Solicitante")
    setFormRelation(person?.relationship ?? "Estudiante")
    setFormStatus(person?.status ?? "Activo")
    setFormPlan(person?.membership ?? "Estudiante")
    setParams(
      person
        ? { id: person.id, ...(edit ? { action: "edit" } : {}) }
        : { action: "create" }
    )
  }
  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const email = String(form.get("email"))
    const dni = String(form.get("dni"))
    if (
      people.some(
        (person) =>
          person.id !== selected?.id &&
          (person.email === email ||
            person.dni.replaceAll(".", "") === dni.replaceAll(".", ""))
      )
    ) {
      setError("Ya existe una persona con ese DNI o email. Revisá los datos.")
      return
    }
    const record: PersonRecord = {
      id: selected?.id ?? `u-${people.length + 1}`,
      name: String(form.get("name")),
      email,
      dni,
      phone: String(form.get("phone")),
      relationship: formRelation,
      role: formRole,
      status: formStatus,
      membership: formPlan,
      lastPayment: selected?.lastPayment ?? "Sin pagos",
      verified: selected?.verified ?? false,
    }
    setPeople((current) =>
      selected
        ? current.map((person) => (person.id === selected.id ? record : person))
        : [...current, record]
    )
    setParams({ id: record.id })
    setNotice(
      selected
        ? "Los cambios se guardaron en esta vista previa."
        : "La persona se agregó a esta vista previa."
    )
  }
  const title = members ? "Socios y membresías" : "Usuarios del sistema"
  return (
    <>
      <PageHeader
        title={title}
        description={
          members
            ? "Consultá membresías, cuotas y datos de la comunidad."
            : "Gestioná las cuentas y los permisos del polideportivo."
        }
        actions={
          <>
            <ExportButton
              onClick={() =>
                exportCsv(
                  members ? "socios" : "usuarios",
                  ["Nombre", "DNI", "Email", "Estado"],
                  filtered.map((person) => [
                    person.name,
                    person.dni,
                    person.email,
                    person.status,
                  ])
                )
              }
            />
            <Button onClick={() => open()}>
              <Plus />
              {members ? "Nuevo socio" : "Crear usuario"}
            </Button>
          </>
        }
      />
      {members && (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <StatCard
            label="Socios activos"
            value="1.248"
            icon={Users}
            detail="Con membresía vigente"
          />
          <StatCard
            label="Cuotas por vencer"
            value="34"
            icon={ShieldCheck}
            detail="Durante los próximos 7 días"
          />
          <StatCard
            label="Membresías suspendidas"
            value="18"
            detail="Con cuotas vencidas"
          />
        </div>
      )}
      {notice && (
        <p
          role="status"
          className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs text-emerald-800"
        >
          {notice}
        </p>
      )}
      <DataToolbar search={search} onSearchChange={setSearch}>
        <FilterSelect
          label="Filtrar por estado"
          value={status}
          onChange={setStatus}
          options={["Todos los estados", "Activo", "Suspendido", "Pendiente"]}
        />
        {members ? (
          <FilterSelect
            label="Relación con la UNSE"
            value={relation}
            onChange={setRelation}
            options={[
              "Todas las relaciones",
              "Estudiante",
              "Docente",
              "No docente",
              "Externo",
            ]}
          />
        ) : (
          <FilterSelect
            label="Filtrar por rol"
            value={role}
            onChange={setRole}
            options={[
              "Todos los roles",
              "Administrador",
              "Responsable",
              "Personal de acceso",
              "Solicitante",
            ]}
          />
        )}
      </DataToolbar>
      <RecordTable
        columns={[
          "Nombre",
          "DNI",
          members ? "Membresía" : "Email",
          members ? "Relación UNSE" : "Rol",
          "Estado",
          members ? "Último pago" : "Verificación",
          "Acciones",
        ]}
        rows={filtered.map((person) => ({
          key: person.id,
          cells: [
            <div className="flex items-center gap-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-blue-50 text-[11px] font-bold text-blue-700">
                {person.name
                  .split(" ")
                  .map((name) => name[0])
                  .slice(0, 2)
                  .join("")}
              </span>
              <Button
                variant="link"
                className="h-auto p-0 text-xs font-semibold text-foreground"
                onClick={() => open(person)}
              >
                {person.name}
              </Button>
            </div>,
            <span className="tabular-nums">{person.dni}</span>,
            members ? (
              person.membership
            ) : (
              <span className="text-muted-foreground">{person.email}</span>
            ),
            members ? person.relationship : person.role,
            <StatusBadge
              tone={
                person.status === "Activo"
                  ? "success"
                  : person.status === "Pendiente"
                    ? "warning"
                    : "danger"
              }
            >
              {person.status}
            </StatusBadge>,
            members ? (
              person.lastPayment
            ) : (
              <span
                className={
                  person.verified ? "text-emerald-700" : "text-amber-700"
                }
              >
                {person.verified ? "Verificada" : "Pendiente"}
              </span>
            ),
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label={`Ver ${person.name}`}
                onClick={() => open(person)}
              >
                <Eye />
              </Button>
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label={`Editar ${person.name}`}
                onClick={() => open(person, true)}
              >
                <Pencil />
              </Button>
            </div>,
          ],
        }))}
      />
      <DetailSheet
        open={creating || !!selected}
        onOpenChange={(openState) => {
          if (!openState) setParams({})
        }}
        title={
          creating
            ? members
              ? "Registrar socio"
              : "Crear usuario"
            : editing
              ? "Editar datos"
              : members
                ? "Ficha de socio"
                : "Detalle de usuario"
        }
        description={
          creating || editing
            ? "Completá los datos. Los cambios se guardan en esta vista previa."
            : "Información personal y actividad en el polideportivo."
        }
      >
        {creating || editing ? (
          <form
            key={`${selected?.id ?? "new"}-${editing}`}
            onSubmit={save}
            className="space-y-5"
          >
            <h3 className="text-sm font-bold">Datos personales</h3>
            <div className="space-y-2">
              <Label htmlFor="person-name">Nombre y apellido</Label>
              <Input
                id="person-name"
                name="name"
                defaultValue={selected?.name}
                placeholder="Nombre y apellido"
                required
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="person-dni">DNI</Label>
                <Input
                  id="person-dni"
                  name="dni"
                  defaultValue={selected?.dni}
                  placeholder="Sin puntos"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="person-phone">Teléfono</Label>
                <Input
                  id="person-phone"
                  name="phone"
                  defaultValue={selected?.phone}
                  type="tel"
                  placeholder="385 123 4567"
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="person-email">Email</Label>
              <Input
                id="person-email"
                name="email"
                type="email"
                defaultValue={selected?.email}
                placeholder="nombre@unse.edu.ar"
                required
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Relación con la UNSE</Label>
                <FilterSelect
                  label="Relación con la UNSE"
                  value={formRelation}
                  onChange={setFormRelation}
                  options={["Estudiante", "Docente", "No docente", "Externo"]}
                />
              </div>
              <div className="space-y-2">
                <Label>Estado</Label>
                <FilterSelect
                  label="Estado"
                  value={formStatus}
                  onChange={setFormStatus}
                  options={["Activo", "Suspendido", "Pendiente"]}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>{members ? "Nivel de membresía" : "Rol inicial"}</Label>
              <FilterSelect
                label={members ? "Nivel de membresía" : "Rol inicial"}
                value={members ? formPlan : formRole}
                onChange={members ? setFormPlan : setFormRole}
                options={
                  members
                    ? ["Estudiante", "Comunidad UNSE", "General"]
                    : [
                        "Solicitante",
                        "Personal de acceso",
                        "Responsable",
                        "Administrador",
                      ]
                }
              />
            </div>
            {members && (
              <p className="rounded-lg bg-blue-50 p-3 text-xs leading-relaxed text-blue-800">
                El importe aplicable se confirmará según la membresía y la
                relación con la UNSE. La verificación es manual.
              </p>
            )}
            {editing && (
              <div className="space-y-2">
                <Label htmlFor="edit-reason">Motivo del cambio</Label>
                <Textarea
                  id="edit-reason"
                  name="reason"
                  required
                  placeholder="Indicá por qué se actualizan los datos"
                />
              </div>
            )}
            {error && (
              <p role="alert" className="text-xs text-destructive">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2 border-t pt-5">
              <Button
                type="button"
                variant="outline"
                onClick={() => setParams({})}
              >
                Cancelar
              </Button>
              <Button type="submit">
                {editing
                  ? "Guardar cambios"
                  : members
                    ? "Registrar socio"
                    : "Crear usuario"}
              </Button>
            </div>
          </form>
        ) : (
          selected && (
            <>
              <div className="mb-6 flex items-center gap-4">
                <span className="flex size-16 items-center justify-center rounded-full bg-blue-50 text-xl font-bold text-blue-700">
                  {selected.name
                    .split(" ")
                    .map((name) => name[0])
                    .slice(0, 2)
                    .join("")}
                </span>
                <div>
                  <h2 className="text-xl font-bold tracking-tight">
                    {selected.name}
                  </h2>
                  <div className="mt-2">
                    <StatusBadge
                      tone={
                        selected.status === "Activo" ? "success" : "warning"
                      }
                    >
                      {selected.status}
                    </StatusBadge>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="icon"
                  className="ml-auto"
                  onClick={() => open(selected, true)}
                  aria-label="Editar datos"
                >
                  <Pencil />
                </Button>
              </div>
              <Tabs defaultValue="information">
                <TabsList className="mb-5 w-full">
                  <TabsTrigger value="information">Información</TabsTrigger>
                  <TabsTrigger value="membership">Membresía</TabsTrigger>
                  <TabsTrigger value="activity">Actividad</TabsTrigger>
                </TabsList>
                <TabsContent value="information">
                  <dl className="grid gap-6 sm:grid-cols-2">
                    <FieldValue label="DNI">{selected.dni}</FieldValue>
                    <FieldValue label="Teléfono">{selected.phone}</FieldValue>
                    <div className="sm:col-span-2">
                      <FieldValue label="Email">{selected.email}</FieldValue>
                    </div>
                    <FieldValue label="Relación con la UNSE">
                      {selected.relationship}
                    </FieldValue>
                    <FieldValue label="Rol">{selected.role}</FieldValue>
                    <FieldValue label="Fecha de alta">12/03/2026</FieldValue>
                    <FieldValue label="Verificación UNSE">
                      {selected.verified ? "Verificada" : "Pendiente"}
                    </FieldValue>
                  </dl>
                  {!selected.verified && (
                    <Button
                      variant="outline"
                      className="mt-6"
                      onClick={() => {
                        setPeople((current) =>
                          current.map((person) =>
                            person.id === selected.id
                              ? { ...person, verified: true }
                              : person
                          )
                        )
                        setNotice(
                          "La relación con la UNSE se marcó como verificada."
                        )
                      }}
                    >
                      <Check />
                      Confirmar verificación manual
                    </Button>
                  )}
                </TabsContent>
                <TabsContent value="membership">
                  <SectionCard title={`Membresía ${selected.membership}`}>
                    <dl className="grid gap-5 sm:grid-cols-2">
                      <FieldValue label="Estado">{selected.status}</FieldValue>
                      <FieldValue label="Último pago">
                        {selected.lastPayment}
                      </FieldValue>
                      <FieldValue label="Próximo vencimiento">
                        10/10/2026
                      </FieldValue>
                      <FieldValue label="Pago recurrente">
                        Desactivado
                      </FieldValue>
                    </dl>
                  </SectionCard>
                </TabsContent>
                <TabsContent value="activity">
                  <div className="space-y-5">
                    {[
                      "Datos de perfil actualizados",
                      "Cuota de septiembre registrada",
                      "Ingreso al polideportivo",
                    ].map((text, index) => (
                      <div key={text} className="flex gap-3">
                        <span className="mt-1 size-2 shrink-0 rounded-full bg-primary" />
                        <div>
                          <p className="text-sm font-medium">{text}</p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {10 - index}/09/2026 · Registrado por Axel Castaño
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </TabsContent>
              </Tabs>
              <div className="mt-8 border-t pt-4 text-[11px] leading-relaxed text-muted-foreground">
                Última modificación: 10/09/2026, 09:42
                <br />
                Responsable: Axel Castaño
              </div>
            </>
          )
        )}
      </DetailSheet>
    </>
  )
}

export { UsersPage } from "./users"
export function MembersPage() {
  return <PeoplePage members />
}
