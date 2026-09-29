package io.github.rohrl.interstellar.client;

import org.lwjgl.opengl.GL11;
import java.util.*;

/** No optional classes are touched until the user selects the separately compiled backend. */
final class WorldBackendBridge {
    static final boolean ENABLED=Boolean.getBoolean("interstellar.rtx");
    private Map<String,Integer> previous=Map.of();
    private Map<String,WorldRenderBackend.Image> images=Map.of();
    Map<String,WorldRenderBackend.Image> images(Map<String,Integer> ids) {
        if(ids.equals(previous))return images;
        var result=new LinkedHashMap<String,WorldRenderBackend.Image>();
        try(var state=new TerrainReplay.PackState()) {
            for(var entry:ids.entrySet()) {
                if(entry.getValue()<=0)throw new IllegalStateException("Missing appearance texture: "+entry.getKey());
                GL11.glBindTexture(GL11.GL_TEXTURE_2D,entry.getValue());
                int format=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_INTERNAL_FORMAT);
                // Minecraft also allocates unsized GL_RGBA. Normalize only after verifying
                // the actual component sizes; sharing must preserve the native bit layout.
                if(format==GL11.GL_RGBA && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_RED_SIZE)==8
                        && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_GREEN_SIZE)==8
                        && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_BLUE_SIZE)==8
                        && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_ALPHA_SIZE)==8)format=GL11.GL_RGBA8;
                result.put(entry.getKey(),new WorldRenderBackend.Image(entry.getValue(),
                    GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT),
                    GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER),GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER),
                    GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S),GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T),
                    format));
            }
        }
        previous=Map.copyOf(ids);images=Map.copyOf(result);return images;
    }
    WorldRenderBackend create(WorldMesh mesh,String source,Map<String,Integer> ids,int width,int height,int samples) throws Exception {
        var scene=new WorldRenderBackend.Scene(mesh.backendTerrain(),source,images(ids),width,height,samples);
        try(var state=new TerrainReplay.PackState()) {
            return (WorldRenderBackend)Class.forName("VulkanWorldBackend").getConstructor(WorldRenderBackend.Scene.class).newInstance(scene);
        }
    }
}
