package io.github.rohrl.interstellar.client;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

/** Optional backend boundary. No Vulkan types, dependency loading or update work in OpenGL-only builds. */
public interface WorldRenderBackend extends AutoCloseable {
    enum Optics { EXTERIOR, EXTENDED, HORIZON }
    record Texture(int width,int height,int minFilter,int magFilter,int wrapS,int wrapT,int format,ByteBuffer rgba) {}
    record Snapshot(Path geometry,String opticalSource,Map<String,Texture> textures,int width,int height) {}
    record Chunk(long key,long revision,int row,int quads) {}
    /** read returns an owned native RGBA32F quad buffer; caller releases it after copying. */
    interface Terrain {
        long revision();
        int rows();
        List<Chunk> chunks();
        ByteBuffer read(Chunk chunk);
    }
    record Image(int id,int width,int height,int minFilter,int magFilter,int wrapS,int wrapT,int format) {}
    record Scene(Terrain terrain,String opticalSource,Map<String,Image> images,int width,int height) {}
    default void update(float[] triangles,int count,long revision,Map<String,Image> images) {}
    default void profiling(boolean enabled) {}
    /** Select precompiled optics without rebuilding resident scene geometry. */
    default void optics(Optics model) {
        if(model!=Optics.EXTERIOR)throw new UnsupportedOperationException("Optical variant unavailable: "+model);
    }
    default double previousGpuMillis() {return Double.NaN;}
    /** Returns a shared OpenGL RGBA32F texture containing both AA samples side by side. */
    int render(Map<String,float[]> uniforms);
    /** Benchmark only: explicitly finish backend work and read its own GPU clock. */
    double completedGpuMillis();
    String description();
    @Override void close();
}
