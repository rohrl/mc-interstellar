package io.github.rohrl.interstellar.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RendererAvailabilityTest {
    @Test void normalBuildRemainsOpenGlAndBundleDefaultsToRtx() {
        assertFalse(RendererAvailability.rtxEnabled(false,null,"Windows 11","amd64"));
        assertTrue(RendererAvailability.rtxEnabled(true,null,"Windows 11","amd64"));
    }
    @Test void explicitOptOutAndUnsupportedPlatformsNeverEnableNativeBackend() {
        assertFalse(RendererAvailability.rtxEnabled(true,"false","Windows 11","amd64"));
        assertFalse(RendererAvailability.rtxEnabled(true,"true","Linux","amd64"));
        assertFalse(RendererAvailability.rtxEnabled(true,"true","Windows 11","aarch64"));
        assertFalse(RendererAvailability.rtxEnabled(true,null,"Darwin","x86_64"));
    }
    @Test void legacyDevelopmentOptInRemainsAvailable() {
        assertTrue(RendererAvailability.rtxEnabled(false,"true","Windows 10","x86_64"));
    }
}
