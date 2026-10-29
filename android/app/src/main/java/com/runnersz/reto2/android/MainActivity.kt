package com.runnersz.reto2.android
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.*
import kotlin.concurrent.thread
class MainActivity:Activity(){
 private lateinit var status:TextView
 override fun onCreate(state:Bundle?){super.onCreate(state);showSettings()}
 private fun showSettings(){val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(32,32,32,32);root.contentDescription="Ajustes de conexiÃ³n RunnersZ";val url=EditText(this);url.hint="DirecciÃ³n API, por ejemplo http://10.0.2.2:8000";url.minHeight=56;val token=EditText(this);token.hint="Token de API";token.minHeight=56;val test=Button(this);test.text="Probar conexiÃ³n";test.minHeight=56;status=TextView(this);status.text="Configura la conexiÃ³n";status.setTextColor(Color.DKGRAY);root.addView(url);root.addView(token);root.addView(test);root.addView(status);setContentView(root);test.setOnClickListener{status.text="Comprobandoâ€¦";thread{try{val settings=ConnectionSettings(url.text.toString(),token.text.toString());ApiClient(settings.normalizedUrl(),token.text.toString()).checkAuth();runOnUiThread{status.text="ConexiÃ³n verificada"}}catch(e:Exception){runOnUiThread{status.text="Error: "+e.message}}}}}
}