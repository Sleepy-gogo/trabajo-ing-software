# SERA frontend product specification

## 1. Purpose of this file

This file is the frontend product specification for SERA, the web system for the UNSE sports complex. It condenses the software requirements document `SI-II-2026-Plantilla-ERS-DOO.pdf` into implementation context for a coding agent.

Treat the PDF as the product source of truth. This file reorganizes that material around frontend work. It does not replace backend contracts, database design, or the academic UML artifacts.

The frontend must implement the product described here, not a generic court-booking dashboard.

## 2. Product definition

SERA centralizes the administrative and operational work of the UNSE sports complex. The current manual process uses spreadsheets, email, and scattered records. SERA brings the following areas into one web application:

- people and user accounts
- members and membership levels
- monthly membership payments
- spaces and services
- availability and reservations
- full payment for reservations
- reservation cancellation and reservation tickets or credit
- digital membership card and QR identification
- access validation
- usage records
- reports
- service surveys
- user and role administration

The application must work from desktop and mobile web browsers.

### Brand naming note

The source document uses two expansions for the SERA acronym. The cover and interface comps use wording equivalent to "Sistema de Gestión de Socios, Reservas y Accesos" while the introduction says "Sistema de Espacios, Reservas y Accesos". Keep the visible product name as `SERA`. Do not invent or normalize the long-form name in prominent UI until the team chooses one.

## 3. Scope for the prototype

The prototype must support the principal flows even when external integrations are not real.

The system must support manual entry for users, payments, memberships, reservations, spaces, and access records. The requirements also describe Mercado Pago flows. For the prototype, the frontend must contain the payment flow and payment-result states, but the payment provider may be mocked or simulated behind the interface.

The source is not fully consistent about payment channels. The system requirements mention bank transfer and cash registration, while later use cases model Mercado Pago. Do not hard-wire the frontend around one provider. Treat payment method as a variable. The prototype should be able to represent online/simulated payment, transfer pending confirmation, and a cash/manual payment recorded by an authorized administrator. The business rule stays the same: a reservation is not confirmed until the full amount has been recorded as paid.

The first version does not require:

- automatic SIU Guaraní integration
- a native Android or iOS app
- advanced survey analytics
- automatic WhatsApp notifications
- automatic email or mobile notification workflows
- automatic refunds

Do not remove the UI concepts for payments simply because Mercado Pago can be simulated. Payment state affects memberships and reservations throughout the product.

## 4. Non-negotiable product rules

These rules affect multiple screens and should be modeled consistently.

1. A person may register before becoming a member.
2. The administration manually validates the person's relationship with UNSE.
3. Prices and conditions may change according to the person's relationship with UNSE and membership level.
4. A reservation belongs to one space, one date, and a specific time or duration.
5. A reservation requires full payment before confirmation. There are no deposits in the specified version.
6. A confirmed reservation receives a unique reservation code and QR code.
7. Cancelling a reservation does not trigger an automatic refund.
8. A cancellation may create a reservation ticket that can be applied to a later reservation.
9. A ticket must be valid, unused, and owned by the user who applies it.
10. If a ticket covers only part of the new reservation price, the user pays the difference.
11. Once used, a ticket cannot be reused.
12. Membership benefits depend on membership status and payment status.
13. An overdue unpaid membership charge can suspend membership benefits.
14. Access to a service can depend on account state, membership state, membership level, and/or a reservation.
15. Access attempts must be recorded whether access is authorized or rejected.
16. Sensitive administrative actions must retain the responsible user and date.
17. The system must restrict features by role.
18. Personal information must not be exposed to unauthorized users.

## 5. User types and operational actors

The source document describes more operational actors than the simplified role list in RF-17. Model frontend permissions around capabilities. Do not assume that every actor name maps one-to-one to a database role.

### Visitor

An unauthenticated person who can:

- sign in
- recover a password
- create an account

### Registered user

An authenticated person who is not necessarily an active member. A registered user can:

- view and edit allowed profile data
- browse membership levels
- contract a membership
- browse spaces and services
- check availability
- create and pay for reservations
- view and cancel their reservations
- use reservation tickets
- view their payments
- answer surveys when eligible

### Member or socio

A registered user with a membership record. Depending on state, a member can:

- view membership status
- pay monthly membership charges
- configure recurring payment when available
- receive membership pricing and benefits
- use a digital membership card and personal QR
- cancel membership

Membership state can be active, expired, suspended, or cancelled.

### Staff or access personnel

Operational staff at the sports complex. They need a focused access-control interface to:

- scan a user's personal QR
- scan a reservation QR
- enter a reservation code manually
- register a manual access
- see authorization or rejection and the reason

### Administrator / sports complex manager

Administrative user with management permissions. Depending on granted permissions, this actor can:

- create and search users
- modify users and members
- validate membership and UNSE-related data
- register cash/manual payments
- manage membership information
- manage spaces, availability, blocks, and tariffs
- inspect reservations
- operate access-related workflows if allowed
- generate and load reports
- manage roles, users, tariffs, and general parameters

### System administrator

The document also mentions a technical or administrative system administrator for users, roles, tariffs, and general parameters. The prototype can share the same admin shell while permission checks determine which actions are visible.

## 6. Core domain concepts the frontend must understand

These are not database schemas. They are the minimum concepts needed to render consistent screens and mock data.

### User

Useful UI fields include:

- unique identifier
- name
- surname
- DNI
- email
- phone
- address, optional
- profile photo, optional
- student or staff/teacher ID, when applicable
- relationship with UNSE
- account status
- role or capabilities
- verification state

### UNSE relationship

The document requires the relationship with UNSE because it affects prices and benefits. Administration validates it manually in the prototype.

At minimum, the UI needs:

- declared relationship
- verification status
- optional legajo/student/staff identifier

Do not hard-code a final taxonomy beyond what the backend or mock fixtures define.

### Membership level

A membership level exposes:

- name
- price
- benefits
- included services
- conditions
- whether it is active/available for contracting
- pricing rules according to UNSE relationship when applicable

### Membership

A member-facing membership view needs:

- level
- state
- activation date
- expiration or next due date
- monthly charge status
- recurring payment status when applicable
- benefits/services currently available

Expected membership states mentioned in the requirements:

- active
- expired
- suspended
- cancelled

### Payment

Payments may belong to a membership charge or a reservation. The UI should support at least:

- concept
- amount
- date
- status
- payment method/source
- associated membership or reservation
- receipt/comprobante when available

Payment results must distinguish at least:

- pending
- approved
- rejected
- cancelled/abandoned
- expired when relevant to a reservation order

### Space / service

Examples from the document are football courts, quinchos, asadores, pool, SUM, and other sports or recreational spaces.

A space may expose:

- name
- description
- images
- current state
- registration date
- last maintenance date
- usage count
- availability calendar
- pricing by user/member category
- whether membership is required
- whether reservation is required

Explicit space states in the requirements:

- enabled / habilitado
- under maintenance / en mantenimiento
- unusable / inutilizable
- in use / en uso

### Availability

Availability is configured per space. The interface needs days, time slots, unavailable blocks, and existing confirmed reservations.

A user-facing calendar must distinguish free and occupied slots clearly.

### Reservation

A reservation needs enough frontend state to show:

- unique reservation identifier or code
- QR code after confirmation
- owner
- space
- date and start time
- duration
- price calculation
- payment state
- reservation state

The requirements refer to reservations as pending, confirmed, cancelled, finalized, and fulfilled/cumplida. The UI can normalize labels only if the underlying state mapping remains clear.

### Reservation ticket

A cancellation can create a ticket for a future reservation. The frontend needs:

- ticket identifier
- owner
- originating space, where restrictions apply
- value/coverage
- validity/expiration
- used/unused state

### Access record

The access-control UI must be able to show:

- person or reservation
- validation method
- selected service/space
- authorized or rejected result
- reason
- staff member responsible
- timestamp
- manual override reason when applicable

### Survey

A survey is associated with a used service or completed reservation. It contains:

- general questions
- space-specific questions
- rating/answers
- optional comment where configured
- reservation and space association
- completed state

### Report

Reports can cover:

- members
- reservations
- payments
- use of services

Generated reports can be exported or shown as a printable view. The document also specifies recent reports stored in CSV form and loadable later.

## 7. Application shell and navigation

The interface comps in the source document use a left sidebar on desktop. Keep this information architecture unless the existing codebase already has an equivalent shell.

The reference sidebar contains these sections:

- Inicio
- Usuarios
- Socios y Membresías
- Pagos
- Reservas
- Espacios y Servicios
- Accesos
- Encuestas
- Informes
- Configuración

The exact menu must depend on the current user's permissions. A normal registered user should not see admin-only tools merely disabled. Hide sections that the actor cannot use.

The desktop shell shown in the source also includes:

- SERA branding
- current date near the top area
- notification affordance
- profile/avatar menu
- contextual page title

For mobile, convert the persistent sidebar into a compact navigation pattern such as a drawer or bottom navigation plus overflow. Preserve the same page hierarchy. Do not build a separate mobile product.

## 8. Authentication flows

### CU-01. Register user

Actor: visitor.

Precondition: no account exists with the same DNI or email.

Required frontend flow:

1. Visitor opens registration.
2. Show an account creation form.
3. Collect personal data, contact data, and credentials.
4. Validate required fields.
5. User confirms registration.
6. Create the account through the application API or mock action.
7. Show success and route to the next valid state, normally sign-in or an authenticated onboarding state if the backend signs the user in.

Required fields from the requirements/interface section:

- first name
- surname
- DNI
- email
- phone
- relationship with UNSE
- password
- password confirmation
- acceptance of terms, conditions, and privacy policy

Optional/extended fields described in the requirements:

- profile photo
- address
- student or staff/teacher ID
- digital-card reading to prefill data, if implemented

Validation states:

- missing required field
- malformed field
- password requirements not met
- password confirmation mismatch
- duplicate DNI
- duplicate email

After registration, the user's UNSE relationship and personal information can still require administrative verification.

### Login screen

The explicit interface section specifies:

- email or username
- password
- remember session
- forgot password
- create account
- required-field indicators
- invalid credential feedback

After authentication, route the user to the functions allowed by the user's role/capabilities.

### CU-02. Administrator creates user

Actor: administrator.

Precondition: authenticated and authorized for user management.

Flow:

1. Admin selects "Crear usuario".
2. Show creation form, preferably a side panel/drawer as in the reference comp.
3. Admin enters personal data, contact data, and UNSE relationship.
4. Validate required fields.
5. Admin assigns initial role.
6. Admin confirms.
7. Create account and show success.

Alternate state: duplicate DNI or email. Keep the form open and identify the conflict.

### CU-03. Modify user data

Actor: registered user or administrator.

Flow:

1. Open the user profile.
2. Load current data.
3. Edit only fields allowed for the current actor.
4. Confirm changes.
5. Save and show updated data.

If a user tries to edit restricted fields, block the operation. Administrative changes must be represented as auditable actions in the UI when the backend exposes that information.

### CU-04. Search user

Actor: administrator.

Search inputs may include:

- DNI
- surname
- email
- user code

Results should lead to a detail view that can show general data, status, role, membership, and associated reservations. If the admin has limited permissions, do not render restricted data.

Required states:

- initial search state
- loading
- matches
- no results
- limited-data result when permissions restrict detail

## 9. Membership flows

### CU-05. Browse membership levels

Actor: registered user.

Flow:

1. Open memberships.
2. Show active membership levels available to the user.
3. User opens a level.
4. Show price, benefits, included services, and conditions.

If the user has a UNSE relationship, show the plan or price applicable to that condition.

If there are no available levels, render an empty state rather than an empty grid.

### CU-06. Contract membership

Actor: registered user.

Precondition: no incompatible active membership of the same type.

Flow:

1. Select membership level.
2. Confirm intent to contract it.
3. Create a membership request pending payment.
4. Continue to payment.
5. Show external/simulated payment step.
6. Receive a payment result.
7. Store/show payment status.
8. On approval, show membership as active and user as active member.

Alternate states:

- active incompatible membership already exists
- user cancels payment, request remains inactive/pending
- payment rejected, membership remains pending or rejected

### CU-07. Cancel monthly membership

Actor: member, possibly administrator when handled in person.

Precondition: active membership.

Flow:

1. User selects membership cancellation.
2. Show confirmation dialog that explains loss of membership benefits.
3. User confirms.
4. Cancel membership or record the cancellation request.
5. Update UI to non-member state and stop showing future monthly payment expectations once cancellation is effective.

Alternate states:

- membership already expired
- recurring payment cancellation fails, keep a recorded cancellation request and show the problem

Do not make this a one-click destructive action without confirmation.

### CU-08. Check member status

Actor: member or administrator.

Show:

- membership state
- level
- expiration or due information
- payment state/debt

If the user is not a member, show that no active membership exists and offer the appropriate next action, such as viewing membership levels.

### CU-09. Modify member data

Actor: administrator.

Editable administrative fields include:

- membership level
- membership state
- relationship with UNSE

Flow:

1. Find member.
2. Open member detail.
3. Edit allowed values.
4. Validate changes.
5. Confirm.
6. Save.
7. Show audit context when available, including responsible user, date, and reason.

If the change affects price, show the new applicable amount before confirmation.

Reject nonexistent membership levels.

## 10. Membership payment flows

### CU-10. Pay monthly membership

Actor: member.

Precondition: a charge is pending or near its due date.

Flow:

1. Open payments.
2. Show pending and upcoming membership charges.
3. Select a charge.
4. Show concept and amount.
5. Confirm payment.
6. Create payment order.
7. Continue through Mercado Pago or the prototype payment simulator.
8. Receive payment result.
9. Update payment and membership state.

On success, make "al día" status visible.

Alternate states:

- user abandons payment
- payment rejected

### Membership due-state behavior

The requirements say the system generates a membership invoice/charge with an automatic due date, checks unpaid charges daily, and suspends membership benefits when overdue.

The frontend must therefore have states for:

- due soon
- unpaid
- overdue
- membership benefits suspended due to debt

When a user signs in with an overdue charge, show a visible notice and a direct route to payment.

### CU-11. Configure recurring payment

Actor: member.

Precondition: active membership or membership being contracted.

Flow:

1. Select recurring payment.
2. Show monthly billing conditions.
3. User accepts authorization.
4. Persist/submit acceptance.
5. User confirms payment method.
6. Send configuration to Mercado Pago or simulator.
7. Receive result.
8. Associate recurring payment with membership.

Alternate states:

- user cancels and remains on manual monthly payment
- provider rejects recurring setup

### CU-13. Register payment result

Actor: payment provider or the prototype payment simulator. This is primarily a system-to-system flow, but the frontend must react to its states.

Precondition: a payment order exists.

Expected behavior:

1. The provider/simulator reports a result.
2. The application resolves the corresponding payment order.
3. It records the payment state.
4. It updates the associated membership or reservation when appropriate.
5. The frontend refreshes or polls the affected view and shows the resulting state rather than assuming success immediately after redirect.

Important alternate states:

- payment order cannot be associated
- duplicate result must not produce duplicate payment UI or double confirmation
- rejected payment leaves the membership charge or reservation unconfirmed

For mocks, expose these states through the same frontend contract used for real payment results.

### CU-14. Consult payment history

Actor: registered user or administrator with permission.

Flow:

1. Open payments/history.
2. Show payments visible to the current actor.
3. Apply filters.
4. Refresh results.
5. Select a payment.
6. Show payment detail and receipt/comprobante when available.

Required states:

- list with records
- filtered results
- no movements
- payment detail
- receipt unavailable

A normal user sees only their own allowed payment records. An administrator may see broader results according to permissions.

## 11. Reservation flows

Reservation creation is a guided flow. The interface comp on printed page 61 of the document, PDF page 62, shows a four-step progress header: `Espacio`, `Fecha y horario`, `Confirmación`, `Pago`.

Use a step-based layout on desktop and a compact equivalent on mobile.

### CU-15. Check availability

Actor: registered user.

Precondition: signed in.

Flow:

1. Open reservation creation.
2. Show reservable spaces.
3. Select a space.
4. Show a weekly/date calendar with slots marked free or occupied.

The reference interface also shows:

- space cards with category and hourly price
- a date calendar
- individual hourly slots with `Libre` and `Ocupado` states
- duration selection
- a live reservation summary
- member discount/pricing information
- total amount

The exact numbers in the comp are examples, not hard-coded product data.

### CU-16. Create reservation

Actor: registered user.

Flow:

1. Complete availability selection from CU-15.
2. Choose an available time slot.
3. Show calculated price according to user type/member status, selected space, and duration.
4. Continue to reservation payment.
5. When payment succeeds, confirm the reservation.
6. Generate and show the unique reservation code and QR.
7. Show a clear success state and route to reservation detail or "Mis Reservas".

Do not mark a reservation as confirmed before successful full payment.

### Reservation price calculation

The source requirements say the amount depends on:

- user/member type
- space
- number of reserved hours

The frontend should display the calculation as line items when enough data is available. It must not hide a membership discount or category-specific tariff inside an unexplained total.

### CU-12. Pay reservation

Actor: registered user.

Preconditions:

- reservation exists in pending-payment state
- selected space/time remains available

Flow:

1. User confirms the pending reservation.
2. Show total amount.
3. User chooses payment.
4. Create payment order.
5. User completes payment.
6. Receive payment result.
7. If approved, confirm reservation and generate QR.

Alternate states:

- rejected payment leaves reservation unconfirmed
- expired payment releases held time
- availability conflict discovered before final confirmation cancels the attempt and explains the conflict

The UI must handle the conflict state cleanly and return the user to availability selection rather than pretending the reservation succeeded.

### CU-19. View my reservations

Actor: registered user.

Show reservations grouped or filterable by state. The requirements explicitly mention:

- pending
- confirmed
- cancelled
- finalized

Reservation detail needs:

- reservation code
- QR
- space
- date/time
- duration
- payment
- status
- available actions

If a reservation is pending payment, offer "continue payment" and cancellation/abandon options.

If there are no reservations, show an empty state with a route to make a reservation.

The reference comp on printed page 62 of the document, PDF page 63, uses a table/list of reservations with a side detail panel containing the QR and reservation detail.

### CU-17. Cancel reservation

Actor: registered user.

Precondition: user owns an existing reservation and cancellation is allowed by policy.

Flow:

1. Open "Mis Reservas".
2. Select reservation.
3. Show reservation detail.
4. Choose cancel.
5. Show confirmation.
6. On confirmation, cancel reservation and release the time slot.
7. Generate reservation ticket when rules require it.
8. Show cancellation success and new ticket information.

If the user does not confirm, make no state change.

The document does not define an automatic refund flow. Do not add one.

### CU-18. Apply reservation ticket

Actor: registered user.

Precondition: user owns a valid, unused reservation ticket.

Flow:

1. User starts a new reservation.
2. Show available tickets at the appropriate pricing/confirmation step.
3. User selects a ticket.
4. Validate ownership and validity.
5. User confirms applying it.
6. Apply its value/coverage to the reservation.
7. If fully consumed, mark it used after successful reservation processing.

Alternate states:

- expired ticket
- ticket owned by another user
- ticket covers only part of total, show remaining amount to pay

Do not silently apply a ticket. The user must see which ticket is being used and its monetary or reservation coverage.

## 12. Spaces and services flows

### CU-20. Register space

Actor: administrator.

Flow:

1. Admin selects "Registrar nuevo espacio".
2. Show creation form.
3. Collect at least name, description, sample images, and reservation prices by user type.
4. Save.
5. Show success and the new space in management views.

### Space management detail

The requirements say each space stores or exposes:

- name
- current state
- last maintenance date
- usage count
- registration date

Admin UI should make the current operational state easy to scan.

### CU-21. Configure space availability

Actor: administrator.

Precondition: space exists.

Flow:

1. Select space.
2. Show availability calendar.
3. Define enabled days and times.
4. Validate overlap/conflicts.
5. Add unavailable date blocks if needed.
6. Save blocks/configuration.
7. Confirm changes.
8. Refresh calendar.

Alternate states:

- existing confirmed reservations prevent normal blocking of that time without a special administrative action
- overlapping configuration must be corrected

The normal frontend flow should not let an admin accidentally invalidate an existing confirmed reservation.

### CU-22. Configure space tariff

Actor: administrator.

Flow:

1. Select space.
2. Show current space and tariff information.
3. Enter/edit reservation prices.
4. Show confirmation before applying price changes.
5. Save and refresh displayed pricing.

Pricing should support differences by user/member category where provided by backend fixtures.

### CU-23. Browse available services

Actor: registered user.

Flow:

1. Open services/spaces.
2. Show available services and spaces.
3. Select one.
4. Show description, conditions, hours, and whether membership is required.
5. User asks/checks whether they have access.
6. Evaluate account/member status and show authorization status.

Alternate states:

- service requires active membership and user does not have it
- service requires reservation, offer a direct reservation action

## 13. Access-control flows

The access area is an operational tool. Optimize it for quick decisions, strong status feedback, and minimal navigation.

The reference interface on printed page 63 of the document, PDF page 64, has an access-control page with:

- QR scan tab/action
- code entry option
- scanner panel
- large authorized-result panel
- recent access records
- explicit "Registrar ingreso" action

### CU-24. Validate user by personal QR

Actor: staff.

Preconditions:

- staff has validation permission
- user presents personal QR

Flow:

1. Scan personal QR.
2. Resolve associated user.
3. Check account state, membership state, and membership level.
4. Staff selects the service or benefit being accessed.
5. Show authorization or rejection.
6. Staff confirms result when required.
7. Register access or access attempt.

Alternate states:

- unknown/invalid QR
- expired membership blocks member-only service
- membership level does not include selected service

Authorization result should be visually unambiguous and include the reason. Never rely on color alone.

### CU-25. Validate reservation by QR

Actor: staff.

Flow:

1. Scan reservation QR.
2. Resolve reservation.
3. Verify reservation state.
4. Show reserved space and mark reservation fulfilled/cumplida when access is confirmed.
5. Permit access for the user and companions according to operational rules.

Alternate states:

- invalid QR
- reservation already cancelled

### CU-26. Validate reservation by code

Actor: staff.

Same business validation as QR reservation validation, but the staff enters the unique reservation code manually.

Required UI states:

- code input
- loading/checking
- valid reservation
- invalid code
- cancelled reservation
- confirmed access

### CU-27. Register manual access

Actor: staff.

Preconditions:

- staff has permission
- a reason exists for manual registration

Flow:

1. Search user using CU-04 behavior.
2. Show services available for the user.
3. Select service.
4. Enter reason and confirm manual access.
5. Register the access with staff member, timestamp, and reason.

Alternate states:

- unknown user blocks normal manual access registration
- when system rules reject access, staff may need an administrative reason to force authorization if permissions allow it

A forced authorization must look different from a normal authorization in history/audit UI.

## 14. Digital membership card

RF-12 requires a digital card with basic user data and QR code.

The member-facing profile or membership area needs a dedicated digital-card view that can be opened quickly on mobile. It should show:

- SERA/UNSE context
- member name
- enough identity information for staff to confirm the person
- membership state/level where appropriate
- personal QR code

Do not expose unnecessary personal information on the card.

The QR identifies the person in the system. Staff still validates the user's eligibility for the selected service after scanning it.

## 15. Reports and feedback

### Reports landing

Admin reports should expose report types for:

- members
- reservations
- payments
- service usage
- recent generated reports

The reference report UI on printed page 64 of the document, PDF page 65, uses KPI cards, a data table, filters, and a chart. Use that composition where appropriate, but keep filters and tabular results usable on smaller screens.

### CU-28. Member report

Actor: administrator.

Filters required by the use case:

- member state
- membership level
- relationship with UNSE

Show results and support export or printable view. The use case says generated reports are stored as recent reports in CSV form.

### CU-29. Reservation report

Filters:

- reservation state
- reservation date
- reserved space

Show results and allow export/print.

### CU-30. Payment report

Filters:

- payment date
- payment type
- user who made the payment

Show results and allow export/print.

### CU-31. Service-usage report

The use case explicitly mentions filtering by state and date. RF/RNF requirements also expect useful report filtering by date, space, member type, and payment state where applicable. Keep report filter components reusable so the actual fields can be enabled by report type.

### CU-33. Load recent report

Actor: administrator.

Flow:

1. Open recent reports.
2. Show stored reports with creation date/time.
3. Select one.
4. Load report data and render the appropriate view.

Alternate states:

- no recent reports
- export/print current loaded report

### CU-32. Answer service survey

Actor: registered user.

Precondition: user used a service or completed a reservation.

Flow:

1. Open available survey.
2. Show evaluation questions.
3. Complete rating/answers and comment if present.
4. Validate required answers.
5. Submit.
6. Show completion state.

A survey must not be submitted twice for the same eligible event.

Questions include a general section plus questions specific to the reserved/used space.

## 16. Admin dashboard

The explicit administrator dashboard comp in the source document shows a summary of system activity. The requirements say the dashboard should surface at least:

- active members
- overdue membership charges
- today's reservations
- pending payments
- registered accesses
- spaces in use
- monthly income

The reference comp also includes:

- charts
- latest movements/activity
- quick actions to common admin operations

The dashboard is an operational overview, not a marketing page. Avoid oversized decorative sections that push actual status information below the fold.

Every dashboard metric should link to the corresponding filtered management view when practical.

## 17. Admin user-management screen

The source includes a concrete comp for user administration. Match its information density and interaction model.

Required list columns or equivalents:

- name
- DNI
- email
- role
- status
- actions

Required controls:

- search
- role filter
- status filter
- create-user action

The reference uses a right-side creation panel. A drawer is appropriate on desktop. On mobile, make creation a full-screen sheet/page rather than compressing the table and form side by side.

## 18. Profile and member-facing home

The PDF gives detailed use cases but does not provide a full member dashboard comp. Build member-facing pages around the defined tasks instead of copying the admin dashboard.

A useful authenticated member home should prioritize information that already exists in the requirements:

- membership state
- next or overdue payment
- upcoming reservation
- available reservation tickets
- personal/digital card
- quick action to reserve a space

Do not add social feeds, training plans, loyalty points, teams, chat, or other sports-club features that are absent from the requirements.

## 19. Suggested logical routes

These route names are an implementation suggestion, not an academic requirement. Adapt them to the repository's router conventions.

Public:

- `/login`
- `/register`
- `/forgot-password`

Authenticated user:

- `/app`
- `/app/profile`
- `/app/card`
- `/app/memberships`
- `/app/memberships/status`
- `/app/payments`
- `/app/reservations`
- `/app/reservations/new`
- `/app/reservations/:id`
- `/app/services`
- `/app/surveys`

Staff/admin:

- `/admin`
- `/admin/users`
- `/admin/members`
- `/admin/payments`
- `/admin/reservations`
- `/admin/spaces`
- `/admin/spaces/:id`
- `/admin/access`
- `/admin/reports`
- `/admin/reports/recent`
- `/admin/settings`

If the codebase uses one shared authenticated route tree, preserve that and gate navigation/items by permission rather than creating a second application.

## 20. Screen inventory for frontend implementation

The coding agent should plan for at least these screen families.

### Public/auth

- login
- registration
- forgot-password state
- invalid-credentials state
- successful registration state

### User/member

- authenticated home
- profile view/edit
- digital membership card
- membership levels
- membership detail/contract flow
- membership status
- membership cancellation confirmation/result
- monthly charges and payment history
- recurring-payment setup
- services/spaces browser
- service/space detail
- new reservation wizard
- reservation payment/result
- reservation success with QR/code
- my reservations list
- reservation detail
- reservation cancellation flow
- ticket selection/apply state
- survey list/available survey
- survey form and completion

### Staff

- access-control home
- QR scan state
- user QR result
- reservation QR result
- manual reservation-code validation
- manual access form
- authorization result
- rejection result
- recent access history

### Admin

- admin dashboard
- users list/search/filter
- create user
- user detail/edit
- members list/search
- member detail/edit
- payments management/history
- reservation management
- spaces/services list
- create space
- space detail
- space availability editor
- space tariff editor
- reports landing
- member report
- reservation report
- payment report
- service-usage report
- recent report viewer
- settings/roles entry point

## 21. Responsive behavior

RNF-02 requires browser use on computers and phones. Every page above must have a mobile state.

General rules:

- do not shrink desktop tables until they become unreadable
- convert dense tables to cards, stacked rows, or horizontally scrollable data grids according to the task
- use full-screen mobile sheets/pages for long admin forms
- keep primary actions reachable without precision clicking
- QR codes and the digital card must be easy to present from a phone
- reservation date and slot selection must remain understandable on narrow screens
- access-validation result screens need large status text and action buttons
- charts are secondary to the actual values and data tables on small screens
- keep touch targets large enough for operational use

The source does not define a separate mobile visual language. Keep the same brand, hierarchy, terminology, and states.

## 22. Visual direction from the source comps

The document includes reference UI on pages 57 through 64. Use them as product direction rather than pixel-perfect mandates.

Common characteristics:

- dark navy SERA sidebar/header
- white/light neutral working canvas
- blue as the main interaction color
- compact data cards
- bordered white panels
- restrained corner radii
- clear status badges
- icon-supported navigation
- tables for admin data
- charts only when they help summarize report/dashboard data

The login/register pages use a split layout with SERA context/illustration on the left and form card on the right. On mobile, collapse to a single-column form-first layout.

The reservation comp is especially important because it expresses the intended product flow. Preserve its progressive selection model, visible availability states, live summary, and explicit payment continuation.

## 23. State and feedback requirements

Every data-driven screen should define these UI states where they apply:

- loading
- success
- empty
- validation error
- server/action error
- permission denied
- stale/conflict state when availability changed

For destructive or financially meaningful actions, use confirmation and result states. This includes:

- cancelling membership
- cancelling reservation
- changing tariff
- applying a reservation ticket
- forcing manual access
- final payment confirmation

Status messages must say what happened and what the user can do next.

## 24. Permissions and privacy

RNF-03 through RNF-05 require authenticated administrative actions, role-based restrictions, and protection of member personal data.

Frontend expectations:

- route guards are not enough by themselves, but still implement them
- hide unauthorized navigation and actions
- render a proper unauthorized state if a protected URL is entered directly
- never assume a hidden button equals backend authorization
- avoid showing full personal records in global search suggestions or unrelated screens
- admin detail views should only request/render data the current capability allows

## 25. Audit-sensitive actions

The source specifically requires date and responsible user for sensitive operations such as payments, reservations, cancellations, and administrative profile/member changes.

Where the API exposes audit metadata, show it in detail/history views. Useful examples:

- `Registrado por`
- `Fecha y hora`
- `Última modificación`
- `Motivo`
- `Autorización forzada por`

Do not fabricate audit events in production code. Mock them only inside fixture data for frontend development.

## 26. Data fixtures the frontend should include during development

To avoid building only the happy path, prepare mock data for these cases.

Users:

- visitor
- registered non-member
- active member
- suspended member with overdue payment
- member with upcoming due charge
- staff
- administrator

Reservations:

- pending payment
- confirmed with QR
- cancelled with generated ticket
- finalized
- fulfilled/cumplida
- payment conflict because slot became unavailable

Tickets:

- valid unused ticket
- expired ticket
- partially covering ticket
- used ticket

Spaces:

- enabled with free slots
- enabled and fully booked on a date
- under maintenance
- unusable
- in use

Access:

- authorized member QR
- rejected expired member QR
- rejected level/service mismatch
- authorized reservation QR
- cancelled reservation QR
- invalid code
- manual forced access with reason

Reports:

- report with data
- no-result filters
- recent report list
- no recent reports

## 27. Backend and integration assumptions for frontend work

The frontend agent may work before the real backend is complete. Keep API dependencies behind typed adapters/services/hooks according to the repository conventions.

Do not couple page components directly to hard-coded mock arrays when the same screen will later call an API. Use mock implementations of the same interface where possible.

Payment UI must support a simulated provider result. The source document explicitly allows a simulated or basic Mercado Pago integration for the prototype.

SIU Guaraní validation is manual in the prototype. Do not create a fake automatic SIU verification flow.

## 28. Out-of-scope behavior that must not appear accidentally

Do not add these unless a later requirement changes the product:

- reservation deposits or partial payment as the normal booking path
- automatic monetary refunds after cancellation
- automatic SIU Guaraní verification
- native mobile-only interactions
- advanced survey analytics suite
- WhatsApp automation
- loyalty points
- social/community features
- tournament management
- coaching/training management
- team rosters
- e-commerce
- generic "sports SaaS" modules unrelated to the UNSE sports-complex workflow

## 29. Implementation priorities

When work must be staged, preserve complete vertical flows rather than producing many disconnected screens.

A practical frontend order is:

1. app shell, authentication, permission model, shared states
2. user/profile and membership status
3. spaces, availability, reservation wizard, payment simulation, reservation detail/QR
4. membership levels and monthly payment flows
5. staff access control
6. admin users/members
7. admin spaces/tariffs/availability
8. reports and surveys
9. responsive polish and full state coverage

This order is a frontend implementation suggestion. It does not change the use-case priorities in the academic document.

## 30. Definition of done for the frontend agent

A screen is not done because a static mock exists. For each implemented flow, verify that:

- navigation matches the current actor's permissions
- the main happy path works with mock or real data
- required validations are visible
- alternate states from the corresponding use case are represented
- loading and empty states exist
- success and failure feedback is clear
- destructive actions require confirmation
- monetary totals and payment state are visible where required
- reservation status stays consistent across list, detail, access, and reports
- membership/payment state stays consistent across home, membership, payments, and access
- QR and reservation-code views are usable on mobile
- the page works at desktop and phone widths
- the UI uses Spanish product copy
- no unsupported product feature was added

## 31. Use-case coverage checklist

The frontend must account for every use case in the source requirements document.

- [ ] CU-01 Registrar usuario
- [ ] CU-02 Crear cuenta de usuario
- [ ] CU-03 Modificar datos de usuario
- [ ] CU-04 Buscar usuario
- [ ] CU-05 Consultar niveles de membresía
- [ ] CU-06 Contratar membresía
- [ ] CU-07 Dar de baja membresía mensual
- [ ] CU-08 Consultar estado de socio
- [ ] CU-09 Modificar datos de socio
- [ ] CU-10 Pagar membresía mensual
- [ ] CU-11 Configurar pago recurrente
- [ ] CU-12 Pagar reserva
- [ ] CU-13 Registrar resultado de pago
- [ ] CU-14 Consultar historial de pagos
- [ ] CU-15 Consultar disponibilidad
- [ ] CU-16 Crear reserva
- [ ] CU-17 Cancelar reserva
- [ ] CU-18 Utilizar ticket de reserva
- [ ] CU-19 Consultar mis reservas
- [ ] CU-20 Registrar espacio
- [ ] CU-21 Configurar disponibilidad de espacio
- [ ] CU-22 Configurar tarifa de espacio
- [ ] CU-23 Consultar servicios disponibles
- [ ] CU-24 Validar usuario por QR
- [ ] CU-25 Validar reserva por QR
- [ ] CU-26 Validar reserva por código
- [ ] CU-27 Registrar acceso manual
- [ ] CU-28 Generar reporte de socios
- [ ] CU-29 Generar reporte de reservas
- [ ] CU-30 Generar reporte de pagos
- [ ] CU-31 Generar reporte de uso de servicios
- [ ] CU-32 Responder encuesta de servicio
- [ ] CU-33 Cargar reporte reciente

## 32. Source-page map

Use these PDF areas when more detail is needed:

- pages 3-6: product scope, functions, actors, restrictions, assumptions, deferred requirements
- pages 7-9: functional and system requirements
- pages 10-28: complete use-case flows CU-01 through CU-33
- pages 29-39: conceptual models per use case
- pages 40-56: sequence models per use case
- page 57: entity-relationship overview
- pages 58-65: interface and report comps
- page 66: non-functional requirements

When this spec and a later explicit product decision conflict, follow the later product decision. When implementation details are missing, do not invent business rules. Keep the UI flexible and request a contract or add a clearly marked mock assumption.
