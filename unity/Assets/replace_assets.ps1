\='D:\IA\Reto2-RunnersZ\unity\Assets\Scripts\Game\PixelFantasyArt.cs'
\=Get-Content -Raw \
\=\.Replace('public readonly Texture2D Wraith;','public readonly Texture2D Wraith;\n  public readonly Texture2D Door;')
\=\.Replace('using System.Globalization;','using System.Globalization;\nusing System.IO;')
\=\.Replace('Background=Texture(W,H,pixels);','Background=LoadAsset("background.png",Texture(W,H,pixels));')
\=\.Replace('Wraith=Texture(16,16,sp);','Wraith=LoadAsset("enemy.png",Texture(16,16,sp));\n   Door=LoadAsset("door.png",Texture(16,16,sp));')
\='static Texture2D Texture(int w,int h,Color32[] data){'
\='static Texture2D LoadAsset(string name,Texture2D fallback){\n   string path=Path.Combine(Application.dataPath,"Art",name);\n   if(!File.Exists(path)) return fallback;\n   var bytes=File.ReadAllBytes(path); var t=new Texture2D(2,2,TextureFormat.RGBA32,false);\n   if(!t.LoadImage(bytes,true)){UnityEngine.Object.Destroy(t); return fallback;}\n   t.filterMode=FilterMode.Point; t.wrapMode=TextureWrapMode.Clamp; return t;\n  }\n  static Texture2D Texture(int w,int h,Color32[] data){'
\=\.Replace(\,\)
\=\.Replace('UnityEngine.Object.Destroy(Background);UnityEngine.Object.Destroy(Wraith);','UnityEngine.Object.Destroy(Background);UnityEngine.Object.Destroy(Wraith);UnityEngine.Object.Destroy(Door);')
Set-Content -Path \ -Value \ -NoNewline
\='D:\IA\Reto2-RunnersZ\unity\Assets\Scripts\Game\RunnerGame.cs'
\=Get-Content -Raw \
\=\.IndexOf('  void DrawGate(')
\=\.IndexOf('  void DrawWraith(', \)
\='  void DrawGate(float x,float y,float w,float h){\n   GUI.DrawTexture(new Rect(x-22,y-22,w+44,h+42),pixelArt.Door,ScaleMode.ScaleToFit,true);\n  }\n'
\=\.Substring(0,\)+\+\.Substring(\)
\=\.IndexOf('  void DrawWraith(')
\=\.IndexOf('  void OnDestroy()', \)
\='  void DrawWraith(float x,float y){\n   int frame=(int)(Time.time*8f)%8;\n   Rect uv=new Rect(frame/8f,0,1f/8f,1f);\n   GUI.DrawTextureWithTexCoords(new Rect(Mathf.Round(x/3)*3,y-18,92,92),pixelArt.Wraith,uv,true);\n  }\n'
\=\.Substring(0,\)+\+\.Substring(\)
Set-Content -Path \ -Value \ -NoNewline