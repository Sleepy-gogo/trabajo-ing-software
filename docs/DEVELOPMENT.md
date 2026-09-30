# Desarrollo local

La instalación automatizada y la carga de demo están en [ENTREGA.md](ENTREGA.md).
Esta guía describe el arranque manual, la configuración y las comprobaciones.

## Requisitos

Java 21, Docker con Compose, Node.js 22.13+ dentro de la rama 22 o 24+, y pnpm
11.18.0. Maven Wrapper está incluido. La automatización requiere Python 3.10+.

```powershell
java -version
node --version
pnpm --version
docker compose version
```

En Windows, `JAVA_HOME` y `PATH` deben apuntar a Java 21.

## Arranque manual

Desde la raíz:

```powershell
docker compose up -d
docker compose ps
cd apps/api
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
cd apps/web
pnpm install --frozen-lockfile
pnpm dev
```

En Linux/macOS se usa `./mvnw` en lugar de `.\mvnw.cmd`.

| Servicio | Dirección local |
| --- | --- |
| Web | `http://localhost:5173` |
| API | `http://localhost:4500` |
| PostgreSQL | `localhost:5432`, base/usuario/contraseña `sera` |

El perfil `dev` es el predeterminado. Encuentra `../../compose.yml` desde
`apps/api`. El arranque con Maven configura UTC; al ejecutar el JAR o desde el
IDE debe agregarse `-Duser.timezone=UTC`.

## Variables de entorno

Copiar `apps/api/.env.example` a `apps/api/.env` y
`apps/web/.env.example` a `apps/web/.env.local`. Los archivos con valores locales
no se versionan.

El perfil `dev` importa el `.env` de la API al iniciar desde `apps/api`.
Los valores no llevan comillas ni `export`. Las variables del proceso tienen
prioridad; los cambios requieren reiniciar la API.

La interfaz usa `API_PROXY_TARGET` para reenviar `/api`, con valor predeterminado
`http://localhost:4500`. `WEB_ALLOWED_HOSTS` admite nombres de host separados por
comas para accesos mediante túnel.

Para una base externa se activa otro perfil y se definen:

```text
SPRING_PROFILES_ACTIVE=entrega
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/base
SPRING_DATASOURCE_USERNAME=usuario
SPRING_DATASOURCE_PASSWORD=contraseña
SPRING_DOCKER_COMPOSE_ENABLED=false
```

Las variables de Mercado Pago están en [MERCADO_PAGO.md](MERCADO_PAGO.md).

## Migraciones

`V1__esquema_inicial.sql` crea las tablas, índices y restricciones de la entrega.
Hibernate usa `ddl-auto=validate`. Cada cambio posterior requiere una migración
nueva y la actualización correspondiente del modelo JPA.

El historial anterior V1–V20 se consolidó en esta migración inicial. Una base con
ese historial no debe iniciarse directamente con el esquema consolidado ni
corregirse con `flyway repair`: sus checksums y versiones pertenecen a la versión
anterior del código.

Para la demo local, detener la aplicación y ejecutar:

```powershell
python scripts/sera.py reset-demo --confirm REINICIAR-SERA-LOCAL --once
```

El comando guarda un respaldo y recrea únicamente `sera`. Las bases aisladas de
pruebas anteriores se conservan; las nuevas pruebas deben usar una base vacía.
Para recuperar una base con el historial anterior, restaurar su respaldo y usar
la revisión del código que contiene aquellas migraciones.

## Implementación y comprobaciones

Los módulos siguen BCE por funcionalidad. Boundary traduce HTTP y transporta DTO;
Control coordina los casos de uso y transacciones; Entity mantiene invariantes;
Persistence accede a los datos. Ver [ARCHITECTURE.md](ARCHITECTURE.md) y el
[CRUD de referencia](CRUD_REFERENCE.md).

Desde `apps/api`:

```powershell
.\mvnw.cmd spotless:apply verify
```

Desde `apps/web`:

```bash
pnpm lint
pnpm typecheck
pnpm build
```

Desde la raíz:

```bash
docker compose config --quiet
git diff --check
```

`verify` incluye tests, empaquetado, Spotless y Checkstyle. El build web incluye
Vitest. El endpoint `/api/health` comprueba la respuesta HTTP; el arranque completo
con Flyway y validación JPA comprueba la conexión y el esquema.

Los scripts `smoke_*.py` prueban recorridos HTTP con PostgreSQL. Su ejecución usa
bases aisladas y no debe compartir datos con la demo. Ver [ENTREGA.md](ENTREGA.md),
[RESERVAS.md](RESERVAS.md) y [SOCIOS_MEMBRESIAS.md](SOCIOS_MEMBRESIAS.md).

## Demostración remota

Para publicar temporalmente la web con su proxy:

```bash
ngrok http 5173
```

Agregar el hostname del túnel a `WEB_ALLOWED_HOSTS`, reiniciar Vite y configurar
la URL pública de retorno de Mercado Pago. Para recibir webhooks, el túnel debe
reenviar `/api/webhooks/mercadopago` a la API. La URL y la firma se configuran en
el panel del proveedor.

Un servidor de archivos estáticos para `dist` necesita también el proxy `/api`
y el soporte de sesión. Los túneles son temporales; su hostname no se guarda en
el código ni constituye una configuración de producción.
