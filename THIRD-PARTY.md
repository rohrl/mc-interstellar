# Third-party material

Gradle wrapper files are from the official Gradle v8.10.2 source (Apache License 2.0); their script headers are preserved. See gradle/wrapper/LICENSE. The official Fabric example repository (CC0) was consulted for build structure; Interstellar's application code and documents are new work.

Minecraft and Fabric are external build/runtime dependencies with their own terms. No Minecraft game binaries or extracted game assets are committed. The illustrated guide includes screenshots of the running development mod for documentation. No scientific reference implementation or external sky imagery has been copied into this repository. A license for Interstellar itself remains unselected.

The optional standalone RTX probe downloads official LWJGL 3.3.3 and shaderc binding/native distributions from Maven Central, with pinned SHA-256 checksums. They are external dependencies stored in ignored `run/rtx/lib`; their upstream license/notice files remain in the distributions. No jars or upstream sample implementations are vendored. See [the probe README](tools/rtx-probe/README.md).
