export interface PersonRecord {
  id: string
  name: string
  dni: string
  email: string
  phone: string
  relationship: string
  role: string
  status: string
  membership: string
  lastPayment: string
  verified: boolean
}
export const peopleFixtures: PersonRecord[] = [
  {
    id: "u-01",
    name: "Gonzalo Pérez",
    dni: "41.234.567",
    email: "gonzalo.perez@unse.edu.ar",
    phone: "385 123 4567",
    relationship: "Estudiante",
    role: "Solicitante",
    status: "Activo",
    membership: "Estudiante",
    lastPayment: "01/09/2026",
    verified: true,
  },
  {
    id: "u-02",
    name: "María González",
    dni: "38.765.432",
    email: "maria.gonzalez@unse.edu.ar",
    phone: "385 412 9083",
    relationship: "Docente",
    role: "Responsable",
    status: "Activo",
    membership: "Comunidad UNSE",
    lastPayment: "03/09/2026",
    verified: true,
  },
  {
    id: "u-03",
    name: "Juan Torres",
    dni: "42.789.123",
    email: "juan.torres@gmail.com",
    phone: "385 542 1180",
    relationship: "Externo",
    role: "Solicitante",
    status: "Suspendido",
    membership: "General",
    lastPayment: "05/08/2026",
    verified: false,
  },
  {
    id: "u-04",
    name: "Lucía Fernández",
    dni: "40.321.654",
    email: "lucia.fernandez@unse.edu.ar",
    phone: "385 601 2834",
    relationship: "No docente",
    role: "Personal de acceso",
    status: "Activo",
    membership: "Comunidad UNSE",
    lastPayment: "02/09/2026",
    verified: true,
  },
  {
    id: "u-05",
    name: "Diego López",
    dni: "37.654.987",
    email: "diego.lopez@unse.edu.ar",
    phone: "385 334 1290",
    relationship: "Estudiante",
    role: "Solicitante",
    status: "Pendiente",
    membership: "Estudiante",
    lastPayment: "Sin pagos",
    verified: false,
  },
  {
    id: "u-06",
    name: "Camila Díaz",
    dni: "39.456.221",
    email: "camila.diaz@unse.edu.ar",
    phone: "385 552 6310",
    relationship: "Docente",
    role: "Solicitante",
    status: "Activo",
    membership: "Comunidad UNSE",
    lastPayment: "05/09/2026",
    verified: true,
  },
  {
    id: "u-07",
    name: "Axel Castaño",
    dni: "42.876.120",
    email: "axel.castano@unse.edu.ar",
    phone: "385 482 7120",
    relationship: "Estudiante",
    role: "Administrador",
    status: "Activo",
    membership: "Estudiante",
    lastPayment: "01/09/2026",
    verified: true,
  },
]
export const money = (value: number) =>
  new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(value)
