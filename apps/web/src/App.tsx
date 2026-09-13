import { RequireSession } from "@/components/layout/require-session"
import { Navigate, Route, Routes } from "react-router-dom"

import { AppShell } from "@/components/layout"
import { ForgotPasswordPage, LoginPage, RegisterPage } from "@/pages/auth"
import {
  BookingPage,
  MemberCardPage,
  MemberHomePage,
  MemberPaymentsPage,
  MemberProfilePage,
  MemberReservationDetailPage,
  MemberReservationsPage,
  MemberServiceDetailPage,
  MemberServicesPage,
  MemberSurveysPage,
  MembershipsPage,
  MembershipStatusPage,
} from "@/pages/member"
import {
  AdminDashboardPage,
  AdminPaymentsPage,
  AdminReservationsPage,
  AdminSpaceDetailPage,
  AdminSpacesPage,
  MembersPage,
  RecentReportsPage,
  ReportsPage,
  SettingsPage,
  UsersPage,
} from "@/pages/admin"
import { AccessPage } from "@/pages/staff"

export function App() {
  return (
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
          <Route element={<MembershipStatusPage />} path="memberships/status" />
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
  )
}

export default App
