package lk.kiu.safewomen

import lk.kiu.safewomen.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPayloadTest {

    @Test
    fun testSmsPayloadFormatting() {
        val lat = 6.9271
        val lon = 79.8612
        val mapsUrl = "https://maps.google.com/?q=$lat,$lon"
        val template = Constants.DEFAULT_SMS_TEMPLATE

        val formattedMessage = String.format(template, mapsUrl)

        assertTrue(formattedMessage.contains("https://maps.google.com/?q=6.9271,79.8612"))
        assertTrue(formattedMessage.contains("119"))
        assertTrue(formattedMessage.contains("Emergency alert"))
    }

    @Test
    fun testSriLankaPoliceHotlineConstant() {
        assertEquals("119", Constants.POLICE_EMERGENCY_HOTLINE)
    }

    @Test
    fun testNtcBusHotlineConstant() {
        assertEquals("1933", Constants.NTC_BUS_SAFETY_HOTLINE)
    }
}
