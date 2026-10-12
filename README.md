# Tareas

App Android para controlar tareas pendientes y hábitos. Todo se guarda en el propio móvil; la app
**no tiene permiso de internet**.

**Stack:** Kotlin · Jetpack Compose (Material 3) · Room · ViewModel + StateFlow · Jetpack Glance
(widgets) · WorkManager · minSdk 26

## Funciones

**Tareas**
- Crear, editar y eliminar (con «Deshacer»); marcar como hechas
- Título, notas, fecha límite, categoría, recordatorio con notificación
- Prioridad (baja, media, alta) y orden por fecha o por prioridad (también en el widget)
- Repetición (diaria, semanal, mensual, anual): al completarla se reprograma sola
- Pasos (subtareas) con progreso en la tarjeta
- Búsqueda por título, notas, categoría y pasos (sin distinguir mayúsculas ni tildes)
- Filtros por estado y por categoría; categorías con emoji

**Hábitos**
- Días de la semana en que se hace, racha actual y mejor racha, vista de 7 días
- Calendario mensual con el historial (los días pasados se pueden marcar o desmarcar)
- Recordatorio diario opcional (solo los días que tocan; no avisa si ya está hecho; botón «Hecho»)

**Estadísticas**
- Tareas completadas por semana (últimas 8; el histórico empieza al instalar esta versión), pendientes y vencidas
- Cumplimiento semanal de hábitos y rachas por hábito
- El registro de tareas completadas va incluido en la copia de seguridad

**Widgets de pantalla de inicio:** «Tareas pendientes» y «Hábitos de hoy» (se marcan sin abrir la app)

**Datos**
- Copia de seguridad manual: exportar / restaurar un `.json` con el selector de archivos
- Copia automática semanal a una carpeta elegida (conserva las últimas 5)

**Ajustes:** una pantalla propia (engranaje de la cabecera) con apariencia, orden, categorías,
avisos (resumen diario y estado de las notificaciones) y copias de seguridad.

**Diseño:** tema claro / oscuro / sistema, azul marino sobrio (ver `ui/theme`).

## Estructura
```
app/src/main/java/com/cdiacal/tasks/
├── MainActivity.kt
├── data/       Task, Category, Subtask, DAOs, TaskDatabase (migraciones), copias (Backup.kt)
├── habits/     Habit, HabitCheck, rachas y calendario (lógica pura y testeada)
├── reminders/  alarmas de tareas y hábitos, notificaciones, reprogramación tras reinicio
├── widget/     widgets Glance
├── backup/     copia automática (WorkManager)
└── ui/         pantallas, diálogos, componentes y tema
```

## Compilar
Con Android Studio (Koala o posterior) abre la carpeta y pulsa Run, o por terminal (JDK 17 y
Android SDK 34):

```
./gradlew assembleDebug
./gradlew test
```

## APK desde GitHub (sin ordenador)
Cada `push` ejecuta `.github/workflows/build-apk.yml`: pasa los tests, compila el APK y lo publica
en *Releases* (`tareas.apk`). El `versionCode` es el número de ejecución del workflow, así que cada
APK es más nuevo que el anterior.

**Firma:** para que el APK se actualice encima de la versión instalada, debe firmarse siempre con
la misma clave. La clave **no está en el repositorio**: se inyecta desde los secretos de GitHub
`KEYSTORE_BASE64` (el `.p12` en base64) y `KEYSTORE_PASSWORD` (alias `tareas`). Sin ellos, el
workflow genera un APK firmado con la clave de depuración. Guarda la clave en un sitio seguro: si se
pierde, hay que desinstalar la app (y perder sus datos) para instalar una versión firmada con otra.

## Formato de la copia de seguridad
JSON legible con `app`, `version`, `categories`, `tasks`, `subtasks`, `habits`, `habitChecks` y `completions`.
Restaurar **reemplaza** todos los datos. Las copias antiguas sin sección de hábitos no tocan los
hábitos actuales.
