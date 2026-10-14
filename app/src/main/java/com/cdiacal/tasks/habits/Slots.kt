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

/** One slot of today for a habit with day slots. */
data class SlotCell(val slot: DaySlot, val done: Boolean)

fun slotEmoji(slot: DaySlot): String = when (slot) {
    DaySlot.MORNING -> "🌅"
    DaySlot.NOON -> "☀️"
    DaySlot.NIGHT -> "🌙"
}
