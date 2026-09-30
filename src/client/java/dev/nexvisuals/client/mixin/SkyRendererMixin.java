package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.sky.SkyboxModule;

@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
    @Unique private final Vector4f nexvisuals$color=new Vector4f();
    @Unique private SkyboxModule nexvisuals$skybox() { return NexVisualsClient.instance()==null?null:NexVisualsClient.instance().skybox(); }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void nexvisuals$palette(ClientLevel level, float delta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        var skybox=nexvisuals$skybox();
        if(skybox!=null) skybox.extract(level,delta,camera,state);
        if (NexVisualsClient.instance() != null && (skybox==null || !skybox.active())) NexVisualsClient.instance().sky().apply(state);
    }
    @Inject(method="renderSkyDisc",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$dome(int color,CallbackInfo ci) {
        var skybox=nexvisuals$skybox(); if(skybox!=null && skybox.renderDome()) ci.cancel();
    }
    @Inject(method="renderStars",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$stars(float brightness,PoseStack poses,CallbackInfo ci) {
        var skybox=nexvisuals$skybox(); if(skybox!=null && skybox.renderStars(poses)) ci.cancel();
    }
    @ModifyConstant(method="renderSun",constant=@Constant(floatValue=30),require=2)
    private float nexvisuals$sunSize(float original) {
        var skybox=nexvisuals$skybox(); return skybox!=null && skybox.active()?original*skybox.sunSize.get().floatValue():original;
    }
    @ModifyConstant(method="renderMoon",constant=@Constant(floatValue=20),require=2)
    private float nexvisuals$moonSize(float original) {
        var skybox=nexvisuals$skybox(); return skybox!=null && skybox.active()?original*skybox.moonSize.get().floatValue():original;
    }
    @ModifyArgs(method="renderSun",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private void nexvisuals$sunTint(Args args) {
        var skybox=nexvisuals$skybox(); if(skybox!=null && skybox.active()) nexvisuals$tint(args,skybox.sunTint.get(),skybox.sunOpacity.get().floatValue(),1);
    }
    @ModifyArgs(method="renderMoon",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private void nexvisuals$moonTint(Args args) {
        var skybox=nexvisuals$skybox(); if(skybox!=null && skybox.active()) nexvisuals$tint(args,skybox.moonTint.get(),skybox.moonOpacity.get().floatValue(),1);
    }
    @ModifyArgs(method="renderStars",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private void nexvisuals$vanillaStars(Args args) {
        var skybox=nexvisuals$skybox();
        if(skybox!=null && skybox.active() && skybox.stars.get()==SkyboxModule.Stars.VANILLA) {
            float pulse=skybox.twinkle.get()?(float)dev.nexvisuals.core.visual.StarField.twinkle(skybox.frame().seconds(),skybox.twinkleSpeed.get(),0,skybox.twinkleIntensity.get()):1;
            nexvisuals$tint(args,skybox.starColor.get(),skybox.starOpacity.get().floatValue(),skybox.starBrightness.get().floatValue()*pulse);
        }
    }
    @Unique private void nexvisuals$tint(Args args,int tint,float opacity,float brightness) {
        Vector4fc original=args.get(1);
        nexvisuals$color.set(original).mul(((tint>>>16)&255)/255f*brightness,((tint>>>8)&255)/255f*brightness,(tint&255)/255f*brightness,(tint>>>24)/255f*opacity);
        args.set(1,nexvisuals$color);
    }
    @Inject(method="close",at=@At("HEAD"))
    private void nexvisuals$release(CallbackInfo ci) { var skybox=nexvisuals$skybox(); if(skybox!=null) skybox.closeRenderer(); }
}
