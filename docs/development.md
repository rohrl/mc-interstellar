# Windows development

## Requirements

Complete Java 21 JDK; Git; IntelliJ IDEA; Minecraft Java account for normal gameplay. Gradle/Fabric dependencies are managed by the wrapper. Current Minecraft target is 1.21.1.

On the original machine, JDK is C:\Portable\jdks\temurin-21.0.12.1. Select it for IntelliJ Project SDK and Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM. The repo does not store a machine-specific JDK location.

For a PowerShell terminal:

```powershell
$env:JAVA_HOME = 'C:\Portable\jdks\temurin-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --version
.\gradlew.bat build
.\gradlew.bat runClient
```

Build artifacts go to build/libs. runClient creates a separate run/ game directory; it does not edit the normal launcher profile or existing worlds. On first startup Minecraft assets and dependencies need network access. Use a disposable creative world for calibration.

If the primary Fabric Maven service times out, this project supports its official secondary service:

```powershell
.\gradlew.bat build '-Pfabric_maven_url=https://maven2.fabricmc.net/'
```

Pass the same property to runClient. To use it from IntelliJ on an affected machine, set `fabric_maven_url=https://maven2.fabricmc.net/` in your user Gradle properties (outside Git). The secondary service is listed by Fabric's official installer in `src/main/java/net/fabricmc/installer/util/Reference.java`.

## Bootstrap controls

- F6: show/hide diagnostics (session toggle).
- F7: set virtual reference centre 64 coordinate blocks ahead of the camera (session only).
- Config: run/config/interstellar.json. Restart the client to reload edited values.
- HUD respects Minecraft's hidden-HUD setting (F1).

No optical effect is present in iteration 0. Config changes control measurements only; entering the reference radius has no gameplay effect.

## Verification checklist

- Build/test passes on JDK 21.
- Launch with runClient; verify main menu and no classloading exceptions.
- Join a disposable world, F6 toggle, F7 reference placement, movement changes r/r_s, F1 hides HUD.
- Return to title/rejoin and verify reference is cleared.
- Check invalid config produces a readable error and safe defaults without overwriting the invalid file.
- No FPS claims until the real optical renderer and agreed quality settings are running.

## Mobile monitoring

Official docs: https://learn.chatgpt.com/docs/remote-connections . Pair the desktop host with the ChatGPT mobile app through Settings > Connections > Control this PC. Account/workspace and rollout must support it. Keep host awake and app running; enable notification permissions on the phone. Agent can flag attention in the task but cannot confirm push delivery. No external notification service has been configured.
