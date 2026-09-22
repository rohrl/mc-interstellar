package io.github.rohrl.interstellar;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Interstellar implements ModInitializer {
    public static final String MOD_ID = "interstellar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        io.github.rohrl.interstellar.source.SourceBlocks.register();
        io.github.rohrl.interstellar.demo.DemoCommands.register();
        LOGGER.info("Interstellar loaded. F8 opens the optical lab in a development world.");
    }
}
