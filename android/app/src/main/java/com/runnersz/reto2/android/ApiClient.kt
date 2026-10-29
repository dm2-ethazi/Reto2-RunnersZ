package com.runnersz.reto2.android
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
class ApiClient(private val base:String, private val token:String, private val client:OkHttpClient=OkHttpClient()) {
 private fun request(path:String)=Request.Builder().url(base.trimEnd('/')+path).header("Authorization","Bearer $token")
 fun checkAuth(){client.newCall(request("/api/v1/auth/check").get().build()).execute().use{if(!it.isSuccessful)throw IllegalStateException("API error \${it.code}")}}
 fun decrypt(file:ByteArray,password:String):ByteArray{require(file.isNotEmpty()){"Archivo vacÃ­o"};require(password.isNotEmpty()){"Clave vacÃ­a"};val body=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file","input.enc",file.toRequestBody()).addFormDataPart("password",password).build();return client.newCall(request("/api/v1/files/decrypt").post(body).build()).execute().use{if(!it.isSuccessful)throw IllegalStateException("API error \${it.code}");it.body!!.bytes()}}
 fun caesar(text:String,shift:Int):String{require(shift in 1..25){"Desplazamiento invÃ¡lido"};val body=JSONObject().put("text",text).put("shift",shift).put("mode","decrypt").toString().toRequestBody("application/json".toMediaType());return client.newCall(request("/api/v1/text/caesar").post(body).build()).execute().use{if(!it.isSuccessful)throw IllegalStateException("API error \${it.code}");it.body!!.string()}}
}