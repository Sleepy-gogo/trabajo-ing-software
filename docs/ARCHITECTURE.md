# Arquitectura

SERA usa una API Spring Boot y una interfaz React. La comunicación usa REST y
JSON; PostgreSQL almacena los datos. Flyway crea el esquema y Hibernate lo valida
con `ddl-auto=validate`.

```text
Navegador → React / Vite → Spring Boot → Spring Data JPA → PostgreSQL
                                   └→ Flyway: creación del esquema
```

## Organización BCE

El paquete base es `edu.unse.sera`. Cada módulo reúne cuatro paquetes:

| Paquete | Responsabilidad |
| --- | --- |
| `boundary` | Controllers, DTO de entrada/salida y traducción de errores a HTTP. |
| `control` | Casos de uso, reglas de negocio y transacciones. |
| `entity` | Estado del dominio, validaciones e invariantes. |
| `persistence` | Repositories de Spring Data JPA y consultas. |

Los controllers llaman a Control y no acceden a repositories. La API recibe y
devuelve DTO; las entidades JPA no cruzan la frontera HTTP. La inyección se realiza
por constructor. No hay una capa DAO adicional.

```text
POST /api/reservas → ReservaController → ReservaService → Reserva y repositories
```

Los módulos son `usuario`, `socio`, `membresia`, `espacio`, `disponibilidad`,
`reserva`, `pagos`, `acceso`, `reporte` y `encuesta`. `shared` contiene configuración,
errores comunes y el endpoint de salud.

## Sesión y permisos

La autenticación usa una sesión HTTP. Las escrituras requieren CSRF. Los roles son
`USUARIO`, `ADMIN` y `STAFF`; el backend verifica rol, estado de cuenta y titularidad
en cada operación protegida. Las contraseñas se almacenan como hashes.

`GET /api/health` devuelve `{"status":"ok"}`. Comprueba la respuesta HTTP de la API,
sin consultar la base de datos.

## Transacciones y concurrencia

Control define las transacciones. La confirmación de un pago, su aplicación a la
membresía o reserva y el comprobante se guardan juntos.

Las versiones JPA detectan ediciones concurrentes. Los bloqueos de fila protegen
horarios, tickets, cobros y envíos de encuestas. Las restricciones únicas y la
exclusión de horarios en PostgreSQL refuerzan esas reglas.

## Pagos

El efectivo requiere confirmación administrativa. Para Mercado Pago, la API crea
la suscripción mensual o el checkout de reserva y verifica el cobro remoto antes
de aprobarlo. El retorno del navegador no acredita un pago.

Los webhooks requieren firma. La clave de solicitud, los identificadores externos
únicos y el identificador de contratación evitan duplicados y aplicaciones de
cobros antiguos. Los secretos del proveedor permanecen en el backend.

Ver [MERCADO_PAGO.md](MERCADO_PAGO.md), [SOCIOS_MEMBRESIAS.md](SOCIOS_MEMBRESIAS.md)
y [RESERVAS.md](RESERVAS.md).

## Reportes y encuestas

Los reportes usan consultas SQL sobre PostgreSQL y guardan instantáneas en
`informes`. Consultar o exportar un informe conserva sus filtros y resultados.

Las encuestas relacionan definiciones y respuestas con reservas utilizadas.
Control valida titularidad, período y valores. La combinación encuesta/reserva es
única y el cierre comparte el bloqueo con los envíos.

Ver [REPORTES_ENCUESTAS.md](REPORTES_ENCUESTAS.md).

## Esquema y entornos

`V1__esquema_inicial.sql` contiene el esquema completo de la entrega. Los cambios
posteriores deben agregarse como migraciones nuevas. Las bases creadas con el
historial anterior requieren el procedimiento de [DEVELOPMENT.md](DEVELOPMENT.md).

Docker Compose proporciona PostgreSQL local. La API admite una conexión externa
mediante variables de entorno; la automatización de la demo fija la conexión a
la base local `sera`.
