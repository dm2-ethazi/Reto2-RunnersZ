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
  AlphabetWheel wheel; bool manuscriptOpen; PhraseBank bank; DifficultyConfig config; int shift, spawned; float clock; string answer="", hint="", feedback=""; bool started, backPressed;
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
  void Awake(){
   // Un runner de teclado y pantalla ancha necesita apaisado y 60 fps estables.
   if(Application.isMobilePlatform){
    Screen.orientation=ScreenOrientation.AutoRotation;
    Screen.autorotateToPortrait=false; Screen.autorotateToPortraitUpsideDown=false;
    Screen.autorotateToLandscapeLeft=true; Screen.autorotateToLandscapeRight=true;
    Application.targetFrameRate=60; QualitySettings.vSyncCount=0;
    Screen.sleepTimeout=SleepTimeout.NeverSleep;
   }
  }
  void Update() {
   if(Input.GetKeyDown(KeyCode.Escape)) backPressed=true;
   if(!started || State!=GameState.EnCurso || manuscriptOpen)return;
   clock+=Time.deltaTime;
   if(clock>=config.Interval && spawned<config.Enemies+2*(Round-1)){clock=0;spawned++;enemies.Add(0);}
   float travel=30f-(int)difficulty*5f-(Round-1)*3f;
   for(int i=enemies.Count-1;i>=0;i--){enemies[i]+=Time.deltaTime/travel;if(enemies[i]>=1){enemies.RemoveAt(i);Lives=Mathf.Max(0,Lives-1);feedback="La puerta ha recibido un golpe.";if(Lives==0){State=GameState.Derrota;feedback="La puerta ha caido.";break;}}}
  }

  UiFrame frame; GameLayout gameLayout; MenuLayout menuLayout; ManuscriptLayout manuscriptLayout;
  GUIStyle heading, body, feedbackStyle, small, cipherStyle, fieldStyle, actionStyle, secondaryStyle, letterStyle;
  PixelFantasyArt pixelArt; Texture2D rounded; GUIStyle panelStyle; Font manuscriptFont;
  readonly Color ink=new Color(.035f,.055f,.095f);
  readonly Color muted=new Color(.57f,.65f,.73f);
  readonly Color gold=new Color(.79f,.62f,.36f);
  readonly Color cream=new Color(.92f,.87f,.76f);
  readonly Color surface=new Color(.055f,.078f,.12f);
  readonly Color panel=new Color(.07f,.095f,.14f);
  readonly Color danger=new Color(.86f,.47f,.42f);
  bool stylesReady;

  void InitStyles(){
   if(stylesReady)return;
   rounded=new Texture2D(32,32,TextureFormat.RGBA32,false);
   for(int y=0;y<32;y++)for(int x=0;x<32;x++){
    float cx=Mathf.Clamp(x,2,29),cy=Mathf.Clamp(y,2,29);
    rounded.SetPixel(x,y,new Color(1,1,1,Mathf.Clamp01(2-Mathf.Sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy)))));
   }
   rounded.Apply(); rounded.filterMode=FilterMode.Point;
   panelStyle=new GUIStyle();panelStyle.normal.background=rounded;panelStyle.border=new RectOffset(8,8,8,8);
   heading=new GUIStyle(GUI.skin.label){fontStyle=FontStyle.Bold};heading.normal.textColor=Color.white;
   body=new GUIStyle(GUI.skin.label){wordWrap=true,clipping=TextClipping.Clip};body.normal.textColor=Color.white;
   feedbackStyle=new GUIStyle(body){wordWrap=true,clipping=TextClipping.Clip};
   small=new GUIStyle(body);small.normal.textColor=muted;
   cipherStyle=new GUIStyle(body){fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleLeft};
   actionStyle=new GUIStyle(GUI.skin.button){fontStyle=FontStyle.Bold,border=new RectOffset(8,8,8,8),padding=new RectOffset(10,10,6,6)};
   foreach(var s in new[]{actionStyle.normal,actionStyle.hover,actionStyle.active,actionStyle.focused}){s.background=rounded;s.textColor=ink;}
   secondaryStyle=new GUIStyle(actionStyle);
   foreach(var s in new[]{secondaryStyle.normal,secondaryStyle.hover,secondaryStyle.active,secondaryStyle.focused})s.textColor=Color.white;
   fieldStyle=new GUIStyle(GUI.skin.textField){alignment=TextAnchor.MiddleLeft,clipping=TextClipping.Clip};
   fieldStyle.normal.background=rounded;fieldStyle.focused.background=rounded;
   fieldStyle.normal.textColor=Color.white;fieldStyle.focused.textColor=Color.white;
   letterStyle=new GUIStyle(body){fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleCenter,wordWrap=false};
   manuscriptFont=LoadManuscriptFont();
   if(manuscriptFont!=null){heading.font=manuscriptFont;cipherStyle.font=manuscriptFont;letterStyle.font=manuscriptFont;}
   pixelArt=new PixelFantasyArt(); stylesReady=true;
  }

  // Android no trae Georgia ni Times New Roman: se cae a la fuente incluida.
  static Font LoadManuscriptFont(){
   if(!Application.isMobilePlatform){
    var serif=Font.CreateDynamicFontFromOSFont(new[]{"Georgia","Times New Roman"},34);
    if(serif!=null)return serif;
   }
   try{return Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");}catch{return null;}
  }

  void ApplyMetrics(){
   heading.fontSize=Mathf.RoundToInt(frame.Heading);
   body.fontSize=Mathf.RoundToInt(frame.Body);
   small.fontSize=Mathf.RoundToInt(frame.Small);
   cipherStyle.fontSize=Mathf.RoundToInt(frame.Cipher);
   letterStyle.fontSize=Mathf.RoundToInt(frame.Letter);
   feedbackStyle.fontSize=Mathf.RoundToInt(frame.Body*.92f);
   actionStyle.fontSize=Mathf.RoundToInt(frame.Button);
   secondaryStyle.fontSize=Mathf.RoundToInt(frame.Button);
   fieldStyle.fontSize=Mathf.RoundToInt(frame.Field);
   int pad=Mathf.RoundToInt(frame.Body*.7f);
   fieldStyle.padding=new RectOffset(pad,pad,Mathf.RoundToInt(pad*.45f),Mathf.RoundToInt(pad*.45f));
  }

  void Fill(Rect r,Color c,bool round=true){
   if(Event.current.type!=EventType.Repaint)return;
   if(r.width<=0f||r.height<=0f)return;
   var old=GUI.color;GUI.color=c;
   if(round)panelStyle.Draw(r,GUIContent.none,false,false,false,false);else GUI.DrawTexture(r,Texture2D.whiteTexture);
   GUI.color=old;
  }
  void Label(Rect r,string value,GUIStyle style,Color? color=null){
   if(style==heading||style==letterStyle||style==cipherStyle){pixelArt.Text(r,value,Mathf.RoundToInt(style.fontSize),style.alignment,color??style.normal.textColor);return;}
   Color oldContent=GUI.contentColor,oldText=style.normal.textColor;
   GUI.contentColor=Color.white;if(color.HasValue)style.normal.textColor=color.Value;
   GUI.Label(r,value,style);
   style.normal.textColor=oldText;GUI.contentColor=oldContent;
  }
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
  void Frame(Rect r,Color fill){
   if(r.width<=0f||r.height<=0f)return;
   float b=Mathf.Max(2f,Mathf.Round(frame.Body*.1f));
   Fill(r,new Color(.30f,.27f,.21f),false);
   Fill(new Rect(r.x+b,r.y+b,r.width-2*b,r.height-2*b),fill,false);
   var edge=new Color(.46f,.37f,.24f);
   float x=r.x+b*2f,y=r.y+b*2f,w=r.width-b*4f,h=r.height-b*4f;
   float c=Mathf.Min(22f,Mathf.Min(w,h)*.14f);
   if(c<b*2f)return;
   Line(x,y,x+c,y,edge,b);Line(x,y,x,y+c,edge,b);
   Line(x+w-c,y,x+w,y,edge,b);Line(x+w,y,x+w,y+c,edge,b);
   Line(x,y+h,x+c,y+h,edge,b);Line(x,y+h-c,x,y+h,edge,b);
   Line(x+w-c,y+h,x+w,y+h,edge,b);Line(x+w,y+h-c,x+w,y+h,edge,b);
  }
  bool Button(Rect r,string value,bool primary=false){
   if(r.width<=0f||r.height<=0f)return false;
   bool hover=r.Contains(Event.current.mousePosition)&&GUI.enabled;
   Color tint=primary?gold:new Color(.075f,.10f,.15f);
   if(hover)tint=Color.Lerp(tint,new Color(.70f,.57f,.37f),.20f);
   Frame(r,hover?gold:new Color(.30f,.27f,.21f));
   Color old=GUI.backgroundColor;GUI.backgroundColor=tint;
   float inset=Mathf.Max(1f,Mathf.Round(frame.Body*.09f));
   bool pressed=GUI.Button(new Rect(r.x+inset,r.y+inset,r.width-2*inset,r.height-2*inset),value,primary?actionStyle:secondaryStyle);
   GUI.backgroundColor=old;return pressed;
  }

  string VisibleCipher {get{return wheel==null?Cipher:wheel.MaskCipher(Cipher,shift);}}

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

  void OnGUI(){
   InitStyles();
   var safe=Screen.safeArea;
   var measured=UiFrame.Measure(Screen.width,Screen.height,Screen.dpi,safe);
   if(gameLayout==null||!Mathf.Approximately(measured.Width,frame.Width)||!Mathf.Approximately(measured.Height,frame.Height)){
    frame=measured;
    gameLayout=new GameLayout(frame);menuLayout=new MenuLayout(frame);manuscriptLayout=new ManuscriptLayout(frame);
   }
   ApplyMetrics();
   var origin=UiFrame.Origin(safe,Screen.height);
   float shift=KeyboardShift(origin);
   var matrix=GUI.matrix;
   GUI.matrix=Matrix4x4.TRS(new Vector3(origin.x,origin.y-shift,0f),Quaternion.identity,new Vector3(frame.Scale,frame.Scale,1f));
   GUI.DrawTexture(new Rect(0f,0f,frame.Width,frame.Height),pixelArt.Background);
   if(!started)DrawWelcome();else DrawGame();
   if(manuscriptOpen&&started)DrawManuscript();
   GUI.matrix=matrix;
   if(backPressed){backPressed=false;HandleBack();}
  }

  // Con el teclado abierto se sube el lienzo lo justo para que el campo de
  // respuesta siga visible; nada se redimensiona ni se solapa.
  float KeyboardShift(Vector2 origin){
   if(!TouchScreenKeyboard.visible||manuscriptOpen||!started)return 0f;
   var area=TouchScreenKeyboard.area;
   if(area.height<=1f||area.height>Screen.height*.85f)return 0f;
   float keyboardTop=Screen.height-(area.y+area.height);
   float fieldTop=origin.y+gameLayout.Field.y*frame.Scale;
   float fieldBottom=origin.y+gameLayout.Field.yMax*frame.Scale;
   if(fieldBottom<=keyboardTop-6f)return 0f;
   float needed=fieldBottom-keyboardTop+10f;
   return Mathf.Clamp(needed,0f,Mathf.Max(0f,fieldTop-10f));
  }

  void HandleBack(){
   if(manuscriptOpen){manuscriptOpen=false;return;}
   if(started){started=false;enemies.Clear();return;}
   Application.Quit();
  }

  void DrawWelcome(){
   var l=menuLayout;
   Label(l.Title,"Descifra el juramento. Protege la puerta.",heading);
   Label(l.Blurb,"Protege la puerta resolviendo tres mensajes C\u00e9sar. Abre el manuscrito y usa los pares de letras para encontrar el desplazamiento.",body);
   string[] names={"F\u00e1cil","Normal","Dif\u00edcil"};
   string[] details={
    "Abecedarios completos. Tres pistas por ronda. El desplazamiento se muestra.",
    "Ocho letras cifradas ocultas. Dos pistas por ronda. Encuentra el desplazamiento.",
    "Catorce letras cifradas ocultas. Una pista por ronda. El mayor desaf\u00edo."
   };
   for(int i=0;i<3;i++){
    Frame(l.Cards[i],surface);
    Label(l.CardNames[i],names[i],heading);
    Label(l.CardDetails[i],details[i],body);
    if(Button(l.CardButtons[i],"Jugar en "+names[i],i==0))Begin((Difficulty)i);
   }
   Label(l.Help,"Abre el manuscrito, cambia los pares y escribe la respuesta.\nCada pista cuesta 50 puntos; cada error acerca a los enemigos.",small);
   if(Button(l.Quit,"Salir"))Application.Quit();
  }

  void DrawGame(){
   var l=gameLayout;
   bool playing=State==GameState.EnCurso;
   // Con el manuscrito abierto la partida queda de fondo: sin foco ni clics, para
   // que ningun control de debajo se lleve el toque ni habra el teclado.
   bool uiEnabled=!manuscriptOpen;
   Frame(l.TopBar,surface);
   Label(l.Title,"RUNNERS Z",heading);
   Label(l.StatRound,"RONDA "+Round+" / 3",small);
   Label(l.StatLives,"VIDAS "+Lives+" / 3",small,Lives<=1?danger:(Color?)null);
   Label(l.StatScore,"PUNTOS "+Score,small);
   GUI.enabled=uiEnabled;
   if(Button(l.Manuscript,"Manuscrito",true)){manuscriptOpen=true;CloseKeyboard();}

   float segment=l.Ticks.width/3f;
   float gapTick=Mathf.Max(4f,frame.Gap*.5f);
   for(int i=0;i<3;i++)Fill(new Rect(l.Ticks.x+i*segment,l.Ticks.y,Mathf.Max(0f,segment-gapTick),l.Ticks.height),i<Round?gold:new Color(.17f,.23f,.32f),false);

   Frame(l.CipherPanel,panel);
   Label(l.CipherLabel,"01 / MANUSCRITO INTERCEPTADO",small,gold);
   Label(l.CipherText,VisibleCipher,cipherStyle,cream);

   DrawArena(l.Arena);

   Frame(l.SidePanel,surface);
   Label(l.FieldLabel,"02 / ESCRIBE EL MENSAJE DESCIFRADO",small);
   GUI.enabled=playing&&uiEnabled;
   GUI.SetNextControlName("answer");
   var bg=GUI.backgroundColor;GUI.backgroundColor=new Color(.16f,.22f,.31f);
   answer=GUI.TextField(l.Field,answer,160,fieldStyle);
   GUI.backgroundColor=bg;
   bool canSubmit=!string.IsNullOrWhiteSpace(answer);
   bool previous=GUI.enabled;GUI.enabled=previous&&canSubmit;
   if(Button(l.Submit,"Validar respuesta",true)){Submit(answer);GUI.FocusControl(null);CloseKeyboard();}
   GUI.enabled=previous;
   if(!manuscriptOpen&&playing&&canSubmit&&GUI.GetNameOfFocusedControl()=="answer"&&Event.current.type==EventType.KeyDown&&Event.current.keyCode==KeyCode.Return){Submit(answer);Event.current.Use();}
   GUI.enabled=previous&&HintsRemaining>0;
   if(Button(l.Hint,"Pista \u00b7 "+HintsRemaining+" restantes"))UseHint();
   GUI.enabled=previous;
   Label(l.HintText,string.IsNullOrEmpty(hint)?"Cada pista revela parte del mensaje.":"Pista: "+hint,small);
   Label(l.Feedback,feedback,feedbackStyle,State==GameState.Derrota?danger:cream);

   // Perdida la partida la seccion de respuesta queda deshabilitada; estos dos
   // botones deben seguir respondiendo, pero nunca con el manuscrito abierto.
   GUI.enabled=uiEnabled;
   if(Button(l.Restart,"Reiniciar"))Begin(difficulty);
   if(Button(l.Menu,"Dificultad")){started=false;enemies.Clear();}

   if(!playing)DrawOutcome(l.Outcome);
   GUI.enabled=true;
  }

  // Soltar el control cerrando el teclado tactil sin depender de la plataforma.
  static void CloseKeyboard(){ GUIUtility.keyboardControl=0; GUI.FocusControl(null); }

  void DrawOutcome(Rect area){
   Frame(area,new Color(.045f,.065f,.10f));
   Rect inner=UiSplit.Inset(area,frame.Pad);
   float buttonH=frame.ActionHeight;
   Rect titleRect=new Rect(inner.x,inner.y,inner.width,frame.Heading+12f);
   Rect buttonRect=new Rect(inner.x,inner.yMax-buttonH,Mathf.Min(inner.width,frame.ActionHeight*5.2f),buttonH);
   Rect bodyRect=new Rect(inner.x,titleRect.yMax+4f,inner.width,Mathf.Max(0f,buttonRect.y-8f-(titleRect.yMax+4f)));
   Label(titleRect,State==GameState.Victoria?"\u00a1Puerta protegida!":"La puerta ha ca\u00eddo",heading);
   Label(bodyRect,"Puntos: "+Score+"\n"+(State==GameState.Victoria?"Has descifrado los tres mensajes.":"Prueba los pares del manuscrito y vuelve a intentarlo."),body);
   // El panel se dibuja con la partida ya detenida, asi que GUI.enabled llegaba en
   // false y el boton salia bloqueado. Ahora devuelve al selector de dificultad.
   GUI.enabled=!manuscriptOpen;
   if(Button(buttonRect,"Volver al men\u00fa principal",true)){started=false;enemies.Clear();manuscriptOpen=false;}
  }

  void DrawArena(Rect area){
   Frame(area,new Color(.045f,.07f,.105f));
   Rect inner=UiSplit.Inset(area,Mathf.Round(frame.Body*.6f));
   Label(new Rect(inner.x,inner.y,inner.width,frame.Small+6f),"LLEGADA DE ENEMIGOS",small,gold);
   // El carril ocupa toda la arena: el arte se recorta por UV, nunca se deforma.
   float captionH=frame.Small+10f;
   Rect lane=new Rect(inner.x,inner.y+captionH,inner.width,Mathf.Max(0f,inner.height-captionH));
   if(lane.width<8f||lane.height<8f)return;
   float uvHeight=Mathf.Min(1f,lane.height*pixelArt.Arena.width/(lane.width*pixelArt.Arena.height));
   float uvBottom=Mathf.Clamp(.19f,0f,1f-uvHeight);
   GUI.DrawTextureWithTexCoords(lane,pixelArt.Arena,new Rect(0f,uvBottom,1f,uvHeight),true);
   float entityHeight=Mathf.Clamp(lane.height*.48f,frame.Body*1.5f,lane.height*.8f);
   float entityWidth=entityHeight*(36f/56f);
   float gateHeight=Mathf.Clamp(lane.height*.98f,entityHeight,lane.height);
   float gateWidth=gateHeight*.72f;
   // Puerta pegada al extremo derecho del carril, con un margen minimo para que no toque el marco.
   float gateX=lane.xMax-gateWidth*.52f;
   // El suelo visible del carril es su borde inferior: ahi se apoyan la puerta y los
   // enemigos. Antes se usaba el 94 % del alto y quedaba una franja vacia bajo la puerta.
   float groundY=lane.y+lane.height;
   foreach(float progress in enemies){
    float t=Mathf.Clamp01(progress);
    float px=Mathf.Lerp(lane.x+entityWidth*.5f,gateX-entityWidth*.6f,t);
    DrawWraith(px,groundY,entityWidth,entityHeight);
   }
   // door.png es 1:1 y el rectangulo del carril es mas alto que ancho: con ScaleToFit el
   // cuadrado se centraba y la base quedaba flotando. Se dibuja cuadrado y apoyado en groundY,
   // igual que los pies de los enemigos, para que la puerta se vea sobre el suelo.
   float gateSize=Mathf.Min(gateWidth,gateHeight);
   GUI.DrawTexture(new Rect(gateX-gateSize*.5f,groundY-gateSize,gateSize,gateSize),pixelArt.Door,ScaleMode.StretchToFill,true);
  }

  void DrawWraith(float centerX,float groundY,float width,float height){
   int index=Mathf.FloorToInt(Time.time*8f)%8;
   // Oscilacion solo hacia arriba: con la base en el borde del carril, bajar los pies
   // haria que se salieran por debajo del terreno.
   float bob=(Mathf.Sin(Time.time*8f)+1f)*.5f*Mathf.Max(1f,height*.04f);
   GUI.DrawTextureWithTexCoords(new Rect(centerX-width*.5f,groundY-height+bob,width,height),pixelArt.Wraith,WraithFrames[index],true);
  }

  void DrawManuscript(){
   var l=manuscriptLayout;
   // La partida queda deshabilitada mientras la ventana esta abierta, asi que no
   // hace falta ninguna capa que la tape: la capa a pantalla completa se quedaba
   // el clic de los botones de esta misma ventana y dejaba vivo el campo de texto.
   GUI.enabled=true;
   Fill(new Rect(0f,0f,frame.Width,frame.Height),new Color(0f,0f,0f,.78f),false);
   Frame(l.Window,new Color(.045f,.065f,.10f));
   Label(l.Title,"El archivo de los juramentos",heading,cream);
   if(Button(l.Close,"Cerrar")){manuscriptOpen=false;CloseKeyboard();return;}
   Label(l.Subtitle,"JUEGO EN PAUSA  \u00b7  "+DifficultyName()+"  \u00b7  Desplazamiento +"+wheel.Offset+"  /  Ocultas: "+wheel.HiddenCount,small,gold);
   Frame(l.Banner,panel);
   Label(l.BannerText,VisibleCipher,cipherStyle,cream);
   for(int group=0;group<2;group++){
    Label(l.GroupLabel(group),"ORIGINAL\nCIFRADO",small,gold);
    for(int column=0;column<13;column++){
     int index=group*13+column;
     bool visible=wheel.IsColumnVisible(index);
     Rect top=l.Cell(group,column,0), bottom=l.Cell(group,column,1);
     bool hovered=top.Contains(Event.current.mousePosition);
     Fill(top,hovered?new Color(.20f,.27f,.31f):new Color(.10f,.15f,.20f));
     Fill(bottom,!visible?new Color(.045f,.055f,.08f):hovered?new Color(.24f,.30f,.29f):new Color(.12f,.22f,.26f));
     Label(top,wheel.ReferenceAt(index).ToString(),letterStyle,cream);
     Label(bottom,visible?wheel.ShiftedAt(index).ToString():"?",letterStyle,visible?cream:gold);
    }
   }
   if(Button(l.Back,"< Retroceder"))wheel.Move(-1);
   if(Button(l.Next,"Avanzar >",true))wheel.Move(1);
   GUI.enabled=State==GameState.EnCurso&&HintsRemaining>0;
   if(Button(l.Hint,"Pista ("+HintsRemaining+")"))UseHint();
   GUI.enabled=true;
   if(Button(l.Reset,"Volver a +0"))wheel.Move(-wheel.Offset);
   Label(l.Footer,string.IsNullOrEmpty(hint)?"Cambia los pares hasta que el cifrado tenga sentido; las pistas recuperan letras borradas.":"Pista: "+hint,small);
   if(Event.current.type==EventType.KeyDown){
    if(Event.current.keyCode==KeyCode.RightArrow){wheel.Move(1);Event.current.Use();}
    else if(Event.current.keyCode==KeyCode.LeftArrow){wheel.Move(-1);Event.current.Use();}
   }
  }

  string DifficultyName(){return difficulty==Difficulty.Easy?"F\u00e1cil":difficulty==Difficulty.Normal?"Normal":"Dif\u00edcil";}

  void OnDestroy(){
   if(pixelArt!=null)pixelArt.Dispose();
   if(rounded!=null)Destroy(rounded);
   if(manuscriptFont!=null)Destroy(manuscriptFont);
  }
 }
}
