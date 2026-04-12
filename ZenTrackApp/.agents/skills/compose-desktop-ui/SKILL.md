---
name: compose-desktop-ui
description: >
  Playbook prohibitivo para construir pantallas, componentes y flujos de datos
  en `composeApp/` de ZenTrack (Compose Multiplatform 1.10.0 + Material Design 3).
  Carga esta habilidad siempre que la tarea implique: crear o modificar una
  pantalla Desktop, definir o actualizar un ViewModel MVI, añadir un componente
  Composable reutilizable, gestionar navegación, manejar efectos de un solo uso
  (Snackbar, diálogos, notificaciones), derivar colores o tipografía del tema
  Material 3, conectar un Composable a un repositorio de red, o depurar bugs de
  recomposición o estado zombie. También aplica ante menciones de "pantalla",
  "ViewModel", "State", "Effect", "Channel", "Composable", "LaunchedEffect",
  "recomposición", "MVI", "tema M3", "Kanban Desktop", "BoardScreen",
  "TaskDetailScreen" o cualquier trabajo de UI en Kotlin para la app de escritorio.
  Actívate proactivamente ante cualquier tarea de interfaz Desktop aunque no se
  mencione Compose explícitamente.
---

# Compose Desktop UI — Playbook Operativo para composeApp/

## Por Qué Este Playbook Existe

Compose Multiplatform recompone el árbol de UI cada vez que cambia el estado
observable. Sin disciplina arquitectónica, dos patrones se repiten que destruyen
la estabilidad: **estado zombie** (un booleano en el State que debería haberse
consumido una vez pero persiste) y **recomposiciones innecesarias** (lambdas
inestables, capturas de objetos que cambian constantemente). El patrón MVI +
Channels corta ambos problemas de raíz.

---

## Mapa de Arquitectura

```
composeApp/src/desktopMain/kotlin/
├── ui/
│   ├── screens/           → una carpeta por pantalla (board/, backlog/, task_detail/…)
│   │   └── board/
│   │       ├── BoardScreen.kt       → Composable raíz de la pantalla
│   │       ├── BoardViewModel.kt    → ViewModel MVI
│   │       └── BoardContract.kt     → State, Event, Effect sellados
│   ├── components/        → Composables reutilizables sin lógica de negocio
│   └── theme/
│       ├── ZenTrackTheme.kt         → MaterialTheme root, paleta, tipografía, formas
│       ├── Color.kt                 → Paleta de colores nombrada (nunca inline)
│       └── Type.kt                  → Escala tipográfica
├── navigation/            → NavHost y graph de rutas
└── di/
    └── AppModule.kt       → Módulo Koin: viewModelOf { }, single { }
```

---

## Reglas Absolutas (no negociables)

Estas reglas derivan de `shared/CLAUDE.md`. Violarlas introduce bugs que no
aparecen en pruebas unitarias pero explotan en producción bajo flujos de datos
concurrentes o navegación hacia atrás.

### MVI — Flujo de Datos

- El **State** es inmutable (`data class`). Nunca mutes el estado directamente;
  siempre emite una copia con `copy(…)`.
- Los **Events** son la única forma en que el Composable se comunica con el ViewModel.
  Ninguna lambda de callback debe escapar hacia arriba sin pasar por un `Event`.
- Los **Effects** son para side-effects de un solo uso. Si puede repetirse o
  necesita persistir, va en el State. Si ocurre una sola vez y no debe re-ejecutarse
  al recomponer, va en un `Effect`.

### Effects — Channels Obligatorios

Modelar un evento de un solo uso como campo del State crea estado zombie: el
indicador permanece `true` incluso después de que el efecto se consumió, y
cualquier recomposición posterior lo volverá a disparar.

```kotlin
// PROHIBIDO — estado zombie garantizado
data class BoardState(val showErrorDialog: Boolean = false)

// CORRECTO — el Channel consume el efecto exactamente una vez
private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

Recollecta efectos en el Composable dentro de `LaunchedEffect(Unit)`. Si pones
la colección dentro de un `if` o de un bloque condicional, pierdes efectos que
lleguen antes de que el bloque se evalúe.

### Material 3 — Tema Obligatorio

Hardcodear un color crea dos problemas inmediatos: el tema oscuro nunca funciona
y cambiar la paleta de ZenTrack requiere buscar en todos los ficheros. Toda
decisión visual debe fluir desde `MaterialTheme`.

```kotlin
// PROHIBIDO — rompe dark mode y hace imposible el theming
Box(modifier = Modifier.background(Color(0xFF1A237E)))
Text("Título", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))

// CORRECTO — el tema propaga el cambio a todo el árbol automáticamente
Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary))
Text("Título", style = MaterialTheme.typography.titleLarge)
```

- **PROHIBIDO** pasar `Color`, `TextStyle` o `Shape` como parámetros a
  Composables internos si el tema ya los resuelve.
- **PROHIBIDO** instanciar `MaterialTheme` en más de un punto. Solo existe
  en el root de `composeApp/`.

### Recomposiciones — Estabilidad

El compilador de Compose marca un Composable como *restartable* solo si todos
sus parámetros son estables. Las lambdas capturadas, las listas mutables y los
objetos que no son `data class` o `@Stable` / `@Immutable` invalidan esa marca.

```kotlin
// PROHIBIDO — la lambda se recrea en cada recomposición del padre
BoardCard(
    task = task,
    onClick = { viewModel.onEvent(BoardEvent.SelectTask(task.id)) }  // nueva instancia cada vez
)

// CORRECTO — la lambda es estable porque viewModel es estable y el Event es @Immutable
val onTaskClick: (String) -> Unit = remember(viewModel) {
    { taskId -> viewModel.onEvent(BoardEvent.SelectTask(taskId)) }
}
BoardCard(task = task, onClick = onTaskClick)
```

- Marca con `@Immutable` todos los `sealed class` de Events y Effects.
- Marca con `@Stable` las `data class` de State cuando contengan tipos no
  inferibles como estables por el compilador (ej. `List<T>` de Kotlin).

---

## Procedimiento: Crear una Nueva Pantalla (paso a paso)

### Paso 1 — Definir el Contrato MVI

Crea `[Nombre]Contract.kt` en la carpeta de la pantalla. El contrato es la
especificación formal de la pantalla: qué puede mostrar, qué puede recibir y
qué efectos puede emitir.

```kotlin
// ui/screens/board/BoardContract.kt

@Immutable
data class BoardState(
    val tasks: List<Task> = emptyList(),
    val columns: List<TaskStatus> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTask: Task? = null
)
// Nota: emptyList() es estable; evita List<Task>? nullable cuando sea posible.

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

**Por qué primero el contrato:** Define la interfaz pública entre el ViewModel
y el Composable antes de implementar ninguno de los dos. Eso garantiza que el
ViewModel no filtre detalles de implementación y que el Composable no asuma
comportamientos no comprometidos.

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

    // Channel.BUFFERED garantiza que los efectos emitidos antes de que el
    // Composable esté listo no se pierdan.
    private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: BoardEvent) {
        when (event) {
            is BoardEvent.LoadBoard -> loadBoard(event.workspaceId)
            is BoardEvent.SelectTask -> navigateToTask(event.taskId)
            is BoardEvent.MoveTask -> moveTask(event.taskId, event.newStatusId)
            BoardEvent.RefreshRequested -> {
                val current = _state.value
                _state.update { it.copy(isLoading = true) }
                // reutiliza la última workspaceId cargada
                current.tasks.firstOrNull()?.let { loadBoard(it.id /* workspaceId */) }
            }
        }
    }

    private fun loadBoard(workspaceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { getTasksUseCase(workspaceId) }
                .onSuccess { tasks ->
                    _state.update { it.copy(tasks = tasks, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(BoardEffect.ShowError(error.message ?: "Error desconocido"))
                }
        }
    }

    private fun navigateToTask(taskId: String) {
        viewModelScope.launch {
            _effects.send(BoardEffect.NavigateToTask(taskId))
        }
    }
}
```

**Reglas de ViewModel:**
- Solo expone `StateFlow` (estado) y `Flow<Effect>` (efectos). Nunca expone
  el `MutableStateFlow` ni el `Channel` directamente.
- Toda la lógica de negocio delega a Use Cases de `core/`. El ViewModel no
  habla directamente con Repositories.
- Usa `_state.update { }` para actualizar el estado — es atómico y seguro
  bajo concurrencia.
- Registra el ViewModel en el módulo Koin: `viewModelOf { BoardViewModel(get(), get()) }`.
  No uses `viewModel { }` deprecated.

---

### Paso 3 — Construir el Composable Raíz de Pantalla

El Composable raíz tiene una sola responsabilidad: conectar el ViewModel con la
UI sin introducir lógica de negocio. Delega el rendering a sub-composables.

```kotlin
// ui/screens/board/BoardScreen.kt
@Composable
fun BoardScreen(
    viewModel: BoardViewModel = koinViewModel(),
    onNavigateToTask: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Effects: colecta UNA vez en el ciclo de vida de la pantalla.
    // LaunchedEffect(Unit) se cancela automáticamente cuando el Composable
    // sale del árbol, evitando fugas de coroutines.
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                is BoardEffect.NavigateToTask ->
                    onNavigateToTask(effect.taskId)
                BoardEffect.TaskMoveSuccess ->
                    snackbarHostState.showSnackbar("Tarea movida")
            }
        }
    }

    // Dispara la carga inicial. El workspaceId viene de la navegación o del
    // contexto de sesión inyectado por Koin.
    LaunchedEffect(Unit) {
        viewModel.onEvent(BoardEvent.LoadBoard(workspaceId = "current"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        BoardContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
```

**Reglas del Composable raíz:**
- El parámetro `onNavigateToTask` (y cualquier callback de navegación) llega
  como lambda — nunca se importa NavController directamente en la pantalla.
- `koinViewModel()` inyecta el ViewModel. **Prohibido** instanciar el ViewModel
  con `remember { BoardViewModel(…) }`.
- Mantén la función corta: si supera ~60 líneas, extrae `BoardContent` y otros
  sub-composables.

---

### Paso 4 — Diseñar Componentes Internos

Los componentes de la pantalla reciben `state` y `onEvent` — nunca el ViewModel.
Eso los hace previsualizables, testeables y reutilizables en otras pantallas.

```kotlin
// Componente de contenido principal — no sabe que existe un ViewModel
@Composable
private fun BoardContent(
    state: BoardState,
    onEvent: (BoardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(modifier = modifier) {
        items(state.tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                // La lambda es estable porque onEvent es una referencia de función estable
                onClick = { onEvent(BoardEvent.SelectTask(task.id)) }
            )
        }
    }
}

// Componente atómico — sin estado, sin efectos, puro rendering
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = task.displayId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

**Reglas de componentes:**
- El `modifier: Modifier = Modifier` siempre es el último parámetro (convención Compose).
- Usa `key = { it.id }` en `items()` para evitar recomposiciones completas de la lista.
- Los colores, tamaños de texto y paddings significativos van como constantes con nombre
  en el fichero o en el tema — nunca como literales dispersos.

---

### Paso 5 — Definir el Tema Material 3

El tema se define una vez en el root. Cualquier cambio en `ZenTrackTheme.kt`
se propaga a toda la UI automáticamente.

```kotlin
// ui/theme/Color.kt
val ZenPrimary = Color(0xFF1A237E)
val ZenPrimaryContainer = Color(0xFFBBDEFB)
val ZenSurface = Color(0xFFFAFAFA)
val ZenError = Color(0xFFB00020)
// … define la paleta completa aquí, no en los Composables

// ui/theme/ZenTrackTheme.kt
@Composable
fun ZenTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = ZenPrimaryContainer,
            onPrimary = ZenPrimary,
            surface = Color(0xFF121212)
        )
    } else {
        lightColorScheme(
            primary = ZenPrimary,
            onPrimary = Color.White,
            surface = ZenSurface
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZenTypography,   // definida en Type.kt
        shapes = ZenShapes,           // definida en Shape.kt si se requiere
        content = content
    )
}
```

Envuelve toda la app en `ZenTrackTheme` en `main()` o en el `Application` root:

```kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ZenTrack") {
        ZenTrackTheme {
            App()  // NavHost y pantallas
        }
    }
}
```

---

### Paso 6 — Registrar el ViewModel en Koin

```kotlin
// di/AppModule.kt
val appModule = module {
    viewModelOf(::BoardViewModel)
    viewModelOf(::BacklogViewModel)
    viewModelOf(::TaskDetailViewModel)
    // Use Cases que los ViewModels necesitan
    single { GetTasksForBoardUseCase(get()) }
    single { MoveTaskUseCase(get()) }
}
```

Inicializa Koin en el punto de entrada antes de cualquier `@Composable`:

```kotlin
fun main() = application {
    startKoin {
        modules(networkModule, appModule)  // networkModule viene de shared/
    }
    Window(…) { ZenTrackTheme { App() } }
}
```

---

## Patrones de Recomposición: Qué Hacer y Qué Evitar

| Situación | Prohibido | Correcto |
|---|---|---|
| Callback que pasa a hijo | Lambda inline `{ viewModel.onEvent(…) }` | `remember(viewModel) { { … } }` o referencia de función `viewModel::onEvent` |
| Lista de items | `items(state.tasks)` sin `key` | `items(state.tasks, key = { it.id })` |
| Objeto de estado en hijo | Pasar el `BoardState` completo | Pasar solo los campos que el hijo necesita |
| Carga condicional de datos | `if (loaded) LaunchedEffect(Unit) { … }` | `LaunchedEffect(loadTrigger) { … }` con clave explícita |
| Leer `StateFlow` en Composable | `.value` directo | `collectAsStateWithLifecycle()` |

---

## Checklist de Validación

Antes de marcar cualquier pantalla o componente como completado:

```
ESTADO Y EFECTOS
[ ] Los efectos de un solo uso van en Channel<Effect>, no en booleanos del State
[ ] Los effects se colectan en LaunchedEffect(Unit) dentro del Composable raíz
[ ] El State es una data class @Immutable con solo tipos estables
[ ] El ViewModel usa _state.update { } para mutaciones (atómico)
[ ] El Channel tiene capacidad BUFFERED (no RENDEZVOUS ni CONFLATED)

MATERIAL 3
[ ] Cero hardcodeos de Color(0xFF…) fuera de Color.kt
[ ] Cero TextStyle o FontSize literales fuera de Type.kt
[ ] Todos los colores derivan de MaterialTheme.colorScheme.*
[ ] Todas las tipografías derivan de MaterialTheme.typography.*
[ ] ZenTrackTheme envuelve el árbol exactamente una vez

RECOMPOSICIÓN
[ ] Las lambdas pasadas a hijos usan remember o son referencias de función estables
[ ] Los items de listas tienen key = { it.id } (o campo único)
[ ] Los componentes reciben solo los campos que necesitan, no el State completo
[ ] collectAsStateWithLifecycle() en lugar de .value directo

ARQUITECTURA
[ ] El Composable raíz no tiene lógica de negocio
[ ] Los componentes internos no conocen el ViewModel
[ ] El ViewModel no importa io.ktor.* ni org.jetbrains.exposed.*
[ ] Los ViewModels están registrados con viewModelOf { } en Koin, no instanciados con remember
[ ] La navegación fluye vía Effect (NavigateToX), nunca mediante llamada directa al NavController dentro del ViewModel
```

Comandos de verificación:

```bash
./gradlew :composeApp:test      # Tests unitarios de ViewModels
./gradlew :composeApp:run       # Verifica compilación y arranque desktop
```

---

## Referencia Rápida de Ficheros

```
composeApp/src/desktopMain/kotlin/
├── ui/
│   ├── screens/
│   │   └── [nombre]/
│   │       ├── [Nombre]Contract.kt   → State, Event, Effect (@Immutable sealed)
│   │       ├── [Nombre]ViewModel.kt  → StateFlow + Channel<Effect>
│   │       └── [Nombre]Screen.kt     → koinViewModel() + LaunchedEffect + Scaffold
│   ├── components/                   → Composables sin ViewModel, testeables
│   └── theme/
│       ├── ZenTrackTheme.kt          → MaterialTheme root único
│       ├── Color.kt                  → Paleta nombrada completa
│       └── Type.kt                   → Escala tipográfica
├── navigation/                       → NavHost + rutas tipadas
└── di/
    └── AppModule.kt                  → viewModelOf { } + single { }
```
