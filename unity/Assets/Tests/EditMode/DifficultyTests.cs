#if UNITY_INCLUDE_TESTS
using NUnit.Framework;using RunnersZ.Logic;
public class DifficultyTests{[Test]public void ConfigMatchesContract(){Assert.AreEqual(12,new DifficultyConfig(Difficulty.Hard).Enemies);Assert.AreEqual(5f,new DifficultyConfig(Difficulty.Hard).Interval);}[Test]public void RoundsStopAtThree(){var r=new RoundProgression();Assert.IsTrue(r.Next());Assert.IsTrue(r.Next());Assert.IsFalse(r.Next());}[Test]public void HintHasFloor(){var h=new HintScore();h.Use("A");h.Use("B");Assert.AreEqual(0,h.Score);}}
#endif