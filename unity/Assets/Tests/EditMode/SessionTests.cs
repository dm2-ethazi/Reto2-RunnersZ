using NUnit.Framework;
using UnityEngine;
using RunnersZ.Game;
using RunnersZ.Logic;
public class SessionTests {
 GameObject owner; RunnerGame game;
 [SetUp] public void SetUp(){owner=new GameObject("Test game");game=owner.AddComponent<RunnerGame>();game.Begin(Difficulty.Easy);}
 [TearDown] public void TearDown(){Object.DestroyImmediate(owner);}
 [Test] public void ThreeMessagesWinAndFurtherAnswersDoNotScore(){for(int i=0;i<3;i++)Assert.IsTrue(game.Submit(game.Message));Assert.AreEqual(GameState.Victoria,game.State);Assert.AreEqual(300,game.Score);Assert.IsFalse(game.Submit(game.Message));Assert.AreEqual(300,game.Score);}
 [Test] public void WrongAnswerDoesNotCompleteRound(){Assert.IsFalse(game.Submit("wrong"));Assert.AreEqual(1,game.Round);Assert.AreEqual(0,game.Score);}
 [Test] public void HintsRespectDifficultyAndScoreFloor(){game.Begin(Difficulty.Hard);game.UseHint();Assert.AreEqual(0,game.HintsRemaining);Assert.AreEqual(0,game.Score);game.UseHint();Assert.AreEqual(0,game.HintsRemaining);}
 [Test] public void RestartRestoresSession(){game.Submit(game.Message);game.Begin(Difficulty.Normal);Assert.AreEqual(1,game.Round);Assert.AreEqual(3,game.Lives);Assert.AreEqual(0,game.Score);Assert.AreEqual(GameState.EnCurso,game.State);Assert.AreEqual(2,game.HintsRemaining);}
}
