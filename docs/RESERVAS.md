# Incremento 5: reservas y tickets

Alcance acordado para `TRA-10` y `TRA-40` a `TRA-45`. Se conserva BCE y se reutilizan
usuarios, espacios, calendario, tarifas y pagos. Mercado Pago sigue usando
Suscripciones para membresías; las reservas usan Checkout Pro para un cobro único.

## Plan previo a la implementación

1. Agregar `Reserva`, su repository, un servicio de casos de uso y un controller.
2. Descontar reservas pendientes y confirmadas del calendario existente.
3. Calcular el precio en el backend y crear el pago por el importe restante.
4. Confirmar con efectivo registrado por administración o con un pago verificado
   en Mercado Pago. El retorno del navegador nunca confirma por sí solo.
5. Conectar creación, listado, detalle, cancelación y tickets en la UI.
6. Probar conflictos concurrentes, permisos, pagos y conservación del saldo.

## Modelo reducido y decisiones de la demo

- Una reserva pertenece a un usuario y un espacio. Fecha, inicio, fin, cantidad de
  personas, tarifa aplicada, total, estado, vencimiento y código son atributos.
  No se crean clases Horario, Precio, Confirmación ni CódigoQR.
- El ticket es el saldo disponible de una reserva cancelada. La nueva reserva
  guarda el origen y el importe aplicado. No hay billetera ni tabla Ticket.
  Se permite un ticket por reserva; el sobrante se conserva. Si una reserva
  pendiente se cancela o vence, se devuelve el saldo al ticket de origen.
- Cancelar antes del inicio devuelve el total abonado como ticket; después del
  inicio no se permite cancelar. No hay devolución de dinero automática.
  Los tickets no vencen en esta demo: la consigna no fija un plazo.
- La reserva pendiente retiene el horario hasta una hora, o hasta su inicio si
  ocurre antes. Un proceso periódico libera los vencimientos.
- Se aceptan horas completas de duración, dentro de una franja disponible y sin
  cruzar medianoche. Todos los horarios corresponden a Argentina.
- Puede reservar cualquier usuario activo, como indica CU-16. No se obliga a
  contratar membresía: no hay servicios restringidos por nivel en el modelo
  actual. Se reutiliza la tarifa por relación UNSE verificada, sin inventar
  descuentos adicionales por membresía. Esto precisa el texto genérico de TRA-41.
- Los estados visibles Próxima (`CONFIRMADA`), En curso ahora (`EN_CURSO`) y
  Finalizada sin ingreso (`FINALIZADA`) se calculan por fecha y hora.
- Consumida (`CONSUMIDA`) indica que el personal confirmó el ingreso. Se guardan
  `consumida_en` y `consumida_por`; no se confunde el fin del horario con asistencia.
  La reserva persiste como confirmada para conservar la exclusión de horarios.
- El código se genera al confirmar y el frontend representa ese mismo código en
  un QR. El personal puede consultar su vigencia desde Validar ingreso.
- Se mantienen las transacciones y el control de superposición. Simplificar el
  modelo no permite confirmar dos reservas del mismo espacio y horario.

## Integración con Mercado Pago

Se reutilizan el SDK, token y validador de firmas. Checkout Pro crea una
preferencia cuyo `external_reference` identifica el pago local. El backend
consulta el pago remoto y verifica referencia, moneda e importe antes de confirmar.
Un cobro aprobado después de cancelar o vencer queda para revisión administrativa;
no recupera el horario ni genera un ticket automáticamente.

Referencia: [crear una preferencia de Checkout Pro](https://www.mercadopago.com.ar/developers/es/reference/online-payments/checkout-pro-preferences/create-preference/post).

Se usan las mismas variables `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET` y `MP_BACK_URL`
que las membresías. El retorno de la reserva se construye sobre el origen de
`MP_BACK_URL`, con la ruta `/app/reservations/{id}`. Activar el tópico `payment`
en el webhook existente del panel de Mercado Pago. No hace falta otra aplicación
ni una suscripción para reservar.

Al volver del checkout con `payment_id`, el detalle solicita una verificación al
backend una vez por retorno. Si no llega el webhook o la consulta falla, el titular
o administración pueden verificar el número del pago desde el detalle. SERA consulta
Mercado Pago; nunca confía en el estado enviado por el navegador. Si la sesión venció,
el login conserva la ruta de la reserva y el número de pago. Los rechazos permiten reintentar dentro del plazo.
No se implementan reembolsos automáticos ni conciliación masiva de reservas.

## Recorrido para la presentación

1. Ingresar como usuario y abrir Espacios → Reservar este espacio.
2. Elegir fecha, horario y personas. Revisar tarifa, ticket y saldo a pagar.
3. Crear la reserva. El horario desaparece de la disponibilidad de otros usuarios.
4. Pagar con Mercado Pago, o elegir efectivo y confirmarlo desde Administración
   → Pagos. El detalle muestra el código y QR al confirmar.
5. Cancelar antes del inicio. El horario queda libre y aparece un ticket con saldo.
6. Crear otra reserva usando ese ticket. Si falta dinero, se cobra la diferencia;
   si alcanza, se confirma directamente. El sobrante se conserva.

La administración puede consultar y cancelar desde Reservas. Para evitar cambios
que invaliden reservas existentes, no se permite modificar el espacio ni reducir
sus horarios mientras tenga reservas pendientes o confirmadas desde hoy. Las
tarifas nuevas no alteran el precio guardado de reservas anteriores.

## Validación local

Desde `apps/api`: `./mvnw spotless:apply verify` (en Windows, `mvnw.cmd`).
Desde `apps/web`: `pnpm typecheck`, `pnpm lint` y `pnpm build` (incluye Vitest).

La prueba `python scripts/smoke_reservas.py` usa HTTP real y PostgreSQL. Preparar
una base vacía `sera_reservas_test` en el Docker local e iniciar la API con:

```powershell
$env:SPRING_DOCKER_COMPOSE_ENABLED='false'
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/sera_reservas_test'
.\mvnw.cmd spring-boot:run
```

El soporte automático de Compose debe estar desactivado en esta prueba porque
sus datos de conexión reemplazan la URL de la base aislada. El script comprueba
el destino antes de promover su cuenta ficticia a administrador. No borra datos.

Las pruebas del proveedor con respuestas controladas cubren preferencia, firma,
importe incorrecto, cobro repetido y cobro tardío.

El 28/09/2026 se completó un Checkout Pro con las cuentas de prueba del equipo y
una tarjeta de prueba de Mercado Pago. El pago fue aprobado y la verificación
contra la API del proveedor confirmó la reserva y generó su QR. No se observó
confirmación automática por webhook durante ese recorrido; revisar el tópico
`payment`, la URL pública y la clave de firma en el panel antes de la presentación.
Un segundo pago de prueba comprobó el retorno automático: SERA consultó al
backend sin intervención del usuario y mostró la reserva confirmada con su QR.
El botón manual queda disponible si esa consulta falla.

El mismo recorrido detectó que el perfil local no cargaba `apps/api/.env`. Ahora
el perfil `dev` lo importa opcionalmente al iniciar desde `apps/api`.
El frontend genera `claveSolicitud` con `crypto.getRandomValues`, disponible en
HTTP, y conserva la clave en los reintentos. Es una clave de idempotencia, no el ID
de la entidad: JPA sigue generando los IDs de las reservas.


## Lector QR y validación de acceso

`/admin/access` está conectado a `POST /api/accesos/validacion`, disponible solo
para ADMIN y STAFF, con sesión y CSRF. Recibe `{ "codigo": "SERA-…" }` y devuelve
un DTO con `autorizado`, `motivo` y los datos del titular y horario cuando corresponde.
No devuelve entidades ni interpreta URLs externas.

El frontend usa [qr-scanner](https://github.com/nimiq/qr-scanner) para leer cámara
tras una acción del operador o una imagen seleccionada. La cámara requiere HTTPS
(o localhost), se detiene después de una lectura y se libera al cerrar o salir.
La entrada manual funciona también con lectores USB que escriben como un teclado.
Una lectura nunca autoriza por sí sola: se consulta el estado actual en la API.

Reglas implementadas:

- Reserva: cuenta activa, reserva confirmada, espacio habilitado y hora actual
  dentro de `[desde, hasta)` en Argentina. No hay tolerancia de ingreso anticipado.
- Carnet personal: cuenta activa y membresía vigente según `estaVigente`.
  Habilita acceso general; no acredita una reserva de espacio.
- Códigos desconocidos, cancelaciones y vencimientos se rechazan.

Escanear sigue siendo una consulta. Para una reserva habilitada, el operador pulsa
**Confirmar ingreso y consumir reserva**: `POST /api/accesos/ingresos` recibe el
código, vuelve a validar con la hora del servidor y registra el primer ingreso junto
al operador autenticado. Un bloqueo de fila serializa confirmaciones concurrentes;
reintentar devuelve el registro original. Una reserva consumida no habilita otro
acceso y deja de mostrar un QR utilizable. Los carnets personales no se consumen.
La migración V19 agrega ambos atributos sin modificar migraciones anteriores.
Los listados y detalles de usuario y administración muestran estados con texto,
iconos y colores distintos; se actualizan mediante las consultas periódicas existentes.


Para probar ingreso simultáneo contra PostgreSQL, iniciar el JAR desde `apps/api`
con `--server.port=4501 --spring.docker.compose.enabled=false` y
`--spring.datasource.url=jdbc:postgresql://localhost:5432/sera_reservas_test`.
Desde la raíz, ejecutar `python scripts/smoke_accesos.py` antes de las 23:00.
El script comprueba la base aislada, crea sus propios datos, coloca su reserva
ficticia en el horario actual y confirma dos ingresos concurrentes. Ambos deben
conservar el mismo instante y operador; una validación posterior rechaza el QR.
