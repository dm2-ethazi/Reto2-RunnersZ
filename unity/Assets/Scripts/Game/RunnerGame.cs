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
  PhraseBank bank; DifficultyConfig config; int shift, spawned; float clock; string answer="", hint="", feedback=""; bool started;
  public void Begin(Difficulty selected) {
   difficulty=selected; config=new DifficultyConfig(selected); bank=new PhraseBank(System.Environment.TickCount);
   State=GameState.EnCurso; Round=1; Score=0; Lives=3; started=true; PrepareRound();
  }
  void PrepareRound() {
   Message=bank.Next(difficulty); shift=Random.Range(1,26); Cipher=Caesar.Transform(Message,shift);
   HintsRemaining=config.Hints; answer=""; hint=""; feedback="Descifra el mensaje antes de que lleguen.";
   spawned=0; clock=0; enemies.Clear(); enemies.Add(0); spawned++;
  }
  public bool Submit(string value) {
   if(!started || State!=GameState.EnCurso) return false;
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
   int used=config.Hints-HintsRemaining+1; HintsRemaining--; Score=Mathf.Max(0,Score-50);
   var words=Message.Split(' '); hint=string.Join(" ",words,0,Mathf.Min(used,words.Length)); return hint;
  }
  void Update() {
   if(!started || State!=GameState.EnCurso)return;
   clock+=Time.deltaTime;
   if(clock>=config.Interval && spawned<config.Enemies+2*(Round-1)){clock=0;spawned++;enemies.Add(0);}
   float travel=30f-(int)difficulty*5f-(Round-1)*3f;
   for(int i=enemies.Count-1;i>=0;i--){enemies[i]+=Time.deltaTime/travel;if(enemies[i]>=1){enemies.RemoveAt(i);Lives=Mathf.Max(0,Lives-1);feedback="La puerta ha recibido un golpe.";if(Lives==0){State=GameState.Derrota;feedback="La puerta ha caido.";break;}}}
  }
  void OnGUI() {
   var previous=GUI.matrix; GUI.matrix=Matrix4x4.TRS(Vector3.zero,Quaternion.identity,new Vector3(Screen.width/1100f,Screen.height/720f,1));
   GUI.Box(new Rect(0,0,1100,720),GUIContent.none);
   var title=new GUIStyle(GUI.skin.label){fontSize=34,fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleCenter};
   var text=new GUIStyle(GUI.skin.label){fontSize=20,wordWrap=true};
   GUI.Label(new Rect(30,20,1040,55),"RUNNERS Z  /  LA PUERTA CIFRADA",title);
   if(!started){GUI.Label(new Rect(170,130,760,100),"Descifra tres mensajes Cesar. A-Z rota; espacios y signos se conservan. Los errores acercan a los enemigos. Tienes tres vidas y las pistas cuestan 50 puntos.",text);
    if(GUI.Button(new Rect(170,260,240,70),"FACIL / 3 pistas"))Begin(Difficulty.Easy);
    if(GUI.Button(new Rect(430,260,240,70),"NORMAL / 2 pistas"))Begin(Difficulty.Normal);
    if(GUI.Button(new Rect(690,260,240,70),"DIFICIL / 1 pista"))Begin(Difficulty.Hard);
   }else{
    GUI.Label(new Rect(45,85,1000,40),$"Ronda {Round}/3     Vidas {Lives}/3     Puntos {Score}     {difficulty}",text);
    GUI.Label(new Rect(45,140,1000,70),"MENSAJE: "+Cipher,text);
    GUI.Label(new Rect(45,215,1000,35),config.ShowsShift?"Desplazamiento: "+shift:"Descubre el desplazamiento entre 1 y 25",text);
    GUI.enabled=State==GameState.EnCurso;
    GUI.SetNextControlName("answer"); answer=GUI.TextField(new Rect(45,265,750,50),answer,160);
    if(GUI.Button(new Rect(815,265,235,50),"VALIDAR"))Submit(answer);
    if(Event.current.type==EventType.KeyDown && Event.current.keyCode==KeyCode.Return){Submit(answer);Event.current.Use();}
    if(GUI.Button(new Rect(45,335,220,45),"PISTA ("+HintsRemaining+")"))UseHint();
    GUI.Label(new Rect(290,335,760,65),hint,text);GUI.enabled=true;
    GUI.Label(new Rect(45,410,1000,65),feedback,text);
    GUI.color=new Color(.25f,.55f,.3f);GUI.DrawTexture(new Rect(45,575,1000,12),Texture2D.whiteTexture);
    GUI.color=new Color(.4f,.65f,.9f);GUI.DrawTexture(new Rect(990,495,50,80),Texture2D.whiteTexture);
    foreach(float x in enemies){GUI.color=new Color(.9f,.25f,.25f);GUI.DrawTexture(new Rect(50+x*925,535+Mathf.Sin(x*80)*8,30,40),Texture2D.whiteTexture);}
    GUI.color=Color.white;
    if(State!=GameState.EnCurso)GUI.Label(new Rect(45,590,1000,45),State==GameState.Victoria?"VICTORIA":"DERROTA",title);
    if(GUI.Button(new Rect(45,650,220,45),"REINICIAR"))Begin(difficulty);
    if(GUI.Button(new Rect(290,650,220,45),"CAMBIAR DIFICULTAD")){started=false;enemies.Clear();}
   }
   GUI.matrix=previous;
  }
 }
}
