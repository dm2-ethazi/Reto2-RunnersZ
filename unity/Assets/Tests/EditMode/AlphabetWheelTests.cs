using NUnit.Framework;
using RunnersZ.Logic;
public class AlphabetWheelTests {
 int Hidden(AlphabetWheel w){int n=0;for(int i=0;i<26;i++)if(!w.IsColumnVisible(i))n++;return n;}
 [Test] public void DifficultyControlsMissingPairs(){Assert.AreEqual(0,Hidden(new AlphabetWheel(Difficulty.Easy,7)));Assert.AreEqual(8,Hidden(new AlphabetWheel(Difficulty.Normal,7)));Assert.AreEqual(14,Hidden(new AlphabetWheel(Difficulty.Hard,7)));}
 [Test] public void ShuffleContainsEveryLetterExactlyOnce(){var w=new AlphabetWheel(Difficulty.Easy,7);var seen=new System.Collections.Generic.HashSet<char>();string order="";for(int i=0;i<26;i++){seen.Add(w.ReferenceAt(i));order+=w.ReferenceAt(i);Assert.AreEqual(w.ReferenceAt(i),w.ShiftedAt(i));}Assert.AreEqual(26,seen.Count);Assert.AreNotEqual(AlphabetWheel.Alphabet,order);}
 [Test] public void EveryColumnAdvancesInAlphabetInsteadOfMovingColumns(){var w=new AlphabetWheel(Difficulty.Normal,7);var original=new char[26];for(int i=0;i<26;i++)original[i]=w.ReferenceAt(i);w.Move(1);for(int i=0;i<26;i++){Assert.AreEqual(original[i],w.ReferenceAt(i));Assert.AreEqual((char)('A'+(original[i]-'A'+1)%26),w.ShiftedAt(i));}w.Move(-1);for(int i=0;i<26;i++)Assert.AreEqual(original[i],w.ShiftedAt(i));}
 [Test] public void MasksCannotBeBypassedByMoving(){var w=new AlphabetWheel(Difficulty.Hard,7);var mask=new bool[26];for(int i=0;i<26;i++)mask[i]=w.IsColumnVisible(i);for(int turn=0;turn<26;turn++){w.Move(1);for(int i=0;i<26;i++)Assert.AreEqual(mask[i],w.IsColumnVisible(i));}Assert.AreEqual(0,w.Offset);}
 [Test] public void HintsRevealEachPairOnce(){var w=new AlphabetWheel(Difficulty.Hard,7);for(int i=0;i<14;i++){Assert.IsTrue(w.RevealHint().HasValue);Assert.AreEqual(13-i,Hidden(w));}Assert.IsFalse(w.RevealHint().HasValue);}
 [Test] public void MovementWrapsBothWays(){var w=new AlphabetWheel(Difficulty.Hard,7);w.Move(-1);Assert.AreEqual(25,w.Offset);w.Move(27);Assert.AreEqual(0,w.Offset);}
 [Test] public void HiddenPositionsVaryBetweenRounds(){
  var masks=new System.Collections.Generic.HashSet<string>();
  for(int seed=0;seed<12;seed++){
   var w=new AlphabetWheel(Difficulty.Normal,seed);string mask="";
   for(int i=0;i<26;i++)mask+=w.IsColumnVisible(i)?"1":"0";
   masks.Add(mask);
  }
  Assert.Greater(masks.Count,1);
 }
}
