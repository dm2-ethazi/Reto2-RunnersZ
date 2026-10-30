package com.runnersz.reto2.android

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.runnersz.reto2.android.data.ServerSettings
import com.runnersz.reto2.android.databinding.ActivityMainBinding
import com.runnersz.reto2.android.ui.*

/** Navegación y recepción de archivos. Author: Jairo. */
class MainActivity:AppCompatActivity(){
 private lateinit var binding:ActivityMainBinding
 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  binding=ActivityMainBinding.inflate(layoutInflater)
  setContentView(binding.root)
  binding.root.setOnApplyWindowInsetsListener { v, insets -> v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom);insets }
  binding.barraSuperior.setOnMenuItemClickListener{
   if(it.itemId==R.id.opcionAjustes){show(SettingsFragment());true}else false
  }
  binding.navegacionInferior.setOnItemSelectedListener{
   show(if(it.itemId==R.id.navOrden)OrderFragment()else ImageFragment());true
  }
  if(state==null){
   if(!openIncoming(intent)){if(ServerSettings(this).estaIncompleto())show(SettingsFragment())else show(ImageFragment())}
  }
 }
 override fun onNewIntent(incoming:Intent){
  super.onNewIntent(incoming);setIntent(incoming);openIncoming(incoming)
 }
 @Suppress("DEPRECATION")
 private fun openIncoming(incoming:Intent):Boolean{
  val uri:Uri?=if(incoming.action==Intent.ACTION_SEND)incoming.getParcelableExtra(Intent.EXTRA_STREAM) else incoming.data
  if(uri==null)return false
  val destination:Fragment=if(incoming.type?.startsWith("text/")==true)OrderFragment()else ImageFragment()
  destination.arguments=Bundle().apply{putString("uri",uri.toString())}
  show(destination);return true
 }
 private fun show(fragment:Fragment){supportFragmentManager.beginTransaction().replace(R.id.contenedor,fragment).commit()}
}
