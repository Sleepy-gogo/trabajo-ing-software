# Instalación y presentación de SERA

Instalación local, carga de datos de demo y recorrido de presentación.

## Requisitos

- Git.
- Java 21, visible mediante `java -version`.
- Docker Desktop iniciado, o Docker Engine con Compose.
- Node 22.13+ dentro de la rama 22, o Node 24+.
- pnpm 11.18.0, según `apps/web/package.json`. Si falta, instalar con `npm install -g pnpm@11.18.0`.
- Python 3.10+ para los scripts, visible como `python` en Windows o `python3` en Linux/macOS.

No hace falta instalar Maven global ni paquetes de Python. Los scripts usan Maven Wrapper, el lockfile de pnpm y la biblioteca estándar de Python. La implementación se verificó en Windows; los wrappers de Linux/macOS usan el mismo script y todavía requieren una ejecución en esos sistemas para confirmar el entorno.

## Primera instalación

Clonar el repositorio y abrir una terminal en su raíz. En Windows:

```powershell
python scripts/sera.py doctor
python scripts/sera.py setup
python scripts/sera.py start --demo
```

En Linux/macOS, reemplazar `python` por `python3`. También se puede usar `powershell -File scripts/sera.ps1 setup` o `sh scripts/sera.sh setup`.

El wrapper de PowerShell depende de la política de ejecución del equipo. Si Windows bloquea `.ps1`, usar los comandos `python scripts/sera.py` indicados arriba; no hace falta cambiar esa política.

`doctor` comprueba versiones y que Docker responda. `setup` crea los archivos de entorno a partir de los ejemplos si faltan, instala con `--frozen-lockfile`, inicia PostgreSQL y ejecuta tests, compilación, Spotless, Checkstyle y lint. No sobrescribe archivos de entorno existentes ni borra datos.

`start` inicia el JAR y Vite, espera que ambos respondan y mantiene los procesos asociados a la terminal. La API se ejecuta en UTC; los horarios de negocio siguen siendo de Argentina. Si un servicio falla, revisar `.sera/logs/api.log` o `.sera/logs/web.log`. Ctrl+C detiene la API y el frontend iniciados por esa ejecución. PostgreSQL queda activo; para detenerlo usar `docker compose stop postgres`.

Abrir <http://localhost:5173>. API: <http://localhost:4500>. PostgreSQL local: `localhost:5432`, base/usuario/contraseña `sera`. Los scripts fijan esta conexión local aunque la terminal o `.env` tengan otra configuración. Para trabajar con una base externa, usar los comandos manuales de [DEVELOPMENT.md](DEVELOPMENT.md).

La opción `--demo` pide una contraseña para las tres cuentas ficticias, de al menos 8 caracteres. Para una presentación local se puede usar `SeraDemo2026!`. Si las cuentas ya existen, usar su misma contraseña. Cambiar el valor no modifica contraseñas guardadas. Repetir la carga recupera las cuentas y datos existentes, sin duplicar los fixtures completos.

| Cuenta | Rol | Uso |
| --- | --- | --- |
| `admin@sera.local` | ADMIN | Usuarios, espacios, pagos, informes y encuestas |
| `socio@sera.local` | USUARIO | Membresía activa, reserva utilizada y encuesta disponible |
| `staff@sera.local` | STAFF | Validación de ingresos |

La carga crea tres espacios con horarios y tarifas, un nivel de membresía, una cuota y una reserva pagadas en efectivo, una encuesta y cuatro informes guardados. La reserva histórica se prepara como fixture local con ingreso de ayer; no representa una operación real ni requiere esperar a que pase un horario. Las cuentas tienen relación UNSE pendiente de verificación, por lo que se aplica la tarifa de externo hasta que administración la verifique.

## Arranques posteriores y comprobaciones

```powershell
python scripts/sera.py start
```

Este comando conserva los datos y no pide una contraseña. Para ejecutar las comprobaciones, primero detener la aplicación con Ctrl+C:

```powershell
python scripts/sera.py verify
```

En Windows, un JAR en ejecución puede impedir que Maven lo reemplace. `verify` ejecuta `mvnw verify`, `pnpm lint`, `pnpm build` y `docker compose config --quiet`; el build frontend incluye TypeScript y Vitest.

## Reiniciar la demo local

Detener la API, el frontend y cualquier cliente conectado a `sera`. El reinicio es una acción separada y explícita:

```powershell
python scripts/sera.py reset-demo --confirm REINICIAR-SERA-LOCAL
```

Pide la contraseña, guarda un respaldo binario con `pg_dump`, elimina el esquema de la base local `sera`, arranca la API para que Flyway aplique todas las migraciones y carga la demo. No borra el volumen de Docker ni otras bases del contenedor. Si hay conexiones abiertas o falla el respaldo, no ejecuta el borrado.

El reinicio solo cambia datos locales; no cancela suscripciones remotas de Mercado Pago. El respaldo conserva las referencias de la base anterior. Para cancelar cobros externos, usar el flujo de cancelación de membresía antes del reinicio o administrar la suscripción en el proveedor.

Los respaldos se guardan fuera del repositorio, en `%LOCALAPPDATA%/SERA/backups` en Windows y `~/.local/share/SERA/backups` en Linux/macOS. La consola informa la ruta exacta. Con `--once`, migra y carga la demo, verifica la API y la detiene sin iniciar la web:

```powershell
python scripts/sera.py reset-demo --confirm REINICIAR-SERA-LOCAL --once
```

Para restaurar un respaldo, detener la API, copiarlo al contenedor y restaurar su esquema. Elegir el archivo que informa la consola:

```powershell
$container = docker compose ps -q postgres
docker cp 'C:\ruta\sera-fecha.dump' "${container}:/tmp/sera-backup.dump"
docker compose exec -T postgres psql -U sera -d sera -v ON_ERROR_STOP=1 -c 'DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION sera;'
docker compose exec -T postgres pg_restore -U sera -d sera --no-owner --exit-on-error /tmp/sera-backup.dump
```

## Recorrido para profesores

La [guía de usuario](GUIA_USUARIO.md) desarrolla estos recorridos con capturas de
la interfaz y explica los estados, errores y acciones disponibles para cada rol.

1. Entrar como administración y revisar espacios, horarios, tarifas, usuarios y nivel de membresía.
2. Entrar como socio. Mi membresía está activa y Mis pagos muestra una cuota y una reserva aprobadas, con sus comprobantes.
3. Abrir Encuestas, elegir la reserva utilizada, completar las preguntas y enviar. Recargar la página para comprobar que la respuesta sigue guardada.
4. Volver como administración. En Encuestas, abrir resultados y revisar titular, reserva, promedio y respuestas.
5. Abrir Informes, generar socios, reservas, pagos y uso de servicios. Cambiar período, estado, relación o espacio según el tipo. Descargar CSV y abrir el historial para verificar que los filtros y resultados se conservan.
6. Como socio, crear una reserva futura en efectivo. Antes de confirmar el cobro no tiene código de acceso. Como administración, confirmar efectivo desde Pagos. Volver al detalle del socio y revisar el QR.
7. Cancelar esa reserva antes del inicio. Revisar que el horario vuelve a estar libre y usar su ticket en otra reserva, con diferencia de precio cuando corresponda.
8. Como personal, validar el código de una reserva. Una reserva futura se rechaza antes del horario; una reserva ya consumida se rechaza porque su ingreso ya fue registrado.

## Pagos y Mercado Pago

La demo completa se puede evaluar sin credenciales externas, usando efectivo confirmado por ADMIN. Repetir la confirmación no renueva dos veces la membresía ni genera otro código. Un usuario no puede confirmar efectivo ni consultar pagos ajenos. Un pago pendiente o rechazado no activa beneficios.

Mercado Pago está implementado para cuotas mediante Suscripciones y para reservas mediante Checkout Pro. Un regreso al sitio con `status=approved` no acredita el pago. La API consulta al proveedor y valida el cobro; el webhook requiere firma. Un cobro de reserva que llega después de cancelación o vencimiento queda para revisión y no recupera el horario.

La configuración está en [MERCADO_PAGO.md](MERCADO_PAGO.md) y [RESERVAS.md](RESERVAS.md). Los tests automatizados usan dobles del proveedor y operaciones de efectivo contra PostgreSQL. Para una demo remota, seguir [DEVELOPMENT.md](DEVELOPMENT.md), exponer Vite con ngrok y permitir el hostname. `dist` necesita un servidor con proxy `/api` para conservar el flujo de sesión.

## Prueba de integración aislada

Sin una API en el puerto 4501, crear una base de prueba separada:

```powershell
docker compose exec -T postgres psql -U sera -d postgres -c 'CREATE DATABASE sera_cierre_test;'
cd apps/api
java '-Duser.timezone=UTC' -jar target/sera-0.0.1-SNAPSHOT.jar --spring.profiles.active=entrega --spring.datasource.url=jdbc:postgresql://localhost:5432/sera_cierre_test --spring.datasource.username=sera --spring.datasource.password=sera --spring.docker.compose.enabled=false --server.port=4501
```

En otra terminal desde la raíz:

```powershell
python scripts/smoke_cierre.py
```

La prueba confirma que los fixtures se escribieron en la base aislada antes de asignar permisos. Verifica pagos en efectivo y renovación única, permisos, las cuatro consultas de reportes, filtros, CSV, historial, validaciones de preguntas, cierre de encuesta, concurrencia de respuestas y estadísticas. No borra datos, no toca `sera` y no consulta Mercado Pago. Si la base ya existe, omitir `CREATE DATABASE`. Detener esta API antes de volver a empaquetar o reiniciar la demo.

Para comprobar cancelación y nueva contratación de membresías, usar el mismo
arranque con una base separada `sera_membresias_test` y ejecutar
`python scripts/smoke_membresias.py`. La API debe usar esa base y no tener
credenciales de Mercado Pago: este smoke comprueba también el error de proveedor
sin configurar y nunca debe iniciar cobros externos. No compartir el puerto 4501
con la prueba anterior. La confirmación concurrente, el historial paginado y la
renovación vencida se comprueban con operaciones HTTP y fixtures SQL aislados.

La migración inicial consolidada requiere bases de prueba vacías. Las bases con
el historial anterior no se actualizan automáticamente; ver
[DEVELOPMENT.md](DEVELOPMENT.md#migraciones).

## Límites pendientes

El control de accesos registra el consumo de reservas con fecha y responsable. Todavía faltan el historial de cada intento autorizado/rechazado y una autorización manual con motivo, según TRA-46, TRA-49 y la parte de historial de TRA-50. Esas tareas quedan abiertas. El despliegue en Vercel de TRA-56 también queda pendiente; la entrega documentada usa el entorno local y el proxy de Vite.

Los informes son instantáneas operativas, sin exportación PDF ni borrado del historial. Las encuestas están asociadas a reservas utilizadas, no a visitas generales sin reserva. Estas decisiones mantienen el modelo simple y se detallan en [REPORTES_ENCUESTAS.md](REPORTES_ENCUESTAS.md).
