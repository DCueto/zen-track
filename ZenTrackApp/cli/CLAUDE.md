# CLAUDE.md — cli/ (Kotlin/JVM + Clikt)

## QUÉ hace este módulo
CLI de línea de comandos escrita en Kotlin/JVM usando Clikt. Permite a los desarrolladores interactuar con ZenTrack desde la terminal: consultar tareas, cambiar estados, crear issues y gestionar sprints sin abrir la UI web o Android. Depende de `shared/` (target JVM) para los modelos, DTOs y el cliente Ktor.

## POR QUÉ estas reglas existen
Clikt declara los comandos de forma declarativa con propiedades delegadas; mezclar lógica de negocio dentro de `run()` hace los comandos intesteables y difíciles de componer. Separar la lógica en servicios/use cases (provenientes de `shared`) mantiene los comandos como adaptadores de presentación, igual que las rutas en `server/`.

## CÓMO estructurar el código

### Estructura de directorios

```
src/main/kotlin/me/dcueto/zentrackapp/cli/
├── Main.kt              → Entry point: ZenTrack root command + subcommands wiring
├── commands/            → Un archivo por dominio
│   ├── TaskCommands.kt  → task list, task create, task move, task show
│   ├── WorkspaceCommands.kt
│   └── SprintCommands.kt
├── output/              → Formatters: tabla, JSON, plain text
└── di/                  → Módulo Koin para el CLI (registra servicios, repositorios)
```

### Estructura de un comando Clikt

```kotlin
class TaskListCommand(
    private val taskService: TaskService   // inyectado por Koin o pasado en tests
) : CliktCommand(name = "list", help = "Lista las tareas de un workspace") {

    private val workspaceId by option("--workspace", "-w", help = "ID del workspace")
        .required()

    override fun run() {
        val tasks = taskService.getTasksByWorkspace(workspaceId)
        tasks.forEach { echo("${it.displayId}  ${it.title}") }
    }
}
```

Reglas:
- `run()` solo llama a servicios y formatea salida con `echo()`. **PROHIBIDO** lógica de negocio ni llamadas HTTP directas dentro de `run()`.
- **SIEMPRE** usa `echo()` para la salida estándar. **PROHIBIDO** `println()` en comandos (rompe la testabilidad con `CliktCommand.output`).
- Los errores de negocio se reportan con `echo(message, err = true)` y `currentContext.exit(1)`. **PROHIBIDO** lanzar excepciones no capturadas en `run()`.

### Wiring de subcomandos (Main.kt)

```kotlin
class ZenTrack : CliktCommand(name = "zentrack", help = "ZenTrack CLI") {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    val koin = startKoin { modules(cliModule) }.koin
    ZenTrack()
        .subcommands(
            TaskCommands(koin.get()),
            WorkspaceCommands(koin.get()),
            SprintCommands(koin.get())
        )
        .main(args)
}
```

- **SIEMPRE** agrupa subcomandos por dominio en un `CliktCommand` padre (ej. `TaskCommands` agrupa `list`, `create`, `move`).
- El root command (`ZenTrack`) no hace nada en `run()` — solo sirve de punto de entrada.

### Inyección de Dependencias

- Usa Koin igual que en `shared/`. El módulo CLI (`di/cliModule`) extiende los módulos de `shared`.
- En tests, instancia los comandos directamente pasando fakes como constructor params — no levantes Koin en tests unitarios.

### Autenticación y Configuración

- El token JWT y la URL del servidor se leen de `~/.config/zentrack/config.json` (o variable de entorno `ZENTRACK_TOKEN` / `ZENTRACK_API_URL`). **NUNCA** se pasan como flags en cada comando.
- **PROHIBIDO** guardar el token en `~/.bash_history` ni pasarlo como argumento posicional.

### Compilación y distribución

```bash
./gradlew :cli:installDist
# Genera: cli/build/install/cli/bin/zentrack

./gradlew :cli:distZip
# Genera: cli/build/distributions/cli-1.0.0.zip
```

Tras `installDist`, el binario está disponible en `cli/build/install/cli/bin/zentrack`.

### Tests

#### Tipos de test

- **Unit tests** de comandos individuales: se instancian con fakes, se llama `parse(listOf(...))` y se verifica la salida.
- No hay integration tests de CLI contra servidor real (eso lo cubre `server/api/`).

#### Cómo testear un comando Clikt

```kotlin
class TaskListCommandTest {
    private val fakeTaskService = FakeTaskService()
    private val command = TaskListCommand(fakeTaskService)

    @Test
    fun `list command prints task display id and title`() {
        fakeTaskService.seed(listOf(Task(displayId = "ZTK-1", title = "Setup CI")))

        val output = command.test("--workspace ws-1")

        assertTrue(output.output.contains("ZTK-1"))
        assertTrue(output.output.contains("Setup CI"))
    }
}
```

- Usa `CliktCommand.test(...)` (disponible desde Clikt 4.x) para capturar la salida sin ejecutar el proceso.
- **SIEMPRE** usa **fake implementations** de los servicios, nunca mocks de MockK (por consistencia con el resto del monorepo).
- **PROHIBIDO** `@Ignore` / `@Disabled` sin comentario que explique cuándo se habilitará.

```bash
./gradlew :cli:test
```
