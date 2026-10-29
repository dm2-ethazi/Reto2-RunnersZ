package com.runnersz.reto2.android
import android.graphics.BitmapFactory
class ImageDecryptor(private val api:ApiClient){
 fun decryptPng(encrypted:ByteArray,password:String)=api.decrypt(encrypted,password).let{bytes->BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: throw IllegalArgumentException("Imagen corrupta")}
}