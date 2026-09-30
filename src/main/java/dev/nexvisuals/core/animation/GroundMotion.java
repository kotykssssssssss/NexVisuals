package dev.nexvisuals.core.animation;

/** Observes local movement only. First samples, teleports and walking off ledges are not jumps. */
public final class GroundMotion {
    public enum Event { NONE, JUMP, LAND }
    private boolean initialized, grounded;
    private double x, y, z, descent, impact;

    public Event sample(double x, double y, double z, boolean onGround) {
        Event event = Event.NONE;
        if (initialized) {
            double dx=x-this.x, dy=y-this.y, dz=z-this.z;
            if (dx*dx+dy*dy+dz*dz > 9) descent=0;
            else {
                if (grounded && !onGround && dy>.08) event=Event.JUMP;
                if (!onGround && dy<0) descent+=-dy;
                if (!grounded && onGround && descent>.15) {
                    event=Event.LAND; impact=Math.min(1,descent/3);
                }
                if (onGround) descent=0;
            }
        }
        this.x=x; this.y=y; this.z=z; grounded=onGround; initialized=true;
        return event;
    }
    public double impact() { return impact; }
    public void reset() { initialized=false; descent=impact=0; }
}
