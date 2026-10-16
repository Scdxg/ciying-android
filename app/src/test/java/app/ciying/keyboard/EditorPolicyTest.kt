package app.ciying.keyboard
import org.junit.Assert.*
import org.junit.Test

class EditorPolicyTest {
    @Test fun passwordsUseDirectPrivateInput() {
        for (type in listOf(0x81, 0x91, 0xe1)) {
            val mode = EditorPolicy.from(type, 0)
            assertTrue(mode.english); assertTrue(mode.privateInput)
        }
        assertTrue(EditorPolicy.from(0x12, 0).privateInput)
        assertTrue(EditorPolicy.from(0x12, 0).numeric)
    }
    @Test fun emailUriAndNumericFieldsHaveSuitableModes() {
        assertTrue(EditorPolicy.from(0x21, 0).english)
        assertTrue(EditorPolicy.from(0x11, 0).english)
        assertTrue(EditorPolicy.from(3, 0).numeric)
        assertFalse(EditorPolicy.from(1, 0).english)
    }
    @Test fun noPersonalizedLearningFlagDisablesContextAndLearning() {
        assertTrue(EditorPolicy.from(1, 0x01000000).privateInput)
        assertFalse(EditorPolicy.from(1, 0).privateInput)
    }
}
