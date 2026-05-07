# ZenTrack — Guía de Aprendizaje

Documentación progresiva del ecosistema Kotlin/KMP construida **a medida que desarrollamos ZenTrack**. Cada documento explica los conceptos desde la perspectiva de alguien que viene del mundo C# / .NET / JavaScript.

---

## Índice

### Ecosistema y Herramientas

| Doc | Qué cubre |
|-----|-----------|
| [01 — El ecosistema JVM](./01-ecosistema-jvm.md) | JVM, JDK, Maven Central, diferencia con NuGet |
| [02 — Gradle para devs .NET](./02-gradle.md) | Gradle vs MSBuild/dotnet CLI, version catalogs, monorepo |

### Backend — Ktor + PostgreSQL

| Doc | Qué cubre |
|-----|-----------|
| [03 — Ktor para devs ASP.NET](./03-ktor-vs-aspnet.md) | Plugins, routing, ContentNegotiation, CORS, StatusPages |
| [04 — Exposed ORM para devs EF](./04-exposed-vs-ef.md) | Tables, HikariCP, application.conf vs appsettings.json |
| [05 — JWT en Ktor](./05-jwt-en-ktor.md) | Autenticación JWT, JwtService, rutas públicas vs protegidas |

### Multiplatforma — KMP

| Doc | Qué cubre |
|-----|-----------|
| [06 — KMP Overview](./06-kmp-overview.md) | `expect/actual`, source sets, Ktor Client engines, Koin en KMP |

### Android — Jetpack Compose

| Doc | Qué cubre |
|-----|-----------|
| [07 — Jetpack Compose vs Blazor](./07-compose-vs-blazor.md) | Composables, MVI, ViewModel, Navigation, Koin |

---

## Cómo está organizado

Cada documento sigue la misma estructura:

1. **¿Qué es?** — Definición concisa
2. **¿Por qué existe?** — El problema que resuelve
3. **Analogía .NET** — Qué conoces tú ya que equivale a esto
4. **Cómo funciona en ZenTrack** — El código real que escribimos
5. **Conceptos clave** — Tabla de equivalencias C# ↔ Kotlin

---

> Estos docs se actualizan con cada tarea completada en TASKS.md.
