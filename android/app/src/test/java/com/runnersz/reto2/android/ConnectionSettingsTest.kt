package com.runnersz.reto2.android
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlin.test.assertFailsWith
class ConnectionSettingsTest{
 @Test fun normalizes(){assertEquals("http://10.0.2.2:8000",ConnectionSettings("http://10.0.2.2:8000/","t").normalizedUrl())}
 @Test fun rejectsEmptyToken(){assertFailsWith<IllegalArgumentException>{ConnectionSettings("http://x","")}}
 @Test fun rejectsBadUrl(){assertFailsWith<IllegalArgumentException>{ConnectionSettings("x","t")}}
}