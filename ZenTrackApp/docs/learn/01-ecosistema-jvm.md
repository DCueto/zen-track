# 01 — El ecosistema JVM

> Contexto: ¿qué es la JVM, qué papel juega Maven Central, y por qué usamos Gradle si la web dice "Maven"?

---

## ¿Qué es la JVM?

La **Java Virtual Machine** (JVM) es una máquina virtual que ejecuta bytecode. Kotlin compila a bytecode de JVM, igual que Java o Scala. La JVM es el equivalente al **CLR** (.NET Common Language Runtime): el motor que ejecuta el programa en tiempo de ejecución.

```
Kotlin (.kt)  →  compilador Kotlin  →  bytecode (.class)  →  JVM  →  ejecución
C# (.cs)      →  compilador Roslyn  →  IL (.dll)          →  CLR  →  ejecución
```

La diferencia práctica para ti: cuando ejecutas `./gradlew :server:run`, la JVM arranca y carga el bytecode de tu servidor Ktor, exactamente como `dotnet run` carga el IL de tu app ASP.NET Core.

---

## JDK — El SDK del ecosistema Java

El **JDK** (Java Development Kit) es el equivalente al **.NET SDK**. Contiene el compilador, la JVM y las bibliotecas base (`java.lang`, `java.util`, etc.).

En ZenTrack usamos **Java 21 LTS** (la versión Long-Term Support más reciente, equivalente a .NET 8/9 LTS).

```bash
# Equivalencias de comandos
dotnet --version      →  java -version
dotnet new            →  (IntelliJ IDEA o plantillas Gradle)
dotnet run            →  ./gradlew :server:run
dotnet build          →  ./gradlew build
dotnet test           →  ./gradlew test
dotnet publish        →  ./gradlew :server:buildFatJar
```

Hay varios proveedores de JDK (como hay builds de .NET): **Microsoft** (el que usamos, ms-21.0.9), Eclipse Temurin, Amazon Corretto, etc. Todos son compatibles.

---

## Maven Central ≠ Maven (el build tool)

Esta es la confusión más común cuando llegas del mundo .NET.

**Maven** tiene dos significados en el ecosistema Java:

| Término | Qué es | Analogía .NET |
|---------|--------|--------------|
| **Apache Maven** | Herramienta de build (como `dotnet build`) | MSBuild / dotnet CLI |
| **Maven Central** | Repositorio de paquetes públicos | NuGet.org |

**Maven Central** es simplemente el repositorio donde los autores publican sus librerías. Es el equivalente exacto a **NuGet.org**. Cuando en `build.gradle.kts` ves:

```kotlin
repositories {
    mavenCentral()
}
```

Eso es lo mismo que en un `.csproj` tener configurado `https://api.nuget.org/v3/index.json` como fuente de paquetes. Gradle (la herramienta de build) va a Maven Central a descargar las dependencias — no usas Maven como build tool.

```
NuGet.org     ←→  Maven Central    (repositorio de paquetes)
dotnet CLI    ←→  Gradle           (herramienta de build)
MSBuild       ←→  Gradle           (motor de compilación)
PackageReference ←→ implementation() (cómo declaras una dependencia)
```

En ZenTrack nunca usamos Apache Maven como build tool — solo Gradle. Pero Gradle descarga los JARs desde Maven Central, igual que `dotnet restore` los descarga desde NuGet.org.

---

## JAR vs DLL

La unidad de distribución en el ecosistema JVM es el **JAR** (Java ARchive):

- Un JAR es un ZIP con `.class` files y metadatos.
- Las dependencias que declaras en Gradle son JARs publicados en Maven Central.

En .NET, `dotnet publish` recoge automáticamente tu código **y** todas las DLLs de dependencias en una carpeta lista para desplegar. En el ecosistema JVM hay dos variantes:

| Artefacto | Contiene | Para ejecutarlo | Analogía .NET |
|-----------|----------|-----------------|---------------|
| **JAR normal** | Solo tu código compilado | Necesita las deps en el classpath de la máquina | Una DLL suelta |
| **Fat JAR** (uber JAR) | Tu código + **todas las dependencias** empaquetadas dentro | Solo necesita `java` instalado | `dotnet publish` (framework-dependent) |

Un **Fat JAR** empaqueta Ktor, HikariCP, Exposed y todas las demás librerías dentro de un único `.jar`. Lo despliegas en cualquier servidor que tenga Java instalado y ejecutas:

```bash
./gradlew :server:buildFatJar             # Produce server/build/libs/server-all.jar
java -jar server/build/libs/server-all.jar  # Arranca el servidor
```

Lo que el Fat JAR **no incluye** es la propia JVM — igual que `dotnet publish` sin `--self-contained` no incluye el runtime .NET. Si necesitas un binario completamente autocontenido (sin depender de Java instalado), existe **GraalVM Native Image**, pero eso está fuera del scope de ZenTrack.

---

## Resumen: tabla de equivalencias

| .NET / NuGet | Ecosistema JVM/Kotlin | Notas |
|---|---|---|
| CLR | JVM | Runtime que ejecuta el código |
| .NET SDK | JDK | Compilador + herramientas |
| .NET 8/9 LTS | Java 21 LTS | Versión LTS actual |
| MSBuild / dotnet CLI | Gradle | Herramienta de build |
| NuGet.org | Maven Central | Repositorio de paquetes |
| `.dll` / NuGet package | `.jar` | Unidad de distribución |
| `dotnet publish` | `./gradlew buildFatJar` | Empaquetado para producción |
| `PackageReference` | `implementation()` | Cómo declarar una dependencia |
| `appsettings.json` | `application.conf` (HOCON) | Configuración de la app |
