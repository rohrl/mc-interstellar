# Install the standalone RTX edition

The RTX edition is an optional **Windows x64** Fabric mod package. It includes
the OpenGL fallback. Install one Interstellar edition at a time.

## Player installation

1. Set up **Minecraft Java 1.21.1**, **Fabric Loader 0.16.14 or newer**, **Java 21**
   and **Fabric API for 1.21.1** (tested with 0.102.1+1.21.1).
2. Put **`interstellar-rtx-0.1.0-dev.jar`** in that instance's `mods` directory,
   alongside Fabric API. Remove the ordinary Interstellar jar if present.
   If using the demo ZIP, copy its `mods` contents, not the ZIP itself.
3. Launch the normal Fabric profile. No Gradle, JDK, Vulkan SDK, separate binding
   downloads or custom JVM arguments are needed for an end user.
4. Open **F4 → Graphics**. RTX is preferred by default; an existing saved OpenGL
   preference is respected. Use Renderer or **Alt+F12** to change it. The status
   overlay reports the actual active backend, including fallback.

The GPU driver must support Vulkan ray queries and Windows Vulkan/OpenGL external
memory/semaphore sharing. Development testing used an RTX 5070 Ti. A GPU's name
alone does not establish compatibility; the renderer checks the required features
and falls back to OpenGL on supported failure paths. This does not add ray-query
capability to an unsupported GPU. Linux, macOS and Windows ARM are not RTX targets
for this build; automatic backend eligibility remains off there.

The same Fabric mod ID is used by both editions. Do **not** install both together.
The ordinary `interstellar-0.1.0-dev.jar` remains free of Vulkan/shaderc bindings.
Worlds and settings are shared across editions; switching does not require a new
world. Back up saves before updating any experimental mod.

## Building the artifacts

From a checkout with JDK 21:

```powershell
# Normal jar and demo ZIP:
.\gradlew.bat clean build packageDemo

# RTX jar and demo ZIP (Windows x64):
.\gradlew.bat build packageDemo -PinterstellarRtx

# Headless check using the remapped jar and its bundled libraries:
.\gradlew.bat verifyRtxDistribution -PinterstellarRtx
```

Jars are in `build/libs`; ZIPs are in `build/distributions`. RTX filenames include
`-rtx` so the normal artifact is not overwritten. `clean` removes previous build
outputs; copy a distribution elsewhere before cleaning if you want to retain it.
The package does not include Minecraft, Fabric API, Fabric Loader or a GPU driver.

## Packaging design and verification limits

Fabric's jar-in-jar mechanism loads LWJGL 3.3.3 Vulkan, shaderc and Windows x64
shaderc natives from the mod. Minecraft already supplies LWJGL core; it is not
duplicated. Upstream license/notice texts are included in the outer RTX jar under
`META-INF/licenses/interstellar-rtx/` (the Maven jars do not contain them).
A bundled marker enables the optional backend without loading Vulkan in the
ordinary edition. `-Dinterstellar.rtx=false` remains an optional diagnostic opt-out.

Large shader source strings use explicitly freed native buffers instead of the
thread-local LWJGL stack. No stack-size flag is required. The distribution check
loads shaderc from the extracted nested jars and compiles all 16 shader variants
from packaged shader resources with the normal 64 KiB stack; no Minecraft or
graphics window is started. This checks packaging/native compilation, not a full
Fabric launcher session or GPU rendering. See the handoff for performed checks.

For gameplay and troubleshooting, see the [repository README](https://github.com/rohrl/mc-interstellar/blob/main/README.md).
Packaging references: [Fabric Loom jar-in-jar](https://docs.fabricmc.net/develop/loom/)
and [LWJGL stack configuration](https://javadoc.lwjgl.org/org/lwjgl/system/Configuration.html).
