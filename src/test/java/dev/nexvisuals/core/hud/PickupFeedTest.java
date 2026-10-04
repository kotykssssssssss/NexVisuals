package dev.nexvisuals.core.hud;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PickupFeedTest {
    private void offer(PickupFeed<String> feed, String item, int amount, long tick) {
        feed.offer(item, amount, tick, 60, 4, Objects::equals);
    }
    @Test void repeatedPickupsMergeCountsRefreshTheirLifetimeAndMoveToTheFront() {
        var feed = new PickupFeed<String>();
        offer(feed, "diamond", 3, 0); offer(feed, "apple", 2, 1); offer(feed, "diamond", 4, 10);
        assertEquals(2, feed.notices().size());
        assertEquals(new PickupFeed.Notice<>("diamond", 7, 10), feed.notices().getFirst());
        feed.prune(61, 60, 4);
        assertEquals(1, feed.notices().size());
        feed.prune(70, 60, 4);
        assertTrue(feed.notices().isEmpty());
    }
    @Test void distinctComponentsAreNotMergedAndExpiredAmountsAreNotReused() {
        var feed = new PickupFeed<String>();
        offer(feed, "sword#sharpness", 1, 0); offer(feed, "sword#smite", 2, 0);
        assertEquals(2, feed.notices().size());
        offer(feed, "sword#smite", 1, 100);
        assertEquals(1, feed.notices().size()); assertEquals(1, feed.notices().getFirst().amount());
    }
    @Test void burstsAreBoundedAndCountsCannotOverflow() {
        var feed = new PickupFeed<String>();
        for (int i = 0; i < 1000; i++) feed.offer("item" + i, 1, 0, 60, Integer.MAX_VALUE, Objects::equals);
        assertEquals(PickupFeed.MAX_ROWS, feed.notices().size());
        feed.offer("item999", Integer.MAX_VALUE, 1, 60, 6, Objects::equals);
        assertEquals(PickupFeed.MAX_AMOUNT, feed.notices().getFirst().amount());
        feed.prune(2, 60, 2); assertEquals(2, feed.notices().size());
        assertThrows(UnsupportedOperationException.class, () -> feed.notices().clear());
    }
    @Test void invalidEventsDoNotCreateNotificationsAndClockRollbackClearsOldOnes() {
        var feed = new PickupFeed<String>();
        offer(feed, "apple", 0, 0); offer(feed, "apple", -1, 0); offer(feed, "apple", 1, -1);
        assertTrue(feed.notices().isEmpty());
        offer(feed, "diamond", 5, 100);
        feed.prune(10, 60, 4); assertTrue(feed.notices().isEmpty());
        offer(feed, "apple", 1, 11); feed.clear(); assertTrue(feed.notices().isEmpty());
    }
    @Test void opacityUsesElapsedTimeWithBoundedEntryAndExitFades() {
        assertEquals(0, PickupFeed.opacity(0, 60));
        assertEquals(.5, PickupFeed.opacity(2, 60));
        assertEquals(1, PickupFeed.opacity(20, 60));
        assertEquals(.5, PickupFeed.opacity(56, 60));
        assertEquals(0, PickupFeed.opacity(60, 60));
        assertEquals(0, PickupFeed.opacity(Double.NaN, 60));
        assertEquals(0, PickupFeed.opacity(-1, 60));
        assertEquals(0, PickupFeed.opacity(1, 0));
        for (double age = 0; age <= 60; age += .25) assertTrue(PickupFeed.opacity(age, 60) >= 0 && PickupFeed.opacity(age, 60) <= 1);
    }
}
