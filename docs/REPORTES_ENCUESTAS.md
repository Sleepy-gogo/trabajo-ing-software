# Reportes y encuestas

Contratos, filtros y reglas de los módulos de reportes y encuestas.

## Reportes

Solo `ADMIN` puede generar, consultar o exportar informes. Las consultas se ejecutan en PostgreSQL mediante Spring Data JPA; las agregaciones de utilización se calculan en SQL. Control valida los filtros, construye los DTO y guarda una instantánea en `informes`. Volver a abrir o exportar un informe conserva el resultado original aunque después cambien los datos.

| Tipo | Período | Estado | Métricas |
| --- | --- | --- | --- |
| `socios` | Fecha de alta del socio | Situación actual de cuenta/membresía | Datos de socio, relación declarada y nivel actual |
| `reservas` | Fecha de utilización prevista | Estado visible actual, incluida `CONSUMIDA` | Titular, espacio, horario, personas e importe |
| `pagos` | Fecha de creación de la orden | Estado actual del pago | Importe aprobado, medio, concepto y revisión pendiente |
| `uso_servicios` | Fecha de la reserva | Solo reservas confirmadas | Cantidad de reservas, reservas terminadas, ingresos registrados, horas reservadas y personas declaradas en reservas utilizadas |
| `actividad` | Fecha de la acción | Tipo de acción (`INICIO_SESION`, `INICIO_SESION_FALLIDO`, `INICIO_SESION_BLOQUEADO`, `CIERRE_SESION`, `RESERVA_CREADA`, `RESERVA_CANCELADA`, `VERIFICACION_ACTUALIZADA`, `INFORME_GENERADO`) | Fecha y hora, acción, correo, nombre y detalle |

Las fechas se interpretan en `America/Argentina/Buenos_Aires`. Desde y hasta son inclusivos. Relación UNSE y espacio son filtros opcionales; socios no admite espacio, utilización no admite estado y actividad solo filtra por período y tipo de acción. El estado de membresía respeta el vencimiento efectivo aunque el proceso periódico todavía no haya actualizado la fila.

Las horas reservadas incluyen reservas confirmadas futuras y finalizadas. Un ingreso requiere `consumida_en`, por lo que el fin del horario no se cuenta como asistencia. El número de personas se toma de lo declarado en la reserva y no representa una medición individual. No se presenta un porcentaje de ocupación porque no hay una definición funcional de su denominador.

Se admiten períodos de hasta cinco años y hasta 5000 filas. Un resultado mayor se rechaza para que el usuario reduzca el período o agregue filtros; no se exportan filas truncadas silenciosamente. El historial muestra páginas de 20 informes.

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/api/reportes` | Generar y guardar un informe |
| GET | `/api/reportes?pagina=0` | Historial |
| GET | `/api/reportes/{id}` | Consultar instantánea |
| GET | `/api/reportes/{id}/csv` | Descargar CSV de la instantánea |

Ejemplo de request:

```json
{
  "tipo": "pagos",
  "desde": "2026-09-01",
  "hasta": "2026-09-30",
  "estado": "APROBADO",
  "relacion": "",
  "espacioId": null
}
```

Cada informe muestra arriba de la tabla un resumen y gráficos según su tipo: socios por estado y por relación; reservas por estado y por espacio, con el importe total sin canceladas ni vencidas; pagos con importe aprobado por concepto y por medio, y pagos que requieren revisión; uso de servicios con horas y asistencia (ingresos sobre reservas terminadas) por espacio; actividad por tipo de acción. Se calculan en la interfaz (`lib/report-summary.ts`) con las filas guardadas, así que un informe abierto más tarde muestra los mismos valores que su tabla.

El CSV usa UTF-8 con BOM, comas, comillas escapadas y líneas CRLF. Los importes tienen dos decimales. Los textos que podrían interpretarse como fórmulas se exportan como texto. La interfaz permite imprimir mediante el diálogo del navegador.

## Registro de actividad (SM-05)

El módulo `actividad` (BCE) guarda en `registros_actividad` quién hizo cada acción, cuál, cuándo y un detalle breve. Registran `UsuarioService` (inicio de sesión, intentos fallidos y bloqueados por la verificación de SM-02, cierre de sesión desde `AuthController.logout`), `ReservaService` (creación y cancelación, con espacio y horario), `SocioService` (cambio de verificación) y `ReporteService` (informe generado). El detalle nunca incluye contraseñas ni datos de pago; un intento fallido indica solo el motivo, como "Contraseña incorrecta".

`ActividadService.registrar` usa una transacción propia (`REQUIRES_NEW`), por lo que el intento bloqueado queda guardado aunque el login termine con una excepción. `usuario_id` no tiene clave foránea a propósito: un intento fallido puede usar un correo sin cuenta, el historial se conserva si la cuenta cambia, y `ReservaService` bloquea la fila del usuario durante la transacción, de modo que una clave foránea haría esperar a la transacción nueva. La tabla se crea en `V3__crear_registros_actividad.sql`, con un `CHECK` alineado con `TipoActividad`. La consulta es el informe `actividad`, que reutiliza historial, CSV e impresión.

## Panel de indicadores (SM-04)

`GET /api/reportes/indicadores?desde=&hasta=` (solo `ADMIN`, hasta dos años) devuelve el panel del inicio de administración. `IndicadoresService` compara el período con el anterior de igual duración y `ReporteRepository` agrega los datos con SQL nativo:

| Dato | Cálculo |
| --- | --- |
| Ingresos aprobados | Suma de pagos `APROBADO` por fecha de aprobación (`aprobado_en`, Argentina). |
| Reservas confirmadas y horas | Reservas `CONFIRMADA` por fecha de la reserva. |
| Tasa de cancelación | Canceladas sobre confirmadas más canceladas; su variación se informa en puntos. |
| Membresías activas | Membresías no dadas de baja con una cuota aprobada que cubre el último día del período (o hoy, si termina después). |
| Ingresos por mes | Cuotas (`CUOTA_MENSUAL`) y reservas (`RESERVA`, `DIFERENCIA_TICKET`); al menos los últimos seis meses. |
| Horas e ingresos por espacio | Horas de reservas confirmadas e ingresos de pagos de reservas aprobados. |
| Demanda | Día ISO y hora de cada hora ocupada: una reserva de 18 a 20 suma en 18 y en 19. |
| Asistencia | Reservas confirmadas terminadas con `consumida_en` sobre el total de terminadas. |
| Socios y membresías | Socios activos por relación (con pendientes de verificar) y membresías activas por nivel. |

Las conclusiones en texto se escriben con reglas simples y solo cuando hay datos: variación de ingresos y reservas (no se informa un porcentaje si el período anterior fue 0), espacio que concentra más horas, franja con más demanda, reservas terminadas sin ingreso y cancelaciones. El panel muestra también las últimas ocho acciones del registro de actividad. El porcentaje de ocupación sigue sin implementarse: su denominador es una decisión pendiente con la cátedra.

## Encuestas

Administración define título, descripción, espacio opcional, período de respuesta y entre 1 y 20 preguntas. Hay preguntas de calificación de 1 a 5, de opción única con 2 a 10 opciones distintas y de texto de hasta 2000 caracteres. Cada pregunta puede ser obligatoria. Una encuesta sin espacio específico corresponde a todos los espacios.

La publicación fija las preguntas para conservar el significado de respuestas ya recibidas. Se puede cerrar y habilitar la encuesta; modificar su contenido requiere crear otra definición.

Una respuesta pertenece a una encuesta y a una reserva del usuario autenticado, con ingreso registrado y estado persistido `CONFIRMADA`. Se admite una respuesta por combinación de encuesta y reserva. No se habilita feedback sobre reservas finalizadas sin ingreso. El período establece cuándo se puede responder; no filtra la fecha histórica de la reserva.

Las respuestas no son anónimas. El usuario puede consultar las propias y administración puede ver titular, espacio, reserva, respuestas, promedio de calificaciones y distribución de opciones. No se permiten preguntas ajenas, valores fuera de escala, opciones desconocidas, respuestas obligatorias vacías, encuestas cerradas ni respuestas de otra cuenta.

El envío bloquea la definición de encuesta durante la transacción. La restricción única de PostgreSQL y el bloqueo impiden guardar dos respuestas en envíos simultáneos. El cierre usa el mismo bloqueo. La UI muestra éxito únicamente después de recibir la confirmación del servidor.

| Método | Ruta | Rol/función |
| --- | --- | --- |
| POST / GET | `/api/encuestas` | ADMIN, publicar/listar definiciones |
| PUT | `/api/encuestas/{id}/estado` | ADMIN, `{"activa":false}` para cerrar |
| GET | `/api/encuestas/{id}/resultados` | ADMIN, resultados y estadísticas |
| GET | `/api/encuestas/me` | Sesión, encuestas asociadas a reservas utilizadas propias |
| GET | `/api/encuestas/{id}/reservas/{reservaId}` | Titular, definición y respuesta guardada |
| POST | `/api/encuestas/{id}/reservas/{reservaId}/respuestas` | Titular, `{"respuestas":{"uuid-pregunta":"4"}}` |

Las escrituras requieren CSRF. Las tablas forman parte de `V1__esquema_inicial.sql`.

## Verificación

Hay tests unitarios del panel de indicadores (comparaciones, meses, conclusiones), del informe de actividad y del registro, tests HTTP del panel (ADMIN sí, USUARIO y STAFF no), tests React de los gráficos calculados con las filas guardadas y tests unitarios de preguntas, permisos y validaciones de Control, filtros/CSV/instantáneas, tests HTTP de autorización y CSRF, y tests React de generación, errores, publicación y envío real. `scripts/smoke_cierre.py` verifica las consultas SQL contra PostgreSQL y el recorrido HTTP de pago efectivo, renovación única, permisos, cuatro informes, CSV, historial, cierre, respuestas concurrentes y estadísticas.
