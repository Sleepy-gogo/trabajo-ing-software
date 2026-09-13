# SERA web

Frontend React, TypeScript y Vite. React Router maneja las rutas y TanStack Query administra sesión, perfil y usuarios mediante el proxy `/api` de Vite a `http://localhost:4500`.

## Desarrollo

Usar Node.js 22.13 o superior dentro de la rama 22, o Node.js 24 o superior, y pnpm 11.18.0.

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Abrir <http://localhost:5173> e iniciar sesión o registrar una cuenta. Ver [desarrollo local](../../docs/DEVELOPMENT.md) para iniciar backend y PostgreSQL, y [usuarios](../../docs/USUARIOS.md) para crear el primer administrador local. Las secciones de incrementos posteriores siguen en demostración.

## Checks

```bash
pnpm format
pnpm format-check
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

`typecheck` comprueba tanto la aplicación como la configuración de Vite. `build` ejecuta los tests antes de generar `dist/`, por lo que el job de CI existente también los verifica.

## Componentes y estilos

El proyecto se inicializó con shadcn/ui y el preset `b1Z5ezsQa`: Base Vega, base neutral, acento sky, fuente Raleway y Lucide. La configuración está en `components.json` y los tokens en `src/index.css`.

Agregar componentes desde este directorio:

```bash
pnpm exec shadcn add card
```

Los componentes quedan en `src/components/ui`. Mantener el preset al agregar nuevos componentes.

`pnpm-workspace.yaml` solo declara los permisos de scripts de instalación de pnpm. Este frontend es un único paquete; el repositorio no usa un orquestador de monorepo.
