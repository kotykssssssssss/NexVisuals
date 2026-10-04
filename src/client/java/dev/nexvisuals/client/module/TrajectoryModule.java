package dev.nexvisuals.client.module;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nexvisuals.client.integration.RenderCompatibility;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.TrajectoryMath;
import dev.nexvisuals.core.visual.TrajectoryMath.*;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;

/** A held-item estimate for the local player only. Never queries or tracks other entities. */
public final class TrajectoryModule extends VisualModule {
    public enum Style { LINE, POINTS, LINE_POINTS }
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", "Ribbon, spaced points or both. Ordinary world depth is preserved.", Style.LINE_POINTS, Style.class));
    private final ColorSetting color = add(new ColorSetting("color", "Color", "ARGB trajectory color.", 0xFF91E2FF));
    private final DoubleSetting opacity = add(new DoubleSetting("opacity", "Opacity", "Additional alpha multiplier.", .8, .1, 1));
    private final DoubleSetting thickness = add(new DoubleSetting("thickness", "Thickness", "World-space ribbon / point size, in blocks.", .025, .01, .1));
    private final IntSetting time = add(new IntSetting("prediction_ticks", "Prediction time", "Maximum simulated ticks, at most six seconds.", 80, 10, TrajectoryMath.MAX_STEPS));
    private final IntSetting distance = add(new IntSetting("distance", "Maximum path distance", "Travelled path length, not a scan radius.", 96, 16, (int) TrajectoryMath.MAX_DISTANCE));
    private final BooleanSetting marker = add(new BooleanSetting("impact_marker", "Block impact marker", "Face-aligned ring at the first known block collision; never an entity marker.", true));
    private final DoubleSetting markerSize = add(new DoubleSetting("marker_size", "Impact marker size", "Radius of the block collision ring.", .18, .08, .5));
    private final BooleanSetting fade = add(new BooleanSetting("fade", "Fade", "Soften the distant part of the estimate.", true));
    private static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create();
    private record Frame(Path path, Vec3 camera, Style style, int color, double width, boolean fade, boolean marker, double markerSize) { }
    private final TrajectoryMath math = new TrajectoryMath();
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private final Collision collision = this::traceBlocks;
    private static final java.util.function.BiFunction<TrajectoryModule, BlockPos, Boolean> CHECK_UNCERTAIN =
            (module, cell) -> module.uncertainCell(cell) ? Boolean.TRUE : null;
    private static final java.util.function.Function<TrajectoryModule, Boolean> TRACE_COMPLETE = module -> Boolean.FALSE;
    private Path path = Path.EMPTY;
    private Minecraft currentClient;
    private ClientLevel predictedLevel;
    private String status = "Local held-item estimate: block collisions only; server spread is unknown.";

    public TrajectoryModule() {
        super("local_trajectory", "Local Trajectory", "Approximate launch path of your own held vanilla projectile. No foreign projectiles, entity scans or trajectory automation.", Category.WORLD);
        preset("Azure Guide", "A soft line with spaced points.");
        preset("Minimal", "A fine short ribbon.", "style", "LINE", "thickness", .015, "opacity", .65, "prediction_ticks", 60);
        preset("Pearl Dots", "Violet points and a slightly larger impact ring.", "style", "POINTS", "color", "#FFC6AAFF", "thickness", .04, "marker_size", .24);
        group("Appearance", style, color, opacity, thickness, fade);
        group("Prediction", time, distance, marker, markerSize);
    }

    public void tick(Minecraft client) {
        if (!enabled()) { clear(); return; }
        if (client.player == null || client.level == null || client.player.isSpectator() || client.player.isPassenger()
                || !client.options.getCameraType().isFirstPerson()) { clear(); return; }
        if (client.isPaused()) return;
        currentClient = client;
        predictedLevel = client.level;
        String blocked = RenderCompatibility.blockReason();
        if (!blocked.isEmpty()) { path = Path.EMPTY; status = blocked; return; }
        var player = client.player;
        ItemStack item = player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        if (item.isEmpty()) item = player.getOffhandItem();
        Physics physics = null;
        double speed = 0; float pitchOffset = 0;
        boolean inherit = true;
        if (item.is(Items.BOW) && player.isUsingItem() && !player.getProjectile(item).isEmpty()) {
            speed = TrajectoryMath.bowPower(player.getTicksUsingItem()) * 3F;
            if (speed >= .3) physics = Physics.ARROW;
        } else if (item.is(Items.CROSSBOW)) {
            var charged = item.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            // Server-side enchantment spread and rocket acceleration are deliberately not guessed.
            if (charged.getItems().size() == 1 && vanillaArrow(charged.getItems().getFirst())) {
                physics = Physics.ARROW; speed = 3.15F; inherit = false;
            }
        } else if (item.is(Items.TRIDENT) && player.isUsingItem() && player.getTicksUsingItem() >= 10
                && !item.nextDamageWillBreak() && EnchantmentHelper.getTridentSpinAttackStrength(item, player) == 0) {
            physics = Physics.ARROW; speed = 2.5F;
        } else if (item.is(Items.ENDER_PEARL) || item.is(Items.SNOWBALL) || item.is(Items.EGG) || item.is(Items.BLUE_EGG) || item.is(Items.BROWN_EGG)) {
            physics = Physics.THROWABLE; speed = 1.5F;
        } else if (item.is(Items.SPLASH_POTION) || item.is(Items.LINGERING_POTION)) {
            physics = Physics.POTION; speed = .5F; pitchOffset = -20;
        }
        if (physics == null || player.getCooldowns().isOnCooldown(item)) {
            path = Path.EMPTY; status = "Hold a throwable, draw a bow/trident, or use a single-arrow charged crossbow."; return;
        }
        if (item.is(Items.BOW) && !vanillaArrow(player.getProjectile(item))) {
            path = Path.EMPTY; status = "Paused: unsupported bow ammunition."; return;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD, pitch = player.getXRot() * Mth.DEG_TO_RAD;
        var movement = player.getKnownMovement();
        Launch launch = TrajectoryMath.launch(player.getX(), player.getEyeY() - (double) .1F, player.getZ(),
                -Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin((player.getXRot() + pitchOffset) * Mth.DEG_TO_RAD), Mth.cos(yaw) * Mth.cos(pitch),
                speed, inherit ? movement.x : 0, inherit && !player.onGround() ? movement.y : 0, inherit ? movement.z : 0);
        path = math.predict(launch, physics, time.get(), distance.get(), collision);
        status = path.end() == End.UNCERTAIN ? "Estimate stopped at unloaded terrain, fluid, portal or unsupported movement."
                : "Local air estimate: blocks only; entities, server spread and server changes are not predicted.";
    }
    private boolean vanillaArrow(ItemStack item) {
        return item.is(Items.ARROW) || item.is(Items.SPECTRAL_ARROW) || item.is(Items.TIPPED_ARROW);
    }
    private void traceBlocks(double x, double y, double z, double ex, double ey, double ez, Trace result) {
        var level = currentClient.level;
        // Check every crossed chunk before vanilla clipping; never request unloaded terrain.
        int minChunkX = Mth.floor(Math.min(x, ex)) >> 4, maxChunkX = Mth.floor(Math.max(x, ex)) >> 4;
        int minChunkZ = Mth.floor(Math.min(z, ez)) >> 4, maxChunkZ = Mth.floor(Math.max(z, ez)) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            if (!level.getChunkSource().hasChunk(cx, cz)) { result.contact = Contact.UNCERTAIN; return; }
        }
        position.set(Mth.floor(ex), Mth.floor(ey), Mth.floor(ez));
        if (level.isOutsideBuildHeight(position) || !level.getWorldBorder().isWithinBounds(position)) {
            result.contact = Contact.UNCERTAIN; return;
        }
        var from = new Vec3(x,y,z);
        var to = new Vec3(ex,ey,ez);
        var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        // Check only up to the first solid collision. Fluid behind a wall must not hide its marker.
        if (hit.getType() == HitResult.Type.BLOCK) {
            to = hit.getLocation();
        }
        if (BlockGetter.traverseBlocks(from, to, this, CHECK_UNCERTAIN, TRACE_COMPLETE)) {
            result.contact = Contact.UNCERTAIN; return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            var point = hit.getLocation(); var normal = hit.getDirection();
            result.block(point.x, point.y, point.z, normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
    }
    private boolean uncertainCell(BlockPos cell) {
        var level = currentClient.level;
        if (level.isOutsideBuildHeight(cell) || !level.getWorldBorder().isWithinBounds(cell)) return true;
        var block = level.getBlockState(cell);
        return !level.getFluidState(cell).isEmpty() || block.is(Blocks.NETHER_PORTAL) || block.is(Blocks.END_PORTAL)
                || block.is(Blocks.END_GATEWAY) || block.is(Blocks.COBWEB) || block.is(Blocks.POWDER_SNOW) || block.is(Blocks.SCAFFOLDING);
    }

    public void registerRendering() {
        WorldRenderEvents.END_EXTRACTION.register(this::extractFrame);
        WorldRenderEvents.AFTER_ENTITIES.register(this::drawFrame);
    }
    void extractFrame(WorldExtractionContext context) {
        if (!enabled()) return;
        var state = context.worldState();
        if (state == null) return;
        ((FabricRenderState) state).setData(FRAME, null);
        if (currentClient == null || predictedLevel != currentClient.level || currentClient.player == null
                || !currentClient.options.getCameraType().isFirstPerson() || state.cameraRenderState == null
                || state.cameraRenderState.pos == null || path.points() < 2) return;
        String reason = RenderCompatibility.blockReason();
        if (!reason.isEmpty()) { status = reason; return; }
        ((FabricRenderState) state).setData(FRAME, new Frame(path, state.cameraRenderState.pos, style.get(),
                Draw.withAlpha(color.get(), opacity.get().floatValue()), thickness.get(), fade.get(), marker.get(), markerSize.get()));
    }
    void drawFrame(WorldRenderContext context) {
        if (!enabled() || context.worldState() == null || context.matrices() == null || context.consumers() == null) return;
        var frame = ((FabricRenderState) context.worldState()).getDataOrDefault(FRAME, null);
        if (frame == null) return;
        var out = context.consumers().getBuffer(RenderTypes.debugQuads());
        var pose = context.matrices().last().pose();
        var p = frame.path(); var camera = frame.camera();
        for (int i = 1; i < p.points(); i++) {
            double x = p.coordinate(i,0)-camera.x, y = p.coordinate(i,1)-camera.y, z = p.coordinate(i,2)-camera.z;
            int argb = frame.fade() ? Draw.withAlpha(frame.color(), (float)(1-.8*i/(p.points()-1))) : frame.color();
            if (frame.style() != Style.POINTS) {
                double ax=p.coordinate(i-1,0)-camera.x, ay=p.coordinate(i-1,1)-camera.y, az=p.coordinate(i-1,2)-camera.z;
                double dx=x-ax, dy=y-ay, dz=z-az;
                double sx=dy*(-z)-dz*(-y), sy=dz*(-x)-dx*(-z), sz=dx*(-y)-dy*(-x);
                double length=Math.sqrt(sx*sx+sy*sy+sz*sz);
                if(length>1e-8) {
                    double scale=frame.width()*.5/length;
                    sx*=scale; sy*=scale; sz*=scale;
                    quad(out,pose,ax-sx,ay-sy,az-sz,ax+sx,ay+sy,az+sz,x+sx,y+sy,z+sz,x-sx,y-sy,z-sz,argb);
                }
            }
            if (frame.style()!=Style.LINE && i%2==0) point(out,pose,x,y,z,frame.width()*1.8,argb);
        }
        if (frame.marker() && p.end()==End.BLOCK) impact(out,pose,frame);
    }
    static void point(VertexConsumer out,Matrix4f pose,double x,double y,double z,double radius,int color) {
        double length=Math.sqrt(x*x+y*y+z*z), horizontal=Math.hypot(x,z);
        if(length<1e-8) return;
        // Face the camera in all three axes, including shots almost straight up/down.
        double rx=horizontal<1e-8?1:z/horizontal, rz=horizontal<1e-8?0:-x/horizontal;
        double ux=y*rz/length, uy=(z*rx-x*rz)/length, uz=-y*rx/length;
        rx*=radius; rz*=radius; ux*=radius; uy*=radius; uz*=radius;
        quad(out,pose,x-rx-ux,y-uy,z-rz-uz,x+rx-ux,y-uy,z+rz-uz,
                x+rx+ux,y+uy,z+rz+uz,x-rx+ux,y+uy,z-rz+uz,color);
    }
    private static void impact(VertexConsumer out,Matrix4f pose,Frame f) {
        Path p=f.path(); int last=p.points()-1;
        double nx=p.normal(0), ny=p.normal(1), nz=p.normal(2);
        double x=p.coordinate(last,0)-f.camera().x+nx*.008, y=p.coordinate(last,1)-f.camera().y+ny*.008, z=p.coordinate(last,2)-f.camera().z+nz*.008;
        double ux=ny!=0?1:0, uy=ny!=0?0:1, uz=0;
        double vx=ny*uz-nz*uy, vy=nz*ux-nx*uz, vz=nx*uy-ny*ux;
        double inner=f.markerSize()-.012, outer=f.markerSize()+.012;
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12, b=(i+1)*Math.PI/12;
            double ax=ux*Math.cos(a)+vx*Math.sin(a), ay=uy*Math.cos(a)+vy*Math.sin(a), az=uz*Math.cos(a)+vz*Math.sin(a);
            double bx=ux*Math.cos(b)+vx*Math.sin(b), by=uy*Math.cos(b)+vy*Math.sin(b), bz=uz*Math.cos(b)+vz*Math.sin(b);
            quad(out,pose,x+ax*inner,y+ay*inner,z+az*inner,x+ax*outer,y+ay*outer,z+az*outer,
                    x+bx*outer,y+by*outer,z+bz*outer,x+bx*inner,y+by*inner,z+bz*inner,f.color());
        }
    }
    private static void quad(VertexConsumer out,Matrix4f p,double ax,double ay,double az,double bx,double by,double bz,
                             double cx,double cy,double cz,double dx,double dy,double dz,int color) {
        out.addVertex(p,(float)ax,(float)ay,(float)az).setColor(color); out.addVertex(p,(float)bx,(float)by,(float)bz).setColor(color);
        out.addVertex(p,(float)cx,(float)cy,(float)cz).setColor(color); out.addVertex(p,(float)dx,(float)dy,(float)dz).setColor(color);
    }
    @Override public String runtimeStatus() { return status; }
    private void clear() { path=Path.EMPTY; currentClient=null; predictedLevel=null; }
    @Override protected void onDisable() { clear(); }
}
