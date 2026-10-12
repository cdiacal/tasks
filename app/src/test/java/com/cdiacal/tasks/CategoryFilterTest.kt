package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.inCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryFilterTest {
    private val tasks = listOf(
        Task(id = 1, title = "a", categoryId = 10),
        Task(id = 2, title = "b", categoryId = 20),
        Task(id = 3, title = "c", categoryId = null),
        Task(id = 4, title = "d", categoryId = 10),
    )

    @Test fun nullKeepsEverything() = assertEquals(4, tasks.inCategory(null).size)

    @Test fun filtersBySelectedCategory() =
        assertEquals(listOf(1L, 4L), tasks.inCategory(10).map { it.id })

    @Test fun unknownCategoryYieldsNothing() = assertEquals(emptyList<Task>(), tasks.inCategory(99))
}
