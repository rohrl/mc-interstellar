package io.github.rohrl.interstellar.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.rohrl.interstellar.Interstellar;
import io.github.rohrl.interstellar.config.CalibrationSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class CalibrationConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CalibrationConfig() { }

    static CalibrationSettings load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("interstellar.json");
        CalibrationSettings defaults = CalibrationSettings.defaults();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(defaults) + System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return defaults;
            }
            CalibrationSettings settings = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8), CalibrationSettings.class);
            if (settings == null) {
                throw new IllegalArgumentException("Config must be an object, not null");
            }
            return settings;
        } catch (IOException | RuntimeException exception) {
            // Preserve invalid user input so it can be corrected rather than silently replaced.
            Interstellar.LOGGER.error("Cannot load {}. Using calibration defaults for this session; "
                    + "existing file is unchanged.", path, exception);
            return defaults;
        }
    }
}
