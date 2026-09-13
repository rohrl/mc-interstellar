package io.github.rohrl.interstellar.config;

/** Only implemented calibration options. No placeholder effect switches. */
public record CalibrationSettings(boolean hudEnabled, double schwarzschildRadius,
                                  double sourceDistance) {
    public CalibrationSettings {
        if (!Double.isFinite(schwarzschildRadius) || schwarzschildRadius <= 0.0) {
            throw new IllegalArgumentException("schwarzschildRadius must be finite and positive");
        }
        if (!Double.isFinite(sourceDistance) || sourceDistance <= 0.0) {
            throw new IllegalArgumentException("sourceDistance must be finite and positive");
        }
    }

    public static CalibrationSettings defaults() {
        return new CalibrationSettings(true, 8.0, 64.0);
    }
}
