
# ZenTrack 🧘‍♂️

ZenTrack es una plataforma minimalista de gestión de proyectos diseñada para equipos ágiles. Nuestro objetivo es reducir el ruido visual de las herramientas tradicionales y fomentar la disciplina de desarrollo integrando la gestión de tareas directamente con nuestro flujo de **GitFlow**.

## 🚀 Características Principales

- **Workspaces Aislados:** Cada cliente/entorno tiene su propio espacio de trabajo con sus proyectos y flujos de estados personalizados.
    
- **Integración GitFlow Automatizada:** Al crear una tarea (ej. `ZTK-24`), el sistema genera automáticamente la rama correspondiente en GitLab/GitHub.
    
- **Actualización por Commits:** Hacer _push_ a la rama de una tarea mueve automáticamente la tarjeta a la columna "In Progress".
    
- **Sprints Transversales:** Agrupa tareas de diferentes proyectos de un mismo Workspace en un único ciclo de desarrollo.
    
- **Multiplataforma:** Experiencia nativa en Escritorio y Web bajo los principios de diseño de Material 3.
    

## 🛠️ Stack Tecnológico

Este proyecto es un monorepo que utiliza el ecosistema de Kotlin para compartir lógica entre el backend y los clientes, combinado con una aplicación web moderna.

- **Backend:** Ktor (Kotlin) + REST API.
    
- **Base de Datos:** PostgreSQL + ORM (Exposed/Ktorm).
    
- **Core Compartido:** Kotlin Multiplatform (KMP) para modelos de datos y cliente HTTP (Ktor Client).
    
- **Frontend Escritorio:** Compose Multiplatform (JVM) + Material 3.
    
- **Frontend Web:** React + TypeScript + Zustand + MUI (Material 3).
    

## 📂 Estructura del Monorepo

Plaintext

```
zentrackapp/
├── backend/                  # API Server (Ktor). Lógica de negocio e integraciones Git.
├── shared/                   # KMP Module. Modelos compartidos y lógica de red.
├── composeApp/               # App nativa de escritorio (Windows/Mac/Linux).
└── webApp/                   # Aplicación Web (React).
```

## 🏁 Primeros Pasos (Setup Local)

### Requisitos Previos

- JDK 17 o superior.
    
- Node.js (v18+) y npm/yarn (para la web).
    
- PostgreSQL ejecutándose en local (puerto 5432).
    
- IntelliJ IDEA (Recomendado para Kotlin/KMP) y VSCode/Cursor (para React).
    

### 1. Base de Datos

Crea una base de datos local llamada `zentrack_db`. Las credenciales por defecto para desarrollo están en `backend/src/main/resources/application.conf`.

### 2. Levantar el Backend (Ktor)

Navega a la carpeta del backend y ejecuta la aplicación:

Bash

```
cd backend
./gradlew run
```

_El servidor estará disponible en `http://localhost:8080`._

### 3. Levantar la App de Escritorio (Compose)

Para ejecutar la aplicación nativa desde tu máquina:

Bash

```
./gradlew :composeApp:run
```

### 4. Levantar la Web App (React)

Navega a la carpeta web, instala las dependencias y arranca el servidor de desarrollo:

Bash

```
cd webApp
npm install
npm run dev
```

## 📜 Reglas de Trabajo y GitFlow (¡Importante!)

Para mantener la disciplina en el equipo, **nadie debe crear ramas manualmente a menos que sea estrictamente necesario o el sistema falle.** 1. **Creación:** Todas las ramas de nuevas funcionalidades o bugs deben generarse a través de la UI de ZenTrack al crear la tarea. 2. **Nomenclatura:** El formato estricto que utiliza la plataforma es `[tipo]/[ID-TAREA]/[descripcion]`.

- _Ejemplo:_ `feature/ZTK-42/login-oauth`
    

3. **Commits:** Asegúrate de hacer _push_ a tu rama remota. ZenTrack escuchará el webhook de GitLab/GitHub y pasará tu tarea a "In Progress" automáticamente.
    
4. **Merge:** Los Pull Requests/Merge Requests deben apuntar a la rama `develop` (o a la rama de la tarea padre si es una subtarea). Nunca se hace merge directo a `main`.