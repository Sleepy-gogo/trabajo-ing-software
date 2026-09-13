# Usuarios

Contrato del Incremento 1: registro, sesión, perfil y administración de cuentas. Conserva el modelo de usuarios ya integrado en `main`.

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

## Sesión y perfil

| Método | Ruta | Permiso y resultado |
| --- | --- | --- |
| `GET` | `/api/auth/csrf` | Público. Entrega `token` y `headerName`; conserva la cookie de sesión |
| `POST` | `/api/auth/registro` | Público con CSRF. Recibe `nombreCompleto`, `email`, `dni`, `password`; crea siempre `USUARIO` y responde `201` |
| `POST` | `/api/auth/login` | Público con CSRF. Recibe `email`, `password`; devuelve el usuario y una nueva sesión |
| `POST` | `/api/auth/logout` | Sesión y CSRF. Invalida la sesión, responde `204` |
| `GET` | `/api/usuarios/me` | Sesión. Devuelve el usuario actual |
| `PUT` | `/api/usuarios/me` | Sesión y CSRF. Recibe solo `nombreCompleto`, `email`, `dni` |

Todos los endpoints administrativos de `/api/usuarios` exigen `ADMIN`. `STAFF` y `USUARIO` pueden consultar y editar su propio perfil. El CRUD de referencia de espacios también exige `ADMIN`; los permisos de las futuras consultas públicas se definirán en Incremento 3.

La sesión vive en el servidor, vence tras 30 minutos de inactividad y usa cookie HttpOnly/SameSite=Lax. El login invalida la sesión anterior. Cada request vuelve a consultar el rol y estado: una baja o deshabilitación invalida la sesión existente y un cambio de rol rige en la siguiente operación. Reiniciar la API requiere iniciar sesión de nuevo. En HTTPS, configurar `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.

El cliente obtiene un token CSRF antes de cada escritura y envía el header indicado. La API conserva la protección CSRF de Spring Security. No se guardan tokens ni contraseñas en localStorage. Ver [ADR de sesiones](adr/0001-sesiones-de-usuario.md).

Un login inválido o una cuenta no activa responde `401`. Las operaciones sin permiso o sin CSRF válido responden `403`. Las contraseñas tienen entre 8 y 72 caracteres y un máximo de 72 bytes UTF-8, límite de BCrypt.

## Primera cuenta administrativa local

Registrar una cuenta desde `/register`. Después, un integrante con acceso a la base local puede promover **esa cuenta** desde psql:

```bash
docker compose exec postgres psql -U sera -d sera
```

```sql
UPDATE usuarios SET rol = 'ADMIN' WHERE email = 'email-de-la-cuenta@ejemplo.com';
```

Reemplazar el email por el de la cuenta registrada. Luego iniciar sesión desde `/login`. No hay contraseñas predeterminadas, promoción automática del primer usuario ni endpoint público para elegir roles.

## Frontend y pruebas

`/login`, `/register`, `/app/profile`, `/admin/users` y `/admin/users/:id` consumen la API real mediante TanStack Query. La administración permite buscar, crear, editar, cambiar contraseña y dar de baja con confirmación. La recuperación de acceso remite a administración. Teléfono, relación UNSE y legajo no se recopilan porque el contrato persistente actual no los incluye.

Las pantallas de los incrementos siguientes conservan sus mocks y muestran un aviso de demostración. Las rutas administrativas exigen rol; el backend vuelve a comprobarlo independientemente del frontend.

La auditoría mínima de este incremento consiste en `created_at` y `updated_at`; la baja es lógica y el QR permanece estable. No se agrega un historial por responsable ni permisos granulares.

Ejecutar `mvnw verify` en `apps/api` y `pnpm test`, `pnpm lint`, `pnpm typecheck`, `pnpm build` en `apps/web`. Los tests de seguridad cubren CSRF, permisos, sesión, baja y protección del perfil. Los tests de interacción del frontend usan Vitest, Testing Library y jsdom; no sustituyen una revisión visual en navegador.
