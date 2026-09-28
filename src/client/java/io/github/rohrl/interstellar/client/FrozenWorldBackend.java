package io.github.rohrl.interstellar.client;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;

/** Optional backend boundary. No Vulkan types or class initialization in the normal renderer.
 * The prototype owns a frozen scene; live updates and automatic backend selection are separate work. */
public interface FrozenWorldBackend extends AutoCloseable {
    record Texture(int width,int height,int minFilter,int magFilter,int wrapS,int wrapT,int format,ByteBuffer rgba) {}
    record Scene(Path geometry,String opticalSource,Map<String,Texture> textures,int width,int height) {}
    /** Returns a shared OpenGL RGBA32F texture containing both AA samples side by side. */
    int render(Map<String,float[]> uniforms);
    /** Benchmark only: explicitly finish backend work and read its own GPU clock. */
    double completedGpuMillis();
    String description();
    @Override void close();
}
