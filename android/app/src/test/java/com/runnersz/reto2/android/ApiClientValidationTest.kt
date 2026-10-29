package com.runnersz.reto2.android
import org.junit.Test
import kotlin.test.assertFailsWith
class ApiClientValidationTest{@Test fun rejectsInvalidShift(){assertFailsWith<IllegalArgumentException>{ApiClient("http://x","t").caesar("x",0)}}@Test fun rejectsEmptyFile(){assertFailsWith<IllegalArgumentException>{ApiClient("http://x","t").decrypt(byteArrayOf(),"p")}}@Test fun rejectsEmptyPassword(){assertFailsWith<IllegalArgumentException>{ApiClient("http://x","t").decrypt(byteArrayOf(1),"")}}}