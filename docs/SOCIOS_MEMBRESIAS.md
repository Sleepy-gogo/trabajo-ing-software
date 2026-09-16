# Socios y membresías: definición del Incremento 2

La rama se llama `feat/SERA-25-socios-membresias`, pero la tarea correspondiente en Linear es
`TRA-25: I2.1 — Niveles, tarifas y relación con UNSE`. Este documento también prepara las clases y
rutas de sus tareas hermanas para que el equipo pueda implementar sin volver a traducir CU-05 a
CU-09.

Las clases y rutas ya existen, pero todavía no implementan negocio ni persistencia. Mientras tanto,
los endpoints responden `501 Not Implemented`.

## Reparto según Linear

| Tarea | Responsabilidad preparada |
| --- | --- |
| `TRA-25` | Nivel de membresía, tarifa por relación UNSE, vigencia, persistence y consultas |
| `TRA-26` | Alta de socio, contratación y consulta del estado de membresía |
| `TRA-27` | Cambio de nivel o estado, baja y transiciones válidas |
| `TRA-28` | CRUD de niveles, filtros, validaciones, errores y tests |
| `TRA-29` | Reemplazo de mocks del frontend por estas rutas |

El padre `TRA-7` depende del Incremento 1. Por eso `Socio` referencia a `Usuario` por identificador y
deja el mapeo JPA pendiente hasta integrar el trabajo de usuarios.

## Alcance funcional

El incremento prepara estos casos de uso:

- consultar y administrar niveles de membresía y sus tarifas;
- registrar un socio desde administración;
- contratar una membresía;
- consultar el estado de un socio o una membresía;
- modificar la relación UNSE, el nivel y el estado de membresía;
- cancelar una membresía con confirmación previa en el cliente.

Los pagos, las cuotas mensuales, el pago recurrente y la reacción a webhooks pertenecen a los flujos
CU-10, CU-11 y CU-13. La membresía debe quedar `PENDIENTE_PAGO` hasta que el módulo de pagos informe
un resultado aprobado.

## Modelo propuesto

`Socio` complementa a `Usuario`. Guarda solo la relación con la UNSE y su verificación. Nombre, DNI,
email, contraseña, rol y estado de cuenta siguen perteneciendo a `Usuario`.

`NivelMembresia` describe el plan que puede contratarse. `TarifaMembresia` mantiene el precio del
nivel para una relación UNSE durante una vigencia. `Membresia` registra la contratación de un nivel
por un socio.

```text
Usuario 1 --- 0..1 Socio 1 --- 0..* Membresia * --- 1 NivelMembresia
                                                        |
                                                        +--- 1..* TarifaMembresia
```

Un nivel deshabilitado y una tarifa vencida deben conservarse para consultar membresías e importes
históricos. La regla de una sola membresía vigente debe resolverse en Control y reforzarse en
PostgreSQL si el modelo final lo permite.

## Rutas preparadas

| Método | Ruta | Dueño | Resultado esperado al implementar |
| --- | --- | --- | --- |
| `GET` | `/api/niveles-membresia?soloDisponibles=true` | `TRA-25` | Lista niveles y tarifas vigentes |
| `GET` | `/api/niveles-membresia/{id}` | `TRA-25` | Muestra tarifas, beneficios y condiciones |
| `POST` | `/api/niveles-membresia` | `TRA-28` | Crea un nivel con sus tarifas |
| `PUT` | `/api/niveles-membresia/{id}` | `TRA-28` | Actualiza el nivel sin perder historial |
| `DELETE` | `/api/niveles-membresia/{id}` | `TRA-28` | Deshabilita el nivel, no lo borra físicamente |
| `POST` | `/api/socios` | `TRA-26` | Crea socio y membresía pendiente en una transacción |
| `GET` | `/api/socios` | `TRA-28` | Busca y filtra socios |
| `GET` | `/api/socios/{id}` | `TRA-26` | Combina usuario, socio y membresía vigente |
| `PUT` | `/api/socios/{id}` | `TRA-27` | Actualiza datos administrativos y deja auditoría |
| `POST` | `/api/membresias` | `TRA-26` | Crea una contratación pendiente de pago |
| `GET` | `/api/membresias/{id}` | `TRA-26` | Devuelve el estado de la membresía |
| `POST` | `/api/membresias/{id}/cancelacion` | `TRA-27` | Cancela o registra la solicitud de cancelación |

No se agregó `DELETE /api/membresias/{id}`: cancelar una membresía cambia su estado y conserva el
historial. Los cambios administrativos de nivel y estado pasan por `PUT /api/socios/{id}` para
guardar el agregado y su auditoría en una sola transacción.

## Orden sugerido de implementación

1. Integrar o esperar el modelo de `Usuario` del Incremento 1.
2. Confirmar la lista de relaciones UNSE y las reglas de vigencia de tarifas.
3. Implementar `TRA-25`: mapeos JPA, migraciones y consultas de nivel y tarifa. Elegir el número de
   migración después de integrar usuarios para evitar una colisión.
4. Implementar `TRA-26`: socio, membresía, alta y consulta de estado.
5. Implementar `TRA-27`: cambios y transiciones, con motivo, responsable y fecha.
6. Implementar `TRA-28`: CRUD de niveles, validaciones, errores y tests.
7. Implementar `TRA-29`: adaptar los DTO del frontend y reemplazar mocks con TanStack Query.

Cada paso debe completar las anotaciones y comportamiento de sus entidades, extender los
repositories necesarios con `JpaRepository`, inyectar los services por constructor y reemplazar las
respuestas `501` de sus rutas.

## Decisiones que todavía necesita el equipo

- Si `VISITANTE` forma parte de la relación UNSE o solo representa a una persona sin relación.
- Si la verificación UNSE pertenece a `Usuario` o al perfil `Socio`. El diseño provisional la deja en
  `Socio` porque el modelo actual de usuarios no la contiene.
- Cómo se detectan vigencias superpuestas y si cada nivel necesita una tarifa base además de las
  tarifas por relación.
- Qué combinaciones de membresías se consideran incompatibles.
- Qué estados permiten cancelación y desde qué fecha se hace efectiva.
- Cómo se guardan beneficios y condiciones. Una tabla hija permite consultarlos; JSON simplifica el
  prototipo, pero debe acordarse antes de crear la migración.
- Cómo serializar los enums para el frontend. El prototipo usa valores en minúscula como `activa` y
  `no_docente`; los enums Java usan constantes en mayúscula.
- Qué actor autenticado se registra como responsable de una modificación. La identidad depende del
  trabajo de autenticación.
