# FEATURE CONTEXT: Nueva Pantalla Android (Jetpack Compose)

Contexto específico para implementar **una pantalla completa** en la app Android. Úsalo junto al prompt de la tarea.

## Archivos a leer antes de implementar

```
androidApp/CLAUDE.md                          → reglas de arquitectura del módulo
androidApp/src/main/.../ui/                   → pantallas existentes (referencia de patrón)
androidApp/src/main/.../viewmodel/            → ViewModels existentes
shared/src/commonMain/.../model/              → modelos de dominio disponibles
```

## Estructura obligatoria (MVI)

```
ScreenState (data class, inmutable)
  + ScreenEvent (sealed interface)
    → ViewModel (consume events, emite StateFlow<ScreenState>)
      → Screen @Composable (observa state, dispara events)
```

Nunca pongas lógica de negocio en el Composable. El Composable solo renderiza estado y dispara eventos.

## Checklist de implementación

- [ ] `ScreenState` definido como `data class` con todos los campos de UI necesarios
- [ ] `ScreenEvent` definido como `sealed interface` con todos los eventos de usuario
- [ ] `ViewModel` con `StateFlow<ScreenState>` y función `onEvent(event: ScreenEvent)`
- [ ] ViewModel inyectado con Koin (no instanciado manualmente)
- [ ] Composable recibe `state` y `onEvent` como parámetros (sin dependencia directa del ViewModel)
- [ ] Composable anotado con `@Preview` para el estado principal
- [ ] Navegación registrada en el NavGraph correspondiente
- [ ] Colores y dimensiones usando tokens de Material 3 (no hardcodeados)
- [ ] Estados de loading y error contemplados en `ScreenState`
- [ ] Verificación: `./gradlew :androidApp:assembleDebug`

## Patrón de referencia

```kotlin
// ScreenState
data class TaskListState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ScreenEvent
sealed interface TaskListEvent {
    data object LoadTasks : TaskListEvent
    data class SelectTask(val taskId: String) : TaskListEvent
}

// ViewModel
class TaskListViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val _state = MutableStateFlow(TaskListState())
    val state: StateFlow<TaskListState> = _state.asStateFlow()

    fun onEvent(event: TaskListEvent) { /* ... */ }
}
```
