package com.runnersz.reto2.android
import android.graphics.BitmapFactory
class ImageDecryptor(private val api:ApiClient){
 fun decryptPng(encrypted:ByteArray,password:String)=BitmapFactory.decodeByteArray(api.decrypt(encrypted,password),0,api.decrypt(encrypted,password).size) ?: throw IllegalArgumentException("Imagen corrupta")
}