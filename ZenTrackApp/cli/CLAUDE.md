# CLAUDE.md — cli/ (Kotlin/JVM + Clikt)

## QUÉ hace este módulo
CLI de línea de comandos escrita en Kotlin/JVM usando Clikt + jline3. Funciona como una sesión interactiva REPL (al igual que Claude Code): al ejecutar `zentrack` sin argumentos se abre un prompt persistente con historial y edición de línea; con argumentos (`zentrack tasks list`) actúa como herramienta one-shot para scripts. El estado de sesión (token JWT, workspace/proyecto activo) se mantiene en memoria en `ReplSession`. Depende de `shared/` (target JVM) para los modelos, DTOs y el cliente Ktor.

## POR QUÉ estas reglas existen
Clikt declara los comandos de forma declarativa con propiedades delegadas; mezclar lógica de negocio dentro de `run()` hace los comandos intesteables y difíciles de componer. Separar la lógica en servicios/use cases (provenientes de `shared`) mantiene los comandos como adaptadores de presentación, igual que las rutas en `server/`.

## CÓMO estructurar el código

### Estructura de directorios

```
src/main/kotlin/me/dcueto/zentrackapp/cli/
├── Main.kt              → Entry point: REPL loop (jline3) + buildRootCommand factory
├── ReplSession.kt       → Estado de sesión en memoria: token, workspace/proyecto activo, prompt dinámico
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

### REPL y modo one-shot (Main.kt)

El CLI tiene dos modos de operación:

| Invocación | Comportamiento |
|---|---|
| `zentrack` | Abre el REPL interactivo con prompt dinámico |
| `zentrack tasks list` | One-shot: ejecuta y sale (sirve para scripts) |

```kotlin
fun main(args: Array<String>) {
    val session = ReplSession()

    if (args.isNotEmpty()) {
        // One-shot: comportamiento clásico, System.exit() normal al terminar
        buildRootCommand(session).main(args)
        return
    }

    // REPL: jline3 gestiona historial, edición, Ctrl+C / Ctrl+D
    val reader = LineReaderBuilder.builder()
        .terminal(TerminalBuilder.builder().system(true).build())
        .history(DefaultHistory())
        .parser(DefaultParser())
        .variable(LineReader.HISTORY_FILE, "~/.zentrack/history")
        .build()

    while (true) {
        val line = try { reader.readLine(session.prompt()) }
            catch (e: EndOfFileException) { break }
            catch (e: UserInterruptException) { continue }
        if (line.trim() == "exit") break
        val words = parser.parse(line, line.length).words()
        buildRootCommand(session, replMode = true).main(words.toTypedArray())
    }
}
```

`buildRootCommand(session, replMode = true)` pasa `replMode = true` a `ZenTrack`, que configura `exitProcess = { _ -> }` para que `--help` y errores no maten el proceso REPL.

### ReplSession

`ReplSession` almacena el estado mutable de la sesión en memoria:

```kotlin
data class ReplSession(
    var token: String? = null,           // JWT activo
    var refreshToken: String? = null,    // para renovación automática
    var userEmail: String? = null,
    var activeWorkspaceName: String? = null,
    var activeProjectKey: String? = null
) {
    val isAuthenticated: Boolean get() = token != null
    fun prompt() = "ZenTrack [$activeWorkspaceName/$activeProjectKey] > "
}
```

Los comandos leen y escriben `session` para cambiar contexto (workspace activo, token tras `auth login`, etc.).

### Wiring de subcomandos

- **SIEMPRE** agrupa subcomandos por dominio en un `CliktCommand` padre (ej. `TaskCommands` agrupa `list`, `create`, `move`).
- El root command (`ZenTrack`) no hace nada en `run()` — solo sirve de punto de entrada.
- Usa `import com.github.ajalt.clikt.core.context as cliktContext` para evitar el conflicto con la función `context()` del stdlib de Kotlin 2.3+.

### Inyección de Dependencias

- Usa Koin igual que en `shared/`. El módulo CLI (`di/cliModule`) extiende los módulos de `shared`.
- En tests, instancia los comandos directamente pasando fakes como constructor params — no levantes Koin en tests unitarios.

### Autenticación y Configuración

- Al arrancar el REPL, se carga `~/.zentrack/credentials.json` en `ReplSession`. Durante la sesión, el token vive en memoria — no se relee el fichero en cada comando.
- La URL del servidor se lee de `~/.zentrack/config.json` o de la variable de entorno `ZENTRACK_API_URL`.
- **NUNCA** se pasan tokens como flags en cada comando ni como argumentos posicionales.
- **PROHIBIDO** guardar el token en `~/.bash_history`.

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
