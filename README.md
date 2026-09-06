# SERA

SERA es el sistema desarrollado para el trabajo práctico de Ingeniería de Software. El proyecto usa un monorepo simple con frontend React y backend Java/Spring Boot.

## Stack

Backend:

- Java 21
- Spring Boot 4.1.1
- Spring Web
- Spring Data JPA
- Hibernate
- Jakarta Validation
- Flyway
- PostgreSQL
- Maven Wrapper

Frontend:

- Vite
- TypeScript
- React
- React Router
- TanStack Query
- Tailwind CSS
- shadcn/ui

Infraestructura y desarrollo:

- Docker Compose para PostgreSQL local
- ngrok para exponer la aplicación durante pruebas y demostraciones
- Git + GitHub
- IntelliJ IDEA para Java
- Linear para seguimiento de tareas
- google-java-format
- Checkstyle

## Estructura

```text
sera/
├── apps/
│   ├── api/
│   └── web/
├── docs/
│   ├── adr/
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md
│   └── TOOLING.md
├── .github/
│   └── pull_request_template.md
├── AGENTS.md
├── REPOSITORY_RULES.md
├── PROJECT_SPEC.md
├── compose.yml
└── README.md
```

## Arquitectura del backend

El backend sigue BCE, organizado por feature.

```text
edu.unse.sera
├── socio/
│   ├── boundary/
│   ├── control/
│   ├── entity/
│   └── persistence/
├── reserva/
│   ├── boundary/
│   ├── control/
│   ├── entity/
│   └── persistence/
├── pago/
│   ├── boundary/
│   ├── control/
│   ├── entity/
│   └── persistence/
└── shared/
```

La regla principal es:

```text
Boundary -> Control -> Entity
                |
                +-> Persistence -> JPA/Hibernate -> PostgreSQL
```

Los controllers no acceden directamente a repositories. Las entidades no conocen HTTP. Los DTO no se persisten.

Los módulos del diagrama son ejemplos para futuras features. El bootstrap solo incluye `shared` y `GET /api/health`, que devuelve `{"status":"ok"}` sin consultar la base de datos. El package `edu.unse.sera` se conserva del ZIP de Spring Initializr.

## Desarrollo local

Requisitos: JDK 21, Docker con Compose, Node.js 22.13+ dentro de la rama 22 o Node.js 24+ y pnpm. Ejecutar el backend desde `apps/api` para que encuentre el Compose de la raíz. Ver [la guía de desarrollo](docs/DEVELOPMENT.md) para configurar Java y los perfiles.

1. Levantar PostgreSQL:

```bash
docker compose up -d
```

2. Levantar backend:

Windows:

```powershell
cd apps/api
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
cd apps/api
./mvnw spring-boot:run
```

3. Levantar frontend:

```bash
cd apps/web
pnpm install
pnpm dev
```

Abrir <http://localhost:5173>. La página inicial consulta `/api/health` mediante el proxy de Vite al backend en el puerto 8080 y muestra el estado de la conexión.

## Checks

Desde `apps/api`, usar `./mvnw` en Linux/macOS o `.\mvnw.cmd` en Windows:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check checkstyle:check test package
```

Desde `apps/web`:

```bash
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm build
```

Desde la raíz: `docker compose config`.

## Documentación

Antes de modificar la estructura del repositorio o agregar una dependencia importante, leer:

- `REPOSITORY_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/TOOLING.md`
- `docs/DEVELOPMENT.md`

Los agentes de código también deben leer `AGENTS.md`.
