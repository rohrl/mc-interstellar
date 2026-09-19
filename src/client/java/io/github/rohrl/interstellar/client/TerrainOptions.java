package io.github.rohrl.interstellar.client;

import com.google.gson.JsonParser;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

record TerrainOptions(boolean enabled,float renderScale,boolean distantPrototype,int antialiasing) {
    static TerrainOptions load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("interstellar-terrain.json");
        try {
            if(!Files.exists(path)) Files.writeString(path,"{\n  \"enabled\": true,\n  \"renderScale\": 0.5,\n  \"distantPrototype\": false,\n  \"antialiasing\": \"2x\"\n}\n",StandardOpenOption.CREATE_NEW);
            var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            boolean enabled=true;float scale=.5f;
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
            boolean distant=false;
            if(json.has("distantPrototype")) {
                var value=json.get("distantPrototype").getAsJsonPrimitive();
                if(!value.isBoolean())throw new IllegalArgumentException("distantPrototype must be boolean");
                distant=value.getAsBoolean();
            }
            int aa=2;
            if(json.has("antialiasing")) {
                var value=json.get("antialiasing").getAsJsonPrimitive();
                aa=value.isBoolean()?(value.getAsBoolean()?2:0):switch(value.getAsString()) {
                    case "off" -> 0;case "edge" -> 1;case "2x" -> 2;
                    default -> throw new IllegalArgumentException("antialiasing must be off, edge or 2x");
                };
            }
            return new TerrainOptions(enabled,scale,distant,aa);
        } catch(Exception failure) {Interstellar.LOGGER.error("Cannot load {}; defaults used, file preserved",path,failure);return new TerrainOptions(true,.5f,false,2);}
    }
}
