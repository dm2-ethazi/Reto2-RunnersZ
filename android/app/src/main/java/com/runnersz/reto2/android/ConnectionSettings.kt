package com.runnersz.reto2.android
data class ConnectionSettings(val baseUrl:String,val token:String){
 init{require(baseUrl.matches(Regex("https?://.+"))){"URL inválida"};require(token.isNotBlank()){"Token requerido"}}
 fun normalizedUrl()=baseUrl.trimEnd('/')
}