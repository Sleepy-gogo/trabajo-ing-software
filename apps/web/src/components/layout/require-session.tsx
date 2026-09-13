import { Navigate, Outlet } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { useSession, homeFor } from "@/hooks/use-session"
import type { UserRole } from "@/lib/users-api"
export function RequireSession({ roles }: { roles?: UserRole[] }) {
  const session = useSession()
  if (session.isPending)
    return (
      <p role="status" className="p-8">
        Cargando tu cuenta…
      </p>
    )
  if (session.isError)
    return (
      <div className="p-8">
        <p role="alert">{session.error.message}</p>
        <Button onClick={() => void session.refetch()}>Reintentar</Button>
      </div>
    )
  if (!session.data) return <Navigate to="/login" replace />
  if (roles && !roles.includes(session.data.rol))
    return <Navigate to={homeFor(session.data.rol)} replace />
  return <Outlet />
}
