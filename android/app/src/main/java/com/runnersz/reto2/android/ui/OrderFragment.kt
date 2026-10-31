package com.runnersz.reto2.android.ui
import android.os.Bundle
import android.net.Uri
import android.view.*
import android.widget.*
import android.graphics.Color
import android.content.res.Configuration
import android.graphics.Typeface
import android.content.res.ColorStateList
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
  val horizontal=resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE
  val root=LinearLayout(ctx).apply{orientation=LinearLayout.VERTICAL;setPadding(if(horizontal)16 else 24,16,if(horizontal)16 else 24,16);setBackgroundColor(Color.rgb(232,220,198))}
  val title=TextView(ctx).apply{text="Orden César";textSize=24f;setTextColor(Color.rgb(27,42,65));typeface=Typeface.DEFAULT_BOLD}
  val validationHint=TextView(ctx).apply{text="1-25";setTextColor(Color.rgb(74,53,32));setPadding(0,8,0,8);visibility=View.VISIBLE}
  input=EditText(ctx).apply{hint="Orden cifrada";minLines=if(horizontal)2 else 4;isSaveEnabled=false;setTextColor(Color.rgb(46,46,51));setHintTextColor(Color.rgb(74,53,32));setPadding(16,12,16,12)}
  val shift=EditText(ctx).apply{hint="Desplazamiento 1–25";inputType=2;setTextColor(Color.rgb(46,46,51));setHintTextColor(Color.rgb(74,53,32));minHeight=(56*resources.displayMetrics.density).toInt()}
  result=TextView(ctx).apply{text="1-25";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;setTextIsSelectable(true);setTextColor(Color.rgb(46,46,51));setPadding(16,16,16,16);setBackgroundColor(Color.rgb(255,255,255))}
  val open=Button(ctx).apply{text="Abrir TXT";setTextColor(Color.rgb(27,42,65));backgroundTintList=ColorStateList.valueOf(Color.rgb(201,169,106));setOnClickListener{picker.launch(arrayOf("text/plain"))}}
  val send=Button(ctx).apply{text="Descifrar orden";setTextColor(Color.rgb(232,220,198));backgroundTintList=ColorStateList.valueOf(Color.rgb(27,42,65));setOnClickListener{
   val n=shift.text.toString().toIntOrNull();val settings=ServerSettings(ctx)
   if(n==null||n !in 1..25||settings.estaIncompleto()){result?.text="Usa un desplazamiento entre 1-25 y configura Ajustes";return@setOnClickListener}
   isEnabled=false
   ApiClient(settings.direccion,settings.token).caesarDecrypt(input?.text.toString(),n){r->
    if(view==null)return@caesarDecrypt
    isEnabled=true
    result?.text=when(r){is ApiResult.Success->r.data;is ApiResult.Failure->"Error: comprueba conexión, token y texto (desplazamiento 1-25)"}
   }
  }}
  for(v in listOf(title,validationHint,input,open,shift,send,result))root.addView(v)
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







