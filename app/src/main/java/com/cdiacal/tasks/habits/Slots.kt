package com.cdiacal.tasks.habits

/** Parts of the day a habit can be required in, so one habit can be done twice or more a day. */
enum class DaySlot(val value: Int) {
    MORNING(1),
    NOON(2),
    NIGHT(3);

    /** The bit of [Habit.slotsMask] that switches this slot on. */
    val bit: Int get() = 1 shl (value - 1)
}

/** The slots switched on in [mask], in day order. Empty = the habit is done once a day. */
fun slotsOf(mask: Int): List<DaySlot> = DaySlot.entries.filter { mask and it.bit != 0 }

/**
 * Whether a day counts as done, given the slot values stored for it. Without slots any tick
 * counts; with slots every slot is needed, but a plain tick (slot 0, from before slots existed)
 * covers the whole day.
 */
fun isDayComplete(mask: Int, stored: Set<Int>): Boolean = when {
    stored.isEmpty() -> false
    mask == 0 || 0 in stored -> true
    else -> slotsOf(mask).all { it.value in stored }
}

/** The slot value a "done" shortcut should tick next, or null when the day is already complete. */
fun nextPendingSlot(mask: Int, stored: Set<Int>): Int? = when {
    isDayComplete(mask, stored) -> null
    mask == 0 -> 0
    else -> slotsOf(mask).first { it.value !in stored }.value
}

/** Every local epoch day on which the habit counts as done. */
fun completeDays(mask: Int, checks: List<HabitCheck>): Set<Long> =
    checks.groupBy({ it.epochDay }, { it.slot })
        .filterValues { isDayComplete(mask, it.toSet()) }
        .keys

/** Default reminder time (minutes after midnight) offered for each slot: 08:00, 14:00 and 21:00. */
fun defaultSlotMinutes(slot: DaySlot): Int = when (slot) {
    DaySlot.MORNING -> 8 * 60
    DaySlot.NOON -> 14 * 60
    DaySlot.NIGHT -> 21 * 60
}

/** "1=480;3=1260" -> {1: 480, 3: 1260}. Malformed entries are skipped. */
fun parseSlotReminders(text: String): Map<Int, Int> =
    text.split(';').mapNotNull { part ->
        val (slot, minutes) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        val s = slot.trim().toIntOrNull()?.takeIf { v -> DaySlot.entries.any { it.value == v } } ?: return@mapNotNull null
        val m = minutes.trim().toIntOrNull()?.takeIf { it in 0 until 24 * 60 } ?: return@mapNotNull null
        s to m
    }.toMap()

fun encodeSlotReminders(times: Map<Int, Int>): String =
    times.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" }

/**
 * The reminders a habit has as (slot value, minutes after midnight). A plain habit has at most
 * one, with slot 0; a habit with day slots has one per slot that has a time.
 */
fun reminderTimes(habit: Habit): List<Pair<Int, Int>> {
    val slots = slotsOf(habit.slotsMask)
    if (slots.isEmpty()) return listOfNotNull(habit.reminderMinutes?.let { 0 to it })
    val times = parseSlotReminders(habit.slotReminders)
    return slots.mapNotNull { slot -> times[slot.value]?.let { slot.value to it } }
}

/** Whether one slot of a day is done (a whole-day tick covers every slot). */
fun isSlotDone(slot: Int, stored: Set<Int>): Boolean = slot in stored || 0 in stored

/** One slot of today for a habit with day slots. */
data class SlotCell(val slot: DaySlot, val done: Boolean)

fun slotEmoji(slot: DaySlot): String = when (slot) {
    DaySlot.MORNING -> "🌅"
    DaySlot.NOON -> "☀️"
    DaySlot.NIGHT -> "🌙"
}
