# Adelanto hasta el incremento 3

## Alcance demostrable

- Registro, login, perfil y administración de usuarios con sesión y CSRF.
- Creación automática del socio junto al usuario, sin membresía, con verificación UNSE pendiente.
- Niveles, beneficios, precios por relación y baja lógica de niveles.
- Consulta de socios, filtros, verificación manual y auditoría.
- Contratación pendiente, cancelación inmediata y nueva solicitud sobre la misma membresía.
- Espacios con imagen, capacidad, estado y tarifas.
- Horarios semanales sin superposiciones, bloqueos por fecha y consulta de franjas disponibles.
- Cálculo de la tarifa aplicable en backend y permisos administrativos.

Las pantallas de pagos, reservas, accesos, reportes, encuestas y configuración general conservan
sus prototipos para los siguientes incrementos. Tienen un aviso visible de datos de ejemplo. Los
inicios de administrador y usuario muestran datos reales. El QR personal identifica la cuenta;
todavía no autoriza el acceso.

## Iniciar

Desde la raíz:

```powershell
docker compose up -d
```

En una terminal:

```powershell
cd apps/api
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

En otra:

```powershell
cd apps/web
pnpm install --frozen-lockfile
pnpm dev
```

Abrir `http://localhost:5173`. La API usa `4500` y PostgreSQL `5432`.

`clean` evita arrastrar migraciones compiladas de ramas anteriores. Las migraciones consolidadas
`V1` a `V4` no se modificaron. Se conservaron `V5` a `V7` de socios/membresías; las de la rama de
espacios se integraron como `V8` y `V9` para evitar números repetidos.

## Datos de demostración

Con la API y Docker activos, desde la raíz:

```powershell
python scripts/demo.py
```

El script pide una contraseña para las cuentas locales `admin@sera.local` y `socio@sera.local`.
Crea un nivel y tres espacios con horarios semanales. No borra los datos existentes. Se puede
repetir con la misma contraseña. Si esas cuentas ya existen con otra contraseña, el script falla
sin sobrescribirla. Promueve únicamente la cuenta ficticia `admin@sera.local` mediante Docker local.

Para la base aislada usada durante la revisión, ejecutar la API con
`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sera_entrega_i3`, un perfil distinto de dev,
las credenciales locales `sera` y `--database sera_entrega_i3` en el script. La base debe existir.

## Recorrido sugerido

1. Entrar como administrador y abrir el inicio. Mostrar los conteos reales.
2. Crear un usuario en Usuarios. Encontrarlo en Socios sin registrar un socio por separado.
3. Verificar manualmente su relación UNSE y mostrar el historial.
4. Abrir Niveles y precios. Editar un importe o un beneficio.
5. Entrar con la cuenta de socio. Solicitar una membresía y mostrar `Pendiente pago`.
6. Cancelarla con motivo. Volver a solicitarla; se conserva el identificador de membresía.
7. Como administrador, abrir un espacio y configurar un horario y un bloqueo para una fecha futura.
8. Como socio, consultar esa fecha. Mostrar las franjas libres y la tarifa calculada por backend.
9. Poner el espacio en mantenimiento y volver a consultar: no ofrece franjas disponibles.

La aprobación del pago, las reservas confirmadas y el control de acceso pertenecen a incrementos
posteriores. No deben presentarse como terminados.

## Verificaciones

```powershell
cd apps/api
.\mvnw.cmd clean verify
cd ../web
pnpm lint
pnpm format-check
pnpm build
cd ../..
docker compose config
```

`build` incluye TypeScript, Vitest y Vite. `verify` incluye tests, empaquetado, Spotless y Checkstyle.

`scripts/smoke_i3.py` prueba la API real y deja registros ficticios con el prefijo `Prueba I3`.
Requiere datos de demo y `SERA_DEMO_PASSWORD` en la terminal. Ejecutar con `python scripts/smoke_i3.py`.
Comprueba permisos, transacciones, consultas SQL, contratación/cancelación, auditoría, tarifas,
bloqueos y dos altas concurrentes del mismo horario.

La revisión automatizada de esta entrega no contó con un navegador conectado. Queda pendiente
la inspección visual manual en escritorio y móvil antes de exponer.
