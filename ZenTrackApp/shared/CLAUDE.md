# CLAUDE.md — shared/ + composeApp/ (KMP + Compose Multiplatform)

## QUÉ hacen estos módulos
`shared/` es el módulo KMP con targets `jvm()` y `js()`. Contiene modelos de dominio, DTOs `@Serializable`, Ktor Client y repositorios de red. Genera TypeScript definitions para `webApp/`.
`composeApp/` es la aplicación Desktop (JVM) con Compose Multiplatform 1.10.0 + Material 3. Depende de `shared`.

## POR QUÉ estas reglas existen
El valor de KMP reside en que `commonMain` es 100% portable. Una sola importación de `android.*` o `java.*` rompe el target JS silenciosamente o fuerza compilaciones condicionales innecesarias. El patrón MVI + Channels elimina bugs de estado inconsistente en UIs reactivas.

## CÓMO estructurar el código

### Aislamiento de commonMain

- **PROHIBIDO** importar `android.*`, `ios.*` o `java.*` dentro de `commonMain`.
- **PROHIBIDO** importar `androidx.*` en `commonMain` (usar `org.jetbrains.androidx.*` si existe equivalente KMP, o `expect/actual`).
- **SIEMPRE** mantén `commonMain` como Kotlin puro: solo `kotlin.*`, `kotlinx.*` y dependencias declaradas en `commonMain.dependencies`.
- Dependencias de plataforma van en `jvmMain` o `jsMain` exclusivamente.

### expect/actual e Interoperabilidad

Usa `expect/actual` **solo** para APIs nativas simples (formato de fecha, UUID, logging de plataforma).
Para lógica compleja que requiere APIs nativas, define una `interface` en `commonMain` e inyéctala con Koin desde `jvmMain`/`jsMain`.

```kotlin
// commonMain — CORRECTO
expect fun generateUuid(): String

// jvmMain — CORRECTO
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// jsMain — CORRECTO
actual fun generateUuid(): String = js("crypto.randomUUID()") as String
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
├── jvmMain/kotlin/   → actual implementations JVM
└── jsMain/kotlin/    → actual implementations JS
```

### Arquitectura UI: MVI en composeApp/

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

### Eventos One-Shot (Effects): regla crítica

**NUNCA** modeles un evento de un solo uso como booleano en el State:

```kotlin
// PROHIBIDO
data class BoardState(val showErrorSnackbar: Boolean = false) // causa re-renders y estado zombie

// OBLIGATORIO — usa Channel en el ViewModel
private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

**SIEMPRE** colecciona los effects en el Composable usando `LaunchedEffect` vinculado al ciclo de vida:

```kotlin
LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
        when (effect) {
            is BoardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
        }
    }
}
```

### Diseño Material 3 en Compose

- **NUNCA** uses colores hardcodeados (`Color(0xFF...)` directamente en el árbol de composición).
- **SIEMPRE** deriva colores de `MaterialTheme.colorScheme`, tipografías de `MaterialTheme.typography` y formas de `MaterialTheme.shapes`.
- El `MaterialTheme` con la paleta de ZenTrack se define una sola vez en el root de `composeApp/` y se propaga por composición.

```kotlin
// PROHIBIDO
Text("ZenTrack", color = Color(0xFF1A237E))

// OBLIGATORIO
Text("ZenTrack", color = MaterialTheme.colorScheme.primary)
```

- **PROHIBIDO** pasar `Color`, `TextStyle` o `Shape` como parámetros en Composables internos si pueden resolverse desde el tema.

### Koin (Inyección de Dependencias)

- Declara módulos Koin en `shared/commonMain/di/`. Los módulos de plataforma (`jvmMain`, `jsMain`) extienden o sobreescriben con implementaciones `actual`.
- **PROHIBIDO** usar `GlobalContext.get()` o `KoinComponent` directamente en Composables; usa `koinViewModel()` o inyección en el punto de entrada.
- Los ViewModels se registran con `viewModelOf { }` en el módulo Koin del cliente (composeApp), nunca en `shared`.

### TypeScript Definitions (para webApp/)

- `shared/build.gradle.kts` tiene `generateTypeScriptDefinitions()` activo en el target `js`.
- Al añadir o modificar clases `@Serializable` en `commonMain`, ejecuta `./gradlew :shared:jsBrowserLibraryDistribution` para regenerar las definiciones `.d.ts`.
- **NUNCA** edites manualmente los archivos `.d.ts` generados; son artefactos de compilación.