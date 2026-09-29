package io.github.rohrl.interstellar.wormhole;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntryGateTest {
    @Test void openingAroundAPlayerRequiresLeavingAndReentering() {
        boolean armed=EntryGate.armed(false,false,false,0);
        armed=EntryGate.armed(true,false,armed,0);assertFalse(armed);
        armed=EntryGate.armed(true,false,armed,.1);assertFalse(armed);
        armed=EntryGate.armed(true,true,armed,.1);assertTrue(armed);
        armed=EntryGate.armed(true,false,armed,.1);assertTrue(armed);
    }
    @Test void replacementAndTeleportCannotInheritEntry() {
        assertFalse(EntryGate.armed(false,false,true,.1));
        boolean armed=EntryGate.armed(true,false,true,100);
        assertFalse(armed);assertFalse(EntryGate.armed(true,false,armed,0));
    }
}
