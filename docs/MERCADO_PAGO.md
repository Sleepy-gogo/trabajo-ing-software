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
| `POST` | `/api/pagos/membresias/{id}/verificacion` | Titular verifica cobros de su propia membres�a |
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


El 28/09/2026 se reprodujo el rechazo `Both payer and collector must be real or test users`
al enviar un email de SERA normal con el vendedor de prueba. La misma solicitud
con el email del comprador de prueba devolvió una suscripción `pending` y un
`init_point` válido. La corrección no cambia el email del usuario ni aprueba pagos.
El modo de prueba requiere el email `@testuser.com` real del comprador y un vendedor
con la etiqueta `test_user`; no sustituye compradores de una cuenta vendedora real.
Reiniciar la API cuando sea conveniente para cargar cambios de configuración.

Al abrir �Mi membres�a� con un pago pendiente de Mercado Pago se consultan las facturas una vez.
�Ya pagu� � Verificar pago� permite reintentar. Solo el titular puede verificar su membres�a;
la API consulta y valida el cobro remoto, sin confiar en par�metros de retorno. La b�squeda
usa el tama�o de p�gina predeterminado del proveedor y avanza por `offset`: durante la prueba
el valor expl�cito `limit=20` devolvi� HTTP 400, mientras omitirlo devolvi� la factura aprobada.
La URL `back_url` permite volver a SERA; si el checkout no vuelve autom�ticamente, el usuario
puede regresar a �Mi membres�a�. Esta recuperaci�n no reemplaza la configuraci�n de webhooks.

Prueba del 28/09/2026: una factura `processed` con cobro `approved/accredited` de
ARS 18.000 se recuper� mediante la verificaci�n del titular. SERA dej� el pago aprobado
y la membres�a activa hasta el 28/10/2026. Repetir la verificaci�n conserv� el mismo
vencimiento y comprobante; otro usuario recibi� 403. No se verific� la entrega autom�tica
del webhook. La URL de retorno estaba configurada, pero el checkout no redirigi� al usuario.
