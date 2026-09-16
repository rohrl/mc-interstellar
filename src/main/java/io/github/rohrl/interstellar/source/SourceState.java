package io.github.rohrl.interstellar.source;

/** READY accompanies a complete SourcePayload; all other states have no usable optical metadata. */
public enum SourceState {
    NONE("Inspect a mass block to select a source"),
    READY("Source ready"),
    REFRESHING("Source changed: refreshing automatically"),
    UNLOADED("Source incomplete: waiting for chunks to load"),
    REMOVED("Inspected block removed: replace it or inspect another"),
    LIMIT("Source exceeds 4096-block limit: reduce it or inspect another");
    private final String message;
    SourceState(String message) {this.message=message;}
    public String message() {return message;}
}
