# CLAUDE.md — ZenTrack (Raíz del Monorepo)

## QUÉ es este proyecto
Monorepo Kotlin con cinco módulos (`server`, `shared`, `androidApp`, `cli`, `webApp`) que implementan ZenTrack: plataforma ágil multi-tenant con integración GitFlow automatizada. Consulta `AGENTS.md` para el stack completo y los comandos Gradle.

## POR QUÉ estas reglas existen
Mantener coherencia arquitectónica entre módulos escritos en distintos lenguajes y plataformas, garantizando que cada capa respeta sus límites y que el código es verificable en CI sin intervención manual.

## CÓMO trabajar en este repositorio

### Reglas de Modificación de Código

1. **Lee antes de editar.** NUNCA propongas cambios sobre un archivo que no has leído primero.
2. **Scope mínimo.** PROHIBIDO añadir funcionalidad no solicitada, refactors de oportunidad o docstrings en código no modificado.
3. **Sin abstracciones prematuras.** Tres líneas similares no justifican un helper; extrae solo cuando haya cuatro o más usos reales.
4. **Sin manejo de errores especulativo.** Solo valida en las fronteras del sistema (input del usuario, respuestas de APIs externas, webhooks).

### Reglas de Dependencias

- **SIEMPRE** declara versiones en `gradle/libs.versions.toml`. **PROHIBIDO** hardcodear versiones en cualquier `build.gradle.kts`.
- Las dependencias JS del módulo `shared` se declaran con `npm()` dentro del `build.gradle.kts` del módulo. **PROHIBIDO** añadirlas directamente a `webApp/package.json` si corresponden a bindings del shared KMP.
- Usa `implementation(projects.shared)` (typesafe accessors). **PROHIBIDO** usar la notación string `":shared"`.

### Reglas de Git y Branching

- **NUNCA** hagas `git push --force` a `main` o `develop`.
- **NUNCA** omitas hooks de pre-commit (`--no-verify`). Si un hook falla, corrige la causa raíz.
- Formato de rama obligatorio: `[tipo]/[ID-TAREA]/[descripcion-breve]` (ej. `feature/ZTK-42/login-oauth`).
- Los PRs/MRs apuntan a `develop`. **PROHIBIDO** merge directo a `main`.

### Reglas de Seguridad

- **NUNCA** incluyas secrets, tokens, contraseñas ni ficheros `.env` en commits.
- **NUNCA** generes ni expongas URLs de servicios internos o externos sin autorización explícita del usuario.
- **PROHIBIDO** deshabilitar Row Level Security (RLS) en PostgreSQL bajo ninguna circunstancia.

### Archivos de Contexto Disponibles

Antes de implementar cualquier tarea, lee los archivos relevantes de `.ai/`:

| Archivo | Cuándo leerlo |
|---|---|
| `.ai/PROJECT_CONTEXT.md` | Visión general del proyecto, arquitectura y decisiones técnicas |
| `.ai/WORKFLOW_FEATURE_REQUEST.md` | Al implementar una nueva feature |
| `.ai/WORKFLOW_BUG_FIX.md` | Al corregir un bug |
| `.ai/WORKFLOW_REFACTOR.md` | Al reestructurar código sin cambio de comportamiento |
| `.ai/WORKFLOW_DOCUMENTATION.md` | Al escribir o actualizar documentación |
| `.ai/FEATURE_API_ENDPOINT.md` | Al añadir un endpoint Ktor |
| `.ai/FEATURE_ANDROID_SCREEN.md` | Al crear una pantalla Android nueva |
| `.ai/FEATURE_REACT_SCREEN.md` | Al crear una pantalla o componente React nuevo |

### Estructura de Módulos (contexto rápido)

| Módulo | Responsabilidad | CLAUDE.md específico |
|---|---|---|
| `server/` | API Ktor, lógica de negocio, BD | `server/CLAUDE.md` |
| `shared/` | Modelos, DTOs, Ktor Client (JVM + Android) | `shared/CLAUDE.md` |
| `androidApp/` | UI Android Jetpack Compose M3 + ViewModel | `androidApp/CLAUDE.md` |
| `cli/` | CLI Kotlin (Clikt), depende de shared | `cli/CLAUDE.md` |
| `webApp/` | UI Web React + TS + Zustand + MUI | `webApp/CLAUDE.md` |

### Comandos de Verificación Rápida

```bash
./gradlew test                           # Valida todos los módulos antes de proponer cambios
./gradlew :server:buildFatJar            # Verifica compilación del backend
./gradlew :shared:jvmJar                 # Verifica compilación del módulo shared
./gradlew :androidApp:assembleDebug      # Verifica compilación Android
./gradlew :cli:installDist               # Verifica compilación y empaquetado del CLI
```

### Convenciones de Dominio

- El ID de tarea (`display_id`) es **inmutable** tras su creación: `[PROJECT_KEY]-[N]`.
- `task_number` es autoincremental **por proyecto**, generado con `SELECT FOR UPDATE` para garantizar unicidad bajo concurrencia.
- El campo `git_branch_name` puede ser `null` (tarea guardada como borrador o fallo de la API Git); esto es un estado válido, no un error.
- Los estados del Kanban (`TaskStatus`) son **configurables por workspace**, nunca hardcodeados en el cliente.