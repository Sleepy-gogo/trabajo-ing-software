# Sesiones de usuario para Incremento 1

Estado: implementado como parte del cierre de TRA-6 y TRA-20 a TRA-24.

## Contexto

El modelo de usuarios ya persistía roles, estado, BCrypt y timestamps. Faltaban login, usuario actual y autorización. React consume la API a través de `/api` en el mismo origen, usando el proxy de Vite durante desarrollo.

## Decisión

Usar sesiones HTTP del servidor y Spring Security para autorización y CSRF. El navegador conserva una cookie HttpOnly; la sesión guarda únicamente el UUID de la cuenta. Un filtro Boundary consulta a Control para obtener el rol y estado vigentes en cada request y construye la identidad usada por Spring Security. No se persiste una copia del rol en la sesión.

El registro público crea cuentas `USUARIO`. Solo `ADMIN` administra cuentas. `STAFF` y `USUARIO` editan su propio perfil, sin cambiar rol ni estado. El primer administrador local se promueve explícitamente desde PostgreSQL.

## Consecuencias

No hay JWT, refresh tokens ni almacenamiento de credenciales en el navegador. Un cambio administrativo de estado o rol rige en la siguiente request. La API consulta PostgreSQL para cada request con sesión y las sesiones se pierden al reiniciar el servidor. Estas restricciones son aceptables para el monorepo universitario y un único backend.

En despliegue, servir web y `/api` bajo el mismo origen y usar HTTPS con cookie Secure. Si se requiere otro esquema de despliegue, revisar explícitamente cookies y CORS. Se conserva CSRF también para registro y login. La recuperación por email y las sesiones persistentes quedan fuera de este incremento.
