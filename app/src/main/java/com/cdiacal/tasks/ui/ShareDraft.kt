package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Task

private const val MAX_TITLE = 120
private const val MAX_NOTES = 5000

/**
 * The task to prefill when text is shared into the app, or null when there is nothing usable.
 * The subject becomes the title when there is one (the text then goes to the notes); otherwise
 * the first line is the title and the whole text is kept in the notes when it says more.
 */
fun sharedTextToDraft(subject: String?, text: String?): Task? {
    val body = text?.trim().orEmpty()
    val subj = subject?.trim().orEmpty()
    val title = when {
        subj.isNotEmpty() -> subj
        else -> body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    }
    if (title.isEmpty()) return null
    val shortTitle = if (title.length > MAX_TITLE) title.take(MAX_TITLE - 1).trimEnd() + "…" else title
    val notes = if (body == shortTitle) "" else body.take(MAX_NOTES)
    return Task(title = shortTitle, notes = notes)
}
