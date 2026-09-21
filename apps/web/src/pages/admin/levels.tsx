import { useState, type FormEvent } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import {
  membersApi,
  relationships,
  label,
  type Level,
  type LevelInput,
} from "@/lib/members-api"
import { formatCurrency } from "@/lib/format"
import { Button } from "@/components/ui/button"
import {
  PageHeader,
  SectionCard,
  DetailSheet,
  ConfirmationDialog,
  StatusBadge,
} from "@/components/shared"
import { Field, ErrorMessage, QueryState } from "@/components/shared/real-data"
export function LevelsPage() {
  const client = useQueryClient()
  const [selected, setSelected] = useState<Level | null>(null)
  const [open, setOpen] = useState(false)
  const [disabling, setDisabling] = useState<Level | null>(null)
  const [notice, setNotice] = useState("")
  const levels = useQuery({
    queryKey: ["levels", "admin"],
    queryFn: ({ signal }) => membersApi.levels(true, signal),
  })
  const save = useMutation({
    mutationFn: (data: LevelInput) => membersApi.saveLevel(data, selected?.id),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["levels"] })
      setOpen(false)
      setNotice("El nivel y sus precios se guardaron.")
    },
  })
  const disable = useMutation({
    mutationFn: (id: string) => membersApi.disableLevel(id),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["levels"] })
      setNotice("El nivel ya no está disponible para nuevas contrataciones.")
    },
  })
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    save.mutate({
      nombre: String(f.get("nombre")),
      descripcion: String(f.get("descripcion")),
      beneficios: String(f.get("beneficios"))
        .split("\n")
        .map((s) => s.trim())
        .filter(Boolean),
      disponibleParaContratar: f.get("disponible") === "on",
      preciosPorRelacion: Object.fromEntries(
        relationships
          .filter((r) => f.get(r) !== "")
          .map((r) => [r, Number(f.get(r))])
      ),
    })
  }
  return (
    <>
      <PageHeader
        title="Niveles y precios"
        description="Configurá los planes, sus beneficios y el importe mensual por relación con la UNSE."
        actions={
          <Button
            onClick={() => {
              setSelected(null)
              save.reset()
              setOpen(true)
            }}
          >
            <Plus />
            Crear nivel
          </Button>
        }
      />
      <p role="status" className="mb-4 text-sm text-emerald-800">
        {notice}
      </p>
      <ErrorMessage error={disable.error} />
      <QueryState
        pending={levels.isPending}
        error={levels.error}
        retry={levels.refetch}
      >
        <div className="grid gap-5 lg:grid-cols-2">
          {levels.data?.map((n) => (
            <SectionCard
              key={n.id}
              title={n.nombre}
              action={
                <StatusBadge
                  tone={n.disponibleParaContratar ? "success" : "neutral"}
                >
                  {n.disponibleParaContratar ? "Disponible" : "Inactivo"}
                </StatusBadge>
              }
            >
              <p className="text-sm text-muted-foreground">{n.descripcion}</p>
              <dl className="my-4 grid grid-cols-2 gap-3 text-sm">
                {Object.entries(n.preciosPorRelacion).map(([r, price]) => (
                  <div key={r}>
                    <dt className="text-muted-foreground">{label(r)}</dt>
                    <dd className="font-semibold">{formatCurrency(price!)}</dd>
                  </div>
                ))}
              </dl>
              <ul className="mb-5 list-inside list-disc text-sm">
                {n.beneficios.map((b) => (
                  <li key={b}>{b}</li>
                ))}
              </ul>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setSelected(n)
                    save.reset()
                    setOpen(true)
                  }}
                >
                  Editar nivel
                </Button>
                {n.disponibleParaContratar && (
                  <Button
                    variant="ghost"
                    disabled={disable.isPending}
                    onClick={() => setDisabling(n)}
                  >
                    Deshabilitar
                  </Button>
                )}
              </div>
            </SectionCard>
          ))}
        </div>
        {levels.data?.length === 0 && (
          <SectionCard>
            <p>
              No hay niveles. Creá el primero para habilitar contrataciones.
            </p>
          </SectionCard>
        )}
      </QueryState>
      <DetailSheet
        open={open}
        onOpenChange={(v) => {
          if (!save.isPending) setOpen(v)
        }}
        title={selected ? "Editar nivel" : "Crear nivel"}
        description="Los precios se aplican a futuras cuotas. Los pagos históricos conservan su importe."
      >
        <form
          key={selected?.id ?? "new"}
          onSubmit={submit}
          className="space-y-5"
        >
          <Field
            label="Nombre"
            name="nombre"
            required
            maxLength={100}
            defaultValue={selected?.nombre}
          />
          <Field
            label="Descripción y condiciones"
            name="descripcion"
            required
            maxLength={500}
            defaultValue={selected?.descripcion}
          />
          <label className="block space-y-2 text-sm font-medium">
            Beneficios, uno por línea
            <textarea
              className="min-h-24 w-full rounded-lg border p-3 font-normal"
              name="beneficios"
              required
              defaultValue={selected?.beneficios.join("\n")}
            />
          </label>
          <fieldset>
            <legend className="mb-3 text-sm font-semibold">
              Importe mensual en pesos
            </legend>
            <p className="mb-3 text-xs text-muted-foreground">
              Dejá vacío si el nivel no está disponible para esa relación.
            </p>
            <div className="grid grid-cols-2 gap-4">
              {relationships.map((r) => (
                <Field
                  key={r}
                  label={label(r)}
                  name={r}
                  type="number"
                  min="0.01"
                  max="99999999.99"
                  step="0.01"
                  defaultValue={selected?.preciosPorRelacion[r] ?? ""}
                />
              ))}
            </div>
          </fieldset>
          <label className="flex items-center gap-3 text-sm">
            <input
              type="checkbox"
              name="disponible"
              defaultChecked={selected?.disponibleParaContratar ?? true}
            />
            Disponible para contratar
          </label>
          <ErrorMessage error={save.error} />
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? "Guardando…" : "Guardar nivel"}
          </Button>
        </form>
      </DetailSheet>
      <ConfirmationDialog
        open={!!disabling}
        onOpenChange={(v) => {
          if (!v) setDisabling(null)
        }}
        title="Deshabilitar nivel"
        description="Las membresías existentes se conservan. Este nivel dejará de ofrecerse para nuevas contrataciones."
        confirmLabel="Deshabilitar"
        onConfirm={() => {
          if (disabling) disable.mutate(disabling.id)
        }}
      />
    </>
  )
}
