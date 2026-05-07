# 06 — Kotlin Multiplatform (KMP) para devs .NET

> Cómo entender el módulo `shared/` y el patrón `expect/actual` si conoces .NET.

---

## ¿Qué es KMP?

**Kotlin Multiplatform** permite escribir código Kotlin que compila a múltiples targets — JVM, Android, iOS, JS — desde un único módulo. En ZenTrack, `shared/` compila a **JVM** (para `server/` y `cli/`) y **Android** (para `androidApp/`).

La analogía más cercana en .NET son las **librerías de clase portables (PCL)** o las librerías `.NET Standard` — código que puede ser referenciado por proyectos .NET Framework, .NET Core y Xamarin al mismo tiempo. KMP es el equivalente para el ecosistema Kotlin/JVM.

```
.NET Standard library    →  referenciada por .NET Framework + .NET Core + Xamarin
shared/ (KMP)            →  compilada para JVM (server, cli) + Android (androidApp)
```

---

## Source sets — los "proyectos" dentro de shared/

En una librería .NET Standard escribes todo en un único proyecto. En KMP, el código se organiza en **source sets** según el target:

```
shared/src/
├── commonMain/     → Kotlin puro: solo kotlin.*, kotlinx.*
│   ├── model/      → Entidades de dominio
│   ├── dto/        → Request/Response DTOs
│   ├── network/    → Ktor Client + configuración
│   ├── repository/ → Interfaces de repositorio
│   └── di/         → Módulos Koin
├── jvmMain/        → APIs de Java disponibles (java.util.UUID, etc.) — usado por server y cli
└── androidMain/    → APIs Android disponibles (android.*) — usado por androidApp
```

| .NET | KMP | Qué contiene |
|---|---|---|
| Código de la librería .NET Standard | `commonMain/` | Lógica portable, sin APIs de plataforma |
| Código específico de .NET Framework | `jvmMain/` | APIs Java — `java.util.*`, JDBC, etc. |
| Código específico de Xamarin Android | `androidMain/` | APIs Android — `android.*`, OkHttp, etc. |

**Regla crítica de `commonMain`:** está **prohibido** importar `java.*` o `android.*`. Cualquier API nativa debe ir en `jvmMain` o `androidMain` respectivamente. El compilador de Kotlin lo verifica — no es una convención, es una restricción.

---

## `expect/actual` — el equivalente a interfaces con implementaciones por plataforma

En .NET puedes tener una interfaz en la librería compartida e implementaciones distintas por plataforma:

```csharp
// .NET Standard — interfaz compartida
public interface IUuidGenerator { string Generate(); }

// .NET Core — implementación
public class UuidGenerator : IUuidGenerator {
    public string Generate() => Guid.NewGuid().ToString();
}

// Xamarin Android — implementación
public class UuidGenerator : IUuidGenerator {
    public string Generate() => Java.Util.UUID.RandomUUID().ToString();
}
```

En KMP, el mecanismo equivalente es **`expect/actual`**. En lugar de una interfaz y clases que la implementan, declaras una función o clase con `expect` en `commonMain` y proporcionas la implementación con `actual` en cada source set:

```kotlin
// commonMain — "promesa" de que esta función existirá en cada plataforma
expect fun generateUuid(): String

// jvmMain — implementación JVM (para server y cli)
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// androidMain — implementación Android
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()
```

En nuestro caso `generateUuid()` usa la misma implementación en ambos targets porque `java.util.UUID` está disponible en Android. Pero si necesitáramos algo exclusivo de Android (como `android.content.Context`), lo pondríamos solo en `androidMain`.

El caso más importante de `expect/actual` en ZenTrack es `apiBaseUrl`:

```kotlin
// commonMain/network/ApiConfig.kt — declaración
expect val apiBaseUrl: String

// jvmMain/network/ApiConfig.jvm.kt — server y cli leen la variable de entorno
actual val apiBaseUrl: String
    get() = System.getenv("ZENTRACK_API_URL") ?: "http://localhost:8080"

// androidMain/network/ApiConfig.android.kt — emulador → host machine
actual val apiBaseUrl: String = "http://10.0.2.2:8080"
```

`10.0.2.2` es la IP especial del emulador Android que apunta a `localhost` del ordenador de desarrollo. En un dispositivo real o en producción se sobreescribiría con `BuildConfig.API_BASE_URL`.

---

## Ktor Client en KMP — engines por plataforma

Ktor Server (el que usa `server/`) y Ktor Client (el que usa `shared/`) son librerías distintas. El cliente tiene una arquitectura de **engines intercambiables** — el core es multiplataforma pero el motor HTTP concreto varía por plataforma:

```
.NET:  HttpClient  →  (motor integrado, transparent)
Ktor:  HttpClient  →  engine (CIO en JVM, OkHttp en Android, Darwin en iOS)
```

Por eso en `shared/build.gradle.kts` declaramos:
- `commonMain` → `ktor-client-core` (la API genérica, sin motor)
- `jvmMain` → `ktor-client-cio` (motor CIO — coroutine-based, puro Kotlin)
- `androidMain` → `ktor-client-okhttp` (motor OkHttp — librería HTTP estándar de Android)

```kotlin
kotlin {
    jvm()

    // android {} = target Android + configuración Android (AGP 9.0+).
    // Reemplaza tanto androidTarget {} como el bloque android {} de primer nivel.
    android {
        namespace  = "me.dcueto.zentrackapp.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk     = libs.versions.androidMinSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.clientCore)               // API genérica
            implementation(libs.ktor.clientContentNegotiation) // plugin JSON
            implementation(libs.ktor.serializationJson)        // kotlinx.serialization
            implementation(libs.koin.core)                     // DI
        }
        jvmMain.dependencies {
            implementation(libs.ktor.clientCio)    // motor para server y cli
        }
        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp) // motor para androidApp
        }
    }

    jvmToolchain(17)
}
```

Cuando Gradle construye el target JVM, añade `CIO` al classpath. Cuando construye el target Android, añade `OkHttp`. El `HttpClient { }` de `commonMain` detecta automáticamente qué engine está disponible en el classpath — no hay que especificarlo explícitamente en el factory.

```kotlin
// commonMain/network/ZenTrackHttpClient.kt
fun createHttpClient(): HttpClient = HttpClient {   // ← sin especificar engine
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    // El engine (CIO u OkHttp) se resuelve en tiempo de build según el target
}
```

---

## Por qué no usamos DefaultRequest para la URL base

La razón real: en un módulo KMP construido con `com.android.kotlin.multiplatform.library`, el package `io.ktor.client.plugins.defaultrequest` fallaba en tiempo de compilación para el target Android. En lugar de añadir otra dependencia para resolverlo, eliminamos `DefaultRequest` por completo. La arquitectura más limpia es: el factory de `HttpClient` es genérico y agnóstico al transporte; cada repositorio conoce su propia URL y construye rutas completas usando `apiBaseUrl`. Esto evita acoplar el cliente a un servidor concreto.

Nota adicional: la llamada `header()` dentro de un bloque `DefaultRequest` no existe — `DefaultRequestBuilder` usa `headers.append(name, value)`. Así que aunque el import hubiera resuelto, el código habría fallado igualmente.

---

## TokenStore y withAuth() — inyectar el Bearer token

Tras un login o registro exitoso, el token JWT se guarda en un objeto singleton accesible desde `commonMain`:

```kotlin
// commonMain/network/TokenStore.kt
object TokenStore {
    var token: String? = null
}
```

El código que llama al repositorio de autenticación asigna el token:

```kotlin
TokenStore.token = authResponse.token
```

Y `withAuth()` es una función de extensión sobre `HttpRequestBuilder`:

```kotlin
// commonMain/network/ZenTrackHttpClient.kt
fun HttpRequestBuilder.withAuth() {
    TokenStore.token?.let { bearerAuth(it) }
}
```

`HttpRequestBuilder` es el builder que Ktor te da dentro de cualquier lambda `client.get { }` / `client.post { }`. `bearerAuth(token)` es una extensión de Ktor que establece `Authorization: Bearer $token`.

Analogía con .NET: en `HttpClient` harías `client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token)`. En Ktor no existe `DefaultRequestHeaders`, así que añadimos la cabecera en el builder de cada llamada mediante la función de extensión.

Cada llamada en los repositorios la usa así:

```kotlin
client.get("$apiBaseUrl/api/workspaces") { withAuth() }
client.post("$apiBaseUrl/api/workspaces") {
    withAuth()
    setBody(CreateWorkspaceRequest(name))
}
```

---

## Network repositories — implementaciones de red en commonMain

`WorkspaceApiRepository` y `ProjectApiRepository` implementan las interfaces compartidas realizando llamadas HTTP reales:

```kotlin
// commonMain/network/WorkspaceApiRepository.kt
class WorkspaceApiRepository(private val client: HttpClient) : WorkspaceRepository {

    override suspend fun findAllByUser(userId: String): List<Workspace> =
        client.get("$apiBaseUrl/api/workspaces") { withAuth() }
            .body<List<WorkspaceResponse>>()
            .map { it.toDomain() }

    override suspend fun create(name: String, ownerId: String): Workspace =
        client.post("$apiBaseUrl/api/workspaces") {
            withAuth()
            setBody(CreateWorkspaceRequest(name))
        }.body<WorkspaceResponse>().toDomain()

    private fun WorkspaceResponse.toDomain() =
        Workspace(id = id, name = name, ownerId = ownerId, createdAt = createdAt)
}
```

Los parámetros `userId` y `ownerId` se ignoran en la implementación de red porque el servidor extrae la identidad desde el JWT. Las interfaces fueron diseñadas pensando en las implementaciones del lado del servidor (que necesitan el UUID para `SET LOCAL app.user_id`). Las implementaciones cliente simplemente ignoran estos parámetros — la cabecera JWT transporta la identidad.

`.body<T>()` es la forma de Ktor de deserializar el cuerpo de la respuesta al tipo `T` usando ContentNegotiation — equivalente a `response.Content.ReadFromJsonAsync<T>()` de `System.Net.Http.Json`.

El `HttpResponseValidator` de `ZenTrackHttpClient` convierte los códigos de estado HTTP de error en subtipos de `ApiException` antes de que se llame a `.body<T>()`, por lo que los repositorios no necesitan try/catch para errores HTTP.

---

## Koin en KMP

En .NET, `IServiceCollection` se configura en el punto de entrada (`Program.cs`). En KMP, Koin tiene módulos por capa:

```
shared/commonMain/di/SharedModule.kt     → HttpClient, repositorios
androidApp/di/AndroidAppModule.kt        → ViewModels
```

El `sharedModule` registra el cliente HTTP y los repositorios de red:

```kotlin
val sharedModule = module {
    single { createHttpClient() }
    single<WorkspaceRepository> { WorkspaceApiRepository(get()) }
    single<ProjectRepository> { ProjectApiRepository(get()) }
}
```

Estos bindings ya están registrados y disponibles para `androidApp` y `cli`. El `sharedModule` se registra junto al `androidAppModule` en `ZenTrackApp.onCreate()`:

```kotlin
// ZenTrackApp.kt
startKoin {
    androidLogger()
    androidContext(this@ZenTrackApp)
    modules(sharedModule, androidAppModule)   // shared primero, app después
}
```

---

## Cómo consume cada módulo el código de shared/

```kotlin
// server/build.gradle.kts y cli/build.gradle.kts
implementation(projects.shared)   // target JVM — usa jvmMain + commonMain

// androidApp/build.gradle.kts
implementation(projects.shared)   // target Android — usa androidMain + commonMain
```

Gradle resuelve automáticamente qué source sets incluir según el target del módulo que declara la dependencia. No hay que especificar "quiero el target JVM" — se infiere del plugin que tiene el módulo consumidor (`kotlinJvm`, `kotlinAndroid`, etc.).

---

## El plugin `com.android.kotlin.multiplatform.library` — por qué existe

Hasta AGP 8.x, un módulo KMP con target Android necesitaba DOS plugins aplicados al mismo tiempo:

```kotlin
// shared/build.gradle.kts — estilo AGP 8.x (OBSOLETO)
plugins {
    alias(libs.plugins.kotlinMultiplatform)  // org.jetbrains.kotlin.multiplatform
    alias(libs.plugins.androidLibrary)       // com.android.library — conflicto con KMP en AGP 9.0
}

kotlin {
    androidTarget {                          // declaración del target Android...
        compilations.all {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }
    jvm()
    sourceSets { ... }
}

android {                                    // ...y configuración Android en bloque separado
    namespace = "..."
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17 }
}
```

En AGP 9.0 estos dos plugins (`kotlinMultiplatform` + `androidLibrary`) son incompatibles. JetBrains publicó un tercer plugin que los integra:

```kotlin
// shared/build.gradle.kts — estilo AGP 9.0 (el que usa ZenTrack)
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)    // com.android.kotlin.multiplatform.library
}

kotlin {
    jvm()

    // android {} reemplaza TANTO androidTarget {} COMO el bloque android {} de primer nivel.
    // Declaración del target + configuración Android en un único bloque.
    android {
        namespace  = "me.dcueto.zentrackapp.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk     = libs.versions.androidMinSdk.get().toInt()
    }

    sourceSets { ... }

    jvmToolchain(17)
}
```

### Qué cambió exactamente

| Concepto | AGP 8.x | AGP 9.0+ |
|---|---|---|
| Plugin de Android para KMP | `com.android.library` | `com.android.kotlin.multiplatform.library` |
| Declarar target Android | `kotlin { androidTarget { } }` | Se elimina — reemplazado por `kotlin { android { } }` |
| Configurar namespace/sdk | Bloque `android { }` de primer nivel | Dentro de `kotlin { android { } }` |
| Configurar JVM target | `androidTarget { compilations... compilerOptions }` | `kotlin { jvmToolchain(17) }` al final del bloque `kotlin { }` |
| Plugin `kotlinAndroid` en `androidApp` | Requerido (`com.android.application` + `kotlin-android`) | Integrado en AGP 9.0 — ya no se aplica manualmente |

### Versiones mínimas requeridas

| Herramienta | Mínimo para KMP + AGP 9.0 | En ZenTrack |
|---|---|---|
| AGP | 9.0.0 | `9.0.1` |
| Gradle | 9.1.0 | `9.1.0` |
| KGP (Kotlin Gradle Plugin) | 2.0.0 | `2.3.0` |
| JDK | 17 | 17 |

---

## Resumen: tabla de equivalencias

| .NET | KMP | Notas |
|---|---|---|
| Librería .NET Standard | `shared/` módulo KMP | Código portable entre plataformas |
| Código de la librería | `commonMain/` | Solo Kotlin puro, sin APIs nativas |
| Implementación específica por plataforma | `jvmMain/` / `androidMain/` | APIs nativas de cada plataforma |
| Interfaz + implementaciones por plataforma | `expect` + `actual` | Polimorfismo a nivel de compilación |
| `HttpClient` con `BaseAddress` | `HttpClient` + engine por plataforma | CIO en JVM, OkHttp en Android |
| `IServiceCollection` en `Program.cs` | Módulo Koin en `startKoin { }` | Registro de dependencias |
| `nuget add package` en cada proyecto | Dependencia por source set en `build.gradle.kts` | Dependencias específicas por target |
