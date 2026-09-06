# Instrucciones para agentes

Leer este archivo antes de modificar el repositorio.

Después leer:

1. `REPOSITORY_RULES.md`
2. `docs/ARCHITECTURE.md`
3. `docs/TOOLING.md`
4. `docs/DEVELOPMENT.md`

## Objetivo

Mantener un monorepo simple para SERA, un trabajo práctico universitario de Ingeniería de Software.

No convertirlo en una arquitectura empresarial más compleja de lo necesario.

## Restricciones

- Java 21.
- Spring Boot 4.1.1.
- Maven Wrapper.
- React + TypeScript + Vite.
- PostgreSQL.
- Docker Compose para desarrollo local.
- Spring Data JPA + Hibernate.
- Flyway para migraciones.
- BCE para organización del backend.
- REST + JSON entre frontend y backend.
- pnpm para frontend.

No cambiar estas decisiones sin instrucción explícita.

## Estructura del repositorio

```text
apps/
├── api/
└── web/

docs/
├── adr/
├── ARCHITECTURE.md
├── DEVELOPMENT.md
└── TOOLING.md

compose.yml
README.md
REPOSITORY_RULES.md
PROJECT_SPEC.md
```

## Reglas de implementación

No crear controllers que llamen repositories directamente.

No poner reglas de negocio en controllers.

No devolver entidades JPA desde la API.

No recibir entidades JPA como request body.

No crear una capa DAO adicional si ya se usa Spring Data JPA.

No usar field injection.

No usar `ddl-auto=update` como estrategia de schema.

No modificar una migración de Flyway ya consolidada.

No agregar Lombok, MapStruct, Spring Security u otras dependencias si la tarea no las necesita.

No generar código de dominio ficticio para "completar" el proyecto. Si falta una definición funcional, dejar una nota clara o preguntar.

## Forma de trabajar

Antes de editar:

1. inspeccionar archivos existentes;
2. identificar qué parte de la arquitectura toca el cambio;
3. mantener el alcance pequeño;
4. reutilizar convenciones existentes.

Después de editar:

1. formatear;
2. ejecutar checks;
3. ejecutar tests relevantes;
4. verificar que el backend arranque si se modificó configuración;
5. verificar que el frontend compile si se modificó frontend;
6. resumir archivos modificados y decisiones tomadas.

## Backend BCE

Para una feature `socio`:

```text
socio/
├── boundary/
│   ├── SocioController.java
│   └── dto/
├── control/
│   └── SocioService.java
├── entity/
│   └── Socio.java
└── persistence/
    └── SocioRepository.java
```

Boundary recibe y devuelve DTO.

Control implementa el caso de uso.

Entity contiene estado y comportamiento del dominio.

Persistence encapsula acceso a datos.

## Testing

Prioridad:

1. tests unitarios de lógica de dominio;
2. tests unitarios de Control;
3. tests de integración de persistence cuando una query no sea trivial;
4. tests HTTP para endpoints importantes.

No escribir tests que solo repitan getters, setters o comportamiento del framework.

## Cambios de base de datos

Cada cambio estructural necesita migración Flyway.

Ejemplo:

```text
V4__add_estado_to_socios.sql
```

El modelo JPA debe quedar alineado con la migración.

## Respuesta esperada del agente

Al terminar una tarea, informar:

- qué se cambió;
- por qué;
- comandos ejecutados;
- tests/checks ejecutados;
- cualquier decisión pendiente o supuesto realizado.

No afirmar que un comando pasó si no se ejecutó.
