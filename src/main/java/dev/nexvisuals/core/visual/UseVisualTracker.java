package dev.nexvisuals.core.visual;

/** Observes only an item's local use animation. Completion is cosmetic, never proof of consumption. */
public final class UseVisualTracker {
    public enum Event { NONE, ACTIVE, FINISHED }
    private Object world,item;
    private int remaining,observations;
    public Event sample(Object level,Object usedItem,int ticksLeft,boolean consuming) {
        if(level==null || level!=world) { reset();world=level; }
        if(!consuming || ticksLeft<=0) {
            boolean finished=level!=null && remaining==1 && observations>=2;
            remaining=observations=0;item=null;
            return finished?Event.FINISHED:Event.NONE;
        }
        if(item!=usedItem || ticksLeft>=remaining) observations=0;
        item=usedItem;remaining=ticksLeft;observations++;
        return Event.ACTIVE;
    }
    public void reset() { world=item=null;remaining=observations=0; }
}
