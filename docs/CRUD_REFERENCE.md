# CRUD vertical de referencia

La funcionalidad `espacio` muestra una operación desde HTTP hasta PostgreSQL. Cubre nombre, descripción, capacidad, tarifa por hora, tipo, una ruta de imagen y disponibilidad semanal. El estado operativo, varias imágenes y las tarifas por categoría siguen pendientes.

## Recorrido

```text
POST /api/espacios
        |
        v
GuardarEspacioRequest       boundary
        |
        v
EspacioController           boundary
        |
        v
EspacioService              control
        |
        +-> Espacio          entity
        |
        v
EspacioRepository           persistence
        |
        v
PostgreSQL
```

Spring valida el request con `@Valid` y el controller traduce HTTP. El service abre la transacción y coordina el caso de uso. La entidad conserva su estado y normaliza sus textos. El repository delega el acceso a datos en Spring Data JPA. Control devuelve `EspacioDetalle` y Boundary lo convierte en `EspacioResponse`. La entidad JPA no cruza la API.

## Estructura de referencia

```text
espacio/
├── boundary/
│   ├── dto/
│   │   ├── EspacioResponse.java
│   │   └── GuardarEspacioRequest.java
│   └── EspacioController.java
├── control/
│   ├── EspacioDetalle.java
│   ├── EspacioNoEncontradoException.java
│   └── EspacioService.java
├── entity/
│   └── Espacio.java
└── persistence/
    └── EspacioRepository.java
```

También intervienen la migración `V1__create_espacios.sql` y el manejo común de errores bajo `shared/boundary`.

## Contrato HTTP

| Método | Ruta | Resultado |
| --- | --- | --- |
| `POST` | `/api/espacios` | Crea y responde `201` con `Location` |
| `GET` | `/api/espacios` | Lista espacios |
| `GET` | `/api/espacios/{id}` | Devuelve uno o responde `404` |
| `PUT` | `/api/espacios/{id}` | Reemplaza nombre y descripción |
| `DELETE` | `/api/espacios/{id}` | Elimina y responde `204` |

Ejemplo de alta:

```http
POST /api/espacios
Content-Type: application/json

{
  "nombre": "Cancha cubierta",
  "descripcion": "Piso de parquet",
  "capacidad": 20,
  "tarifaHora": 1500.00,
  "tipo": "FUTSAL",
  "rutaImagen": null
}
```

`nombre` y `tipo` son obligatorios y admiten hasta 100 caracteres. `descripcion` es opcional y admite hasta 500. `capacidad` debe ser mayor que cero, `tarifaHora` no puede ser negativa y `rutaImagen` admite hasta 255 caracteres.

## Cómo repetir el patrón

1. Crear el package de la feature con `boundary`, `control`, `entity` y `persistence`.
2. Definir la entidad y una migración Flyway nueva. Los nombres, tipos y restricciones deben coincidir.
3. Crear el repository de Spring Data JPA.
4. Implementar el caso de uso en un service. La transacción empieza allí.
5. Crear request y response DTOs. El controller no recibe ni devuelve la entidad.
6. Exponer los endpoints desde el controller.
7. Probar una regla del service y el contrato HTTP principal. Agregar más pruebas sólo cuando cubran una decisión real.

## Probarlo

Desde la raíz del repositorio:

```powershell
git clone git@github.com:Sleepy-gogo/trabajo-ing-software.git
cd trabajo-ing-software
docker compose up -d
cd apps/api
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
$espacio = Invoke-RestMethod -Method Post -Uri http://localhost:4500/api/espacios `
  -ContentType "application/json" `
  -Body '{"nombre":"Cancha cubierta","descripcion":"Piso de parquet","capacidad":20,"tarifaHora":1500.00,"tipo":"FUTSAL","rutaImagen":null}'

Invoke-RestMethod http://localhost:4500/api/espacios

Invoke-RestMethod -Method Put -Uri "http://localhost:4500/api/espacios/$($espacio.id)" `
  -ContentType "application/json" `
  -Body '{"nombre":"Cancha norte","descripcion":"Piso renovado","capacidad":20,"tarifaHora":1800.00,"tipo":"FUTSAL","rutaImagen":null}'

Invoke-RestMethod -Method Delete -Uri "http://localhost:4500/api/espacios/$($espacio.id)"
```

Antes de abrir el PR:

```powershell
cd apps/api
.\mvnw.cmd spotless:apply
.\mvnw.cmd verify
```
