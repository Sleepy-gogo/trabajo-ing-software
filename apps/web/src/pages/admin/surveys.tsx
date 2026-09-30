import { useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Button } from "@/components/ui/button"
import {
  EmptyState,
  PageHeader,
  SectionCard,
  StatusBadge,
} from "@/components/shared"
import {
  ErrorMessage,
  Field,
  QueryState,
  SelectField,
} from "@/components/shared/real-data"
import {
  surveysApi,
  type QuestionInput,
  type QuestionType,
  type SurveyInput,
} from "@/lib/surveys-api"
import { spacesApi } from "@/lib/spaces-api"

const newQuestion = (): QuestionInput => ({
  texto: "",
  tipo: "CALIFICACION",
  obligatoria: true,
  opciones: [],
})
export function AdminSurveysPage() {
  const [params, setParams] = useSearchParams()
  const selected = params.get("id")
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState("")
  const list = useQuery({
    queryKey: ["surveys"],
    queryFn: ({ signal }) => surveysApi.list(signal),
  })
  const results = useQuery({
    queryKey: ["survey-results", selected],
    queryFn: ({ signal }) => surveysApi.results(selected!, signal),
    enabled: !!selected,
  })
  const client = useQueryClient()
  const active = useMutation({
    mutationFn: ({ id, value }: { id: string; value: boolean }) =>
      surveysApi.active(id, value),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ["surveys"] })
      void client.invalidateQueries({ queryKey: ["survey-results"] })
    },
  })
  if (selected)
    return (
      <div>
        <PageHeader
          title={results.data?.encuesta.titulo ?? "Resultados de encuesta"}
          description="Respuestas asociadas a reservas utilizadas. Acceso exclusivo de administración."
          actions={
            <Button variant="outline" onClick={() => setParams({})}>
              Volver
            </Button>
          }
        />
        <QueryState
          pending={results.isPending}
          error={results.error}
          retry={results.refetch}
        >
          {results.data && (
            <>
              <SectionCard title={`${results.data.total} respuestas recibidas`}>
                <div className="grid gap-4 sm:grid-cols-2">
                  {results.data.estadisticas.map((stat) => (
                    <div
                      key={stat.preguntaId}
                      className="rounded-lg border p-4"
                    >
                      <h2 className="text-sm font-semibold">{stat.texto}</h2>
                      <p className="mt-2 text-sm text-muted-foreground">
                        {stat.respuestas} respuestas
                        {stat.promedio !== null
                          ? ` · Promedio ${stat.promedio.toFixed(2)} de 5`
                          : ""}
                      </p>
                      {Object.entries(stat.distribucion).map(
                        ([value, count]) => (
                          <p key={value} className="mt-1 text-sm">
                            {value}: {count}
                          </p>
                        )
                      )}
                    </div>
                  ))}
                </div>
              </SectionCard>
              <div className="mt-5">
                <SectionCard title="Detalle de respuestas">
                  {results.data.envios.length ? (
                    <div className="space-y-4">
                      {results.data.envios.map((envio) => (
                        <article
                          className="rounded-lg border p-4 text-sm"
                          key={envio.envioId}
                        >
                          <h2 className="font-semibold">
                            {envio.titular} · {envio.espacio}
                          </h2>
                          <p className="mt-1 text-muted-foreground">
                            Reserva del {envio.fecha} · Enviada el{" "}
                            {new Date(envio.enviadaEn).toLocaleString("es-AR")}
                          </p>
                          <dl className="mt-3 space-y-2">
                            {results.data!.encuesta.preguntas.map((p) => (
                              <div key={p.id}>
                                <dt className="font-medium">{p.texto}</dt>
                                <dd className="whitespace-pre-wrap text-muted-foreground">
                                  {envio.respuestas[p.id] ?? "Sin respuesta"}
                                </dd>
                              </div>
                            ))}
                          </dl>
                          <Link
                            className="mt-3 inline-block text-primary underline"
                            to={`/admin/reservations?id=${envio.reservaId}`}
                          >
                            Ver reserva
                          </Link>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <EmptyState
                      title="Todavía no hay respuestas"
                      description="Los resultados aparecen después de que un usuario envía la encuesta."
                    />
                  )}
                </SectionCard>
              </div>
            </>
          )}
        </QueryState>
      </div>
    )
  return (
    <div>
      <PageHeader
        title="Encuestas"
        description="Definí preguntas y consultá el feedback de los usuarios."
        actions={
          <Button
            onClick={() => {
              setCreating((c) => !c)
              setNotice("")
            }}
          >
            {creating ? "Cerrar formulario" : "Nueva encuesta"}
          </Button>
        }
      />
      {notice && (
        <p role="status" className="mb-5 text-sm text-emerald-800">
          {notice}
        </p>
      )}
      {creating && (
        <div className="mb-5">
          <CreateSurvey
            onCreated={() => {
              setCreating(false)
              setNotice(
                "Encuesta creada. Los usuarios con reservas utilizadas pueden responder dentro del período indicado."
              )
              void client.invalidateQueries({ queryKey: ["surveys"] })
            }}
          />
        </div>
      )}
      <ErrorMessage error={active.error} />
      <QueryState
        pending={list.isPending}
        error={list.error}
        retry={list.refetch}
      >
        <SectionCard title="Encuestas publicadas">
          {list.data?.length ? (
            <div className="divide-y">
              {list.data.map((survey) => (
                <div
                  className="flex flex-wrap items-center justify-between gap-4 py-4"
                  key={survey.id}
                >
                  <div>
                    <h2 className="text-sm font-semibold">{survey.titulo}</h2>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {survey.desde} a {survey.hasta} ·{" "}
                      {survey.preguntas.length} preguntas
                    </p>
                    <StatusBadge tone={survey.activa ? "info" : "neutral"}>
                      {survey.activa ? "Habilitada" : "Cerrada"}
                    </StatusBadge>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      onClick={() => setParams({ id: survey.id })}
                    >
                      Ver resultados
                    </Button>
                    <Button
                      variant="outline"
                      disabled={active.isPending}
                      onClick={() =>
                        active.mutate({ id: survey.id, value: !survey.activa })
                      }
                    >
                      {survey.activa ? "Cerrar encuesta" : "Habilitar encuesta"}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No hay encuestas publicadas"
              description="Creá la primera encuesta para recoger feedback."
            />
          )}
        </SectionCard>
      </QueryState>
    </div>
  )
}

function CreateSurvey({ onCreated }: { onCreated: () => void }) {
  const today = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date())
  const [form, setForm] = useState<SurveyInput>({
    titulo: "",
    descripcion: "",
    espacioId: null,
    desde: today,
    hasta: today,
    preguntas: [newQuestion()],
  })
  const spaces = useQuery({
    queryKey: ["spaces"],
    queryFn: ({ signal }) => spacesApi.list("", signal),
  })
  const create = useMutation({
    mutationFn: surveysApi.create,
    onSuccess: onCreated,
  })
  const [options, setOptions] = useState<Record<number, string>>({})
  function question(index: number, patch: Partial<QuestionInput>) {
    setForm((current) => ({
      ...current,
      preguntas: current.preguntas.map((p, i) =>
        i === index ? { ...p, ...patch } : p
      ),
    }))
  }
  return (
    <SectionCard
      title="Nueva encuesta"
      description="Las preguntas se conservan después de publicar para mantener comparables las respuestas."
    >
      <form
        className="space-y-5"
        onSubmit={(event) => {
          event.preventDefault()
          create.mutate({
            ...form,
            preguntas: form.preguntas.map((p, i) => ({
              ...p,
              opciones:
                p.tipo === "OPCION"
                  ? (options[i] ?? "").split("\n").map((o) => o.trim())
                  : [],
            })),
          })
        }}
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Título"
            required
            maxLength={200}
            value={form.titulo}
            onChange={(event) =>
              setForm((f) => ({ ...f, titulo: event.target.value }))
            }
          />
          <Field
            label="Descripción"
            maxLength={1000}
            value={form.descripcion}
            onChange={(event) =>
              setForm((f) => ({ ...f, descripcion: event.target.value }))
            }
          />
          <Field
            label="Disponible desde"
            type="date"
            required
            value={form.desde}
            max={form.hasta}
            onChange={(event) =>
              setForm((f) => ({ ...f, desde: event.target.value }))
            }
          />
          <Field
            label="Disponible hasta"
            type="date"
            required
            min={form.desde}
            value={form.hasta}
            onChange={(event) =>
              setForm((f) => ({ ...f, hasta: event.target.value }))
            }
          />
          <SelectField
            label="Espacio de la encuesta"
            value={form.espacioId ?? ""}
            onChange={(event) =>
              setForm((f) => ({ ...f, espacioId: event.target.value || null }))
            }
          >
            <option value="">Todos los espacios</option>
            {spaces.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </SelectField>
        </div>
        {form.preguntas.map((p, index) => (
          <fieldset className="space-y-4 rounded-lg border p-4" key={index}>
            <legend className="px-2 text-sm font-semibold">
              Pregunta {index + 1}
            </legend>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                label={`Texto de pregunta ${index + 1}`}
                required
                maxLength={500}
                value={p.texto}
                onChange={(event) =>
                  question(index, { texto: event.target.value })
                }
              />
              <SelectField
                label={`Tipo de pregunta ${index + 1}`}
                value={p.tipo}
                onChange={(event) =>
                  question(index, {
                    tipo: event.target.value as QuestionType,
                    opciones: [],
                  })
                }
              >
                <option value="CALIFICACION">Calificación de 1 a 5</option>
                <option value="OPCION">Una opción</option>
                <option value="TEXTO">Texto libre</option>
              </SelectField>
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={p.obligatoria}
                onChange={(event) =>
                  question(index, { obligatoria: event.target.checked })
                }
              />
              Respuesta obligatoria
            </label>
            {p.tipo === "OPCION" && (
              <label className="block space-y-2 text-sm">
                <span>
                  Opciones de pregunta {index + 1}, una por línea, entre 2 y 10
                </span>
                <textarea
                  className="min-h-24 w-full rounded-lg border p-3"
                  required
                  value={options[index] ?? ""}
                  onChange={(event) =>
                    setOptions((o) => ({ ...o, [index]: event.target.value }))
                  }
                />
              </label>
            )}
            {index === form.preguntas.length - 1 && index > 0 && (
              <Button
                type="button"
                variant="outline"
                onClick={() =>
                  setForm((f) => ({
                    ...f,
                    preguntas: f.preguntas.slice(0, -1),
                  }))
                }
              >
                Quitar última pregunta
              </Button>
            )}
          </fieldset>
        ))}
        <ErrorMessage error={create.error ?? spaces.error} />
        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={form.preguntas.length >= 20 || create.isPending}
            onClick={() =>
              setForm((f) => ({
                ...f,
                preguntas: [...f.preguntas, newQuestion()],
              }))
            }
          >
            Agregar pregunta
          </Button>
          <Button type="submit" disabled={create.isPending}>
            {create.isPending ? "Publicando…" : "Publicar encuesta"}
          </Button>
        </div>
      </form>
    </SectionCard>
  )
}
