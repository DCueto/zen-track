---
name: kmp-core-feature
description: >
  Manual de procedimientos para añadir o modificar modelos de datos, DTOs y
  peticiones HTTP con Ktor Client en el módulo `shared/` de ZenTrack (Kotlin
  Multiplatform). Carga esta habilidad siempre que la tarea implique: crear o
  cambiar una entidad de dominio en `commonMain`, añadir o actualizar un DTO
  `@Serializable`, definir o modificar una función de Ktor Client, o introducir
  un patrón `expect/actual` nuevo.
  También aplica cuando el usuario mencione "modelo compartido", "DTO",
  "repositorio de red", "Ktor Client", "shared KMP", "androidMain", "jvmMain"
  o "compartir lógica entre Android y servidor".
---

# KMP Core Feature — Procedimientos para shared/

## Contexto de Arquitectura

`shared/` es el módulo KMP central de ZenTrack. Compila a dos targets:
- **JVM** → consumido por `server/` y `cli/`.
- **Android** → consumido por `androidApp/`.

`commonMain` es la **única fuente de verdad** de todos los tipos de dominio. Cualquier modelo o DTO que exista aquí se propaga automáticamente a todos los clientes mediante compilación, sin duplicación manual.

```
shared/commonMain  (@Serializable data class Task, Workspace…)
      ↓  jvmMain (CIO engine)       ↓  androidMain (OkHttp engine)
   server/ + cli/                  androidApp/
```

---

## Reglas Absolutas (no negociables)

Estas reglas derivan de `shared/CLAUDE.md` y `AGENTS.md`. Violarlas rompe uno o más targets de compilación.

### commonMain — Pureza de Plataforma

- **PROHIBIDO** importar `android.*`, `java.*` o `androidx.*` en cualquier archivo bajo `commonMain/`.
- **PERMITIDO** exclusivamente: `kotlin.*`, `kotlinx.*` (serialization, coroutines, datetime) y dependencias declaradas en `commonMain.dependencies` del `build.gradle.kts`.
- Si necesitas una API de plataforma (UUID, reloj de sistema, logging nativo), usa `expect/actual` o una `interface` inyectada con Koin desde `jvmMain`/`androidMain`.

### expect/actual — Alcance Estricto

`expect/actual` es **solo** para adaptadores de plataforma simples. No pongas lógica de negocio en bloques `actual`.

```kotlin
// commonMain — CORRECTO: wrapper de plataforma simple
expect fun generateUuid(): String

// jvmMain — CORRECTO: delega a la API nativa, cero lógica
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// androidMain — CORRECTO
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// PROHIBIDO — lógica de dominio dentro de un actual
actual fun generateUuid(): String {
    val prefix = computePrefix()   // ← lógica de negocio, muévela a commonMain
    return "$prefix-${java.util.UUID.randomUUID()}"
}
```

Si la implementación de plataforma es compleja, define una `interface` en `commonMain` e inyéctala con Koin:

```kotlin
// commonMain/di/ — contrato
interface DateTimeProvider {
    fun nowIso8601(): String
}

// jvmMain/ — implementación
class JvmDateTimeProvider : DateTimeProvider {
    override fun nowIso8601() = java.time.Instant.now().toString()
}
```

### Dependencias — Solo desde libs.versions.toml

- **PROHIBIDO** hardcodear versiones en `build.gradle.kts`. Toda versión va en `gradle/libs.versions.toml`.

---

## Flujo de Trabajo por Tipo de Cambio

### 1. Añadir o Modificar un Modelo de Dominio

Los modelos de dominio son entidades persistibles que representan conceptos del negocio (Task, Workspace, Project, Sprint…).

**Ubicación:** `shared/src/commonMain/kotlin/model/`

**Paso 1 — Leer el esquema BD** en `docs/SDD/PLAN.md` (sección 2) para entender las columnas y relaciones antes de definir la clase.

**Paso 2 — Escribir la clase:**

```kotlin
// commonMain/model/Task.kt
@Serializable
data class Task(
    val id: String,                      // UUID como String (portable entre targets)
    val projectId: String,
    val taskNumber: Int,
    val displayId: String,               // inmutable tras creación: "[KEY]-[N]"
    val title: String,
    val description: String? = null,
    val statusId: String,
    val priority: TaskPriority,
    val sprintId: String? = null,        // null = backlog
    val gitBranchName: String? = null,   // null es estado válido, no error
    val assignees: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: String,               // ISO-8601 como String
    val updatedAt: String
)

@Serializable
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }
```

**Reglas de modelado:**
- Usa `String` para UUIDs (no `java.util.UUID` — no es portable a Android target sin `androidMain`).
- Usa `String` para fechas ISO-8601 (no `java.time.*`). Si necesitas aritmética de fechas, declara `expect fun` o usa `kotlinx-datetime`.
- Los campos opcionales usan `val campo: Tipo? = null`. No uses `lateinit var`.
- `displayId` es inmutable: nunca lo recalcules en el cliente; confía en el valor que devuelve el servidor.

---

### 2. Añadir o Modificar un DTO

Los DTOs son contratos de la API (request/response). Son efímeros — no representan el estado del dominio, solo el payload de una llamada HTTP.

**Ubicación:** `shared/src/commonMain/kotlin/dto/`

**Convención de nombres:**
- Request body: `[Acción][Entidad]Request` → `CreateTaskRequest`
- Response body: `[Entidad]Response` → `TaskResponse`
- Response paginado: `PagedResponse<T>`

```kotlin
// commonMain/dto/CreateTaskRequest.kt
@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val statusId: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val sprintId: String? = null,
    val assigneeIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList()
)

// commonMain/dto/TaskResponse.kt
@Serializable
data class TaskResponse(
    val task: Task,
    val projectKey: String
)
```

**Reglas:**
- Todos los DTOs son `@Serializable data class`.
- Los campos opcionales con default no requieren `@Required`; úsalo solo si el servidor rechaza la ausencia del campo en JSON.
- No incluyas lógica de transformación dentro del DTO; si el servidor devuelve un campo que el cliente debe transformar, hazlo en el repositorio.
- No reutilices un DTO de Request como Response ni viceversa, aunque los campos coincidan hoy.

---

### 3. Añadir o Modificar una Petición HTTP con Ktor Client

**Ubicación:** `shared/src/commonMain/kotlin/network/`

**Estructura esperada:**

```
network/
├── ZenTrackApiClient.kt   → instancia de HttpClient + baseUrl + plugins
├── TaskApi.kt             → llamadas HTTP relacionadas con tareas
├── WorkspaceApi.kt        → llamadas HTTP de workspaces
└── ...
```

#### 3a. Configurar el HttpClient (si no existe)

El cliente debe configurarse en `commonMain` usando únicamente motores y plugins KMP. El motor real (`CIO` para JVM, `OkHttp` para Android) se inyecta desde `jvmMain`/`androidMain` mediante Koin.

```kotlin
// commonMain/network/ZenTrackApiClient.kt
class ZenTrackApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
)

// commonMain — expect para la construcción del cliente
expect fun provideHttpClient(): HttpClient
```

```kotlin
// jvmMain/di/NetworkModuleJvm.kt
actual fun provideHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) { bearer { /* token desde Koin */ } }
}

// androidMain/di/NetworkModuleAndroid.kt
actual fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) { bearer { /* token desde Koin */ } }
}
```

#### 3b. Añadir un endpoint

```kotlin
// commonMain/network/TaskApi.kt
class TaskApi(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getTasks(projectId: String): List<Task> =
        client.get("$baseUrl/api/projects/$projectId/tasks").body()

    suspend fun createTask(projectId: String, request: CreateTaskRequest): TaskResponse =
        client.post("$baseUrl/api/projects/$projectId/tasks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateTask(taskId: String, patch: UpdateTaskRequest): Task =
        client.patch("$baseUrl/api/tasks/$taskId") {
            contentType(ContentType.Application.Json)
            setBody(patch)
        }.body()
}
```

**Reglas de red:**
- Toda función de red es `suspend`. Nunca bloquees con `runBlocking` en `commonMain`.
- Usa `.body<T>()` para deserializar la respuesta. El `HttpClient` ya tiene `ContentNegotiation` con `kotlinx.serialization`.
- Maneja solo errores de frontera del sistema: `ClientRequestException` (4xx), `ServerResponseException` (5xx), `IOException`. No catches `Throwable` genérico.
- Las rutas de API deben coincidir exactamente con `docs/SDD/PLAN.md` sección 3. Si necesitas una ruta no documentada, añádela al plan antes de implementarla.

#### 3c. Registrar la API en el módulo Koin

```kotlin
// commonMain/di/NetworkModule.kt
val networkModule = module {
    single { ZenTrackApiClient(get(), get(named("baseUrl"))) }
    single { TaskApi(get(), get(named("baseUrl"))) }
    single { WorkspaceApi(get(), get(named("baseUrl"))) }
}
```

**PROHIBIDO** usar `GlobalContext.get()` o instanciar `HttpClient` directamente en un Composable o en un ViewModel.

---

## Declarar Nuevas Dependencias

Si el cambio requiere una nueva librería en `commonMain` (por ejemplo, `kotlinx-datetime`):

1. Añade la versión a `gradle/libs.versions.toml`:
   ```toml
   [versions]
   kotlinxDatetime = "0.6.0"

   [libraries]
   kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
   ```

2. Añade la dependencia en `shared/build.gradle.kts`:
   ```kotlin
   commonMain.dependencies {
       implementation(libs.kotlinx.datetime)
   }
   ```

3. Si es una dependencia específica de Android, añádela en `androidMain.dependencies`:
   ```kotlin
   androidMain.dependencies {
       implementation(libs.ktor.client.okhttp)
   }
   ```

**PROHIBIDO** hardcodear la versión directamente en `build.gradle.kts`.

---

## Checklist de Validación

Antes de marcar el cambio como completado, verifica cada punto:

```
[ ] No hay imports de android.*, java.* ni androidx.* en commonMain/
[ ] Toda versión de dependencia está en gradle/libs.versions.toml
[ ] Los bloques actual contienen solo adaptadores de plataforma, sin lógica de dominio
[ ] Los modelos y DTOs son data class @Serializable con tipos portables (String para UUID/fecha)
[ ] Las funciones de red son suspend y usan .body<T>() para deserialización
[ ] La ruta HTTP coincide con docs/SDD/PLAN.md sección 3
[ ] El módulo Koin correspondiente registra los nuevos bindings
```

Ejecuta los tests de compilación cruzada para confirmar que ambos targets compilan:

```bash
./gradlew :shared:jvmTest               # Valida target JVM
./gradlew :shared:testDebugUnitTest     # Valida target Android
```

---

## Referencia Rápida de Estructura de Ficheros

```
shared/src/
├── commonMain/kotlin/
│   ├── model/         → Entidades de dominio (@Serializable data class)
│   ├── dto/           → Request/Response DTOs (@Serializable)
│   ├── repository/    → Interfaces de repositorio (contratos, sin implementación)
│   ├── network/       → HttpClient config + clases *Api
│   └── di/            → Módulos Koin comunes (networkModule, etc.)
├── jvmMain/kotlin/    → actual implementations JVM (server + cli)
└── androidMain/kotlin/ → actual implementations Android (androidApp)
```