using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using UnityEngine;

namespace RunnersZ.Game {
 // Original procedural pixel artwork; all textures use nearest-neighbour sampling.
 public sealed class PixelFantasyArt : IDisposable {
  public readonly Texture2D Background;
  public readonly Texture2D Arena;
  public readonly Texture2D Wraith;
  public readonly Texture2D Door;
  // Variante roja opcional (Resources/bleed.png) para el fogonazo del golpe.
  public readonly Texture2D Bleed;
  readonly Dictionary<char,Texture2D> glyphs=new Dictionary<char,Texture2D>();
  const int W=400,H=267;
  readonly Color32[] pixels=new Color32[W*H];
  static Color32 C(byte r,byte g,byte b){return new Color32(r,g,b,255);}
  void Box(int x,int y,int w,int h,Color32 c){
   for(int j=Math.Max(0,y);j<Math.Min(H,y+h);j++)for(int i=Math.Max(0,x);i<Math.Min(W,x+w);i++)pixels[(H-1-j)*W+i]=c;
  }
  void Tower(int x,int y,int w,int height){
   Box(x,y,w,height,C(17,25,37));Box(x,y,2,height,C(29,40,53));Box(x+w-2,y,2,height,C(6,11,19));
   for(int row=y+8;row<y+height;row+=9){
    Box(x+2,row,w-4,1,C(10,16,25));
    for(int col=x+3+((row/9)%2)*7;col<x+w-2;col+=14)Box(col,row+1,1,8,C(11,18,27));
   }
   Box(x-2,y-3,w+4,3,C(36,44,52));
   for(int i=0;i<w;i+=8)Box(x+i,y-7,4,5,C(26,36,47));
   for(int row=y+15;row<y+height-10;row+=30){
    Box(x+w/2-3,row,6,12,C(4,9,17));Box(x+w/2-1,row-2,2,2,C(4,9,17));
    Box(x+w/2,row+3,1,5,C(116,88,43));
   }
  }
  public PixelFantasyArt(){
   var rng=new System.Random(991);
   for(int y=0;y<H;y++)for(int x=0;x<W;x++){
    int noise=rng.Next(0,3);byte d=(byte)(y<170?0:2);
    pixels[(H-1-y)*W+x]=C((byte)(8+noise+d),(byte)(13+noise+d),(byte)(22+noise+d));
   }
   // Cold moon behind distant spires, in a restrained version of the game palette.
   for(int y=20;y<57;y++)for(int x=330;x<367;x++){
    int dx=x-348,dy=y-38;
    if(dx*dx+dy*dy<18*18)Box(x,y,1,1,C(39,49,61));
   }
   for(int i=0;i<55;i++)Box(rng.Next(15,386),rng.Next(5,75),1,1,C(45,54,61));
   // Distant castle walls and individual crenellations.
   for(int x=140;x<400;x+=25){
    int y=230+rng.Next(-12,6);
    Box(x,y,21,H-y,C(5,9,16));
    for(int j=0;j<21;j+=7)Box(x+j,y-4,3,5,C(5,9,16));
    Box(x+8,y+7,3,5,C(23,28,31));
   }
   Tower(0,55,15,212);Tower(385,55,15,212);
   for(int y=72;y<267;y+=36){
    Box(0,y,17,3,C(41,43,42));Box(383,y,17,3,C(41,43,42));
    Box(2,y+3,11,1,C(9,15,23));Box(387,y+3,11,1,C(9,15,23));
   }
   // Stepped gothic finials.
   for(int i=0;i<8;i++){Box(7-i,45+i,1,1,C(61,61,54));Box(7+i,45+i,1,1,C(61,61,54));
    Box(392-i,45+i,1,1,C(61,61,54));Box(392+i,45+i,1,1,C(61,61,54));}
   // Iron brackets and pixel flames use the existing aged-gold palette.
   foreach(int x in new[]{8,391}){
    Box(x-1,130,3,15,C(35,32,30));Box(x-3,129,7,2,C(72,57,39));
    Box(x-2,122,5,7,C(110,76,38));Box(x-1,119,3,8,C(169,126,60));Box(x,122,1,5,C(220,184,105));
    for(int r=1;r<4;r++)Box(x-r,139+r*5,r*2+1,1,C(51,45,35));
   }
   Background=Texture(W,H,pixels);
   Arena=LoadAsset("background.png",Background);
   string[] sprite={
    "       oo       ","      oooo      ","     oddddo     ","     odggdo     ",
    "     oddddo     ","    osssssso    ","    osassaso    ","   osassssaso   ",
    "   ossssssssso  ","  ossassssasso  ","  osssssssssso  "," ossassssssasso ",
    " osssssssssssso ","ossassssssssasso"," osssossssosss  ","  oo  ooo  oo   "
   };
   var colors=new Dictionary<char,Color32>{{'o',C(11,16,24)},{'d',C(28,32,39)},{'g',C(202,160,80)},{'s',C(67,48,50)},{'a',C(94,61,57)}};
   var sp=new Color32[16*16];
   for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(x<sprite[y].Length&&colors.ContainsKey(sprite[y][x]))sp[(15-y)*16+x]=colors[sprite[y][x]];
   Wraith=LoadAsset("enemy.png",Texture(16,16,sp));
   Door=LoadAsset("door.png",Texture(16,16,sp));
   Bleed=LoadAsset("bleed.png",null);
   string[] rows={
    "A:0E11111F111111","B:1E11111E11111E","C:0E11101010110E","D:1E11111111111E",
    "E:1F10101E10101F","F:1F10101E101010","G:0E11101711110F","H:1111111F111111",
    "I:0E04040404040E","J:0702020212120C","K:11121418141211","L:1010101010101F",
    "M:111B1515111111","N:11191513111111","O:0E11111111110E","P:1E11111E101010",
    "Q:0E11111115120D","R:1E11111E141211","S:0F10100E01011E","T:1F040404040404",
    "U:1111111111110E","V:11111111110A04","W:11111115151B11","X:11110A040A1111",
    "Y:11110A04040404","Z:1F01020408101F","0:0E11131519110E","1:040C040404040E",
    "2:0E11010204081F","3:1E01010601011E","4:02060A121F0202","5:1F10101E01011E",
    "6:0E10101E11110E","7:1F010204080808","8:0E11110E11110E","9:0E11110F01010E",
    "?:0E110102040004","!:04040404040004","+:0004041F040400","-:0000001F000000",
    ".:00000000000004","/:01010204081010",">:10080402040810","<:01020408040201",
    "::00040000040000"
   };
   foreach(string row in rows){
    var gp=new Color32[35];
    for(int y=0;y<7;y++){int bits=int.Parse(row.Substring(2+y*2,2),NumberStyles.HexNumber);for(int x=0;x<5;x++)if((bits&(1<<(4-x)))!=0)gp[(6-y)*5+x]=new Color32(255,255,255,255);}
    glyphs[row[0]]=Texture(5,7,gp);
   }
  }
  static Texture2D LoadAsset(string name,Texture2D fallback){
   string resourceName=System.IO.Path.GetFileNameWithoutExtension(name);
   var loaded=Resources.Load<Texture2D>(resourceName);
   if(loaded==null) return fallback;
   loaded.filterMode=FilterMode.Point; loaded.wrapMode=TextureWrapMode.Clamp; return loaded;
  }
  static Texture2D Texture(int w,int h,Color32[] data){
   var t=new Texture2D(w,h,TextureFormat.RGBA32,false){filterMode=FilterMode.Point,wrapMode=TextureWrapMode.Clamp};
   t.SetPixels32(data);t.Apply();return t;
  }
  public void Text(Rect rect,string text,int fontSize,TextAnchor alignment,Color color){
   if(Event.current.type!=EventType.Repaint||string.IsNullOrEmpty(text))return;
   var clean=new StringBuilder();
   foreach(char c in text.ToUpperInvariant().Normalize(NormalizationForm.FormD))
    if(CharUnicodeInfo.GetUnicodeCategory(c)!=UnicodeCategory.NonSpacingMark)clean.Append(c);
   int scale=Math.Max(2,fontSize/8),advance=6*scale,lineHeight=9*scale;
   int capacity=Math.Max(1,(int)rect.width/advance);
   var lines=new List<string>();
   foreach(string paragraph in clean.ToString().Split('\n')){
    string remaining=paragraph;
    while(remaining.Length>capacity){
     int cut=remaining.LastIndexOf(' ',capacity-1,capacity);if(cut<=0)cut=capacity;
     lines.Add(remaining.Substring(0,cut));remaining=remaining.Substring(cut).TrimStart();
    }
    lines.Add(remaining);
   }
   float y=rect.y+(alignment==TextAnchor.MiddleCenter?(rect.height-lines.Count*lineHeight+2*scale)/2:3);
   var old=GUI.color;GUI.color=color;
   foreach(string line in lines){
    float x=rect.x+(alignment==TextAnchor.MiddleCenter?(rect.width-(line.Length*advance-scale))/2:0);
    foreach(char c in line){
     Texture2D glyph;
     if(c!=' '&&glyphs.TryGetValue(c,out glyph)&&y+7*scale<=rect.yMax)GUI.DrawTexture(new Rect(x,y,5*scale,7*scale),glyph,ScaleMode.StretchToFill);
     x+=advance;
    }
    y+=lineHeight;
   }
   GUI.color=old;
  }
  public void Dispose(){UnityEngine.Object.Destroy(Background);UnityEngine.Object.Destroy(Arena);UnityEngine.Object.Destroy(Wraith);UnityEngine.Object.Destroy(Door);if(Bleed!=null)UnityEngine.Object.Destroy(Bleed);foreach(var t in glyphs.Values)UnityEngine.Object.Destroy(t);}
 }
}
