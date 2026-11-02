#if UNITY_INCLUDE_TESTS
using NUnit.Framework;using RunnersZ.Logic;
public class LogicTests { [Test] public void CaesarRoundtrip(){var s="Attack Ñ 2";Assert.AreEqual(s,Caesar.Transform(Caesar.Transform(s,3),3,true));} [Test] public void Normalizes(){Assert.IsTrue(AnswerValidator.IsCorrect("  átaque,   al amanecer! ","ATAQUE AL AMANECER"));} [Test] public void Score(){var r=new MessageRound();Assert.IsTrue(r.Submit("OPEN THE DOOR","OPEN THE DOOR"));Assert.AreEqual(100,r.Score);} }
#endif