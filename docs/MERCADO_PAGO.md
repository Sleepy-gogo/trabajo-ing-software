# Suscripciones de Mercado Pago

SERA usa la API de Suscripciones sin plan asociado. El backend crea una suscripción mensual en estado `pending` por cada contratación con `MERCADO_PAGO`. El navegador abre el `init_point` que devuelve Mercado Pago. La primera cuota y las siguientes solo se aprueban en SERA al verificar un cobro con la API de Mercado Pago.

## Configuración

Definir estas variables en el proceso de la API, sin guardarlas en el repositorio:

| Variable | Valor |
| --- | --- |
| `MP_ACCESS_TOKEN` | Access Token de la aplicación de Mercado Pago, de prueba o producción según el entorno |
| `MP_WEBHOOK_SECRET` | Clave secreta de Webhooks de la misma aplicación y entorno |
| `MP_BACK_URL` | URL HTTPS pública del frontend que muestra Mi membresía, por ejemplo `https://sera.example.org/app/memberships/status` |

La API arranca sin estas variables para permitir el desarrollo de otras funciones. Crear una suscripción requiere `MP_ACCESS_TOKEN` y `MP_BACK_URL`; aceptar un webhook requiere `MP_WEBHOOK_SECRET`. Nunca usar el token de acceso en Vite ni en el navegador.

En el panel de Mercado Pago, registrar para prueba y producción la URL:

```text
https://<dominio-publico-de-la-api>/api/webhooks/mercadopago
```

Activar `subscription_preapproval`, `subscription_authorized_payment` y `payment`. SERA procesa los dos primeros; reconoce `payment` sin cambiar el estado local porque la factura autorizada aporta el vínculo con la suscripción. Con ngrok sobre el puerto 4500, usar `https://<subdominio-ngrok>/api/webhooks/mercadopago`. El panel debe tener la clave secreta correspondiente al entorno de las credenciales.

El webhook es un `POST` público sin sesión ni CSRF. Valida `x-signature` con el SDK oficial de Java, `x-request-id` y el parámetro `data.id`. Rechaza firmas inválidas con 401. Consulta la suscripción o factura a Mercado Pago, comprueba referencia, moneda, monto, identificador del cobrador y estado del pago antes de registrar una aprobación. Guarda el identificador del cobro con una restricción única para tolerar reintentos. Un retorno del navegador no confirma pagos.

## Rutas de SERA

| Método | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/pagos/membresias/{id}/suscripcion` | Titular inicia o recupera el enlace de autorización |
| `GET` | `/api/pagos/membresias/{id}/suscripcion` | Titular consulta la suscripción local |
| `POST` | `/api/webhooks/mercadopago` | Mercado Pago envía notificaciones firmadas |

La cancelación de una membresía cancela también su suscripción remota antes de cerrar la membresía local. Un fallo de Mercado Pago impide completar la cancelación para evitar que continúen los débitos mientras SERA muestra la membresía como cancelada. El importe se fija al crear la suscripción. Para cambiar de nivel, primero hay que cancelar la suscripción y contratar de nuevo.

## Prueba manual con credenciales

1. Iniciar PostgreSQL y la API con las tres variables configuradas; abrir el frontend en una URL HTTPS pública.
2. Crear una membresía con Mercado Pago y seguir el enlace de autorización.
3. Usar usuarios y medios de pago de prueba de Mercado Pago.
4. Confirmar en el panel que llegan eventos `subscription_preapproval` y `subscription_authorized_payment` con respuesta 200.
5. Verificar que un pago aprobado activa la membresía y que repetir una notificación no crea otra cuota.
6. Cancelar la membresía y comprobar en Mercado Pago que la suscripción quedó `canceled`.

Sin credenciales reales no se puede ejecutar este recorrido contra Mercado Pago. Los tests del repositorio cubren validación, correlación e idempotencia con dobles de la API.

Referencias: [crear suscripción](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/create-preapproval/post), [webhooks](https://www.mercadopago.com.ar/developers/es/docs/links-and-debts/additional-content/your-integrations/notifications/webhooks), [factura autorizada](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/get-authorized-payment/get).
