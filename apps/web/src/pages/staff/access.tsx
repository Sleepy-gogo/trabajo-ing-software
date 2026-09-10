import { useMemo, useState } from "react"
import { useSearchParams } from "react-router-dom"
import {
  AlertTriangle,
  CheckCircle2,
  History,
  KeyRound,
  LoaderCircle,
  QrCode,
  ScanLine,
  ShieldAlert,
  UserRound,
  XCircle,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DataToolbar,
  EmptyState,
  ImagePlaceholder,
  PageHeader,
  SectionCard,
  StatusBadge,
} from "@/components/shared"
import { accessRecords, accessScenarios, spaces, users } from "@/mocks"
import { formatDateTime } from "@/lib/format"

type AccessTab = "scan" | "code" | "manual" | "history"
type AccessState =
  | "idle"
  | "loading"
  | "authorized"
  | "rejected"
  | "invalid"
  | "cancelled"
  | "forced"

const tabFromParam = (value: string | null): AccessTab =>
  value === "code" || value === "manual" || value === "history" ? value : "scan"

export function AccessPage() {
  const [params, setParams] = useSearchParams()
  const [tab, setTab] = useState<AccessTab>(() =>
    tabFromParam(params.get("tab"))
  )
  const [state, setState] = useState<AccessState>("idle")
  const [scenario, setScenario] = useState("access-authorized-member")
  const [code, setCode] = useState("")
  const [manualUser, setManualUser] = useState(
    users.find((user) => user.id === "usr-003")?.id ?? ""
  )
  const [manualSpace, setManualSpace] = useState(spaces[0]?.id ?? "")
  const [forced, setForced] = useState(false)
  const [reason, setReason] = useState("")
  const [historySearch, setHistorySearch] = useState("")
  const [notice, setNotice] = useState("")

  function changeTab(value: string | null) {
    if (!value) return
    const next = tabFromParam(value)
    setTab(next)
    setState("idle")
    setNotice("")
    setParams(next === "scan" ? {} : { tab: next })
  }

  function runScan() {
    setState("loading")
    window.setTimeout(() => {
      if (scenario === "access-authorized-member") setState("authorized")
      else if (scenario === "access-invalid-qr") setState("invalid")
      else if (scenario === "access-cancelled") setState("cancelled")
      else setState("rejected")
    }, 300)
  }

  function validateCode() {
    setState("loading")
    window.setTimeout(() => {
      if (code.trim() === "SERA-260825-842") setState("authorized")
      else if (code.trim() === "SERA-260824-310") setState("cancelled")
      else setState("invalid")
    }, 300)
  }

  function registerManual() {
    if (!manualUser || !manualSpace) {
      setNotice(
        "Seleccioná una persona y un espacio para registrar el ingreso."
      )
      return
    }
    if (forced && !reason.trim()) {
      setNotice("Escribí el motivo de la autorización forzada.")
      return
    }
    setNotice("El ingreso manual quedó registrado en esta vista previa.")
    setState(forced ? "forced" : "authorized")
  }

  const filteredHistory = useMemo(
    () =>
      accessRecords.filter((record) =>
        `${record.personName} ${record.spaceName} ${record.resultadoLabel}`
          .toLowerCase()
          .includes(historySearch.toLowerCase())
      ),
    [historySearch]
  )

  return (
    <div>
      <PageHeader
        title="Control de accesos"
        description="Validá carnets y reservas antes de permitir el ingreso al polideportivo."
        actions={<StatusBadge tone="success">Personal autorizado</StatusBadge>}
      />
      {notice && (
        <p
          role="status"
          className="mb-5 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-xs text-blue-800"
        >
          {notice}
        </p>
      )}
      <Tabs value={tab} onValueChange={changeTab}>
        <TabsList className="mb-6 grid w-full grid-cols-2 sm:w-auto sm:grid-cols-4">
          <TabsTrigger value="scan">
            <QrCode />
            Escanear QR
          </TabsTrigger>
          <TabsTrigger value="code">
            <KeyRound />
            Código
          </TabsTrigger>
          <TabsTrigger value="manual">
            <UserRound />
            Manual
          </TabsTrigger>
          <TabsTrigger value="history">
            <History />
            Historial
          </TabsTrigger>
        </TabsList>
        <TabsContent value="scan">
          <div className="grid items-start gap-5 lg:grid-cols-[1fr_360px]">
            <SectionCard
              title="Escanear un código QR"
              description="Acercá el carnet digital o el QR de una reserva al lector."
            >
              <ImagePlaceholder
                label="Área de escaneo"
                icon={ScanLine}
                className="mx-auto aspect-[1.5] max-w-2xl rounded-xl border-2 border-dashed border-primary/30 bg-primary/5 text-primary"
              />
              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <Button
                  className="h-11"
                  onClick={runScan}
                  disabled={state === "loading"}
                >
                  {state === "loading" ? (
                    <>
                      <LoaderCircle className="animate-spin" />
                      Analizando código…
                    </>
                  ) : (
                    <>
                      <ScanLine />
                      Simular escaneo
                    </>
                  )}
                </Button>
                <Button
                  variant="outline"
                  className="h-11"
                  onClick={() => changeTab("code")}
                >
                  <KeyRound />
                  Ingresar código
                </Button>
              </div>
              <p className="mt-4 text-center text-xs text-muted-foreground">
                La cámara real se conectará cuando esté disponible. Esta vista
                muestra los estados operativos.
              </p>
            </SectionCard>
            <ScenarioPicker
              value={scenario}
              onChange={(value) => {
                setScenario(value)
                setState("idle")
              }}
            />
          </div>
          {state !== "idle" && (
            <AccessResultPanel state={state} onReset={() => setState("idle")} />
          )}
        </TabsContent>
        <TabsContent value="code">
          <div className="grid items-start gap-5 lg:grid-cols-[1fr_360px]">
            <SectionCard
              title="Validar reserva por código"
              description="Ingresá el código único que figura en la confirmación de la reserva."
            >
              <div className="mx-auto max-w-xl space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="reservation-code">Código de reserva</Label>
                  <Input
                    id="reservation-code"
                    value={code}
                    onChange={(event) =>
                      setCode(event.target.value.toUpperCase())
                    }
                    placeholder="SERA-260825-842"
                    className="h-12 font-mono tracking-wider"
                  />
                </div>
                <Button
                  className="h-11 w-full"
                  onClick={validateCode}
                  disabled={!code.trim() || state === "loading"}
                >
                  {state === "loading" ? (
                    <>
                      <LoaderCircle className="animate-spin" />
                      Comprobando…
                    </>
                  ) : (
                    "Validar reserva"
                  )}
                </Button>
                <p className="text-xs leading-relaxed text-muted-foreground">
                  Probá con SERA-260825-842 para un ingreso autorizado o
                  SERA-260824-310 para una reserva cancelada.
                </p>
              </div>
            </SectionCard>
            <SectionCard
              title="Qué se valida"
              description="El sistema revisa estos datos antes de habilitar el acceso."
            >
              <ul className="space-y-3 text-sm text-muted-foreground">
                <li className="flex gap-2">
                  <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" />
                  Estado de la reserva
                </li>
                <li className="flex gap-2">
                  <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" />
                  Fecha y horario vigente
                </li>
                <li className="flex gap-2">
                  <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" />
                  Espacio reservado
                </li>
              </ul>
            </SectionCard>
          </div>
          {state !== "idle" && (
            <AccessResultPanel state={state} onReset={() => setState("idle")} />
          )}
        </TabsContent>
        <TabsContent value="manual">
          <div className="grid items-start gap-5 lg:grid-cols-[1fr_360px]">
            <SectionCard
              title="Registrar ingreso manual"
              description="Usá esta opción cuando no sea posible leer el código y exista una razón operativa."
            >
              <div className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="manual-user">Persona</Label>
                  <select
                    id="manual-user"
                    className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                    value={manualUser}
                    onChange={(event) => setManualUser(event.target.value)}
                  >
                    {users
                      .filter((user) => user.id !== "usr-visitante")
                      .map((user) => (
                        <option key={user.id} value={user.id}>
                          {user.nombreCompleto} · {user.dni}
                        </option>
                      ))}
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="manual-space">Servicio o espacio</Label>
                  <select
                    id="manual-space"
                    className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                    value={manualSpace}
                    onChange={(event) => setManualSpace(event.target.value)}
                  >
                    {spaces.map((space) => (
                      <option key={space.id} value={space.id}>
                        {space.nombre}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
                  <div>
                    <p className="text-sm font-medium">Autorización forzada</p>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                      Usala solo con permiso administrativo.
                    </p>
                  </div>
                  <Switch
                    checked={forced}
                    onCheckedChange={setForced}
                    aria-label="Autorización forzada"
                  />
                </div>
                {forced && (
                  <div className="space-y-2">
                    <Label htmlFor="force-reason">
                      Motivo de la autorización
                    </Label>
                    <Input
                      id="force-reason"
                      value={reason}
                      onChange={(event) => setReason(event.target.value)}
                      placeholder="Actividad institucional autorizada"
                    />
                  </div>
                )}
                <Button className="h-11 w-full" onClick={registerManual}>
                  {forced
                    ? "Registrar autorización forzada"
                    : "Registrar ingreso"}
                </Button>
              </div>
            </SectionCard>
            <SectionCard
              title="Permiso requerido"
              description="Los ingresos forzados quedan diferenciados en el historial de auditoría."
            >
              <div className="rounded-lg bg-amber-50 p-4 text-xs leading-relaxed text-amber-900">
                <ShieldAlert className="mb-2 size-5" />
                Verificá la identidad y dejá un motivo claro antes de confirmar
                una excepción.
              </div>
            </SectionCard>
          </div>
          {state !== "idle" && (
            <AccessResultPanel state={state} onReset={() => setState("idle")} />
          )}
        </TabsContent>
        <TabsContent value="history">
          <SectionCard
            title="Historial de accesos"
            description="Ingresos autorizados, rechazados y excepciones registradas."
          >
            <DataToolbar
              search={historySearch}
              onSearchChange={setHistorySearch}
              placeholder="Buscar persona, espacio o resultado…"
            />
            {filteredHistory.length ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-xs">
                  <thead>
                    <tr className="border-b text-muted-foreground">
                      <th className="px-3 py-3 font-semibold">Fecha y hora</th>
                      <th className="px-3 py-3 font-semibold">Persona</th>
                      <th className="px-3 py-3 font-semibold">Método</th>
                      <th className="px-3 py-3 font-semibold">Espacio</th>
                      <th className="px-3 py-3 font-semibold">Resultado</th>
                      <th className="px-3 py-3 font-semibold">Responsable</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredHistory.map((record) => (
                      <tr key={record.id} className="border-b last:border-0">
                        <td className="px-3 py-4 text-muted-foreground">
                          {formatDateTime(record.fechaHora)}
                        </td>
                        <td className="px-3 py-4 font-medium">
                          {record.personName}
                        </td>
                        <td className="px-3 py-4">{record.metodoLabel}</td>
                        <td className="px-3 py-4">{record.spaceName}</td>
                        <td className="px-3 py-4">
                          <div className="flex flex-wrap gap-1.5">
                            <StatusBadge
                              tone={
                                record.resultado === "autorizado"
                                  ? "success"
                                  : "danger"
                              }
                            >
                              {record.resultadoLabel}
                            </StatusBadge>
                            {record.esAutorizacionForzada && (
                              <StatusBadge tone="warning">Forzada</StatusBadge>
                            )}
                          </div>
                          <p className="mt-1 max-w-xs text-[11px] text-muted-foreground">
                            {record.motivo}
                          </p>
                        </td>
                        <td className="px-3 py-4 text-muted-foreground">
                          {record.personalResponsable}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState
                title="No hay accesos con estos filtros"
                description="Probá con otro término de búsqueda."
              />
            )}
          </SectionCard>
        </TabsContent>
      </Tabs>
    </div>
  )
}

function ScenarioPicker({
  value,
  onChange,
}: {
  value: string
  onChange: (value: string) => void
}) {
  return (
    <SectionCard
      title="Estados para revisar"
      description="Elegí un escenario para recorrer la respuesta visual."
      className="h-fit"
    >
      <div className="space-y-2">
        {accessScenarios.map((item) => (
          <button
            type="button"
            key={item.id}
            onClick={() => onChange(item.id)}
            className={`w-full rounded-lg border px-3 py-3 text-left transition-colors ${value === item.id ? "border-primary bg-primary/5" : "hover:bg-muted/50"}`}
          >
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm font-semibold">{item.titulo}</span>
              <StatusBadge
                tone={item.estado === "autorizado" ? "success" : "danger"}
              >
                {item.estadoLabel}
              </StatusBadge>
            </div>
            <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
              {item.descripcion}
            </p>
          </button>
        ))}
      </div>
    </SectionCard>
  )
}

function AccessResultPanel({
  state,
  onReset,
}: {
  state: AccessState
  onReset: () => void
}) {
  if (state === "loading")
    return (
      <SectionCard className="mt-5">
        <div className="flex items-center gap-3">
          <LoaderCircle className="size-5 animate-spin text-primary" />
          <p className="text-sm font-medium">Validando datos de acceso…</p>
        </div>
      </SectionCard>
    )
  const authorized = state === "authorized" || state === "forced"
  const cancelled = state === "cancelled"
  const invalid = state === "invalid"
  const Icon = authorized ? CheckCircle2 : invalid ? AlertTriangle : XCircle
  const title = authorized
    ? state === "forced"
      ? "Acceso autorizado excepcionalmente"
      : "Acceso autorizado"
    : cancelled
      ? "Reserva cancelada"
      : invalid
        ? "Código no válido"
        : "Acceso rechazado"
  const description = authorized
    ? state === "forced"
      ? "La excepción quedó registrada con un motivo y responsable."
      : "La persona puede ingresar al espacio seleccionado."
    : cancelled
      ? "La reserva ya no permite el ingreso. Informá a la persona y revisá alternativas."
      : invalid
        ? "No encontramos una persona o reserva asociada al código."
        : "No se cumplen las condiciones para acceder al servicio seleccionado."
  return (
    <SectionCard
      className={`mt-5 border-2 ${authorized ? "border-emerald-300 bg-emerald-50/50" : "border-rose-300 bg-rose-50/50"}`}
    >
      <div className="flex flex-col items-center py-5 text-center">
        <Icon
          className={`size-14 ${authorized ? "text-emerald-600" : "text-rose-600"}`}
        />
        <h2 className="mt-4 text-xl font-bold">{title}</h2>
        <p className="mt-2 max-w-lg text-sm leading-relaxed text-muted-foreground">
          {description}
        </p>
        {authorized && (
          <div className="mt-5 grid w-full max-w-lg gap-3 rounded-lg border bg-white/70 p-4 text-left sm:grid-cols-2">
            <div>
              <p className="text-[11px] text-muted-foreground">Persona</p>
              <p className="mt-1 text-sm font-semibold">Axel Ignacio Castaño</p>
            </div>
            <div>
              <p className="text-[11px] text-muted-foreground">Espacio</p>
              <p className="mt-1 text-sm font-semibold">Cancha Fútbol 5</p>
            </div>
          </div>
        )}
        {!authorized && (
          <div className="mt-5 flex items-center gap-2 rounded-lg bg-white/70 px-4 py-3 text-xs text-rose-900">
            <ShieldAlert className="size-4" />
            No habilitar el ingreso hasta resolver la observación.
          </div>
        )}
        <Button
          variant={authorized ? "default" : "outline"}
          className="mt-6"
          onClick={onReset}
        >
          {authorized ? "Registrar otro acceso" : "Volver a validar"}
        </Button>
      </div>
    </SectionCard>
  )
}
