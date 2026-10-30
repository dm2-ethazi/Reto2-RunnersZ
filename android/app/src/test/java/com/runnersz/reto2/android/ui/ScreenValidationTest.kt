package com.runnersz.reto2.android.ui

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.data.ServerSettings
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ScreenValidationTest {
 private fun views(view:View):List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap{views(view.getChildAt(it))}else emptyList()
 private fun screen(fragment:Fragment,check:(List<View>)->Unit){
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
  val activity=controller.get()
  activity.supportFragmentManager.beginTransaction().replace(R.id.contenedor,fragment).commitNow()
  check(views(requireNotNull(fragment.view)))
  controller.pause().stop().destroy()
 }
 @Test fun imageWithoutFileShowsValidation(){
  screen(ImageFragment()){all->
   all.filterIsInstance<Button>().first{it.text=="Descifrar"}.performClick()
   assertTrue(all.filterIsInstance<TextView>().any{it.text.toString().contains("Selecciona archivo")})
   assertTrue(all.filterIsInstance<EditText>().all{!it.isSaveEnabled})
  }
 }
 @Test fun invalidCaesarShowsValidationWithoutRequest(){
  screen(OrderFragment()){all->
   all.filterIsInstance<EditText>().last().setText("26")
   all.filterIsInstance<Button>().first{it.text=="Descifrar orden"}.performClick()
   assertTrue(all.filterIsInstance<TextView>().any{it.text.toString().contains("1–25")})
  }
 }
 @Test fun firstLaunchHasSettingsAndNavigation(){
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
  val activity=controller.get()
  assertNotNull(activity.findViewById<View>(R.id.navegacionInferior))
  assertNotNull(activity.findViewById<View>(R.id.barraSuperior))
  controller.pause().stop().destroy()
 }
}
