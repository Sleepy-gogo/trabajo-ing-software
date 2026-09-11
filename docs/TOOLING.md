# Tooling

## Java

Versión:

```text
Java 21
```

El proyecto backend usa Maven Wrapper. Nadie necesita instalar Maven globalmente.

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

## Spring Boot

Versión inicial:

```text
4.1.1
```

Dependencias base:

```text
Spring Web
Spring Data JPA
Validation
PostgreSQL Driver
Flyway Migration
Spring Boot DevTools
Docker Compose Support
```

No agregar starters sin necesidad concreta.

## Formato Java

Usar google-java-format.

Para evitar que cada integrante dependa de una instalación global, integrarlo al build de Maven.

Si se usa Spotless como wrapper del formatter:

```text
Spotless
  |
  v
google-java-format
```

Spotless no reemplaza Checkstyle.

Desde `apps/api`:

| Tarea | Linux/macOS | Windows |
| --- | --- | --- |
| Formatear | `./mvnw spotless:apply` | `.\mvnw.cmd spotless:apply` |
| Verificar formato | `./mvnw spotless:check` | `.\mvnw.cmd spotless:check` |
| Checkstyle | `./mvnw checkstyle:check` | `.\mvnw.cmd checkstyle:check` |
| Tests | `./mvnw test` | `.\mvnw.cmd test` |
| Empaquetar | `./mvnw package` | `.\mvnw.cmd package` |

Las versiones del formatter y de los plugins están fijadas en `pom.xml`.

## Checkstyle

Checkstyle valida reglas estáticas.

Ejemplos de problemas que puede detectar:

- imports incorrectos;
- convenciones de nombres;
- estructura no permitida;
- reglas acordadas por el equipo.

No usar Checkstyle como formatter.

## IntelliJ IDEA

Configurar el proyecto para usar el JDK 21.

No commitear configuraciones personales de IntelliJ salvo que el equipo acuerde compartir una configuración concreta.

El formatter ejecutado por el build tiene prioridad sobre preferencias personales del IDE.

## Node y pnpm

Frontend:

```text
Node.js LTS
pnpm
```

Usar `pnpm-lock.yaml`.

No mezclar:

```text
package-lock.json
yarn.lock
pnpm-lock.yaml
```

El repositorio debe tener un único lockfile de frontend.

## Vite

Vite ejecuta el frontend durante desarrollo.

Puerto habitual:

```text
5173
```

Configurar proxy para `/api` hacia Spring Boot durante desarrollo.

Ejemplo conceptual:

```text
/api/* -> http://localhost:4500/api/*
```

Eso evita hardcodear URLs de ngrok o localhost dentro de componentes React.

## TanStack Query

Usar TanStack Query para server state.

Casos típicos:

```text
GET /api/socios
GET /api/reservas
POST /api/reservas
```

No copiar cada response a un store global.

## React Router

React Router maneja rutas del frontend.

Ejemplos:

```text
/
 /socios
 /socios/:id
 /reservas
 /pagos
```

## Tailwind y shadcn/ui

Tailwind maneja estilos.

shadcn/ui aporta componentes que quedan dentro del código del proyecto.

No modificar componentes copiados de shadcn sin una razón de producto o accesibilidad.

## PostgreSQL y Docker Compose

PostgreSQL local corre en Docker.

Comando:

```bash
docker compose up -d
```

Ver estado:

```bash
docker compose ps
```

Logs:

```bash
docker compose logs postgres
```

Borrar contenedor y volumen solo si realmente se quiere resetear toda la base:

```bash
docker compose down -v
```

Ese comando elimina datos locales.

## Spring Boot Docker Compose support

Si el archivo Compose se mantiene en la raíz y el backend se ejecuta desde `apps/api`, configurar:

```properties
spring.docker.compose.file=../../compose.yml
```

El soporte de Docker Compose de Spring Boot puede usar ese archivo durante desarrollo.

No depender de esta integración para producción.

## Flyway

Las migraciones viven en:

```text
apps/api/src/main/resources/db/migration/
```

Ejemplo:

```text
V1__create_socios.sql
V2__create_canchas.sql
```

Flyway modifica el schema.

JPA/Hibernate realiza operaciones sobre el schema.

No mezclar ambas responsabilidades.

## ngrok

Uso previsto:

```bash
ngrok http 4500
```

Objetivo:

- demostraciones;
- pruebas desde fuera de la red local;
- recibir webhooks de Mercado Pago durante desarrollo.

No guardar tokens de ngrok en Git.

No describir ngrok como hosting permanente.

## GitHub

GitHub almacena el repositorio y centraliza PRs.

Configurar protección de `main` cuando el equipo empiece a trabajar en paralelo.

Checks recomendados para PR:

```text
backend test
backend checkstyle
backend format check
frontend lint
frontend typecheck
frontend build
```

Estos checks están configurados en `.github/workflows/checks.yml` para pull requests y pushes a `main`. El job de backend usa `mvnw verify`, que ejecuta tests, empaquetado, Spotless y Checkstyle. El job de frontend instala con el lockfile y ejecuta lint, typecheck y build. La validación de Compose comprueba su configuración sin iniciar contenedores.

## Linear

Linear se usa para tareas, no como fuente de verdad técnica.

Una decisión arquitectónica importante debe quedar también en `docs/adr/`.

## Comandos que conviene tener documentados

Backend:

```text
test
package
format
format-check
checkstyle
```

Frontend:

```text
dev
build
lint
typecheck
```

Infra:

```text
docker compose up -d
docker compose down
docker compose ps
```
