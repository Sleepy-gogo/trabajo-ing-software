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

`fechaAlta` y `fechaBaja` registran la solicitud y su cancelación. `proximoVencimiento` pertenece al
seguimiento de la próxima cuota y puede ser nulo. No define un intervalo de vigencia.

La activación requiere la aprobación de un pago mediante el webhook del incremento 4. No existe
una ruta administrativa que permita saltarse ese requisito. Tampoco se simula un pago aprobado.

## Estados y auditoría

- Una solicitud pendiente puede cancelarse inmediatamente.
- Una membresía activa puede pasar a vencida, suspendida o cancelada.
- Una vencida puede suspenderse o cancelarse; una suspendida puede cancelarse.
- Una cancelada puede volver a solicitarse, con estado pendiente de pago.
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
El futuro módulo de pagos debe aplicar la misma verificación antes de emitir una cuota.

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
usuario ya crea el socio. Rechaza un segundo socio del mismo usuario.

## Consultas

`SocioRepository.buscar` aplica búsqueda y filtros en SQL. Trae usuario, membresía opcional y nivel
con `JOIN FETCH` y `LEFT JOIN FETCH`. Las consultas por identificador usan `EntityGraph` para el
mismo recorrido. Esto evita cargar las relaciones una por una para construir el listado.

No se agregó una referencia inversa `Usuario.socio`: la sesión consulta usuarios en cada request y
no necesita cargar el socio. `mappedBy` define el propietario de la relación, pero no resuelve por
sí solo el problema de consultas N+1. El DTO de socios ya reúne los datos que necesita la pantalla.
