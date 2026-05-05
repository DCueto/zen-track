# FEATURE CONTEXT: Nuevo Endpoint API (Ktor)

Contexto específico para añadir **un endpoint concreto** al servidor Ktor. Úsalo junto al prompt de la tarea.

## Archivos a leer antes de implementar

```
server/CLAUDE.md                         → reglas de arquitectura del módulo
docs/SDD/PLAN.md                         → esquema de BD y lista de endpoints existentes
server/src/main/kotlin/.../routes/       → patrón de rutas existente
server/src/main/kotlin/.../services/     → patrón de servicios existente
server/src/main/resources/db/migration/  → última migración SQL
```

## Estructura obligatoria

```
Route (routing.kt o archivo de rutas del dominio)
  └── Service (lógica de negocio, sin acceso directo a BD)
        └── Repository (acceso a BD con Exposed/Ktorm)
```

No accedas a la BD desde la ruta ni desde el service directamente.

## Checklist de implementación

- [ ] Ruta definida con el método HTTP correcto y path coherente con la API REST existente
- [ ] Handler delega en Service, no contiene lógica de negocio
- [ ] Service delega en Repository para acceso a datos
- [ ] Errores HTTP manejados con `respond(HttpStatusCode.X)` apropiado
- [ ] Autenticación JWT verificada si el endpoint no es público
- [ ] Multi-tenancy respetada: las queries filtran por `workspace_id`
- [ ] Migración SQL añadida si hay cambio de esquema
- [ ] `openapi.json` actualizado con el nuevo endpoint y sus schemas
- [ ] Tests del endpoint escritos
- [ ] Verificación: `./gradlew :server:test`

## Post-implementación (si hay clientes web)

```bash
cd webApp && npx openapi-typescript http://localhost:8080/openapi.json -o src/types/api.ts
```
