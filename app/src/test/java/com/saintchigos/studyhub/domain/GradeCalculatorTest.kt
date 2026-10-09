package com.saintchigos.studyhub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeCalculatorTest {

    @Test
    fun solvesForTheExamMark() {
        // 60% coursework worth 40%, aiming for 50 overall: 24 banked, 26 still needed out
        // of the 60 that the exam is worth, which is 43.33...
        val needed = GradeCalculator.requiredExamMark(60.0, 40.0, 50.0)!!
        assertEquals(43.333, needed, 0.01)
    }

    @Test
    fun overallIsTheInverseOfRequired() {
        val needed = GradeCalculator.requiredExamMark(72.0, 30.0, 65.0)!!
        assertEquals(65.0, GradeCalculator.overall(72.0, 30.0, needed), 1e-9)
    }

    @Test
    fun verdictsCoverTheThreeOutcomes() {
        assertEquals(
            GradeCalculator.Verdict.ALREADY_THERE,
            GradeCalculator.verdict(GradeCalculator.requiredExamMark(100.0, 80.0, 50.0)!!)
        )
        assertEquals(
            GradeCalculator.Verdict.POSSIBLE,
            GradeCalculator.verdict(GradeCalculator.requiredExamMark(60.0, 40.0, 50.0)!!)
        )
        assertEquals(
            GradeCalculator.Verdict.OUT_OF_REACH,
            GradeCalculator.verdict(GradeCalculator.requiredExamMark(10.0, 40.0, 90.0)!!)
        )
    }

    @Test
    fun rejectsInputsThatAreNotARealCourse() {
        assertNull(GradeCalculator.requiredExamMark(-1.0, 40.0, 50.0))
        assertNull(GradeCalculator.requiredExamMark(60.0, 101.0, 50.0))
        assertNull(GradeCalculator.requiredExamMark(60.0, 40.0, 120.0))
        // No exam left to solve for.
        assertNull(GradeCalculator.requiredExamMark(60.0, 100.0, 50.0))
    }
}
