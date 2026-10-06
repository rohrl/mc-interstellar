# Development setup

For ordinary player installation, start with the [README](../README.md).
This guide describes the source checkout. Reviewed 6 October 2026.

## Requirements and commands

Use a complete **JDK 21** and Git. An IDE is optional. The Gradle wrapper supplies
Gradle, and Loom manages the pinned Minecraft 1.21.1/Fabric dependencies.
In IntelliJ, select JDK 21 as both Project SDK and Gradle JVM.

From the directory containing `build.gradle` and `gradlew.bat`:

```powershell
# Point this at your installed JDK, not a JRE or somebody else's machine path.
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --version
.\gradlew.bat clean build
.\gradlew.bat runClient
```

The ordinary remapped mod is `build/libs/interstellar-0.1.0-dev.jar`; the source
jar is not an installable mod. `runClient` uses the separate `run/` game directory
and stable development name `InterstellarDev`. Logs are in `run/logs/latest.log`,
screenshots in `run/screenshots`, and worlds in `run/saves`. Do not run two clients
against the same save. First launch downloads dependencies and Minecraft assets.

The Windows `.cmd` launchers call the same Gradle tasks. They accept `JAVA_HOME`
and contain an optional fallback for the original developer's portable JDK.
On other systems use `./gradlew` for the ordinary build; runtime compatibility
outside the tested Windows machine has not been broadly verified.

## Optional Windows RTX backend

```powershell
.\gradlew.bat runClient -PinterstellarRtx
# Build/check without starting Minecraft:
.\gradlew.bat build checkRtxShaders -PinterstellarRtx
```

The property adds optional backend sources, LWJGL Vulkan/shaderc and Windows
shaderc natives. `runClient` also sets `-Dinterstellar.rtx=true` and the native
stack size. It requires supported Vulkan ray queries and Windows OpenGL/Vulkan
external-memory/semaphore sharing. The driver supplies Vulkan support.

**A jar produced with the property is not a standalone RTX distribution:**
Gradle's runtime dependencies and JVM configuration are not embedded by the
current packaging task. Use the development launch until dedicated RTX packaging
exists. Ordinary builds exclude these optional sources/dependencies. Build cleanly
without RTX properties when producing the normal installable artifact; both modes
use the same output filename, so copy artifacts aside if retaining both.

`checkRtxShaders` compiles 16 compute variants without a Vulkan device or Minecraft
launch. It does not verify runtime rendering. F4 → Graphics or Alt+F12 changes the
backend preference; the status overlay identifies the actual renderer/fallback.

## Demo packaging

```powershell
.\gradlew.bat clean build packageDemo
```

The ZIP under `build/distributions` contains the ordinary mod and a curated set
of documents, not Minecraft, Java, Fabric Loader or Fabric API. The repository
has the complete documentation/evidence; the ZIP is not a complete mirror.
Do not add RTX properties when creating this ordinary distribution.

## Verification

- Java/resource changes: build and appropriate JVM tests.
- Optical changes: relevant numerical checks, runtime shader compilation and
  representative images; compare timings at fixed settings when performance matters.
- Documentation-only changes: check commands/versions against code and validate
  local links. Do not launch Minecraft merely to validate prose.

Past successful runs do not validate later edits. Current outstanding runtime
checks are recorded in [handoff](../handoff.md). The owner's no-launch instruction
remains in force until superseded.

F8/F9 provide the optical/frozen diagnostic laboratories. F6 toggles diagnostics;
F7 places a virtual reference point only. These are separate from F4 gameplay
settings. See [the documentation index](README.md) for dated numerical studies.

## Slow startup and dependency downloads

Separate Gradle compilation/downloads, driver shader compilation and world-view
preparation when diagnosing startup. Cold shaders have taken several minutes on
the tested driver; local terrain preparation recurs when its cache is recreated.
The latest logs reveal which stage is active.

If the primary Fabric Maven endpoint is unavailable, the project also accepts:

```powershell
.\gradlew.bat build '-Pfabric_maven_url=https://maven2.fabricmc.net/'
```

Use the same property for `runClient` if needed. Keep machine-specific paths,
credentials, personal settings and worlds out of commits.
