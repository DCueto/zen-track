# WORKFLOW: Documentation

Sigue este proceso cuando se solicite **crear o actualizar documentación** en ZenTrack.

## Tipos de documentación en este proyecto

| Tipo | Ubicación | Actualiza quién |
|---|---|---|
| Reglas para agentes AI | `CLAUDE.md`, `AGENTS.md` | Solo cuando cambian convenciones técnicas |
| Spec de producto | `docs/SDD/SPEC.md` | Solo cuando cambian requisitos de negocio |
| Plan técnico | `docs/SDD/PLAN.md` | Cuando cambia arquitectura, BD o endpoints |
| Backlog | `docs/SDD/TASKS.md` | Cuando se añaden o completan tareas |
| README | `README.md` | Cuando cambia setup o comandos clave |

## Pre-condiciones

1. Identifica qué tipo de documentación se necesita (tabla arriba).
2. Lee el archivo existente antes de proponer cambios.
3. Nunca borres secciones existentes sin confirmación explícita.

## Pasos

### 1. Leer el estado actual

Lee el archivo destino completo. Identifica:
- ¿Qué información ya existe y es correcta?
- ¿Qué está desactualizado?
- ¿Qué falta?

### 2. Escribir

- Sé preciso y conciso. La documentación que no se lee no sirve.
- En `CLAUDE.md`: usa el formato `PROHIBIDO / SIEMPRE / NUNCA` para reglas. No escribas explicaciones largas donde una regla basta.
- En `SPEC.md`: mantén el formato de Historia de Usuario + Criterios de Aceptación.
- En `PLAN.md`: mantén tablas de BD y lista de endpoints actualizadas con el código real.

### 3. Verificar coherencia

- Asegúrate de que lo documentado refleja el código actual, no el código planeado.
- Si la documentación describe algo que aún no está implementado, márcalo explícitamente como `[PENDIENTE]`.

### 4. Cierre

- Confirma que los `CLAUDE.md` de módulo y el `AGENTS.md` raíz son coherentes entre sí.
- Nunca documentes comportamiento que no existe en el código.
