# AGENTS.md — ZenTrack

## Propósito

ZenTrack es una plataforma minimalista de gestión de proyectos multi-tenant estructurada en **Workspaces**, con integración bidireccional con GitLab/GitHub que automatiza el ciclo de vida de las tareas (GitFlow, webhooks de commit, IDs correlativos tipo `ZTK-1`).

## Stack Global (versiones canónicas)

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.3.0 |
| Backend | Ktor (Netty) | 3.3.3 |
| Base de datos | PostgreSQL + Exposed/Ktorm | — |
| Core compartido | Kotlin Multiplatform | 2.3.0 |
| UI Desktop | Compose Multiplatform + Material 3 | 1.10.0 / 1.10.0-alpha05 |
| UI Web | React + TypeScript + Zustand + MUI | 19.2.0 / 5.8.3 |
| Coroutines | kotlinx-coroutines | 1.10.2 |
| Lifecycle | androidx.lifecycle | 2.9.6 |
| Build tool | Gradle (Kotlin DSL) | — |
| Node | Node.js 18+ / Vite 7 | 7.1.6 |

## Módulos del Monorepo

```
zentrackapp/
├── server/       → Ktor API + lógica de negocio + integración Git
├── shared/       → KMP: modelos, DTOs, Ktor Client (targets: JVM + JS)
├── composeApp/   → Compose Multiplatform Desktop (JVM)
└── webApp/       → React + TypeScript (consume shared via npm)
```

## Comandos Gradle de Referencia

### Validación y Tests
```bash
./gradlew test                            # Todos los módulos
./gradlew :server:test                    # Backend únicamente
./gradlew :shared:jvmTest                 # Shared (target JVM)
./gradlew :shared:jsTest                  # Shared (target JS)
./gradlew :composeApp:test                # Desktop únicamente
```

### Compilación y Distribución
```bash
# Backend
./gradlew :server:buildFatJar             # Fat JAR desplegable

# Shared
./gradlew :shared:jvmJar                  # Artefacto JVM
./gradlew :shared:jsBrowserLibraryDistribution  # Bundle JS + TypeScript definitions

# Desktop
./gradlew :composeApp:packageDistributionForCurrentOS  # DMG/MSI/DEB nativo
./gradlew :composeApp:packageReleaseDistributionForCurrentOS

# Web (Node)
cd webApp && npm run build                # Producción (vite build)
```

### Ejecución en Desarrollo
```bash
./gradlew :server:run -t                  # Backend con hot-reload
./gradlew :composeApp:run                 # Desktop

# Primero construir shared JS, luego web:
./gradlew :shared:jsBrowserLibraryDistribution
cd webApp && npm run start                # Vite dev server
```

## Restricciones Globales

- **PROHIBIDO** crear ramas manualmente salvo fallo del sistema; las ramas se generan desde la UI siguiendo el formato `[tipo]/[ID-TAREA]/[descripcion]`.
- **PROHIBIDO** hacer merge directo a `main`. Todo merge va a `develop` (o rama padre en subtareas).
- **SIEMPRE** referenciar los módulos mediante `projects.shared`, `projects.server`, etc. (typesafe project accessors activos).
- **SIEMPRE** declarar versiones en `gradle/libs.versions.toml`. **PROHIBIDO** hardcodear versiones en `build.gradle.kts`.
- **NUNCA** comprometer credenciales, secrets o ficheros `.env` al repositorio.

## Contexto de Dominio para Agentes

- **Workspace**: unidad de aislamiento multi-tenant. Un usuario pertenece a N workspaces.
- **Project**: tiene `project_key` único dentro del workspace (ej. `ZTK`).
- **Task**: ID compuesto `[project_key]-[task_number]` generado atómicamente con `SELECT FOR UPDATE`.
- **TaskStatus**: configurable por workspace (columnas del Kanban).
- **Sprint**: transversal a proyectos; pertenece al workspace.
- **Webhook Git**: `POST /api/webhooks/git` → parsea `branch_name` → busca tarea → cambia status a "In Progress".

## Archivos de Contexto por Módulo

- `server/CLAUDE.md` — Reglas de Ktor, PostgreSQL, Clean Architecture, RLS.
- `shared/CLAUDE.md` — Reglas KMP, expect/actual, Koin, módulos comunes.
- `composeApp/CLAUDE.md` → ver `shared/CLAUDE.md` (mismas reglas MVI + Compose M3).
- `webApp/CLAUDE.md` — Reglas React, TypeScript, Zustand, MUI.
- `docs/SDD/SPEC.md` — Historias de usuario y criterios de aceptación MVP.
- `docs/SDD/PLAN.md` — Arquitectura técnica, esquema BD y endpoints.
- `docs/SDD/TASKS.md` — Backlog de tareas por fase.