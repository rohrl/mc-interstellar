package io.github.rohrl.interstellar.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record OpticalSettings(boolean lensing, boolean grid, boolean aligned, boolean falling,
                              boolean lookBack, double startRadius, double playbackRate, Quality quality) {
    public enum Quality {
        FAST(.04f), STANDARD(.02f), FINE(.01f);
        private final float step;
        Quality(float step) { this.step = step; }
        public float step() { return step; }
        public Quality next() { return values()[(ordinal()+1)%values().length]; }
    }
    public OpticalSettings {
        if (!Double.isFinite(startRadius) || startRadius < (falling ? .35 : 1.05) || startRadius > 64)
            throw new IllegalArgumentException("startRadius must be within the supported observer range");
        if (!Double.isFinite(playbackRate) || playbackRate < .05 || playbackRate > 4)
            throw new IllegalArgumentException("playbackRate must be between 0.05 and 4");
        if (quality == null) throw new IllegalArgumentException("Unknown quality; use FAST, STANDARD or FINE");
    }
    public static OpticalSettings defaults() { return new OpticalSettings(true,true,true,false,false,8,1,Quality.STANDARD); }
    /** Partial files inherit defaults; wrong JSON types are rejected rather than coerced. */
    public static OpticalSettings parse(String input) {
        Gson gson = new Gson();
        JsonObject supplied = JsonParser.parseString(input).getAsJsonObject();
        JsonObject merged = gson.toJsonTree(defaults()).getAsJsonObject();
        for (String key : merged.keySet()) if (supplied.has(key)) {
            var value = supplied.get(key);
            var template = merged.getAsJsonPrimitive(key);
            if (!value.isJsonPrimitive() || (template.isBoolean() && !value.getAsJsonPrimitive().isBoolean())
                    || (template.isNumber() && !value.getAsJsonPrimitive().isNumber())
                    || (template.isString() && !value.getAsJsonPrimitive().isString()))
                throw new IllegalArgumentException("Wrong type for " + key);
            merged.add(key,value);
        }
        return gson.fromJson(merged, OpticalSettings.class);
    }
}
