# WORKFLOW: Refactor

Sigue este proceso cuando se solicite una **reestructuración de código** sin cambio de comportamiento observable.

## Pre-condiciones

1. Define el scope exacto: qué se refactoriza y qué queda fuera.
2. **Los tests deben existir y pasar antes de empezar.** Si no existen, escríbelos en un commit separado antes del refactor. No refactorices código sin cobertura — no hay forma de verificar que no rompiste nada.
3. Lee `CLAUDE.md` del módulo afectado para asegurarte de que el refactor cumple las convenciones.

## Regla fundamental

> Un refactor no cambia comportamiento. Si los tests pasan antes y después, el refactor es correcto. Si alguno falla, introdujiste un cambio de comportamiento — revisar.

## Pasos

### 1. Verificar cobertura existente

Ejecuta los tests del módulo antes de tocar código:

```bash
./gradlew :modulo:test         # Kotlin
cd webApp && npm run test:run  # Web
```

Si hay tests insuficientes: detente, escríbelos en un commit separado (`test: add coverage for X before refactor`), y luego inicia el refactor.

### 2. Inventario

- Lista los archivos que se van a modificar.
- Identifica dependencias: ¿qué otros módulos importan lo que vas a cambiar?

### 3. Plan de cambios

Describe brevemente (al usuario) qué va a cambiar y por qué, antes de ejecutar. Espera confirmación si el scope afecta a más de un módulo.

Ejemplos de refactors válidos:
- Extraer lógica duplicada en un helper con 4+ usos reales.
- Renombrar para alinearse con la convención de dominio.
- Separar una clase/función con demasiadas responsabilidades.

### 4. Ejecutar

- Aplica los cambios en el orden que minimice compilación rota.
- En Kotlin: `./gradlew :modulo:compileKotlin` frecuentemente para detectar errores temprano.
- En webApp: `npx tsc --noEmit` tras cada cambio significativo.

### 5. Verificar invariante

```bash
./gradlew test
cd webApp && npm run test:run
```

Todos los tests deben pasar sin modificaciones. Si necesitas cambiar un test durante el refactor, es probable que estés cambiando comportamiento — confirma con el usuario.

### 6. Cierre

- Confirma que no se añadió funcionalidad no solicitada.
- Confirma que no se introdujeron abstracciones que no tienen al menos 3-4 usos reales.
