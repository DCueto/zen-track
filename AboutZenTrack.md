# Propuesta de Proyecto Final: ZenTrack (Gestor de Proyectos Minimalista)

### 1. Visión del Proyecto y Problema a Resolver
Actualmente, las plataformas de gestión de proyectos de software (Jira, Linear, Trello) sufren de sobrecarga de funcionalidades y un exceso de ruido visual. Para equipos ágiles que necesitan ir al grano, la curva de aprendizaje y la fricción diaria en estas herramientas reducen la productividad.

**ZenTrack** es una herramienta de gestión de proyectos diseñada bajo una filosofía minimalista y "user-friendly". Inspirada en interfaces limpias (como las de neobancos o plataformas de inversión tipo Trade Republic), busca ofrecer solo lo estrictamente necesario para gestionar ciclos de desarrollo de software sin distracciones.

### 2. Alcance del MVP (Producto Mínimo Viable)
Para garantizar que el proyecto es alcanzable en el tiempo de la formación, el MVP se centrará en las siguientes características core:

- **Gestión de Proyectos**: Creación de proyectos básicos.
- **Gestión de Sprints**: Ciclos de trabajo activos.
- **Tareas y Subtareas**: Creación, asignación y jerarquía básica.
- **Vistas**: 
	1. Tablero Kanban simple (To Do, In Progress, Done).
	2. Dashboard minimalista con el resumen del sprint actual.
- **Plataformas**: App desarrollada con Kotlin y KMP (focalizada inicialmente en Escritorio/Web).

### 3. Stack Tecnológico
- **Lenguaje / Framework**: Kotlin y Kotlin Multiplatform (KMP) + Compose Multiplatform para la UI.

- **Inteligencia Artificial**: SDK de OpenAI (o Google Gemini) integrado en el cliente/servidor.

- **Herramientas de Desarrollo AI**: Cursor (IDE), Claude Code/Codex (CLI), V0/Lovable (para conceptualización de UI).

### 4. Integración de la Inteligencia Artificial (El Valor Extra)
La IA no será el centro de la aplicación, sino un "asistente invisible" que reducirá aún más la fricción de uso. Funcionalidades IA para el MVP (a elegir o implementar gradualmente):

1. **"Magic Breakdown" (Desglose automático)**: Al crear una tarea compleja con una breve descripción, la IA propondrá automáticamente las subtareas necesarias para completarla.

2. **Creación de tareas en lenguaje natural**: Un input de texto rápido (ej. "Bug: el botón de login no funciona en safari, asignar a Carlos y poner en To Do") que la IA formatea y convierte mágicamente en una tarjeta estructurada en el Kanban.

### 5. Plan de Acción y Aplicación del Temario (Roadmap de 6 Semanas)
- **Semana 1: Investigación y Setup**
	- _Objetivo_: Usar IA para hacer research de UI/UX minimalistas (referencias visuales) y sintetizar los requerimientos técnicos de KMP.
	- _Acción_: Definición final de los modelos de datos (Proyecto, Sprint, Tarea) ayudado por IA para la validación cruzada.

- **Semana 2: Context Engineering y Prompts**
	- _Objetivo_: Preparar la estructura de prompts (Few-Shot) que usaré para la función de "Magic Breakdown" (desglose de subtareas).
	- _Acción_: Crear un archivo de contexto en el repositorio explicando la arquitectura KMP elegida para que la IA me asista mejor en el código.

- **Semana 3: Desarrollo Aumentado en IDE**
	- _Objetivo_: Desarrollo intenso del CRUD de tareas y la UI del tablero Kanban.
	- _Acción_: Uso de Cursor (IDE Agéntico) trabajando con múltiples archivos a la vez para acelerar la creación de componentes en Compose Multiplatform.

- **Semana 4: Automatización de Terminal (CLI)**
	- _Objetivo_: Acelerar tareas tediosas del desarrollo.
	- _Acción_: Usar herramientas como Claude Code o MCPs para automatizar la creación de datos de prueba (mock data) en el proyecto, o para generar tests unitarios de la lógica de negocio.

- **Semana 5: Integración de la API de IA**
	- _Objetivo_: Programar la feature clave de Inteligencia Artificial de la app.
	- _Acción_: Integración real del SDK de OpenAI/Gemini en KMP para dar vida a la creación de tareas por lenguaje natural o al desglose de subtareas. Gestión del prompt, parseo del JSON devuelto y pintado en la UI.

- **Semana 6: Prototipado Final y Entrega**
	- _Objetivo_: Pulido de la interfaz y preparación para la presentación.
	- _Acción_: Uso de herramientas como v0 o Lovable para inspirar detalles finales de la interfaz gráfica minimalista. Refactorización final, subida a GitHub/GitLab y grabación del vídeo de presentación mostrando la app funcionando y explicando el código.

### 6. Entregables Finales
- Repositorio en GitHub con el código de la aplicación (KMP).
- Vídeo (sin cortes ni filtros) demostrando el funcionamiento de la app, el tablero minimalista y mostrando en tiempo real cómo la IA ayuda a desglosar una tarea o crearla mediante lenguaje natural, explicando brevemente el proceso de desarrollo.