package lk.kiu.safewomen

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class AuthAndChatTest {

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testPinHashingAndValidation() {
        val originalPin = "1234"
        val hashedPin = hashSha256(originalPin)

        assertEquals(64, hashedPin.length)
        assertEquals(hashedPin, hashSha256("1234"))
        assertNotEquals(hashedPin, hashSha256("0000"))
    }

    @Test
    fun testMultiGuardianBroadcastRecipientFormat() {
        val testNumbers = listOf("0711111111", "0722222222", "0712345678")
        val cleanNumbers = testNumbers.map { it.trim().replace(" ", "").replace("-", "") }

        assertEquals(3, cleanNumbers.size)
        assertTrue(cleanNumbers.contains("0711111111"))
        assertTrue(cleanNumbers.contains("0722222222"))
        assertTrue(cleanNumbers.contains("0712345678"))
    }
}
