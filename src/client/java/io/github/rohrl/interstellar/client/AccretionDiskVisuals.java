package io.github.rohrl.interstellar.client;

import com.google.gson.*;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.config.AccretionSettings;
import io.github.rohrl.interstellar.source.SourcePayload;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.math.Vec3d;
import java.nio.file.*;

/** Client-only disk controls. Updating a uniform never rebuilds the world/backend. */
final class AccretionDiskVisuals {
    private static AccretionSettings options=load();
    static AccretionSettings options(){return options;}
    static void reload(){options=load();}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("interstellar-disk.json");}
    static void apply(AccretionSettings next) throws java.io.IOException {
        var json=Files.exists(path())?JsonParser.parseString(Files.readString(path())).getAsJsonObject():new JsonObject();
        var fields=new Gson().toJsonTree(next).getAsJsonObject();
        fields.entrySet().forEach(e->json.add(e.getKey(),e.getValue()));
        Files.writeString(path(),new GsonBuilder().setPrettyPrinting().create().toJson(json)+"\n");
        options=next;Interstellar.LOGGER.info("Accretion disk settings saved: {}",next);
    }
    private static AccretionSettings load() {
        var d=AccretionSettings.defaults();
        try {
            if(!Files.exists(path()))return d;
            var json=JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            boolean animate=d.animation();
            if(json.has("animation")) {
                var value=json.getAsJsonPrimitive("animation");
                if(!value.isBoolean())throw new IllegalArgumentException("animation must be boolean");
                animate=value.getAsBoolean();
            }
            float mode=number(json,"mode",d.mode());
            if(mode!=(int)mode)throw new IllegalArgumentException("mode must be an integer");
            return new AccretionSettings((int)mode,animate,number(json,"brightness",d.brightness()),
                number(json,"outerRadius",d.outerRadius()),number(json,"tilt",d.tilt()),number(json,"threshold",d.threshold()),number(json,"glow",d.glow()));
        } catch(Exception e){Interstellar.LOGGER.error("Cannot load accretion settings; defaults used, file preserved",e);return d;}
    }
    private static float number(JsonObject json,String name,float fallback) {
        if(!json.has(name))return fallback;var value=json.getAsJsonPrimitive(name);
        if(!value.isNumber())throw new IllegalArgumentException(name+" must be numeric");
        return value.getAsFloat();
    }
    static double radius(SourcePayload source) {
        return source!=null&&options.visible(source.blackHoleProxy(),source.schwarzschildRadius())?source.schwarzschildRadius():0;
    }
    static void configure(ShaderProgram shader,SourcePayload source,Vec3d origin,double seconds) {
        double radius=radius(source);var centre=source==null?Vec3d.ZERO:new Vec3d(source.x(),source.y(),source.z()).subtract(origin);
        shader.getUniformOrDefault("DiskSource").set((float)centre.x,(float)centre.y,(float)centre.z,(float)radius);
        double tilt=Math.toRadians(options.tilt());
        shader.getUniformOrDefault("DiskAxis").set((float)Math.sin(tilt),(float)Math.cos(tilt),0f);
        // Orbital rates use c_display=60 blocks/s; this is an animation clock,
        // independent of Minecraft motion and the observer potion's prescribed c.
        shader.getUniformOrDefault("DiskSettings").set(options.outerRadius(),options.brightness()*36,
            radius>0&&options.animation()?(float)(seconds*60/radius):0f,4200f);
        shader.getUniformOrDefault("DiskBloom").set(radius>0?options.glow():0f);
    }
}
