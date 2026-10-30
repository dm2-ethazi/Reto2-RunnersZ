package com.runnersz.reto2.android.ui
import android.os.Bundle
import android.net.Uri
import android.graphics.BitmapFactory
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.runnersz.reto2.android.data.*
import java.io.File
import kotlin.concurrent.thread

/** Cliente de imágenes AES remoto. Author: Jairo. */
class ImageFragment:Fragment(){
 private var status:TextView?=null
 private var picture:ImageView?=null
 private var selected:File?=null
 private var busy=false
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)load(uri)}
 override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,state:Bundle?):View{
  val ctx=requireContext()
  val root=LinearLayout(ctx).apply{orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24)}
  val title=TextView(ctx).apply{text="Imagen AES";textSize=24f}
  val key=EditText(ctx).apply{hint="Contraseña AES";inputType=129;isSaveEnabled=false}
  status=TextView(ctx).apply{text="Selecciona .png.enc";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
  picture=ImageView(ctx).apply{adjustViewBounds=true;contentDescription="Imagen descifrada"}
  val open=Button(ctx).apply{text="Seleccionar archivo";setOnClickListener{if(!busy)picker.launch(arrayOf("*/*"))}}
  val send=Button(ctx).apply{text="Descifrar";setOnClickListener{
   val file=selected;val password=key.text.toString();val settings=ServerSettings(ctx)
   if(busy)return@setOnClickListener
   if(file==null||password.isEmpty()||settings.estaIncompleto()){status?.text="Selecciona archivo, clave y configura Ajustes";return@setOnClickListener}
   busy=true;isEnabled=false;key.text.clear();status?.text="Descifrando…"
   ApiClient(settings.direccion,settings.token).decryptFile(file,password){r->
    busy=false
    if(view==null)return@decryptFile
    isEnabled=true
    when(r){
     is ApiResult.Failure->status?.text="Error: comprueba clave, archivo, token y conexión"
     is ApiResult.Success->thread{
      try{
       require(r.data.size>=8 && r.data.take(8).toByteArray().contentEquals(byteArrayOf(137.toByte(),80,78,71,13,10,26,10)))
       val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
       BitmapFactory.decodeByteArray(r.data,0,r.data.size,bounds)
       require(bounds.outWidth>0 && bounds.outHeight>0)
       var sample=1;while(bounds.outWidth/sample>2048 || bounds.outHeight/sample>2048)sample*=2
       val bitmap=BitmapFactory.decodeByteArray(r.data,0,r.data.size,BitmapFactory.Options().apply{inSampleSize=sample})?:throw IllegalArgumentException()
       activity?.runOnUiThread{if(view!=null){picture?.setImageBitmap(bitmap);status?.text="Integridad verificada"}}
      }catch(e:Exception){activity?.runOnUiThread{status?.text="Imagen corrupta o demasiado grande"}}
     }
    }
   }
  }}
  for(v in listOf(title,open,key,send,status,picture))root.addView(v)
  for(i in 0 until root.childCount)root.getChildAt(i).minimumHeight=(48*resources.displayMetrics.density).toInt()
  arguments?.getString("uri")?.let{load(Uri.parse(it))}
  return ScrollView(ctx).apply{addView(root)}
 }
 private fun load(uri:Uri){
  if(busy)return
  val ctx=context?:return;busy=true
  thread{
   var temp:File?=null
   try{
    temp=File.createTempFile("runnersz-",".enc",ctx.cacheDir)
    ctx.contentResolver.openInputStream(uri).use{input->
     requireNotNull(input)
     temp.outputStream().use{output->
      val buffer=ByteArray(65536);var total=0L
      while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=100L*1024*1024);output.write(buffer,0,n)}
     }
    }
    require(temp.length()>0)
    val ready=temp
    activity?.runOnUiThread{busy=false;if(status!=null){selected?.delete();selected=ready;status?.text="Archivo preparado"}else ready.delete()}?:ready.delete()
   }catch(e:Exception){temp?.delete();activity?.runOnUiThread{busy=false;status?.text="Archivo vacío, ilegible o mayor de 100 MiB"}}
  }
 }
 override fun onDestroyView(){picture?.setImageDrawable(null);picture=null;status=null;selected?.delete();selected=null;super.onDestroyView()}
}
