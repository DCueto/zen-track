# WORKFLOW: Bug Fix

Sigue este proceso cuando se reporte o identifique un **bug** en ZenTrack.

## Pre-condiciones

1. Obtén la descripción completa del bug: comportamiento esperado vs. real.
2. Identifica el módulo afectado (`server`, `shared`, `androidApp`, `cli`, `webApp`).
3. Lee el archivo `CLAUDE.md` del módulo antes de tocar código.

## Pasos

### 1. Reproducir

- Localiza el código que produce el comportamiento incorrecto.
- Confirma que entiendes la causa raíz antes de proponer soluciones.
- Si el bug está en la API, verifica primero con logs del server (`./gradlew :server:run -t`).

### 2. Escribir el test que falla (antes de corregir)

**Escribe primero el test que reproduce el bug y confirma que falla.** Este orden es obligatorio:

```
test falla → fix → test pasa
```

Esto garantiza que:
- El test realmente prueba lo que dice probar.
- El fix es lo que hace pasar el test, no otra cosa.

Si el bug está en el server: escribe un integration test con `testApplication { }`.
Si el bug está en la webApp: escribe un test de componente o store con React Testing Library / Vitest.
Si el bug está en shared: escribe el test en `commonTest/`.

### 3. Corregir

- **Scope mínimo**: corrige solo lo que produce el bug. No refactorices código adyacente.
- Si el fix requiere cambiar un modelo en `shared`, aplica el orden de capas del `WORKFLOW_FEATURE_REQUEST.md`.
- Si el fix cambia el contrato de un endpoint, actualiza `openapi.json` y regenera tipos web.

### 4. Verificación

El test que escribiste en el paso 2 debe pasar. Después ejecuta la suite completa del módulo afectado:

```bash
./gradlew :server:test
./gradlew :androidApp:testDebugUnitTest
./gradlew :shared:jvmTest
cd webApp && npm run test:run
```

Si el bug estaba en `shared`, ejecuta todos los módulos:
```bash
./gradlew test
```

### 5. Cierre

- Confirma que el comportamiento esperado se cumple.
- Revisa si el mismo patrón defectuoso existe en otros lugares del código.
