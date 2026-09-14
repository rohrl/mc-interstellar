package io.github.rohrl.interstellar.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpticalSettingsTest {
    @Test void partialConfigurationPreservesDefaultsAndUnknownFutureFields() {
        var settings = OpticalSettings.parse("{\"grid\":false,\"futureOption\":42}");
        assertFalse(settings.grid());
        assertTrue(settings.lensing());
        assertEquals(8,settings.startRadius());
        assertEquals(OpticalSettings.Quality.STANDARD,settings.quality());
    }
    @Test void invalidTypesAndRangesAreRejected() {
        for (String json : new String[]{"null","[]","{\"lensing\":\"false\"}","{\"grid\":null}",
                "{\"quality\":\"SUPER\"}","{\"startRadius\":0.5}","{\"playbackRate\":0}","{\"startRadius\":1e999}"})
            assertThrows(RuntimeException.class,()->OpticalSettings.parse(json),json);
    }
    @Test void interiorBookmarkRequiresFallingFrameAndQualityBudgetsAreBounded() {
        var settings=OpticalSettings.parse("{\"falling\":true,\"startRadius\":0.5,\"quality\":\"FINE\"}");
        assertEquals(.5,settings.startRadius());
        for (var quality:OpticalSettings.Quality.values()) assertTrue(Math.round(16/quality.step())<=1600);
        assertEquals(OpticalSettings.Quality.FAST,OpticalSettings.Quality.FINE.next());
    }
}
