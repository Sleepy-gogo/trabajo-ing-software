# Herramientas

## Versiones

| Herramienta | Versión o requisito |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.1 |
| Maven | Wrapper del repositorio |
| Node.js | 22.13+ en la rama 22, o 24+ |
| pnpm | 11.18.0 |
| Python | 3.10+; biblioteca estándar |
| PostgreSQL | Imagen definida en `compose.yml` |
| Docker | Compose disponible y motor iniciado |

Las dependencias están en `apps/api/pom.xml`, `apps/web/package.json` y
`apps/web/pnpm-lock.yaml`.

## Automatización local

Desde la raíz:

```powershell
python scripts/sera.py doctor
python scripts/sera.py setup
python scripts/sera.py start --demo
python scripts/sera.py verify
```

Los wrappers `scripts/sera.ps1` y `scripts/sera.sh` llaman al mismo script.
`doctor` comprueba requisitos; `setup` instala y verifica; `start` inicia la API
y Vite; `verify` ejecuta las comprobaciones. El reinicio con respaldo está en
la [guía de instalación](INSTALACION_SERA.docx).

## Java

Desde `apps/api`:

| Operación | Windows | Linux/macOS |
| --- | --- | --- |
| Formato | `.\mvnw.cmd spotless:apply` | `./mvnw spotless:apply` |
| Comprobación de formato | `.\mvnw.cmd spotless:check` | `./mvnw spotless:check` |
| Estilo | `.\mvnw.cmd checkstyle:check` | `./mvnw checkstyle:check` |
| Tests | `.\mvnw.cmd test` | `./mvnw test` |
| Verificación completa | `.\mvnw.cmd verify` | `./mvnw verify` |

Spotless aplica google-java-format. Checkstyle verifica convenciones e imports.
`verify` ejecuta tests, empaquetado y ambas comprobaciones. El IDE debe usar Java 21
y respetar el formato del repositorio.

## Interfaz web

Desde `apps/web`:

```bash
pnpm install --frozen-lockfile
pnpm dev
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

`build` ejecuta TypeScript, Vitest y Vite. Prettier define el formato. El frontend
es un único paquete; `pnpm-workspace.yaml` declara permisos de instalación.

## PostgreSQL y Flyway

```bash
docker compose up -d
docker compose ps
docker compose config --quiet
docker compose stop postgres
```

Flyway usa `apps/api/src/main/resources/db/migration/`. La migración inicial es
`V1__esquema_inicial.sql`; Hibernate comprueba el modelo contra ese esquema.
El reinicio de demo conserva otras bases y guarda un respaldo antes del borrado.

## Integraciones y seguimiento

ngrok expone un puerto local para una demostración o para recibir webhooks.
No sustituye un despliegue permanente. La configuración está en
[DEVELOPMENT.md](DEVELOPMENT.md) y [MERCADO_PAGO.md](MERCADO_PAGO.md).

GitHub almacena el repositorio y ejecuta `.github/workflows/checks.yml` en los
pull requests y cambios a `main`. El flujo verifica backend, frontend y Compose.
Linear organiza las tareas; los contratos técnicos se documentan en el repositorio.
