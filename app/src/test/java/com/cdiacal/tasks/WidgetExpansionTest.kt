package com.cdiacal.tasks

import com.cdiacal.tasks.widget.toggleId
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetExpansionTest {
    @Test fun addsAnIdThatIsNotThereYet() = assertEquals(setOf(1L, 2L), toggleId(setOf(1L), 2L))

    @Test fun removesAnIdThatIsAlreadyThere() = assertEquals(setOf(1L), toggleId(setOf(1L, 2L), 2L))

    @Test fun togglingTwiceRestoresTheOriginal() =
        assertEquals(setOf(5L), toggleId(toggleId(setOf(5L), 9L), 9L))

    @Test fun worksOnAnEmptySet() = assertEquals(setOf(3L), toggleId(emptySet(), 3L))
}
