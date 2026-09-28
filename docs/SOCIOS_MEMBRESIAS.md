# Socios y membresías

## Modelo definitivo

Cada usuario tiene un socio, creado en la misma transacción que su cuenta. La relación con la UNSE
queda `PENDIENTE` hasta su verificación manual por un administrador. Las cuentas anteriores reciben
su socio mediante `V12`, con relación `EXTERNO` pendiente de confirmar.

```text
Usuario 1 --- 1 Socio 1 --- 0..1 Membresia * --- 1 NivelMembresia
```

Hay una sola membresía por socio. No se modelan múltiples membresías ni rangos `vigenciaDesde` y
`vigenciaHasta`. La contratación crea `PENDIENTE_PAGO`; una nueva contratación tras la cancelación
reutiliza la misma fila. La restricción única sobre `socio_id` refuerza esta regla.

`fechaAlta` y `fechaBaja` registran la solicitud y su cancelación. `proximoVencimiento` es el
límite de vigencia de los beneficios y puede ser nulo mientras el pago está pendiente. Los
beneficios requieren que esa fecha sea posterior al día actual de Buenos Aires.

Al aprobar el primer pago, el próximo vencimiento se fija un mes después de la fecha de
aprobación. Cada pago posterior aprobado extiende un mes desde el vencimiento actual cuando aún
está en el futuro; si ya venció, el nuevo plazo se cuenta desde la fecha de aprobación.

La solicitud de un nivel crea en una sola transacción la membresía `PENDIENTE_PAGO` y un pago
`PENDIENTE` con el precio de la relación UNSE verificada, o `EXTERNO` mientras no esté verificada.
El alta del usuario y el alta del socio no crean ninguno de esos registros. El resultado del pago
se procesa en backend: una aprobación activa la membresía. Para `MERCADO_PAGO`, SERA crea una
suscripción mensual sin plan asociado y comprueba cada factura y cobro notificado por webhook antes
de aprobar la cuota. La interfaz no aprueba pagos. Ver [MERCADO_PAGO.md](MERCADO_PAGO.md).

## Estados y auditoría

Un proceso diario a las 00:05 de Buenos Aires sincroniza el estado persistido con los
vencimientos. El día de `proximoVencimiento` ya no hay beneficios: las consultas y las reglas
de acceso calculan el estado efectivo aunque el proceso no haya corrido. La falta de renovación
deja la membresía `VENCIDA`, sin deuda acumulada ni suspensión automática. `SUSPENDIDA` es una
decisión administrativa que un nuevo cobro no levanta. Un pago aprobado de una membresía vencida
la reactiva y fija un nuevo vencimiento según la regla anterior.

Un proceso horario cancela los pagos que siguen `PENDIENTE` después de una hora desde su creación.
También cancela las solicitudes `PENDIENTE_PAGO` de más de una hora que ya no tienen un pago
pendiente. Una nueva tentativa de pago aún pendiente mantiene viva la solicitud hasta que esa
tentativa cumpla su propia hora. El proceso horario usa UTC para comparar instantes. Un pago inicial
vinculado a una suscripción de Mercado Pago no vence mientras esa suscripción siga vigente.

- Una solicitud pendiente puede cancelarse inmediatamente.
- Una membresía activa puede pasar a vencida, suspendida o cancelada.
- Una vencida puede suspenderse, cancelarse o reactivarse con un pago aprobado; una suspendida
  solo puede cancelarse. Un cobro recibido mientras esté suspendida queda para revisión.
- Un pago aprobado mantiene activa la membresía activa y renueva su próximo vencimiento.
- Una membresía cancelada no admite pagos; se debe crear una nueva solicitud para volver a
  contratar.
- Una cancelada puede volver a solicitarse, con estado pendiente de pago.
- La activación o reactivación requiere un pago aprobado; una modificación administrativa no puede
  activarla directamente.
- Las transiciones no permitidas devuelven 409.

Las altas, cambios administrativos, cancelaciones y nuevas contrataciones guardan motivo,
responsable, fecha y detalle en `cambios_socios`. Los cambios de socio y membresía son
transaccionales. Las versiones de JPA detectan ediciones concurrentes.

## Niveles y precios

Cada nivel contiene nombre, descripción, beneficios, disponibilidad y precios por relación UNSE.
Los importes deben ser positivos, con hasta 8 enteros y 2 decimales. Una relación sin precio no
puede contratar ese nivel. Deshabilitar un nivel conserva las membresías existentes.

La tarifa de espacios se calcula en backend según la relación UNSE verificada. Para una relación
pendiente o rechazada se usa `EXTERNO`; si no existe tarifa específica, se usa la tarifa general.
El módulo de pagos aplica la misma verificación al iniciar cada cobro y congela el importe.

## API

| Método | Ruta | Acceso |
| --- | --- | --- |
| GET | `/api/niveles-membresia?soloDisponibles=true` | Autenticado |
| GET | `/api/niveles-membresia/{id}` | Autenticado |
| POST, PUT, DELETE | `/api/niveles-membresia` y `/{id}` | Administrador |
| GET | `/api/socios/me` | Socio de la sesión, 204 si no existe |
| GET | `/api/socios?buscar=&estadoMembresia=&relacionUnse=` | Administrador |
| GET, PUT | `/api/socios/{id}` | Administrador |
| GET | `/api/socios/{id}/historial` | Administrador |
| POST | `/api/membresias` | Titular del socio o administrador |
| GET | `/api/membresias/{id}` | Titular o administrador |
| POST | `/api/membresias/{id}/cancelacion` | Titular o administrador |

`POST /api/socios` se conserva para compatibilidad, pero normalmente no es necesario: el alta del
usuario ya crea el socio. Rechaza un segundo socio del mismo usuario y no admite un nivel de
membresía. `POST /api/membresias` recibe `socioId`, `nivelMembresiaId` y `medioPago`.

## Consultas

`SocioRepository.buscar` aplica búsqueda y filtros en SQL. Trae usuario, membresía opcional y nivel
con `JOIN FETCH` y `LEFT JOIN FETCH`. Las consultas por identificador usan `EntityGraph` para el
mismo recorrido. Esto evita cargar las relaciones una por una para construir el listado.

No se agregó una referencia inversa `Usuario.socio`: la sesión consulta usuarios en cada request y
no necesita cargar el socio. `mappedBy` define el propietario de la relación, pero no resuelve por
sí solo el problema de consultas N+1. El DTO de socios ya reúne los datos que necesita la pantalla.
