package io.github.rohrl.interstellar.client;

import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Opt-in bridge: capture once, then pass only camera/uniform values to the frozen backend. */
final class FrozenBackendCapture {
    static final boolean ENABLED=Boolean.getBoolean("interstellar.rtxImage");
    private static final Pattern UNIFORM=Pattern.compile("uniform\\s+(float|vec[234])\\s+([^;]+);");
    private FrozenBackendCapture() {}
    static String source() throws Exception {
        try(var in=FrozenBackendCapture.class.getResourceAsStream("/assets/interstellar/shaders/include/terrain_shared.glsl")) {
            if(in==null)throw new IllegalStateException("Optical shader resource missing");
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    static Map<String,float[]> uniforms(ShaderProgram program,String source) {
        var values=new LinkedHashMap<String,float[]>();var matcher=UNIFORM.matcher(source);
        while(matcher.find())for(String name:matcher.group(2).split(",")) {
            name=name.trim();var u=program.getUniform(name);float[] row=new float[4];
            if(u!=null && u.getFloatData()!=null)for(int i=0;i<Math.min(4,u.getCount());i++)row[i]=u.getFloatData().get(i);
            values.put(name,row);
        }
        return values;
    }
    static FrozenWorldBackend create(WorldMesh mesh,WorldMesh moving,Map<String,Integer> ids,int w,int h,String source) throws Exception {
        var textures=new LinkedHashMap<String,FrozenWorldBackend.Texture>();
        try(TerrainReplay.PackState state=new TerrainReplay.PackState()) {
            for(var entry:ids.entrySet()) {
                int id=entry.getValue();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
                int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
                textures.put(entry.getKey(),new FrozenWorldBackend.Texture(width,height,
                    GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER),GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER),
                    GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S),GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T),
                    GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_INTERNAL_FORMAT),TerrainReplay.readTexture(id,0,0,width,height,false)));
            }
            var scene=new FrozenWorldBackend.Scene(TerrainReplay.exportFrozenGeometry(mesh,moving),source,textures,w,h);
            return (FrozenWorldBackend)Class.forName("FullImageProbe").getConstructor(FrozenWorldBackend.Scene.class).newInstance(scene);
        } finally {for(var texture:textures.values())MemoryUtil.memFree(texture.rgba());}
    }
}
