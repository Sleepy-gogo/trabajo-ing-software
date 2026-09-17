# Estado actual y próximos pasos

Este documento resume el código que existe al 17 de septiembre de 2026 y propone un orden de trabajo. No reemplaza `SPEC.md`. Su objetivo es que cada integrante pueda tomar un método o un test sin tener que rediseñar el flujo completo.

## Relación con Linear

Este trabajo adelanta una parte del Incremento 3 aunque el Incremento 2 todavía no empezó. El código actual se reparte así:

- `TRA-32`: el CRUD de espacios está encaminado, pero faltan los estados y la regla que excluye espacios no reservables.
- `TRA-31`: existe la franja semanal básica. Faltan bloqueos, excepciones y mantenimiento.
- `TRA-34`: el contrato HTTP y las funciones cliente están declarados. Falta conectar las pantallas.
- `TRA-30`: `tarifaHora` es una tarifa única provisional. La tarea requiere tarifas por espacio y tipo de usuario.
- `TRA-33`: todavía faltan el cálculo por fecha y las superposiciones.

Linear marca `TRA-8` como dependiente de `TRA-7`, el incremento de socios y membresías. Adelantar el CRUD y los horarios base es viable. La tarifa aplicable a una persona debe esperar al modelo de relación con UNSE y membresía para evitar duplicar ese dominio.

## Qué ya funciona

| Área | Backend | Frontend | Pruebas |
| --- | --- | --- | --- |
| Sesión y usuarios | Registro, login, perfil y administración | Conectado a la API | Entity, Control y HTTP |
| Espacios | CRUD, búsqueda simple y DTOs | Pantallas completas con mocks | Entity, Control y HTTP |
| Disponibilidad semanal | CRUD anidado por espacio | Pantallas con mocks | Entity, Control y HTTP |
| Reservas, pagos, membresías y accesos | Sin modelo backend en esta rama | Prototipo con mocks | Pendientes |

El frontend tiene una diferencia importante: usuarios ya consume datos reales, pero las pantallas de espacios, disponibilidad y reservas todavía importan datos desde `src/mocks`.

## Recorrido de controllers

Un controller solo traduce HTTP. No decide si un horario está libre ni accede a un repository.

```text
POST /api/espacios/{espacioId}/disponibilidades
        |
        v
CrearDisponibilidadRequest           Boundary valida JSON
        |
        v
DisponibilidadController             Boundary traduce HTTP
        |
        v
DisponibilidadService                Control ejecuta el caso de uso
        |
        +-> EspacioRepository         comprueba que el espacio existe
        +-> DisponibilidadRepository  comprueba duplicados y guarda
        |
        v
Disponibilidad                       Entity valida el rango horario
        |
        v
DisponibilidadResponse               Boundary devuelve JSON
```

Los endpoints declarados son:

| Método | Ruta | Método de Control |
| --- | --- | --- |
| `GET` | `/api/espacios?buscar={texto}` | `EspacioService.listar(criterio)` |
| `POST` | `/api/espacios` | `EspacioService.registrarEspacio(...)` |
| `GET` | `/api/espacios/{id}` | `EspacioService.consultarDetalle(id)` |
| `PUT` | `/api/espacios/{id}` | `EspacioService.actualizar(...)` |
| `DELETE` | `/api/espacios/{id}` | `EspacioService.eliminar(id)` |
| `GET` | `/api/espacios/{id}/disponibilidades` | `DisponibilidadService.listarPorEspacio(id)` |
| `POST` | `/api/espacios/{id}/disponibilidades` | `DisponibilidadService.registrarDisponibilidad(...)` |
| `PUT` | `/api/espacios/{id}/disponibilidades/{disponibilidadId}` | `DisponibilidadService.actualizar(...)` |
| `DELETE` | `/api/espacios/{id}/disponibilidades/{disponibilidadId}` | `DisponibilidadService.eliminar(...)` |

Las funciones equivalentes del frontend están declaradas en `src/lib/spaces-api.ts`. Las pantallas todavía no las llaman.

## Orden recomendado para el wiring del frontend

### 1. Lectura de espacios

- Crear `useSpaces(search)` con TanStack Query y `spacesApi.list`.
- Crear `useSpace(id)` con `spacesApi.get`.
- Escribir un adaptador explícito entre `SpaceResponse` y el tipo visual `Space`.
- Reemplazar primero los mocks en la lista de administración.
- Reutilizar la misma query en la lista para socios.

El adaptador es necesario porque el prototipo visual tiene campos que el backend todavía no modela, como estado, último mantenimiento, cantidad de usos, varias imágenes y precios por categoría. No conviene inventar esos valores dentro del cliente. La pantalla debe ocultarlos o marcarlos como pendientes hasta que exista el contrato correspondiente.

### 2. Alta y edición de espacios

- Crear mutations para `spacesApi.save` y `spacesApi.remove`.
- Invalidar `['spaces']` después de guardar o eliminar.
- Mostrar los errores de `ApiError.fields` junto a cada campo.
- Sustituir el alta local de `AdminSpacesPage`, que hoy crea IDs `space-demo-*`.

### 3. Disponibilidad semanal

- Crear una query con clave `['spaces', spaceId, 'availability']`.
- Conectar alta, edición y eliminación con las funciones ya declaradas.
- Invalidar el detalle del espacio y la query de disponibilidad después de cada cambio.
- Permitir varias franjas por día y mostrar el conflicto `409` cuando una franja se superpone con otra.

### 4. Horarios reservables

Este paso queda bloqueado hasta definir:

- duración mínima y múltiplos permitidos;
- zona horaria del complejo;
- cómo se representan bloqueos y mantenimiento;
- qué estados de reserva ocupan un horario;
- si un día admite una sola franja o varias franjas separadas.

La configuración de bloqueos y el cálculo base por fecha pueden avanzar en `TRA-31` y `TRA-33`. La resta de reservas existentes deberá completarse cuando exista el modelo de reservas. El caso de uso final debe combinar disponibilidad semanal, bloqueos y reservas. No debe implementarse en el controller ni calcularse restando mocks en React.

## Trabajo backend pendiente

- Definir y modelar el estado del espacio.
- Definir categorías de espacio. Hoy `tipo` es texto libre.
- Decidir si `tarifaHora` alcanza o si se necesitan tarifas por relación con UNSE o membresía.
- Definir almacenamiento de imágenes. `rutaImagen` solo admite una cadena.
- Definir qué ocurre al eliminar un espacio con reservas existentes.
- Traducir colisiones concurrentes de los índices únicos a respuestas `409`.
- Definir protección concurrente para franjas superpuestas. La validación actual del service no reemplaza una restricción de base de datos.
- Agregar tests de integración de persistence para las restricciones de nombre y disponibilidad.
- Crear el modelo de reservas antes de declarar endpoints de pago o cálculo de horarios libres.

## Cómo avanzar de a una prueba

Para un cambio de Control:

1. agregar un caso a `EspacioServiceTest` o `DisponibilidadServiceTest`;
2. ejecutar solo esa clase;
3. implementar el método;
4. ejecutar toda la suite backend.

```powershell
cd apps/api
.\mvnw.cmd -Dtest=DisponibilidadServiceTest test
.\mvnw.cmd test
```

Para un cambio de Boundary, usar `EspacioControllerTest` o `DisponibilidadControllerTest`. Esos tests muestran el JSON, la ruta, el status esperado y la llamada exacta al service.

Para el wiring web, probar primero el adaptador y después la pantalla. No copiar responses a un store global. TanStack Query debe conservar el server state.

```powershell
cd apps/web
pnpm test
pnpm typecheck
pnpm lint
```
