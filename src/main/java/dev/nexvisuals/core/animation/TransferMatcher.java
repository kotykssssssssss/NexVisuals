package dev.nexvisuals.core.animation;

import java.util.ArrayList;
import java.util.List;

/** Counts of one exact item+components before/after a local action. Never guesses destinations. */
public final class TransferMatcher {
    private TransferMatcher() { }
    public static List<Integer> destinations(int source, int[] before, int[] after) {
        if (before.length != after.length || source < 0 || source >= before.length) return List.of();
        int lost = before[source] - after[source];
        if (lost <= 0) return List.of();
        long gained = 0;
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < before.length; i++) {
            if (before[i] < 0 || after[i] < 0) return List.of();
            if (i == source) continue;
            int delta = after[i] - before[i];
            if (delta < 0) return List.of();
            if (delta > 0) { gained += delta; targets.add(i); }
        }
        return gained == lost && targets.size() <= 8 ? List.copyOf(targets) : List.of();
    }
}
