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

## El Gradle Wrapper — por qué `./gradlew` y no `gradle`

Si ejecutaras `gradle build` usarías la versión de Gradle instalada en tu máquina. El problema: tú puedes tener Gradle 8.x, tu compañero 7.x, el CI 6.x — cada versión genera builds diferentes con bugs y sintaxis distintos.

El **Gradle Wrapper** resuelve esto. Es un pequeño script (`gradlew` en macOS/Linux, `gradlew.bat` en Windows) comprometido dentro del repositorio que:

1. Lee la versión exacta de Gradle declarada en `gradle/wrapper/gradle-wrapper.properties`
2. Si esa versión no está en caché local (`~/.gradle/wrapper/`), la descarga automáticamente
3. La ejecuta — ignorando cualquier Gradle instalado globalmente

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
```

Todo el equipo y el CI usan exactamente `9.1.0`, sin importar lo que tengan instalado.

> **ZenTrack usa Gradle 9.1.0** porque AGP 9.0 lo requiere como mínimo (ver más abajo).

### La analogía en .NET: `global.json`

```json
// global.json — fuerza una versión específica del .NET SDK
{
  "sdk": { "version": "9.0.101" }
}
```

`global.json` hace lo mismo: cuando existe, `dotnet build` usa esa versión aunque tengas otra instalada. La diferencia es que el Wrapper además **descarga** la versión si no existe — `global.json` solo selecciona entre las ya instaladas.

### Por qué `./` delante

En macOS/Linux, `.` significa "directorio actual". Sin `./`, el shell busca `gradlew` en el `PATH` del sistema y no lo encuentra porque es un archivo local del proyecto. El `./` le dice explícitamente "ejecuta este archivo que está aquí mismo".

```bash
./gradlew build          # compila todo el monorepo
./gradlew :server:run    # ejecuta el módulo server
./gradlew :server:test   # tests del módulo server
```

El `:` antes del nombre identifica el módulo. Sin `:`, el comando aplica a todos los módulos.

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

### ¿Qué es `alias(libs.plugins.kotlinJvm)`?

En el bloque `plugins {}` los plugins se aplican con `alias()`. Para entenderlo hay que trazar la cadena completa desde el `.toml`:

**En `gradle/libs.versions.toml`:**

```toml
[versions]
kotlin = "2.3.0"

[plugins]
kotlinJvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
#             ↑ ID real del plugin              ↑ apunta a [versions].kotlin
```

**Gradle genera automáticamente** un objeto Kotlin llamado `libs` con propiedades typesafe. No lo escribes tú — existe en memoria durante el build:

```
libs.plugins.kotlinJvm  →  id="org.jetbrains.kotlin.jvm", version="2.3.0"
libs.ktor.serverCore    →  "io.ktor:ktor-server-core-jvm:3.3.3"
```

**En `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlinJvm)        // usando el catálogo
}
// es exactamente igual a escribir:
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0"   // hardcodeado
}
```

**¿Por qué `alias()` solo en `plugins {}` y no en `dependencies {}`?**

El bloque `plugins {}` se evalúa antes que el resto del script, en un contexto restringido donde los accessors typesafe no están disponibles directamente. `alias()` es la función puente que resuelve esa limitación. En `dependencies {}` no existe esa restricción, por eso vas directo:

```kotlin
plugins {
    alias(libs.plugins.kotlinJvm)        // plugins: necesita alias()
}
dependencies {
    implementation(libs.ktor.serverCore) // dependencies: sin alias(), directo
}
```

Descompuesto:
```
alias( libs  .  plugins  .  kotlinJvm )
  │     │         │            │
  │     │         │            └─ clave bajo [plugins] en el .toml
  │     │         └─ accede a la sección [plugins] del catálogo
  │     └─ el objeto generado por Gradle que representa el catálogo
  └─ "aplica esta entrada del catálogo como plugin"
```

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

## Versiones del toolchain de ZenTrack (2026-05)

Las versiones se declaran en `gradle/libs.versions.toml`. Esta tabla resume las que afectan al toolchain de build:

| Herramienta | Versión en ZenTrack | Dónde se declara | Notas |
|---|---|---|---|
| Gradle Wrapper | `9.1.0` | `gradle/wrapper/gradle-wrapper.properties` | Mínimo requerido por AGP 9.0 |
| AGP (Android Gradle Plugin) | `9.0.0-alpha06` | `libs.versions.toml` → `agp` | Necesario para `com.android.kotlin.multiplatform.library` |
| Kotlin / KGP | `2.3.0` | `libs.versions.toml` → `kotlin` | Mínimo 2.0.0 para KMP+AGP 9.0 |
| Ktor | `3.3.3` | `libs.versions.toml` → `ktor` | Server + Client KMP |
| Exposed | `0.61.0` | `libs.versions.toml` → `exposed` | ORM del servidor |
| Flyway | `10.15.0` | `libs.versions.toml` → `flyway` | Migraciones SQL del servidor |

### Por qué AGP 9.0 + Gradle 9.1

Hasta AGP 8.x, un módulo KMP usaba dos plugins en el mismo `build.gradle.kts`:
```kotlin
alias(libs.plugins.kotlinMultiplatform)
alias(libs.plugins.androidLibrary)  // com.android.library — DEPRECADO con KMP
```

Desde AGP 9.0 estos dos plugins son incompatibles. La solución oficial de JetBrains es un tercer plugin que integra ambos:
```kotlin
alias(libs.plugins.kotlinMultiplatform)
alias(libs.plugins.androidKmpLibrary)  // com.android.kotlin.multiplatform.library
```

Gradle 9.1 es el mínimo requerido por AGP 9.0. El wrapper en `gradle-wrapper.properties` garantiza que todos los developers y el CI usan exactamente esa versión.

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

---

## Errores comunes de configuración

### 1. Plugin duplicado: `alias()` + `kotlin("jvm")` en el mismo archivo

**Síntoma:**
```
Plugin with id 'org.jetbrains.kotlin.jvm' was already requested at line 1
```
o bien:
```
The request for this plugin could not be satisfied because the plugin is already on the classpath with a different version (2.3.0)
```

**Causa:** En algún momento del desarrollo, se añadió `kotlin("jvm") version "2.3.21"` a los `build.gradle.kts` de los submódulos además del `alias(libs.plugins.kotlinJvm)` que ya existía. Son dos formas de declarar el mismo plugin — el segundo intento lanza un error.

```kotlin
// MAL — duplicado
plugins {
    alias(libs.plugins.kotlinJvm)      // declara kotlinJvm desde el catálogo
    kotlin("jvm") version "2.3.21"     // lo declara otra vez con versión hardcodeada
}

// BIEN
plugins {
    alias(libs.plugins.kotlinJvm)      // una sola declaración, versión desde el catálogo
}
```

**Regla:** NUNCA uses `kotlin("jvm") version "X"` en un submódulo si ya tienes `alias(libs.plugins.kotlinJvm)`. Y NUNCA hardcodees versiones en `build.gradle.kts` — todas van en `libs.versions.toml`.

---

### 2. El `build.gradle.kts` raíz no debe tener `dependencies` ni `repositories`

El build raíz solo declara plugins con `apply false`. Si añades bloques `dependencies { }`, `repositories { }` o `kotlin { }` en el raíz, estás configurando el proyecto raíz como si fuera un módulo de aplicación.

```kotlin
// BIEN — build.gradle.kts raíz (solo declara plugins disponibles)
plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktor)      apply false
    // ...
}
// ← aquí no va nada más

// MAL — bloque de dependencias en el raíz
dependencies {
    implementation(kotlin("stdlib-jdk8"))  // no tiene sentido en el proyecto raíz
}
```

En .NET, el equivalente sería añadir `<PackageReference>` al archivo `.sln` — el `.sln` solo lista proyectos, no tiene dependencias propias.

---

### 3. Inconsistencia entre `jvmToolchain` y `jvmTarget.set`

**Síntoma en IntelliJ:**
```
Inconsistent JVM targets between Java and Kotlin compile tasks: 1.8 and 17.
```

**Causa:** `jvmToolchain(8)` configura el compilador de Java a JVM 1.8, mientras `compilerOptions { jvmTarget.set(JVM_17) }` configura el compilador de Kotlin a JVM 17. Las dos tareas de compilación (Java y Kotlin) tienen targets distintos.

```kotlin
// MAL — conflicto entre los dos mecanismos
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)  // Kotlin → 17
    }
    jvmToolchain(8)  // Java → 8
}

// BIEN — jvmToolchain(N) configura ambas tareas de forma consistente
kotlin {
    jvmToolchain(17)  // Java y Kotlin → 17, sin conflicto
}
```

**Regla:** Usa `jvmToolchain(N)` como única fuente de verdad para el JVM target. No combines `jvmToolchain` con `jvmTarget.set` — el toolchain ya establece el target de Kotlin automáticamente.

| Mecanismo | Configura | Cuándo usarlo |
|---|---|---|
| `jvmToolchain(17)` | Java + Kotlin (ambas tasks) | Siempre — la forma correcta |
| `jvmTarget.set(JVM_17)` | Solo Kotlin | Solo si NO usas jvmToolchain (raro) |

---

### 4. Compatibilidad AGP — IntelliJ bundlea su propia versión del Android plugin

Si IntelliJ muestra:
```
The project is using an incompatible version (AGP X.Y.Z) of the Android Gradle plugin.
Latest supported version is AGP A.B.C
```

Esto significa que el Android plugin **bundleado en tu versión de IntelliJ** tiene un límite de compatibilidad inferior al AGP que usa el proyecto. Los builds desde terminal (`./gradlew`) NO están afectados — el error es solo del Gradle Sync de IntelliJ.

Soluciones, de más a menos disruptiva:
1. Actualizar IntelliJ a la última versión disponible
2. Bajar el AGP del proyecto a la versión máxima soportada por tu IntelliJ (en `libs.versions.toml` → `agp`)

En ZenTrack usamos `9.0.0-alpha06` porque es la última versión de AGP 9.0 soportada por la versión actual de IntelliJ. Los builds de CLI (`./gradlew :server:buildFatJar`) no requieren Gradle Sync y funcionan con cualquier versión de AGP.
