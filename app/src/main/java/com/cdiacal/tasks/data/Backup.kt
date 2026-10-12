package com.cdiacal.tasks.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupData(val categories: List<Category>, val tasks: List<Task>)

/** The file is not a readable Tareas backup (wrong app, newer version, or malformed). */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Human-readable JSON backup format (version 1). */
object BackupJson {
    private const val APP = "tareas"
    private const val VERSION = 1

    fun encode(tasks: List<Task>, categories: List<Category>, exportedAt: Long): String {
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
                                .put("createdAt", t.createdAt),
                        )
                    }
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
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
            return BackupData(categories, tasks)
        } catch (e: JSONException) {
            throw InvalidBackupException("Malformed backup", e)
        }
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { getJSONObject(it) }
}
