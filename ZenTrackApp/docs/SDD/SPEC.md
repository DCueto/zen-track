# Spec: ZenTrack - Gestión Ágil, Workspaces e Integración GitFlow

**Descripción General**: ZenTrack es una plataforma minimalista de gestión de proyectos estructurada en **Workspaces**. Cada Workspace aísla los datos de un cliente (proyectos, sprints transversales y flujos de estados configurables). La herramienta automatiza el flujo de trabajo mediante una integración profunda con GitLab/GitHub, generando ramas basadas en GitFlow con IDs de proyecto legibles (ej. ZTK-1), y sienta las bases para una futura asistencia integral por Inteligencia Artificial.

**Arquitectura de Frontend Web**: La aplicación web es TypeScript puro (React 19 + Zustand + MUI). Los modelos y DTOs compartidos se consumen desde el módulo KMP `shared`, compilado a JS con definiciones TypeScript generadas automáticamente (`generateTypeScriptDefinitions()`). **No se usa Kotlin/JS directamente en `webApp/`**; la web es una aplicación TypeScript estándar que importa tipos desde el paquete npm local `shared`.

## Historias de Usuario: MVP (Fase 1)

### Historia 1: Navegación de Workspaces y Configuración de Proyectos

**Como** miembro del equipo, **Quiero** navegar entre mis Workspaces y configurar proyectos con identificadores únicos, **Para** mantener la información organizada y garantizar que las tareas tengan una nomenclatura clara. **Criterios de Aceptación:**

1. Al iniciar sesión, debo ver un panel con todos los Workspaces a los que tengo acceso.
2. Al entrar en un Workspace, la interfaz debe mostrar únicamente la información de ese espacio.
3. Al crear un nuevo Proyecto dentro de un Workspace, el sistema debe requerir un "ID de Proyecto" (un string corto y único, ej. `ZTK`).
4. Todas las tareas generadas dentro de ese proyecto adoptarán automáticamente el formato `[ID-PROYECTO]-[Número Ascendente]` (ej. `ZTK-1`, `ZTK-24`).

### Historia 2: Creación de tareas, subtareas y ramas automatizadas

**Como** miembro del equipo, **Quiero** crear tareas/subtareas y que se generen automáticamente sus ramas en GitLab/GitHub siguiendo la nomenclatura de GitFlow, **Para** estandarizar el código. **Criterios de Aceptación:**

1. Debo poder definir: asignados, fechas, prioridad, etiquetas, estimaciones y añadir un checklist manual.
2. La tarea recibirá automáticamente su ID correlativo (ej. `ZTK-25`).
3. Debo poder elegir el prefijo de GitFlow (feature, bug, etc.) y editar el nombre de la rama. El formato por defecto que propondrá el sistema será: `[prefijo]/[ID-TAREA]/[descripcion-breve]` (ej. `feature/ZTK-25/login-bug`).
4. Debo poder guardar la tarea como "Borrador" sin generar la rama en el repositorio.
5. Las subtareas deben generar su propia rama, configurada para hacer merge únicamente a la rama de la tarea "padre".

### Historia 3: Actualización automática de estado por commit

**Como** equipo, **Queremos** definir estados personalizados por cada Workspace y que los commits actualicen las tareas automáticamente, **Para** adaptarnos a metodologías ágiles sin esfuerzo manual. **Criterios de Aceptación:**

1. Cada Workspace debe permitir configurar sus propios estados de tarea (ej. Backlog, ToDo, In Progress, Stopped, Testing, Git MR, Done).
2. Al hacer push del primer commit a la rama de una tarea, esta debe moverse automáticamente a la columna equivalente a "In Progress".

### Historia 4: Visualización flexible, ordenación y filtrado

**Como** miembro del equipo, **Quiero** ver las tareas en diferentes niveles con opciones potentes de filtrado, **Para** encontrar rápidamente lo que necesito. **Criterios de Aceptación:**

1. Dentro de un Workspace, debo poder alternar entre Tablero Global, Tablero de Sprint o Tablero de Proyecto (en vista Kanban o Lista).
2. Debo poder filtrar y ordenar la vista actual usando cualquier atributo de la tarea.

---

## Historias de Usuario: Inteligencia Artificial (Fase 2 - Post-MVP)

_(Estas historias se mantienen igual: Creación por Lenguaje Natural, Magic Breakdown para subtareas, y Generación de Descripciones/Checklists)._

---

## Casos Borde y Escenarios de Error (MVP)

1. **Fallo Git:** Si la API de GitLab/GitHub falla, se permite crear la tarea sin rama vinculada y reintentarlo después, o forzar la vinculación manual.
2. **Cambios de Proyecto:** Definir comportamiento si una tarea activa en un sprint cambia a un proyecto que no pertenece a ese sprint.
3. **Concurrencia de IDs:** ¿Qué ocurre a nivel de base de datos si dos usuarios crean una tarea para el mismo proyecto en el mismo milisegundo exacto? El sistema debe garantizar que los IDs (`ZTK-N`) no se dupliquen y el número ascendente sea seguro y consistente.

## Requisitos Clave (Must-Haves)

- Arquitectura multi-tenant lógica para separar los **Workspaces**.
- ID de Proyecto configurable (String único) con autoincremento para las tareas.
- Integración bidireccional con GitLab/GitHub.
