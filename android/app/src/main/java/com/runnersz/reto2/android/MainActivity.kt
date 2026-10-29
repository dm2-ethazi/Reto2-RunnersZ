package com.runnersz.reto2.android
import android.app.Activity
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import kotlin.concurrent.thread

class MainActivity:Activity(){
 private lateinit var status:TextView
 private lateinit var image:ImageView
 private var selected:ByteArray?=null
 override fun onCreate(state:Bundle?){super.onCreate(state);showImageScreen()}
 private fun showImageScreen(){
  val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(24,24,24,24)
  val url=EditText(this);url.hint="API: http://10.0.2.2:8000";val token=EditText(this);token.hint="Token"
  val password=EditText(this);password.hint="Clave AES";password.inputType=129
  val choose=Button(this);choose.text="Abrir .png.enc";val decrypt=Button(this);decrypt.text="Descifrar y mostrar"
  status=TextView(this);status.text="Selecciona un fichero";image=ImageView(this);image.adjustViewBounds=true
  root.addView(url);root.addView(token);root.addView(password);root.addView(choose);root.addView(decrypt);root.addView(status);root.addView(image);setContentView(root)
  choose.setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),10)}
  decrypt.setOnClickListener{val data=selected;if(data==null){status.text="Selecciona un fichero";return@setOnClickListener};thread{try{val s=ConnectionSettings(url.text.toString(),token.text.toString());val bytes=ApiClient(s.normalizedUrl(),token.text.toString()).decrypt(data,password.text.toString());val bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:throw IllegalArgumentException("Imagen corrupta");runOnUiThread{image.setImageBitmap(bitmap);status.text="Imagen descifrada; integridad recibida de la API"}}catch(e:Exception){runOnUiThread{status.text="Error: "+e.message}}}}
 }
 override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data);if(request==10&&result==RESULT_OK){try{selected=data?.data?.let{contentResolver.openInputStream(it)?.use{stream->stream.readBytes()}};status.text=if(selected!=null)"Fichero cargado" else "Fichero vacÃ­o"}catch(e:Exception){status.text="No se pudo abrir: "+e.message}}}
}