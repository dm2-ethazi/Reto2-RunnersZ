using UnityEngine;

namespace RunnersZ.Game {
 // Audio del nivel: el ambiente en bucle, las pisadas de los espectros mientras
 // caminan y el golpe seco cada vez que uno entra en la puerta. Los clips viven
 // en Resources para que viajen dentro del APK sin tocar la escena.
 public sealed class GameAudio : MonoBehaviour {
  AudioSource ambient, steps, hit;
  float ambientTarget, stepTarget;
  const float AmbientLevel=.30f, StepLevel=.42f, HitLevel=.9f;

  void Awake(){
   // La escena no trae AudioListener y sin uno la mezcla no sale por el altavoz:
   // el reproductor se abre pero todo suena a silencio.
   if(FindAnyObjectByType<AudioListener>()==null){
    gameObject.AddComponent<AudioListener>();
    Debug.Log("[audio] AudioListener creado");
   }
   ambient=Source("ambient",true,AmbientLevel);
   steps=Source("footsteps",true,0f);
   hit=Source("door_hit",false,1f);
  }

  AudioSource Source(string clipName,bool loop,float volume){
   var clip=Resources.Load<AudioClip>(clipName);
   if(clip==null){Debug.LogWarning("[audio] falta el clip "+clipName);return null;}
   var source=gameObject.AddComponent<AudioSource>();
   source.clip=clip; source.loop=loop; source.playOnAwake=false; source.volume=volume;
   // Sin mezcla 3D: el juego es una sola pantalla y los espectros comparten carril.
   source.spatialBlend=0f;
   return source;
  }

  // Entrar al nivel arranca el ambiente; salir lo deja apagarse solo.
  public void EnterLevel(){ambientTarget=AmbientLevel;}
  public void LeaveLevel(){ambientTarget=0f;stepTarget=0f;}

  // Pisadas en bucle mientras quede algun espectro caminando.
  public void Marching(bool walking){stepTarget=walking?StepLevel:0f;}

  public void DoorHit(){if(hit!=null)hit.PlayOneShot(hit.clip,HitLevel);}

  void Update(){
   // Subidas y bajadas cortas en vez de cortes secos: al entrar suena desde cero
   // y al quedarse el carril vacio se apaga sin chasquido.
   if(ambient!=null)ambient.volume=Fade(ambient,ambient.volume,ambientTarget,.9f);
   if(steps!=null)steps.volume=Fade(steps,steps.volume,stepTarget,1.6f);
  }

  float Fade(AudioSource source,float current,float target,float speed){
   float volume=Mathf.MoveTowards(current,target,Time.unscaledDeltaTime*speed);
   if(volume>0f&&!source.isPlaying)source.Play();
   if(volume<=0f&&source.isPlaying)source.Stop();
   return volume;
  }
 }
}
