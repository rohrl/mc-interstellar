package io.github.rohrl.interstellar.client;

import com.google.gson.JsonParser;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

record TerrainOptions(boolean enabled,float renderScale,boolean antialiasing) {
    static TerrainOptions load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("interstellar-terrain.json");
        try {
            if(!Files.exists(path)) Files.writeString(path,"{\n  \"enabled\": true,\n  \"renderScale\": 0.5\n}\n",StandardOpenOption.CREATE_NEW);
            var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            boolean enabled=true,aa=true;float scale=.5f;
            if(json.has("enabled")) {
                var value=json.get("enabled").getAsJsonPrimitive();
                if(!value.isBoolean()) throw new IllegalArgumentException("enabled must be boolean");
                enabled=value.getAsBoolean();
            }
            if(json.has("renderScale")) {
                var value=json.get("renderScale").getAsJsonPrimitive();
                if(!value.isNumber()) throw new IllegalArgumentException("renderScale must be numeric");
                scale=value.getAsFloat();
                if(scale!=.5f && scale!=1f) throw new IllegalArgumentException("renderScale must be 0.5 or 1");
            }
                        if(json.has("antialiasing")) {
                var value=json.get("antialiasing").getAsJsonPrimitive();
                if(!value.isBoolean()) throw new IllegalArgumentException("antialiasing must be boolean");
                aa=value.getAsBoolean();
            }
            return new TerrainOptions(enabled,scale,aa);
        } catch(Exception failure) {Interstellar.LOGGER.error("Cannot load {}; defaults used, file preserved",path,failure);return new TerrainOptions(true,.5f,true);}
    }
}
