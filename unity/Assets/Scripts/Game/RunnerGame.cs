using System.Collections.Generic;
using UnityEngine;
using RunnersZ.Logic;
namespace RunnersZ.Game {
 public sealed class RunnerGame : MonoBehaviour {
  public Difficulty difficulty = Difficulty.Easy;
  public GameState State { get; private set; }
  public int Round { get; private set; }
  public int Score { get; private set; }
  public int Lives { get; private set; }
  public int HintsRemaining { get; private set; }
  public string Message { get; private set; }
  public string Cipher { get; private set; }
  readonly List<float> enemies = new List<float>();
  AlphabetWheel wheel; bool manuscriptOpen; PhraseBank bank; DifficultyConfig config; int shift, spawned; float clock; string answer="", hint="", feedback=""; bool started;
  public void Begin(Difficulty selected) {
   difficulty=selected; config=new DifficultyConfig(selected); bank=new PhraseBank(System.Environment.TickCount);
   State=GameState.EnCurso; Round=1; Score=0; Lives=3; started=true; PrepareRound();
  }
  void PrepareRound() {
   wheel=new AlphabetWheel(difficulty); manuscriptOpen=false; Message=bank.Next(difficulty); shift=Random.Range(1,26); Cipher=Caesar.Transform(Message,shift);
   HintsRemaining=config.Hints; answer=""; hint=""; feedback="Descifra el mensaje antes de que lleguen.";
   spawned=0; clock=0; enemies.Clear(); enemies.Add(0); spawned++;
  }
  public bool Submit(string value) {
   if(!started || State!=GameState.EnCurso || string.IsNullOrWhiteSpace(value)) return false;
   if(!AnswerValidator.IsCorrect(value,Message)) {
    feedback="Respuesta incorrecta: los enemigos avanzan.";
    for(int i=0;i<enemies.Count;i++) enemies[i]=Mathf.Min(1,enemies[i]+.12f);
    return false;
   }
   Score+=100; if(Round==3){State=GameState.Victoria;feedback="Has protegido la puerta.";enemies.Clear();}
   else {Round++;PrepareRound();} return true;
  }
  public string UseHint() {
   if(State!=GameState.EnCurso || HintsRemaining<=0) return hint;
   if(wheel!=null) { int letters=difficulty==Difficulty.Hard?14:4; for(int i=0;i<letters;i++)wheel.RevealHint(); } int used=config.Hints-HintsRemaining+1; HintsRemaining--; Score=Mathf.Max(0,Score-50);
   var words=Message.Split(' '); hint=string.Join(" ",words,0,Mathf.Min(used,words.Length)); return hint;
  }
  void Update() {
   if(!started || State!=GameState.EnCurso || manuscriptOpen)return;
   clock+=Time.deltaTime;
   if(clock>=config.Interval && spawned<config.Enemies+2*(Round-1)){clock=0;spawned++;enemies.Add(0);}
   float travel=30f-(int)difficulty*5f-(Round-1)*3f;
   for(int i=enemies.Count-1;i>=0;i--){enemies[i]+=Time.deltaTime/travel;if(enemies[i]>=1){enemies.RemoveAt(i);Lives=Mathf.Max(0,Lives-1);feedback="La puerta ha recibido un golpe.";if(Lives==0){State=GameState.Derrota;feedback="La puerta ha caido.";break;}}}
  }


  // Presentation uses a single aspect-preserving canvas and cached styles.
  GUIStyle heading, body, feedbackStyle, small, cipherStyle, fieldStyle, actionStyle, secondaryStyle, letterStyle, badgeStyle;
  PixelFantasyArt pixelArt; Texture2D rounded, stone, cloak; GUIStyle panelStyle; Font manuscriptFont;
  readonly Color ink=new Color(.035f,.055f,.095f);
  readonly Color muted=new Color(.57f,.65f,.73f);
  readonly Color gold=new Color(.79f,.62f,.36f);
  bool stylesReady;
  void InitStyles(){
   if(stylesReady)return;
   rounded=new Texture2D(32,32,TextureFormat.RGBA32,false);
   for(int y=0;y<32;y++)for(int x=0;x<32;x++){
    float cx=Mathf.Clamp(x,2,29),cy=Mathf.Clamp(y,2,29);
    rounded.SetPixel(x,y,new Color(1,1,1,Mathf.Clamp01(2-Mathf.Sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy)))));
   }
   rounded.Apply(); panelStyle=new GUIStyle();panelStyle.normal.background=rounded;panelStyle.border=new RectOffset(8,8,8,8);
   heading=new GUIStyle(GUI.skin.label){fontSize=34,fontStyle=FontStyle.Bold};
   heading.normal.textColor=Color.white;
   body=new GUIStyle(GUI.skin.label){fontSize=18,wordWrap=true};body.normal.textColor=Color.white;
   feedbackStyle=new GUIStyle(body){alignment=TextAnchor.MiddleRight,wordWrap=false};
   small=new GUIStyle(body){fontSize=14};small.normal.textColor=muted;
   cipherStyle=new GUIStyle(body){fontSize=26,fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleLeft};
   actionStyle=new GUIStyle(GUI.skin.button){fontSize=17,fontStyle=FontStyle.Bold,border=new RectOffset(8,8,8,8)};
   foreach(var s in new[]{actionStyle.normal,actionStyle.hover,actionStyle.active,actionStyle.focused}){s.background=rounded;s.textColor=ink;}
   secondaryStyle=new GUIStyle(actionStyle);
   foreach(var s in new[]{secondaryStyle.normal,secondaryStyle.hover,secondaryStyle.active,secondaryStyle.focused})s.textColor=Color.white;
   fieldStyle=new GUIStyle(GUI.skin.textField){fontSize=22,padding=new RectOffset(18,18,12,10)};
   fieldStyle.normal.background=rounded;fieldStyle.focused.background=rounded;fieldStyle.normal.textColor=Color.white;fieldStyle.focused.textColor=Color.white;
   letterStyle=new GUIStyle(body){fontSize=25,fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleCenter};
   badgeStyle=new GUIStyle(small){alignment=TextAnchor.MiddleCenter,fontStyle=FontStyle.Bold};
   manuscriptFont=Font.CreateDynamicFontFromOSFont(new[]{"Georgia","Times New Roman"},34);
   heading.font=manuscriptFont;cipherStyle.font=manuscriptFont;letterStyle.font=manuscriptFont;
   stone=new Texture2D(256,256,TextureFormat.RGBA32,false);
   for(int y=0;y<256;y++)for(int x=0;x<256;x++){
    float n=Mathf.PerlinNoise(x*.13f,y*.13f)*.014f+Mathf.PerlinNoise(x*.025f,y*.025f)*.018f;
    stone.SetPixel(x,y,new Color(.025f+n,.042f+n,.075f+n,1));
   }
   stone.Apply();
   cloak=new Texture2D(32,32,TextureFormat.RGBA32,false);
   for(int y=0;y<32;y++)for(int x=0;x<32;x++)cloak.SetPixel(x,y,new Color(1,1,1,Mathf.Abs(x-15.5f)<(31-y)*.44f+2?1:0));
   cloak.Apply();
   rounded.filterMode=FilterMode.Point;pixelArt=new PixelFantasyArt();stylesReady=true;
  }
  void Fill(Rect r,Color c,bool round=true){if(Event.current.type!=EventType.Repaint)return;var old=GUI.color;GUI.color=c;if(round)panelStyle.Draw(r,GUIContent.none,false,false,false,false);else GUI.DrawTexture(r,Texture2D.whiteTexture);GUI.color=old;}
  void Label(Rect r,string value,GUIStyle style,Color? color=null){ if(style==heading||style==letterStyle||style==cipherStyle){pixelArt.Text(r,value,style.fontSize,style.alignment,color??style.normal.textColor);return;}
   Color old=GUI.contentColor,oldText=style.normal.textColor;GUI.contentColor=Color.white;if(color.HasValue)style.normal.textColor=color.Value;GUI.Label(r,value,style);style.normal.textColor=oldText;GUI.contentColor=old;
  }
  bool Button(Rect r,string value,bool primary=false){
   bool hover=r.Contains(Event.current.mousePosition)&&GUI.enabled;
   Color tint=primary?gold:new Color(.075f,.10f,.15f);
   if(hover)tint=Color.Lerp(tint,new Color(.70f,.57f,.37f),.20f);
   Frame(r,hover?gold:new Color(.30f,.27f,.21f));
   Color old=GUI.backgroundColor;GUI.backgroundColor=tint;
   bool pressed=GUI.Button(new Rect(r.x+1,r.y+1,r.width-2,r.height-2),value,primary?actionStyle:secondaryStyle);
   GUI.backgroundColor=old;return pressed;
  }

  string VisibleCipher {get{return wheel==null?Cipher:wheel.MaskCipher(Cipher,shift);}}
  void Line(float x1,float y1,float x2,float y2,Color color,float width=1){
   if(Mathf.Abs(y2-y1)<.1f){Fill(new Rect(Mathf.Min(x1,x2),y1,Mathf.Max(width,Mathf.Abs(x2-x1)),width),color,false);return;}
   if(Mathf.Abs(x2-x1)<.1f){Fill(new Rect(x1,Mathf.Min(y1,y2),width,Mathf.Max(width,Mathf.Abs(y2-y1))),color,false);return;}
   int steps=Mathf.CeilToInt(Mathf.Max(Mathf.Abs(x2-x1),Mathf.Abs(y2-y1)));
   for(int i=0;i<=steps;i++){
    float t=(float)i/steps;
    Fill(new Rect(Mathf.Lerp(x1,x2,t),Mathf.Lerp(y1,y2,t),width,width),color,false);
   }
  }
  void Sigil(float x,float y,float radius){
   Color c=new Color(gold.r,gold.g,gold.b,.7f);
   Line(x,y-radius,x+radius,y,c);Line(x+radius,y,x,y+radius,c);
   Line(x,y+radius,x-radius,y,c);Line(x-radius,y,x,y-radius,c);
   Line(x,y-radius-5,x,y+radius+5,c);
  }
  void Frame(Rect r,Color surface){
   Fill(r,new Color(.30f,.27f,.21f),false);
   Fill(new Rect(r.x+1,r.y+1,r.width-2,r.height-2),surface,false);
   var edge=new Color(.46f,.37f,.24f);
   float x=r.x+5,y=r.y+5,w=r.width-10,h=r.height-10;
   Line(x,y,x+18,y,edge);Line(x,y,x,y+18,edge);
   Line(x+w-18,y,x+w,y,edge);Line(x+w,y,x+w,y+18,edge);
   Line(x,y+h,x+18,y+h,edge);Line(x,y+h-18,x,y+h,edge);
   Line(x+w-18,y+h,x+w,y+h,edge);Line(x+w,y+h-18,x+w,y+h,edge);
  }
  void DrawSanctuary(){
   GUI.DrawTexture(new Rect(0,0,1200,800),pixelArt.Background);
   // El arte nuevo ocupa solo el carril inferior se�alado en la interfaz.
   if(started){
    Rect lane=new Rect(48,590,1104,90);
    // Uniform scaling: crop vertically around the ground, never squash the art.
    float uvHeight=Mathf.Min(1f,lane.height*pixelArt.Arena.width/(lane.width*pixelArt.Arena.height));
    float uvBottom=Mathf.Clamp(0.19f,0f,1f-uvHeight);
    GUI.DrawTextureWithTexCoords(lane,pixelArt.Arena,new Rect(0f,uvBottom,1f,uvHeight),true);
   }
   Sigil(1075,53,24);
   Line(910,53,1030,53,new Color(.30f,.27f,.20f));
   Line(1120,53,1152,53,new Color(.30f,.27f,.20f));
  }
  void DrawGate(float x,float y,float w,float h){
   // The gate is the collision target at the right edge of the lane.
   const float gateSize=120f, gateBottom=680f;
   GUI.DrawTexture(new Rect(1088f-gateSize*0.5f,gateBottom-gateSize,gateSize,gateSize),pixelArt.Door,ScaleMode.ScaleToFit,true);
  }
  // Sheet espaciado 2656x944: cada frame queda aislado en una ventana de 220x340,
  // asi el recorte no arrastra restos del frame anterior ni del siguiente.
  static readonly Rect[] WraithFrames={
   new Rect(0.027108f,0.324153f,0.082831f,0.360169f), // x72  y298
   new Rect(0.152108f,0.328390f,0.082831f,0.360169f), // x404 y294
   new Rect(0.271837f,0.325212f,0.082831f,0.360169f), // x722 y297
   new Rect(0.395331f,0.325212f,0.082831f,0.360169f), // x1050 y297
   new Rect(0.519202f,0.324153f,0.082831f,0.360169f), // x1379 y298
   new Rect(0.641566f,0.320975f,0.082831f,0.360169f), // x1704 y301
   new Rect(0.761295f,0.322034f,0.082831f,0.360169f), // x2022 y300
   new Rect(0.890060f,0.324153f,0.082831f,0.360169f), // x2364 y298
  };
  void DrawWraith(float x,float y){
   int frame=Mathf.FloorToInt(Time.time*8f)%8;
   float bob=Mathf.Sin(Time.time*8f)*2f;
   Rect uv=WraithFrames[frame];
   const float frameWidth=36f, frameHeight=56f, groundY=677f;
   float left=Mathf.Round((x-frameWidth*0.5f)/3f)*3f;
   GUI.DrawTextureWithTexCoords(new Rect(left,groundY-frameHeight+bob,frameWidth,frameHeight),pixelArt.Wraith,uv,true);
  }
  void OnDestroy(){ if(pixelArt!=null)pixelArt.Dispose();
   if(rounded!=null)Destroy(rounded);if(stone!=null)Destroy(stone);if(cloak!=null)Destroy(cloak);
   if(manuscriptFont!=null)Destroy(manuscriptFont);
  }

  string DifficultyName(){return difficulty==Difficulty.Easy?"F\u00e1cil":difficulty==Difficulty.Normal?"Normal":"Dif\u00edcil";}
  void OnGUI(){
   InitStyles();
   var matrix=GUI.matrix;
   float scale=Mathf.Min(Screen.width/1200f,Screen.height/800f);
   GUI.matrix=Matrix4x4.TRS(new Vector3((Screen.width-1200*scale)/2,(Screen.height-800*scale)/2,0),Quaternion.identity,new Vector3(scale,scale,1));
   DrawSanctuary();
   Fill(new Rect(48,98,1104,1),new Color(.40f,.32f,.20f),false);
   Label(new Rect(48,28,600,42),"RUNNERS Z",heading);
   Label(new Rect(50,73,650,24),"LA PUERTA CIFRADA  /  DESCIFRA \u00b7 RESISTE \u00b7 AVANZA",small);
   GUI.enabled=!manuscriptOpen;
   if(!started)DrawWelcome();else DrawGame();
   GUI.enabled=true;
   if(manuscriptOpen && started){
    Fill(new Rect(0,0,1200,800),new Color(0,0,0,.78f),false);
    GUI.Window(7281,new Rect(65,104,1070,600),DrawManuscriptWindow,GUIContent.none,GUIStyle.none);
   }
   GUI.matrix=matrix;
  }
  void DrawWelcome(){
   Label(new Rect(70,150,1000,60),"Descifra el juramento. Protege la puerta.",heading);
   Label(new Rect(72,218,950,62),"Protege la puerta resolviendo tres mensajes C\u00e9sar. Abre el manuscrito y usa los pares de letras para encontrar el desplazamiento.",body);
   string[] names={"F\u00e1cil","Normal","Dif\u00edcil"};
   string[] details={"Abecedarios completos\n3 pistas por ronda\nDesplazamiento visible","8 letras cifradas ocultas\n2 pistas por ronda\nEncuentra el desplazamiento","14 letras cifradas ocultas\n1 pista por ronda\nEl mayor desaf\u00edo"};
   for(int i=0;i<3;i++){
    float x=70+i*360;
    Frame(new Rect(x,326,340,285),new Color(.055f,.078f,.12f));
    Sigil(x+282,365,14);
    Label(new Rect(x+24,350,290,44),names[i],heading);
    Label(new Rect(x+24,414,292,100),details[i],body);
    if(Button(new Rect(x+24,535,292,50),"Jugar en "+names[i],i==0))Begin((Difficulty)i);
   }
   Label(new Rect(72,653,1055,50),"C\u00f3mo jugar: abre el manuscrito \u2192 cambia los pares \u2192 escribe tu respuesta.\nCada pista cuesta 50 puntos. Los errores acercan a los enemigos.",small);
   if(Button(new Rect(945,730,185,42),"Salir"))Application.Quit();
  }
  void DrawGame(){
   bool playing=State==GameState.EnCurso;
   Frame(new Rect(48,114,1104,66),new Color(.055f,.078f,.12f));
   Label(new Rect(70,130,250,34),"RONDA "+Round+" / 3",body);
   Label(new Rect(330,130,250,34),"VIDAS "+Lives+" / 3",body);
   Label(new Rect(590,130,230,34),"PUNTOS "+Score,body);
   Label(new Rect(940,132,185,28),DifficultyName().ToUpperInvariant(),small);
   for(int i=0;i<3;i++)Fill(new Rect(48+i*374,191,356,4),i<Round?gold:new Color(.17f,.23f,.32f),false);

   Frame(new Rect(48,221,1104,151),new Color(.07f,.095f,.14f));
   Label(new Rect(72,239,600,25),"01 / MANUSCRITO INTERCEPTADO",small,gold);
   Label(new Rect(72,274,810,76),VisibleCipher,cipherStyle,new Color(.92f,.87f,.76f));
   if(Button(new Rect(912,274,215,56),"Abrir manuscrito",true))manuscriptOpen=true;
   Label(new Rect(72,380,1000,26),config.ShowsShift?"Ayuda de nivel: desplazamiento C\u00e9sar +"+shift:"El manuscrito te permite probar las correspondencias de letras.",small);

   Label(new Rect(50,422,700,25),"02 / ESCRIBE EL MENSAJE DESCIFRADO",small);
   GUI.enabled=GUI.enabled && playing;
   GUI.SetNextControlName("answer");
   var bg=GUI.backgroundColor;GUI.backgroundColor=new Color(.16f,.22f,.31f);
   answer=GUI.TextField(new Rect(48,455,842,56),answer,160,fieldStyle);GUI.backgroundColor=bg;
   bool canSubmit=!string.IsNullOrWhiteSpace(answer);
   bool previous=GUI.enabled;GUI.enabled=previous&&canSubmit;
   if(Button(new Rect(910,455,242,56),"Validar respuesta",true))Submit(answer);
   GUI.enabled=previous;
   if(!manuscriptOpen && playing && canSubmit && GUI.GetNameOfFocusedControl()=="answer" && Event.current.type==EventType.KeyDown && Event.current.keyCode==KeyCode.Return){Submit(answer);Event.current.Use();}
   GUI.enabled=previous&&HintsRemaining>0;
   if(Button(new Rect(48,527,255,44),"Pista \u00b7 "+HintsRemaining+" restantes"))UseHint();
   GUI.enabled=previous;
   Label(new Rect(325,526,824,56),string.IsNullOrEmpty(hint)?"Cada pista revela parte del mensaje y recupera pares ocultos.":"Pista: "+hint,small);
   GUI.enabled=!manuscriptOpen;
   Label(new Rect(650,682,502,24),feedback,feedbackStyle);

   Label(new Rect(50,682,1000,25),"LLEGADA DE ENEMIGOS",small);
   foreach(float x in enemies){
    float px=58+x*1030;
    DrawWraith(px,635+Mathf.Sin(x*80)*4);
   }
   // Draw the gate last so the collision target remains visible at the finish.
   DrawGate(1092,626,43,45);
   if(Button(new Rect(48,735,185,42),"Reiniciar"))Begin(difficulty);
   if(Button(new Rect(250,735,225,42),"Cambiar dificultad")){started=false;enemies.Clear();}
   if(Button(new Rect(1002,735,150,42),"Salir"))Application.Quit();
   if(!playing){
    Frame(new Rect(300,260,600,300),new Color(.045f,.065f,.10f));
    Label(new Rect(335,286,530,50),State==GameState.Victoria?"\u00a1Puerta protegida!":"La puerta ha ca\u00eddo",heading);
    Label(new Rect(335,350,530,70),"Puntos: "+Score+"\n"+(State==GameState.Victoria?"Has descifrado los tres mensajes.":"Prueba los pares del manuscrito y vuelve a intentarlo."),body);
    if(Button(new Rect(335,455,530,58),"Volver a jugar",true))Begin(difficulty);
   }
  }
  void DrawManuscriptWindow(int id){
   Frame(new Rect(0,0,1070,600),new Color(.045f,.065f,.10f));
   Sigil(790,46,18);
   Label(new Rect(28,22,770,42),"El archivo de los juramentos",heading,new Color(.92f,.87f,.76f));
   Label(new Rect(30,70,800,24),"JUEGO EN PAUSA \u00b7 "+DifficultyName()+" \u00b7 Desplazamiento +"+wheel.Offset+"  /  Ocultas: "+wheel.HiddenCount,small,gold);
   if(Button(new Rect(909,26,130,42),"Cerrar \u00b7 Esc")){manuscriptOpen=false;return;}
   Frame(new Rect(28,111,1014,75),new Color(.07f,.095f,.14f));
   Label(new Rect(45,119,980,60),VisibleCipher,cipherStyle,new Color(.92f,.87f,.76f));
   Label(new Rect(30,197,1010,40),"Arriba: letra original. Abajo: equivalente cifrado. Los signos ? son letras borradas; las pistas las recuperan.",small);
   for(int group=0;group<2;group++){
    float y=252+group*104;
    Label(new Rect(28,y,110,36),"ORIGINAL",small,gold);
    Label(new Rect(28,y+43,110,36),"CIFRADO",small,gold);
    for(int j=0;j<13;j++){
     int col=group*13+j;float x=139+j*69;
     bool visible=wheel.IsColumnVisible(col);
     bool hovered=new Rect(x,y,60,82).Contains(Event.current.mousePosition);
     Color top=hovered?new Color(.20f,.27f,.31f):new Color(.10f,.15f,.20f);
     Color bottom=!visible?new Color(.045f,.055f,.08f):hovered?new Color(.24f,.30f,.29f):new Color(.12f,.22f,.26f);
     Fill(new Rect(x,y,60,38),top);
     Fill(new Rect(x,y+43,60,38),bottom);
     Label(new Rect(x,y,60,38),wheel.ReferenceAt(col).ToString(),letterStyle,new Color(.92f,.87f,.76f));
     Label(new Rect(x,y+43,60,38),visible?wheel.ShiftedAt(col).ToString():"?",letterStyle,visible?new Color(.92f,.87f,.76f):gold);
    }
   }
   if(Button(new Rect(28,466,245,49),"< Retroceder una letra"))wheel.Move(-1);
   if(Button(new Rect(291,466,245,49),"Avanzar una letra >",true))wheel.Move(1);
   GUI.enabled=State==GameState.EnCurso&&HintsRemaining>0;
   if(Button(new Rect(554,466,235,49),"Pista ("+HintsRemaining+") \u00b7 50 pt"))UseHint();
   GUI.enabled=true;
   if(Button(new Rect(807,466,235,49),"Restablecer a +0"))wheel.Move(-wheel.Offset);
   Label(new Rect(30,531,1010,54),string.IsNullOrEmpty(hint)?"Orden aleatorio fijo durante la ronda \u00b7 Flechas del teclado para cambiar \u00b7 Esc para volver":"Pista: "+hint,small);
   if(Event.current.type==EventType.KeyDown){
    if(Event.current.keyCode==KeyCode.RightArrow){wheel.Move(1);Event.current.Use();}
    else if(Event.current.keyCode==KeyCode.LeftArrow){wheel.Move(-1);Event.current.Use();}
    else if(Event.current.keyCode==KeyCode.Escape){manuscriptOpen=false;Event.current.Use();}
   }
  }
 }
}
