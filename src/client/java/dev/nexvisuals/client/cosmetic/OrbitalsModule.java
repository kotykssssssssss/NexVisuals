package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

public final class OrbitalsModule extends VisualModule {
    public enum Style { CROWN, ATOM, HELIX }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Path","A horizontal crown, intersecting tilted orbits or a vertical spiral.",Style.ATOM,Style.class));
    private final ColorSetting primary=add(new ColorSetting("primary","Primary","ARGB with opacity.",0xDD98EEFF));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Secondary","Alternating orbit color.",0xBBB894FF));
    private final IntSetting count=add(new IntSetting("count","Orbit points","At most twelve points, emitted every two ticks and removed after four ticks.",6,3,12));
    private final DoubleSetting radius=add(new DoubleSetting("radius","Radius","Distance from the local body in blocks.",.8,.3,1.6));
    private final DoubleSetting height=add(new DoubleSetting("height","Height","Center of the pattern above the player's feet.",1.1,.3,2.4));
    private final DoubleSetting size=add(new DoubleSetting("size","Size","Star radius in blocks.",.09,.025,.22));
    private final DoubleSetting speed=add(new DoubleSetting("speed","Orbit speed","Turns per second, calculated from world time.",.12,.02,.5));
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    public OrbitalsModule(EffectEmitter emitter) {
        super("cosmetic_orbitals","Cosmetic Orbitals","Small moving constellations around the visible local player in third person. Ordinary depth; no remote entities.",Category.WORLD);
        this.emitter=emitter;
        preset("Atom","Intersecting cool tilted orbits.");
        preset("Starlight Crown","A slow golden crown above the head.","style","CROWN","height",2.1,"radius",.5,"primary","#FFF9DE95","secondary","#BBFFFFFF","speed",.06);
        preset("Spiral","A violet spiral around the body.","style","HELIX","count",10,"radius",.65,"primary","#DDD0A0FF","secondary","#BB70DAFF");
    }
    public void tick(Minecraft client) {
        if(!enabled() || client.level==null || client.player==null || client.isPaused() || client.player.isSpectator()
                || client.player.isInvisible() || client.options.getCameraType().isFirstPerson() || client.level.getGameTime()%2!=0) return;
        double phase=client.level.getGameTime()/20.0*speed.get()*Math.PI*2;
        Vec3 center=client.player.position().add(0,height.get(),0);
        for(int i=0;i<count.get();i++) {
            double a=phase+i*Math.PI*2/count.get(),r=radius.get(),x=Math.cos(a)*r,y=0,z=Math.sin(a)*r;
            switch(style.get()) {
                case ATOM -> { double tilt=(i%3-1)*Math.PI/3; y=z*Math.sin(tilt);z*=Math.cos(tilt); }
                case HELIX -> y=((double)i/(count.get()-1)-.5)*1.5;
                case CROWN -> { }
            }
            emitter.emit(client,center.add(x,y,z),0,0,0,EffectParticle.Shape.STAR,size.get().floatValue(),i%2==0?primary.get():secondary.get(),
                    secondary.get(),4,0,true,true,EffectParticle.Scaling.PULSE,Easing.LINEAR,(float)a,.03f,active);
        }
    }
}
