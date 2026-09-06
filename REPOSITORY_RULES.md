# Reglas del repositorio

Estas reglas se aplican a todo el monorepo SERA.

## 1. Organización general

El repositorio representa un único producto y contiene frontend, backend, infraestructura local y documentación.

```text
apps/api   -> backend Spring Boot
apps/web   -> frontend React
docs       -> arquitectura, decisiones y guías
compose.yml -> servicios locales de desarrollo
```

No crear repositorios adicionales para frontend, backend o base de datos salvo que la cátedra exija explícitamente separar algún componente.

El componente reutilizable compartido entre grupos, si la consigna exige un repositorio común externo, se mantiene fuera de este repositorio y se consume como dependencia.

## 2. Branches

La rama estable es `main`.

No trabajar directamente sobre `main`.

Formato recomendado:

```text
feat/SERA-23-registrar-socio
fix/SERA-31-corregir-reserva-duplicada
docs/SERA-42-documentar-bce
chore/SERA-50-configurar-checkstyle
```

Las ramas deben vivir lo menos posible. Evitar ramas que acumulen varias features sin relación.

## 3. Commits

Usar Conventional Commits.

Ejemplos:

```text
feat(api): agregar registro de socio
feat(web): agregar formulario de reserva
fix(api): rechazar reservas superpuestas
docs: explicar reglas del paquete BCE
chore(api): configurar checkstyle
test(api): cubrir servicio de reserva
```

No usar mensajes como:

```text
cambios
cosas
fix
final
final ahora si
```

## 4. Pull requests

Cada PR debe resolver una tarea concreta.

Un PR puede tocar frontend, backend y migraciones si todo corresponde a la misma feature.

Antes de mergear:

- el backend compila;
- los tests pasan;
- Checkstyle pasa;
- el formatter no deja cambios;
- el frontend compila;
- no hay secretos en el diff;
- las migraciones nuevas arrancan desde una base limpia;
- la arquitectura BCE no se rompe.

No mergear código comentado, archivos temporales, dumps de base de datos ni configuraciones personales del IDE.

## 5. Reglas BCE

### Boundary

Incluye los puntos de entrada y salida relacionados con el exterior.

Ejemplos:

- REST controllers;
- request DTOs;
- response DTOs;
- manejo HTTP;
- validación estructural de entrada.

Un Boundary puede llamar a Control.

Un Boundary no accede directamente a repositories.

Un Boundary no implementa reglas de negocio.

### Control

Implementa casos de uso y coordina el dominio.

Ejemplos:

- registrar socio;
- crear reserva;
- cancelar reserva;
- registrar pago;
- suspender socio.

En Spring, la implementación habitual es una clase `@Service`.

Las transacciones que abarcan un caso de uso se declaran en esta capa.

Control puede usar Entity y Persistence.

Control no conoce `HttpServletRequest`, `ResponseEntity`, códigos HTTP ni detalles de React.

### Entity

Representa conceptos del dominio.

Ejemplos:

- `Socio`;
- `Reserva`;
- `Cancha`;
- `Pago`;
- `Cuota`.

Una entidad puede contener reglas relacionadas con su propio estado.

Ejemplo:

```java
public boolean puedeReservar() {
    return estado == EstadoSocio.ACTIVO && !tieneDeudaVencida();
}
```

No devolver entidades JPA directamente desde controllers.

### Persistence

Contiene acceso a datos.

En el stack actual se usa Spring Data JPA.

Ejemplo:

```java
public interface SocioRepository extends JpaRepository<Socio, Long> {
    Optional<Socio> findByEmail(String email);
}
```

No agregar una capa DAO adicional encima de `JpaRepository` sin una necesidad concreta.

Si una actividad de la materia exige JDBC o DAO explícitamente, implementarlo como ejercicio o módulo separado y documentar esa excepción.

## 6. DTOs

Los DTO existen para transportar datos entre la API y sus consumidores.

Para requests y responses simples, preferir `record`.

```java
public record CrearSocioRequest(
    @NotBlank String nombre,
    @Email String email
) {}
```

No usar una entidad JPA como request body.

No exponer automáticamente una entidad JPA como respuesta HTTP.

## 7. Dependency injection

Usar constructor injection.

Correcto:

```java
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;

    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }
}
```

Evitar field injection:

```java
@Autowired
private ReservaRepository reservaRepository;
```

Una clase de dominio normal no debe depender del contenedor de Spring para poder ejecutarse.

## 8. Persistencia y migraciones

Flyway es la fuente de verdad para cambios de esquema.

Las migraciones viven en:

```text
apps/api/src/main/resources/db/migration/
```

Formato:

```text
V1__create_socios.sql
V2__create_canchas.sql
V3__create_reservas.sql
```

Una migración ya mergeada a `main` no se modifica. Crear una nueva.

No usar `ddl-auto=update` como mecanismo permanente de evolución de esquema.

JPA modela y usa el esquema. Flyway lo crea y modifica.

## 9. Java

Usar Java 21.

Convenciones:

```text
Clase:          PascalCase
Método:         camelCase
Variable:       camelCase
Constante:      UPPER_SNAKE_CASE
Package:        lowercase
```

Una clase pública por archivo.

Usar `BigDecimal` para dinero.

Preferir tipos de fecha de `java.time`.

Evitar `null` como señal de "no encontrado" cuando `Optional` represente mejor el resultado.

No usar Lombok inicialmente. El código explícito es preferible mientras el equipo aprende Java y Spring.

## 10. Frontend

El frontend consume la API por HTTP/JSON.

TanStack Query administra server state. No duplicar responses de la API en stores globales sin necesidad.

React Router maneja client-side routing.

No colocar secretos ni access tokens privados de Mercado Pago en el frontend.

Toda operación sensible se inicia o verifica desde el backend.

## 11. Errores

No usar `return null` para errores de dominio.

Crear excepciones con nombres concretos:

```text
SocioNoEncontradoException
HorarioNoDisponibleException
ReservaNoCancelableException
```

El mapeo de excepciones de dominio a HTTP vive en Boundary, por ejemplo mediante `@RestControllerAdvice`.

## 12. Seguridad del repositorio

Nunca commitear:

```text
.env
.env.*
application-local.properties con secretos
credenciales de PostgreSQL remotas
tokens de Mercado Pago
tokens de ngrok
claves privadas
```

Las credenciales locales de Docker pueden usar valores de desarrollo sin valor fuera de la máquina.

Los secretos reales se leen desde variables de entorno.

## 13. Dependencias

No agregar una librería sin identificar qué problema resuelve.

Evitar dependencias que dupliquen una capacidad ya cubierta por Spring Boot o el JDK.

No agregar arquitecturas o frameworks por anticipado.

Para un cambio grande de stack o estructura, crear primero un ADR en `docs/adr/`.

## 14. Regla de simplicidad

Este es un trabajo universitario.

Preferir una solución clara que el equipo pueda explicar, testear y mantener.

No introducir microservicios, CQRS, event sourcing, Kubernetes, módulos Maven múltiples ni abstracciones de infraestructura sin que un requisito real las justifique.
