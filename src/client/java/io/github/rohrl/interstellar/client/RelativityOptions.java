package io.github.rohrl.interstellar.client;

import com.google.gson.*;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** Saved educational controls; disabling a component does not reset the sprint charge. */
record RelativityOptions(boolean enabled,boolean aberration,int colour,boolean brightness,double cap,int rampSeconds) {
    static RelativityOptions defaults(){return new RelativityOptions(true,true,1,true,.99,15);}
    static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("interstellar-relativity.json");}
    static RelativityOptions load() {
        try {
            if(!Files.exists(path())){var value=defaults();value.save();return value;}
            var json=JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            var d=defaults();var value=new RelativityOptions(bool(json,"enabled",d.enabled),bool(json,"aberration",d.aberration),
                json.has("colour")?json.get("colour").getAsInt():d.colour,bool(json,"brightness",d.brightness),
                json.has("cap")?json.get("cap").getAsDouble():d.cap,json.has("rampSeconds")?json.get("rampSeconds").getAsInt():d.rampSeconds);
            if(value.colour<0||value.colour>2||!Double.isFinite(value.cap)||value.cap<.1||value.cap>.99||value.rampSeconds<5||value.rampSeconds>60)
                throw new IllegalArgumentException("Relativity settings out of range");
            return value;
        } catch(Exception e){Interstellar.LOGGER.error("Cannot load relativity settings; file preserved",e);return defaults();}
    }
    private static boolean bool(JsonObject j,String key,boolean fallback) {
        if(!j.has(key))return fallback;var p=j.getAsJsonPrimitive(key);
        if(!p.isBoolean())throw new IllegalArgumentException(key+" must be boolean");return p.getAsBoolean();
    }
    void save() throws java.io.IOException {
        var j=Files.exists(path())?JsonParser.parseString(Files.readString(path())).getAsJsonObject():new JsonObject();
        j.addProperty("enabled",enabled);j.addProperty("aberration",aberration);j.addProperty("colour",colour);
        j.addProperty("brightness",brightness);j.addProperty("cap",cap);j.addProperty("rampSeconds",rampSeconds);
        Files.writeString(path(),new GsonBuilder().setPrettyPrinting().create().toJson(j)+"\n");
    }
    String colourName(){return colour==0?"Off":colour==1?"Gentle":"Full shift";}
}
