/**
 * Tipos de dominio usados por el prototipo visual.
 *
 * Son DTOs del frontend, no representan necesariamente tablas del backend.
 * Las fechas se mantienen como ISO strings para que puedan viajar por JSON sin
 * conversiones implícitas cuando los mocks sean reemplazados por la API.
 */

export type IsoDate = string

export type Role =
  | "visitante"
  | "usuario"
  | "personal_acceso"
  | "responsable"
  | "administrador"
  | "administrador_sistema"

export interface RoleOption {
  value: Role
  label: string
  descripcion: string
}

export type Capability =
  | "consultar_servicios"
  | "reservar_espacio"
  | "gestionar_perfil"
  | "gestionar_socios"
  | "gestionar_espacios"
  | "gestionar_pagos"
  | "validar_accesos"
  | "gestionar_reportes"
  | "gestionar_usuarios"
  | "gestionar_configuracion"

export type AccountStatus =
  "activo" | "pendiente_verificacion" | "suspendido" | "bloqueado" | "inactivo"

export type VerificationStatus = "verificado" | "pendiente" | "rechazado"

export type UnseRelationship =
  "estudiante" | "docente" | "nodocente" | "graduado" | "externo" | "visitante"

export type MembershipState = "activa" | "vencida" | "suspendida" | "cancelada"

export type MembershipChargeStatus =
  "al_dia" | "proxima_a_vencer" | "vencida" | "sin_cargo"

export type RecurringPaymentStatus = "activo" | "inactivo" | "pendiente"

export type PaymentStatus =
  "pendiente" | "aprobado" | "rechazado" | "cancelado" | "vencido"

export type PaymentMethod =
  "mercado_pago_simulado" | "transferencia_bancaria" | "efectivo" | "tarjeta"

export type PaymentConcept = "cuota_mensual" | "reserva" | "diferencia_ticket"

export type SpaceState =
  "habilitado" | "en_mantenimiento" | "inutilizable" | "en_uso"

export type SpaceCategory = "deporte" | "recreacion" | "servicio"

export type AvailabilitySlotStatus =
  "disponible" | "ocupado" | "bloqueado" | "mantenimiento"

export type ReservationState =
  | "pendiente_pago"
  | "confirmada"
  | "cancelada"
  | "finalizada"
  | "cumplida"
  | "conflicto_disponibilidad"

export type TicketState = "valido" | "vencido" | "usado"

export type AccessValidationMethod =
  "qr_personal" | "qr_reserva" | "codigo_reserva" | "manual"

export type AccessResult = "autorizado" | "rechazado"

export type ReportType = "socios" | "reservas" | "pagos" | "uso_servicios"

export type ReportState = "con_datos" | "sin_resultados" | "cargando" | "error"

export type SurveyState =
  "disponible" | "incompleta" | "completada" | "no_elegible"

export type AsyncState =
  "cargando" | "exito" | "vacio" | "error" | "sin_permiso"

export interface User {
  id: string
  nombre: string
  apellido: string
  nombreCompleto: string
  dni: string
  email: string
  telefono?: string
  direccion?: string
  avatarUrl?: string
  relacionUnse: UnseRelationship
  relacionUnseLabel: string
  identificadorUnse?: string
  estado: AccountStatus
  estadoLabel: string
  rol: Role
  rolLabel: string
  capacidades: Capability[]
  verificacion: VerificationStatus
  verificacionLabel: string
  membershipId?: string
}

export interface MembershipPlan {
  id: string
  nombre: string
  descripcion: string
  moneda: "ARS"
  beneficios: string[]
  serviciosIncluidos: string[]
  condiciones: string[]
  disponibleParaContratar: boolean
  preciosPorRelacion: Partial<Record<UnseRelationship, number>>
}

export interface Membership {
  id: string
  userId: string
  planId: string
  planNombre: string
  estado: MembershipState
  estadoLabel: string
  fechaAlta: IsoDate
  fechaBaja?: IsoDate
  proximoVencimiento: IsoDate
  estadoCargo: MembershipChargeStatus
  estadoCargoLabel: string
  pagoRecurrente: RecurringPaymentStatus
  pagoRecurrenteLabel: string
  serviciosDisponibles: string[]
}

export interface Payment {
  id: string
  userId: string
  concepto: PaymentConcept
  conceptoLabel: string
  importe: number
  moneda: "ARS"
  fecha: IsoDate
  estado: PaymentStatus
  estadoLabel: string
  medio: PaymentMethod
  medioLabel: string
  membershipId?: string
  reservationId?: string
  comprobante?: string
  registradoPor?: string
  motivo?: string
}

export interface SpaceImage {
  id: string
  alt: string
  /** Placeholder until final photography is generated. */
  placeholder: string
}

export interface SpacePrice {
  relacion: UnseRelationship | "socio_activo" | "no_socio"
  relacionLabel: string
  importe: number
}

export interface Space {
  id: string
  nombre: string
  tipo: SpaceCategory
  tipoLabel: string
  descripcion: string
  capacidad: number
  estado: SpaceState
  estadoLabel: string
  fechaRegistro: IsoDate
  ultimaMantenimiento: IsoDate
  cantidadUsos: number
  imagenes: SpaceImage[]
  precios: SpacePrice[]
  requiereMembresia: boolean
  requiereReserva: boolean
  servicios: string[]
}

export interface AvailabilitySlot {
  id: string
  spaceId: string
  fecha: IsoDate
  inicio: string
  fin: string
  estado: AvailabilitySlotStatus
  estadoLabel: string
  reservationId?: string
  motivo?: string
}

export interface AvailabilityDay {
  fecha: IsoDate
  etiqueta: string
  slots: AvailabilitySlot[]
}

export interface ReservationPriceBreakdown {
  tarifaBase: number
  descuentoMembresia: number
  valorTicket: number
  diferenciaAPagar: number
  total: number
  moneda: "ARS"
}

export interface Reservation {
  id: string
  codigo: string
  userId: string
  ownerName: string
  spaceId: string
  spaceName: string
  fecha: IsoDate
  inicio: string
  fin: string
  duracionMinutos: number
  cantidadPersonas: number
  precio: ReservationPriceBreakdown
  pagoEstado: PaymentStatus
  pagoEstadoLabel: string
  estado: ReservationState
  estadoLabel: string
  qrPayload?: string
  creadaEn: IsoDate
  actualizadaEn: IsoDate
  registradoPor?: string
  ticketId?: string
  motivoCancelacion?: string
  motivoConflicto?: string
}

export interface ReservationTicket {
  id: string
  codigo: string
  userId: string
  ownerName: string
  espacioOrigenId?: string
  espacioOrigenNombre?: string
  valorTotal: number
  valorDisponible: number
  coberturaDescripcion: string
  estado: TicketState
  estadoLabel: string
  emitidoEn: IsoDate
  venceEn: IsoDate
  usadoEn?: IsoDate
  reservationIdOrigen?: string
}

export interface AccessRecord {
  id: string
  userId?: string
  reservationId?: string
  personName: string
  metodo: AccessValidationMethod
  metodoLabel: string
  spaceId?: string
  spaceName: string
  resultado: AccessResult
  resultadoLabel: string
  motivo: string
  personalResponsable: string
  fechaHora: IsoDate
  motivoAutorizacionForzada?: string
  esAutorizacionForzada: boolean
}

export type SurveyQuestionKind = "calificacion" | "opcion" | "texto"

export interface SurveyQuestion {
  id: string
  texto: string
  tipo: SurveyQuestionKind
  obligatoria: boolean
  opciones?: string[]
  respuesta?: string | number
}

export interface Survey {
  id: string
  userId: string
  reservationId: string
  spaceId: string
  spaceName: string
  titulo: string
  descripcion: string
  estado: SurveyState
  estadoLabel: string
  disponibleDesde: IsoDate
  fechaLimite: IsoDate
  preguntasGenerales: SurveyQuestion[]
  preguntasEspacio: SurveyQuestion[]
  comentario?: string
  completadaEn?: IsoDate
}

export interface ReportColumn {
  key: string
  label: string
}

export type ReportRow = Record<string, string | number | boolean | null>

export interface Report {
  id: string
  tipo: ReportType
  tipoLabel: string
  nombre: string
  descripcion: string
  estado: ReportState
  estadoLabel: string
  creadoEn: IsoDate
  creadoPor: string
  filtros: Record<string, string>
  columnas: ReportColumn[]
  filas: ReportRow[]
  formato: "csv" | "vista_imprimible"
  archivo?: string
}

export interface ReportSummaryMetric {
  id: string
  label: string
  value: string
  helper?: string
  trend?: string
}

export interface DashboardData {
  fecha: IsoDate
  saludo: string
  metricas: ReportSummaryMetric[]
  actividadReciente: string[]
}

export interface AuthScenario {
  id: string
  titulo: string
  descripcion: string
  estado:
    | "inicio"
    | "error_credenciales"
    | "registro_exitoso"
    | "recuperacion_enviada"
  estadoLabel: string
}

export interface PaymentScenario {
  id: string
  titulo: string
  descripcion: string
  estado: PaymentStatus
  estadoLabel: string
  siguienteAccion: string
}

export interface AccessScenario {
  id: string
  titulo: string
  descripcion: string
  estado: AccessResult | "qr_invalido" | "codigo_invalido" | "reserva_cancelada"
  estadoLabel: string
}
