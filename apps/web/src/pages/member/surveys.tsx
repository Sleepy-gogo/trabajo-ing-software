import { useState } from "react"
import { Link, useParams, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ClipboardCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import {
  EmptyState,
  PageHeader,
  SectionCard,
  StatusBadge,
} from "@/components/shared"
import { ErrorMessage, QueryState } from "@/components/shared/real-data"
import { surveysApi, type Assignment } from "@/lib/surveys-api"

export function MemberSurveysPage() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const reservation = params.get("reserva")
  const list = useQuery({
    queryKey: ["my-surveys"],
    queryFn: ({ signal }) => surveysApi.mine(signal),
    enabled: !id,
  })
  const selected = useQuery({
    queryKey: ["survey-assignment", id, reservation],
    queryFn: ({ signal }) => surveysApi.get(id!, reservation!, signal),
    enabled: !!id && !!reservation,
  })
  if (id) {
    if (!reservation)
      return (
        <>
          <PageHeader
            title="Seleccioná una reserva"
            description="Cada respuesta está asociada a una reserva utilizada."
          />
          <Button
            nativeButton={false}
            role="link"
            render={<Link to="/app/surveys" />}
          >
            Ver mis encuestas
          </Button>
        </>
      )
    return (
      <QueryState
        pending={selected.isPending}
        error={selected.error}
        retry={selected.refetch}
      >
        {selected.data && (
          <SurveyForm key={`${id}-${reservation}`} assignment={selected.data} />
        )}
      </QueryState>
    )
  }
  return (
    <div>
      <PageHeader
        title="Mis encuestas"
        description="Compartí tu experiencia después de registrar el ingreso a una reserva."
      />
      <QueryState
        pending={list.isPending}
        error={list.error}
        retry={list.refetch}
      >
        {list.data?.length ? (
          <div className="grid gap-5 lg:grid-cols-2">
            {list.data.map((item) => (
              <SectionCard
                key={`${item.encuesta.id}-${item.reservaId}`}
                title={item.encuesta.titulo}
                description={item.encuesta.descripcion}
              >
                <StatusBadge
                  tone={
                    item.estado === "RESPONDIDA"
                      ? "success"
                      : item.estado === "DISPONIBLE"
                        ? "info"
                        : "neutral"
                  }
                >
                  {item.estado === "RESPONDIDA"
                    ? "Respondida"
                    : item.estado === "DISPONIBLE"
                      ? "Disponible"
                      : "Cerrada"}
                </StatusBadge>
                <p className="my-4 text-sm text-muted-foreground">
                  {item.espacio} · Reserva del {item.fecha}
                  <br />
                  Período de respuesta: {item.encuesta.desde} a{" "}
                  {item.encuesta.hasta}
                </p>
                <Button
                  variant={item.estado === "DISPONIBLE" ? "default" : "outline"}
                  nativeButton={false}
                  role="link"
                  render={
                    <Link
                      to={`/app/surveys/${item.encuesta.id}?reserva=${item.reservaId}`}
                    />
                  }
                >
                  {item.estado === "DISPONIBLE"
                    ? "Responder encuesta"
                    : "Ver encuesta"}
                </Button>
              </SectionCard>
            ))}
          </div>
        ) : (
          <SectionCard title="Encuestas">
            <EmptyState
              icon={ClipboardCheck}
              title="No tenés encuestas"
              description="Aparecen cuando administración publica una encuesta y registrás el ingreso a una reserva del espacio correspondiente."
            />
          </SectionCard>
        )}
      </QueryState>
    </div>
  )
}

function SurveyForm({ assignment }: { assignment: Assignment }) {
  const client = useQueryClient()
  const [answers, setAnswers] = useState<Record<string, string>>(
    assignment.respuestas
  )
  const [validation, setValidation] = useState<Error | null>(null)
  const submit = useMutation({
    mutationFn: () =>
      surveysApi.respond(assignment.encuesta.id, assignment.reservaId, answers),
    onSuccess: (data) => {
      client.setQueryData(
        ["survey-assignment", data.encuesta.id, data.reservaId],
        data
      )
      void client.invalidateQueries({ queryKey: ["my-surveys"] })
    },
  })
  const current = submit.data ?? assignment
  const editable = current.estado === "DISPONIBLE" && !submit.isPending
  function answer(id: string, value: string) {
    setAnswers((a) => ({ ...a, [id]: value }))
    setValidation(null)
    submit.reset()
  }
  return (
    <div>
      <PageHeader
        title={current.encuesta.titulo}
        description={`${current.espacio} · Reserva del ${current.fecha}. ${current.encuesta.descripcion}`}
        actions={
          <Button
            variant="outline"
            nativeButton={false}
            role="link"
            render={<Link to="/app/surveys" />}
          >
            Volver a encuestas
          </Button>
        }
      />
      {current.estado === "RESPONDIDA" && (
        <p
          role="status"
          className="mb-5 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
        >
          Tu respuesta quedó registrada. Podés consultarla a continuación.
        </p>
      )}
      {current.estado === "CERRADA" && (
        <p role="status" className="mb-5 text-sm">
          La encuesta está cerrada o fuera de su período de respuesta.
        </p>
      )}
      <SectionCard
        title="Sobre tu experiencia"
        description={`Podés responder entre ${current.encuesta.desde} y ${current.encuesta.hasta}. Las respuestas no son anónimas.`}
      >
        <form
          className="space-y-7"
          onSubmit={(event) => {
            event.preventDefault()
            if (
              current.encuesta.preguntas.some(
                (p) => p.obligatoria && !answers[p.id]?.trim()
              )
            ) {
              setValidation(
                new Error("Completá todas las preguntas obligatorias.")
              )
              return
            }
            submit.mutate()
          }}
        >
          {current.encuesta.preguntas.map((question, index) => (
            <fieldset
              key={question.id}
              disabled={!editable}
              className="space-y-3"
            >
              <legend className="mb-3 text-sm font-semibold">
                {index + 1}. {question.texto}
                {question.obligatoria ? " *" : " (opcional)"}
              </legend>
              {question.tipo === "TEXTO" ? (
                <Textarea
                  aria-label={question.texto}
                  required={question.obligatoria}
                  maxLength={2000}
                  value={answers[question.id] ?? ""}
                  onChange={(event) => answer(question.id, event.target.value)}
                />
              ) : (
                <div className="flex flex-wrap gap-3">
                  {(question.tipo === "CALIFICACION"
                    ? ["1", "2", "3", "4", "5"]
                    : question.opciones
                  ).map((value) => (
                    <label
                      key={value}
                      className="flex cursor-pointer items-center gap-2 rounded-lg border px-4 py-3 text-sm has-checked:border-primary has-checked:bg-primary/5"
                    >
                      <input
                        type="radio"
                        name={question.id}
                        value={value}
                        required={question.obligatoria}
                        checked={answers[question.id] === value}
                        onChange={() => answer(question.id, value)}
                      />
                      {question.tipo === "CALIFICACION"
                        ? `${value} de 5`
                        : value}
                    </label>
                  ))}
                </div>
              )}
            </fieldset>
          ))}
          <ErrorMessage error={validation ?? submit.error} />
          {current.estado === "DISPONIBLE" && (
            <Button type="submit" disabled={submit.isPending}>
              {submit.isPending ? "Enviando…" : "Enviar encuesta"}
            </Button>
          )}
        </form>
      </SectionCard>
    </div>
  )
}
