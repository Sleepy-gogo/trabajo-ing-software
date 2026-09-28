import { useCallback, useEffect, useRef, useState } from "react"
import { useMutation } from "@tanstack/react-query"
import QrScanner from "qr-scanner"
import { Camera, CheckCircle2, ShieldX } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader, SectionCard } from "@/components/shared"
import { Field, ErrorMessage } from "@/components/shared/real-data"
import { accessApi } from "@/lib/access-api"
import { formatDate } from "@/lib/format"

function CameraScanner({
  onCode,
  onClose,
}: {
  onCode: (code: string) => void
  onClose: () => void
}) {
  const video = useRef<HTMLVideoElement>(null)
  const [error, setError] = useState("")
  useEffect(() => {
    if (!video.current) return
    let active = true
    let decoded = false
    const scanner = new QrScanner(
      video.current,
      (result) => {
        if (!active || decoded) return
        decoded = true
        scanner.stop()
        onCode(result.data)
      },
      {
        preferredCamera: "environment",
        maxScansPerSecond: 5,
        returnDetailedScanResult: true,
      }
    )
    void scanner.start().catch(() => {
      if (active)
        setError(
          "No se pudo abrir la cámara. Revisá el permiso del navegador o ingresá el código manualmente."
        )
    })
    return () => {
      active = false
      scanner.destroy()
    }
  }, [onCode])
  return (
    <div className="space-y-3">
      <video
        ref={video}
        muted
        playsInline
        aria-label="Vista de la cámara para leer el QR"
        className="aspect-video w-full rounded-xl bg-slate-950 object-cover"
      />
      {error ? (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      ) : (
        <p role="status" className="text-sm text-muted-foreground">
          Apuntá al QR. La cámara se detiene al leerlo.
        </p>
      )}
      <Button variant="outline" onClick={onClose}>
        Cerrar cámara
      </Button>
    </div>
  )
}

export function AccessPage() {
  const [camera, setCamera] = useState(false)
  const [code, setCode] = useState("")
  const [imageError, setImageError] = useState("")
  const [reading, setReading] = useState(false)
  const imageAttempt = useRef(0)
  useEffect(
    () => () => {
      imageAttempt.current++
    },
    []
  )
  const validation = useMutation({
    mutationFn: ({
      codigo,
      confirmar = false,
    }: {
      codigo: string
      confirmar?: boolean
    }) => (confirmar ? accessApi.confirm(codigo) : accessApi.validate(codigo)),
  })
  const { mutate, reset } = validation
  const submitCode = useCallback(
    (value: string) => {
      setCamera(false)
      setCode(value)
      setImageError("")
      mutate({ codigo: value.trim() })
    },
    [mutate]
  )
  const busy = validation.isPending || reading
  const result = validation.data
  const secureCamera =
    window.isSecureContext && !!navigator.mediaDevices?.getUserMedia
  return (
    <>
      <PageHeader
        title="Validar ingreso"
        description="Escaneá un carnet o una reserva. SERA verifica su vigencia en este momento."
      />
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <SectionCard
          title="Leer un QR"
          description="Usá la cámara o una imagen del código."
        >
          <div className="space-y-5">
            {camera ? (
              <CameraScanner
                onCode={submitCode}
                onClose={() => setCamera(false)}
              />
            ) : (
              <>
                <div className="flex min-h-40 flex-col items-center justify-center gap-4 rounded-xl border border-dashed bg-muted/40 p-6 text-center">
                  <Camera
                    className="size-8 text-muted-foreground"
                    aria-hidden="true"
                  />
                  <Button
                    disabled={busy || !secureCamera}
                    onClick={() => {
                      reset()
                      setImageError("")
                      setCamera(true)
                    }}
                  >
                    Activar cámara
                  </Button>
                  {!secureCamera && (
                    <p className="text-sm text-muted-foreground">
                      La cámara necesita HTTPS o localhost. Podés subir una
                      imagen o escribir el código.
                    </p>
                  )}
                </div>
                <Field
                  label="Imagen del QR"
                  type="file"
                  accept="image/*"
                  disabled={busy}
                  onChange={async (event) => {
                    const file = event.target.files?.[0]
                    event.target.value = ""
                    if (!file) return
                    const attempt = ++imageAttempt.current
                    reset()
                    setImageError("")
                    setReading(true)
                    try {
                      const decoded = await QrScanner.scanImage(file, {
                        returnDetailedScanResult: true,
                      })
                      if (attempt === imageAttempt.current)
                        submitCode(decoded.data)
                    } catch {
                      if (attempt === imageAttempt.current)
                        setImageError(
                          "No encontramos un QR en la imagen. Probá una imagen más nítida o ingresá el código."
                        )
                    } finally {
                      if (attempt === imageAttempt.current) setReading(false)
                    }
                  }}
                />
              </>
            )}
            {reading && (
              <p role="status" className="text-sm">
                Leyendo imagen…
              </p>
            )}
            {imageError && (
              <p role="alert" className="text-sm text-destructive">
                {imageError}
              </p>
            )}
            <form
              className="space-y-3 border-t pt-5"
              onSubmit={(event) => {
                event.preventDefault()
                if (!busy) submitCode(code)
              }}
            >
              <Field
                label="Código de reserva o carnet"
                required
                maxLength={128}
                value={code}
                disabled={busy}
                autoComplete="off"
                spellCheck={false}
                onChange={(event) => {
                  setCode(event.target.value)
                  reset()
                  setImageError("")
                }}
                placeholder="SERA-…"
              />
              <Button type="submit" disabled={busy}>
                {validation.isPending ? "Verificando…" : "Validar código"}
              </Button>
            </form>
          </div>
        </SectionCard>
        <SectionCard title="Resultado de la validación">
          <div role="status" aria-live="polite">
            {validation.isPending ? (
              <p>Consultando el estado actual…</p>
            ) : result ? (
              <div className="space-y-5">
                <div
                  className={`rounded-xl border p-5 ${result.autorizado || result.consumidaEn ? "border-emerald-200 bg-emerald-50 text-emerald-950" : "border-red-200 bg-red-50 text-red-950"}`}
                >
                  {result.autorizado || result.consumidaEn ? (
                    <CheckCircle2 className="mb-3 size-7" aria-hidden="true" />
                  ) : (
                    <ShieldX className="mb-3 size-7" aria-hidden="true" />
                  )}
                  <h2 className="text-xl font-semibold">
                    {result.autorizado || result.consumidaEn
                      ? result.consumidaEn
                        ? "Reserva consumida"
                        : "Acceso habilitado"
                      : "Acceso no habilitado"}
                  </h2>
                  <p className="mt-2 text-sm">{result.motivo}</p>
                </div>
                {result.titular && (
                  <dl className="space-y-4 text-sm">
                    <div>
                      <dt className="text-muted-foreground">Titular</dt>
                      <dd className="mt-1 font-semibold">{result.titular}</dd>
                    </div>
                    {result.espacio && (
                      <div>
                        <dt className="text-muted-foreground">Espacio</dt>
                        <dd className="mt-1 font-semibold">{result.espacio}</dd>
                      </div>
                    )}
                    {result.fecha && (
                      <div>
                        <dt className="text-muted-foreground">
                          Fecha y horario
                        </dt>
                        <dd className="mt-1 font-semibold">
                          {formatDate(result.fecha)} ·{" "}
                          {result.desde?.slice(0, 5)} a{" "}
                          {result.hasta?.slice(0, 5)}
                        </dd>
                      </div>
                    )}
                  </dl>
                )}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                El resultado aparecerá después de leer o ingresar un código.
              </p>
            )}
          </div>
          {result?.autorizado && result.tipo === "RESERVA" && !busy && (
            <Button
              className="mt-5"
              onClick={() => mutate({ codigo: code.trim(), confirmar: true })}
            >
              Confirmar ingreso y consumir reserva
            </Button>
          )}
          <ErrorMessage error={validation.error} />
          <p className="mt-6 border-t pt-4 text-xs text-muted-foreground">
            Escanear solo consulta la vigencia. Confirmar el ingreso consume la
            reserva y evita un segundo uso del QR. Los carnets personales no se
            consumen.
          </p>
        </SectionCard>
      </div>
    </>
  )
}
