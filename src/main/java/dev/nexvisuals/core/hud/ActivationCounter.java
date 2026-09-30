package dev.nexvisuals.core.hud;

import java.lang.ref.WeakReference;

/** Session-only counts for explicit events. Placement/settings persist separately through the HUD framework. */
public final class ActivationCounter {
    private WeakReference<Object> level=new WeakReference<>(null);
    private int count;
    private long last;
    public void observeLevel(Object current,boolean resetOnWorldChange) {
        if(level.get()!=current) {if(resetOnWorldChange) reset();level=new WeakReference<>(current);}
    }
    public void record(Object current,long nanos,boolean resetOnWorldChange) {
        if(current==null) return;
        observeLevel(current,resetOnWorldChange);if(count<Integer.MAX_VALUE) count++;last=nanos;
    }
    public int count() {return count;}
    public long elapsedSeconds(long nanos) {return count==0?0:Math.max(0,(nanos-last)/1_000_000_000L);}
    public void reset() {count=0;last=0;}
}
