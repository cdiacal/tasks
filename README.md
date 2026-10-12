# Tareas

App Android para controlar tareas pendientes.

**Stack:** Kotlin · Jetpack Compose (Material 3) · Room · ViewModel + StateFlow · minSdk 26

## Funciones (v0.1)
- Crear, editar y eliminar tareas (con "Deshacer" al eliminar)
- Marcar como hecha / pendiente
- Título, notas y fecha límite opcional
- Filtros: Pendientes / Hechas / Todas, y borrado de las hechas
- Datos guardados localmente (Room), modo oscuro y colores dinámicos

## Estructura
```
app/src/main/java/com/cdiacal/tasks/
├── MainActivity.kt
├── data/   Task, TaskDao, TaskDatabase, TaskRepository
└── ui/     TasksScreen, TaskEditorDialog, TasksViewModel, TaskFilter, theme/
```

## Compilar
Abre la carpeta en Android Studio (Koala o posterior) y pulsa Run, o desde terminal
(requiere JDK 17 y Android SDK 34):

```
./gradlew assembleDebug
./gradlew test
```

## Ideas siguientes
Recordatorios (WorkManager/AlarmManager), tareas recurrentes, categorías/etiquetas,
widget de pantalla de inicio, inyección de dependencias (Hilt) y pruebas de UI.
