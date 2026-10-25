# Garantía de calidad del acceso a SERA

Fecha de ejecución: 25/10/2026.

## Propósito y alcance

Este trabajo comprueba la entrada del usuario al sistema: registro, autenticación,
sesión HTTP, perfil y permisos de administración. La clase principal es
`edu.unse.sera.usuario.control.UsuarioService`.

El PDF del taller pide diseñar y ejecutar pruebas sobre una clase que controle la
entrada del usuario y elaborar un informe. En el adjunto son las actividades 3
y 4. El ingreso físico con QR, implementado en `acceso`, pertenece a otro caso
de uso.

El umbral del 100% comprende estas clases:

- Todas las de `usuario.control`, incluidos sus resultados y excepciones.
- Todas las de `usuario.boundary`, incluidos los DTO.
- `shared.config.SecurityConfig` y `shared.config.PasswordConfig`.

El informe de JaCoCo muestra todo el backend. Las entidades JPA, los repositories,
el frontend y los demás módulos quedan fuera del umbral específico de esta
actividad. No se excluyen clases del informe general ni se desactivan validaciones
para alcanzar el porcentaje.

## Plan de pruebas

Se mantienen los productos de trabajo en Git: código, tests, configuración de
Maven, workflow de GitHub Actions y este informe. Los estándares de implementación
son los definidos en `REPOSITORY_RULES.md` y la organización BCE de
`ARCHITECTURE.md`.

Las pruebas unitarias de Control usan JUnit y Mockito para fijar los datos del
repositorio y los resultados del encoder. Una prueba adicional usa BCrypt real
para comprobar el registro del hash, la comparación y el cambio de contraseña.
Las pruebas HTTP usan MockMvc, la configuración real de seguridad y servicios
simulados. Esto permite comprobar DTO, códigos HTTP, CSRF, sesiones y roles sin
necesitar PostgreSQL.

Se acepta la entrega cuando pasan todos los tests del backend, Spotless,
Checkstyle, el umbral de cobertura y los checks del frontend. Un fallo se corrige
y se repite la comprobación afectada. Antes de cerrar el cambio se ejecuta la
verificación completa desde un directorio `target` limpio.

## Herramientas

- Java 21 y Maven Wrapper del repositorio.
- JUnit, AssertJ, Mockito, Spring MVC Test y Spring Security Test existentes.
- JaCoCo 0.8.15, incorporado como plugin de Maven.
- Spotless y Checkstyle para formato y convenciones Java.
- ESLint, TypeScript, Vitest, Prettier y Vite para la web.

La consigna menciona TikiOne JaCoCoverage para NetBeans. Aquí se usa JaCoCo desde
Maven para medir y verificar la cobertura con el mismo comando en Windows, Linux
y CI, sin depender de un IDE.

## Diseño de los casos

Cada método de test contiene las entradas, el resultado esperado y las aserciones.
Los casos parametrizados ejecutan una prueba por cada valor indicado.

| Caso | Entrada o situación | Resultado esperado | Tests |
| --- | --- | --- | --- |
| Registro | Nombre y email con espacios y mayúsculas | Datos normalizados, cuenta activa, hash y socio asociados | `UsuarioServiceTest.registraUnUsuarioActivoConPasswordHasheado` |
| Relación UNSE | Cada relación y relación ausente | Se conserva la declarada; se usa EXTERNO si falta | `registraLaRelacionDeclaradaYUsaExternoSiFalta` |
| Duplicados | Email o DNI en uso al registrar o editar | Error concreto; no se modifica el usuario ni se genera un hash innecesario | `rechazaUnEmailDuplicadoAntesDeHashearElPassword`, `rechazaDniDuplicadoAlRegistrar`, `rechazaDuplicadosAlEditarSinModificarElUsuario` |
| Colisión al guardar | Restricciones únicas de email o DNI | Excepción de duplicado con causa original | `traduceUnaColisionConcurrenteDeEmailAlRegistrar`, `traduceUnaColisionConcurrenteDeDniAlActualizar` |
| Otra restricción | Error sin constraint, nombre desconocido o nulo | Se conserva el error de integridad original | `noDisfrazaOtraViolacionDeIntegridadComoDuplicado`, `conservaLaExcepcionDeUnaRestriccionDesconocida` |
| Autenticación | Email ausente, contraseña incorrecta o cuenta inactiva | No se autentica | `autenticarRechazaEmailInexistenteSinCompararPassword`, `autenticarRechazaPasswordIncorrectoYCuentaInactiva` |
| Límite BCrypt | 72 bytes y más de 72 bytes en UTF-8 | Se aceptan 72; el exceso impide autenticar o cambiar la contraseña | `cambiaPasswordConElLimiteExactoDeBcryptSinGuardarTextoPlano`, `rechazaPasswordQueExcedeLimiteUtf8DeBcrypt`, `noActualizaUnaPasswordQueSupera72Bytes` |
| Hash real | Contraseña válida, incorrecta y cambio de contraseña | El hash no es el texto plano; la contraseña anterior deja de autenticar | `UsuarioAutenticacionTest.autenticaConElHashRealYRechazaLaPasswordAnteriorTrasCambiarla` |
| Consulta de sesión | Cuenta activa, deshabilitada, inactiva o inexistente | Solo se devuelve la cuenta activa | `consultaSesionSoloParaUnaCuentaActiva`, `consultaDeSesionInexistenteDevuelveVacio` |
| Búsqueda | Criterio nulo, vacío, nombre, DNI existente, inexistente y número fuera de rango | Listado completo, coincidencia por DNI o búsqueda de texto según corresponda | `listaTodosCuandoNoHayBusqueda`, `buscaUnUsuarioPorDniExacto`, `buscaPorTextoSiNoEncuentraUnDniExacto` |
| Perfil y baja | Cambio de datos o baja de un usuario | Perfil conserva rol, estado y QR; baja sin borrar historial | `perfilConservaRolEstadoYQr`, `actualizaYFuerzaLaAuditoriaAntesDeResponder`, `daDeBajaSinEliminarElRegistro` |
| Registro HTTP | Rol ADMIN enviado en el registro público | Se registra USUARIO, incluso con relación UNSE declarada | `AuthControllerTest.registroPublicoNoPuedeElegirRol`, `registroPublicoConservaRelacionUnseSinPermitirRolAdministrativo` |
| Datos HTTP inválidos | Campos inválidos, rol desconocido o contraseña demasiado larga en bytes | HTTP 400 con error de validación | `UsuarioControllerTest.rechazaDatosInvalidos`, `rechazaUnRolFueraDelEnum`, `passwordQueSuperaElLimiteEnBytesResponde400`, `AuthControllerTest.rechazaLoginInvalidoAntesDeAutenticar` |
| Credenciales HTTP | Email y contraseña que no autentican | HTTP 401 sin iniciar sesión | `AuthControllerTest.loginRechazaCredencialesInvalidas` |
| CSRF | Consulta del token y registro sin token | Token disponible antes del login; escritura sin token devuelve HTTP 403 | `obtieneTokenCsrfAntesDeIniciarSesion`, `exigeCsrfEnRegistro` |
| Sesión | Login con o sin sesión previa y logout | Sesión previa invalidada, nueva identidad registrada y sesión cerrada inutilizable | `loginRotaSesionYLogoutImpideReutilizarla`, `loginSinSesionAnteriorCreaUnaNuevaConLaIdentidadAutenticada`, `logoutSinSesionEsIdempotente` |
| Sesión inválida | Petición anónima, identidad de formato incorrecto o cuenta dada de baja | HTTP 401; la baja invalida la sesión | `exigeSesionParaConsultarUsuarios`, `unaSesionSinIdentidadValidaNoAutoriza`, `administradorPuedeListarYLaBajaRevocaSesion` |
| Permisos | Usuario o staff intentan administrar; administrador pasa a usuario | HTTP 403; el cambio de rol rige en la siguiente petición | `usuarioYStaffNoPuedenAdministrar`, `cambioDeRolSeAplicaEnLaSiguientePeticion` |
| Identidad del perfil | Cliente envía otro ID, rol o estado | Se usa la identidad de sesión y se conservan sus permisos | `perfilUsaIdentidadDeSesionEIgnoraRolYEstadoDelCliente` |
| Administración HTTP | Consulta, edición y cambio de contraseña como administrador | HTTP 200 o 204, sin contraseñas en respuestas; usuario ausente devuelve 404 | `UsuarioControllerTest.consultaUnUsuarioSinExponerCredenciales`, `actualizaLosDatosDelUsuario`, `cambiaLaPasswordSinDevolverla`, `consultaInexistenteResponde404` |

Salvo donde se indica otra clase, los primeros once casos corresponden a
`UsuarioServiceTest`. Los tests están en `apps/api/src/test/java/edu/unse/sera/usuario`.

## Ejecución reproducible

Desde `apps/api`, en Windows:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
```

En Linux o macOS se usa `./mvnw`. La ejecución completa no necesita Docker:
los repositories de estas pruebas están simulados.

Para ejecutar solo las pruebas del alcance, sin exigir cobertura del resto:

```powershell
.\mvnw.cmd clean test "-Dtest=UsuarioServiceTest,UsuarioAutenticacionTest,AuthControllerTest,UsuarioControllerTest"
```

Ambos comandos generan `target/site/jacoco/index.html`, `jacoco.xml` y
`jacoco.csv`. El informe de ejecución de JUnit queda en
`target/surefire-reports/`. Los archivos generados permanecen fuera de Git.
GitHub Actions ejecuta `verify` y conserva ambos informes en el artefacto
`backend-coverage` durante 30 días.

Desde `apps/web`:

```powershell
pnpm lint
pnpm format-check
pnpm build
```

`build` incluye TypeScript, los tests de Vitest y el empaquetado de Vite.
Vitest usa dos workers, 15 segundos por caso y cinco segundos para las consultas
asíncronas de Testing Library. Se ajustaron esos límites después de observar
timeouts en las comprobaciones simultáneas de frontend y backend.

## Resultados

La revisión inicial `7be508c` tenía el árbol de trabajo limpio y 144 tests de
backend aprobados. JaCoCo midió 1970 de 2875 líneas del backend, 68,52%, y 651 de
1217 ramas, 53,49%. En `UsuarioService` se cubrían 91 de 107 líneas, 85,05%,
y 27 de 34 ramas, 79,41%.

La medición inicial se obtuvo antes de modificar el código, con:

```powershell
.\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.15:report
```

La ejecución final de `clean verify` aprobó los 182 tests del backend, sin fallos,
errores ni casos omitidos. Los cuatro archivos de tests de Control y Boundary del
alcance reúnen 66 casos ejecutados, incluidos los parametrizados.

| Clase o grupo | Líneas cubiertas/total | Ramas cubiertas/total | Instrucciones cubiertas/total |
| --- | --- | --- | --- |
| `UsuarioService` | 98/98 | 30/30 | 392/392 |
| `AuthController` | 33/33 | 8/8 | 112/112 |
| `SesionFilter` | 17/17 | 6/6 | 58/58 |
| `UsuarioController` | 33/33 | Sin ramas | 99/99 |
| `UsuarioExceptionHandler` | 9/9 | Sin ramas | 33/33 |
| `SecurityConfig` | 67/67 | Sin ramas | 363/363 |
| `PasswordConfig` | 2/2 | Sin ramas | 7/7 |
| DTO y demás clases de Control | 29/29 | Sin ramas | 196/196 |
| Total del alcance, 18 clases | 288/288, 100% | 44/44, 100% | 1260/1260, 100% |

"Sin ramas" indica que JaCoCo no cuenta decisiones condicionales en esa clase.
En particular, ejecutar la configuración de seguridad registra las reglas; sus
resultados HTTP se verifican mediante los casos de permisos y sesión.

La cobertura general del backend quedó en 1994 de 2857 líneas, 69,79%, y
659 de 1213 ramas, 54,33%. La reducción de líneas totales respecto de la revisión
inicial corresponde a la simplificación del código. El 100% se aplica únicamente
al alcance definido al comienzo de este informe.

Comprobaciones finales ejecutadas:

- `.\mvnw.cmd clean verify`: 182 tests aprobados, umbral JaCoCo aprobado,
  empaquetado correcto, Spotless aprobado y cero infracciones de Checkstyle.
- `pnpm lint` y `pnpm format-check`: aprobados.
- `pnpm build`: TypeScript aprobado, 43 tests de Vitest aprobados y build de Vite
  generado correctamente.
- `pnpm exec prettier --check ../../.github/workflows/checks.yml`: aprobado.
- `docker compose config --quiet` y `git diff --check`: aprobados.

También se ejecutó intencionalmente `verify` con solo
`-Dtest=UsuarioAutenticacionTest`, antes de la medición final limpia. JaCoCo
rechazó esa suite incompleta por líneas y ramas sin cubrir. La comprobación final
se ejecutó con la suite completa y el umbral obligatorio habilitado.

También se comprobó el arranque del JAR con el perfil `calidad`, en el puerto
4510, contra una base local vacía `sera_calidad_20261025_141221`. Flyway aplicó
`V1__esquema_inicial.sql` y Hibernate validó el modelo con `ddl-auto=validate`.
El recorrido HTTP usó `scripts.demo.Client` con cookies y tokens CSRF reales.
Pasaron registro, duplicados, credenciales inválidas y válidas, perfil, rechazo
de escritura sin CSRF, permisos, cambio de rol, cambio de contraseña, logout y
baja. La preparación del rol administrativo se hizo por SQL únicamente sobre
esa base de prueba. Al terminar se detuvo la API de prueba y se eliminó esa base.

## Revisión del código

Se eliminó `validarCredenciales`, que duplicaba la autenticación y solo tenía
llamadas desde un test. `autenticar` concentra ahora ese comportamiento y su
límite de contraseña. El parseo de DNI quedó privado, el controller reutiliza
`UsuarioResponse.from` y la traducción de conflictos recorre las causas en un
solo método. Los imports reemplazan nombres de clases escritos completos.

En la pantalla de acceso se quitaron comentarios sobre ediciones anteriores,
un contenedor vacío y una condición de teléfono que no correspondía a ningún
campo. Se conservaron los comentarios que explican decisiones de seguridad o
concurrencia en el resto del código.

La cobertura de ramas registra las salidas de las decisiones que ejecutó la
suite. El 100% del alcance no demuestra ausencia de errores ni equivale a probar
todas las combinaciones de datos. Las pruebas con repositories simulados tampoco
comprueban por sí solas consultas, restricciones o concurrencia en PostgreSQL.

## Referencias

- Consigna: *Taller - plan de garantía de calidad - Ingeniería de Software, 2022*,
  páginas impresas 7 y 8, actividades y criterios de evaluación.
- [JaCoCo para Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html).
- [Verificación de cobertura con JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html).
- [Reglas del repositorio](../REPOSITORY_RULES.md) y [arquitectura BCE](ARCHITECTURE.md).
