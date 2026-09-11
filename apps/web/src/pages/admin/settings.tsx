import { useMemo, useState } from "react"
import {
  Bell,
  Check,
  LockKeyhole,
  Save,
  Settings2,
  ShieldCheck,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { PageHeader, SectionCard } from "@/components/shared"
import { roles } from "@/mocks"
import type { Capability, Role } from "@/types"

const capabilities: {
  value: Capability
  label: string
  description: string
}[] = [
  {
    value: "consultar_servicios",
    label: "Consultar servicios",
    description: "Puede ver espacios y condiciones de uso.",
  },
  {
    value: "reservar_espacio",
    label: "Reservar espacios",
    description: "Puede crear y consultar sus reservas.",
  },
  {
    value: "gestionar_perfil",
    label: "Gestionar perfil",
    description: "Puede modificar sus datos personales.",
  },
  {
    value: "gestionar_socios",
    label: "Gestionar socios",
    description: "Puede revisar y editar membresías.",
  },
  {
    value: "gestionar_espacios",
    label: "Gestionar espacios",
    description: "Puede configurar espacios y disponibilidad.",
  },
  {
    value: "gestionar_pagos",
    label: "Gestionar pagos",
    description: "Puede registrar y conciliar pagos.",
  },
  {
    value: "validar_accesos",
    label: "Validar accesos",
    description: "Puede autorizar ingresos y revisar historial.",
  },
  {
    value: "gestionar_reportes",
    label: "Gestionar informes",
    description: "Puede generar y exportar informes.",
  },
  {
    value: "gestionar_usuarios",
    label: "Gestionar usuarios",
    description: "Puede administrar cuentas y roles.",
  },
  {
    value: "gestionar_configuracion",
    label: "Gestionar configuración",
    description: "Puede modificar parámetros de operación.",
  },
]

export function SettingsPage() {
  const [role, setRole] = useState<Role>("administrador")
  const [saved, setSaved] = useState(false)
  const selectedRole = roles.find((item) => item.value === role) ?? roles[0]
  const [enabled, setEnabled] = useState<Record<Capability, boolean>>(
    () =>
      Object.fromEntries(
        capabilities.map((capability) => [capability.value, false])
      ) as Record<Capability, boolean>
  )
  const roleDefaults = useMemo(
    () =>
      new Set(
        selectedRole?.value === "administrador"
          ? [
              "consultar_servicios",
              "gestionar_socios",
              "gestionar_espacios",
              "gestionar_pagos",
              "validar_accesos",
              "gestionar_reportes",
            ]
          : selectedRole?.value === "administrador_sistema"
            ? ["gestionar_usuarios", "gestionar_configuracion"]
            : selectedRole?.value === "personal_acceso"
              ? ["consultar_servicios", "validar_accesos"]
              : ["consultar_servicios"]
      ),
    [selectedRole]
  )

  function hasCapability(capability: Capability) {
    return enabled[capability] || roleDefaults.has(capability)
  }

  function toggleCapability(capability: Capability, checked: boolean) {
    setSaved(false)
    setEnabled((current) => ({ ...current, [capability]: checked }))
  }

  return (
    <div>
      <PageHeader
        title="Configuración"
        description="Definí roles, permisos y preferencias operativas de SERA."
      />
      <Tabs defaultValue="roles">
        <TabsList className="mb-6 grid w-full grid-cols-3 sm:w-auto">
          <TabsTrigger value="roles">
            <ShieldCheck />
            Roles y permisos
          </TabsTrigger>
          <TabsTrigger value="general">
            <Settings2 />
            General
          </TabsTrigger>
          <TabsTrigger value="notifications">
            <Bell />
            Avisos
          </TabsTrigger>
        </TabsList>
        <TabsContent value="roles">
          <div className="grid gap-5 lg:grid-cols-[280px_1fr]">
            <SectionCard
              title="Roles del sistema"
              description="Seleccioná un rol para revisar sus capacidades."
            >
              <div className="space-y-2">
                {roles
                  .filter((item) => item.value !== "visitante")
                  .map((item) => (
                    <button
                      key={item.value}
                      type="button"
                      onClick={() => {
                        setRole(item.value)
                        setSaved(false)
                      }}
                      className={`w-full rounded-lg border px-4 py-3 text-left transition-colors ${item.value === role ? "border-primary bg-primary/5" : "hover:bg-muted/50"}`}
                    >
                      <p className="text-sm font-semibold">{item.label}</p>
                      <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                        {item.descripcion}
                      </p>
                    </button>
                  ))}
              </div>
            </SectionCard>
            <SectionCard
              title={selectedRole?.label ?? "Rol"}
              description="Los cambios se aplican a las nuevas sesiones y quedan registrados."
            >
              <div className="mb-5 flex items-start gap-3 rounded-lg bg-muted/50 p-4">
                <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <LockKeyhole className="size-4" />
                </span>
                <div>
                  <p className="text-sm font-semibold">Permisos del rol</p>
                  <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                    Las acciones sensibles siempre requieren validación del
                    backend.
                  </p>
                </div>
              </div>
              <div className="divide-y">
                {capabilities.map((capability) => (
                  <div
                    key={capability.value}
                    className="flex items-center justify-between gap-4 py-4"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium">{capability.label}</p>
                      <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                        {capability.description}
                      </p>
                    </div>
                    <Switch
                      aria-label={`Permiso ${capability.label}`}
                      checked={hasCapability(capability.value)}
                      onCheckedChange={(checked) =>
                        toggleCapability(capability.value, checked)
                      }
                    />
                  </div>
                ))}
              </div>
              <div className="mt-5 flex items-center justify-between gap-3 border-t pt-5">
                <p className="text-xs text-muted-foreground">
                  {saved
                    ? "Cambios guardados en esta vista previa."
                    : "Hay cambios sin guardar."}
                </p>
                <Button onClick={() => setSaved(true)}>
                  <Save />
                  Guardar permisos
                </Button>
              </div>
            </SectionCard>
          </div>
        </TabsContent>
        <TabsContent value="general">
          <div className="grid gap-5 lg:grid-cols-2">
            <SectionCard
              title="Datos del polideportivo"
              description="Información visible en comunicaciones y comprobantes."
            >
              <div className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="club-name">Nombre visible</Label>
                  <Input id="club-name" defaultValue="Polideportivo UNSE" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="club-address">Ubicación</Label>
                  <Input
                    id="club-address"
                    defaultValue="Santiago del Estero, Argentina"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="club-email">Email de contacto</Label>
                  <Input
                    id="club-email"
                    defaultValue="deportes@unse.edu.ar"
                    type="email"
                  />
                </div>
                <Button onClick={() => setSaved(true)}>
                  <Check />
                  Guardar datos
                </Button>
              </div>
            </SectionCard>
            <SectionCard
              title="Preferencias operativas"
              description="Valores de referencia para la administración."
            >
              <div className="space-y-4">
                <Preference
                  label="Solicitar motivo en cambios sensibles"
                  description="Pagos, tarifas, cancelaciones y accesos forzados."
                  defaultChecked
                />
                <Preference
                  label="Mostrar avisos de mantenimiento"
                  description="Informa a usuarios sobre espacios no disponibles."
                  defaultChecked
                />
                <Preference
                  label="Permitir reservas con pago pendiente"
                  description="Mantiene el turno bloqueado hasta la confirmación."
                />
              </div>
            </SectionCard>
          </div>
        </TabsContent>
        <TabsContent value="notifications">
          <SectionCard
            title="Avisos y notificaciones"
            description="Configurá qué eventos aparecen en el panel administrativo."
          >
            <div className="max-w-2xl divide-y">
              {[
                "Nueva reserva confirmada",
                "Pago pendiente de conciliación",
                "Membresía próxima a vencer",
                "Espacio en mantenimiento",
                "Acceso rechazado",
              ].map((item, index) => (
                <Preference
                  key={item}
                  label={item}
                  description={
                    index === 0
                      ? "Recibí una notificación cuando se confirma una reserva."
                      : "Mantener este aviso visible para el equipo responsable."
                  }
                  defaultChecked={index !== 4}
                />
              ))}
            </div>
          </SectionCard>
        </TabsContent>
      </Tabs>
    </div>
  )
}

function Preference({
  label,
  description,
  defaultChecked = false,
}: {
  label: string
  description: string
  defaultChecked?: boolean
}) {
  const [checked, setChecked] = useState(defaultChecked)
  return (
    <div className="flex items-center justify-between gap-4 py-4">
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
          {description}
        </p>
      </div>
      <Switch
        checked={checked}
        onCheckedChange={setChecked}
        aria-label={label}
      />
    </div>
  )
}

export { SettingsPage as AdminSettingsPage }
