using System.Collections.Generic;
using UnityEngine;

namespace RunnersZ.Game {
 // La interfaz se mide en unidades virtuales: 1 unidad = Scale pixeles de
 // pantalla. El lienzo ocupa siempre el area segura del dispositivo, asi que
 // Width/Height no bajan nunca del diseno base y todo rect calculado aqui cabe.
 public readonly struct UiFrame {
  public const float DesignWidth=1200f, DesignHeight=800f;
  public readonly float Width, Height, Scale;
  public readonly float Heading, Body, Small, Cipher, Letter, Button, Field;
  public readonly float Touch, Gap, Margin, Pad;

  public UiFrame(float width,float height,float scale,float unitsPerDp){
   Width=width; Height=height; Scale=scale;
   // Los textos se piden en dp y se acotan al diseno para que quepan igual.
   Heading=Mathf.Clamp(26f*unitsPerDp,34f,58f);
   Body=Mathf.Clamp(16f*unitsPerDp,18f,34f);
   Small=Mathf.Clamp(12.5f*unitsPerDp,14f,26f);
   Cipher=Mathf.Clamp(21f*unitsPerDp,26f,46f);
   Letter=Mathf.Clamp(17f*unitsPerDp,24f,40f);
   Button=Mathf.Clamp(15f*unitsPerDp,16f,30f);
   Field=Mathf.Clamp(18f*unitsPerDp,20f,36f);
   // 48 dp es el minimo tactil; el suelo solo protege al escritorio, donde
   // 48 dp medidos en pixeles quedaria por debajo del diseno base.
   Touch=Mathf.Clamp(48f*unitsPerDp,42f,132f);
   Gap=Mathf.Clamp(Body*.5f,12f,24f);
   Margin=Mathf.Clamp(Body*.85f,18f,34f);
   Pad=Mathf.Clamp(Body*.7f,14f,26f);
  }

  public static UiFrame Measure(float screenWidth,float screenHeight,float dpi,Rect safeAreaPixels){
   float w=safeAreaPixels.width>1f?safeAreaPixels.width:screenWidth;
   float h=safeAreaPixels.height>1f?safeAreaPixels.height:screenHeight;
   if(w<320f)w=Mathf.Max(320f,screenWidth);
   if(h<240f)h=Mathf.Max(240f,screenHeight);
   float density=(dpi>1f?dpi:160f)/160f;
   // La tipografia se dimensiona con el ancho: asi no oscila cuando la altura
   // obliga a ampliar el lienzo.
   float scaleWidth=Mathf.Max(.0001f,w/DesignWidth);
   var probe=new UiFrame(0f,0f,scaleWidth,density/scaleWidth);
   float scale=Mathf.Min(scaleWidth,h/probe.RequiredHeight);
   return new UiFrame(w/scale,h/scale,scale,density/scaleWidth);
  }

  public static Vector2 Origin(Rect safeAreaPixels,float screenHeight){
   float x=safeAreaPixels.width>1f?safeAreaPixels.x:0f;
   float top=safeAreaPixels.height>1f?screenHeight-(safeAreaPixels.y+safeAreaPixels.height):0f;
   return new Vector2(x,top);
  }

  public Rect Canvas{get{return new Rect(0f,0f,Width,Height);}}

  public float ContentWidth{get{return Mathf.Min(Width-2f*Margin,1680f);}}
  public float ContentX{get{return (Width-ContentWidth)*.5f;}}
  public Rect Content{get{return new Rect(ContentX,Margin,ContentWidth,Height-2f*Margin);}}

  public float ActionHeight{get{return Mathf.Max(Touch,Field*1.05f);}}
  public float TopBarHeight{get{return Mathf.Max(Touch,Heading+8f);}}
  public float TickHeight{get{return Mathf.Max(4f,Mathf.Round(Body*.18f));}}
  public float CipherPanelHeight{get{return Mathf.Round(Cipher*2.3f+Small+44f);}}
  // Dos lineas para pista y dos para el aviso: con una sola se cortaba el texto.
  public float RestHeight{get{return Mathf.Round(Body*4.6f);}}
  public float SidePanelHeight{get{return Small+9f+ActionHeight*4f+Gap*3.8f+RestHeight+Pad*2f;}}
  public float ArenaMinimum{get{return Mathf.Max(ActionHeight*1.5f,180f);}}
  public float GameHeight{
   get{
    float left=CipherPanelHeight+ArenaMinimum+Gap;
    return Margin*2f+TopBarHeight+Gap+TickHeight+Gap+Mathf.Max(left,SidePanelHeight);
   }
  }
  public float MenuHeight{
   get{return Margin*2f+Heading+10f+Gap+Body*2.6f+Gap+Mathf.Max(240f,ActionHeight*2.6f)+Gap+ActionHeight;}
  }
  // Alto minimo real: lo que piden las filas mas un suelo que deja respirar a la
  // ventana del manuscrito. Sin el, el lienzo crece hasta llenar la pantalla.
  public float RequiredHeight{get{return Mathf.Max(Mathf.Max(GameHeight,MenuHeight),540f);}}
 }

 public static class UiSplit {
  public static Rect Inset(Rect r,float padding){
   float p=Mathf.Max(0f,padding);
   return new Rect(r.x+p,r.y+p,Mathf.Max(0f,r.width-2f*p),Mathf.Max(0f,r.height-2f*p));
  }
  public static Rect[] Columns(Rect area,float gap,int count){
   var parts=new Rect[Mathf.Max(0,count)];
   if(count<=0)return parts;
   float w=(area.width-gap*(count-1))/count;
   for(int i=0;i<count;i++)parts[i]=new Rect(area.x+i*(w+gap),area.y,Mathf.Max(0f,w),area.height);
   return parts;
  }
  public static bool OverlapsAny(Rect candidate,IEnumerable<Rect> others){
   foreach(var other in others)if(candidate.Overlaps(other))return true;
   return false;
  }
 }

 // Pantalla de partida: barra superior, mensaje cifrado y arena a la izquierda,
 // columna de accion a la derecha. Todas las filas se encadenan con un cursor,
 // de modo que no pueden solaparse aunque cambie el tamano de letra.
 public sealed class GameLayout {
  public readonly UiFrame Frame;
  public readonly Rect TopBar, Ticks, CipherPanel, Arena, SidePanel;
  public readonly Rect Title, Hud, Manuscript;
  public readonly Rect StatRound, StatLives, StatScore;
  public readonly Rect CipherLabel, CipherText;
  public readonly Rect FieldLabel, Field, Submit, Hint, HintText, Feedback, Restart, Menu;
  public readonly Rect Outcome;

  public GameLayout(UiFrame f){
   Frame=f;
   float x=f.ContentX, w=f.ContentWidth, m=f.Margin, g=f.Gap;
   float topH=f.TopBarHeight;
   TopBar=new Rect(x,m,w,topH);
   float y=TopBar.yMax+g;
   float tickH=f.TickHeight;
   Ticks=new Rect(x,y,w,tickH);
   y=Ticks.yMax+g;
   float sideW=Mathf.Clamp(w*.34f,420f,580f);
   float leftW=Mathf.Max(320f,w-sideW-g);
   float cipherH=f.CipherPanelHeight;
   CipherPanel=new Rect(x,y,leftW,cipherH);
   float arenaY=CipherPanel.yMax+g;
   Arena=new Rect(x,arenaY,leftW,Mathf.Max(f.ArenaMinimum,f.Height-m-arenaY));
   SidePanel=new Rect(x+leftW+g,y,sideW,Mathf.Max(0f,f.Height-m-y));

   float barPad=f.Pad*.65f;
   Rect bar=new Rect(TopBar.x+barPad,TopBar.y,Mathf.Max(0f,TopBar.width-2f*barPad),TopBar.height);
   float manuscriptW=Mathf.Clamp(bar.width*.26f,f.Touch*1.9f,280f);
   Manuscript=new Rect(bar.xMax-manuscriptW,bar.y,manuscriptW,bar.height);
   float titleW=Mathf.Clamp(bar.width*.32f,200f,420f);
   Title=new Rect(bar.x,bar.y,titleW,bar.height);
   Hud=new Rect(Title.xMax+g,bar.y,Mathf.Max(0f,Manuscript.x-g-(Title.xMax+g)),bar.height);
   var stats=UiSplit.Columns(Hud,g*.6f,3);
   StatRound=stats[0]; StatLives=stats[1]; StatScore=stats[2];

   Rect cipher=UiSplit.Inset(CipherPanel,f.Pad);
   CipherLabel=new Rect(cipher.x,cipher.y,cipher.width,f.Small+9f);
   CipherText=new Rect(cipher.x,CipherLabel.yMax+2f,cipher.width,Mathf.Max(0f,cipher.height-CipherLabel.height-2f));

   Rect side=UiSplit.Inset(SidePanel,f.Pad);
   float action=f.ActionHeight;
   float gap=g*.7f;
   float cursor=side.y;
   FieldLabel=new Rect(side.x,cursor,side.width,f.Small+9f);
   cursor=FieldLabel.yMax+gap*.7f;
   Field=new Rect(side.x,cursor,side.width,action);
   cursor=Field.yMax+gap;
   Submit=new Rect(side.x,cursor,side.width,action);
   cursor=Submit.yMax+gap;
   Hint=new Rect(side.x,cursor,side.width,action);
   cursor=Hint.yMax+gap;
   float bottomY=side.yMax-action;
   float rest=Mathf.Max(0f,bottomY-gap-cursor);
   float split=gap*.4f;
   float half=Mathf.Max(0f,(rest-split)*.5f);
   HintText=new Rect(side.x,cursor,side.width,half);
   Feedback=new Rect(side.x,cursor+half+split,side.width,Mathf.Max(0f,rest-half-split));
   var bottom=UiSplit.Columns(new Rect(side.x,bottomY,side.width,action),gap*.8f,2);
   Restart=bottom[0]; Menu=bottom[1];
   Outcome=UiSplit.Inset(Arena,f.Pad*1.2f);
  }

  public IEnumerable<Rect> Blocks{get{yield return TopBar;yield return Ticks;yield return CipherPanel;yield return Arena;yield return SidePanel;}}
  public IEnumerable<Rect> TopBlocks{get{yield return Title;yield return Hud;yield return Manuscript;}}
  public IEnumerable<Rect> StatBlocks{get{yield return StatRound;yield return StatLives;yield return StatScore;}}
  public IEnumerable<Rect> CipherBlocks{get{yield return CipherLabel;yield return CipherText;}}
  public IEnumerable<Rect> SideBlocks{
   get{yield return FieldLabel;yield return Field;yield return Submit;yield return Hint;
    yield return HintText;yield return Feedback;yield return Restart;yield return Menu;}
  }
  public IEnumerable<Rect> TouchTargets{
   get{yield return Manuscript;yield return Field;yield return Submit;yield return Hint;
    yield return Restart;yield return Menu;}
  }
  public IEnumerable<Rect> All{
   get{
    foreach(var r in Blocks)yield return r;
    foreach(var r in TopBlocks)yield return r;
    foreach(var r in StatBlocks)yield return r;
    foreach(var r in CipherBlocks)yield return r;
    foreach(var r in SideBlocks)yield return r;
   }
  }
 }

 // Menu inicial: titulo, tres tarjetas de dificultad y pie de ayuda. En una
 // pantalla estrecha las tarjetas se reparten el alto disponible.
 public sealed class MenuLayout {
  public readonly UiFrame Frame;
  public readonly Rect Title, Blurb, CardsRow, Help, Quit;
  public readonly Rect[] Cards=new Rect[3];
  public readonly Rect[] CardNames=new Rect[3];
  public readonly Rect[] CardDetails=new Rect[3];
  public readonly Rect[] CardButtons=new Rect[3];

  public MenuLayout(UiFrame f){
   Frame=f;
   float x=f.ContentX, w=f.ContentWidth, m=f.Margin, g=f.Gap;
   Title=new Rect(x,m+4f,w,f.Heading+10f);
   Blurb=new Rect(x,Title.yMax+g*.6f,w,f.Body*2.6f);
   float bottomH=f.ActionHeight;
   float quitW=Mathf.Clamp(w*.22f,f.ActionHeight*2.2f,300f);
   Quit=new Rect(x+w-quitW,f.Height-m-bottomH,quitW,bottomH);
   Help=new Rect(x,Quit.y,Mathf.Max(0f,w-quitW-g),bottomH);
   float cardsY=Blurb.yMax+g;
   float cardsH=Mathf.Max(f.ActionHeight*2.4f,Help.y-g-cardsY);
   CardsRow=new Rect(x,cardsY,w,cardsH);
   var cards=UiSplit.Columns(CardsRow,g,3);
   for(int i=0;i<3;i++){
    Cards[i]=cards[i];
    Rect inner=UiSplit.Inset(cards[i],f.Pad);
    CardNames[i]=new Rect(inner.x,inner.y,inner.width,f.Heading+8f);
    float btnH=f.ActionHeight;
    CardButtons[i]=new Rect(inner.x,inner.yMax-btnH,inner.width,btnH);
    float detailsY=CardNames[i].yMax+g*.4f;
    CardDetails[i]=new Rect(inner.x,detailsY,inner.width,Mathf.Max(0f,CardButtons[i].y-g*.4f-detailsY));
   }
  }

  public IEnumerable<Rect> Blocks{get{yield return Title;yield return Blurb;yield return CardsRow;yield return Help;yield return Quit;}}
  public IEnumerable<Rect> CardBlocks{
   get{
    for(int i=0;i<3;i++){yield return CardNames[i];yield return CardDetails[i];yield return CardButtons[i];}
   }
  }
  public IEnumerable<Rect> TouchTargets{get{yield return CardButtons[0];yield return CardButtons[1];yield return CardButtons[2];yield return Quit;}}
  public IEnumerable<Rect> All{
   get{
    foreach(var r in Blocks)yield return r;
    foreach(var r in CardBlocks)yield return r;
   }
  }
 }

 // Manuscrito: ventana emergente con el par original/cifrado por columna. Las
 // celdas se derivan del alto disponible, asi que la rueda nunca invade el pie.
 public sealed class ManuscriptLayout {
  public readonly UiFrame Frame;
  public readonly Rect Window, Header, Title, Close, Subtitle, Banner, BannerText, WheelArea;
  public readonly Rect Back, Next, Hint, Reset, Footer;
  public readonly float CellWidth, CellHeight, LabelWidth, GroupGap, RowGap;

  public ManuscriptLayout(UiFrame f){
   Frame=f;
   float g=f.Gap;
   // Casi todo el lienzo: asi no asoma ningun control de la partida por detras.
   float pad=f.Margin*.5f;
   Window=new Rect(pad,pad,Mathf.Max(0f,f.Width-2f*pad),Mathf.Max(0f,f.Height-2f*pad));
   Rect inner=UiSplit.Inset(Window,f.Pad*.85f);
   float headerH=Mathf.Max(f.Touch,f.Heading+8f);
   Header=new Rect(inner.x,inner.y,inner.width,headerH);
   float closeW=Mathf.Clamp(inner.width*.16f,f.Touch*1.6f,220f);
   Close=new Rect(inner.xMax-closeW,Header.y,closeW,headerH);
   Title=new Rect(inner.x,Header.y,Mathf.Max(0f,inner.width-closeW-g),headerH);
   Subtitle=new Rect(inner.x,Header.yMax+4f,inner.width,f.Small+10f);
   float bannerY=Subtitle.yMax+g*.6f;
   Banner=new Rect(inner.x,bannerY,inner.width,Mathf.Round(f.Cipher*1.7f+18f));
   BannerText=UiSplit.Inset(Banner,f.Body*.5f);

   // El pie lleva hasta dos lineas: con una sola se cortaban los textos largos.
   float footerTextH=Mathf.Round(f.Body*2.1f);
   float footerH=f.ActionHeight+10f+footerTextH;
   float wheelY=Banner.yMax+g*.6f;
   float wheelH=Mathf.Max(0f,inner.yMax-footerH-12f-wheelY);
   WheelArea=new Rect(inner.x,wheelY,inner.width,wheelH);

   LabelWidth=Mathf.Clamp(inner.width*.075f,72f,120f);
   GroupGap=Mathf.Max(8f,g*.6f);
   RowGap=Mathf.Max(3f,Mathf.Round(f.Body*.18f));
   // 2 unidades de separacion por columna deben caber dentro de la rueda.
   CellWidth=Mathf.Max(18f,(WheelArea.width-LabelWidth-12f-26f)/13f);
   float byHeight=(WheelArea.height-GroupGap)/(4f)-RowGap;
   CellHeight=Mathf.Max(28f,Mathf.Min(CellWidth*1.3f,byHeight));

   float buttonsY=inner.yMax-footerH;
   var buttons=UiSplit.Columns(new Rect(inner.x,buttonsY,inner.width,f.ActionHeight),g*.8f,4);
   Back=buttons[0]; Next=buttons[1]; Hint=buttons[2]; Reset=buttons[3];
   Footer=new Rect(inner.x,buttonsY+f.ActionHeight+10f,inner.width,footerTextH);
  }

  public float GroupTop(int group){return WheelArea.y+group*(CellHeight*2f+RowGap+GroupGap);}
  public Rect GroupLabel(int group){return new Rect(WheelArea.x,GroupTop(group),LabelWidth,CellHeight*2f+RowGap);}
  public Rect Cell(int group,int column,int row){
   float x=WheelArea.x+LabelWidth+12f+column*(CellWidth+2f);
   float y=GroupTop(group)+row*(CellHeight+RowGap);
   return new Rect(x,y,CellWidth,CellHeight);
  }
  public IEnumerable<Rect> Cells{
   get{
    for(int group=0;group<2;group++)for(int column=0;column<13;column++)
     for(int row=0;row<2;row++)yield return Cell(group,column,row);
   }
  }
  public IEnumerable<Rect> Blocks{
   get{yield return Header;yield return Subtitle;yield return Banner;yield return WheelArea;
    yield return Back;yield return Next;yield return Hint;yield return Reset;yield return Footer;}
  }
  public IEnumerable<Rect> HeaderBlocks{get{yield return Title;yield return Close;}}
  public IEnumerable<Rect> TouchTargets{get{yield return Close;yield return Back;yield return Next;yield return Hint;yield return Reset;}}
  public IEnumerable<Rect> All{
   get{
    foreach(var r in Blocks)yield return r;
    foreach(var r in HeaderBlocks)yield return r;
    foreach(var r in Cells)yield return r;
   }
  }
 }
}
