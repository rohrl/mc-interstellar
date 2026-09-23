package io.github.rohrl.interstellar.source;

/** READY accompanies complete metadata; REFRESHING permits a bounded client-side optical grace period. */
public enum SourceState {
    NONE("Looking for nearby mass blocks automatically"),
    READY("Source ready"),
    REFRESHING("Source changed: refreshing automatically"),
    UNLOADED("Source incomplete: waiting for chunks to load"),
    REMOVED("Source removed: looking for another nearby cluster"),
    LIMIT("Source exceeds 4096-block limit: reduce it or inspect another");
    private final String message;
    SourceState(String message) {this.message=message;}
    public String message() {return message;}
}
