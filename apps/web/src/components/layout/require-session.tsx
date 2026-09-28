import { Navigate, Outlet, useLocation } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { useSession, homeFor } from "@/hooks/use-session"
import type { UserRole } from "@/lib/users-api"
export function RequireSession({ roles }: { roles?: UserRole[] }) {
  const session = useSession()
  const location = useLocation()
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
  if (!session.data)
    return (
      <Navigate
        to="/login"
        state={{
          returnTo: location.pathname + location.search + location.hash,
        }}
        replace
      />
    )
  if (roles && !roles.includes(session.data.rol))
    return <Navigate to={homeFor(session.data.rol)} replace />
  return <Outlet />
}
