# WORKFLOW: Feature Request

Sigue este proceso completo cuando se solicite implementar una **nueva funcionalidad** en ZenTrack.

## Pre-condiciones

Antes de escribir una línea de código:
1. Lee `docs/SDD/SPEC.md` para validar que la feature está contemplada en el MVP o en la Fase 2.
2. Lee `docs/SDD/PLAN.md` para entender el esquema de BD y los endpoints existentes.
3. Identifica qué módulos se ven afectados: `shared` / `server` / `androidApp` / `cli` / `webApp`.

## Pasos

### 1. Diseño (antes de implementar)

- Determina si la feature requiere **nuevos modelos** en `shared/`. Si es así, empieza por ahí.
- Si hay nuevos endpoints, confirma la estructura del response con el usuario antes de codificar.
- Si hay migraciones de BD, planifica el SQL antes de tocar Kotlin.

### 2. Orden de implementación obligatorio

Respeta siempre este orden de capas (de menos a más dependiente):

```
shared (modelos/DTOs)
  → server (endpoint + service + repositorio + migración BD)
    → androidApp  |  cli  |  webApp  (en paralelo, independientes)
```

Nunca implementes un cliente antes de que el contrato de la API esté definido.

### 3. Implementación por módulo

Principio transversal: **escribe los tests junto con la implementación, no al final**.

---

**shared** (si aplica):
- Añade el modelo/DTO en el paquete correcto.
- Escribe tests en `commonTest/` para la serialización del DTO y la lógica de dominio nueva.
- Verifica: `./gradlew :shared:jvmJar :shared:jvmTest`

---

**server**:
- Ruta → Service → Repository (Clean Architecture).
- Escribe el **unit test del Use Case** antes o durante su implementación (`server/src/test/kotlin/core/`).
- Escribe el **integration test de la ruta** con `testApplication { }` + Testcontainers (`server/src/test/kotlin/api/`).
- Migración SQL en `resources/db/migration/`.
- Actualiza `openapi.json` si hay endpoints nuevos.
- Verifica: `./gradlew :server:test`

---

**webApp** (si aplica):
- Regenera tipos: `cd webApp && npx openapi-typescript http://localhost:8080/openapi.json -o src/types/api.ts`
- Service → Store Zustand → Screen/Component MUI.
- Escribe tests del store (transiciones de estado) y tests de componentes con React Testing Library.
- Verifica: `cd webApp && npx tsc --noEmit && npm run test:run`

---

**androidApp** (si aplica):
- Usa el contexto `FEATURE_ANDROID_SCREEN.md` si creas una nueva pantalla.
- ViewModel → StateUI → Composable.
- Escribe el unit test del ViewModel con un fake repository.
- Verifica: `./gradlew :androidApp:testDebugUnitTest`

---

**cli** (si aplica):
- Añade subcomando Clikt en el paquete correspondiente.
- Verifica: `./gradlew :cli:installDist`

### 4. Cierre

- Confirma que ningún `TODO` quedó sin resolver.
- Confirma que no hay secrets ni URLs hardcodeadas introducidas.
- Asegúrate de que los tipos de `api.ts` están actualizados si hubo cambios de API.
- Todos los tests nuevos pasan: `./gradlew test`
