# Usuarios

Este documento describe el contrato implementado. No define autenticación ni permisos. Esos temas siguen pendientes de una decisión del equipo.

## Modelo

`Usuario` guarda estos datos:

| Campo | Regla |
| --- | --- |
| `id` | UUID generado por la aplicación |
| `nombreCompleto` | Obligatorio, hasta 200 caracteres |
| `email` | Obligatorio, hasta 100 caracteres, único sin distinguir mayúsculas |
| `dni` | Número entre 1 y 99.999.999, único |
| `rol` | Texto obligatorio, hasta 50 caracteres |
| `qrUsuario` | Texto obligatorio, hasta 100 caracteres |
| `estadoCuenta` | `ACTIVO`, `DESHABILITADO` o `INACTIVO` |
| `passwordHash` | Hash BCrypt, nunca se devuelve por HTTP |
| `createdAt`, `updatedAt` | Auditoría administrada por Hibernate |

`rol` y `qrUsuario` son atributos de texto. El módulo no contiene un catálogo de roles ni genera, rota o interpreta códigos QR.

## API administrativa

| Método | Ruta | Resultado |
| --- | --- | --- |
| `POST` | `/api/usuarios` | Crea un usuario activo y responde `201` |
| `GET` | `/api/usuarios` | Lista usuarios ordenados por nombre |
| `GET` | `/api/usuarios?buscar=valor` | Busca por nombre o email parcial, o por DNI exacto |
| `GET` | `/api/usuarios/{id}` | Consulta un usuario |
| `PUT` | `/api/usuarios/{id}` | Actualiza datos, rol, QR y estado |
| `PUT` | `/api/usuarios/{id}/password` | Cambia la contraseña y responde `204` |
| `DELETE` | `/api/usuarios/{id}` | Cambia el estado a `INACTIVO` y responde `204` |

La baja es lógica. El registro permanece disponible para auditoría y relaciones futuras.

## Errores

- Los DTOs rechazan campos vacíos, emails inválidos, tamaños fuera de rango y contraseñas de menos de 8 o más de 72 caracteres.
- Un email o DNI repetido responde `409` con el código `usuario_duplicado`.
- Un UUID inexistente responde `404` con el código común `recurso_no_encontrado`.

## Pendiente

Linear todavía describe login, usuario actual y autorización por rol en `TRA-20` y `TRA-21`. El equipo debe corregir esas tareas según la decisión de simplificar roles antes de implementarlas. Este módulo no publica un endpoint de login ni confía en un rol enviado por el cliente para autorizar acciones.
