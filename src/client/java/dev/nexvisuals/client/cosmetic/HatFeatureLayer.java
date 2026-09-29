package dev.nexvisuals.client.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.animation.EffectMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

final class HatFeatureLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final int SEGMENTS = 40;
    private static final float[] X = new float[SEGMENTS + 1], Z = new float[SEGMENTS + 1];
    static { for (int i = 0; i <= SEGMENTS; i++) { X[i] = (float) Math.cos(i * Math.PI * 2 / SEGMENTS); Z[i] = (float) Math.sin(i * Math.PI * 2 / SEGMENTS); } }
    private final CosmeticHatModule module;
    HatFeatureLayer(AvatarRenderer<?> parent, CosmeticHatModule module) { super(parent); this.module = module; }
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (!module.enabled() || client.player == null || state.id != client.player.getId() || state.isInvisible
                || state.isSpectator || client.options.getCameraType().isFirstPerson()) return;
        float radius = module.radius.get().floatValue(), height = module.height.get().floatValue();
        int rim = module.color.get(), tip = module.gradient.get() ? module.secondary.get() : rim;
        boolean outline = module.outline.get();
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        pose.translate(0, -.5 - module.offset.get(), 0);
        pose.mulPose(Axis.YP.rotationDegrees((float) ((state.ageInTicks / 20.0 * module.rotation.get()) % 360)));
        // Capture small immutable render values, never a mutable player/model reference.
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, vertices) -> {
            for (int i = 0; i < SEGMENTS; i++) {
                int edge = EffectMath.color(rim, tip, .12 * (1 + X[(i*5) % SEGMENTS]));
                int nextEdge = EffectMath.color(rim, tip, .12 * (1 + X[((i+1)*5) % SEGMENTS]));
                vertices.addVertex(matrix.pose(), 0, -height, 0).setColor(tip);
                vertices.addVertex(matrix.pose(), radius * X[i], 0, radius * Z[i]).setColor(edge);
                vertices.addVertex(matrix.pose(), radius * X[i+1], 0, radius * Z[i+1]).setColor(nextEdge);
                vertices.addVertex(matrix.pose(), 0, -height, 0).setColor(tip);
                if (outline) {
                    vertices.addVertex(matrix.pose(), radius * X[i], 0, radius * Z[i]).setColor(rim);
                    vertices.addVertex(matrix.pose(), radius * X[i], .015f, radius * Z[i]).setColor(rim);
                    vertices.addVertex(matrix.pose(), radius * X[i+1], .015f, radius * Z[i+1]).setColor(rim);
                    vertices.addVertex(matrix.pose(), radius * X[i+1], 0, radius * Z[i+1]).setColor(rim);
                }
            }
        });
        pose.popPose();
    }
}
