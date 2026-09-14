package io.github.rohrl.interstellar.client;

import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.config.OpticalSettings;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

final class OpticalConfig {
    private OpticalConfig() { }
    static OpticalSettings load() {
        var path = FabricLoader.getInstance().getConfigDir().resolve("interstellar-optics.json");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(OpticalSettings.defaults())+"\n", StandardOpenOption.CREATE_NEW);
            }
            return OpticalSettings.parse(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            Interstellar.LOGGER.error("Cannot read {}. Using optical defaults; existing file preserved.",path,e);
            return OpticalSettings.defaults();
        }
    }
}
