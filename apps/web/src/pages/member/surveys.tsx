import { useMemo, useState } from "react"
import { useParams, useSearchParams } from "react-router-dom"
import { Check, ClipboardCheck, MessageSquareText, Star } from "lucide-react"

import { EmptyState } from "@/components/shared"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { currentUser, getSurveyById, surveys } from "@/mocks"

import { formatDate } from "./data"
import {
  Go,
  InfoRows,
  MemberHeading,
  Notice,
  Panel,
  Result,
  StateBadge,
} from "./member-parts"

export function MemberSurveysPage() {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [error, setError] = useState("")
  const userSurveys = surveys.filter(
    (survey) => survey.userId === currentUser.id
  )
  const survey = id ? getSurveyById(id) : undefined
  const state = params.get("state")

  if (id && !survey) {
    return (
      <Result
        description="La encuesta puede haber vencido o no estar asociada a tu cuenta."
        error
        title="Encuesta no encontrada"
      >
        <Go to="/app/surveys">Ver encuestas</Go>
      </Result>
    )
  }

  if (id && survey) {
    return (
      <SurveyForm
        answers={answers}
        error={error}
        onAnswer={(questionId, value) => {
          setAnswers((current) => ({ ...current, [questionId]: value }))
          setError("")
        }}
        onBack={() => setParams({})}
        onSubmit={() => {
          const required = [
            ...survey.preguntasGenerales,
            ...survey.preguntasEspacio,
          ]
            .filter((question) => question.obligatoria)
            .some((question) => !answers[question.id])
          if (required) {
            setError(
              "Completá las preguntas obligatorias para enviar la encuesta."
            )
            return
          }
          setParams({ state: "submitted" })
        }}
        survey={survey}
        submitted={state === "submitted"}
      />
    )
  }

  if (state === "submitted") {
    return (
      <Result
        description="Gracias por compartir tu experiencia. Tu respuesta quedó registrada en esta demostración."
        title="Encuesta enviada"
      >
        <Go to="/app/surveys">Volver a encuestas</Go>
        <Go secondary to="/app">
          Volver al inicio
        </Go>
      </Result>
    )
  }

  const showEmpty = state === "empty" || userSurveys.length === 0
  return (
    <div>
      <MemberHeading
        description="Respondé encuestas sobre los espacios que ya utilizaste."
        title="Mis encuestas"
      />
      {state === "loading" ? (
        <Panel>
          <div className="flex min-h-52 items-center justify-center text-sm text-muted-foreground">
            Cargando encuestas…
          </div>
        </Panel>
      ) : showEmpty ? (
        <Panel>
          <EmptyState
            icon={ClipboardCheck}
            description="Las encuestas aparecen después de completar una reserva o utilizar un servicio."
            title="No tenés encuestas pendientes"
          />
        </Panel>
      ) : (
        <div className="grid gap-5 lg:grid-cols-2">
          {userSurveys.map((item) => (
            <Panel key={item.id}>
              <div className="flex items-start justify-between gap-4">
                <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <MessageSquareText aria-hidden="true" className="size-5" />
                </span>
                <StateBadge state={item.estadoLabel} />
              </div>
              <h2 className="mt-5 text-lg font-semibold">{item.titulo}</h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {item.descripcion}
              </p>
              <InfoRows
                rows={[
                  ["Espacio", item.spaceName],
                  ["Disponible hasta", formatDate(item.fechaLimite)],
                ]}
              />
              <div className="mt-5">
                {item.estado === "disponible" ||
                item.estado === "incompleta" ? (
                  <Go to={"/app/surveys/" + item.id}>Responder encuesta</Go>
                ) : (
                  <Go secondary to={"/app/surveys/" + item.id}>
                    Ver respuestas
                  </Go>
                )}
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  )
}

type SurveyFormProps = {
  answers: Record<string, string>
  error: string
  onAnswer: (questionId: string, value: string) => void
  onBack: () => void
  onSubmit: () => void
  survey: NonNullable<ReturnType<typeof getSurveyById>>
  submitted: boolean
}

function SurveyForm({
  answers,
  error,
  onAnswer,
  onBack,
  onSubmit,
  survey,
  submitted,
}: SurveyFormProps) {
  const questions = useMemo(
    () => [...survey.preguntasGenerales, ...survey.preguntasEspacio],
    [survey]
  )
  const completed = submitted || survey.estado === "completada"

  return (
    <div>
      <MemberHeading
        back="/app/surveys"
        description={survey.descripcion}
        title={survey.titulo}
      />
      {completed ? (
        <Result
          description="Esta encuesta ya fue respondida para la reserva seleccionada."
          title="Gracias por tu respuesta"
        >
          <Go to="/app/surveys">Volver a encuestas</Go>
          <Go secondary to="/app">
            Volver al inicio
          </Go>
        </Result>
      ) : (
        <div className="mx-auto max-w-3xl space-y-5">
          <Panel>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold">{survey.spaceName}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Podés responder hasta el {formatDate(survey.fechaLimite)}.
                </p>
              </div>
              <StateBadge state="Disponible" />
            </div>
          </Panel>
          <Panel title="Sobre tu experiencia">
            <div className="space-y-8">
              {questions.map((question, index) => (
                <div className="space-y-3" key={question.id}>
                  <div>
                    <p className="text-sm font-medium">
                      {index + 1}. {question.texto}
                    </p>
                    {question.obligatoria && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        Respuesta obligatoria
                      </p>
                    )}
                  </div>
                  {question.tipo === "calificacion" ? (
                    <div
                      className="flex flex-wrap gap-2"
                      role="radiogroup"
                      aria-label={question.texto}
                    >
                      {[1, 2, 3, 4, 5].map((rating) => {
                        const value = String(rating)
                        const selected = answers[question.id] === value
                        return (
                          <Button
                            aria-checked={selected}
                            aria-label={rating + " de 5"}
                            className="size-11 rounded-full p-0"
                            key={rating}
                            onClick={() => onAnswer(question.id, value)}
                            role="radio"
                            variant={selected ? "default" : "outline"}
                          >
                            <Star
                              aria-hidden="true"
                              className={
                                selected ? "size-4 fill-current" : "size-4"
                              }
                            />
                            <span className="sr-only">{rating}</span>
                          </Button>
                        )
                      })}
                    </div>
                  ) : question.tipo === "opcion" ? (
                    <div className="flex flex-wrap gap-2">
                      {(question.opciones ?? []).map((option) => (
                        <Button
                          aria-pressed={answers[question.id] === option}
                          key={option}
                          onClick={() => onAnswer(question.id, option)}
                          variant={
                            answers[question.id] === option
                              ? "default"
                              : "outline"
                          }
                        >
                          {option}
                        </Button>
                      ))}
                    </div>
                  ) : (
                    <Textarea
                      aria-label={question.texto}
                      onChange={(event) =>
                        onAnswer(question.id, event.target.value)
                      }
                      placeholder="Escribí un comentario (opcional)"
                      value={answers[question.id] ?? ""}
                    />
                  )}
                </div>
              ))}
            </div>
          </Panel>
          {error && (
            <Notice error title="Revisá tus respuestas">
              {error}
            </Notice>
          )}
          <div className="flex flex-wrap justify-between gap-3">
            <Button onClick={onBack} variant="outline">
              Volver
            </Button>
            <Button onClick={onSubmit}>
              <Check aria-hidden="true" />
              Enviar encuesta
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
