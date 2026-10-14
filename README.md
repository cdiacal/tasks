# Tareas

App Android para controlar tareas pendientes y hábitos. Todo se guarda en el propio móvil; la app
**no tiene permiso de internet**. Lee el calendario del móvil solo si lo permites (solo lectura).

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
- «Deshacer» al completar (también las repetitivas) o eliminar una tarea
- Gestos en la lista: deslizar a la derecha completa, a la izquierda elimina (ambos con «Deshacer»); «Pasar a mañana» en las vencidas o de hoy
- Accesos directos al mantener pulsado el icono («Nueva tarea», «Nuevo hábito») y un botón «Nueva tarea» para el panel de ajustes rápidos
- «Compartir → Tareas» desde cualquier app crea una tarea con el texto compartido

**Calendario (importar eventos como tareas)**
- Ajustes → Calendario: próximos 30 días de los calendarios del móvil (los de Google incluidos), con vista previa
- Propone qué eventos son tareas: cumpleaños/aniversarios → «Comprar regalo» 3 días antes; verbos y
  «recordatorio»/«fecha límite» → tarea; exámenes/entrevistas → «Preparar» el día anterior; el resto, sin marcar
- Tú marcas lo que quieras y se importa con su fecha; lo ya importado no se repite; se recuerda qué calendarios ocultaste
- Opcional: cada día a las 8:30 avisa de eventos nuevos que parecen tareas (y solo de los que no te había avisado)

**Calendario de tareas:** pestaña con el mes completo, puntos por día (rojo si hay vencidas, color de la prioridad
si no) y la lista de tareas del día que toques, con su casilla para completarlas.

**Hábitos**
- Días de la semana en que se hace, racha actual y mejor racha, vista de 7 días
- Momentos del día (mañana, mediodía, noche): un hábito puede hacerse varias veces al día; el día cuenta como cumplido cuando están todos, y cada momento puede tener su propio recordatorio
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
