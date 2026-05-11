# 08 — Clikt CLI para devs .NET

> Cómo entender Clikt (la librería de CLI de JetBrains) si conoces `System.CommandLine` o las herramientas CLI de .NET.

---

## ¿Qué es Clikt?

**Clikt** (Command Line Interface for Kotlin) es la librería estándar para construir CLIs en Kotlin. Es el equivalente a `System.CommandLine` de .NET o a librerías como `CommandLineParser`/`Cocona`.

```bash
# Lo que construimos:
zentrack tasks list --workspace ws-1
zentrack tasks create --workspace ws-1 --title "Fix bug"
zentrack workspaces list
zentrack sprints list --workspace ws-1
```

---

## Comandos raíz y subcomandos — árbol de comandos

En `System.CommandLine`, construyes un árbol de `RootCommand` → `Command` → subcomandos. Clikt usa el mismo concepto pero con **clases** en lugar de objetos configurados imperativamente.

```csharp
// .NET System.CommandLine
var rootCommand = new RootCommand("ZenTrack CLI");

var tasksCommand = new Command("tasks", "Gestiona tareas");
var listCommand = new Command("list", "Lista tareas");
var workspaceOption = new Option<string>("--workspace", "ID del workspace") { IsRequired = true };
listCommand.AddOption(workspaceOption);
listCommand.SetHandler(workspaceId => Console.WriteLine($"Lista tareas de {workspaceId}"), workspaceOption);

tasksCommand.AddCommand(listCommand);
rootCommand.AddCommand(tasksCommand);
return await rootCommand.InvokeAsync(args);
```

```kotlin
// Clikt — cada comando es una clase
class ZenTrack : CliktCommand(name = "zentrack") {
    override fun help(context: Context) = "ZenTrack CLI"
    override fun run() = Unit  // comando raíz — solo agrupa, no hace nada
}

class TaskCommands : CliktCommand(name = "tasks") {
    override fun help(context: Context) = "Gestiona tareas de un workspace"
    override fun run() = Unit  // grupo — solo agrupa subcomandos
}

class TaskListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "Lista las tareas de un workspace"
    private val workspaceId by option("--workspace", "-w").required()

    override fun run() {
        echo("Tareas del workspace $workspaceId")
    }
}

// Wiring en Main.kt
fun main(args: Array<String>) = ZenTrack()
    .subcommands(
        TaskCommands().subcommands(
            TaskListCommand(),
            TaskCreateCommand()
        ),
        WorkspaceCommands().subcommands(WorkspaceListCommand()),
        SprintCommands().subcommands(SprintListCommand())
    )
    .main(args)
```

| `System.CommandLine` | Clikt | Notas |
|---|---|---|
| `new RootCommand("desc")` | `class X : CliktCommand(name = "x")` | Cada comando es una clase |
| `rootCommand.AddCommand(sub)` | `.subcommands(SubCmd())` | Añadir subcomandos |
| `command.SetHandler(handler)` | `override fun run()` | Lógica del comando |
| `await rootCommand.InvokeAsync(args)` | `.main(args)` | Punto de entrada |

---

## Opciones y argumentos — `option()` y `argument()`

En `System.CommandLine` defines opciones con `new Option<T>("--name")`. En Clikt usas **delegados de propiedad** — la opción se declara como una propiedad de la clase y se accede directamente en `run()`.

```csharp
// .NET — opción declarada e inyectada vía handler
var workspaceOption = new Option<string>("--workspace", "-w") { IsRequired = true };
command.AddOption(workspaceOption);
command.SetHandler(ws => { /* ws disponible aquí */ }, workspaceOption);
```

```kotlin
// Clikt — delegado de propiedad: declara y accede en la misma clase
class TaskListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "Lista tareas"

    // Opción obligatoria — falla con mensaje de error si no se proporciona
    private val workspaceId by option("--workspace", "-w", help = "ID del workspace")
        .required()

    // Opción con valor por defecto
    private val format by option("--format", "-f", help = "Formato de salida")
        .default("table")

    // Opción de tipo enum
    private val priority by option("--priority")
        .enum<TaskPriority>()
        .default(TaskPriority.MEDIUM)

    // Argumento posicional (sin --nombre, solo valor)
    private val taskId by argument(help = "Display ID de la tarea (ej. ZTK-42)")

    override fun run() {
        echo("Workspace: $workspaceId | Format: $format | Task: $taskId")
    }
}
```

La diferencia clave: en `System.CommandLine` el valor de la opción llega como parámetro del handler. En Clikt, la opción ES una propiedad de la clase — disponible directamente en `run()` sin necesidad de parámetros.

---

## `echo()` — siempre en lugar de `println()`

Clikt requiere usar `echo()` en lugar de `println()`. La razón es que `echo()` escribe al stream de salida que Clikt controla — lo que permite redirigir la salida en tests sin capturar `System.out`.

```kotlin
// PROHIBIDO
override fun run() {
    println("Resultado")          // rompe los tests con CliktCommand.test()
}

// CORRECTO
override fun run() {
    echo("Resultado")             // escribe al stream de Clikt
    echo("Error", err = true)     // escribe a stderr
}
```

En .NET esto es equivalente a usar `Console.Out.WriteLine()` en lugar de `Console.WriteLine()` cuando quieres poder redirigir la salida en tests.

---

## Errores de negocio — cómo reportarlos

```kotlin
override fun run() {
    val result = workspaceService.findById(workspaceId)
    if (result == null) {
        echo("Workspace $workspaceId no encontrado", err = true)
        currentContext.exit(1)   // código de salida 1 = error
        return
    }
    echo(result.name)
}
```

`CliktError` y `Abort` son las dos excepciones que Clikt entiende:
- `throw CliktError("mensaje")` — imprime el mensaje y sale con código 1
- `throw Abort()` — sale silenciosamente con código 1

En ZenTrack se prefiere el patrón `echo(err = true) + exit(1)` por ser más explícito y testeable.

---

## Estructura de carpetas en `cli/`

```
cli/src/main/kotlin/me/dcueto/zentrackapp/cli/
├── Main.kt              → ZenTrack root command + wiring de subcomandos
├── commands/
│   ├── TaskCommands.kt      → TaskCommands (grupo) + TaskListCommand, TaskCreateCommand, TaskShowCommand
│   ├── WorkspaceCommands.kt → WorkspaceCommands (grupo) + WorkspaceListCommand, WorkspaceCreateCommand
│   └── SprintCommands.kt    → SprintCommands (grupo) + SprintListCommand, SprintCreateCommand
├── output/              → Formateadores de salida: tabla, JSON (Fase 2)
└── di/
    └── CliModule.kt     → Módulo Koin del CLI (servicios — Fase 2)
```

---

## Compilar y usar el CLI

```bash
# Compila e instala el binario en cli/build/install/cli/bin/
./gradlew :cli:installDist

# Ejecutar
./cli/build/install/cli/bin/cli --help
./cli/build/install/cli/bin/cli tasks list --workspace ws-1
./cli/build/install/cli/bin/cli tasks create --workspace ws-1 --title "Fix bug"
./cli/build/install/cli/bin/cli workspaces list

# Generar ZIP distribuible
./gradlew :cli:distZip
```

---

## Tests de comandos

Clikt 4.x+ incluye `CliktCommand.test()` para capturar la salida sin ejecutar el proceso:

```kotlin
class TaskListCommandTest {
    private val fakeService = FakeTaskService()
    private val command = TaskListCommand(fakeService)

    @Test
    fun `list prints task id and title`() {
        fakeService.seed(listOf(Task(displayId = "ZTK-1", title = "Fix bug")))

        val result = command.test("--workspace ws-1")

        assertTrue(result.output.contains("ZTK-1"))
        assertTrue(result.output.contains("Fix bug"))
    }
}
```

En .NET, el equivalente sería redirigir `Console.Out` antes de invocar el comando y verificar lo capturado — o usar el `InvocationContext` de `System.CommandLine` en modo test.

```bash
./gradlew :cli:test
```

---

## Modo REPL con jline3

ZenTrack CLI es un REPL interactivo (como Claude Code). Al ejecutar `zentrack` sin args, abre un prompt persistente con historial y edición de línea; con args actúa como herramienta one-shot para scripts.

En .NET, esto sería equivalente a `Spectre.Console`'s `AnsiConsole.Prompt` + un bucle `ReadLine()`, pero jline3 añade historial persistente, navegación con flechas y autocompletado.

### Las dos piezas clave

**`ReplSession`** — estado en memoria durante la sesión:

```kotlin
data class ReplSession(
    var token: String? = null,
    var activeWorkspaceName: String? = null,
    var activeProjectKey: String? = null
) {
    fun prompt() = if (activeWorkspaceName != null)
        "ZenTrack [$activeWorkspaceName] > "
    else "ZenTrack > "
}
```

**jline3** — gestiona el terminal, el historial y el parsing de línea:

```kotlin
val terminal = TerminalBuilder.builder().system(true).build()
val reader = LineReaderBuilder.builder()
    .terminal(terminal)
    .history(DefaultHistory())
    .parser(DefaultParser())  // maneja comillas: task create --title "Fix bug"
    .variable(LineReader.HISTORY_FILE, "~/.zentrack/history")
    .build()

while (true) {
    val line = try {
        reader.readLine(session.prompt())
    } catch (e: EndOfFileException) { break }   // Ctrl+D → salir
      catch (e: UserInterruptException) { continue }  // Ctrl+C → nueva línea

    val words = DefaultParser().parse(line, line.length).words()
    buildRootCommand(session, replMode = true).main(words.toTypedArray())
}
```

### Modo dual — REPL vs. script

```kotlin
fun main(args: Array<String>) {
    val session = ReplSession()
    if (args.isNotEmpty()) {
        buildRootCommand(session).main(args)  // one-shot: System.exit() normal
        return
    }
    // ... REPL loop
}
```

---

## Almacenamiento de credenciales — `CredentialStore` y `TokenManager`

ZenTrack CLI persiste el JWT y el refresh token en `~/.zentrack/credentials.json`. Al arrancar, los carga en `ReplSession`; durante la sesión, el token vive en memoria.

### CredentialStore — I/O de fichero con `kotlinx.serialization`

```kotlin
@Serializable
data class PersistedCredentials(val token: String, val refreshToken: String, val email: String)

object CredentialStore {
    private val file = File(System.getProperty("user.home"), ".zentrack/credentials.json")

    fun save(credentials: PersistedCredentials) {
        file.parentFile.mkdirs()
        file.writeText(Json.encodeToString(credentials))
    }

    fun load(): PersistedCredentials? = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<PersistedCredentials>(file.readText())
    }.getOrNull()

    fun clear() { file.delete() }
}
```

En .NET esto sería `JsonSerializer.Serialize` / `JsonSerializer.Deserialize` con `File.WriteAllText` / `File.ReadAllText`. El patrón `runCatching { }.getOrNull()` es el equivalente al `try { } catch { return null; }` en C#.

### TokenManager — verificar expiración y renovar

Los JWTs son `header.payload.signature` en Base64URL. El campo `exp` del payload es un timestamp Unix en segundos. Sin librería JWT:

```kotlin
fun isExpired(token: String): Boolean = try {
    val padding = "=".repeat((4 - token.split(".")[1].length % 4) % 4)
    val payload = Base64.getUrlDecoder().decode(token.split(".")[1] + padding)
    val exp = Json.parseToJsonElement(String(payload)).jsonObject["exp"]?.jsonPrimitive?.longOrNull ?: return true
    System.currentTimeMillis() / 1000 >= exp - 30  // 30s buffer
} catch (e: Exception) { true }
```

Si está expirado, `TokenManager.refreshIfNeeded(session)` llama a `POST /api/auth/refresh` con el refresh token y actualiza `ReplSession` + `CredentialStore`. Usa `runBlocking { client.post(...).body<AuthResponse>() }` para hacer la llamada asíncrona de Ktor en contexto síncrono del CLI.

En .NET, el equivalente sería usar `JwtSecurityTokenHandler` para el parsing y `HttpClient.PostAsJsonAsync` con `.GetAwaiter().GetResult()` para la llamada blocking.

### Carga al arrancar

```kotlin
fun main(args: Array<String>) {
    val session = ReplSession()
    CredentialStore.load()?.let { creds ->
        session.token = creds.token
        session.refreshToken = creds.refreshToken
        session.userEmail = creds.email
    }
    // ... resto del REPL
}
```

---

### Prevenir System.exit() en el REPL

Cuando Clikt muestra `--help` llama a `exitProcess(0)`, que mataría el proceso REPL. Para evitarlo:

```kotlin
// IMPORTANTE: importar con alias para evitar conflicto con kotlin.context() de Kotlin 2.3+
import com.github.ajalt.clikt.core.context as cliktContext

class ZenTrack(replMode: Boolean = false) : CliktCommand(name = "zentrack") {
    init {
        if (replMode) this.cliktContext { exitProcess = { _ -> } }
    }
}
```

En .NET `System.CommandLine` esto no es necesario porque la librería no llama a `Environment.Exit()` directamente al mostrar ayuda.
