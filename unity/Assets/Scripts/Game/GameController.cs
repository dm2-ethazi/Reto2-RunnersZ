using UnityEngine;
using RunnersZ.Logic;
namespace RunnersZ.Game {
 public sealed class GameController : MonoBehaviour {
  [SerializeField] Door door; [SerializeField] EnemyHopper enemy;
  [SerializeField] GameObject encryptedMessage; [SerializeField] GameObject stateLabel; [SerializeField] GameObject answerInput;
  [SerializeField] string expectedMessage="OPEN THE DOOR"; [SerializeField] int shift=3;
  readonly GameSession session=new GameSession(); MessageRound round; bool subscribed;
  public GameState State=>session.State;
  void Awake(){round=new MessageRound();if(door!=null){door.Hit+=OnDoorHit;subscribed=true;}ResetRound();}
  void OnDestroy(){if(subscribed&&door!=null)door.Hit-=OnDoorHit;}
  public void ValidateAnswer(){if(session.State!=GameState.EnCurso)return;var answer=answerInput==null?"":(answerInput.GetComponent<UnityEngine.UI.InputField>()?.text ?? "");if(round.Submit(answer,expectedMessage)){session.Correct();SetState("VICTORIA");if(enemy!=null)enemy.enabled=false;}else SetState("Respuesta incorrecta");}
  public void OnDoorHit(int lives){if(session.State!=GameState.EnCurso)return;if(lives<=0){session.Lose();SetState("DERROTA");if(enemy!=null)enemy.enabled=false;}else SetState("Vidas: "+lives);}
  public void Restart(){session.Reset();if(door!=null)door.ResetLives();round=new MessageRound();if(enemy!=null)enemy.enabled=true;ResetRound();}
  void ResetRound(){SetState(encryptedMessage,Caesar.Transform(expectedMessage,shift));SetState(stateLabel,"EN CURSO");}
  void SetState(string value){SetState(stateLabel,value);}
  static void SetState(GameObject target,string value){if(target==null)return;target.SendMessage("SetText",value,SendMessageOptions.DontRequireReceiver);target.name=value;}
 }
}