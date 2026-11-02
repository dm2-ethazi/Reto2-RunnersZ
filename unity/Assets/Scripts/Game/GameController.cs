using UnityEngine;
using UnityEngine.UI;
using RunnersZ.Logic;

namespace RunnersZ.Game
{
    public sealed class GameController : MonoBehaviour
    {
        [SerializeField] Door door; [SerializeField] EnemyHopper enemy; [SerializeField] Text encryptedMessage; [SerializeField] Text stateLabel; [SerializeField] InputField answerInput; [SerializeField] Button validateButton; [SerializeField] Button restartButton; [SerializeField] string expectedMessage = "OPEN THE DOOR"; [SerializeField] int shift = 3;
        readonly GameSession session = new GameSession(); MessageRound round; bool subscribed;
        public GameState State => session.State; public int Score => session.Score;
        void Awake(){round=new MessageRound();if(validateButton!=null)validateButton.onClick.AddListener(ValidateAnswer);if(restartButton!=null)restartButton.onClick.AddListener(Restart);if(door!=null){door.Hit+=OnDoorHit;subscribed=true;}ResetRound();}
        void OnDestroy(){if(validateButton!=null)validateButton.onClick.RemoveListener(ValidateAnswer);if(restartButton!=null)restartButton.onClick.RemoveListener(Restart);if(subscribed&&door!=null)door.Hit-=OnDoorHit;}
        public void ValidateAnswer(){if(session.State!=GameState.EnCurso||answerInput==null)return;if(round.Submit(answerInput.text,expectedMessage)){session.Correct();SetState("VICTORIA");if(enemy!=null)enemy.enabled=false;}else SetState("Respuesta incorrecta");}
        public void OnDoorHit(int remainingLives){if(session.State!=GameState.EnCurso)return;if(remainingLives<=0){session.Lose();SetState("DERROTA");if(enemy!=null)enemy.enabled=false;}else SetState($"Vidas: {remainingLives}");}
        public void Restart(){session.Reset();if(door!=null)door.ResetLives();if(round==null)round=new MessageRound();if(answerInput!=null)answerInput.text=string.Empty;if(enemy!=null)enemy.enabled=true;ResetRound();}
        void ResetRound(){if(encryptedMessage!=null)encryptedMessage.text=Caesar.Transform(expectedMessage,shift);SetState("EN CURSO");}
        void SetState(string value){if(stateLabel!=null)stateLabel.text=value;}
    }
}
