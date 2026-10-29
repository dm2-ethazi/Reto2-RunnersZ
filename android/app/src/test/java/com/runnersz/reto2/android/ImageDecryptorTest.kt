package com.runnersz.reto2.android
import org.junit.Test
import org.junit.Assert.assertThrows
class ImageDecryptorTest{
 @Test fun rejectsInvalidImage(){val api=ApiClient("http://127.0.0.1:1","t");assertThrows(Exception::class.java){ImageDecryptor(api).decryptPng(byteArrayOf(1),"pw")}}
}