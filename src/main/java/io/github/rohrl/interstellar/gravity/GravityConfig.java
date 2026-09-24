package io.github.rohrl.interstellar.gravity;

import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;

public final class GravityConfig {
    public boolean enabled=true;
    public boolean capture=true;
    public double strengthPerBlock=GravityField.DEFAULT_STRENGTH;
    public static GravityConfig load() {
        var defaults=new GravityConfig();
        var path=FabricLoader.getInstance().getConfigDir().resolve("interstellar-gravity.json");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        try {
            if(!Files.exists(path)) {Files.writeString(path,gson.toJson(defaults));return defaults;}
            var value=gson.fromJson(Files.readString(path),GravityConfig.class);
            if(value==null||!Double.isFinite(value.strengthPerBlock)||value.strengthPerBlock<0||value.strengthPerBlock>.2)
                throw new IllegalArgumentException("strengthPerBlock must be finite and in 0..0.2");
            return value;
        } catch(Exception failure) {
            Interstellar.LOGGER.error("Invalid gravity config; entity gravity disabled until corrected (file preserved)",failure);
            defaults.enabled=false;return defaults;
        }
    }
}
