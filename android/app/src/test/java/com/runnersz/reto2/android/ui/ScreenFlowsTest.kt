package com.runnersz.reto2.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.widget.*
import com.runnersz.reto2.android.*
import com.runnersz.reto2.android.data.*
import androidx.fragment.app.Fragment
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import okhttp3.mockwebserver.*
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ScreenFlowsTest{
 private lateinit var server:MockWebServer
 @Before fun start(){server=MockWebServer();server.start()}
 @After fun stop(){server.shutdown()}
 private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 private fun waitFor(condition:()->Boolean){
  val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
  while(!condition() && System.nanoTime()<end){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10)}
  assertTrue("No se completó el flujo",condition())
 }
 private fun host(fragment:Fragment,block:(MainActivity,List<View>)->Unit){
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
  val activity=controller.get()
  ServerSettings(activity).guardar(server.url("/").toString().trimEnd('/'),"demo")
  activity.supportFragmentManager.beginTransaction().replace(R.id.contenedor,fragment).commitNow()
  block(activity,views(requireNotNull(fragment.view)))
  controller.pause().stop().destroy()
 }
 private fun button(all:List<View>,text:String)=all.filterIsInstance<Button>().first{it.text.toString().equals(text,true)}
 @Test fun settingsSaveValidateAndProbe(){
  host(SettingsFragment()){activity,all->
   val address=activity.findViewById<EditText>(R.id.direccion)
   val token=activity.findViewById<EditText>(R.id.token)
   val save=activity.findViewById<View>(R.id.botonGuardar)
   val probe=activity.findViewById<View>(R.id.botonProbar)
   address.setText("wrong");token.setText("");probe.performClick();save.performClick()
   address.setText(server.url("/").toString().trimEnd('/'))
   probe.performClick()
   token.setText("demo");save.performClick()
   assertEquals("demo",ServerSettings(activity).token)
   server.enqueue(MockResponse().setResponseCode(204));probe.performClick()
   waitFor{probe.isEnabled}
   assertTrue(activity.findViewById<TextView>(R.id.textoResultado).text.toString().contains("correcta"))
   server.enqueue(MockResponse().setResponseCode(401));probe.performClick()
   waitFor{probe.isEnabled}
   assertTrue(activity.findViewById<TextView>(R.id.textoResultado).text.toString().contains("Token"))
   UiMessages.aviso(activity.supportFragmentManager.findFragmentById(R.id.contenedor)!!,"Error","Reintentar"){}
  }
 }
 @Test fun settingsPersistenceRejectsInvalidWithoutOverwriting(){
  val context=RuntimeEnvironment.getApplication()
  val settings=ServerSettings(context)
  settings.direccion="";settings.token=""
  assertTrue(settings.estaIncompleto())
  assertTrue(settings.guardar("x","") is ServerSettings.ResultadoGuardado.Invalido)
  assertTrue(settings.guardar("x","demo") is ServerSettings.ResultadoGuardado.Invalido)
  assertTrue(settings.guardar("127.0.0.1:8000"," demo ") is ServerSettings.ResultadoGuardado.Guardado)
  assertFalse(settings.estaIncompleto())
  assertEquals("demo",settings.token)
 }
 @Test fun orderUsesApiAndHandlesFailure(){
  host(OrderFragment()){_,all->
   val edits=all.filterIsInstance<EditText>();edits[0].setText("DWD");edits[1].setText("3")
   val send=button(all,"Descifrar orden")
   server.enqueue(MockResponse().setBody("""{"result":"ATA"}"""))
   send.performClick();waitFor{send.isEnabled}
   assertTrue(all.filterIsInstance<TextView>().any{it.text.toString()=="ATA"})
   server.enqueue(MockResponse().setResponseCode(401));send.performClick();waitFor{send.isEnabled}
   assertTrue(all.filterIsInstance<TextView>().any{it.text.toString().contains("Token no válido")})
  }
 }
 private fun imageFlow(body:ByteArray,code:Int=200,expected:String){
  val uri=Uri.parse("content://test/image.png.enc")
  shadowOf(RuntimeEnvironment.getApplication().contentResolver).registerInputStream(uri,ByteArrayInputStream(byteArrayOf(1,2,3)))
  val fragment=ImageFragment().apply{arguments=Bundle().apply{putString("uri",uri.toString())}}
  host(fragment){_,all->
   waitFor{all.filterIsInstance<TextView>().any{it.text.toString()=="Archivo preparado"}}
   all.filterIsInstance<EditText>().first().setText("key")
   server.enqueue(MockResponse().setResponseCode(code).setHeader("X-Integrity","verified").setBody(okio.Buffer().write(body)))
   button(all,"Descifrar").performClick()
   waitFor{all.filterIsInstance<TextView>().any{it.text.toString().contains(expected)}}
   assertEquals("",all.filterIsInstance<EditText>().first().text.toString())
  }
 }
 @Test fun imageDisplaysVerifiedBitmap(){
  val png=java.io.ByteArrayOutputStream()
  android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888).compress(android.graphics.Bitmap.CompressFormat.PNG,100,png)
  imageFlow(png.toByteArray(),expected="Integridad verificada")
 }
 @Test fun invalidBitmapShowsError(){imageFlow(byteArrayOf(1,2),expected="Imagen corrupta")}
 @Test fun wrongPasswordShowsError(){imageFlow("""{"code":"DECRYPT_FAILED"}""".toByteArray(),422,"Clave incorrecta")}
 @Test fun incomingTextIsRead(){
  val uri=Uri.parse("content://test/order.txt")
  shadowOf(RuntimeEnvironment.getApplication().contentResolver).registerInputStream(uri,ByteArrayInputStream("DWD".toByteArray()))
  val fragment=OrderFragment().apply{arguments=Bundle().apply{putString("uri",uri.toString())}}
  host(fragment){_,all->waitFor{all.filterIsInstance<EditText>().first().text.toString()=="DWD"}}
 }
 @Test fun activityRoutesSharedAndViewedFiles(){
  val uri=Uri.parse("content://test/order.txt")
  shadowOf(RuntimeEnvironment.getApplication().contentResolver).registerInputStream(uri,ByteArrayInputStream("DWD".toByteArray()))
  val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,uri)
  val controller=Robolectric.buildActivity(MainActivity::class.java,intent).setup()
  shadowOf(Looper.getMainLooper()).idle()
  assertTrue(controller.get().supportFragmentManager.findFragmentById(R.id.contenedor) is OrderFragment)
  controller.newIntent(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"text/plain"))
  shadowOf(Looper.getMainLooper()).idle()
  controller.pause().stop().destroy()
 }
}
