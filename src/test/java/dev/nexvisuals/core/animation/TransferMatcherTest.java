package dev.nexvisuals.core.animation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferMatcherTest {
    @Test void matchesOnlyConservedSingleItemTransfersIncludingSplitStacks() {
        assertEquals(List.of(1), TransferMatcher.destinations(0, new int[]{16,0}, new int[]{0,16}));
        assertEquals(List.of(1,2), TransferMatcher.destinations(0, new int[]{32,60,0}, new int[]{0,64,28}));
    }
    @Test void ambiguousOrAsynchronousChangesNeverInventADestination() {
        assertTrue(TransferMatcher.destinations(0,new int[]{16,0},new int[]{16,0}).isEmpty());
        assertTrue(TransferMatcher.destinations(0,new int[]{16,0},new int[]{0,0}).isEmpty());
        assertTrue(TransferMatcher.destinations(0,new int[]{16,0},new int[]{0,32}).isEmpty());
        assertTrue(TransferMatcher.destinations(0,new int[]{16,4,0},new int[]{0,0,20}).isEmpty());
        assertTrue(TransferMatcher.destinations(-1,new int[]{16},new int[]{0}).isEmpty());
        assertTrue(TransferMatcher.destinations(0,new int[]{16},new int[]{0,16}).isEmpty());
    }
}
