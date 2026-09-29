package io.github.rohrl.interstellar.wormhole;

/** Opening around an observer is not entry; they must approach from outside. */
final class EntryGate {
    private EntryGate() {}
    static boolean armed(boolean ready,boolean outside,boolean previouslyArmed,double stepSquared) {
        return ready && (outside || previouslyArmed && stepSquared<16);
    }
}
