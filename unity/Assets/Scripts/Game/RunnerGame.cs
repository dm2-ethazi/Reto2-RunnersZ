using System.Collections.Generic;
using UnityEngine;
using RunnersZ.Logic;

namespace RunnersZ.Game {
 public sealed class RunnerGame : MonoBehaviour {
  public Difficulty difficulty = Difficulty.Easy;
  public GameState State { get; private set; }
  public int Round { get; private set; }
  public int Lives { get; private set; }
  public int HintsRemaining { get; private set; }
  public string Message { get; private set; }
  public string Cipher { get; private set; }
  readonly List<float> enemies = new List<float>();
  // 0..1: pulso rojo del paisaje mientras se apaga el golpe de la puerta.
  float doorBleed;
  // Avanza solo con la partida en curso: congela la animacion al caer la puerta.
  float enemyClock;
  AlphabetWheel wheel; bool manuscriptOpen; PhraseBank bank; DifficultyConfig config; int shift, spawned; float nextSpawn; string answer="", hint="", feedback=""; bool started, backPressed;
  // Teclado propio del juego: el campo de respuesta deja de ser un control de
  // texto, asi que Android no puede levantar su teclado encima de la partida.
  bool keypadOpen;
  const string KeypadLetters="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public void Begin(Difficulty selected) {
   difficulty=selected; config=new DifficultyConfig(selected); bank=new PhraseBank(System.Environment.TickCount);
   State=GameState.EnCurso; Round=1; Lives=3; started=true; doorBleed=0f; enemyClock=0f; PrepareRound();
  }
  void PrepareRound() {
   wheel=new AlphabetWheel(difficulty); manuscriptOpen=false; Message=bank.Next(difficulty); shift=Random.Range(1,26); Cipher=Caesar.Transform(Message,shift);
   HintsRemaining=config.Hints; answer=""; hint=""; feedback="Descifra el mensaje antes de que lleguen."; keypadOpen=false;
   spawned=0; enemies.Clear(); enemies.Add(0); spawned++; nextSpawn=SpawnDelay();
  }
  public bool Submit(string value) {
   if(!started || State!=GameState.EnCurso || string.IsNullOrWhiteSpace(value)) return false;
   if(!AnswerValidator.IsCorrect(value,Message)) {
    feedback="Respuesta incorrecta: los enemigos avanzan.";
    // Un fallo solo acerca al espectro que va en cabeza: asi no se amontonan
    // todos en la puerta de golpe y cada error se nota como un paso.
    int closest=-1;
    for(int i=0;i<enemies.Count;i++) if(closest<0||enemies[i]>enemies[closest]) closest=i;
    if(closest>=0) enemies[closest]=Mathf.Min(.97f,enemies[closest]+.16f);
    return false;
   }
   if(Round==3){State=GameState.Victoria;feedback="Has protegido la puerta.";enemies.Clear();}
   else {Round++;PrepareRound();} return true;
  }
  public string UseHint() {
   if(State!=GameState.EnCurso || HintsRemaining<=0) return hint;
   // Cada pista destapa tres pares; el resto del cifrado sigue con "?".
   if(wheel!=null) { int letters=3; for(int i=0;i<letters;i++)wheel.RevealHint(); } int used=config.Hints-HintsRemaining+1; HintsRemaining--;
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
   // El pulso se apaga solo, aunque la partida ya este perdida o en pausa.
   doorBleed=Mathf.MoveTowards(doorBleed,0f,Time.deltaTime*.42f);
   // Con la puerta caida o la partida ganada el teclado propio se cierra solo.
   if(!started || State!=GameState.EnCurso)keypadOpen=false;
   if(!started || State!=GameState.EnCurso || manuscriptOpen)return;
   // Los espectros se paran con la partida: sobre la ventana de derrota ya no
   // caminan, ni desplazandose ni moviendo las piernas.
   enemyClock+=Time.deltaTime;
   float travel=TravelTime();
   // Primero los que llegan. Cada espectro que entra resta una vida y el bucle
   // no se corta: antes un break dejaba a los demas clavados en la puerta sin
   // descontar, y el jugador perdia una vida en vez de tres.
   int hits=0;
   for(int i=enemies.Count-1;i>=0;i--){
    enemies[i]+=Time.deltaTime/travel;
    if(enemies[i]<1f)continue;
    enemies.RemoveAt(i);hits++;
   }
   if(hits>0){RegisterHits(hits);return;}
   // Despues el turno de aparicion: de uno en uno y con margen aleatorio, para
   // que la dificultad marque el ritmo sin que salgan dos en cadena.
   if(spawned<config.Enemies+2*(Round-1)){
    nextSpawn-=Time.deltaTime;
    if(nextSpawn<=0f){nextSpawn=SpawnDelay();spawned++;enemies.Add(0);}
   }
  }

  float TravelTime(){return 30f-(int)difficulty*5f-(Round-1)*3f;}

  float SpawnDelay(){
   float baseInterval=Mathf.Max(5f,config.Interval-(Round-1)*2f);
   return baseInterval*Random.Range(.85f,1.15f);
  }

  void RegisterHits(int hits){
   Lives=Mathf.Max(0,Lives-hits);
   // Este golpe abre ya el cartel de derrota: tenirlo de rojo solo se veria
   // asomar por los bordes de la ventana, asi que el pulso no llega a dispararse.
   if(Lives==0){enemies.Clear();State=GameState.Derrota;feedback="La puerta ha caido.";return;}
   doorBleed=1f;
   feedback=hits==1?"La puerta ha recibido un golpe.":"La puerta ha recibido "+hits+" golpes.";
  }

  UiFrame frame; GameLayout gameLayout; MenuLayout menuLayout; ManuscriptLayout manuscriptLayout;
  GUIStyle heading, body, feedbackStyle, small, cipherStyle, fieldStyle, actionStyle, secondaryStyle, letterStyle, statStyle, answerStyle, keypadStyle;
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
   // El valor se pinta como etiqueta: sin fondo propio para no duplicar el marco.
   answerStyle=new GUIStyle(fieldStyle){wordWrap=false,clipping=TextClipping.Clip};
   answerStyle.normal.background=null;answerStyle.hover.background=null;answerStyle.active.background=null;answerStyle.focused.background=null;
   letterStyle=new GUIStyle(body){fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleCenter,wordWrap=false};
   keypadStyle=new GUIStyle(letterStyle){fontSize=18};
   manuscriptFont=LoadManuscriptFont();
   if(manuscriptFont!=null){heading.font=manuscriptFont;cipherStyle.font=manuscriptFont;letterStyle.font=manuscriptFont;}
   // El marcador va centrado en su recuadro y con cuerpo mayor que el resto de la barra.
   statStyle=new GUIStyle(small){alignment=TextAnchor.MiddleCenter};
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
   statStyle.fontSize=Mathf.RoundToInt(frame.Body*.95f);
   body.fontSize=Mathf.RoundToInt(frame.Body);
   small.fontSize=Mathf.RoundToInt(frame.Small);
   cipherStyle.fontSize=Mathf.RoundToInt(frame.Cipher);
   letterStyle.fontSize=Mathf.RoundToInt(frame.Letter);
   feedbackStyle.fontSize=Mathf.RoundToInt(frame.Body*.92f);
   actionStyle.fontSize=Mathf.RoundToInt(frame.Button);
   secondaryStyle.fontSize=Mathf.RoundToInt(frame.Button);
   fieldStyle.fontSize=Mathf.RoundToInt(frame.Field);
   answerStyle.fontSize=Mathf.RoundToInt(frame.Field);
   keypadStyle.fontSize=Mathf.RoundToInt(frame.Button*.95f);
   int pad=Mathf.RoundToInt(frame.Body*.7f);
   fieldStyle.padding=new RectOffset(pad,pad,Mathf.RoundToInt(pad*.45f),Mathf.RoundToInt(pad*.45f));
   answerStyle.padding=new RectOffset(pad,pad,Mathf.RoundToInt(pad*.45f),Mathf.RoundToInt(pad*.45f));
  }

  void Fill(Rect r,Color c,bool round=true){
   if(Event.current.type!=EventType.Repaint)return;
   if(r.width<=0f||r.height<=0f)return;
   var old=GUI.color;GUI.color=c;
   if(round)panelStyle.Draw(r,GUIContent.none,false,false,false,false);else GUI.DrawTexture(r,Texture2D.whiteTexture);
   GUI.color=old;
  }
  void Label(Rect r,string value,GUIStyle style,Color? color=null,TextAnchor? align=null){
   if(style==heading||style==letterStyle||style==cipherStyle||style==keypadStyle){pixelArt.Text(r,value,Mathf.RoundToInt(style.fontSize),align??style.alignment,color??style.normal.textColor);return;}
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
   var matrix=GUI.matrix;
   GUI.matrix=Matrix4x4.TRS(new Vector3(origin.x,origin.y,0f),Quaternion.identity,new Vector3(frame.Scale,frame.Scale,1f));
   GUI.DrawTexture(new Rect(0f,0f,frame.Width,frame.Height),pixelArt.Background);
   if(!started)DrawWelcome();else DrawGame();
   if(manuscriptOpen&&started)DrawManuscript();
   if(keypadOpen&&started&&!manuscriptOpen)DrawKeypad();
   GUI.matrix=matrix;
   if(backPressed){backPressed=false;HandleBack();}
  }

  void HandleBack(){
   if(keypadOpen){keypadOpen=false;return;}
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
   Label(l.Help,"Abre el manuscrito, cambia los pares y escribe la respuesta.\nCada pista destapa tres pares; cada error acerca a los enemigos.",small);
   if(Button(l.Quit,"Salir"))Application.Quit();
  }

  void DrawGame(){
   var l=gameLayout;
   bool playing=State==GameState.EnCurso;
   // Con el manuscrito abierto la partida queda de fondo: sin foco ni clics, para
   // que ningun control de debajo se lleve el toque ni habra el teclado.
   bool uiEnabled=!manuscriptOpen&&!keypadOpen;
   Frame(l.TopBar,surface);
   // El nombre mantiene su rotulo pixel de siempre: solo cambia la alineacion.
   Label(l.Title,"RUNNERS Z",heading,null,TextAnchor.MiddleCenter);
   Label(l.StatRound,"RONDA "+Round+" / 3",statStyle);
   Label(l.StatLives,"VIDAS "+Lives+" / 3",statStyle,Lives<=1?danger:(Color?)null);
   GUI.enabled=uiEnabled;
   if(Button(l.Manuscript,"Manuscrito",true)){manuscriptOpen=true;keypadOpen=false;CloseKeyboard();}

   float segment=l.Ticks.width/3f;
   float gapTick=Mathf.Max(4f,frame.Gap*.5f);
   for(int i=0;i<3;i++)Fill(new Rect(l.Ticks.x+i*segment,l.Ticks.y,Mathf.Max(0f,segment-gapTick),l.Ticks.height),i<Round?gold:new Color(.17f,.23f,.32f),false);

   Frame(l.CipherPanel,panel);
   Label(l.CipherLabel,"01 / MANUSCRITO INTERCEPTADO",small,gold);
   Label(l.CipherText,Cipher,cipherStyle,cream);

   DrawArena(l.Arena);

   Frame(l.SidePanel,surface);
   Label(l.FieldLabel,"02 / ESCRIBE EL MENSAJE DESCIFRADO",small);
   GUI.enabled=playing&&uiEnabled;
   // Sin TextField no hay teclado del sistema: el valor se pinta como etiqueta y
   // el toque abre el teclado propio, que ademas deja ver el cifrado.
   Fill(l.Field,new Color(.16f,.22f,.31f),true);
   Rect fieldText=UiSplit.Inset(l.Field,Mathf.Round(frame.Body*.65f));
   if(string.IsNullOrEmpty(answer))Label(fieldText,"Toca para escribir",small,muted);
   else Label(fieldText,answer,answerStyle,cream);
   if(GUI.enabled&&Event.current.type==EventType.MouseUp&&l.Field.Contains(Event.current.mousePosition)){keypadOpen=true;Event.current.Use();}
   // Un teclado fisico sigue sirviendo para escribir sin abrir el propio.
   if(GUI.enabled&&Event.current.type==EventType.KeyDown&&!manuscriptOpen){
    char typed=Event.current.character;
    if(typed=='\b'){Backspace();Event.current.Use();}
    else if(typed!='\0'&&!char.IsControl(typed)){AddLetter(typed);Event.current.Use();}
   }
   bool canSubmit=!string.IsNullOrWhiteSpace(answer);
   bool previous=GUI.enabled;GUI.enabled=previous&&canSubmit;
   if(Button(l.Submit,"Validar respuesta",true))Submit(answer);
   GUI.enabled=previous;
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
   Label(bodyRect,(State==GameState.Victoria?"Has descifrado los tres mensajes.":"Prueba los pares del manuscrito y vuelve a intentarlo."),body);
   // El panel se dibuja con la partida ya detenida, asi que GUI.enabled llegaba en
   // false y el boton salia bloqueado. Ahora devuelve al selector de dificultad.
   GUI.enabled=!manuscriptOpen&&!keypadOpen;
   if(Button(buttonRect,"Volver al men\u00fa principal",true)){started=false;enemies.Clear();manuscriptOpen=false;keypadOpen=false;}
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
   // Golpe en la puerta: el fondo se muere a latidos irregulares. La lava sube
   // fundida bajo el paisaje con un brillo de ruido suave (no un parpadeo de
   // reloj) y entre latido y latido el carril cae en penumbra hasta apagarse.
   float beat=doorBleed;
   float flick=Mathf.PerlinNoise(Time.time*2.6f,.37f)*.6f+Mathf.PerlinNoise(Time.time*5.3f,.71f)*.4f;
   flick=Mathf.Clamp01(flick*1.9f-.45f);
   float pulse=.22f+.78f*flick;
   float glow=beat*pulse;
   float dark=beat*(1f-pulse)*.85f;
   Color tint=GUI.color;
   if(dark>0f)GUI.color=Color.Lerp(Color.white,new Color(.07f,.02f,.02f),dark);
   GUI.DrawTextureWithTexCoords(lane,pixelArt.Arena,new Rect(0f,uvBottom,1f,uvHeight),true);
   if(glow>0f){
    if(pixelArt.Bleed!=null){
     // La ilustracion entra fundida sobre el paisaje: bleed.png no tiene la
     // proporcion del carril, asi que se llena recortando y sin bandas.
     GUI.color=new Color(1f,1f,1f,Mathf.Clamp01(glow*1.45f));
     GUI.DrawTexture(lane,pixelArt.Bleed,ScaleMode.ScaleAndCrop,true);
    }else{
     GUI.color=Color.Lerp(GUI.color,new Color(1f,.16f,.10f),Mathf.Clamp01(glow*.95f));
     GUI.DrawTextureWithTexCoords(lane,pixelArt.Arena,new Rect(0f,uvBottom,1f,uvHeight),true);
    }
   }
   GUI.color=tint;
   if(glow>0f){
    // Halo en franjas con alfa creciente hacia la puerta: sin degradado se veia
    // un corte recto en mitad del carril.
    float halo=Mathf.Clamp(lane.width*.55f,64f,lane.width);
    float strength=Mathf.Clamp01(glow*.5f);
    const int bands=20;
    float bandW=halo/bands;
    for(int k=0;k<bands;k++){
     float t=(k+1f)/bands;
     Fill(new Rect(lane.xMax-halo+k*bandW,lane.y,bandW+1f,lane.height),new Color(.74f,.07f,.05f,strength*t*t),false);
    }
    Fill(lane,new Color(.55f,.04f,.03f,Mathf.Clamp01(glow*.22f)),false);
   }
   float entityHeight=Mathf.Clamp(lane.height*.48f,frame.Body*1.5f,lane.height*.8f);
   float entityWidth=entityHeight*(36f/56f);
   float gateHeight=Mathf.Clamp(lane.height*.98f,entityHeight,lane.height);
   float gateWidth=gateHeight*.72f;
   // Puerta pegada al extremo derecho del carril, con un margen minimo para que no toque el marco.
   float gateX=lane.xMax-gateWidth*.52f;
   // El suelo visible del carril es su borde inferior: ahi se apoyan la puerta y los
   // enemigos. Antes se usaba el 94 % del alto y quedaba una franja vacia bajo la puerta.
   float groundY=lane.y+lane.height;
   // door.png es 1:1 y el rectangulo del carril es mas alto que ancho: con ScaleToFit el
   // cuadrado se centraba y la base quedaba flotando. Se dibuja cuadrado y apoyado en groundY,
   // igual que los pies de los enemigos, para que la puerta se vea sobre el suelo.
   float gateSize=Mathf.Min(gateWidth,gateHeight);
   GUI.DrawTexture(new Rect(gateX-gateSize*.5f,groundY-gateSize,gateSize,gateSize),pixelArt.Door,ScaleMode.StretchToFill,true);
   // La puerta va primero: los espectros tienen que pasar por delante de ella al
   // llegar, no quedar tapados por el arco justo antes de golpear.
   foreach(float progress in enemies){
    float t=Mathf.Clamp01(progress);
    float px=Mathf.Lerp(lane.x+entityWidth*.5f,gateX-entityWidth*.6f,t);
    DrawWraith(px,groundY,entityWidth,entityHeight);
   }
  }

  void DrawWraith(float centerX,float groundY,float width,float height){
   int index=Mathf.FloorToInt(enemyClock*8f)%8;
   // Los pies van clavados en el suelo: la oscilacion vertical los hacia dar
   // saltitos en vez de avanzar caminando y ya los anima la propia hoja de frames.
   GUI.DrawTextureWithTexCoords(new Rect(centerX-width*.5f,groundY-height,width,height),pixelArt.Wraith,WraithFrames[index],true);
  }

  // Teclado propio: se apoya en el borde inferior del lienzo y deja a la vista la
  // barra superior y el mensaje cifrado, en vez de tapar media pantalla como el
  // teclado del sistema. Las teclas usan el mismo marco y la misma tipografia.
  void DrawKeypad(){
   float w=Mathf.Min(frame.ContentWidth,1040f);
   float pad=Mathf.Round(frame.Pad*.8f);
   float headerH=Mathf.Round(frame.Small+10f);
   float fieldH=frame.ActionHeight;
   float gap=Mathf.Round(frame.Gap*.55f);
   float keyH=Mathf.Max(46f,Mathf.Round(frame.Touch*.95f));
   float keyGap=Mathf.Max(5f,Mathf.Round(frame.Gap*.32f));
   float keysH=keyH*4f+keyGap*3f;
   float h=pad*2f+headerH+gap+fieldH+gap+keysH;
   float boxY=Mathf.Round(frame.Height-frame.Margin*.6f-h);
   Rect box=new Rect(Mathf.Round((frame.Width-w)*.5f),Mathf.Max(frame.Margin,boxY),w,h);
   Fill(new Rect(0f,0f,frame.Width,frame.Height),new Color(0f,0f,0f,.52f),false);
   Frame(box,new Color(.045f,.065f,.10f));
   Rect inner=UiSplit.Inset(box,pad);

   Rect header=new Rect(inner.x,inner.y,inner.width,headerH);
   float closeW=Mathf.Clamp(inner.width*.16f,frame.Touch*1.5f,180f);
   Rect close=new Rect(inner.xMax-closeW,header.y,closeW,headerH);
   Label(new Rect(header.x,header.y,Mathf.Max(0f,header.width-closeW-gap),headerH),"ESCRIBE LA RESPUESTA",small,gold);
   if(Button(close,"Ocultar")){keypadOpen=false;return;}

   Rect row=new Rect(inner.x,header.yMax+gap,inner.width,fieldH);
   float okW=Mathf.Clamp(inner.width*.26f,frame.Touch*2.3f,300f);
   Rect field=new Rect(row.x,row.y,Mathf.Max(0f,row.width-okW-gap),row.height);
   Rect ok=new Rect(field.xMax+gap,row.y,okW,row.height);
   // El marco ya rellena el interior: mismo azul que el campo de la derecha.
   Frame(field,new Color(.16f,.22f,.31f));
   Rect fieldText=UiSplit.Inset(field,Mathf.Round(frame.Body*.65f));
   if(string.IsNullOrEmpty(answer))Label(fieldText,"Toca las letras",small,muted);
   else Label(fieldText,answer,answerStyle,cream);
   if(Event.current.type==EventType.Repaint&&Mathf.Repeat(Time.unscaledTime,1f)<.55f){
    float textW=string.IsNullOrEmpty(answer)?0f:Mathf.Min(fieldText.width,answerStyle.CalcSize(new GUIContent(answer)).x);
    Fill(new Rect(fieldText.x+answerStyle.padding.left+textW+1f,fieldText.y+fieldText.height*.2f,Mathf.Max(2f,frame.Body*.08f),fieldText.height*.6f),gold,false);
   }
   bool canSubmit=!string.IsNullOrWhiteSpace(answer);
   bool previous=GUI.enabled;GUI.enabled=previous&&canSubmit;
   bool send=Button(ok,"Validar",true);
   GUI.enabled=previous;
   if(send){Submit(answer);keypadOpen=false;return;}

   float keyW=(inner.width-keyGap*6f)/7f;
   float top=row.yMax+gap;
   for(int i=0;i<28;i++){
    Rect key=new Rect(inner.x+(i%7)*(keyW+keyGap),top+(i/7)*(keyH+keyGap),keyW,keyH);
    if(i<26){if(KeypadKey(key,KeypadLetters[i].ToString()))AddLetter(KeypadLetters[i]);}
    else if(i==26){if(KeypadKey(key,"BORRAR"))Backspace();}
    else{if(KeypadKey(key,"ESPACIO"))AddLetter(' ');}
   }
  }

  // Tecla con el marco del juego: el fondo no usa el estilo de la skin porque la
  // tipografia de los rotulos se dibuja con la fuente pixel.
  bool KeypadKey(Rect r,string value){
   if(r.width<=0f||r.height<=0f)return false;
   bool hover=r.Contains(Event.current.mousePosition);
   Frame(r,hover?gold:new Color(.30f,.27f,.21f));
   Fill(new Rect(r.x+2f,r.y+2f,r.width-4f,r.height-4f),hover?new Color(.15f,.19f,.25f):new Color(.075f,.10f,.15f),false);
   Label(r,value,value.Length>1?keypadStyle:letterStyle,cream,TextAnchor.MiddleCenter);
   return GUI.Button(r,GUIContent.none,GUIStyle.none);
  }

  void AddLetter(char c){
   if(answer.Length>=160)return;
   // Un unico espacio entre palabras: el validador compara con el mensaje limpio.
   if(c==' '&&(answer.Length==0||answer[answer.Length-1]==' '))return;
   answer+=c;
  }

  void Backspace(){if(answer.Length>0)answer=answer.Substring(0,answer.Length-1);}

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
   Label(l.BannerText,Cipher,cipherStyle,cream);
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
