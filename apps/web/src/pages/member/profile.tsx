import { useState } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useSession } from "@/hooks/use-session"
import { usersApi, type UserInput } from "@/lib/users-api"
import { UserForm } from "@/components/shared/user-form"
import { PageHeader, SectionCard } from "@/components/shared"

export function MemberProfilePage() {
  const session = useSession()
  const client = useQueryClient()
  const [notice, setNotice] = useState("")
  const save = useMutation({
    mutationFn: (data: UserInput) =>
      usersApi.profile({
        nombreCompleto: data.nombreCompleto,
        email: data.email,
        dni: data.dni,
      }),
    onMutate: () => setNotice(""),
    onSuccess: (user) => {
      client.setQueryData(["session"], user)
      setNotice("Tus datos se guardaron.")
    },
  })
  if (!session.data) return null
  return (
    <div>
      <PageHeader
        title="Mi perfil"
        description="Consultá y actualizá los datos de tu cuenta."
      />
      <p role="status" className="mb-4 text-sm">
        {notice}
      </p>
      <SectionCard className="max-w-xl">
        <UserForm
          user={session.data}
          pending={save.isPending}
          error={save.error}
          onSave={(data) => save.mutate(data)}
        />
      </SectionCard>
    </div>
  )
}
