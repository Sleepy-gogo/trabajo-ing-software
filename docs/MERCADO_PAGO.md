# Suscripciones de Mercado Pago

SERA usa la API de Suscripciones sin plan asociado. El backend crea una suscripción mensual en estado `pending` por cada contratación con `MERCADO_PAGO`. El navegador abre el `init_point` que devuelve Mercado Pago. La primera cuota y las siguientes solo se aprueban en SERA al verificar un cobro con la API de Mercado Pago.

## Configuración

Definir estas variables en el proceso de la API, sin guardarlas en el repositorio:

En desarrollo también se pueden guardar en `apps/api/.env`, copiando `.env.example`.
El perfil `dev` lo carga al iniciar desde `apps/api`; usar valores sin comillas y
reiniciar la API después de cambiarlos. Las variables del proceso tienen prioridad.

| Variable | Valor |
| --- | --- |
| `MP_ACCESS_TOKEN` | Access Token de la aplicación de Mercado Pago, de prueba o producción según el entorno |
| `MP_WEBHOOK_SECRET` | Clave secreta de Webhooks de la misma aplicación y entorno |
| `MP_TEST_PAYER_EMAIL` | Opcional, solo perfil `dev`: email del comprador de prueba de Mercado Pago. Permite usar otro email para iniciar sesión en SERA. El backend verifica que el vendedor sea de prueba. |
| `MP_BACK_URL` | URL HTTPS pública del frontend que muestra Mi membresía, por ejemplo `https://sera.example.org/app/memberships/status` |

La API arranca sin estas variables para permitir el desarrollo de otras funciones. Crear una suscripción requiere `MP_ACCESS_TOKEN` y `MP_BACK_URL`; aceptar un webhook requiere `MP_WEBHOOK_SECRET`. Nunca usar el token de acceso en Vite ni en el navegador.

En el panel de Mercado Pago, registrar para prueba y producción la URL:

```text
https://<dominio-publico-de-la-api>/api/webhooks/mercadopago
```

Activar `subscription_preapproval`, `subscription_authorized_payment` y `payment`. Los dos primeros procesan membresías. `payment` verifica los cobros únicos de reservas; los pagos de suscripciones siguen confirmándose por su factura autorizada. Con ngrok sobre el puerto 4500, usar `https://<subdominio-ngrok>/api/webhooks/mercadopago`. El panel debe tener la clave secreta correspondiente al entorno de las credenciales.

El webhook es un `POST` público sin sesión ni CSRF. Valida `x-signature` con el SDK oficial de Java, `x-request-id` y el parámetro `data.id`. Rechaza firmas inválidas con 401. Consulta la suscripción o factura a Mercado Pago, comprueba referencia, moneda, monto, identificador del cobrador y estado del pago antes de registrar una aprobación. Guarda el identificador del cobro con una restricción única para tolerar reintentos. Un retorno del navegador no confirma pagos.

## Rutas de SERA

| Método | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/pagos/membresias/{id}/suscripcion` | Titular inicia o recupera el enlace de autorización |
| `POST` | `/api/pagos/membresias/{id}/verificacion` | Titular verifica cobros de su propia membresía |
| `GET` | `/api/pagos/membresias/{id}/suscripcion` | Titular consulta la suscripción local |
| `POST` | `/api/webhooks/mercadopago` | Mercado Pago envía notificaciones firmadas |
| `POST` | `/api/pagos/membresias/{id}/conciliacion` | Administrador consulta facturas de la suscripción si faltó un webhook |

La conciliación recorre las facturas de la suscripción mediante
`GET /authorized_payments/search?preapproval_id=...` y procesa cada una por la misma ruta
verificada del webhook. Puede repetirse: el identificador único del cobro evita aplicar dos
veces una renovación. La pantalla administrativa ofrece «Conciliar cobros» para los pagos de
Mercado Pago. La consulta puede tardar si la suscripción tiene muchas facturas.

Al iniciar una suscripción, SERA confirma primero una referencia local y después llama a
Mercado Pago fuera de esa transacción. Un rechazo definitivo del alta (HTTP 400, 401, 403 o 422) se guarda como `rejected` y permite corregir la configuración y reintentar la misma referencia local. No crea otro pago ni otra membresía. Los errores del proveedor se devuelven como JSON con un mensaje visible en la interfaz. Si se pierde la respuesta remota, la solicitud queda
marcada como incierta y un reintento del navegador no crea otra suscripción. Un webhook de
`subscription_preapproval` puede vincular la suscripción usando `external_reference`. Si ese
webhook tampoco llega, administración debe contrastar la referencia en Mercado Pago antes de
resolver el caso; no se debe repetir el alta a ciegas.

La cancelación de una membresía cancela también su suscripción remota antes de cerrar la membresía local. Un fallo de Mercado Pago impide completar la cancelación para evitar que continúen los débitos mientras SERA muestra la membresía como cancelada. El importe se fija al crear la suscripción. Para cambiar de nivel, primero hay que cancelar la suscripción y contratar de nuevo.

## Prueba manual con credenciales

1. Crear cuentas de prueba de vendedor y comprador en Mercado Pago. Usar el Access Token de prueba del vendedor en `MP_ACCESS_TOKEN`, y el email de la cuenta compradora para el usuario de SERA, o configurar `MP_TEST_PAYER_EMAIL` en desarrollo. No usar la misma cuenta para ambos lados.
2. Iniciar PostgreSQL y la API con las tres variables configuradas; abrir el frontend en una URL HTTPS pública. Usar la clave de Webhooks de prueba de esa aplicación en `MP_WEBHOOK_SECRET`.
3. Crear una membresía con Mercado Pago y seguir el enlace de autorización. Usar un medio de pago de prueba.
4. Confirmar en el panel que llegan eventos `subscription_preapproval` y `subscription_authorized_payment` con respuesta 200.
5. Verificar que un pago aprobado activa la membresía y que repetir una notificación no crea otra renovación.
6. Ejecutar «Conciliar cobros» y comprobar que el vencimiento no cambia si la factura ya se procesó.
7. Cancelar la membresía y comprobar en Mercado Pago que la suscripción quedó `canceled`.

Sin credenciales reales no se puede ejecutar este recorrido contra Mercado Pago. Los tests del repositorio cubren validación, correlación e idempotencia con dobles de la API.

Referencias: [cuentas de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/test/accounts), [aprobar un pago de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-test/payment-approval), [crear suscripción](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/create-preapproval/post), [webhooks](https://www.mercadopago.com.ar/developers/es/docs/links-and-debts/additional-content/your-integrations/notifications/webhooks), [factura autorizada](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/get-authorized-payment/get), [buscar facturas](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/authorized-payment-search/get).


## Cuentas de prueba y recuperación

Mercado Pago exige que vendedor y comprador pertenezcan al mismo entorno.
Combinar un vendedor de prueba con un comprador normal provoca un rechazo del
proveedor. `MP_TEST_PAYER_EMAIL` usa el email de la cuenta compradora de prueba,
sin cambiar el email de SERA. El vendedor debe tener la etiqueta `test_user`.
La configuración requiere reiniciar la API.

Al abrir «Mi membresía» con un pago pendiente de Mercado Pago se consultan las facturas una vez.
«Ya pagué, verificar pago» permite reintentar. Solo el titular puede verificar su membresía;
la API consulta y valida el cobro remoto, sin confiar en parámetros de retorno. La búsqueda
usa el tamaño de página predeterminado del proveedor y avanza por `offset`.
La URL `back_url` permite volver a SERA; si el checkout no vuelve automáticamente, el usuario
puede regresar a «Mi membresía». Esta recuperación no reemplaza la configuración de webhooks.

Repetir la verificación de una factura aplicada conserva el vencimiento y el
comprobante. La operación del titular rechaza consultas de otra cuenta con 403.

La cancelación acepta `cancelled` y `canceled` del proveedor y guarda `canceled`
localmente. Si la suscripción remota ya está cancelada, no repite la baja. El
gateway envía `cancelled` y exige una respuesta con el identificador y estado
esperados. Los pagos aprobados se conservan en el historial.

## Protección de contrataciones anteriores

Las cuotas recurrentes conservan el identificador de contratación, nivel y
relación UNSE del pago original de la suscripción. Si llega una factura después
de cancelar y volver a contratar, queda para revisión y no activa la contratación
nueva. Un evento remoto antiguo tampoco puede reabrir una suscripción que SERA
ya registró como cancelada.

La consulta del cobro actual separa la suscripción y el pendiente del historial
paginado. «Ya pagué, verificar pago» también permite recuperar renovaciones de
membresías activas o vencidas. La cancelación fallida del proveedor conserva el
estado local y muestra el error para reintentar.
