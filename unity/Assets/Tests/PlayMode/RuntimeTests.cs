using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using UnityEngine.SceneManagement;
using RunnersZ.Game;
using RunnersZ.Logic;
public class RuntimeTests {
 [UnityTest] public IEnumerator SceneStartsAndCompletes(){
  SceneManager.LoadScene("Game");yield return null;
  var game=Object.FindFirstObjectByType<RunnerGame>();Assert.IsNotNull(game);
  game.Begin(Difficulty.Easy);yield return null;
  for(int i=0;i<3;i++)game.Submit(game.Message);
  yield return null;Assert.AreEqual(GameState.Victoria,game.State);
 }
 [UnityTest] public IEnumerator EnemiesCauseDefeatAndRestartRestoresLives(){
  var owner=new GameObject("Runtime test");var game=owner.AddComponent<RunnerGame>();game.Begin(Difficulty.Hard);
  float previous=Time.timeScale;Time.timeScale=50;
  try {float deadline=Time.realtimeSinceStartup+15;while(game.State==GameState.EnCurso && Time.realtimeSinceStartup<deadline)yield return null;
   Assert.AreEqual(GameState.Derrota,game.State);Assert.AreEqual(0,game.Lives);
   Assert.IsFalse(game.Submit(game.Message));game.Begin(Difficulty.Easy);Assert.AreEqual(3,game.Lives);Assert.AreEqual(GameState.EnCurso,game.State);
  }finally{Time.timeScale=previous;Object.Destroy(owner);}
 }
}
