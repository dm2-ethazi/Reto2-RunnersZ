package com.runnersz.reto2.android
import okhttp3.*
class ApiClient(private val base:String,private val token:String){private val client=OkHttpClient()
 fun decrypt(file:ByteArray,password:String):ByteArray{val body=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file","input.enc",file.toRequestBody()).addFormDataPart("password",password).build();return call("/api/v1/files/decrypt",body)}
 fun caesar(text:String,shift:Int):String{val body="{\"text\":\""+text.replace("\\","\\\\").replace("\"","\\\"")+"\" ,\"shift\":"+shift+",\"mode\":\"decrypt\"}";return callText("/api/v1/text/caesar",body)}
 private fun call(path:String,body:RequestBody):ByteArray{val r=client.newCall(Request.Builder().url(base+path).header("Authorization","Bearer ").post(body).build()).execute();if(!r.isSuccessful)throw IllegalStateException("API error "+r.code);return r.body!!.bytes()}
 private fun callText(path:String,json:String):String{val r=client.newCall(Request.Builder().url(base+path).header("Authorization","Bearer ").post(json.toRequestBody("application/json".toMediaType())).build()).execute();if(!r.isSuccessful)throw IllegalStateException("API error "+r.code);return r.body!!.string()}
}