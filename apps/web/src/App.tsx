import { lazy, Suspense } from "react"
import { LevelsPage } from "@/pages/admin/levels"
import { RequireSession } from "@/components/layout/require-session"
import { Navigate, Route, Routes } from "react-router-dom"

import { AppShell } from "@/components/layout"
import { ForgotPasswordPage, LoginPage, RegisterPage } from "@/pages/auth"
const BookingPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.BookingPage }))
)
const MemberCardPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberCardPage }))
)
const MemberHomePage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberHomePage }))
)
const MemberPaymentsPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberPaymentsPage }))
)
const MemberProfilePage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberProfilePage }))
)
const MemberReservationDetailPage = lazy(() =>
  import("@/pages/member").then((m) => ({
    default: m.MemberReservationDetailPage,
  }))
)
const MemberReservationsPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberReservationsPage }))
)
const MemberServiceDetailPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberServiceDetailPage }))
)
const MemberServicesPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberServicesPage }))
)
const MemberSurveysPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MemberSurveysPage }))
)
const MembershipsPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MembershipsPage }))
)
const MembershipStatusPage = lazy(() =>
  import("@/pages/member").then((m) => ({ default: m.MembershipStatusPage }))
)
const AdminDashboardPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.AdminDashboardPage }))
)
const AdminPaymentsPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.AdminPaymentsPage }))
)
const AdminReservationsPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.AdminReservationsPage }))
)
const AdminSpaceDetailPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.AdminSpaceDetailPage }))
)
const AdminSpacesPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.AdminSpacesPage }))
)
const MembersPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.MembersPage }))
)
const RecentReportsPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.RecentReportsPage }))
)
const ReportsPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.ReportsPage }))
)
const SettingsPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.SettingsPage }))
)
const UsersPage = lazy(() =>
  import("@/pages/admin").then((m) => ({ default: m.UsersPage }))
)
import { AccessPage } from "@/pages/staff"

export function App() {
  return (
    <Suspense
      fallback={
        <p role="status" className="p-8 text-sm text-muted-foreground">
          Cargando página…
        </p>
      }
    >
      <Routes>
        <Route element={<Navigate replace to="/app/profile" />} path="/" />
        <Route element={<LoginPage />} path="/login" />
        <Route element={<RegisterPage />} path="/register" />
        <Route element={<ForgotPasswordPage />} path="/forgot-password" />

        <Route element={<RequireSession />}>
          <Route element={<AppShell role="member" />} path="/app">
            <Route index element={<MemberHomePage />} />
            <Route element={<MemberProfilePage />} path="profile" />
            <Route element={<MemberCardPage />} path="card" />
            <Route element={<MembershipsPage />} path="memberships" />
            <Route element={<MembershipsPage />} path="memberships/:id" />
            <Route
              element={<MembershipStatusPage />}
              path="memberships/status"
            />
            <Route element={<MemberPaymentsPage />} path="payments" />
            <Route element={<MemberServicesPage />} path="services" />
            <Route element={<MemberServiceDetailPage />} path="services/:id" />
            <Route element={<MemberReservationsPage />} path="reservations" />
            <Route element={<BookingPage />} path="reservations/new" />
            <Route element={<BookingPage />} path="reservations/new/:step" />
            <Route
              element={<MemberReservationDetailPage />}
              path="reservations/:id"
            />
            <Route element={<MemberSurveysPage />} path="surveys" />
            <Route element={<MemberSurveysPage />} path="surveys/:id" />
          </Route>
        </Route>
        <Route element={<RequireSession roles={["ADMIN", "STAFF"]} />}>
          <Route element={<AppShell role="staff" />} path="/admin/access">
            <Route index element={<AccessPage />} />
          </Route>
        </Route>
        <Route element={<RequireSession roles={["ADMIN"]} />}>
          <Route element={<AppShell role="admin" />} path="/admin">
            <Route index element={<AdminDashboardPage />} />
            <Route element={<UsersPage />} path="users" />
            <Route element={<UsersPage />} path="users/:id" />
            <Route element={<MembersPage />} path="members" />
            <Route element={<LevelsPage />} path="levels" />
            <Route element={<MembersPage />} path="members/:id" />
            <Route element={<AdminPaymentsPage />} path="payments" />
            <Route element={<AdminReservationsPage />} path="reservations" />
            <Route element={<AdminSpacesPage />} path="spaces" />
            <Route element={<AdminSpaceDetailPage />} path="spaces/:id" />
            <Route element={<ReportsPage />} path="reports" />
            <Route element={<RecentReportsPage />} path="reports/recent" />
            <Route element={<SettingsPage />} path="settings" />
          </Route>
        </Route>
        <Route element={<Navigate replace to="/app/profile" />} path="*" />
      </Routes>
    </Suspense>
  )
}

export default App
