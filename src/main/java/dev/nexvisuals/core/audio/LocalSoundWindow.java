package dev.nexvisuals.core.audio;

/** Short, bounded acoustic context for a local action; never changes packets or global sound options. */
public final class LocalSoundWindow {
    private Object level;
    private long time;
    private double x,y,z;
    private boolean armed;
    public void arm(long nanos,Object level,double x,double y,double z) {
        this.time=nanos;this.level=level;this.x=x;this.y=y;this.z=z;armed=true;
    }
    public boolean contains(long nanos,Object level,double x,double y,double z) {
        expire(nanos,level);
        long age=nanos-time;
        double dx=x-this.x,dy=y-this.y,dz=z-this.z;
        return armed && this.level==level && level!=null && age>=0 && age<=450_000_000L && dx*dx+dy*dy+dz*dz<=1;
    }
    public void clear() {armed=false;level=null;}
    public void expire(long nanos,Object currentLevel) {
        if(armed && (level!=currentLevel || nanos-time>450_000_000L || nanos-time<0)) clear();
    }
    public static boolean isVanillaAttack(String namespace,String path) {
        if(!"minecraft".equals(namespace)) return false;
        return switch(path) {
            case "entity.player.attack.strong", "entity.player.attack.weak", "entity.player.attack.crit",
                 "entity.player.attack.knockback", "entity.player.attack.sweep", "entity.player.attack.nodamage" -> true;
            default -> false;
        };
    }
}
