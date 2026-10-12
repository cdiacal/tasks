package com.cdiacal.tasks.data

import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * [habits] is null when the file predates habits: restoring then leaves current habits untouched.
 */
data class BackupData(
    val categories: List<Category>,
    val tasks: List<Task>,
    val habits: List<Habit>? = null,
    val habitChecks: List<HabitCheck> = emptyList(),
    /** Absent in older files; restoring always replaces the steps with these. */
    val subtasks: List<Subtask> = emptyList(),
    /** Null when the file predates the completion log: restoring then keeps the current log. */
    val completions: List<TaskCompletion>? = null,
)

/** The file is not a readable Tareas backup (wrong app, newer version, or malformed). */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Human-readable JSON backup format (version 1). */
object BackupJson {
    private const val APP = "tareas"
    private const val VERSION = 1

    fun encode(
        tasks: List<Task>,
        categories: List<Category>,
        exportedAt: Long,
        habits: List<Habit> = emptyList(),
        habitChecks: List<HabitCheck> = emptyList(),
        subtasks: List<Subtask> = emptyList(),
        completions: List<TaskCompletion> = emptyList(),
    ): String {
        val root = JSONObject()
            .put("app", APP)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put(
                "categories",
                JSONArray().apply {
                    categories.forEach { c ->
                        put(JSONObject().put("id", c.id).put("name", c.name).put("emoji", c.emoji))
                    }
                },
            )
            .put(
                "tasks",
                JSONArray().apply {
                    tasks.forEach { t ->
                        put(
                            JSONObject()
                                .put("id", t.id)
                                .put("title", t.title)
                                .put("notes", t.notes)
                                .put("isDone", t.isDone)
                                // put(key, null) would drop the key, so nulls are explicit.
                                .put("dueDate", t.dueDate ?: JSONObject.NULL)
                                .put("reminderAt", t.reminderAt ?: JSONObject.NULL)
                                .put("categoryId", t.categoryId ?: JSONObject.NULL)
                                .put("recurrence", t.recurrence)
                                .put("priority", t.priority)
                                .put("createdAt", t.createdAt),
                        )
                    }
                },
            )
            .put(
                "subtasks",
                JSONArray().apply {
                    subtasks.forEach { s ->
                        put(
                            JSONObject()
                                .put("id", s.id)
                                .put("taskId", s.taskId)
                                .put("title", s.title)
                                .put("isDone", s.isDone)
                                .put("position", s.position),
                        )
                    }
                },
            )
            .put(
                "completions",
                JSONArray().apply {
                    completions.forEach { c ->
                        put(JSONObject().put("id", c.id).put("taskId", c.taskId).put("epochDay", c.epochDay))
                    }
                },
            )
            .put(
                "habits",
                JSONArray().apply {
                    habits.forEach { h ->
                        put(
                            JSONObject()
                                .put("id", h.id)
                                .put("name", h.name)
                                .put("emoji", h.emoji)
                                .put("daysMask", h.daysMask)
                                .put("createdAt", h.createdAt)
                                .put("reminderMinutes", h.reminderMinutes ?: JSONObject.NULL),
                        )
                    }
                },
            )
            .put(
                "habitChecks",
                JSONArray().apply {
                    habitChecks.forEach { c -> put(JSONObject().put("habitId", c.habitId).put("epochDay", c.epochDay)) }
                },
            )
        return root.toString(2)
    }

    fun decode(text: String): BackupData {
        try {
            val root = JSONObject(text)
            if (root.optString("app") != APP) throw InvalidBackupException("Not a Tareas backup")
            if (root.optInt("version", -1) !in 1..VERSION) throw InvalidBackupException("Unsupported backup version")

            val categories = root.optJSONArray("categories").objects().map { o ->
                Category(id = o.getLong("id"), name = o.getString("name"), emoji = o.optString("emoji", ""))
            }
            val tasks = root.optJSONArray("tasks").objects().map { o ->
                Task(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    notes = o.optString("notes", ""),
                    isDone = o.optBoolean("isDone", false),
                    dueDate = o.longOrNull("dueDate"),
                    reminderAt = o.longOrNull("reminderAt"),
                    categoryId = o.longOrNull("categoryId"),
                    recurrence = Recurrence.fromName(o.optString("recurrence", "")).name,
                    priority = Priority.fromLevel(o.optInt("priority", 0)).level,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
            val habits = if (root.has("habits")) {
                root.optJSONArray("habits").objects().map { o ->
                    Habit(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        emoji = o.optString("emoji", ""),
                        daysMask = o.optInt("daysMask", ALL_DAYS),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        reminderMinutes = o.longOrNull("reminderMinutes")?.toInt(),
                    )
                }
            } else {
                null
            }
            val checks = root.optJSONArray("habitChecks").objects().map { o ->
                HabitCheck(habitId = o.getLong("habitId"), epochDay = o.getLong("epochDay"))
            }
            val subtasks = root.optJSONArray("subtasks").objects().map { o ->
                Subtask(
                    id = o.getLong("id"),
                    taskId = o.getLong("taskId"),
                    title = o.getString("title"),
                    isDone = o.optBoolean("isDone", false),
                    position = o.optInt("position", 0),
                )
            }
            val completions = if (root.has("completions")) {
                root.optJSONArray("completions").objects().map { o ->
                    TaskCompletion(id = o.getLong("id"), taskId = o.getLong("taskId"), epochDay = o.getLong("epochDay"))
                }
            } else {
                null
            }
            return BackupData(categories, tasks, habits, checks, subtasks, completions)
        } catch (e: JSONException) {
            throw InvalidBackupException("Malformed backup", e)
        }
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { getJSONObject(it) }
}
