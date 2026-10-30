package com.runnersz.reto2.android.ui
import android.os.Bundle
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.runnersz.reto2.android.data.*
import kotlin.concurrent.thread

/** Órdenes César delegadas a la API. Author: Jairo. */
class OrderFragment:Fragment(){
 private var input:EditText?=null
 private var result:TextView?=null
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)load(uri)}
 override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,state:Bundle?):View{
  val ctx=requireContext()
  val root=LinearLayout(ctx).apply{orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24)}
  input=EditText(ctx).apply{hint="Orden cifrada";minLines=3;isSaveEnabled=false}
  val shift=EditText(ctx).apply{hint="Desplazamiento 1–25";inputType=2}
  result=TextView(ctx).apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;setTextIsSelectable(true)}
  val open=Button(ctx).apply{text="Abrir TXT";setOnClickListener{picker.launch(arrayOf("text/plain"))}}
  val send=Button(ctx).apply{text="Descifrar orden";setOnClickListener{
   val n=shift.text.toString().toIntOrNull();val settings=ServerSettings(ctx)
   if(n==null||n !in 1..25||settings.estaIncompleto()){result?.text="Usa 1–25 y configura Ajustes";return@setOnClickListener}
   isEnabled=false
   ApiClient(settings.direccion,settings.token).caesarDecrypt(input?.text.toString(),n){r->
    if(view==null)return@caesarDecrypt
    isEnabled=true
    result?.text=when(r){is ApiResult.Success->r.data;is ApiResult.Failure->"Error: comprueba conexión, token y texto"}
   }
  }}
  for(v in listOf(input,open,shift,send,result))root.addView(v)
  for(i in 0 until root.childCount)root.getChildAt(i).minimumHeight=(48*resources.displayMetrics.density).toInt()
  arguments?.getString("uri")?.let{load(Uri.parse(it))}
  return ScrollView(ctx).apply{addView(root)}
 }
 private fun load(uri:Uri){
  val ctx=context?:return
  thread{
   try{
    val text=ctx.contentResolver.openInputStream(uri).use{stream->
     requireNotNull(stream);val out=java.io.ByteArrayOutputStream();val buf=ByteArray(4096)
     while(true){val n=stream.read(buf);if(n<0)break;require(out.size()+n<=1024*1024);out.write(buf,0,n)}
     out.toString("UTF-8")
    }
    activity?.runOnUiThread{input?.setText(text)}
   }catch(e:Exception){activity?.runOnUiThread{result?.text="TXT ilegible o mayor de 1 MiB"}}
  }
 }
 override fun onDestroyView(){input=null;result=null;super.onDestroyView()}
}
