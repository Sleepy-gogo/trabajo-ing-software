# Reportes y encuestas

Implementación del incremento 7, TRA-51 a TRA-54. La interfaz consume la API y no usa fixtures para estos módulos.

## Reportes

Solo `ADMIN` puede generar, consultar o exportar informes. Las consultas se ejecutan en PostgreSQL mediante Spring Data JPA; las agregaciones de utilización se calculan en SQL. Control valida los filtros, construye los DTO y guarda una instantánea en `informes`. Volver a abrir o exportar un informe conserva el resultado original aunque después cambien los datos.

| Tipo | Período | Estado | Métricas |
| --- | --- | --- | --- |
| `socios` | Fecha de alta del socio | Situación actual de cuenta/membresía | Datos de socio, relación declarada y nivel actual |
| `reservas` | Fecha de utilización prevista | Estado visible actual, incluida `CONSUMIDA` | Titular, espacio, horario, personas e importe |
| `pagos` | Fecha de creación de la orden | Estado actual del pago | Importe aprobado, medio, concepto y revisión pendiente |
| `uso_servicios` | Fecha de la reserva | Solo reservas confirmadas | Cantidad de reservas, ingresos registrados, horas reservadas y personas declaradas en reservas utilizadas |

Las fechas se interpretan en `America/Argentina/Buenos_Aires`. Desde y hasta son inclusivos. Relación UNSE y espacio son filtros opcionales; socios no admite espacio y utilización no admite estado. El estado de membresía respeta el vencimiento efectivo aunque el proceso periódico todavía no haya actualizado la fila.

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

El CSV usa UTF-8 con BOM, comas, comillas escapadas y líneas CRLF. Los importes tienen dos decimales. Los textos que podrían interpretarse como fórmulas se exportan como texto. La interfaz permite imprimir mediante el diálogo del navegador.

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

Las escrituras requieren CSRF como el resto de la aplicación. Las tablas nuevas se crean mediante `V20__reportes_y_encuestas.sql`, sin alterar migraciones anteriores.

## Verificación

Hay tests unitarios de preguntas, permisos y validaciones de Control, filtros/CSV/instantáneas, tests HTTP de autorización y CSRF, y tests React de generación, errores, publicación y envío real. `scripts/smoke_cierre.py` verifica las consultas SQL contra PostgreSQL y el recorrido HTTP de pago efectivo, renovación única, permisos, cuatro informes, CSV, historial, cierre, respuestas concurrentes y estadísticas. Su preparación está documentada en [ENTREGA.md](ENTREGA.md).
