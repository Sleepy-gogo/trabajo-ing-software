# Desarrollo local

## Requisitos

Instalar:

- Git
- JDK 21
- Docker Desktop o Docker Engine con Compose
- Node.js LTS
- pnpm
- IntelliJ IDEA recomendado para backend

Maven global no es obligatorio porque el backend incluye Maven Wrapper.

Comprobar `java -version` antes de usar el wrapper. Debe indicar Java 21. Si Windows usa otra versión, configurar el JDK para la terminal actual, ajustando la ruta a la instalación local:

```powershell
java -version
```

El frontend requiere Node.js 22.13+ dentro de la rama 22 o Node.js 24+. Usar la versión de pnpm declarada en `apps/web/package.json` y conservar `apps/web/pnpm-lock.yaml` como único lockfile.

El comando `spring-boot:run` configura la JVM de la API en UTC. Esto evita aliases del sistema operativo como `America/Buenos_Aires`, que algunas versiones de PostgreSQL no reconocen. Al ejecutar desde IntelliJ o con `java -jar`, agregar la opción de JVM `-Duser.timezone=UTC`. La zona horaria de los futuros casos de uso se definirá con sus requisitos.

## Primera ejecución

Desde la raíz:

```bash
docker compose up -d
```

Confirmar:

```bash
docker compose ps
```

Después levantar backend.

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

En otra terminal:

```bash
cd apps/web
pnpm install
pnpm dev
```

## Puertos

Convención inicial:

```text
Frontend Vite:   5173
Backend Spring:  4500
PostgreSQL:      5432
```

No asumir que estos puertos estarán libres en todas las máquinas. Si se cambia uno, documentarlo.

## Variables de entorno

Spring usa `dev` como perfil por defecto. Este perfil encuentra `../../compose.yml` desde `apps/api` y usa las credenciales locales `sera`. El backend puede iniciar PostgreSQL mediante el soporte de Docker Compose; `docker compose up -d` permite iniciarlo explícitamente y revisar su estado primero.

Para una base externa, activar un perfil distinto con `SPRING_PROFILES_ACTIVE` y proporcionar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. La URL debe ser JDBC, por ejemplo `jdbc:postgresql://host:5432/sera`. Spring no carga archivos `.env` automáticamente.

Nunca commitear secretos.

Para configurar el proxy del frontend, copiar `apps/web/.env.example` a
`apps/web/.env.local` y ajustar:

```dotenv
API_PROXY_TARGET=https://mi-api.ngrok-free.app
WEB_ALLOWED_HOSTS=mi-frontend.ngrok-free.app
```

`API_PROXY_TARGET` es el origen del backend, sin `/api` al final; por defecto usa
`http://localhost:4500`. Se aplica a `pnpm dev` y `pnpm preview`. El proxy conserva
el prefijo `/api`, adapta el header `Host` al destino y omite la pantalla de aviso
de ngrok. Reiniciar Vite después de cambiar estas variables.

`WEB_ALLOWED_HOSTS` permite los dominios con los que se accede al frontend a través
de ngrok o de otro reverse proxy. Usar nombres de host sin protocolo ni puerto,
separados por comas. Para exponer la demo completa localmente, ejecutar
`ngrok http 5173` y agregar el dominio asignado a esta variable; el destino de la
API puede seguir siendo local.

El navegador siempre consume `/api` en el mismo origen que la interfaz, conservando
las cookies de sesión y CSRF. Al desplegar `dist` en un servidor estático, configurar
el reverse proxy de ese servidor para reenviar `/api/*` al backend, conservando la
ruta. La configuración del proxy de Vite no se incluye en `dist`; `pnpm preview`
sirve para verificar el build localmente. El puerto de Spring se puede cambiar con
la variable de entorno `SERVER_PORT`.

Backend puede usar variables de entorno desde Spring:

```properties
mercadopago.access-token=${MERCADOPAGO_ACCESS_TOKEN}
```

No poner el valor real en `application.properties`.

## Base de datos

PostgreSQL local debe arrancar con Docker Compose.

Para reset total de datos locales:

```bash
docker compose down -v
docker compose up -d
```

Hacerlo solo cuando sea intencional.

## Migraciones

`V1__create_espacios.sql` es la primera migración. Hibernate usa `ddl-auto=validate` y detiene el arranque si el modelo JPA no coincide con el esquema creado por Flyway.

Al agregar o modificar schema:

1. crear una nueva migración Flyway;
2. actualizar la Entity JPA;
3. ejecutar aplicación desde una base limpia cuando sea posible;
4. verificar que Flyway complete todas las migraciones;
5. no editar migraciones que ya estén en `main`.

## Crear una feature

Ejemplo para `reserva`:

```text
apps/api/src/main/java/edu/unse/sera/reserva/
├── boundary/
│   ├── ReservaController.java
│   └── dto/
├── control/
│   └── ReservaService.java
├── entity/
│   └── Reserva.java
└── persistence/
    └── ReservaRepository.java
```

Orden práctico recomendado:

1. entender el caso de uso;
2. modelar Entity;
3. crear migración;
4. crear Repository;
5. implementar Service;
6. crear DTOs;
7. crear Controller;
8. agregar tests;
9. integrar frontend.

No es una regla absoluta. Es un orden que evita empezar por HTTP sin haber pensado el dominio.

## Endpoint REST

Ejemplo de recorrido:

```text
POST /api/reservas
      |
      v
CrearReservaRequest
      |
      v
ReservaController
      |
      v
ReservaService
      |
      +-> SocioRepository
      +-> CanchaRepository
      +-> ReservaRepository
      |
      v
Reserva
```

El Controller traduce HTTP.

El Service ejecuta el caso de uso.

La Entity representa dominio.

El Repository accede a datos.

## Errores

Una excepción de dominio no debe construir directamente una respuesta HTTP.

Ejemplo:

```java
throw new HorarioNoDisponibleException(...);
```

Boundary puede convertirla después en:

```text
409 Conflict
```

mediante un handler central.

## Tests

Backend:

```bash
./mvnw test
```

Frontend:

```bash
pnpm typecheck
pnpm lint
pnpm build
```

No dejar un PR con checks conocidos fallando.

El test HTTP del health endpoint no necesita Docker. El arranque completo de la API sí necesita PostgreSQL disponible. Para comprobar la conexión entre aplicaciones, levantar ambas y abrir <http://localhost:5173>: debe mostrar que la API está disponible. También se puede consultar `http://localhost:4500/api/health` directamente.

## Formato

Formatear Java antes del PR.

No depender de "Reformat Code" de un IDE como única fuente de formato.

Desde `apps/api`, ejecutar `./mvnw spotless:apply` para formatear y `./mvnw spotless:check checkstyle:check` para verificar. En Windows, reemplazar `./mvnw` por `.\mvnw.cmd`.

Frontend debe mantener el formatter/linter definido por el proyecto.

## Demo con ngrok

Para exponer Spring Boot:

```bash
ngrok http 4500
```

ngrok entrega una URL HTTPS pública.

La URL cambia según la configuración y el plan de ngrok. No hardcodearla en código fuente.

Para webhooks de Mercado Pago, configurar temporalmente la URL pública del endpoint correspondiente.

Ejemplo conceptual:

```text
https://<tunnel>/api/webhooks/mercadopago
```

## Antes de abrir un PR

Backend:

```bash
cd apps/api
./mvnw test
./mvnw package
```

Frontend:

```bash
cd apps/web
pnpm typecheck
pnpm lint
pnpm build
```

Desde raíz:

```bash
docker compose config
```

Revisar además:

```text
git status
git diff
```

No incluir archivos generados accidentalmente.
