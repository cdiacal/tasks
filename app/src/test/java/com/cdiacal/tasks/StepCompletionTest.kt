package com.cdiacal.tasks

import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.allDone
import com.cdiacal.tasks.data.shouldCompleteTask
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCompletionTest {
    private val task = Task(id = 1, title = "t")
    private fun step(done: Boolean) = Subtask(taskId = 1, title = "s", isDone = done)

    @Test fun noStepsIsNeverAllDone() = assertFalse(emptyList<Subtask>().allDone())

    @Test fun allDoneNeedsEveryStepTicked() {
        assertTrue(listOf(step(true), step(true)).allDone())
        assertFalse(listOf(step(true), step(false)).allDone())
    }

    @Test fun completesWhenTheLastStepIsTicked() =
        assertTrue(shouldCompleteTask(task, listOf(step(true), step(true), step(true))))

    @Test fun doesNotCompleteWhileAStepIsOpen() =
        assertFalse(shouldCompleteTask(task, listOf(step(true), step(false))))

    @Test fun doesNotCompleteATaskWithoutSteps() = assertFalse(shouldCompleteTask(task, emptyList()))

    @Test fun anAlreadyDoneTaskIsLeftAlone() =
        assertFalse(shouldCompleteTask(task.copy(isDone = true), listOf(step(true))))
}
