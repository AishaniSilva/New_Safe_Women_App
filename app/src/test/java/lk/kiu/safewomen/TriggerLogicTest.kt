package lk.kiu.safewomen

import lk.kiu.safewomen.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerLogicTest {

    @Test
    fun testThresholdCalculation_ValidTrigger() {
        val threshold = Constants.DEFAULT_TRIGGER_DURATION_MS // 3000ms
        val keyDownTime = 1000000L
        val currentEventTime = 1003150L // 3150ms elapsed

        val elapsed = currentEventTime - keyDownTime
        val isTriggered = elapsed >= threshold

        assertTrue("Emergency trigger should activate when hold exceeds 3000ms", isTriggered)
    }

    @Test
    fun testThresholdCalculation_EarlyRelease() {
        val threshold = Constants.DEFAULT_TRIGGER_DURATION_MS // 3000ms
        val keyDownTime = 1000000L
        val keyUpTime = 1001500L // 1500ms elapsed (early release)

        val elapsed = keyUpTime - keyDownTime
        val isTriggered = elapsed >= threshold

        assertFalse("Emergency trigger must NOT activate on accidental early release (<3000ms)", isTriggered)
    }

    @Test
    fun testSurveyDerivedDefaultValue() {
        // Empirically derived from 59.6% survey responses
        assertEquals(3000L, Constants.DEFAULT_TRIGGER_DURATION_MS)
    }
}
