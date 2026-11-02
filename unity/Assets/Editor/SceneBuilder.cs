#if UNITY_EDITOR
using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
namespace RunnersZ.Editor {
 public static class SceneBuilder {
  [MenuItem("RunnersZ/Build Game Scene")]
  public static void Build(){
   Directory.CreateDirectory("Assets/Scenes");
   var scene=EditorSceneManager.NewScene(NewSceneSetup.EmptyScene);
   var camera=new GameObject("Camera").AddComponent<Camera>();camera.orthographic=true;camera.backgroundColor=new Color(.05f,.08f,.14f);camera.clearFlags=CameraClearFlags.SolidColor;camera.transform.position=new Vector3(0,0,-10);camera.tag="MainCamera";
   new GameObject("RunnersZ").AddComponent<RunnersZ.Game.RunnerGame>();
   EditorSceneManager.SaveScene(scene,"Assets/Scenes/Game.unity");
   EditorBuildSettings.scenes=new[]{new EditorBuildSettingsScene("Assets/Scenes/Game.unity",true)};
   PlayerSettings.productName="RunnersZ";PlayerSettings.companyName="RunnersZ";
   AssetDatabase.SaveAssets();
  }
  public static void BuildWindows(){Build();Directory.CreateDirectory("Builds/Windows");var result=BuildPipeline.BuildPlayer(EditorBuildSettings.scenes,"Builds/Windows/RunnersZ.exe",BuildTarget.StandaloneWindows64,BuildOptions.None);if(result.summary.result!=UnityEditor.Build.Reporting.BuildResult.Succeeded)throw new System.Exception("Build failed: "+result.summary.result);}
 }
}
#endif
