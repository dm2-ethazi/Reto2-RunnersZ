package com.runnersz.reto2.android.data

import android.os.Looper
import android.os.Handler
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ApiOperationsTest {
 private lateinit var server:MockWebServer
 @Before fun start(){server=MockWebServer();server.start()}
 @After fun stop(){server.shutdown()}
 private fun api()=ApiClient(server.url("/").toString().trimEnd('/'),"test-token")
 private fun <T> await(start:((ApiResult<T>)->Unit)->Unit):ApiResult<T>{
  var result:ApiResult<T>?=null
  start{result=it}
  val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
  while(result==null && System.nanoTime()<end){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10)}
  return requireNotNull(result){"No callback"}
 }
 @Test fun caesarExtractsResultAndEscapesJson(){
  server.enqueue(MockResponse().setBody("""{"result":"ATAQUE"}"""))
  val r=await<String>{api().caesarDecrypt("DWD\"\n",3,it)}
  assertEquals("ATAQUE",(r as ApiResult.Success).data)
  val request=server.takeRequest()
  assertEquals("/api/v1/text/caesar",request.path)
  assertEquals("Bearer test-token",request.getHeader("Authorization"))
  assertEquals("DWD\"\n",org.json.JSONObject(request.body.readUtf8()).getString("text"))
 }
 @Test fun invalidShiftNeverCallsServer(){
  assertThrows(IllegalArgumentException::class.java){api().caesarDecrypt("X",0){}}
  assertEquals(0,server.requestCount)
 }
 @Test fun decryptRequiresIntegrityHeader(){
  val f=File.createTempFile("image",".enc");f.writeBytes(byteArrayOf(1))
  try{
   server.enqueue(MockResponse().setBody("unverified"))
   val r=await<ByteArray>{api().decryptFile(f,"p",it)}
   assertTrue(r is ApiResult.Failure)
   // DJ7: un 200 sin X-Integrity no es «sin conexión», el servidor sí respondió.
   assertEquals("El servidor no confirmó la integridad",(r as ApiResult.Failure).error.texto)
  }finally{f.delete()}
 }
 @Test fun decryptSendsMultipartAndAcceptsVerifiedBytes(){
  val f=File.createTempFile("image",".enc");f.writeBytes(byteArrayOf(1,2))
  try{
   server.enqueue(MockResponse().setHeader("X-Integrity","verified").setBody("PNG"))
   assertArrayEquals("PNG".toByteArray(),(await<ByteArray>{api().decryptFile(f," ",it)} as ApiResult.Success).data)
   val req=server.takeRequest()
   assertEquals("/api/v1/files/decrypt",req.path)
   assertEquals("Bearer test-token",req.getHeader("Authorization"))
   val body=req.body.readUtf8()
   assertTrue(body.contains("name=\"file\""))
   assertTrue(body.contains("name=\"password\""))
  }finally{f.delete()}
 }
 @Test fun decryptTranslatesContractErrors(){
  val f=File.createTempFile("image",".enc");f.writeBytes(byteArrayOf(1))
  try{
   // 401 con code UNAUTHORIZED (§5.1: el code del servidor manda).
   server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"UNAUTHORIZED"}"""))
   val r401=await<ByteArray>{api().decryptFile(f,"p",it)} as ApiResult.Failure
   assertEquals("Token no válido. Revisa los ajustes",r401.error.texto)
   assertEquals(com.runnersz.reto2.android.domain.AccionError.IR_A_AJUSTES,r401.error.accion)
   // 400 EMPTY_FILE ⇒ «El archivo está vacío».
   server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":"EMPTY_FILE"}"""))
   val r400=await<ByteArray>{api().decryptFile(f,"p",it)} as ApiResult.Failure
   assertEquals("El archivo está vacío",r400.error.texto)
   // 413 sin JSON ⇒ mensaje genérico del estado.
   server.enqueue(MockResponse().setResponseCode(413))
   val r413=await<ByteArray>{api().decryptFile(f,"p",it)} as ApiResult.Failure
   assertEquals("El archivo supera el tamaño máximo permitido",r413.error.texto)
   // 500 INTERNAL_ERROR ⇒ aviso con «Reintentar», sin cerrar (§7.2).
   server.enqueue(MockResponse().setResponseCode(500).setBody("""{"code":"INTERNAL_ERROR"}"""))
   val r500=await<ByteArray>{api().decryptFile(f,"p",it)} as ApiResult.Failure
   assertEquals("Error del servidor. Inténtalo de nuevo",r500.error.texto)
   assertEquals(com.runnersz.reto2.android.domain.AccionError.REINTENTAR,r500.error.accion)
  }finally{f.delete()}
 }
 @Test fun uploadTwentyMiBStreamsFromTmpWithoutBuffering(){
  // Genera 20 MiB en tmp por bloques (nunca un ByteArray de 20 MiB)…
  val f=File.createTempFile("big",".enc")
  f.outputStream().use{out->val bloque=ByteArray(1024*1024){(it%251).toByte()};repeat(20){out.write(bloque)}}
  try{
   server.enqueue(MockResponse().setHeader("X-Integrity","verified").setBody("PNG"))
   assertTrue(await<ByteArray>{api().decryptFile(f,"secret",it)} is ApiResult.Success)
   val req=server.takeRequest()
   // …y comprueba el tamaño por cabecera, sin leer el cuerpo en memoria.
   assertTrue("Content-Length>=20MiB",(req.getHeader("Content-Length")?.toLongOrNull()?:0)>=20L*1024*1024)
   assertTrue(req.bodySize>=20L*1024*1024)
   assertEquals("/api/v1/files/decrypt",req.path)
  }finally{f.delete()}
 }
 @Test fun corruptImageResponseBecomesControlledFailure(){
  val f=File.createTempFile("image",".enc");f.writeBytes(byteArrayOf(1))
  try{
   server.enqueue(MockResponse().setResponseCode(422).setBody("""{"code":"DECRYPT_FAILED"}"""))
   assertTrue(await<ByteArray>{api().decryptFile(f,"wrong",it)} is ApiResult.Failure)
  }finally{f.delete()}
 }
}
