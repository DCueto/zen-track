# 02 — Gradle para devs .NET

> Cómo entender Gradle si vienes de MSBuild / dotnet CLI / `.csproj`

---

## ¿Qué es Gradle?

Gradle es la herramienta de build del ecosistema Kotlin. Hace lo mismo que `dotnet CLI` + MSBuild: compila el código, resuelve dependencias, corre tests y empaqueta el artefacto final.

La diferencia fundamental: los archivos de configuración de Gradle **son código Kotlin** (o Groovy). No son XML como los `.csproj`, sino scripts que se ejecutan.

---

## Estructura de archivos — comparativa

### En .NET (solución con varios proyectos)

```
MiSolucion.sln
├── MiApi/
│   └── MiApi.csproj
├── MiCore/
│   └── MiCore.csproj
└── MiTests/
    └── MiTests.csproj
```

### En ZenTrack (monorepo Gradle)

```
settings.gradle.kts          ← equivale a MiSolucion.sln
gradle/libs.versions.toml    ← equivale a Directory.Packages.props (Central Package Management)
build.gradle.kts             ← build raíz (configuración compartida)
├── server/
│   └── build.gradle.kts     ← equivale a MiApi.csproj
├── shared/
│   └── build.gradle.kts     ← equivale a MiCore.csproj
└── webApp/
    └── package.json         ← (este módulo usa npm, no Gradle)
```

---

## `settings.gradle.kts` — El archivo solución

```kotlin
// settings.gradle.kts
rootProject.name = "ZenTrackApp"

include(":server")
include(":shared")
// include(":androidApp")
// include(":cli")
```

Equivale al `.sln`. Declara qué módulos (proyectos) existen en el monorepo y con qué nombre Gradle los conoce. `:server` significa "el módulo en el directorio `server/`".

---

## `build.gradle.kts` — El archivo de proyecto

Cada módulo tiene su propio `build.gradle.kts`. Equivale al `.csproj`:

```kotlin
// server/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinJvm)     // "este módulo es Kotlin/JVM"
    alias(libs.plugins.ktor)          // "usa el plugin de Ktor"
    application                       // "es una aplicación ejecutable"
}

dependencies {
    implementation(libs.ktor.serverCore)    // equivale a <PackageReference Include="..." />
    implementation(projects.shared)         // referencia a otro módulo del monorepo
    testImplementation(libs.kotlin.testJunit)
}
```

### Configuraciones de dependencias

| Gradle | .NET / C# | Cuándo usarla |
|--------|-----------|---------------|
| `implementation` | `<PackageReference>` | Dep. de producción, no expuesta a otros módulos |
| `api` | `<PackageReference>` público | Dep. que forman parte de tu API pública |
| `testImplementation` | `<PackageReference>` en test project | Solo para tests |
| `runtimeOnly` | (similar a copy-local) | Solo en runtime, no en compilación |

---

## `gradle/libs.versions.toml` — Central Package Management

En .NET Enterprise existe **Central Package Management** con `Directory.Packages.props` para centralizar versiones. En Gradle se llama **Version Catalog** y vive en `gradle/libs.versions.toml`:

```toml
[versions]
ktor = "3.3.3"
exposed = "0.61.0"

[libraries]
ktor-serverCore = { module = "io.ktor:ktor-server-core-jvm", version.ref = "ktor" }
exposed-core    = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" }

[plugins]
ktor = { id = "io.ktor.plugin", version.ref = "ktor" }
```

Y en el `build.gradle.kts` usas los alias con typesafe accessors:

```kotlin
implementation(libs.ktor.serverCore)   // el alias que pusiste en [libraries]
alias(libs.plugins.ktor)               // el alias de [plugins]
```

Ventaja: si mañana hay que actualizar Ktor de 3.3.3 a 3.4.0, cambias **una línea** en el `.toml` y todos los módulos del monorepo lo heredan. Lo mismo que con `Directory.Packages.props`.

---

## El Gradle Wrapper — `./gradlew`

Nunca instalas Gradle globalmente. El proyecto incluye el **Gradle Wrapper**: un script (`gradlew` en macOS/Linux, `gradlew.bat` en Windows) que descarga automáticamente la versión correcta de Gradle la primera vez.

```bash
./gradlew build          # compila todo el monorepo
./gradlew :server:run    # ejecuta el módulo server
./gradlew :server:test   # tests del módulo server
```

El `:` antes del nombre identifica el módulo. Sin `:`, el comando aplica a todos los módulos.

Es el equivalente conceptual a que `dotnet` ya viene con el SDK instalado — excepto que aquí el "SDK de build" (Gradle) se descarga por proyecto, garantizando que todos en el equipo usen exactamente la misma versión.

---

## Ciclo de build en comparativa

```
dotnet restore     →  ./gradlew dependencies   (resuelve deps)
dotnet build       →  ./gradlew compileKotlin   (compila)
dotnet test        →  ./gradlew test            (tests)
dotnet publish     →  ./gradlew buildFatJar     (empaqueta)
dotnet run         →  ./gradlew :server:run     (ejecuta)
```

---

## Plugins de Gradle — equivalente a los SDKs de .NET

Cuando un `.csproj` tiene `<Project Sdk="Microsoft.NET.Sdk.Web">`, está usando el SDK web de .NET que configura muchas cosas automáticamente. Los **plugins de Gradle** hacen lo mismo:

| Plugin Gradle | SDK .NET equivalente | Qué hace |
|---|---|---|
| `kotlin("jvm")` | `Microsoft.NET.Sdk` | Compila Kotlin a JVM |
| `io.ktor.plugin` | `Microsoft.NET.Sdk.Web` | Empaquetado de apps Ktor |
| `kotlin("multiplatform")` | (sin equivalente directo) | Compilación multi-target (Android, iOS, JVM, JS) |
| `application` | (implícito en Web SDK) | Hace el módulo ejecutable con `./gradlew run` |

---

## Referencia entre módulos del monorepo

```kotlin
// En .NET:
<ProjectReference Include="../MiCore/MiCore.csproj" />

// En Gradle (typesafe accessor):
implementation(projects.shared)
// equivale a ":shared" pero con type safety
```

La notación `projects.shared` es generada automáticamente por Gradle a partir de lo que declaraste en `settings.gradle.kts`. Si el módulo se llama `:androidApp`, el accessor es `projects.androidApp`.

---

## `application.conf` — El `appsettings.json` de Ktor

Ktor usa el formato **HOCON** (Human-Optimized Config Object Notation) para su configuración, similar a JSON pero más legible. El archivo vive en `server/src/main/resources/application.conf`.

```hocon
# application.conf (Ktor/HOCON)
database {
    url = "jdbc:postgresql://localhost:5433/zentrack_db"
    user = "zentrack"
    password = "zentrack_dev"
}
```

```json
// appsettings.json (.NET)
{
  "ConnectionStrings": {
    "DefaultConnection": "Host=localhost;Database=zentrack_db;Username=zentrack"
  }
}
```

Se lee en código con:
```kotlin
// Ktor (HOCON)
val url = application.environment.config.property("database.url").getString()

// ASP.NET Core
var url = configuration.GetConnectionString("DefaultConnection");
```

> Importante: `application.conf` contiene secrets y está en `.gitignore`. Solo `application.conf.example` (con valores de placeholder) se commitea — igual que `.env` vs `.env.example`.
