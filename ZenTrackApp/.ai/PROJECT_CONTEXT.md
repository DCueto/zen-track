# PROJECT CONTEXT — ZenTrack

Visión general del proyecto para orientar al agente en tareas de alto nivel que abarcan múltiples módulos. Lee este archivo cuando la tarea requiera entender la arquitectura completa o el flujo de datos entre capas.

## Qué es ZenTrack

Plataforma minimalista de gestión de proyectos multi-tenant con integración GitFlow. Los equipos organizan su trabajo en **Workspaces** → **Projects** → **Tasks**. Cada tarea genera automáticamente una rama Git con formato `[tipo]/[ID]/[desc]` (ej. `feature/ZTK-25/login`). Un webhook recibe eventos de Git y actualiza el estado de la tarea automáticamente.

## Arquitectura de módulos

```
┌─────────────────────────────────────────────────────┐
│                      server/                         │
│  Ktor API REST · PostgreSQL · JWT Auth · Webhooks    │
│  GET /api.json (spec) · GET /swagger (Swagger UI)    │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP/REST
         ┌─────────────┼──────────────┐
         ▼             ▼              ▼
    shared/       webApp/          cli/
    KMP Models    React+TS         Kotlin/JVM
    Ktor Client   Zustand+MUI      Clikt
    (JVM+Android) Tipos vía        Depende de
         │        openapi-ts       shared
         ▼
   androidApp/
   Compose M3
   ViewModel MVI
```

## Flujo de datos principal

1. **server** expone la API REST. La spec OpenAPI 3.1 se sirve en `GET /api.json`; Swagger UI en `GET /swagger`.
2. **webApp** regenera `src/types/api.ts` desde la spec para mantener sincronía de tipos.
3. **shared** contiene los modelos KMP usados por `server`, `androidApp` y `cli`.
4. Los clientes (`webApp`, `androidApp`, `cli`) consumen el server; **nunca se comunican entre sí**.

## Modelo de dominio clave

| Entidad | Descripción |
|---|---|
| `Workspace` | Unidad de aislamiento multi-tenant. Un usuario → N workspaces. |
| `Project` | Tiene `project_key` único en el workspace (ej. `ZTK`). |
| `Task` | ID: `[project_key]-[task_number]`, generado con `SELECT FOR UPDATE`. |
| `TaskStatus` | Configurable por workspace. Nunca hardcodeado en clientes. |
| `Sprint` | Transversal a proyectos; pertenece al workspace. |

## Infraestructura local

La base de datos de desarrollo corre en Docker. El fichero `docker-compose.yml` en la raíz del monorepo define el servicio `postgres:16`:

```bash
docker compose up -d    # levanta PostgreSQL (zentrack_db en puerto 5433)
docker compose down     # para (datos persisten en volumen zentrack_postgres_data)
docker compose down -v  # reset completo incluyendo volumen
```

Credenciales de desarrollo: `user=zentrack`, `password=zentrack_dev`, `db=zentrack_db`, `port=5433`. Nunca se commitean; se leen desde `server/src/main/resources/application.conf` (excluido de git).

## Decisiones técnicas tomadas

- **Sin Axios**: la webApp usa `fetch` nativo. Axios tiene vulnerabilidades conocidas.
- **Swagger UI vía ktor-openapi (smiley4 5.4.0)**: la spec se genera automáticamente del DSL de rutas; `GET /api.json` devuelve el JSON, `GET /swagger` sirve la UI. Equivale a Swashbuckle en ASP.NET Core.
- **Sin React Context** para estado dinámico: se usa Zustand con selector pattern.
- **Tipos TypeScript generados**: nunca manuales. La spec OpenAPI es la fuente de verdad.
- **Multi-tenancy lógica**: todas las queries de BD filtran por `workspace_id`. RLS en PostgreSQL nunca deshabilitado.
- **Concurrencia de IDs**: `task_number` se genera con `SELECT FOR UPDATE` para garantizar unicidad.
- **Ramas opcionales**: `git_branch_name` puede ser `null` (borrador o fallo de API Git). Es estado válido.

## Estado actual del proyecto

- Los módulos están scaffoldeados pero sin implementación real de features.
- Las historias de usuario del MVP están en `docs/SDD/SPEC.md`.
- El plan técnico (endpoints, esquema BD) está en `docs/SDD/PLAN.md`.
- El backlog de tareas está en `docs/SDD/TASKS.md`.
