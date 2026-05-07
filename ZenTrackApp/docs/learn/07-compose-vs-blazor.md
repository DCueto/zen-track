# 07 — Jetpack Compose para devs Blazor

> Cómo pensar en Jetpack Compose si conoces Blazor y el ecosistema .NET.

---

## Composables vs Razor Components

En Blazor defines UI con archivos `.razor` que mezclan HTML y C#. En Compose defines UI con **funciones anotadas con `@Composable`** — no hay HTML, no hay XML, todo es Kotlin.

```razor
<!-- Blazor — WorkspacesPage.razor -->
@page "/workspaces"

<h1>Workspaces</h1>
<p>@message</p>

@code {
    private string message = "Fase 2";
}
```

```kotlin
// Compose — WorkspacesScreen.kt
@Composable
fun WorkspacesScreen(onWorkspaceSelected: (workspaceId: String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Workspaces — Fase 2", style = MaterialTheme.typography.headlineMedium)
    }
}
```

La diferencia conceptual más importante: Blazor re-renderiza el componente entero cuando cambia el estado. Compose es más inteligente — solo **recompone** (re-ejecuta) las partes del árbol de UI que dependen del estado que cambió. A esto se le llama **recomposición inteligente**.

---

## `@Preview` — el equivalente a Storybook

Blazor no tiene un sistema de previsualizacion de componentes integrado. Compose tiene `@Preview`: una anotación que permite ver el composable en IntelliJ/Android Studio **sin ejecutar la app**.

```kotlin
@Preview(showBackground = true)
@Composable
private fun WorkspacesScreenPreview() {
    ZenTrackTheme { WorkspacesScreen(onWorkspaceSelected = {}) }
}
```

En ZenTrack siempre añadimos `@Preview` a los composables presentacionales para poder revisarlos visualmente durante el desarrollo.

---

## Estado — `StateFlow` vs `INotifyPropertyChanged`

En Blazor, el componente re-renderiza automáticamente cuando llamas a `StateHasChanged()` o cuando cambia una propiedad `[Parameter]`. En Compose, el estado que desencadena recomposición debe ser **observable** con tipos específicos.

El tipo principal que usamos en ZenTrack es `StateFlow` de Kotlin Coroutines:

```csharp
// Blazor — state en el componente
@code {
    private BoardState state = new();

    private async Task LoadTasks()
    {
        state = state with { IsLoading = true };
        StateHasChanged();  // fuerza re-render manual
        var tasks = await taskRepository.GetTasksAsync();
        state = state with { Tasks = tasks, IsLoading = false };
        StateHasChanged();
    }
}
```

```kotlin
// Compose — state en el ViewModel (fuera del composable)
class BoardViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    fun loadTasks(workspaceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tasks = taskRepository.getTasksByWorkspace(workspaceId)
            _state.update { it.copy(tasks = tasks, isLoading = false) }
        }
    }
}

// En el composable — se suscribe al StateFlow
@Composable
fun BoardScreen(viewModel: BoardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // "state" es un State<BoardState> — Compose se recompone cuando cambia
    Text(if (state.isLoading) "Cargando..." else "Tareas: ${state.tasks.size}")
}
```

| Blazor | Compose | Qué hace |
|---|---|---|
| `StateHasChanged()` | Cambio en `MutableStateFlow` | Dispara re-render/recomposición |
| `[Parameter]` | Parámetros del `@Composable` | Datos que llegan de fuera |
| Estado local del componente | `remember { mutableStateOf(...) }` | Estado local del composable |
| `INotifyPropertyChanged` | `StateFlow` / `MutableState` | Estado observable |
| `@bind` (two-way binding) | No existe — flujo unidireccional (MVI) | Actualización de estado |

---

## El patrón MVI — por qué no MVVM

En .NET el patrón dominante es **MVVM**: el ViewModel expone propiedades observables y la vista hace two-way binding. En Compose el patrón recomendado es **MVI** (Model-View-Intent) porque Compose es unidireccional por diseño.

La diferencia conceptual:

```
MVVM (Blazor/WPF):  View  ↔  ViewModel  (two-way binding)
MVI  (Compose):     View  →  ViewModel  (evento)
                    View  ←  ViewModel  (estado)
```

En ZenTrack cada pantalla define tres tipos:

```kotlin
// BoardState.kt — instantánea inmutable de lo que ve el usuario (equivale al "Model" en MVVM)
data class BoardState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// BoardEvent.kt — intenciones del usuario (equivale a Commands en CQRS)
sealed class BoardEvent {
    data class LoadTasks(val workspaceId: String) : BoardEvent()
    data class MoveTask(val taskId: String, val newStatusId: String) : BoardEvent()
}

// BoardEffect.kt — side-effects de un solo uso: navegación, toasts, etc.
// (no forman parte del estado porque no deben reproducirse si el composable se recrea)
sealed class BoardEffect {
    data class ShowError(val message: String) : BoardEffect()
    data class NavigateToDetail(val taskId: String) : BoardEffect()
}
```

El flujo completo:

```kotlin
// El Composable solo lee estado y despacha eventos — nunca muta directamente
@Composable
fun BoardScreen(viewModel: BoardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Consume effects una sola vez (equivale a un one-shot en Rx o Task.CompletedTask)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardEffect.ShowError       -> { /* mostrar snackbar */ }
                is BoardEffect.NavigateToDetail -> { /* navegar */ }
            }
        }
    }

    BoardContent(state = state, onEvent = viewModel::onEvent)
}

// El ViewModel procesa el evento y actualiza el estado
fun onEvent(event: BoardEvent) {
    when (event) {
        is BoardEvent.LoadTasks -> loadTasks(event.workspaceId)
        is BoardEvent.MoveTask  -> moveTask(event.taskId, event.newStatusId)
    }
}
```

### Por qué `BoardEffect` y no poner todo en `BoardState`

En Blazor puedes acumular mensajes de error en el estado del componente y limpiarlos cuando quieras. En Compose hay un problema: si el composable se destruye y se recrea (p.ej. al rotar la pantalla), reconstruye el estado desde el `StateFlow` — y si el error está en el estado, aparece de nuevo.

Los `Effect` son eventos de canal (`Channel`) que se consumen **una sola vez** y desaparecen. Úsalos para: navegación, mostrar un toast/snackbar, abrir un diálogo.

---

## ViewModel — ciclo de vida y `viewModelScope`

En Blazor el componente gestiona su propio ciclo de vida (`OnInitializedAsync`, `Dispose`). En Android, el `ViewModel` sobrevive a las rotaciones de pantalla — su ciclo de vida está ligado a la pantalla (destino de navegación), no a la `Activity`.

```csharp
// Blazor — ciclo de vida en el componente
@implements IDisposable

@code {
    protected override async Task OnInitializedAsync()
    {
        await LoadTasks();
    }

    public void Dispose() { /* cancelar subscripciones */ }
}
```

```kotlin
// Compose — ciclo de vida gestionado por ViewModel
class BoardViewModel(...) : ViewModel() {

    // viewModelScope se cancela automáticamente cuando el ViewModel se destruye
    // No hay que hacer Dispose() manualmente
    fun loadTasks(workspaceId: String) {
        viewModelScope.launch {
            // Esta coroutine se cancela sola si el usuario sale de la pantalla
        }
    }

    override fun onCleared() {
        super.onCleared()
        // equivalente a Dispose() — raramente necesario porque viewModelScope ya se cancela
    }
}
```

`viewModelScope` es el equivalente a un `CancellationToken` que se cancela automáticamente.

---

## Navigation Compose vs Blazor Router

Blazor define rutas con `@page "/workspaces"` directamente en el componente. Navigation Compose centraliza todas las rutas en un `NavHost`.

```razor
<!-- Blazor — ruta declarada en el propio componente -->
@page "/board/{WorkspaceId}"

@code {
    [Parameter] public string WorkspaceId { get; set; }
}
```

```kotlin
// Compose — rutas centralizadas en ZenTrackNavGraph.kt

// 1. Destinos tipados (evita strings literales dispersos)
sealed class AppDestinations(val route: String) {
    data object Workspaces : AppDestinations("workspaces")
    data object Board : AppDestinations("board/{workspaceId}") {
        const val ARG = "workspaceId"
        fun route(workspaceId: String) = "board/$workspaceId"
    }
}

// 2. NavHost — el equivalente al <Router> de Blazor
@Composable
fun ZenTrackNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestinations.Workspaces.route) {

        composable(AppDestinations.Workspaces.route) {
            WorkspacesScreen(
                onWorkspaceSelected = { id -> navController.navigate(AppDestinations.Board.route(id)) }
            )
        }

        composable(
            route = AppDestinations.Board.route,
            arguments = listOf(navArgument(AppDestinations.Board.ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString(AppDestinations.Board.ARG) ?: return@composable
            BoardScreen(workspaceId = workspaceId, onTaskSelected = { taskId -> /* navegar */ })
        }
    }
}
```

| Blazor Router | Navigation Compose | Notas |
|---|---|---|
| `@page "/route"` | `composable("route") { }` en `NavHost` | Registrar un destino |
| `NavigationManager.NavigateTo("/board/1")` | `navController.navigate("board/1")` | Navegar programáticamente |
| `[Parameter] string Id` | `backStackEntry.arguments?.getString("id")` | Leer parámetros de ruta |
| `<Router>` + `<RouteView>` | `NavHost(navController, startDestination)` | El router central |
| `NavigationManager.Uri` | `navController.currentDestination` | Destino actual |
| Back button del browser | Back button Android — gestionado por `NavHost` automáticamente | Navegación atrás |

**Regla en ZenTrack:** las rutas se definen solo en `AppDestinations` — nunca strings literales en el código de pantallas o ViewModels.

---

## Koin en Android vs DI en .NET

En .NET la inyección de dependencias está integrada en el framework (`IServiceCollection`, `[Inject]` en Blazor). En Android usamos **Koin** — una librería DI ligera para Kotlin.

```csharp
// .NET — registro en Program.cs
builder.Services.AddScoped<ITaskRepository, TaskRepository>();
builder.Services.AddScoped<BoardViewModel>();

// Blazor — inyección en el componente
@inject ITaskRepository TaskRepository
```

```kotlin
// Koin — registro en AndroidAppModule.kt
val androidAppModule = module {
    single<TaskRepository> { TaskRepositoryImpl(get()) }  // get() resuelve las dependencias
    viewModelOf(::BoardViewModel)                          // registra el ViewModel
}

// Koin — arranque en ZenTrackApp.kt (Application)
startKoin {
    androidLogger()
    androidContext(this@ZenTrackApp)
    modules(androidAppModule)
}

// Compose — inyección en el composable de pantalla
@Composable
fun BoardScreen(viewModel: BoardViewModel = koinViewModel()) { ... }
```

| .NET / Blazor | Koin (Android) | Notas |
|---|---|---|
| `builder.Services.AddScoped<T>()` | `single { T() }` | Singleton en Koin |
| `builder.Services.AddTransient<T>()` | `factory { T() }` | Nueva instancia cada vez |
| `[Inject]` en Blazor | `koinViewModel()` en composable | Inyectar ViewModel |
| `IServiceProvider.GetService<T>()` | `get<T>()` dentro del módulo | Resolver dependencia manual |
| `IServiceCollection` | `module { }` | Contenedor de registro |

---

## Resumen: tabla de equivalencias

| Blazor / .NET | Jetpack Compose / Android | Notas |
|---|---|---|
| Archivo `.razor` | Función `@Composable` | Unidad de UI |
| `StateHasChanged()` | Cambio en `MutableStateFlow` | Dispara re-render |
| `[Parameter]` | Parámetro del composable | Datos de entrada |
| Estado local del componente | `remember { mutableStateOf() }` | Estado local |
| MVVM con two-way binding | MVI con unidirectional data flow | Patrón de estado |
| ViewModel / code-behind | `ViewModel` + `StateFlow` | Gestión de estado |
| `OnInitializedAsync()` | `LaunchedEffect(Unit) { }` | Código al inicializar |
| `IDisposable.Dispose()` | `onCleared()` / `viewModelScope` | Limpieza de recursos |
| `@page "/route/{Id}"` | `composable("route/{id}")` en NavHost | Definir destino |
| `NavigationManager.NavigateTo()` | `navController.navigate()` | Navegar |
| `[Inject]` | `koinViewModel()` / `get()` | Inyección de dependencias |
| `IServiceCollection` | Módulo Koin `module { }` | Registro de dependencias |
