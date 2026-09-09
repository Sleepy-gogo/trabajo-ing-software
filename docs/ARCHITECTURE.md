# Arquitectura

## Vista general

SERA usa un frontend React separado y una API REST Spring Boot.

```text
Browser
   |
   v
React + Vite
   |
   | HTTP / JSON
   v
Spring Boot
   |
   v
Spring Data JPA
   |
   v
Hibernate
   |
   v
JDBC
   |
   v
PostgreSQL
```

Flyway también se conecta a PostgreSQL, pero cumple otro trabajo.

```text
Spring Boot
   |
   +-> Hibernate/JPA -> operaciones de la aplicación -> PostgreSQL
   |
   +-> Flyway        -> migraciones de esquema       -> PostgreSQL
```

Flyway no pasa por Hibernate.

## Backend y BCE

El package base es `edu.unse.sera`, tomado del ZIP de Spring Initializr. `espacio` es la primera feature de referencia y `shared` contiene infraestructura común. Los demás módulos de negocio que aparecen en esta guía son ejemplos.

`GET /api/health` devuelve un DTO con `{"status":"ok"}`. Confirma que la API responde y no consulta PostgreSQL. No mide la disponibilidad de la base de datos.

BCE separa clases según su responsabilidad.

### Boundary

Boundary representa la frontera de la aplicación.

En la API actual incluye:

```text
Controller
Request DTO
Response DTO
HTTP exception mapping
```

Ejemplo:

```text
POST /api/reservas
        |
        v
ReservaController
```

Boundary traduce HTTP a una llamada al caso de uso.

### Control

Control implementa casos de uso.

En Spring se representa principalmente con services.

```text
ReservaService.crearReserva(...)
PagoService.registrarPago(...)
SocioService.suspenderSocio(...)
```

Control coordina repositories y entidades.

Una transacción de negocio suele comenzar aquí.

### Entity

Entity representa el dominio.

```text
Socio
Reserva
Cancha
Pago
Cuota
```

Una entidad puede proteger invariantes relacionadas con su estado.

### Persistence

Persistence encapsula acceso a datos.

```text
SocioRepository
ReservaRepository
PagoRepository
```

La implementación habitual usa Spring Data JPA.

## Flujo de una request

Ejemplo conceptual:

```text
POST /api/reservas
        |
        v
CrearReservaRequest
        |
        v
ReservaController             Boundary
        |
        v
ReservaService                Control
        |
        +-> SocioRepository
        +-> CanchaRepository
        +-> ReservaRepository
        |
        v
Reserva                       Entity
        |
        v
JPA / Hibernate
        |
        v
PostgreSQL
```

El response vuelve como DTO.

```text
Reserva
   |
   v
ReservaResponse
   |
   v
HTTP JSON
```

## Dependencias permitidas

Regla práctica:

```text
boundary    -> control
boundary    -> boundary.dto

control     -> entity
control     -> persistence

persistence -> entity

entity      -> JDK
entity      -> anotaciones de persistencia cuando sean necesarias
```

Evitar:

```text
entity      -> boundary
entity      -> controller
control     -> HTTP
repository  -> controller
controller  -> repository
```

## Package by feature

No organizar todo el sistema con carpetas globales como:

```text
controllers/
services/
repositories/
entities/
```

Preferir:

```text
socio/
├── boundary/
├── control/
├── entity/
└── persistence/

reserva/
├── boundary/
├── control/
├── entity/
└── persistence/
```

Esto mantiene cerca las clases que cambian juntas.

## DTO y Entity

DTO y Entity no representan lo mismo.

```text
CrearSocioRequest
```

representa lo que la API acepta.

```text
Socio
```

representa el concepto del dominio y, de forma pragmática en este TP, también puede ser una entidad JPA.

```text
SocioResponse
```

representa lo que la API expone.

No exponer automáticamente el modelo de persistencia.

## DAO y Repository

El proyecto usa Spring Data JPA.

Por eso la abstracción principal de persistence será Repository.

```text
Control
   |
   v
Repository
   |
   v
Spring Data JPA
```

No crear simultáneamente:

```text
Control -> DAO -> Repository -> JPA
```

Eso repite responsabilidades.

Si la cátedra exige una práctica específica con JDBC y DAO, tratarla como una necesidad separada y documentarla.

## Transacciones

Los casos de uso que modifican varias cosas de forma atómica deben usar una transacción.

Ejemplo:

```text
Registrar pago
  1. guardar pago
  2. actualizar cuota
  3. actualizar estado
```

Debe ocurrir todo o nada.

En Spring, la transacción se coloca normalmente en Control:

```java
@Transactional
public void registrarPago(...) {
    ...
}
```

## Mercado Pago

El frontend no decide si un pago está aprobado.

Flujo esperado:

```text
React
  |
  | iniciar pago
  v
Spring Boot
  |
  | crea operación
  v
Mercado Pago

Mercado Pago
  |
  | webhook
  v
ngrok
  |
  v
Spring Boot
  |
  v
PagoService
  |
  v
PostgreSQL
```

Los secretos de Mercado Pago solo existen en backend.

## PostgreSQL

Flyway crea la tabla `espacios` mediante `V1__create_espacios.sql`. Hibernate usa `ddl-auto=validate` para comprobar que el modelo JPA coincide con el esquema. Cada cambio posterior del esquema debe sumar una migración nueva.

Desarrollo:

```text
Docker Compose -> PostgreSQL local
```

Entorno remoto opcional:

```text
Neon -> PostgreSQL administrado
```

No hacer que todos los desarrolladores dependan de una única base remota para trabajar.
