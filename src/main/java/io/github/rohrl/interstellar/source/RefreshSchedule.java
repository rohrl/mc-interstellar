package io.github.rohrl.interstellar.source;

/** Event-driven debounce/backoff; completed or incomplete probes stay idle until another event. */
final class RefreshSchedule {
    private boolean pending=true;
    private long due;
    private long cooldown;
    boolean pending() {return pending;}
    void changed(long tick) {pending=true;due=Math.max(cooldown,tick+5);}
    void retry(long tick) {pending=true;cooldown=tick+20;due=Math.max(due,cooldown);}
    boolean claim(long tick) {
        if(!pending||tick<due)return false;
        pending=false;return true;
    }
}
