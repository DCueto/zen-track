# CLAUDE.md — androidApp/ (Jetpack Compose + ViewModel + Koin)

## QUÉ hace este módulo
Aplicación Android nativa con Jetpack Compose y Material 3. Consume la API REST del servidor Ktor a través de las interfaces de repositorio declaradas en `shared/`. La navegación se gestiona con `navigation-compose`. La inyección de dependencias usa Koin. El estado de pantalla sigue el patrón MVI (Model-View-Intent).

## POR QUÉ estas reglas existen
Compose es reactivo: si el estado muta fuera del `ViewModel`, los recompose son impredecibles y los tests de ViewModel dejan de ser fiables. El patrón MVI hace el flujo de datos unidireccional y elimina ambigüedad sobre quién modifica el estado. Separar `State`, `Event` y `Effect` hace que cada pantalla sea testeable sin renderizar UI.

## CÓMO estructurar el código

### Estructura de directorios

```
src/main/kotlin/me/dcueto/zentrackapp/
├── ui/
│   ├── screens/          → Una carpeta por pantalla (Board, Backlog, TaskDetail, Login…)
│   │   └── board/
│   │       ├── BoardScreen.kt       → Composable raíz de la pantalla
│   │       ├── BoardViewModel.kt    → ViewModel MVI
│   │       ├── BoardState.kt        → Estado UI (data class)
│   │       ├── BoardEvent.kt        → Intenciones del usuario (sealed class)
│   │       └── BoardEffect.kt       → Side-effects de un solo uso (sealed class)
│   ├── components/       → Composables reutilizables sin estado global
│   └── theme/            → ZenTrackTheme, colores, tipografía, shapes
├── navigation/           → NavGraph, rutas, argumentos de navegación
├── di/                   → Módulos Koin del módulo androidApp
└── ZenTrackApp.kt        → Application class, inicio de Koin
```

### Patrón MVI — Estado, Eventos y Effects

Cada pantalla define tres tipos en archivos separados:

```kotlin
// BoardState.kt — estado UI inmutable
data class BoardState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// BoardEvent.kt — intenciones del usuario
sealed class BoardEvent {
    data class LoadTasks(val workspaceId: String) : BoardEvent()
    data class MoveTask(val taskId: String, val newStatusId: String) : BoardEvent()
}

// BoardEffect.kt — side-effects de un solo uso (no reprocesables)
sealed class BoardEffect {
    data class ShowError(val message: String) : BoardEffect()
    data class NavigateToDetail(val taskId: String) : BoardEffect()
}
```

Reglas de MVI:
- El Composable **solo lee** `state` y **solo despacha** eventos vía `onEvent(...)`. **PROHIBIDO** tener lógica de negocio en Composables.
- El `ViewModel` es el único que muta el estado.
- Los `Effect` se emiten con `Channel` / `SharedFlow` y se consumen **una sola vez** en el Composable con `LaunchedEffect`.

### ViewModel

```kotlin
class BoardViewModel(
    private val taskRepository: TaskRepository   // inyectado por Koin, interfaz de shared
) : ViewModel() {

    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    private val _effects = Channel<BoardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: BoardEvent) {
        when (event) {
            is BoardEvent.LoadTasks -> loadTasks(event.workspaceId)
            is BoardEvent.MoveTask  -> moveTask(event.taskId, event.newStatusId)
        }
    }

    private fun loadTasks(workspaceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            taskRepository.getTasksByWorkspace(workspaceId)
                .onSuccess { tasks -> _state.update { it.copy(tasks = tasks, isLoading = false) } }
                .onFailure { e -> _effects.send(BoardEffect.ShowError(e.message ?: "Error")) }
        }
    }
}
```

- **PROHIBIDO** inyectar `Context`, `Activity` ni `NavController` en el `ViewModel`.
- **PROHIBIDO** usar `LiveData`; usa `StateFlow` y `SharedFlow`/`Channel`.

### Composables y Compose UI

- **SIEMPRE** usa tokens del tema (`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`). **PROHIBIDO** colores o tamaños hardcodeados.
- **SIEMPRE** añade `@Preview` a los Composables presentacionales para facilitar revisión visual.
- Los Composables de pantalla reciben `state` y `onEvent` como parámetros; no llaman directamente a `koinViewModel()` en el nivel de componente.
- El `koinViewModel()` se instancia en el nivel de pantalla raíz (dentro del NavGraph o en `NavHost`).

```kotlin
// CORRECTO — screen raíz obtiene el ViewModel; componentes hijos solo reciben datos
@Composable
fun BoardScreen(viewModel: BoardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BoardContent(state = state, onEvent = viewModel::onEvent)
}

// CORRECTO — componente hijo sin dependencias de ViewModel
@Composable
fun TaskCard(task: Task, onMove: (String) -> Unit) { ... }
```

### Navegación

- Define todas las rutas como `sealed class` o `object` en `navigation/`. **PROHIBIDO** strings literales de rutas dispersos por el código.
- Pasa solo IDs escalares entre pantallas (no objetos serializados enteros). El destino carga el detalle desde el repositorio con ese ID.

### Inyección de Dependencias (Koin)

- Los módulos Koin de `androidApp` se declaran en `di/`. Extienden los módulos de `shared/commonMain/di/`.
- Los ViewModels se registran con `viewModelOf { }`:

```kotlin
val androidAppModule = module {
    viewModelOf(::BoardViewModel)
    viewModelOf(::TaskDetailViewModel)
}
```

- **PROHIBIDO** `KoinComponent` en Composables; usa `koinViewModel()` o `get()` en el Application/Activity.

### Configuración de API Base URL

- La URL del servidor se inyecta vía `BuildConfig` (campo generado desde `local.properties` o variables de entorno de CI). **NUNCA** hardcodees `http://10.0.2.2:8080` en el código fuente.

### Tests

#### Tipos de test y ubicación

```
src/test/kotlin/        → Unit tests (JVM, sin emulador)
src/androidTest/kotlin/ → Instrumented tests (requieren emulador/dispositivo)
```

- Unit tests: **ViewModels** con fake repositories. Rápidos, sin emulador.
- Instrumented / UI tests: Composables con `createComposeRule()` para flujos críticos.

#### Unit tests de ViewModel

```kotlin
class BoardViewModelTest {
    private val fakeRepository = FakeTaskRepository()
    private val viewModel = BoardViewModel(fakeRepository)

    @Test
    fun `loadTasks emits tasks to state`() = runTest {
        fakeRepository.seedTasks(listOf(Task(id = "1", title = "Fix bug")))

        viewModel.onEvent(BoardEvent.LoadTasks(workspaceId = "ws-1"))

        val state = viewModel.state.value
        assertEquals(1, state.tasks.size)
        assertEquals("Fix bug", state.tasks.first().title)
    }
}
```

- **SIEMPRE** usa `runTest` de `kotlinx-coroutines-test` para código con coroutines.
- **PROHIBIDO** usar MockK para los repositorios; usa **fake implementations** (las mismas de `shared/commonTest`).
- **PROHIBIDO** `@Ignore` / `@Disabled` sin comentario que explique cuándo se habilitará.

#### Tests de Composables (UI)

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun `board screen shows task title`() {
    composeTestRule.setContent {
        BoardContent(
            state = BoardState(tasks = listOf(Task(id = "1", title = "Fix bug"))),
            onEvent = {}
        )
    }
    composeTestRule.onNodeWithText("Fix bug").assertIsDisplayed()
}
```

- Testea **comportamiento observable** (textos visibles, botones habilitados/deshabilitados), nunca estructura interna del árbol de composables.
- **PROHIBIDO** snapshot tests como estrategia principal.

```bash
./gradlew :androidApp:testDebugUnitTest         # Unit tests (sin emulador)
./gradlew :androidApp:connectedDebugAndroidTest # Instrumented tests (requiere emulador)
```
