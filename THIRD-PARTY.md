# Third-party material

Gradle wrapper files are from the official Gradle v8.10.2 source (Apache License 2.0); their script headers are preserved. See gradle/wrapper/LICENSE. The official Fabric example repository (CC0) was consulted for build structure; Interstellar's application code and documents are new work.

Minecraft and Fabric are external build/runtime dependencies with their own terms. No Minecraft game binaries or extracted game assets are committed. The illustrated guide includes screenshots of the running development mod for documentation. No scientific reference implementation or external sky imagery has been copied into this repository. A license for Interstellar itself remains unselected.

The standalone RTX probe downloads official LWJGL 3.3.3 and shaderc binding/native
distributions from Maven Central, with pinned SHA-256 checksums, into ignored
`run/rtx/lib`. See [the probe README](tools/rtx-probe/README.md).

The optional installable RTX edition now embeds the official LWJGL 3.3.3 Vulkan,
shaderc and Windows x64 shaderc-native jars using Fabric jar-in-jar. Minecraft's
existing LWJGL core is not duplicated. The ordinary OpenGL edition embeds none
of these optional libraries. No dependency binaries are committed to this repo.

The Maven jars lack license text resources, so unmodified upstream notices from
the LWJGL **3.3.3** tag are included in the RTX jar under
`META-INF/licenses/interstellar-rtx/`:

- `LWJGL.txt`: [LWJGL BSD license](https://github.com/LWJGL/lwjgl3/blob/3.3.3/LICENSE.md).
- `shaderc.txt`: [shaderc Apache 2.0 license](https://github.com/LWJGL/lwjgl3/blob/3.3.3/modules/lwjgl/shaderc/shaderc_license.txt).
- `Khronos.txt`: [Vulkan binding notice](https://github.com/LWJGL/lwjgl3/blob/3.3.3/modules/lwjgl/vulkan/khronos_license.txt).

These notices do not select a license for Interstellar itself.
