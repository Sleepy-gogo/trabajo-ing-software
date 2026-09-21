import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import QRCode from "react-qr-code"
import { Users, Landmark, ShieldCheck, ArrowUpRight } from "lucide-react"
import { useSession } from "@/hooks/use-session"
import { usersApi } from "@/lib/users-api"
import { membersApi, label } from "@/lib/members-api"
import { spacesApi } from "@/lib/spaces-api"
import {
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
} from "@/components/shared"
import { QueryState, Note } from "@/components/shared/real-data"
export function AdminDashboardPage() {
  const users = useQuery({
    queryKey: ["users", "overview"],
    queryFn: ({ signal }) => usersApi.list("", signal),
  })
  const members = useQuery({
    queryKey: ["members", "overview"],
    queryFn: ({ signal }) => membersApi.list("", "", "", signal),
  })
  const spaces = useQuery({
    queryKey: ["spaces", "overview"],
    queryFn: ({ signal }) => spacesApi.list("", signal),
  })
  return (
    <>
      <PageHeader
        title="Administración del polideportivo"
        description="Usuarios, membresías y espacios registrados en SERA."
        eyebrow="Adelanto · Incremento 3"
      />
      <QueryState
        pending={users.isPending || members.isPending || spaces.isPending}
        error={users.error ?? members.error ?? spaces.error}
        retry={() => {
          void users.refetch()
          void members.refetch()
          void spaces.refetch()
        }}
      >
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <StatCard
            label="Usuarios registrados"
            value={String(users.data?.length ?? 0)}
            icon={Users}
          />
          <StatCard
            label="Relaciones por verificar"
            value={String(
              members.data?.filter(
                (s) => s.estadoVerificacionUnse === "PENDIENTE"
              ).length ?? 0
            )}
            icon={ShieldCheck}
          />
          <StatCard
            label="Espacios habilitados"
            value={String(
              spaces.data?.filter((s) => s.estado === "HABILITADO").length ?? 0
            )}
            icon={Landmark}
          />
        </div>
      </QueryState>
      <div className="grid gap-5 md:grid-cols-2">
        {[
          {
            title: "Socios y membresías",
            description:
              "Verificá relaciones con la UNSE y consultá las solicitudes de membresía.",
            to: "/admin/members",
          },
          {
            title: "Espacios y disponibilidad",
            description:
              "Configurá los horarios semanales, excepciones y tarifas por relación.",
            to: "/admin/spaces",
          },
          {
            title: "Niveles y precios",
            description:
              "Definí los planes disponibles para contratar y sus beneficios.",
            to: "/admin/levels",
          },
          {
            title: "Usuarios",
            description: "Administrá cuentas, permisos y datos personales.",
            to: "/admin/users",
          },
        ].map((v) => (
          <SectionCard key={v.to} title={v.title}>
            <p className="mb-5 text-sm text-muted-foreground">
              {v.description}
            </p>
            <Link
              to={v.to}
              className="inline-flex min-h-10 items-center gap-2 text-sm font-semibold text-primary"
            >
              Abrir sección
              <ArrowUpRight className="size-4" />
            </Link>
          </SectionCard>
        ))}
      </div>
    </>
  )
}
export function MemberHomePage() {
  const session = useSession()
  const member = useQuery({
    queryKey: ["my-member"],
    queryFn: ({ signal }) => membersApi.me(signal),
  })
  return (
    <>
      <PageHeader
        title={`Hola, ${session.data?.nombreCompleto.split(" ")[0] ?? ""}`}
        description="Tu espacio en el Polideportivo UNSE."
      />
      <QueryState
        pending={member.isPending}
        error={member.error}
        retry={member.refetch}
      >
        <SectionCard
          title={member.data?.nivelMembresiaNombre ?? "Tu membresía"}
          action={
            <StatusBadge
              tone={
                member.data?.estadoMembresia === "ACTIVA"
                  ? "success"
                  : "neutral"
              }
            >
              {label(member.data?.estadoMembresia)}
            </StatusBadge>
          }
        >
          <p className="mb-5 text-sm text-muted-foreground">
            {member.data?.membresiaId
              ? "Consultá el estado de tu solicitud y los datos de tu membresía."
              : "Conocé los niveles y solicitá tu membresía cuando quieras."}
          </p>
          <Link
            className="text-sm font-semibold text-primary underline"
            to={
              member.data?.membresiaId
                ? "/app/memberships/status"
                : "/app/memberships"
            }
          >
            {member.data?.membresiaId
              ? "Ver mi membresía"
              : "Consultar niveles"}
          </Link>
        </SectionCard>
      </QueryState>
      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        <SectionCard title="Espacios del polideportivo">
          <p className="mb-4 text-sm text-muted-foreground">
            Consultá los espacios, sus tarifas y los horarios disponibles.
          </p>
          <Link
            className="text-sm font-semibold text-primary underline"
            to="/app/services"
          >
            Explorar espacios
          </Link>
        </SectionCard>
        <SectionCard title="Tus datos">
          <p className="mb-4 text-sm text-muted-foreground">
            Mantené actualizados tus datos personales y de contacto.
          </p>
          <Link
            className="text-sm font-semibold text-primary underline"
            to="/app/profile"
          >
            Ir a mi perfil
          </Link>
        </SectionCard>
      </div>
    </>
  )
}
export function MemberCardPage() {
  const session = useSession()
  const member = useQuery({
    queryKey: ["my-member"],
    queryFn: ({ signal }) => membersApi.me(signal),
  })
  return (
    <>
      <PageHeader
        title="Mi carnet"
        description="Tu identificación personal en el polideportivo."
      />
      <QueryState
        pending={member.isPending || session.isPending}
        error={member.error ?? session.error}
        retry={() => {
          void member.refetch()
          void session.refetch()
        }}
      >
        <SectionCard className="mx-auto max-w-md text-center">
          <p className="text-sm font-semibold text-primary">
            POLIDEPORTIVO UNSE
          </p>
          <h2 className="mt-3 text-xl font-bold">
            {session.data?.nombreCompleto}
          </h2>
          <p className="my-2 text-sm text-muted-foreground">
            DNI {session.data?.dni}
          </p>
          <div className="my-4">
            <StatusBadge
              tone={
                member.data?.estadoMembresia === "ACTIVA"
                  ? "success"
                  : "warning"
              }
            >
              {label(member.data?.estadoMembresia)}
            </StatusBadge>
          </div>
          {session.data?.qrUsuario && (
            <div className="mx-auto max-w-56 rounded-xl border bg-white p-5">
              <QRCode
                value={session.data.qrUsuario}
                className="h-auto w-full"
              />
            </div>
          )}
          <p className="mt-4 text-xs break-all text-muted-foreground">
            {session.data?.qrUsuario}
          </p>
          <Note>
            Este QR identifica tu cuenta. La autorización de ingreso dependerá
            de la membresía o reserva vigente cuando se habilite el control de
            accesos.
          </Note>
        </SectionCard>
      </QueryState>
    </>
  )
}
