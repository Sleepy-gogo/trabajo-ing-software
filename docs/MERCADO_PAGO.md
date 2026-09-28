# Suscripciones de Mercado Pago

SERA usa la API de Suscripciones sin plan asociado. El backend crea una suscripci√≥n mensual en estado `pending` por cada contrataci√≥n con `MERCADO_PAGO`. El navegador abre el `init_point` que devuelve Mercado Pago. La primera cuota y las siguientes solo se aprueban en SERA al verificar un cobro con la API de Mercado Pago.

## Configuraci√≥n

Definir estas variables en el proceso de la API, sin guardarlas en el repositorio:

En desarrollo tambi√©n se pueden guardar en `apps/api/.env`, copiando `.env.example`.
El perfil `dev` lo carga al iniciar desde `apps/api`; usar valores sin comillas y
reiniciar la API despu√©s de cambiarlos. Las variables del proceso tienen prioridad.

| Variable | Valor |
| --- | --- |
| `MP_ACCESS_TOKEN` | Access Token de la aplicaci√≥n de Mercado Pago, de prueba o producci√≥n seg√∫n el entorno |
| `MP_WEBHOOK_SECRET` | Clave secreta de Webhooks de la misma aplicaci√≥n y entorno |
| `MP_TEST_PAYER_EMAIL` | Opcional, solo perfil `dev`: email del comprador de prueba de Mercado Pago. Permite usar otro email para iniciar sesi√≥n en SERA. El backend verifica que el vendedor sea de prueba. |
| `MP_BACK_URL` | URL HTTPS p√∫blica del frontend que muestra Mi membres√≠a, por ejemplo `https://sera.example.org/app/memberships/status` |

La API arranca sin estas variables para permitir el desarrollo de otras funciones. Crear una suscripci√≥n requiere `MP_ACCESS_TOKEN` y `MP_BACK_URL`; aceptar un webhook requiere `MP_WEBHOOK_SECRET`. Nunca usar el token de acceso en Vite ni en el navegador.

En el panel de Mercado Pago, registrar para prueba y producci√≥n la URL:

```text
https://<dominio-publico-de-la-api>/api/webhooks/mercadopago
```

Activar `subscription_preapproval`, `subscription_authorized_payment` y `payment`. Los dos primeros procesan membres√≠as. `payment` verifica los cobros √∫nicos de reservas; los pagos de suscripciones siguen confirm√°ndose por su factura autorizada. Con ngrok sobre el puerto 4500, usar `https://<subdominio-ngrok>/api/webhooks/mercadopago`. El panel debe tener la clave secreta correspondiente al entorno de las credenciales.

El webhook es un `POST` p√∫blico sin sesi√≥n ni CSRF. Valida `x-signature` con el SDK oficial de Java, `x-request-id` y el par√°metro `data.id`. Rechaza firmas inv√°lidas con 401. Consulta la suscripci√≥n o factura a Mercado Pago, comprueba referencia, moneda, monto, identificador del cobrador y estado del pago antes de registrar una aprobaci√≥n. Guarda el identificador del cobro con una restricci√≥n √∫nica para tolerar reintentos. Un retorno del navegador no confirma pagos.

## Rutas de SERA

| M√©todo | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/pagos/membresias/{id}/suscripcion` | Titular inicia o recupera el enlace de autorizaci√≥n |
| `POST` | `/api/pagos/membresias/{id}/verificacion` | Titular verifica cobros de su propia membresÌa |
| `GET` | `/api/pagos/membresias/{id}/suscripcion` | Titular consulta la suscripci√≥n local |
| `POST` | `/api/webhooks/mercadopago` | Mercado Pago env√≠a notificaciones firmadas |
| `POST` | `/api/pagos/membresias/{id}/conciliacion` | Administrador consulta facturas de la suscripci√≥n si falt√≥ un webhook |

La conciliaci√≥n recorre las facturas de la suscripci√≥n mediante
`GET /authorized_payments/search?preapproval_id=...` y procesa cada una por la misma ruta
verificada del webhook. Puede repetirse: el identificador √∫nico del cobro evita aplicar dos
veces una renovaci√≥n. La pantalla administrativa ofrece ¬´Conciliar cobros¬ª para los pagos de
Mercado Pago. La consulta puede tardar si la suscripci√≥n tiene muchas facturas.

Al iniciar una suscripci√≥n, SERA confirma primero una referencia local y despu√©s llama a
Mercado Pago fuera de esa transacci√≥n. Un rechazo definitivo del alta (HTTP 400, 401, 403 o 422) se guarda como `rejected` y permite corregir la configuraci√≥n y reintentar la misma referencia local. No crea otro pago ni otra membres√≠a. Los errores del proveedor se devuelven como JSON con un mensaje visible en la interfaz. Si se pierde la respuesta remota, la solicitud queda
marcada como incierta y un reintento del navegador no crea otra suscripci√≥n. Un webhook de
`subscription_preapproval` puede vincular la suscripci√≥n usando `external_reference`. Si ese
webhook tampoco llega, administraci√≥n debe contrastar la referencia en Mercado Pago antes de
resolver el caso; no se debe repetir el alta a ciegas.

La cancelaci√≥n de una membres√≠a cancela tambi√©n su suscripci√≥n remota antes de cerrar la membres√≠a local. Un fallo de Mercado Pago impide completar la cancelaci√≥n para evitar que contin√∫en los d√©bitos mientras SERA muestra la membres√≠a como cancelada. El importe se fija al crear la suscripci√≥n. Para cambiar de nivel, primero hay que cancelar la suscripci√≥n y contratar de nuevo.

## Prueba manual con credenciales

1. Crear cuentas de prueba de vendedor y comprador en Mercado Pago. Usar el Access Token de prueba del vendedor en `MP_ACCESS_TOKEN`, y el email de la cuenta compradora para el usuario de SERA, o configurar `MP_TEST_PAYER_EMAIL` en desarrollo. No usar la misma cuenta para ambos lados.
2. Iniciar PostgreSQL y la API con las tres variables configuradas; abrir el frontend en una URL HTTPS p√∫blica. Usar la clave de Webhooks de prueba de esa aplicaci√≥n en `MP_WEBHOOK_SECRET`.
3. Crear una membres√≠a con Mercado Pago y seguir el enlace de autorizaci√≥n. Usar un medio de pago de prueba.
4. Confirmar en el panel que llegan eventos `subscription_preapproval` y `subscription_authorized_payment` con respuesta 200.
5. Verificar que un pago aprobado activa la membres√≠a y que repetir una notificaci√≥n no crea otra renovaci√≥n.
6. Ejecutar ¬´Conciliar cobros¬ª y comprobar que el vencimiento no cambia si la factura ya se proces√≥.
7. Cancelar la membres√≠a y comprobar en Mercado Pago que la suscripci√≥n qued√≥ `canceled`.

Sin credenciales reales no se puede ejecutar este recorrido contra Mercado Pago. Los tests del repositorio cubren validaci√≥n, correlaci√≥n e idempotencia con dobles de la API.

Referencias: [cuentas de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/test/accounts), [aprobar un pago de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-test/payment-approval), [crear suscripci√≥n](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/create-preapproval/post), [webhooks](https://www.mercadopago.com.ar/developers/es/docs/links-and-debts/additional-content/your-integrations/notifications/webhooks), [factura autorizada](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/get-authorized-payment/get), [buscar facturas](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/authorized-payment-search/get).


El 28/09/2026 se reprodujo el rechazo `Both payer and collector must be real or test users`
al enviar un email de SERA normal con el vendedor de prueba. La misma solicitud
con el email del comprador de prueba devolvi√≥ una suscripci√≥n `pending` y un
`init_point` v√°lido. La correcci√≥n no cambia el email del usuario ni aprueba pagos.
El modo de prueba requiere el email `@testuser.com` real del comprador y un vendedor
con la etiqueta `test_user`; no sustituye compradores de una cuenta vendedora real.
Reiniciar la API cuando sea conveniente para cargar cambios de configuraci√≥n.

Al abrir ´Mi membresÌaª con un pago pendiente de Mercado Pago se consultan las facturas una vez.
´Ya paguÈ ∑ Verificar pagoª permite reintentar. Solo el titular puede verificar su membresÌa;
la API consulta y valida el cobro remoto, sin confiar en par·metros de retorno. La b˙squeda
usa el tamaÒo de p·gina predeterminado del proveedor y avanza por `offset`: durante la prueba
el valor explÌcito `limit=20` devolviÛ HTTP 400, mientras omitirlo devolviÛ la factura aprobada.
La URL `back_url` permite volver a SERA; si el checkout no vuelve autom·ticamente, el usuario
puede regresar a ´Mi membresÌaª. Esta recuperaciÛn no reemplaza la configuraciÛn de webhooks.

Prueba del 28/09/2026: una factura `processed` con cobro `approved/accredited` de
ARS 18.000 se recuperÛ mediante la verificaciÛn del titular. SERA dejÛ el pago aprobado
y la membresÌa activa hasta el 28/10/2026. Repetir la verificaciÛn conservÛ el mismo
vencimiento y comprobante; otro usuario recibiÛ 403. No se verificÛ la entrega autom·tica
del webhook. La URL de retorno estaba configurada, pero el checkout no redirigiÛ al usuario.

En esa prueba la suscripciÛn remota ya estaba `cancelled`, mientras SERA conservaba
`pending`. La cancelaciÛn acepta `cancelled` y `canceled` al consultar al proveedor,
normaliza a `canceled` en la entidad local y no repite la baja remota si ya terminÛ.
El gateway envÌa `cancelled` y exige que la respuesta confirme el identificador y estado.
Se recuperÛ el caso confirmando primero la baja remota, sincronizando ˙nicamente el estado
de esa suscripciÛn y ejecutando la cancelaciÛn normal del titular. La membresÌa quedÛ
`CANCELADA`; el pago aprobado se conserva en el historial.
