using UnityEngine;
namespace RunnersZ.Game { public sealed class Door : MonoBehaviour { [SerializeField] int lives=3; public int Lives=>lives; public System.Action<int> Hit; public void ResetLives()=>lives=3; void OnTriggerEnter2D(Collider2D other){if(!other.CompareTag("Enemy"))return;lives=Mathf.Max(0,lives-1);other.gameObject.SetActive(false);Hit?.Invoke(lives);}}}
