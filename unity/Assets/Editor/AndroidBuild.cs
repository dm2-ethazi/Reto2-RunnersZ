#if UNITY_EDITOR
using System.IO;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEngine;
using UnityEngine.Rendering;

namespace RunnersZ.Editor {
 public static class AndroidBuild {
  const string Package="com.runnersz.game";
  const string Scene="Assets/Scenes/Game.unity";
  const string Apk="Builds/Android/RunnersZ.apk";

  [MenuItem("RunnersZ/Configure Android")]
  public static void Configure(){
   PlayerSettings.companyName="RunnersZ";
   PlayerSettings.productName="RunnersZ";
   PlayerSettings.SetApplicationIdentifier(NamedBuildTarget.Android,Package);
   // Juego de teclado y pantalla ancha: apaisado, sin reorientacion vertical.
   PlayerSettings.defaultInterfaceOrientation=UIOrientation.LandscapeLeft;
   PlayerSettings.allowedAutorotateToPortrait=false;
   PlayerSettings.allowedAutorotateToPortraitUpsideDown=false;
   PlayerSettings.allowedAutorotateToLandscapeLeft=true;
   PlayerSettings.allowedAutorotateToLandscapeRight=true;
   PlayerSettings.useAnimatedAutorotation=false;
   PlayerSettings.Android.minSdkVersion=AndroidSdkVersions.AndroidApiLevel25;
   PlayerSettings.Android.targetSdkVersion=AndroidSdkVersions.AndroidApiLevelAuto;
   PlayerSettings.Android.targetArchitectures=AndroidArchitecture.ARM64;
   PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android,ScriptingImplementation.IL2CPP);
   PlayerSettings.Android.minifyRelease=false;
   PlayerSettings.Android.minifyDebug=false;
   // OpenGL ES 3 evita los fallos de IMGUI con Vulkan en moviles modestos.
   PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android,false);
   PlayerSettings.SetGraphicsAPIs(BuildTarget.Android,new[]{GraphicsDeviceType.OpenGLES3});
   AssetDatabase.SaveAssets();
   Debug.Log("ANDROID_CONFIG_OK package="+Package+" orientation=Landscape ARM64/IL2CPP GLES3");
  }

  [MenuItem("RunnersZ/Build Android APK")]
  public static void Build(){
   Configure();
   if(!EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android,BuildTarget.Android))
    throw new System.Exception("No se pudo activar el destino Android");
   Directory.CreateDirectory(Path.GetDirectoryName(Apk));
   var options=new BuildPlayerOptions{
    scenes=new[]{Scene},
    locationPathName=Apk,
    target=BuildTarget.Android,
    targetGroup=BuildTargetGroup.Android,
    options=BuildOptions.None
   };
   var report=BuildPipeline.BuildPlayer(options);
   var summary=report.summary;
   Debug.Log("ANDROID_BUILD_RESULT:"+summary.result+" bytes="+summary.totalSize+" errors="+summary.totalErrors);
   if(summary.result!=BuildResult.Succeeded)throw new System.Exception("Build Android: "+summary.result);
  }

  // Punto de entrada para -executeMethod: sale con codigo de error explicito.
  public static void BuildBatch(){
   try{Build();}
   catch(System.Exception error){Debug.LogError(error);EditorApplication.Exit(1);return;}
   EditorApplication.Exit(0);
  }
 }
}
#endif
