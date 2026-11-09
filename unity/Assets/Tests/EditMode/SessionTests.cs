using NUnit.Framework;
using UnityEngine;
using RunnersZ.Game;
using RunnersZ.Logic;
public class SessionTests {
 GameObject owner; RunnerGame game;
 [SetUp] public void SetUp(){owner=new GameObject("Test game");game=owner.AddComponent<RunnerGame>();game.Begin(Difficulty.Easy);}
 [TearDown] public void TearDown(){Object.DestroyImmediate(owner);}
 [Test] public void ThreeMessagesWinAndFurtherAnswersAreIgnored(){for(int i=0;i<3;i++)Assert.IsTrue(game.Submit(game.Message));Assert.AreEqual(GameState.Victoria,game.State);Assert.AreEqual(3,game.Round);Assert.IsFalse(game.Submit(game.Message));}
 [Test] public void WrongAnswerDoesNotCompleteRound(){Assert.IsFalse(game.Submit("wrong"));Assert.AreEqual(1,game.Round);}
 [Test] public void HintsPerRoundFollowDifficulty(){Assert.AreEqual(3,game.HintsRemaining);game.UseHint();game.UseHint();game.UseHint();Assert.AreEqual(0,game.HintsRemaining);Assert.IsTrue(game.Submit(game.Message));Assert.AreEqual(3,game.HintsRemaining,"cada ronda vuelve a dar sus pistas");game.Begin(Difficulty.Normal);Assert.AreEqual(2,game.HintsRemaining);game.Begin(Difficulty.Hard);Assert.AreEqual(1,game.HintsRemaining);}
 [Test] public void CipherIsShownWholeInsteadOfQuestionMarks(){foreach(var d in new[]{Difficulty.Easy,Difficulty.Normal,Difficulty.Hard}){game.Begin(d);StringAssert.DoesNotContain("?",game.Cipher);Assert.AreEqual(game.Message.Length,game.Cipher.Length,d+": el cifrado no debe recortarse");}}
 [Test] public void RestartRestoresSession(){game.Submit(game.Message);game.Begin(Difficulty.Normal);Assert.AreEqual(1,game.Round);Assert.AreEqual(3,game.Lives);Assert.AreEqual(GameState.EnCurso,game.State);Assert.AreEqual(2,game.HintsRemaining);}
}
