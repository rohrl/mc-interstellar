package io.github.rohrl.interstellar.client;

import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

record TerrainOptions(boolean enabled,float renderScale,boolean distantPrototype,int antialiasing,
                      boolean finePaths,boolean preferRtx,boolean weather,boolean bodyImages) {
    static String aaLabel(int aa) {return aa==0?"Off":aa==1?"Edge":aa+"x";}
    static int nextAa(int aa) {return switch(aa){case 0->1;case 1->2;case 2->4;case 4->8;default->0;};}
    TerrainOptions quality(float scale,int aa,boolean fine,boolean rtx) {
        return new TerrainOptions(enabled,scale,distantPrototype,aa,fine,rtx,weather,bodyImages);
    }
    TerrainOptions features(boolean rain,boolean body) {
        return new TerrainOptions(enabled,renderScale,distantPrototype,antialiasing,finePaths,preferRtx,rain,body);
    }
    void save() throws java.io.IOException {
        var path=FabricLoader.getInstance().getConfigDir().resolve("interstellar-terrain.json");
        // Preserve unrelated keys; a malformed existing file must not be overwritten.
        var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        json.addProperty("enabled",enabled);json.addProperty("renderScale",renderScale);
        json.addProperty("distantPrototype",distantPrototype);
        json.addProperty("antialiasing",aaLabel(antialiasing).toLowerCase(java.util.Locale.ROOT));
        json.addProperty("finePaths",finePaths);json.addProperty("preferRtx",preferRtx);
        json.addProperty("weather",weather);json.addProperty("bodyImages",bodyImages);
        Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json)+"\n");
    }
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
                    case "off" -> 0;case "edge" -> 1;case "2x" -> 2;case "4x" -> 4;case "8x" -> 8;
                    default -> throw new IllegalArgumentException("antialiasing must be off, edge, 2x, 4x or 8x");
                };
            }
            return new TerrainOptions(enabled,scale,distant,aa,bool(json,"finePaths",false),bool(json,"preferRtx",true),
                bool(json,"weather",true),bool(json,"bodyImages",false));
        } catch(Exception failure) {Interstellar.LOGGER.error("Cannot load {}; defaults used, file preserved",path,failure);return new TerrainOptions(true,.5f,false,2,false,true,true,false);}
    }
    private static boolean bool(com.google.gson.JsonObject json,String key,boolean fallback) {
        if(!json.has(key))return fallback;
        var value=json.getAsJsonPrimitive(key);
        if(!value.isBoolean())throw new IllegalArgumentException(key+" must be boolean");
        return value.getAsBoolean();
    }
}
