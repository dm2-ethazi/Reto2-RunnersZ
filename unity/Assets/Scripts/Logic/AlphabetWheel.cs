using System;
using System.Collections.Generic;
using System.Text;
namespace RunnersZ.Logic {
 public sealed class AlphabetWheel {
  public const string Alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  readonly char[] order=Alphabet.ToCharArray();
  readonly HashSet<char> hidden=new HashSet<char>();
  readonly HashSet<char> revealed=new HashSet<char>();
  public int Offset {get;private set;}
  public int HintsUsed {get;private set;}
  public int HiddenCount {get{return hidden.Count-revealed.Count;}}
  public AlphabetWheel(Difficulty difficulty):this(difficulty,Guid.NewGuid().GetHashCode()){}
  public AlphabetWheel(Difficulty difficulty,int seed) {
   var random=new Random(seed);
   Shuffle(order,random);
   // Independent shuffle: both the letters and their screen positions vary per round.
   var missing=Alphabet.ToCharArray();Shuffle(missing,random);
   int count=difficulty==Difficulty.Easy?0:difficulty==Difficulty.Normal?8:14;
   for(int i=0;i<count;i++)hidden.Add(missing[i]);
  }
  static void Shuffle(char[] data,Random random){
   for(int i=data.Length-1;i>0;i--){int j=random.Next(i+1);char t=data[i];data[i]=data[j];data[j]=t;}
  }
  public char ReferenceAt(int column){return order[column];}
  public char ShiftedAt(int column){return (char)('A'+(order[column]-'A'+Offset)%26);}
  public bool IsColumnVisible(int column){return IsVisible(order[column]);}
  public void Move(int amount){Offset=(int)(((long)Offset+amount)%26);if(Offset<0)Offset+=26;}
  public bool IsVisible(char letter){letter=char.ToUpperInvariant(letter);return !hidden.Contains(letter)||revealed.Contains(letter);}
  public char? RevealHint(){foreach(char c in order)if(hidden.Contains(c)&&revealed.Add(c)){HintsUsed++;return c;}return null;}
  public string MaskCipher(string cipher,int actualShift){
   if(cipher==null)return "";
   int normalized=(actualShift%26+26)%26;
   var result=new StringBuilder(cipher.Length);
   foreach(char c in cipher){
    char upper=char.ToUpperInvariant(c);
    if(upper>='A'&&upper<='Z'){
     char original=(char)('A'+(upper-'A'-normalized+26)%26);
     result.Append(IsVisible(original)?c:'?');
    }else result.Append(c);
   }
   return result.ToString();
  }
 }
}
