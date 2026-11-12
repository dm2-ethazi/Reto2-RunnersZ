#if UNITY_EDITOR
using System.Collections.Generic;
using System.IO;
using UnityEditor;
using UnityEngine;
using RunnersZ.Game;

namespace RunnersZ.Editor {
 // Utilidad de revision: pinta la geometria que devuelven los layouts reales en
 // un PNG por pantalla y perfil, para comprobar a ojo que nada se pisa.
 public static class LayoutPreview {
  struct Profile { public string Name; public int Width, Height; public float Dpi, Inset; }

  static readonly Profile[] Profiles={
   new Profile{Name="movil-19.5-9",Width=2340,Height=1080,Dpi=440f,Inset=72f},
   new Profile{Name="movil-16-9",Width=1920,Height=1080,Dpi=400f,Inset=0f},
   new Profile{Name="tablet-4-3",Width=2000,Height=1200,Dpi=240f,Inset=0f},
   new Profile{Name="ultra-21-9",Width=2400,Height=900,Dpi=420f,Inset=0f},
  };

  static readonly Color[] Palette={
   new Color(.26f,.46f,.72f),new Color(.30f,.62f,.46f),new Color(.72f,.55f,.28f),
   new Color(.60f,.36f,.66f),new Color(.76f,.40f,.36f),new Color(.36f,.64f,.68f),
   new Color(.58f,.64f,.32f),new Color(.44f,.42f,.70f),new Color(.70f,.44f,.54f)
  };

  [MenuItem("RunnersZ/Render Layout Previews")]
  public static void Render(){
   string dir=Path.Combine(Path.GetTempPath(),"runnersz-layout");
   Directory.CreateDirectory(dir);
   foreach(var profile in Profiles){
    var safe=new Rect(profile.Inset,0f,profile.Width-2f*profile.Inset,profile.Height);
    var frame=UiFrame.Measure(profile.Width,profile.Height,profile.Dpi,safe);
    Draw.Log(profile.Name+"  canvas "+frame.Width.ToString("0")+"x"+frame.Height.ToString("0")+" uds  escala "+frame.Scale.ToString("0.00")+"  tactil "+frame.Touch.ToString("0.0"));
    Save(dir,profile.Name+"-menu",frame,Collect(new MenuLayout(frame)));
    Save(dir,profile.Name+"-juego",frame,Collect(new GameLayout(frame)));
    Save(dir,profile.Name+"-manuscrito",frame,Collect(new ManuscriptLayout(frame)));
   }
   Debug.Log("LAYOUT_PREVIEW_DIR:"+dir);
  }

  static List<KeyValuePair<string,Rect>> Collect(MenuLayout l){
   var list=new List<KeyValuePair<string,Rect>>();
   Add(list,"titulo",l.Title);Add(list,"texto",l.Blurb);Add(list,"tarjetas",l.CardsRow);
   Add(list,"ayuda",l.Help);Add(list,"salir",l.Quit);
   for(int i=0;i<3;i++){Add(list,"carta"+i+"-nombre",l.CardNames[i]);Add(list,"carta"+i+"-texto",l.CardDetails[i]);Add(list,"carta"+i+"-boton",l.CardButtons[i]);}
   return list;
  }
  static List<KeyValuePair<string,Rect>> Collect(GameLayout l){
   var list=new List<KeyValuePair<string,Rect>>();
   Add(list,"barra",l.TopBar);Add(list,"progreso",l.Ticks);Add(list,"cifrado",l.CipherPanel);
   Add(list,"arena",l.Arena);Add(list,"accion",l.SidePanel);
   Add(list,"titulo",l.Title);Add(list,"marcador",l.Hud);Add(list,"manuscrito",l.Manuscript);
   Add(list,"estimulo",l.CipherLabel);Add(list,"mensaje",l.CipherText);
   Add(list,"etiqueta",l.FieldLabel);Add(list,"campo",l.Field);Add(list,"validar",l.Submit);
   Add(list,"pista",l.Hint);Add(list,"texto-pista",l.HintText);Add(list,"aviso",l.Feedback);
   Add(list,"reiniciar",l.Restart);Add(list,"dificultad",l.Menu);
   return list;
  }
  static List<KeyValuePair<string,Rect>> Collect(ManuscriptLayout l){
   var list=new List<KeyValuePair<string,Rect>>();
   Add(list,"ventana",l.Window);Add(list,"cabecera",l.Header);Add(list,"titulo",l.Title);
   Add(list,"cerrar",l.Close);Add(list,"subtitulo",l.Subtitle);Add(list,"mensaje",l.Banner);
   Add(list,"rueda",l.WheelArea);Add(list,"retroceder",l.Back);Add(list,"avanzar",l.Next);
   Add(list,"pista",l.Hint);Add(list,"reset",l.Reset);Add(list,"pie",l.Footer);
   for(int g=0;g<2;g++)Add(list,"etiqueta"+g,l.GroupLabel(g));
   for(int g=0;g<2;g++)for(int c=0;c<13;c++){Add(list,"celda"+g+"-"+c+"-a",l.Cell(g,c,0));Add(list,"celda"+g+"-"+c+"-b",l.Cell(g,c,1));}
   return list;
  }

  static void Add(List<KeyValuePair<string,Rect>> list,string name,Rect r){list.Add(new KeyValuePair<string,Rect>(name,r));}

  static void Save(string dir,string name,UiFrame frame,List<KeyValuePair<string,Rect>> rects){
   float k=1200f/frame.Width;
   int w=Mathf.Max(64,Mathf.RoundToInt(frame.Width*k)), h=Mathf.Max(64,Mathf.RoundToInt(frame.Height*k));
   var pixels=new Color32[w*h];
   var background=new Color32(16,20,28,255);
   for(int i=0;i<pixels.Length;i++)pixels[i]=background;
   var texture=new Texture2D(w,h,TextureFormat.RGBA32,false);
   int index=0;
   foreach(var entry in rects){
    var hit=new Rect(entry.Value.x*k,entry.Value.y*k,entry.Value.width*k,entry.Value.height*k);
    var color=Palette[index%Palette.Length];index++;
    Paint(pixels,w,h,hit,color);
    Draw.Log(name+"  "+entry.Key+"  x"+entry.Value.x.ToString("0")+" y"+entry.Value.y.ToString("0")+" w"+entry.Value.width.ToString("0")+" h"+entry.Value.height.ToString("0"));
   }
   texture.SetPixels32(pixels);texture.Apply();
   File.WriteAllBytes(Path.Combine(dir,name+".png"),texture.EncodeToPNG());
   Object.DestroyImmediate(texture);
  }

  static void Paint(Color32[] pixels,int w,int h,Rect r,Color color){
   int x0=Mathf.Clamp(Mathf.FloorToInt(r.x),0,w), y0=Mathf.Clamp(Mathf.FloorToInt(r.y),0,h);
   int x1=Mathf.Clamp(Mathf.CeilToInt(r.xMax),0,w), y1=Mathf.Clamp(Mathf.CeilToInt(r.yMax),0,h);
   var fill=new Color32((byte)(color.r*120),(byte)(color.g*120),(byte)(color.b*120),255);
   var edge=new Color32((byte)(color.r*255),(byte)(color.g*255),(byte)(color.b*255),255);
   for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
    bool border=x==x0||y==y0||x==x1-1||y==y1-1;
    // La textura se guarda en coordenadas de pantalla (y crece hacia abajo).
    pixels[(h-1-y)*w+x]=border?edge:fill;
   }
  }

  static class Draw { public static void Log(string message){Debug.Log("LAYOUT_PREVIEW "+message);} }
 }
}
#endif
