package io.github.rohrl.interstellar.config;

/** Backend eligibility without loading optional native bindings. */
public final class RendererAvailability {
    private RendererAvailability() {}
    public static boolean rtxEnabled(boolean bundled,String override,String os,String architecture) {
        boolean windows=os.toLowerCase(java.util.Locale.ROOT).startsWith("windows");
        boolean x64=architecture.equalsIgnoreCase("amd64")||architecture.equalsIgnoreCase("x86_64");
        return windows&&x64&&(override==null?bundled:Boolean.parseBoolean(override));
    }
}
