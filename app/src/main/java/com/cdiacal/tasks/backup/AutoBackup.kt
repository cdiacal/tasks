package com.cdiacal.tasks.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cdiacal.tasks.widget.createRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FILE_PREFIX = "tareas-auto-"
private val FILE_PATTERN = Regex("^tareas-auto-\\d{4}-\\d{2}-\\d{2}\\.json$")

/** `tareas-auto-2026-10-12.json` — ISO dates sort chronologically as plain text. */
fun autoBackupFileName(date: LocalDate): String = "$FILE_PREFIX$date.json"

/**
 * From the file names in the backup folder, the ones to delete so that only the newest [keep]
 * automatic backups remain. Files that do not look like automatic backups are never selected.
 */
fun selectOldBackups(names: List<String>, keep: Int): List<String> =
    names.filter { FILE_PATTERN.matches(it) }.sortedDescending().drop(keep)

/**
 * Weekly automatic backup into a folder the user picked with the system folder picker (so no
 * storage permission is needed). Settings live in the same preferences file as the theme.
 */
object AutoBackup {
    private const val WORK_NAME = "auto_backup"
    private const val KEEP = 5
    private const val PREFS = "settings"
    private const val KEY_URI = "auto_backup_uri"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST = "auto_backup_last"
    private const val KEY_ERROR = "auto_backup_error"

    data class State(
        val folderName: String?,
        val enabled: Boolean,
        val lastBackupAt: Long?,
        val lastError: String?,
    ) {
        val hasFolder: Boolean get() = folderName != null
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun state(context: Context): State {
        val prefs = prefs(context)
        val uri = prefs.getString(KEY_URI, null)?.let(Uri::parse)
        return State(
            folderName = uri?.let { DocumentFile.fromTreeUri(context, it)?.name ?: "—" },
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            lastBackupAt = prefs.getLong(KEY_LAST, 0).takeIf { it > 0 },
            lastError = prefs.getString(KEY_ERROR, null),
        )
    }

    /** Remembers the chosen folder (keeping access across restarts), turns the backup on and schedules it. */
    fun setFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs(context).edit()
            .putString(KEY_URI, uri.toString())
            .putBoolean(KEY_ENABLED, true)
            .remove(KEY_ERROR)
            .apply()
        schedule(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        schedule(context)
    }

    /** Starts or stops the weekly job to match the settings. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        if (state(context).let { it.enabled && it.hasFolder }) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            // KEEP: an already scheduled job keeps its timing instead of restarting the week.
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    /** Writes today's backup file into the folder and prunes old ones. Records success or the error. */
    suspend fun runNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        runCatching {
            val uri = prefs.getString(KEY_URI, null)?.let(Uri::parse) ?: error("No folder chosen")
            val folder = DocumentFile.fromTreeUri(context, uri)?.takeIf { it.canWrite() }
                ?: error("The folder is no longer available")

            val (json, _) = createRepository(context).exportBackup()
            val name = autoBackupFileName(LocalDate.now())
            folder.findFile(name)?.delete() // a second backup the same day replaces the first
            val file = folder.createFile("application/json", name) ?: error("Could not create the file")
            val out = context.contentResolver.openOutputStream(file.uri, "wt") ?: error("Could not write the file")
            out.use { it.write(json.toByteArray(Charsets.UTF_8)) }

            selectOldBackups(folder.listFiles().mapNotNull { it.name }, KEEP)
                .forEach { old -> folder.findFile(old)?.delete() }
            name
        }.onSuccess {
            prefs.edit().putLong(KEY_LAST, System.currentTimeMillis()).remove(KEY_ERROR).apply()
        }.onFailure {
            prefs.edit().putString(KEY_ERROR, it.message ?: it.javaClass.simpleName).apply()
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (AutoBackup.state(applicationContext).enabled) {
            // A failure is recorded and shown in the app; it is retried at the next weekly run.
            AutoBackup.runNow(applicationContext)
        }
        return Result.success()
    }
}
