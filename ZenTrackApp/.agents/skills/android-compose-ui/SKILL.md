---
name: android-compose-ui
description: >
  Playbook para construir pantallas, componentes y flujos de datos en
  `androidApp/` de ZenTrack (Jetpack Compose + Material Design 3 + ViewModel +
  Navigation Compose). Carga esta habilidad siempre que la tarea implique:
  crear o modificar una pantalla Android, definir o actualizar un ViewModel MVI,
  añadir un Composable reutilizable, gestionar navegación tipo-safe, manejar
  efectos de un solo uso (Snackbar, diálogos), configurar el tema Material You,
  gestionar insets de sistema (edge-to-edge), manejar el botón Back predictivo,
  sobrevivir cambios de configuración con rememberSaveable, o depurar bugs de
  recomposición y estado zombie.
  También aplica ante menciones de "pantalla Android", "ViewModel", "State",
  "Effect", "Channel", "Composable", "LaunchedEffect", "collectAsStateWithLifecycle",
  "NavHost", "BackHandler", "rememberSaveable", "MVI", "tema M3", "edge-to-edge",
  "BoardScreen", "TaskDetailScreen" o cualquier trabajo de UI Android en Kotlin.
---

# Android Compose UI — Playbook para androidApp/

## Por Qué Este Playbook Existe

Jetpack Compose tiene tres trampas específicas de Android que no existen en
otros entornos:

1. **Estado zombie**: un booleano en el State consumido una vez que vuelve a
   dispararse en cada recomposición.
2. **Leak de colección en background**: `collectAsState()` sin lifecycle
   awareness sigue recibiendo eventos cuando la app está en segundo plano.
3. **Pérdida de estado en rotación**: un estado en `remember {}` que no usa
   `rememberSaveable` se pierde en cada cambio de configuración.

El patrón MVI + `Channel<Effect>` + `collectAsStateWithLifecycle` + `rememberSaveable`
corta los tres de raíz.

---

## Mapa de Arquitectura

```
androidApp/src/main/kotlin/
├── ZenTrackApp.kt             → Application: startKoin { androidContext(this) … }
├── MainActivity.kt            → ComponentActivity: enableEdgeToEdge() + setContent {}
├── ui/
│   ├── navigation/
│   │   └── ZenTrackNavHost.kt → NavHost con rutas @Serializable (Navigation 2.8+)
│   ├── screens/
│   │   └── board/
│   │       ├── BoardContract.kt     → @Immutable State / sealed Event / sealed Effect
│   │       ├── BoardViewModel.kt    → StateFlow + Channel<Effect> + viewModelScope
│   │       └── BoardScreen.kt       → koinViewModel() + collectAsStateWithLifecycle()
│   ├── components/            → Composables sin estado global, testeables con Preview
│   └── theme/
│       ├── ZenTrackTheme.kt   → MaterialTheme root + Dynamic Color (Android 12+)
│       ├── Color.kt           → Paleta nombrada (fallback pre-Android 12)
│       └── Type.kt            → Escala tipográfica
└── di/
    └── AppModule.kt           → viewModelOf { } + single { }
```

---

## Reglas Absolutas

### 1. `collectAsStateWithLifecycle()` — OBLIGATORIO, nunca `collectAsState()`

`collectAsState()` sigue recibiendo actualizaciones cuando la app está en
background, gastando CPU/batería y potencialmente causando crashes.

```kotlin
// PROHIBIDO — no es lifecycle-aware
val state by viewModel.state.collectAsState()

// OBLIGATORIO — para cuando la app va a background
val state by viewModel.state.collectAsStateWithLifecycle()
```

Requiere `androidx.lifecycle:lifecycle-runtime-compose` en las dependencias.

---

### 2. Effects — `Channel<Effect>`, nunca booleanos en State

```kotlin
// PROHIBIDO — estado zombie: showError vuelve a true en cada recomposición
data class BoardState(val showError: Boolean = false, val errorMsg: String = "")

// OBLIGATORIO
private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

Recoge el flow en `LaunchedEffect(Unit)` **dentro del Composable raíz**. El
`Channel.BUFFERED` garantiza que efectos emitidos antes de que el Composable
esté activo no se pierdan.

---

### 3. `rememberSaveable` para estado que debe sobrevivir rotación

```kotlin
// PROHIBIDO — se pierde en rotación de pantalla o proceso killed
var query by remember { mutableStateOf("") }

// CORRECTO — sobrevive cambios de configuración y Process Death
var query by rememberSaveable { mutableStateOf("") }
```

Usa `rememberSaveable` para cualquier input del usuario, posición de scroll
temporal, o flags de UI que no quieras perder al rotar. Para objetos complejos,
implementa un `Saver` personalizado.

---

### 4. Material 3 — nunca colores hardcodeados

```kotlin
// PROHIBIDO
Box(Modifier.background(Color(0xFF1A237E)))

// OBLIGATORIO
Box(Modifier.background(MaterialTheme.colorScheme.primary))
```

---

### 5. Edge-to-edge — SIEMPRE en `MainActivity`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // dibuja bajo las barras de sistema
        setContent {
            ZenTrackTheme {
                ZenTrackNavHost()
            }
        }
    }
}
```

Los Composables que necesiten espacio bajo la status bar o sobre la nav bar
deben usar `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` o el
parámetro `contentPadding` del `Scaffold`.

---

### 6. Back Predictivo — `BackHandler` para interceptar el gesto

```kotlin
// En una pantalla con estado "edición en progreso":
BackHandler(enabled = state.hasUnsavedChanges) {
    viewModel.onEvent(BoardEvent.ShowDiscardDialog)
}
```

`BackHandler` con `enabled = false` no intercepta el gesto — no hace falta
condicionar su presencia en el árbol.

---

## Procedimiento: Crear una Nueva Pantalla

### Paso 1 — Definir el Contrato MVI

```kotlin
// ui/screens/board/BoardContract.kt
@Immutable
data class BoardState(
    val tasks: List<Task> = emptyList(),
    val columns: List<TaskStatus> = emptyList(),
    val isLoading: Boolean = false
)

@Immutable
sealed class BoardEvent {
    data class LoadBoard(val workspaceId: String) : BoardEvent()
    data class SelectTask(val taskId: String) : BoardEvent()
    data class MoveTask(val taskId: String, val newStatusId: String) : BoardEvent()
    data object RefreshRequested : BoardEvent()
}

@Immutable
sealed class BoardEffect {
    data class ShowError(val message: String) : BoardEffect()
    data class NavigateToTask(val taskId: String) : BoardEffect()
    data object TaskMoveSuccess : BoardEffect()
}
```

---

### Paso 2 — Implementar el ViewModel

```kotlin
// ui/screens/board/BoardViewModel.kt
class BoardViewModel(
    private val getTasksUseCase: GetTasksForBoardUseCase,
    private val moveTaskUseCase: MoveTaskUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: BoardEvent) {
        when (event) {
            is BoardEvent.LoadBoard      -> loadBoard(event.workspaceId)
            is BoardEvent.SelectTask     -> navigateToTask(event.taskId)
            is BoardEvent.MoveTask       -> moveTask(event.taskId, event.newStatusId)
            BoardEvent.RefreshRequested  -> refresh()
        }
    }

    private fun loadBoard(workspaceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { getTasksUseCase(workspaceId) }
                .onSuccess { tasks -> _state.update { it.copy(tasks = tasks, isLoading = false) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(BoardEffect.ShowError(error.message ?: "Error desconocido"))
                }
        }
    }

    private fun navigateToTask(taskId: String) {
        viewModelScope.launch { _effects.send(BoardEffect.NavigateToTask(taskId)) }
    }

    private fun refresh() {
        // reutiliza la última workspaceId del estado
    }
}
```

---

### Paso 3 — Composable Raíz de Pantalla

```kotlin
// ui/screens/board/BoardScreen.kt
@Composable
fun BoardScreen(
    onNavigateToTask: (String) -> Unit,
    viewModel: BoardViewModel = koinViewModel()
) {
    // collectAsStateWithLifecycle detiene la colección cuando la app va a background
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Colecta efectos una sola vez en el ciclo de vida de la pantalla
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardEffect.ShowError       -> snackbarHostState.showSnackbar(effect.message)
                is BoardEffect.NavigateToTask  -> onNavigateToTask(effect.taskId)
                BoardEffect.TaskMoveSuccess    -> snackbarHostState.showSnackbar("Tarea movida")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(BoardEvent.LoadBoard("current"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoardContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
```

---

### Paso 4 — Navegación Tipo-Safe (Navigation Compose 2.8+)

Define rutas como clases `@Serializable` — sin strings mágicos.

```kotlin
// ui/navigation/ZenTrackNavHost.kt
@Serializable object WorkspacesRoute
@Serializable data class BoardRoute(val workspaceId: String)
@Serializable data class TaskDetailRoute(val taskId: String)

@Composable
fun ZenTrackNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = WorkspacesRoute) {
        composable<WorkspacesRoute> {
            WorkspacesScreen(
                onNavigateToBoard = { workspaceId ->
                    navController.navigate(BoardRoute(workspaceId))
                }
            )
        }
        composable<BoardRoute> { backStackEntry ->
            val route: BoardRoute = backStackEntry.toRoute()
            BoardScreen(
                onNavigateToTask = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId))
                }
            )
        }
        composable<TaskDetailRoute> {
            TaskDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Reglas de navegación:**
- **PROHIBIDO** pasar `NavController` directamente a una pantalla. Las pantallas
  reciben lambdas de navegación (`onNavigateToX: () -> Unit`).
- La navegación se dispara siempre vía `Effect` desde el ViewModel, nunca desde
  la lógica interna de un Composable.
- Usa `navController.navigate(route)` con objetos `@Serializable`, nunca con
  strings o rutas construidas manualmente.

---

### Paso 5 — Tema Material 3 con Dynamic Color

```kotlin
// ui/theme/ZenTrackTheme.kt
@Composable
fun ZenTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Color: usa la paleta del wallpaper en Android 12+
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && darkTheme  -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme                  -> darkColorScheme(primary = ZenPrimaryContainer)
        else                       -> lightColorScheme(primary = ZenPrimary, surface = ZenSurface)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZenTypography,
        content = content
    )
}
```

```kotlin
// ui/theme/Color.kt — fallback para Android < 12
val ZenPrimary = Color(0xFF1A237E)
val ZenPrimaryContainer = Color(0xFFBBDEFB)
val ZenSurface = Color(0xFFFAFAFA)
```

---

### Paso 6 — Wiring en Application + MainActivity

```kotlin
// ZenTrackApp.kt
class ZenTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ZenTrackApp)
            modules(networkModule, appModule)
        }
    }
}

// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenTrackTheme {
                ZenTrackNavHost()
            }
        }
    }
}
```

Registra `ZenTrackApp` en `AndroidManifest.xml`:
```xml
<application android:name=".ZenTrackApp" … />
```

---

### Paso 7 — Registrar ViewModels en Koin

```kotlin
// di/AppModule.kt
val appModule = module {
    viewModelOf(::BoardViewModel)
    viewModelOf(::WorkspacesViewModel)
    viewModelOf(::TaskDetailViewModel)
    single { GetTasksForBoardUseCase(get()) }
    single { MoveTaskUseCase(get()) }
}
```

---

## Patrones de Recomposición

| Situación | Prohibido | Correcto |
|---|---|---|
| Leer StateFlow en Composable | `collectAsState()` | `collectAsStateWithLifecycle()` |
| Estado que sobrevive rotación | `remember { mutableStateOf("") }` | `rememberSaveable { mutableStateOf("") }` |
| Callback a hijo | Lambda inline `{ viewModel.onEvent(…) }` | `viewModel::onEvent` o `remember(viewModel) { { … } }` |
| Lista de items | `items(state.tasks)` sin `key` | `items(state.tasks, key = { it.id })` |
| Efecto de un solo uso | Booleano en State | `Channel<Effect>(BUFFERED)` |
| Back personalizado | Interceptar con `onBackPressed()` | `BackHandler(enabled = …) { … }` |

---

## Checklist de Validación

```
ESTADO Y EFECTOS
[ ] collectAsStateWithLifecycle() en lugar de collectAsState()
[ ] rememberSaveable para inputs y estado de UI que deben sobrevivir rotación
[ ] Los efectos de un solo uso van en Channel<Effect>(BUFFERED), no en booleanos del State
[ ] Los effects se colectan en LaunchedEffect(Unit) en el Composable raíz
[ ] _state.update { } para mutaciones (atómico bajo concurrencia)

ANDROID-ESPECÍFICO
[ ] enableEdgeToEdge() llamado en MainActivity.onCreate() antes de setContent
[ ] Scaffold usa innerPadding para respetar los insets del sistema
[ ] BackHandler usado para interceptar el gesto Back cuando sea necesario
[ ] ZenTrackApp declarado en AndroidManifest.xml con android:name

MATERIAL 3
[ ] Cero Color(0xFF…) hardcodeados fuera de Color.kt
[ ] Todos los colores via MaterialTheme.colorScheme.*
[ ] dynamicColorScheme activo en Android 12+ (Material You)
[ ] ZenTrackTheme envuelve el árbol exactamente una vez

NAVEGACIÓN
[ ] Rutas definidas como clases @Serializable, sin strings mágicos
[ ] NavController NO se pasa a pantallas — solo lambdas de navegación
[ ] Navegación disparada via Effect desde ViewModel, no desde Composable

ARQUITECTURA
[ ] Composable raíz sin lógica de negocio
[ ] Componentes internos reciben solo los campos que necesitan, no el State completo
[ ] ViewModel no importa io.ktor.* ni org.jetbrains.exposed.*
[ ] ViewModels registrados con viewModelOf { } en Koin
```

Comandos de verificación:

```bash
./gradlew :androidApp:testDebugUnitTest   # Tests unitarios (ViewModels, UseCases)
./gradlew :androidApp:assembleDebug       # Verifica compilación completa
./gradlew :androidApp:lintDebug          # Detecta problemas de API Android
```

---

## Referencia Rápida de Ficheros

```
androidApp/src/main/kotlin/
├── ZenTrackApp.kt                → Application + startKoin
├── MainActivity.kt               → enableEdgeToEdge + setContent
├── ui/
│   ├── navigation/
│   │   └── ZenTrackNavHost.kt    → @Serializable rutas + NavHost
│   ├── screens/
│   │   └── [nombre]/
│   │       ├── [Nombre]Contract.kt  → @Immutable State / sealed Event / sealed Effect
│   │       ├── [Nombre]ViewModel.kt → StateFlow + Channel<Effect> + viewModelScope
│   │       └── [Nombre]Screen.kt    → collectAsStateWithLifecycle + LaunchedEffect
│   ├── components/               → Composables sin ViewModel, con @Preview
│   └── theme/
│       ├── ZenTrackTheme.kt      → MaterialTheme + Dynamic Color
│       ├── Color.kt              → Paleta fallback (pre-Android 12)
│       └── Type.kt               → Escala tipográfica
└── di/
    └── AppModule.kt              → viewModelOf { } + single { }
```
