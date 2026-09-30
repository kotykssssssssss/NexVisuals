package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Additional decoration for a real vanilla local totem event, never a guessed entity death. */
public final class TotemEchoModule extends VisualModule {
    public enum Style { HELIX, FOUNTAIN, NOVA }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Style","Two spiral ribbons, an upward fountain, or a radial starburst.",Style.HELIX,Style.class));
    private final ColorSetting primary=add(new ColorSetting("primary","Primary","ARGB with opacity.",0xF5FFE8AA));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Secondary","End color.",0x887CE8CE));
    private final IntSetting amount=add(new IntSetting("amount","Amount","Total particles per local totem activation, still subject to the shared budget.",48,12,96));
    private final DoubleSetting size=add(new DoubleSetting("size","Size","Particle radius in blocks.",.12,.04,.4));
    private final DoubleSetting spread=add(new DoubleSetting("spread","Spread","Radius of the initial pattern in blocks.",.7,.2,1.8));
    private final DoubleSetting speed=add(new DoubleSetting("speed","Motion","Cosmetic particle speed multiplier.",1,.25,2));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks. Vanilla particles, item animation and sound remain intact.",32,10,60));
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    public TotemEchoModule(EffectEmitter emitter) {
        super("totem_echo","Totem Echo","Extra local-only decoration after vanilla receives the real totem activation. No remote player tracking.",Category.PARTICLES);
        this.emitter=emitter;
        preset("Golden Helix","Twin gold and mint ribbons rising around the player.");
        preset("Phoenix","Warm upward sparks with gravity.","style","FOUNTAIN","primary","#FFFFC06F","secondary","#AAEE6584","amount",64,"speed",1.2);
        preset("Supernova","A radial violet starburst.","style","NOVA","primary","#FFE4D0FF","secondary","#8879DFFF","size",.18,"spread",.3,"lifetime",24);
    }
    public void popped(Minecraft client) {
        if (!enabled() || client.level==null || client.player==null) return;
        Vec3 center=client.player.position();
        for(int i=0;i<amount.get();i++) {
            double t=(double)i/amount.get(), a=t*Math.PI*8, r=spread.get(), velocity=speed.get();
            Vec3 point;
            double dx,dy,dz;
            var random=client.level.random;
            switch(style.get()) {
                case HELIX -> {
                    a=(i%2==0?1:-1)*a; point=center.add(Math.cos(a)*r,.1+t*1.8,Math.sin(a)*r);
                    dx=-Math.sin(a)*.018*velocity;dy=.035*velocity;dz=Math.cos(a)*.018*velocity;
                }
                case FOUNTAIN -> {
                    a=random.nextDouble()*Math.PI*2; point=center.add(Math.cos(a)*r*.25,.35,Math.sin(a)*r*.25);
                    dx=Math.cos(a)*.065*velocity;dy=(.1+random.nextDouble()*.06)*velocity;dz=Math.sin(a)*.065*velocity;
                }
                case NOVA -> {
                    double y=1-2*t, radial=Math.sqrt(Math.max(0,1-y*y)); a=i*2.39996323;
                    dx=Math.cos(a)*radial;dy=y;dz=Math.sin(a)*radial;
                    point=center.add(dx*r,.9+dy*r,dz*r);dx*=.09*velocity;dy*=.09*velocity;dz*=.09*velocity;
                }
                default -> throw new IllegalStateException();
            }
            boolean fountain=style.get()==Style.FOUNTAIN;
            emitter.emit(client,point,dx,dy,dz,fountain?EffectParticle.Shape.SPARK:EffectParticle.Shape.STAR,size.get().floatValue(),
                    i%2==0?primary.get():secondary.get(),secondary.get(),lifetime.get(),fountain?.22f:0,true,true,
                    EffectParticle.Scaling.SHRINK,Easing.OUT_CUBIC,random.nextFloat()*6.28f,.045f,active);
        }
    }
}
