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
   assertTrue(await<ByteArray>{api().decryptFile(f,"p",it)} is ApiResult.Failure)
  }finally{f.delete()}
 }
 @Test fun decryptSendsMultipartAndAcceptsVerifiedBytes(){
  val f=File.createTempFile("image",".enc");f.writeBytes(byteArrayOf(1,2))
  try{
   server.enqueue(MockResponse().setHeader("X-Integrity","verified").setBody("PNG"))
   assertArrayEquals("PNG".toByteArray(),(await<ByteArray>{api().decryptFile(f," ",it)} as ApiResult.Success).data)
   val req=server.takeRequest()
   assertEquals("/api/v1/files/decrypt",req.path)
   val body=req.body.readUtf8()
   assertTrue(body.contains("name=\"file\""))
   assertTrue(body.contains("name=\"password\""))
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
