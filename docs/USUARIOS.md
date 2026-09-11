# Usuarios

Este documento describe el contrato implementado. No define autenticación ni permisos. Esos temas siguen pendientes de una decisión del equipo.

## Modelo

`Usuario` guarda estos datos:

| Campo | Regla |
| --- | --- |
| `id` | UUID generado por JPA con `GenerationType.UUID` |
| `nombreCompleto` | Obligatorio, hasta 200 caracteres |
| `email` | Obligatorio, hasta 100 caracteres, único sin distinguir mayúsculas |
| `dni` | Número entre 1 y 99.999.999, único |
| `rol` | `ADMIN`, `STAFF` o `USUARIO` |
| `qrUsuario` | Identificador único generado al crear la cuenta, con formato `SERA-U` más 12 caracteres CUID2 |
| `estadoCuenta` | `ACTIVO`, `DESHABILITADO` o `INACTIVO` |
| `passwordHash` | Hash BCrypt, nunca se devuelve por HTTP |
| `createdAt`, `updatedAt` | Auditoría administrada por Hibernate |

`RolUsuario` limita los roles generales del sistema. No modela permisos individuales ni agrega RBAC granular. El QR identifica al usuario y no puede editarse desde la API.

## API administrativa

| Método | Ruta | Resultado |
| --- | --- | --- |
| `POST` | `/api/usuarios` | Crea un usuario activo y responde `201` |
| `GET` | `/api/usuarios` | Lista usuarios ordenados por nombre |
| `GET` | `/api/usuarios?buscar=valor` | Busca por nombre o email parcial, o por DNI exacto |
| `GET` | `/api/usuarios/{id}` | Consulta un usuario |
| `PUT` | `/api/usuarios/{id}` | Actualiza datos, rol y estado |
| `PUT` | `/api/usuarios/{id}/password` | Cambia la contraseña y responde `204` |
| `DELETE` | `/api/usuarios/{id}` | Cambia el estado a `INACTIVO` y responde `204` |

La baja es lógica. El registro permanece disponible para auditoría y relaciones futuras.

## Errores

- Los DTOs rechazan campos vacíos, emails inválidos, tamaños fuera de rango y contraseñas de menos de 8 o más de 72 caracteres.
- Un email o DNI repetido responde `409` con el código `usuario_duplicado`.
- Un UUID inexistente responde `404` con el código común `recurso_no_encontrado`.

## Pendiente

Linear todavía describe login, usuario actual y autorización detallada en `TRA-20` y `TRA-21`. El equipo debe corregir esas tareas antes de implementarlas. Este módulo define tres roles generales, pero todavía no publica un endpoint de login ni aplica permisos.
