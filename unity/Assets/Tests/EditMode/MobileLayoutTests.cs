#if UNITY_INCLUDE_TESTS
using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;
using RunnersZ.Game;

public class MobileLayoutTests {
 sealed class Device {
  public readonly string Name; public readonly float Width, Height, Dpi, Inset; public readonly bool Physical;
  public Device(string name,float width,float height,float dpi,float inset,bool physical=true){
   Name=name;Width=width;Height=height;Dpi=dpi;Inset=inset;Physical=physical;
  }
  public Rect Safe{get{return new Rect(Inset,0f,Width-2f*Inset,Height);}}
  public override string ToString(){return Name+" "+Width+"x"+Height+"@"+Dpi+"dpi";}
 }

 static readonly Device[] Devices={
  new Device("movil 1080p sin notch",1920f,1080f,400f,0f),
  new Device("movil 19.5:9 con notch",2340f,1080f,440f,72f),
  new Device("movil grande 1440p",3120f,1440f,560f,96f),
  new Device("tablet 4:3",2000f,1200f,240f,0f),
  new Device("tablet alta densidad",2560f,1920f,320f,0f),
  new Device("portatil 1080p",1920f,1080f,96f,0f),
  new Device("ventana pequena",1280f,720f,213f,0f),
  new Device("ultra apaisado",2400f,900f,420f,0f),
  // 640 dpi sobre 1080p serian 3,4 pulgadas: no existe como telefono, pero sirve
  // para comprobar que el layout degrada sin solaparse en vez de romperse.
  new Device("densidad extrema",2340f,1080f,640f,0f,false),
 };

 static IEnumerable<UiFrame> Frames(){
  foreach(var d in Devices){
   var frame=UiFrame.Measure(d.Width,d.Height,d.Dpi,d.Safe);
   Assert.GreaterOrEqual(frame.Width,UiFrame.DesignWidth-.5f,d+" ancho por debajo del diseno");
   Assert.GreaterOrEqual(frame.Height,frame.RequiredHeight-.5f,d+" alto insuficiente para el contenido");
   yield return frame;
  }
 }

 static void Inside(string label,Rect r,UiFrame f){
  Assert.GreaterOrEqual(r.xMin,-.5f,label+": se sale por la izquierda");
  Assert.GreaterOrEqual(r.yMin,-.5f,label+": se sale por arriba");
  Assert.LessOrEqual(r.xMax,f.Width+.5f,label+": se sale por la derecha");
  Assert.LessOrEqual(r.yMax,f.Height+.5f,label+": se sale por abajo");
  Assert.GreaterOrEqual(r.width,-.01f,label+": ancho negativo");
  Assert.GreaterOrEqual(r.height,-.01f,label+": alto negativo");
 }

 static void Disjoint(string label,IEnumerable<Rect> rects){
  var list=new List<Rect>(rects);
  for(int i=0;i<list.Count;i++)for(int j=i+1;j<list.Count;j++)
   Assert.IsFalse(list[i].Overlaps(list[j]),label+": los rectangulos "+i+" y "+j+" se solapan ("+list[i]+" / "+list[j]+")");
 }

 static void Touch(string label,Rect r,UiFrame f){
  Assert.GreaterOrEqual(r.height,f.Touch-.5f,label+": alto tactil por debajo de 48 dp ("+r.height+" < "+f.Touch+")");
  Assert.GreaterOrEqual(r.width,f.Touch*.9f,label+": ancho tactil escaso ("+r.width+")");
 }

 [Test] public void GameScreenFitsAndNeverOverlaps(){
  foreach(var f in Frames()){
   var l=new GameLayout(f);
   foreach(var r in l.All)Inside("juego",r,f);
   Disjoint("juego",l.Blocks);
   Disjoint("barra superior",l.TopBlocks);
   Disjoint("marcador",l.StatBlocks);
   Disjoint("panel cifrado",l.CipherBlocks);
   Disjoint("columna de accion",l.SideBlocks);
   foreach(var r in l.TouchTargets)Touch("juego",r,f);
   Assert.GreaterOrEqual(l.Arena.height,f.ArenaMinimum-.5f,"arena por debajo del minimo");
   Assert.IsTrue(l.Arena.Contains(new Vector2(l.Arena.x+1f,l.Arena.y+1f)),"arena mal situada");
   Assert.IsTrue(l.SidePanel.height>0f,"columna de accion vacia");
  }
 }

 [Test] public void MenuFitsAndKeepsTouchTargets(){
  foreach(var f in Frames()){
   var l=new MenuLayout(f);
   foreach(var r in l.All)Inside("menu",r,f);
   Disjoint("menu",l.Blocks);
   Disjoint("tarjetas",l.CardBlocks);
   foreach(var r in l.TouchTargets)Touch("menu",r,f);
   Assert.IsTrue(l.CardsRow.height>f.ActionHeight,"tarjetas demasiado bajas");
  }
 }

 [Test] public void ManuscriptFitsAndWheelCellsStayApart(){
  foreach(var f in Frames()){
   var l=new ManuscriptLayout(f);
   foreach(var r in l.All)Inside("manuscrito",r,f);
   Disjoint("manuscrito",l.Blocks);
   Disjoint("cabecera",l.HeaderBlocks);
   foreach(var r in l.TouchTargets)Touch("manuscrito",r,f);
   var cells=new List<Rect>(l.Cells);
   Assert.AreEqual(52,cells.Count,"la rueda debe tener 26 columnas con par original y cifrado");
   for(int i=0;i<cells.Count;i++){
    Assert.IsTrue(l.WheelArea.Contains(cells[i].center),"celda "+i+" fuera de la rueda");
    Inside("celda "+i,cells[i],f);
    for(int j=i+1;j<cells.Count;j++)Assert.IsFalse(cells[i].Overlaps(cells[j]),"celdas "+i+" y "+j+" solapadas");
   }
   Assert.IsFalse(l.WheelArea.Overlaps(l.Footer),"la rueda invade el pie");
   Assert.IsFalse(l.WheelArea.Overlaps(l.Banner),"la rueda invade el mensaje");
  }
 }

 [Test] public void SafeAreaInsetsKeepContentOnScreen(){
  var inset=new Device("notch lateral",2400f,1080f,420f,110f);
  var frame=UiFrame.Measure(inset.Width,inset.Height,inset.Dpi,inset.Safe);
  var origin=UiFrame.Origin(inset.Safe,inset.Height);
  Assert.AreEqual(110f,origin.x,.01f,"el lienzo debe empezar donde acaba el recorte");
  Assert.AreEqual(frame.Width*frame.Scale,inset.Safe.width,.5f,"el lienzo debe llenar el area segura");
  Assert.AreEqual(frame.Height*frame.Scale,inset.Safe.height,.5f,"el alto debe llenar el area segura");
 }

 // Los objetivos tactiles se miden en dp reales, no en pixeles: es lo unico que
 // garantiza que se puedan pulsar con el dedo en cualquier densidad.
 [Test] public void TouchTargetsKeepTheirPhysicalSize(){
  foreach(var d in Devices){
   if(!d.Physical)continue;
   var f=UiFrame.Measure(d.Width,d.Height,d.Dpi,d.Safe);
   float density=(d.Dpi>1f?d.Dpi:160f)/160f;
   float smallest=float.MaxValue;
   foreach(var target in new GameLayout(f).TouchTargets)smallest=Mathf.Min(smallest,Mathf.Min(target.width,target.height));
   float touchDp=smallest*f.Scale/density;
   Assert.GreaterOrEqual(touchDp,40f,d+" deja objetivos tactiles por debajo de 40 dp ("+touchDp.ToString("0.0")+" dp)");
  }
 }

 [Test] public void CanvasAlwaysFillsTheSafeArea(){
  var phone=UiFrame.Measure(1920f,1080f,400f,new Rect(0f,0f,1920f,1080f));
  var desktop=UiFrame.Measure(1920f,1080f,96f,new Rect(0f,0f,1920f,1080f));
  Assert.Greater(phone.Body,desktop.Body,"el movil necesita texto mayor");
  foreach(var d in Devices){
   var f=UiFrame.Measure(d.Width,d.Height,d.Dpi,d.Safe);
   Assert.AreEqual(d.Safe.width,f.Width*f.Scale,.5f,d+": barras laterales en lugar de lienzo lleno");
   Assert.AreEqual(d.Safe.height,f.Height*f.Scale,.5f,d+": barras verticales en lugar de lienzo lleno");
  }
  var ultrawide=UiFrame.Measure(2400f,900f,420f,new Rect(0f,0f,2400f,900f));
  Assert.Greater(ultrawide.Width,UiFrame.DesignWidth,"una pantalla 21:9 debe ganar ancho virtual, no barras negras");
 }
}
#endif
