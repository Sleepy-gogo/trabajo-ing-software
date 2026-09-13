import { useQuery } from "@tanstack/react-query"
import { ApiError, usersApi, type UserRole } from "@/lib/users-api"

export function useSession() {
  return useQuery({
    queryKey: ["session"],
    queryFn: async ({ signal }) => {
      try {
        return await usersApi.me(signal)
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null
        throw error
      }
    },
    staleTime: 0,
    refetchOnWindowFocus: true,
    refetchInterval: 60_000,
  })
}

export function homeFor(role: UserRole) {
  return role === "ADMIN"
    ? "/admin/users"
    : role === "STAFF"
      ? "/admin/access"
      : "/app/profile"
}
