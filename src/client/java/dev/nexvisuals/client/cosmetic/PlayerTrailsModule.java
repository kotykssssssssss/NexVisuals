package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.awt.Color;
import dev.nexvisuals.core.visual.RibbonMesh;
import dev.nexvisuals.core.visual.TrailHistory;
import dev.nexvisuals.client.integration.RenderCompatibility;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.ParticleStatus;

/** Particle modes share the emitter budget; ribbon modes retain at most 64 tick samples. */
public final class PlayerTrailsModule extends VisualModule {
    public enum Style { MOTES, SILK, SPARKS, RINGS, RAINBOW, RIBBON, DUAL, LINE }
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", "Particle styles or continuous depth-tested Ribbon / Dual / Line geometry.", Style.SILK, Style.class));
    private final ColorSetting primary = add(new ColorSetting("primary", "Primary", "ARGB trail color.", 0xBB6EDFFF));
    private final ColorSetting secondary = add(new ColorSetting("secondary", "Secondary", "Color at the end of particle life.", 0x77CB8AFF));
    private final IntSetting density = add(new IntSetting("density", "Density / quality", "Samples per block travelled; maximum 12 emitted per tick.", 8, 1, 16));
    private final IntSetting lifetime = add(new IntSetting("lifetime", "Lifetime", "Life in ticks. Geometry history is also limited to 64 samples.", 18, 3, 60));
    private final DoubleSetting size = add(new DoubleSetting("size", "Width / size", "Particle radius or full ribbon width in world units.", 0.16, .03, .4));
    private final DoubleSetting distance = add(new DoubleSetting("distance", "Minimum movement", "Emission begins after moving this distance in one tick.", .035, .01, .5));
    private final DoubleSetting height = add(new DoubleSetting("height", "Height above feet", "Local-player trail emitter height.", .15, .05, 1.7));
    private final BooleanSetting fade = add(new BooleanSetting("fade", "Fade", "Fade each trail sample.", true));
    private final BooleanSetting thirdPerson = add(new BooleanSetting("third_person", "Third person only", "Avoid covering the first-person view with your trail.", true));
    private final DoubleSetting length = add(new DoubleSetting("length", "Maximum ribbon length", "Maximum travelled path length retained, in blocks.", 6, .5, 16));
    private final DoubleSetting taper = add(new DoubleSetting("taper", "Tail taper", "Narrow the ribbon towards its oldest samples.", 1, 0, 3));
    private final IntSetting quality = add(new IntSetting("quality", "Ribbon smoothness", "Interpolation subdivisions per tick segment: 1..3. Decreased particles caps this at 1.", 2, 1, 3));
    private static final RenderStateDataKey<RibbonMesh> RIBBON = RenderStateDataKey.create();
    private final TrailHistory history = new TrailHistory(64);
    private Style sampledStyle;
    private String renderStatus = "";
    private final EffectEmitter emitter;
    private Object lastLevel;
    private Vec3 previous;
    public final ParticleAppearance appearance;
    public PlayerTrailsModule(EffectEmitter emitter) {
        super("player_trails", "Player Trails", "Bounded local-player particles, respecting ordinary world depth and particle settings.", Category.PARTICLES);
        this.emitter = emitter;
        preset("Silk", "Soft overlapping gradient motes.", "style", "SILK");
        preset("Ember", "Sparse golden sparks.", "style", "SPARKS", "primary", "#EEFFD080", "secondary", "#88FF684A", "density", 5);
        preset("Prism", "A cycling rainbow trail.", "style", "RAINBOW", "size", .16);
        preset("Ripples", "Expanding rings left along the path.", "style", "RINGS", "density", 3, "size", .2);
        preset("Ribbon", "Continuous cyan ribbon with a tapered violet tail.", "style", "RIBBON", "size", .28, "height", .75, "primary", "#DD72DFFF", "secondary", "#44B298FF");
        preset("Twin Flow", "Two soft parallel ribbons.", "style", "DUAL", "size", .35, "height", .6, "primary", "#DD71E6B5", "secondary", "#44839AFF");
        preset("Light Line", "Fine gold line with a short fading tail.", "style", "LINE", "size", .1, "height", .4, "lifetime", 12, "primary", "#EEFFD080", "secondary", "#44FF7F9E");
        group("Style & colors", style, primary, secondary, thirdPerson);
        group("Motion & particles", density, lifetime, size, distance, height, fade);
        group("Ribbon geometry", length, taper, quality);
        preservePresetDefaults("size", 0.13);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        appearance.spritesWhen(()->!ribbonStyle());
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Smooth Plus", "Refined size, motion and palette; fully editable.", "style", "SILK", "size", 0.16, "particle_envelope", true, "particle_fade_in", 0.1, "particle_fade_out", 0.55);
        preset("Neon Comet", "Refined size, motion and palette; fully editable.", "style", "SPARKS", "size", 0.17, "density", 6, "particle_color_mode", "GRADIENT", "particle_drag", 0.97, "particle_shape", "STREAK");
        preset("Minimal Dots", "Small clean circular motes with a short tail.", "style", "MOTES", "size", 0.1, "density", 3, "lifetime", 12, "particle_opacity", 0.8, "particle_shape", "DOT");

    }
    private boolean ribbonStyle() { return style.get()==Style.RIBBON || style.get()==Style.DUAL || style.get()==Style.LINE; }
    private boolean visible(Minecraft client) {
        return enabled() && client.player!=null && client.level!=null && !client.player.isSpectator() && !client.player.isInvisible()
                && (!thirdPerson.get() || !client.options.getCameraType().isFirstPerson())
                && client.options.particles().get()!=ParticleStatus.MINIMAL;
    }
    public void tick(Minecraft client) {
        if (!visible(client)) { clear(); return; }
        Vec3 current = client.player.position();
        if (lastLevel != client.level || sampledStyle != style.get()) { clear(); lastLevel=client.level; sampledStyle=style.get(); }
        if (ribbonStyle()) {
            long now=client.level.getGameTime()*50;
            history.trim(now,lifetime.get()*50L,length.get());
            history.point(now,current.x,current.y+height.get(),current.z,distance.get(),3);
            previous=current;
            return;
        }
        if (previous == null) { previous = current; return; }
        double travelled = current.distanceTo(previous);
        if (travelled >= distance.get() && travelled < 3) {
            int count = Math.min(12, Math.max(1, (int) Math.ceil(travelled * density.get())));
            for (int i = 0; i < count; i++) {
                Vec3 p = previous.lerp(current, (i + 1.0) / count).add(0, height.get(), 0);
                boolean spark = style.get() == Style.SPARKS, ring = style.get() == Style.RINGS;
                int color = style.get() == Style.RAINBOW ? (primary.get() & 0xFF000000) |
                        (Color.HSBtoRGB((client.level.getGameTime() % 160) / 160f, .65f, 1) & 0xFFFFFF) : primary.get();
                color=appearance.componentColor(primary.get(),color);
                double jitter = style.get() == Style.MOTES || spark ? .02*appearance.randomness.get() : 0;
                emitter.emit(client, p, (client.level.random.nextDouble() - .5) * jitter, spark ? .035 : .002, 0,
                        spark ? EffectParticle.Shape.SPARK : ring ? EffectParticle.Shape.RING : EffectParticle.Shape.ORB,
                        size.get().floatValue(), color, secondary.get(), lifetime.get(), spark ? .25f : 0,
                        fade.get(), true, ring ? EffectParticle.Scaling.EXPAND : EffectParticle.Scaling.SHRINK,
                        Easing.LINEAR, spark ? client.level.random.nextFloat() * (float)(6.283*appearance.randomness.get()) : 0, 0, this::enabled, appearance);
            }
        }
        previous = current;
    }
    public void registerRendering() {
        WorldRenderEvents.END_EXTRACTION.register(this::extractFrame);
        // Fabric rendering-v1 16.2.10 fires BEFORE_DEBUG_RENDER during extraction, before its
        // draw context is prepared. AFTER_ENTITIES belongs to the initialized main draw pass.
        WorldRenderEvents.AFTER_ENTITIES.register(this::drawFrame);
    }
    void extractFrame(WorldExtractionContext context) {
        var state=context.worldState();
        if(state==null) return;
        ((FabricRenderState)state).setData(RIBBON,extract(context,state));
    }
    void drawFrame(WorldRenderContext context) {
        if(!enabled() || !ribbonStyle()) return;
        var state=context.worldState();
        if(state==null) return;
        var matrices=context.matrices();
        var consumers=context.consumers();
        if(matrices==null || consumers==null) return;
        RibbonMesh mesh=((FabricRenderState)state).getDataOrDefault(RIBBON,RibbonMesh.EMPTY);
        if(mesh.vertices()==0) return;
        var vertices=consumers.getBuffer(RenderTypes.debugQuads());
        var pose=matrices.last().pose();
        for(int i=0;i<mesh.vertices();i++) vertices.addVertex(pose,mesh.coordinate(i,0),mesh.coordinate(i,1),mesh.coordinate(i,2)).setColor(mesh.color(i));
    }
    private RibbonMesh extract(WorldExtractionContext context,LevelRenderState state) {
        Minecraft client=Minecraft.getInstance();
        if(!ribbonStyle() || !visible(client) || client.level!=lastLevel) return RibbonMesh.EMPTY;
        renderStatus=RenderCompatibility.blockReason();
        if(!renderStatus.isEmpty()) { history.clear(); return RibbonMesh.EMPTY; }
        float partial=context.tickCounter().getGameTimeDeltaPartialTick(false);
        if(state.cameraRenderState==null || state.cameraRenderState.pos==null) return RibbonMesh.EMPTY;
        Vec3 head=client.player.getPosition(partial), camera=state.cameraRenderState.pos;
        long now=state.gameTime*50+(long)(partial*50);
        history.trim(now,lifetime.get()*50L,length.get());
        int detail=client.options.particles().get()==ParticleStatus.DECREASED?1:quality.get();
        var tuning=appearance.snapshot();
        double visibility=dev.nexvisuals.core.visual.ParticleMath.distanceFade(head.distanceToSqr(camera),
                Math.min(tuning.maxDistance(),emitter.quality().distance),tuning.distanceFade());
        if(visibility<=0) return RibbonMesh.EMPTY;
        int accent=dev.nexvisuals.client.NexVisualsClient.instance()==null?0xFF8B9DFF:dev.nexvisuals.client.NexVisualsClient.instance().accentColor();
        int start=dev.nexvisuals.core.visual.ParticleMath.color(tuning,primary.get(),secondary.get(),0,0,state.gameTime/20.0,accent);
        int end=dev.nexvisuals.core.visual.ParticleMath.color(tuning,primary.get(),secondary.get(),1,1,state.gameTime/20.0,accent);
        start=dev.nexvisuals.client.render.Draw.withAlpha(start,(float)visibility);
        end=dev.nexvisuals.client.render.Draw.withAlpha(end,(float)visibility);
        return RibbonMesh.player(history,now,lifetime.get()*50L,size.get()*(style.get()==Style.LINE?.35:1),taper.get(),
                start,end,detail,style.get()==Style.DUAL,fade.get(),camera.x,camera.y,camera.z,head.x,head.y+height.get(),head.z);
    }
    @Override public String runtimeStatus() { return ribbonStyle()?renderStatus:""; }
    private void clear() { previous=null;lastLevel=null;sampledStyle=null;history.clear();renderStatus=""; }
    @Override protected void onDisable() { clear(); }
}
