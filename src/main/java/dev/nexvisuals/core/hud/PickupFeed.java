package dev.nexvisuals.core.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/** Bounded transient notices. Payloads are supplied by the client, never persisted or executed. */
public final class PickupFeed<T> {
    public static final int MAX_ROWS = 6;
    public static final int MAX_AMOUNT = 9999;
    public record Notice<T>(T item, int amount, long tick) { }
    private final List<Notice<T>> notices = new ArrayList<>(MAX_ROWS);
    private final List<Notice<T>> view = Collections.unmodifiableList(notices);
    private long revision;

    public List<Notice<T>> notices() { return view; }
    public long revision() { return revision; }

    public void offer(T item, int amount, long tick, int lifetime, int rows, BiPredicate<T, T> sameItem) {
        Objects.requireNonNull(item);
        if (amount <= 0 || tick < 0) return;
        prune(tick, lifetime, rows);
        int total = Math.min(amount, MAX_AMOUNT);
        for (int i = 0; i < notices.size(); i++) {
            Notice<T> notice = notices.get(i);
            if (!sameItem.test(notice.item(), item)) continue;
            total = (int) Math.min(MAX_AMOUNT, (long) total + notice.amount());
            notices.remove(i);
            break;
        }
        notices.addFirst(new Notice<>(item, total, tick));
        trim(rows);
        revision++;
    }

    public void prune(long tick, int lifetime, int rows) {
        boolean expired = notices.removeIf(notice -> tick < notice.tick() || tick - notice.tick() >= Math.max(1, lifetime));
        if (expired) revision++;
        trim(rows);
    }

    private void trim(int rows) {
        int limit = Math.clamp(rows, 1, MAX_ROWS);
        if (notices.size() <= limit) return;
        notices.subList(limit, notices.size()).clear();
        revision++;
    }

    public static double opacity(double age, int lifetime) {
        if (!Double.isFinite(age) || age < 0 || lifetime <= 0 || age >= lifetime) return 0;
        double enter = Math.clamp(age / 4, 0, 1);
        double leave = Math.clamp((lifetime - age) / 8, 0, 1);
        return Math.min(enter, leave);
    }

    public void clear() {
        if (!notices.isEmpty()) { notices.clear(); revision++; }
    }
}
