# CLAUDE.md — shared/ (KMP JVM + Android)

## QUÉ hace este módulo
`shared/` es el módulo KMP con targets `jvm()` y `androidTarget()`. Contiene modelos de dominio, DTOs `@Serializable`, Ktor Client y repositorios de red. Es consumido por `server/`, `cli/` (vía target JVM) y `androidApp/` (vía target Android). No compila a JS.

## POR QUÉ estas reglas existen
El valor de KMP reside en que `commonMain` es 100% portable. Una sola importación de `java.*` en `commonMain` rompe el target Android silenciosamente. Las APIs de plataforma van en `jvmMain` o `androidMain` según corresponda.

## CÓMO estructurar el código

### Aislamiento de commonMain

- **PROHIBIDO** importar `java.*` o `android.*` dentro de `commonMain`.
- **PROHIBIDO** importar `androidx.*` en `commonMain` (usar `org.jetbrains.androidx.*` si existe equivalente KMP, o `expect/actual`).
- **SIEMPRE** mantén `commonMain` como Kotlin puro: solo `kotlin.*`, `kotlinx.*` y dependencias declaradas en `commonMain.dependencies`.
- Dependencias de plataforma van en `jvmMain` o `androidMain` exclusivamente.

### expect/actual e Interoperabilidad

Usa `expect/actual` **solo** para APIs nativas simples (formato de fecha, UUID, logging de plataforma).
Para lógica compleja que requiere APIs nativas, define una `interface` en `commonMain` e inyéctala con Koin desde `jvmMain`/`androidMain`.

```kotlin
// commonMain — CORRECTO
expect fun generateUuid(): String

// jvmMain — CORRECTO (usado por server y cli)
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// androidMain — CORRECTO (usado por androidApp)
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
// Nota: en Android, java.util.UUID está disponible, pero si se necesita
// una API exclusiva de Android, se usa android.* solo en androidMain.
```

**PROHIBIDO** duplicar lógica de negocio en bloques `actual`; los `actual` son adaptadores de plataforma, no implementaciones de dominio.

### Estructura de shared/

```
src/
├── commonMain/kotlin/
│   ├── model/        → Entidades de dominio (@Serializable data class)
│   ├── dto/          → Request/Response DTOs (@Serializable)
│   ├── repository/   → Interfaces de repositorio (contratos)
│   ├── network/      → Ktor Client config + API calls
│   └── di/           → Módulos Koin comunes
├── jvmMain/kotlin/   → actual implementations JVM (server + cli)
└── androidMain/kotlin/ → actual implementations Android (androidApp)
```

### Arquitectura UI: MVI en androidApp/

Cada pantalla tiene tres tipos: `Event`, `State` y `Effect`.

```kotlin
// CORRECTO — separación estricta MVI
data class BoardState(val tasks: List<Task>, val isLoading: Boolean)
sealed class BoardEvent { data class LoadTasks(val workspaceId: String) : BoardEvent() }
sealed class BoardEffect { data class ShowError(val message: String) : BoardEffect() }
```

- El **Estado** fluye hacia abajo: el Composable lee `state` y no tiene lógica de negocio.
- Los **Eventos** fluyen hacia arriba: el Composable solo despacha `onEvent(BoardEvent.X)`.
- Los **Effects** son side-effects de un solo uso (navegación, Snackbar, Toast).

Ver `androidApp/CLAUDE.md` para el playbook completo de Jetpack Compose + ViewModel.

### Koin (Inyección de Dependencias)

- Declara módulos Koin en `shared/commonMain/di/`. Los módulos de plataforma (`jvmMain`, `androidMain`) extienden o sobreescriben con implementaciones `actual`.
- **PROHIBIDO** usar `GlobalContext.get()` o `KoinComponent` directamente en Composables; usa `koinViewModel()` o inyección en el punto de entrada.
- Los ViewModels se registran con `viewModelOf { }` en el módulo Koin del cliente (`androidApp`), nunca en `shared`.

### Tipos para webApp/

`webApp/` NO depende del módulo `shared` de Kotlin. Sus tipos TypeScript se generan desde la spec OpenAPI del servidor con `openapi-typescript`. Ver `webApp/CLAUDE.md` para el flujo completo.